package com.healthyai.app.data.network

/** Kết quả một lượt POST: mã HTTP và phần thân trả về (đã đọc thành chuỗi). */
data class HttpPostResponse(val code: Int, val body: String)

/** Cổng gọi HTTP POST ra ngoài (dùng cho Gemini); tách interface để unit test JVM không cần mạng và dễ thay bằng OkHttp/proxy sau này. */
interface HttpPost {

    /** Gửi POST JSON (khoá đặt trong header để không lọt vào log/URL); trả null khi không kết nối được, tách khỏi "server trả lỗi". */
    suspend fun post(url: String, headers: Map<String, String>, body: String): HttpPostResponse?
}
