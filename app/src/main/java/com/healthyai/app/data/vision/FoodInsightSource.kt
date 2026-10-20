package com.healthyai.app.data.vision

import com.healthyai.app.domain.model.FoodInsight

/** Lý do một lượt phân tích món ăn thất bại; tách enum để tầng dữ liệu không phải biết tiếng Việt, ViewModel tự dịch thành thông báo. */
enum class FoodAnalysisError {
    /** Chưa cấu hình khoá Gemini cho bản build này (`GEMINI_API_KEY` rỗng). */
    NOT_CONFIGURED,

    /** Không kết nối được (mất mạng, DNS, timeout). */
    NETWORK,

    /** Gemini quá tải / bị giới hạn tần suất (HTTP 429, 503) — thử lại được. */
    BUSY,

    /** Khoá sai hoặc đã bị thu hồi (HTTP 401, 403). */
    AUTH,

    /** Lỗi phía máy chủ (HTTP 5xx) — thử lại được. */
    SERVER,

    /** Request không được chấp nhận (sai tên model, tham số) hoặc phản hồi không đọc được. */
    BAD_RESPONSE
}

/** Kết quả một lượt phân tích món ăn: thành công (kể cả "ảnh không có đồ ăn") hay thất bại. */
sealed interface FoodAnalysisResult {

    data class Success(val insight: FoodInsight) : FoodAnalysisResult

    data class Failure(val error: FoodAnalysisError) : FoodAnalysisResult
}

/** Cổng nhận diện + phân tích món ăn; bản thật gọi Gemini, unit test JVM thay bằng giả lập. */
interface FoodInsightSource {

    /** Phân tích một ảnh đã nén JPEG; nhận `ByteArray` để không phụ thuộc `android.graphics`, nhờ vậy test chạy trên JVM thuần. */
    suspend fun analyze(jpegBytes: ByteArray): FoodAnalysisResult
}
