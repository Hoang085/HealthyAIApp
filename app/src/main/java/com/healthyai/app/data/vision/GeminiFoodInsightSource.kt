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

/**
 * [FoodInsightSource] thật: gửi ảnh cho **Gemini** (Google AI) và nhận về tên món + lợi ích/điều nên
 * lưu ý (mục 3 #42, #43).
 *
 * ### Vì sao dùng API trực tiếp (REST) chứ không dùng SDK
 * SDK của Google/Firebase AI cần thêm dependency và **không** có sẵn trong cache của build offline;
 * trong khi yêu cầu chỉ là một POST JSON có ảnh base64. Dùng [HttpPost] + Gson giữ được đúng cấu trúc
 * "cổng ra ngoài" của repo và **unit test JVM chạy được toàn bộ** phần ánh xạ lỗi/parse.
 *
 * ### Những điều đã kiểm chứng bằng request thật
 * - Khoá dạng `AQ.…` (Authentication Key mới của Google AI Studio) dùng **header `x-goog-api-key`**;
 *   đặt khoá vào `Authorization: Bearer` sẽ bị `401 ACCESS_TOKEN_TYPE_UNSUPPORTED`.
 * - Tên model **bị Google bỏ** sẽ trả `404` kèm gợi ý model mới (`gemini-2.5-flash` → *"no longer
 *   available to new users, use models/gemini-3.8-flash"*) ⇒ **không hardcode model**: tên model đọc
 *   từ `GEMINI_MODEL`.
 * - **Độ trễ (đo 2026-09-30):** model dòng Gemini 3 **mặc định suy luận ở mức `high`** — một request
 *   đọc ảnh món ăn mất ~**42 giây** ở mức mặc định. Gemini 3 dùng `thinkingConfig.thinkingLevel`
 *   (thay cho `thinkingBudget` của 2.5) ⇒ hạ xuống `"low"` là cách giảm độ trễ đúng theo tài liệu.
 *   `"minimal"` **bị model này từ chối** (`400 INVALID_ARGUMENT`: *"Thinking level MINIMAL is not
 *   supported for this model"*) nên chỉ dùng `"low"`; khi model không hỗ trợ tham số này, lớp này
 *   **tự gửi lại y nguyên request không kèm `thinkingConfig`** (xem [isThinkingConfigRejected]) thay
 *   vì để người dùng gặp lỗi "phản hồi không đọc được".
 * - Google thường xuyên trả `503 UNAVAILABLE` (*"high demand"*) và `429 RESOURCE_EXHAUSTED` (*"you
 *   exceeded your current quota"*) ⇒ có **model dự phòng**: model chính trước, hỏng vì quá tải thì
 *   tự chuyển sang [fallbackModel] (mặc định `gemini-flash-lite-latest` — bản nhẹ, phản hồi nhanh).
 *
 * ### Riêng tư
 * Ảnh **rời khỏi thiết bị** ở lớp này — người dùng bấm "Phân tích món ăn" nghĩa là ảnh được gửi tới
 * Google Gemini (không còn công tắc bật/tắt riêng từ mục 3 #43). Nội dung này được nói rõ trong
 * `privacy_camera_body` (`strings.xml`), màn Chính sách bảo mật mở từ tab **Phân tích AI**.
 */
class GeminiFoodInsightSource @Inject constructor(
    private val httpPost: HttpPost,
    @Named(NAMED_API_KEY) private val apiKey: String,
    @Named(NAMED_MODEL) private val model: String,
    @Named(NAMED_FALLBACK_MODEL) private val fallbackModel: String
) : FoodInsightSource {

    private val gson = Gson()

    /**
     * Một lượt phân tích, đi lần lượt qua **model chính rồi model dự phòng**.
     *
     * Mỗi model chỉ được gọi **một** lần (không thử lại cùng model): thử lại ngay trên model vừa báo
     * quá tải hầu như luôn thất bại tiếp, trong khi đổi sang model khác có cơ hội thành công thật —
     * và người dùng chờ ít lâu hơn. Trường hợp xấu nhất (cả hai model đều quá tải) là **2** request,
     * hoặc **4** nếu cả hai model đều không nhận `thinkingConfig` (mỗi model phải gửi lại một lần).
     */
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

    /**
     * Gọi **một** model: ưu tiên bản request có `thinkingLevel` thấp (nhanh nhất); nếu model không
     * hỗ trợ tham số đó thì gửi lại đúng một lần mà không kèm `thinkingConfig`.
     */
    private suspend fun analyzeWithModel(candidate: String, jpegBase64: String): FoodAnalysisResult {
        val response = post(candidate, jpegBase64, withThinkingConfig = true)
            ?: return FoodAnalysisResult.Failure(FoodAnalysisError.NETWORK)

        // 400 INVALID_ARGUMENT khi model không hỗ trợ thinkingLevel (đã gặp thật với "minimal") ⇒
        // gửi lại không kèm thinkingConfig, vẫn phân tích được thay vì báo "phản hồi hỏng".
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

    /** Khoá API nằm ở **header** (không nhét vào URL) để không lọt vào log/query string. */
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
        // 404: tên model không còn tồn tại (Google đã bỏ) — người dùng phải đổi `gemini.model`.
        // 400 và phần còn lại: request không hợp lệ ⇒ thử lại cũng vô ích.
        else -> FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE)
    }

    /**
     * Đọc phản hồi Gemini: nội dung nằm ở `candidates[0].content.parts[0].text` dưới dạng **một chuỗi
     * JSON** (Gemini luôn trả JSON-as-text, kể cả khi đã bật `responseMimeType`).
     */
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

    /**
     * Dựng body request theo đúng định dạng `generateContent` của Gemini: một `inline_data` (ảnh base64,
     * **không** kèm tiền tố `data:image/...`) + một `part` văn bản là [FoodAnalysis.buildPrompt], và
     * `generationConfig` ép đầu ra theo **JSON schema** để đọc được bằng máy thay vì phải "đoán" văn xuôi.
     *
     * @param withThinkingConfig `true` (mặc định) → kèm `thinkingConfig` hạ mức suy luận xuống
     *   [THINKING_LEVEL] cho **nhanh**; `false` → bỏ hẳn tham số, dùng cho model đời cũ không hỗ trợ.
     */
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

    /**
     * Model có **từ chối** `thinkingConfig` không?
     *
     * Đã gặp thật: `gemini-3.8-flash` trả `400 INVALID_ARGUMENT` với câu *"Thinking level MINIMAL is
     * not supported for this model"*. Nhận biết theo **nội dung thông báo** (chứ không theo mã 400 đơn
     * thuần, vì 400 còn do nhiều nguyên nhân khác) để chỉ gửi lại khi đúng lý do này.
     */
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

        /** Nhiệt độ thấp: đây là bài **đọc ảnh**, không phải bài viết sáng tạo. */
        private const val TEMPERATURE = 0.2

        /**
         * Mức suy luận thấp nhất mà model dòng Gemini 3 **chấp nhận**.
         *
         * Gemini 3 mặc định `high` và mức đó tốn hàng chục giây cho một ảnh; `"minimal"` bị từ chối
         * (`400 INVALID_ARGUMENT`), còn `"low"` thì hợp lệ — đây là đòn giảm độ trễ chính của lớp này.
         */
        private const val THINKING_LEVEL = "low"

        /** Hai mẩu trong thông báo lỗi để biết model **từ chối** `thinkingConfig` (xem hàm cùng tên). */
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

        /**
         * Nghỉ giữa hai **model** (không phải giữa hai lần thử cùng model — chỗ đó đã bỏ): đủ ngắn để
         * người dùng không thấy "đứng hình", đủ dài để không dồn dập vào lúc Google đang quá tải.
         */
        private const val RETRY_DELAY_MS = 600L

        private val RETRYABLE_ERRORS = setOf(
            FoodAnalysisError.BUSY,
            FoodAnalysisError.SERVER,
            FoodAnalysisError.NETWORK
        )

        /**
         * Schema ép model trả đúng JSON cần thiết. `required` **không** gồm `emoji` để model có thể bỏ
         * trống khi không có emoji phù hợp (tầng [FoodAnalysis] sẽ thay bằng emoji mặc định).
         */
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
