package com.healthyai.app.presentation.screen.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.data.vision.FoodAnalysisError
import com.healthyai.app.data.vision.FoodAnalysisResult
import com.healthyai.app.data.vision.FoodInsightSource
import com.healthyai.app.data.vision.ImageJpegEncoder
import com.healthyai.app.domain.model.FoodInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Trạng thái màn "Ghi chép bữa ăn".
 *
 * @property isAnalyzing Đang gọi AI — màn hình hiện **hộp thoại "Đang phân tích món ăn"** và khoá hai
 *   nút phân tích.
 * @property insight Kết quả phân tích (tên món + lợi ích/điều nên lưu ý). `null` khi chưa có kết quả
 *   hoặc khi AI không nhận ra món nào.
 * @property noFoodDetected AI đã trả lời nhưng ảnh **không có gì ăn được** (hoặc không gọi được tên
 *   món) — màn hình báo thẳng thay vì hiện thẻ kết quả rỗng.
 * @property error Thông báo lỗi hiển thị trên màn hình (mạng, AI quá tải, khoá sai…).
 * @property canRetry Có ảnh của lượt trước để **thử lại** mà không cần chụp/chọn lại.
 */
data class CameraUiState(
    val isAnalyzing: Boolean = false,
    val insight: FoodInsight? = null,
    val noFoodDetected: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
)

/**
 * ViewModel màn "Ghi chép bữa ăn": chụp/chọn ảnh → [ImageJpegEncoder] nén → [FoodInsightSource] (Gemini)
 * → [FoodInsight] hiển thị (mục 3 #42).
 *
 * **Không** còn dùng ML Kit Image Labeling: mô hình đó chỉ gán **nhãn ảnh chung** tiếng Anh nên hay ra
 * kết quả vô nghĩa với bữa ăn (chụp đĩa cơm mà ra "Pizza") — xem `FoodAnalysis` để biết các ràng buộc
 * mới đặt lên câu trả lời của AI.
 *
 * **Không** còn cổng đồng ý `food_ai_enabled` (mục 3 #43): bỏ công tắc "Phân tích món ăn bằng AI" ở
 * màn hình, nên bấm "Phân tích món ăn" / "Chọn ảnh từ thư viện" là ảnh được phân tích luôn. Việc ảnh
 * rời thiết bị được nói rõ trong Chính sách bảo mật (mở từ tab **Phân tích AI**).
 *
 * Nguồn AI được **tiêm vào** nên máy trạng thái dưới đây kiểm tra được trên JVM
 * (`CameraViewModelTest`) mà không cần mạng, không cần camera, không cần khoá API.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val foodInsightSource: FoodInsightSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * JPEG của lượt phân tích gần nhất — giữ lại để nút **"Thử lại"** chạy lại mà người dùng không phải
     * chụp lại (ảnh chọn từ thư viện thì càng khó chọn lại). Chỉ giữ **một** ảnh, không ghi ra đĩa.
     */
    private var lastJpeg: ByteArray? = null

    /** Phân tích ảnh dạng [Bitmap] — ảnh vừa chụp từ CameraX hoặc ảnh chọn từ thư viện. */
    fun analyzeBitmap(bitmap: Bitmap) = runAnalysis { ImageJpegEncoder.encode(bitmap) }

    /**
     * Chạy lại lượt phân tích gần nhất (nút "Thử lại" ở thẻ lỗi). Không làm gì nếu chưa từng có ảnh.
     */
    fun retryLastAnalysis() {
        val jpeg = lastJpeg ?: return
        runAnalysis { jpeg }
    }

    /**
     * Một lượt phân tích: lấy **JPEG** của ảnh rồi nhờ [foodInsightSource] phân tích.
     *
     * Nhận **hàm lấy JPEG** thay vì ảnh nên unit test JVM chạy được mọi nhánh (nhận ra món / ảnh không
     * có đồ ăn / lỗi mạng / AI quá tải) mà không cần `Bitmap` của Android.
     */
    internal fun runAnalysis(jpegForImage: suspend () -> ByteArray) {
        // Lượt mới xoá sạch kết quả/lỗi của lượt trước **ngay lập tức** (đồng bộ) để hộp thoại "Đang
        // phân tích món ăn" hiện ra tức thì.
        _uiState.update {
            it.copy(isAnalyzing = true, insight = null, noFoodDetected = false, error = null, canRetry = false)
        }

        viewModelScope.launch {
            try {
                val jpeg = jpegForImage()
                lastJpeg = jpeg

                when (val result = foodInsightSource.analyze(jpeg)) {
                    is FoodAnalysisResult.Success -> _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            insight = result.insight.takeIf { it.hasDish },
                            // AI nói ảnh không có đồ ăn, hoặc có mà không gọi được tên món ⇒ coi như chưa
                            // nhận diện được (một thẻ kết quả không có tên món thì không nói lên điều gì).
                            noFoodDetected = !result.insight.hasDish,
                            error = null,
                            canRetry = false
                        )
                    }

                    is FoodAnalysisResult.Failure -> _uiState.update { state ->
                        state.copy(
                            isAnalyzing = false,
                            error = messageFor(result.error),
                            canRetry = lastJpeg != null
                        )
                    }
                }
            } catch (e: CancellationException) {
                // viewModelScope bị huỷ (rời màn) — không phải lỗi phân tích, ném tiếp (phụ lục C #39).
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Lỗi phân tích món ăn: ${e.message}",
                        isAnalyzing = false,
                        canRetry = lastJpeg != null
                    )
                }
            }
        }
    }

    /** Lỗi xảy ra trước khi có ảnh để phân tích (khởi tạo camera, chụp ảnh thất bại). */
    fun onCaptureError(message: String) = _uiState.update {
        it.copy(
            error = "Không chụp được ảnh: $message",
            isAnalyzing = false,
            canRetry = lastJpeg != null
        )
    }

    /** Lỗi khi đọc ảnh người dùng chọn từ thư viện (uri hỏng, tệp bị xoá, không phải ảnh…). */
    fun onImageLoadError(message: String) = _uiState.update {
        it.copy(
            error = "Không đọc được ảnh đã chọn: $message",
            isAnalyzing = false,
            canRetry = lastJpeg != null
        )
    }

    /** Xoá kết quả/lỗi hiển thị (ví dụ khi rời màn rồi quay lại). */
    fun clearResult() {
        lastJpeg = null
        _uiState.update { CameraUiState() }
    }

    /**
     * Dịch loại lỗi thành câu tiếng Việt cho người dùng.
     *
     * Câu chữ sinh trong ViewModel (giống các lỗi sẵn có của màn này) — nằm ngoài phạm vi i18n đã chốt ở
     * mục 3 #37; xem ghi chú "còn lại" trong `TESTCASE_HealthyAI.md`.
     */
    private fun messageFor(error: FoodAnalysisError): String = when (error) {
        FoodAnalysisError.NOT_CONFIGURED ->
            "Bản cài này chưa có khoá AI (Gemini), nên chưa phân tích được món ăn."
        FoodAnalysisError.NETWORK ->
            "Không kết nối được tới AI. Kiểm tra mạng rồi thử lại."
        FoodAnalysisError.BUSY ->
            "AI đang quá tải. Vui lòng thử lại sau vài giây."
        FoodAnalysisError.AUTH ->
            "Khoá AI không hợp lệ hoặc đã bị thu hồi."
        FoodAnalysisError.SERVER ->
            "Máy chủ AI đang gặp lỗi. Vui lòng thử lại."
        FoodAnalysisError.BAD_RESPONSE ->
            "AI trả về kết quả không đọc được. Vui lòng thử lại."
    }
}
