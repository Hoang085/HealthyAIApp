package com.healthyai.app.data.heartrate

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.healthyai.app.domain.usecase.heartrate.HeartRateMath

/**
 * Bộ đọc khung hình camera cho phép **đo nhịp tim bằng camera + đèn flash** (PPG).
 *
 * Mỗi khung hình, lớp này tính **giá trị đỏ trung bình của vùng giữa khung** — đúng vùng mà ngón tay
 * che lên (đèn flash và ống kính nằm cạnh nhau ở mặt sau máy) — rồi đưa cho tầng phân tích
 * (`PpgAnalyzer`) theo từng khung hình kèm mốc thời gian.
 *
 * Vì sao chỉ đọc **hai mặt phẳng `Y` và `V`** thay vì đổi cả khung hình sang RGB:
 * - `YUV_420_888` là định dạng mặc định của `ImageAnalysis`; `Y` và `V` đã nằm sẵn trong bộ đệm, đổi
 *   sang RGB từng điểm ảnh là thừa (và chậm gấp nhiều lần ở 30 khung hình/giây);
 * - YUV→RGB là phép biến đổi **tuyến tính**, nên trung bình kênh đỏ suy trực tiếp từ trung bình `Y`
 *   và `V` — xem [HeartRateMath.redFromYuvMeans];
 * - lấy vùng giữa thay vì cả khung để bỏ viền tối/ngón tay lệch và giảm số điểm ảnh phải đọc.
 */
class CameraPpgAnalyzer(
    private val onSample: (red: Double, timestampMs: Long) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * Đọc một khung hình và phát giá trị đỏ trung bình.
     *
     * Khung hình luôn được `close()` (kể cả khi lỗi) — nếu không, `ImageAnalysis` sẽ hết bộ đệm và
     * **dừng phát khung hình**, làm phép đo đứng im giữa chừng. Khung hình lỗi/định dạng lạ bị bỏ qua
     * chứ không làm chết camera.
     */
    override fun analyze(image: ImageProxy) {
        try {
            val red = meanRedOfCenterRoi(image)
            if (red != null) {
                onSample(red, image.imageInfo.timestamp / NANOS_PER_MILLI)
            }
        } finally {
            image.close()
        }
    }

    /**
     * Giá trị đỏ trung bình của vùng **giữa** khung hình (50% chiều rộng × 50% chiều cao), hoặc `null`
     * khi khung hình không phải `YUV_420_888` hay không đủ dữ liệu.
     */
    private fun meanRedOfCenterRoi(image: ImageProxy): Double? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val planes = image.planes
        if (planes.size < 3) return null

        val lumaPlane = planes[0]
        val chromaPlane = planes[V_PLANE_INDEX]
        val luma = lumaPlane.buffer
        val chroma = chromaPlane.buffer

        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val left = width / 4
        val right = width * 3 / 4
        val top = height / 4
        val bottom = height * 3 / 4

        var lumaSum = 0L
        var chromaSum = 0L
        var count = 0

        var y = top
        while (y < bottom) {
            val lumaRowStart = y * lumaPlane.rowStride
            // Mặt phẳng sắc độ chỉ có nửa số hàng so với mặt phẳng độ sáng (lấy mẫu 4:2:0).
            val chromaRowStart = (y / 2) * chromaPlane.rowStride
            var x = left
            while (x < right) {
                val lumaIndex = lumaRowStart + x * lumaPlane.pixelStride
                val chromaIndex = chromaRowStart + (x / 2) * chromaPlane.pixelStride
                if (lumaIndex < luma.capacity() && chromaIndex < chroma.capacity()) {
                    lumaSum += luma.get(lumaIndex).toInt() and 0xFF
                    chromaSum += chroma.get(chromaIndex).toInt() and 0xFF
                    count += 1
                }
                x += SAMPLE_STEP
            }
            y += SAMPLE_STEP
        }

        if (count == 0) return null
        return HeartRateMath.redFromYuvMeans(
            luma = lumaSum.toDouble() / count,
            chromaV = chromaSum.toDouble() / count
        )
    }

    private companion object {
        /** `planes` của `YUV_420_888` theo thứ tự `Y`, `U`, `V` — kênh đỏ lấy từ `V`. */
        const val V_PLANE_INDEX = 2

        /**
         * Bước lấy mẫu (điểm ảnh). Vùng giữa ở 640×480 là 320×240 điểm; lấy cách 4 điểm chỉ còn
         * 4 800 mẫu — thừa để tính trung bình mà vẫn kịp 30 khung hình/giây trên máy yếu.
         */
        const val SAMPLE_STEP = 4

        const val NANOS_PER_MILLI = 1_000_000L
    }
}
