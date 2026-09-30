package com.healthyai.app.data.vision

import com.google.gson.Gson
import com.healthyai.app.data.network.HttpPost
import com.healthyai.app.data.network.HttpPostResponse
import com.healthyai.app.domain.usecase.food.FoodAnalysis
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [GeminiFoodInsightSource] — phần **không** cần mạng: dựng request, ánh xạ mã HTTP,
 * đọc phản hồi, và luật thử lại.
 *
 * Cách kiểm thử: thay [HttpPost] bằng giả lập nên chạy được trên JVM; `runTest` cũng bỏ qua `delay`
 * giữa hai lần thử lại nên test chạy tức thì.
 */
class GeminiFoodInsightSourceTest {

    private val gson = Gson()

    /** Giả lập HTTP: trả lần lượt các phản hồi trong [responses], đếm số lần bị gọi. */
    private class FakeHttpPost(private val responses: MutableList<HttpPostResponse?>) : HttpPost {
        var calls = 0
            private set
        var lastUrl: String? = null
            private set
        var lastHeaders: Map<String, String> = emptyMap()
            private set
        var lastBody: String? = null
            private set

        /** Mọi URL đã gọi, theo thứ tự — để kiểm tra việc **đổi model**. */
        val urls = mutableListOf<String>()

        override suspend fun post(url: String, headers: Map<String, String>, body: String): HttpPostResponse? {
            calls++
            lastUrl = url
            lastHeaders = headers
            lastBody = body
            urls += url
            return if (responses.isEmpty()) null else responses.removeAt(0)
        }
    }

    private fun source(
        httpPost: HttpPost = FakeHttpPost(mutableListOf()),
        apiKey: String = "test-key",
        model: String = "gemini-3.8-flash",
        fallbackModel: String = "gemini-flash-lite-latest"
    ) = GeminiFoodInsightSource(httpPost, apiKey, model, fallbackModel)

    /** Một phản hồi Gemini báo lỗi 400 vì model không hỗ trợ `thinkingLevel` (đã gặp thật). */
    private fun thinkingRejectedBody(): String =
        """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Thinking level MINIMAL is not supported for this model."}}"""

    /** Bọc nội dung model trả về trong đúng vỏ phản hồi của Gemini. */
    private fun envelope(modelText: String): String = gson.toJson(
        mapOf(
            "candidates" to listOf(
                mapOf("content" to mapOf("parts" to listOf(mapOf("text" to modelText))))
            )
        )
    )

    private fun payload(
        isFood: Boolean = true,
        dishName: String = "Cơm tấm",
        emoji: String = "🍚",
        benefits: List<String> = listOf("Nhiều năng lượng"),
        cautions: List<String> = listOf("Nhiều dầu mỡ")
    ) = gson.toJson(
        mapOf(
            "is_food" to isFood,
            "dish_name" to dishName,
            "emoji" to emoji,
            "benefits" to benefits,
            "cautions" to cautions
        )
    )

    // ── Đọc phản hồi ───────────────────────────────────────────

    @Test
    fun `a valid answer is parsed into the dish with benefits and cautions`() {
        val result = source().parseResponse(envelope(payload()))

        val insight = (result as FoodAnalysisResult.Success).insight
        assertEquals("Cơm tấm", insight.dishName)
        assertEquals("🍚", insight.emoji)
        assertEquals(listOf("Nhiều năng lượng"), insight.benefits)
        assertEquals(listOf("Nhiều dầu mỡ"), insight.cautions)
    }

    @Test
    fun `an answer that says the photo has no food keeps the empty insight`() {
        val result = source().parseResponse(envelope(payload(isFood = false, dishName = "", emoji = "")))

        assertFalse((result as FoodAnalysisResult.Success).insight.hasDish)
    }

    @Test
    fun `a response without candidates is a bad response`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE),
            source().parseResponse("""{"candidates":[]}""")
        )
    }

    @Test
    fun `a response whose text is not json is a bad response`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE),
            source().parseResponse(envelope("Xin chào, đây là món cơm"))
        )
    }

    @Test
    fun `an empty or broken body is a bad response`() {
        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE), source().parseResponse(""))
        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE), source().parseResponse("{{{not json"))
    }

    // ── Ánh xạ mã HTTP ─────────────────────────────────────────

    @Test
    fun `http 200 with a valid body succeeds`() {
        val result = source().mapResponse(HttpPostResponse(200, envelope(payload())))

        assertTrue(result is FoodAnalysisResult.Success)
    }

    @Test
    fun `http 401 and 403 mean a bad api key`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.AUTH),
            source().mapResponse(HttpPostResponse(401, "{}"))
        )
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.AUTH),
            source().mapResponse(HttpPostResponse(403, "{}"))
        )
    }

    @Test
    fun `http 429 and 503 mean the ai is busy and worth retrying`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.BUSY),
            source().mapResponse(HttpPostResponse(429, "{}"))
        )
        assertEquals(
            "Gemini trả 503 khi model đang cao tải (đã gặp thật khi thử khoá)",
            FoodAnalysisResult.Failure(FoodAnalysisError.BUSY),
            source().mapResponse(HttpPostResponse(503, "{}"))
        )
    }

    @Test
    fun `http 500 means a server error`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.SERVER),
            source().mapResponse(HttpPostResponse(500, "{}"))
        )
    }

    @Test
    fun `http 404 means a wrong model name and is not worth retrying`() {
        assertEquals(
            "Google đã bỏ model cũ và trả 404 kèm gợi ý model mới",
            FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE),
            source().mapResponse(HttpPostResponse(404, "{}"))
        )
    }

    @Test
    fun `http 400 is a bad request`() {
        assertEquals(
            FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE),
            source().mapResponse(HttpPostResponse(400, "{}"))
        )
    }

    // ── Dựng request ───────────────────────────────────────────

    @Test
    fun `the request body carries the image, the prompt and a json schema`() {
        val body = source().buildRequestBody("BASE64JPEG")

        assertTrue("Phải gửi ảnh dạng inline_data", body.contains("inline_data"))
        assertTrue("Đúng MIME type của JPEG", body.contains("image/jpeg"))
        assertTrue("Phải kèm ảnh base64 (không có tiền tố data:)", body.contains("BASE64JPEG"))
        assertTrue("Phải kèm prompt", body.contains("chuyên gia dinh dưỡng"))
        assertTrue("Ép đầu ra JSON", body.contains("application/json"))
        assertTrue("Phải khai schema", body.contains("is_food") && body.contains("dish_name"))
        assertTrue("Danh sách lợi ích bị giới hạn số mục", body.contains("maxItems"))
        assertFalse("KHÔNG dùng tiền tố data URI cho ảnh", body.contains("data:image"))
    }

    /**
     * Mục 3 #43: độ trễ đến từ việc model Gemini 3 **mặc định suy luận ở mức `high`** — request phải
     * yêu cầu mức thấp nhất mà model này chấp nhận (`low`; `minimal` bị trả 400).
     */
    @Test
    fun `the request asks for the low thinking level to keep the answer fast`() {
        val body = source().buildRequestBody("BASE64JPEG")

        assertTrue("Thiếu thinkingConfig là quay lại mức 'high' mặc định (rất chậm)", body.contains("thinkingConfig"))
        assertTrue("Phải là mức 'low' — mức thấp nhất model này chấp nhận", body.contains("\"thinkingLevel\":\"low\""))
    }

    /** Model đời cũ (2.5) không hiểu `thinkingConfig` ⇒ phải bỏ được tham số này mà không mất gì khác. */
    @Test
    fun `the thinking config can be left out for a model that rejects it`() {
        val body = source().buildRequestBody("BASE64JPEG", withThinkingConfig = false)

        assertFalse(body.contains("thinkingConfig"))
        assertTrue("Bỏ thinkingConfig vẫn phải giữ ảnh", body.contains("BASE64JPEG"))
        assertTrue("Bỏ thinkingConfig vẫn phải giữ schema", body.contains("responseSchema"))
    }

    @Test
    fun `the api key never appears in the request body`() {
        val body = source(apiKey = "SUPER-SECRET").buildRequestBody("BASE64JPEG")

        assertFalse("Khoá chỉ được gửi ở header, không nằm trong body", body.contains("SUPER-SECRET"))
    }

    @Test
    fun `the request targets the configured model and sends the key in a header`() = runTest {
        val http = FakeHttpPost(mutableListOf(HttpPostResponse(200, envelope(payload()))))

        source(httpPost = http, apiKey = "k", model = "gemini-3.8-flash", fallbackModel = "")
            .analyze(byteArrayOf(1, 2, 3))

        assertEquals(
            "Tên model KHÔNG hardcode — đổi được mà không phải sửa code",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
            http.lastUrl
        )
        assertEquals("k", http.lastHeaders["x-goog-api-key"])
    }

    // ── Model dự phòng (503/429) ───────────────────────────────

    @Test
    fun `a missing api key fails fast without calling the network`() = runTest {
        val http = FakeHttpPost(mutableListOf(HttpPostResponse(200, envelope(payload()))))

        val result = source(httpPost = http, apiKey = "  ").analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.NOT_CONFIGURED), result)
        assertEquals("Không gọi mạng khi chưa có khoá", 0, http.calls)
    }

    @Test
    fun `a busy primary model falls back to the fallback model`() = runTest {
        val http = FakeHttpPost(
            mutableListOf(
                HttpPostResponse(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}"""),
                HttpPostResponse(200, envelope(payload()))
            )
        )

        val result = source(httpPost = http).analyze(byteArrayOf(1))

        assertTrue("Google quá tải model chính thì phải thử model dự phòng", result is FoodAnalysisResult.Success)
        assertEquals(2, http.calls)
        assertTrue("Lần 1 gọi model chính", http.urls[0].contains("gemini-3.8-flash"))
        assertTrue("Lần 2 gọi model dự phòng", http.urls[1].contains("gemini-flash-lite-latest"))
    }

    @Test
    fun `an exhausted quota also falls back to the fallback model`() = runTest {
        val http = FakeHttpPost(
            mutableListOf(
                HttpPostResponse(429, """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED"}}"""),
                HttpPostResponse(200, envelope(payload()))
            )
        )

        assertTrue(source(httpPost = http).analyze(byteArrayOf(1)) is FoodAnalysisResult.Success)
    }

    @Test
    fun `both models busy reports busy without retrying the same model`() = runTest {
        val http = FakeHttpPost(
            mutableListOf(
                HttpPostResponse(503, "{}"),
                HttpPostResponse(503, "{}"),
                HttpPostResponse(200, envelope(payload()))
            )
        )

        val result = source(httpPost = http).analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BUSY), result)
        assertEquals("Mỗi model chỉ gọi MỘT lần — thử lại cùng model vừa quá tải là vô ích", 2, http.calls)
    }

    @Test
    fun `a blank fallback model keeps a single attempt`() = runTest {
        val http = FakeHttpPost(mutableListOf(HttpPostResponse(503, "{}"), HttpPostResponse(200, envelope(payload()))))

        val result = source(httpPost = http, fallbackModel = "  ").analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BUSY), result)
        assertEquals(1, http.calls)
    }

    @Test
    fun `a fallback model identical to the primary one is not called twice`() = runTest {
        val http = FakeHttpPost(mutableListOf(HttpPostResponse(503, "{}"), HttpPostResponse(200, envelope(payload()))))

        val result = source(httpPost = http, model = "gemini-3.8-flash", fallbackModel = "gemini-3.8-flash")
            .analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BUSY), result)
        assertEquals("Trùng tên model thì không gọi lại y nguyên lần nữa", 1, http.calls)
    }

    @Test
    fun `a wrong api key is not retried and does not fall back`() = runTest {
        val http = FakeHttpPost(mutableListOf(HttpPostResponse(401, "{}"), HttpPostResponse(200, envelope(payload()))))

        val result = source(httpPost = http).analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.AUTH), result)
        assertEquals("Sai khoá thì đổi model cũng tốn thời gian vô ích", 1, http.calls)
    }

    @Test
    fun `unreachable network reports a network error`() = runTest {
        // `FakeHttpPost` trả `null` khi hết phản hồi ⇒ mô phỏng "không kết nối được".
        val http = FakeHttpPost(mutableListOf())

        val result = source(httpPost = http).analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.NETWORK), result)
        assertEquals("Mất mạng thì vẫn thử cả model dự phòng", 2, http.calls)
    }

    // ── Model từ chối thinkingConfig ───────────────────────────

    @Test
    fun `a model rejecting the thinking config is sent again without it`() = runTest {
        val http = FakeHttpPost(
            mutableListOf(HttpPostResponse(400, thinkingRejectedBody()), HttpPostResponse(200, envelope(payload())))
        )

        val result = source(httpPost = http, fallbackModel = "").analyze(byteArrayOf(1))

        assertTrue("Model không nhận thinkingLevel vẫn phải phân tích được", result is FoodAnalysisResult.Success)
        assertEquals(2, http.calls)
        assertEquals("Gửi lại ĐÚNG model đó, không đổi model", http.urls[0], http.urls[1])
        assertFalse("Lần gửi lại không kèm thinkingConfig", http.lastBody!!.contains("thinkingConfig"))
    }

    @Test
    fun `a 400 for another reason is not sent again`() = runTest {
        val http = FakeHttpPost(
            mutableListOf(
                HttpPostResponse(
                    400,
                    """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Invalid image data."}}"""
                ),
                HttpPostResponse(200, envelope(payload()))
            )
        )

        val result = source(httpPost = http, fallbackModel = "").analyze(byteArrayOf(1))

        assertEquals(FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE), result)
        assertEquals("400 vì lý do khác thì gửi lại chỉ tốn thời gian", 1, http.calls)
    }

    @Test
    fun `the thinking rejection is recognised from the error message`() {
        assertTrue(source().isThinkingConfigRejected(thinkingRejectedBody()))
        assertFalse(
            source().isThinkingConfigRejected(
                """{"error":{"status":"INVALID_ARGUMENT","message":"Invalid image data."}}"""
            )
        )
        assertFalse("Body rỗng không phải là từ chối thinkingConfig", source().isThinkingConfigRejected(""))
    }

    @Test
    fun `the prompt and the schema stay in sync with the analysis rules`() {
        // Chốt lại ràng buộc giữa hai tầng: prompt nói tối đa bao nhiêu gạch đầu dòng thì schema cũng
        // phải giới hạn đúng như vậy, nếu không model sẽ trả dài rồi bị cắt âm thầm.
        val body = source().buildRequestBody("x")

        assertTrue(
            "Schema phải giới hạn số gạch đầu dòng bằng FoodAnalysis.MAX_BULLETS",
            body.contains("\"maxItems\":${FoodAnalysis.MAX_BULLETS}")
        )
    }
}
