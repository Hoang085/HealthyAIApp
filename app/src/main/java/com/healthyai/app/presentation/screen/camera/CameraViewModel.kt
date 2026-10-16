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

/** Trạng thái màn "Ghi chép bữa ăn". */
data class CameraUiState(
    val isAnalyzing: Boolean = false,
    val insight: FoodInsight? = null,
    val noFoodDetected: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
)

/** ViewModel màn "Ghi chép bữa ăn": ảnh → nén JPEG → FoodInsightSource (Gemini); AI tiêm vào nên test JVM được. */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val foodInsightSource: FoodInsightSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /** JPEG lượt phân tích gần nhất để nút "Thử lại" chạy lại; chỉ giữ một ảnh, không ghi ra đĩa. */
    private var lastJpeg: ByteArray? = null

    /** Phân tích ảnh dạng [Bitmap] — ảnh vừa chụp từ CameraX hoặc ảnh chọn từ thư viện. */
    fun analyzeBitmap(bitmap: Bitmap) = runAnalysis { ImageJpegEncoder.encode(bitmap) }

    /** Chạy lại lượt phân tích gần nhất (nút "Thử lại"); không làm gì nếu chưa từng có ảnh. */
    fun retryLastAnalysis() {
        val jpeg = lastJpeg ?: return
        runAnalysis { jpeg }
    }

    /** Một lượt phân tích; nhận hàm lấy JPEG thay vì ảnh nên test JVM được mà không cần Bitmap Android. */
    internal fun runAnalysis(jpegForImage: suspend () -> ByteArray) {
        // Xoá kết quả/lỗi lượt trước ngay (đồng bộ) để hộp thoại "Đang phân tích" hiện tức thì.
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
                            // AI nói không có đồ ăn hoặc không gọi được tên món ⇒ coi như chưa nhận diện được.
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
                // viewModelScope bị huỷ (rời màn) không phải lỗi phân tích → ném tiếp.
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

    /** Dịch loại lỗi thành câu tiếng Việt cho người dùng. */
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
