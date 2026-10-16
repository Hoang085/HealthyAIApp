package com.healthyai.app.data.heartrate

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.healthyai.app.domain.usecase.heartrate.HeartRateMath

/** Bộ đọc khung hình PPG: tính giá trị đỏ trung bình vùng giữa mỗi khung, chỉ đọc hai mặt phẳng Y và V thay vì đổi sang RGB cho nhanh. */
class CameraPpgAnalyzer(
    private val onSample: (red: Double, timestampMs: Long) -> Unit
) : ImageAnalysis.Analyzer {

    /** Đọc một khung hình rồi phát giá trị đỏ trung bình; luôn close() khung hình để ImageAnalysis không hết bộ đệm. */
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

    /** Trả về giá trị đỏ trung bình vùng giữa khung (50%×50%), hoặc null nếu không phải YUV_420_888/thiếu dữ liệu. */
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

        /** Bước lấy mẫu 4 điểm: đủ mẫu để tính trung bình mà vẫn kịp 30 khung hình/giây trên máy yếu. */
        const val SAMPLE_STEP = 4

        const val NANOS_PER_MILLI = 1_000_000L
    }
}
