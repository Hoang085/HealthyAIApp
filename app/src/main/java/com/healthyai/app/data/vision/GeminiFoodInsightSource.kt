package com.healthyai.app.data.vision

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.healthyai.app.data.network.HttpPost
import com.healthyai.app.data.network.HttpPostResponse
import com.healthyai.app.domain.usecase.food.FoodAnalysis
import kotlinx.coroutines.delay
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named

/** [FoodInsightSource] thật gọi Gemini qua REST: model đọc từ cấu hình, hạ `thinkingLevel` xuống "low" cho nhanh, có model dự phòng khi quá tải. */
class GeminiFoodInsightSource @Inject constructor(
    private val httpPost: HttpPost,
    @Named(NAMED_API_KEY) private val apiKey: String,
    @Named(NAMED_MODEL) private val model: String,
    @Named(NAMED_FALLBACK_MODEL) private val fallbackModel: String
) : FoodInsightSource {

    private val gson = Gson()

    /** Một lượt phân tích, lần lượt qua model chính rồi model dự phòng; mỗi model chỉ gọi một lần vì thử lại cùng model vừa quá tải hầu như vô ích. */
    override suspend fun analyze(jpegBytes: ByteArray): FoodAnalysisResult {
        if (apiKey.isBlank()) return FoodAnalysisResult.Failure(FoodAnalysisError.NOT_CONFIGURED)

        val jpegBase64 = Base64.getEncoder().encodeToString(jpegBytes)

        val models = candidates()
        var lastError = FoodAnalysisError.NETWORK
        models.forEachIndexed { index, candidate ->
            when (val result = analyzeWithModel(candidate, jpegBase64)) {
                is FoodAnalysisResult.Success -> return result
                is FoodAnalysisResult.Failure -> {
                    lastError = result.error
                    // Lỗi không thể tự khỏi (sai khoá, sai model…) thì đổi model cũng vô ích.
                    if (result.error !in RETRYABLE_ERRORS) return result
                    if (index < models.lastIndex) delay(RETRY_DELAY_MS)
                }
            }
        }

        Log.w(TAG, "Phân tích món ăn thất bại sau ${models.size} model: $lastError")
        return FoodAnalysisResult.Failure(lastError)
    }

    /** Model chính trước, model dự phòng sau — bỏ giá trị rỗng và tên trùng nhau. */
    private fun candidates(): List<String> =
        listOf(model, fallbackModel).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Gọi một model: ưu tiên request có `thinkingLevel` thấp; nếu model không hỗ trợ thì gửi lại một lần không kèm `thinkingConfig`. */
    private suspend fun analyzeWithModel(candidate: String, jpegBase64: String): FoodAnalysisResult {
        val response = post(candidate, jpegBase64, withThinkingConfig = true)
            ?: return FoodAnalysisResult.Failure(FoodAnalysisError.NETWORK)

        // 400 khi model không hỗ trợ thinkingLevel ⇒ gửi lại không kèm thinkingConfig, vẫn phân tích được thay vì báo "phản hồi hỏng".
        if (response.code == HTTP_BAD_REQUEST && isThinkingConfigRejected(response.body)) {
            Log.w(TAG, "Model $candidate không nhận thinkingConfig — gửi lại không kèm tham số này.")
            val withoutThinking = post(candidate, jpegBase64, withThinkingConfig = false)
                ?: return FoodAnalysisResult.Failure(FoodAnalysisError.NETWORK)
            return mapResponse(withoutThinking)
        }

        return mapResponse(response)
    }

    private suspend fun post(
        candidate: String,
        jpegBase64: String,
        withThinkingConfig: Boolean
    ): HttpPostResponse? = httpPost.post(
        "$ENDPOINT_BASE$candidate$GENERATE_SUFFIX",
        headers(),
        buildRequestBody(jpegBase64, withThinkingConfig)
    )

    /** Khoá API nằm ở header (không nhét vào URL) để không lọt vào log/query string. */
    private fun headers(): Map<String, String> = mapOf(HEADER_API_KEY to apiKey)

    /** Ánh xạ mã HTTP của Gemini sang loại lỗi nghiệp vụ. */
    internal fun mapResponse(response: HttpPostResponse): FoodAnalysisResult = when (response.code) {
        HTTP_OK -> parseResponse(response.body)
        // 401/403: khoá sai, chưa bật API, hoặc đã bị thu hồi.
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> FoodAnalysisResult.Failure(FoodAnalysisError.AUTH)
        // 429/503: quá tải/giới hạn tần suất — Google mô tả là tạm thời.
        HTTP_TOO_MANY_REQUESTS, HTTP_UNAVAILABLE -> FoodAnalysisResult.Failure(FoodAnalysisError.BUSY)
        // 5xx khác: lỗi phía máy chủ, cũng đáng thử lại.
        in HTTP_SERVER_ERROR_RANGE -> FoodAnalysisResult.Failure(FoodAnalysisError.SERVER)
        // 404: model đã bị Google bỏ — phải đổi `gemini.model`; 400 và phần còn lại: request không hợp lệ, thử lại vô ích.
        else -> FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE)
    }

    /** Đọc phản hồi Gemini: nội dung là một chuỗi JSON ở `candidates[0].content.parts[0].text` (Gemini trả JSON-as-text). */
    internal fun parseResponse(body: String): FoodAnalysisResult {
        val envelope = runCatching { gson.fromJson(body, GeminiEnvelope::class.java) }.getOrNull()
        val text = envelope?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (text.isNullOrBlank()) return FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE)

        val payload = try {
            gson.fromJson(text, LlmFoodPayload::class.java)
        } catch (e: JsonSyntaxException) {
            null
        } ?: return FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE)

        return FoodAnalysisResult.Success(
            FoodAnalysis.toInsight(
                isFood = payload.isFood,
                dishName = payload.dishName,
                emoji = payload.emoji,
                benefits = payload.benefits,
                cautions = payload.cautions
            )
        )
    }

    /** Dựng body `generateContent`: ảnh base64 + prompt, `generationConfig` ép đầu ra theo JSON schema; `withThinkingConfig=false` cho model đời cũ. */
    internal fun buildRequestBody(jpegBase64: String, withThinkingConfig: Boolean = true): String {
        val generationConfig = mutableMapOf<String, Any>(
            "temperature" to TEMPERATURE,
            "responseMimeType" to JSON_MIME_TYPE,
            "responseSchema" to RESPONSE_SCHEMA
        )
        if (withThinkingConfig) {
            generationConfig["thinkingConfig"] = mapOf("thinkingLevel" to THINKING_LEVEL)
        }

        val payload = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to JPEG_MIME_TYPE,
                                "data" to jpegBase64
                            )
                        ),
                        mapOf("text" to FoodAnalysis.buildPrompt())
                    )
                )
            ),
            "generationConfig" to generationConfig
        )
        return gson.toJson(payload)
    }

    /** Model có từ chối `thinkingConfig` không? Nhận biết theo nội dung thông báo chứ không theo mã 400 đơn thuần. */
    internal fun isThinkingConfigRejected(body: String): Boolean =
        body.contains(THINKING_MARKER, ignoreCase = true) &&
            body.contains(INVALID_ARGUMENT_MARKER, ignoreCase = true)

    /** Phần thân phản hồi của Gemini (chỉ khai báo trường thật sự dùng). */
    private data class GeminiEnvelope(val candidates: List<Candidate>? = null) {
        data class Candidate(val content: Content? = null)
        data class Content(val parts: List<Part>? = null)
        data class Part(val text: String? = null)
    }

    /** JSON mà model được yêu cầu trả về (khoá snake_case theo [RESPONSE_SCHEMA]). */
    private data class LlmFoodPayload(
        @SerializedName("is_food") val isFood: Boolean = false,
        @SerializedName("dish_name") val dishName: String? = null,
        @SerializedName("emoji") val emoji: String? = null,
        @SerializedName("benefits") val benefits: List<String>? = null,
        @SerializedName("cautions") val cautions: List<String>? = null
    )

    companion object {
        private const val TAG = "GeminiFoodInsight"

        /** Tên tham số `@Named` để `di/AiModule.kt` cấp khoá/model cho class này. */
        const val NAMED_API_KEY = "geminiApiKey"
        const val NAMED_MODEL = "geminiModel"
        const val NAMED_FALLBACK_MODEL = "geminiFallbackModel"

        private const val ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models/"
        private const val GENERATE_SUFFIX = ":generateContent"
        private const val HEADER_API_KEY = "x-goog-api-key"

        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val JSON_MIME_TYPE = "application/json"

        /** Nhiệt độ thấp: đây là bài đọc ảnh, không phải bài viết sáng tạo. */
        private const val TEMPERATURE = 0.2

        /** Mức suy luận thấp nhất model dòng Gemini 3 chấp nhận (Gemini 3 mặc định `high` rất chậm; "minimal" bị từ chối, chỉ "low" hợp lệ). */
        private const val THINKING_LEVEL = "low"

        /** Hai mẩu thông báo lỗi để biết model từ chối `thinkingConfig` (xem hàm cùng tên). */
        private const val THINKING_MARKER = "thinking"
        private const val INVALID_ARGUMENT_MARKER = "INVALID_ARGUMENT"

        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_UNAVAILABLE = 503
        private const val HTTP_BAD_REQUEST = 400

        /** Mọi mã 5xx khác (500, 502…) — lỗi phía máy chủ, đáng thử lại. */
        private val HTTP_SERVER_ERROR_RANGE = 500..599

        /** Nghỉ giữa hai model: đủ ngắn để người dùng không thấy "đứng hình", đủ dài để không dồn dập lúc Google quá tải. */
        private const val RETRY_DELAY_MS = 600L

        private val RETRYABLE_ERRORS = setOf(
            FoodAnalysisError.BUSY,
            FoodAnalysisError.SERVER,
            FoodAnalysisError.NETWORK
        )

        /** Schema ép model trả đúng JSON cần thiết; `required` không gồm `emoji` để model bỏ trống khi không có emoji phù hợp. */
        private val RESPONSE_SCHEMA = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "is_food" to mapOf("type" to "BOOLEAN"),
                "dish_name" to mapOf("type" to "STRING"),
                "emoji" to mapOf("type" to "STRING"),
                "benefits" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf("type" to "STRING"),
                    "maxItems" to FoodAnalysis.MAX_BULLETS
                ),
                "cautions" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf("type" to "STRING"),
                    "maxItems" to FoodAnalysis.MAX_BULLETS
                )
            ),
            "required" to listOf("is_food", "dish_name", "benefits", "cautions"),
            "propertyOrdering" to listOf("is_food", "dish_name", "emoji", "benefits", "cautions")
        )
    }
}
