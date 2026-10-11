package com.healthyai.app.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * [HttpPost] thật, dùng `HttpURLConnection` của JDK.
 *
 * Vì sao **không** thêm OkHttp: yêu cầu ở đây chỉ là một POST JSON kèm ảnh base64, trong khi build của
 * dự án chạy **offline** (dependency phải có sẵn trong cache). `HttpURLConnection` không cần thêm thư
 * viện nào — cùng lý do đã áp dụng cho `GalleryImageLoader` (dùng API ảnh có sẵn của Android).
 *
 * Chạy trên [Dispatchers.IO] vì đây là I/O mạng (gọi từ `viewModelScope` = Main thread sẽ ném
 * `NetworkOnMainThreadException`).
 *
 * **Không bao giờ ném ra ngoài**: mọi lỗi kết nối (mất mạng, DNS, timeout, TLS) trả `null` để tầng
 * trên phân biệt "không gọi được" với "server trả lỗi"; riêng [CancellationException] được **ném
 * tiếp** đúng quy ước của repo (phụ lục C #39).
 */
class HttpUrlConnectionPost @Inject constructor() : HttpPost {

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String
    ): HttpPostResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
            }

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            // Lỗi HTTP nằm ở `errorStream` (không phải `inputStream`) — vẫn đọc để biết lý do thật.
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            HttpPostResponse(code, text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mất mạng/DNS/timeout: không phải lỗi lập trình, màn hình đã có nhánh báo riêng.
            null
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        /** Ảnh gửi kèm nên phản hồi có thể lâu hơn request thường. */
        const val READ_TIMEOUT_MS = 60_000
    }
}
