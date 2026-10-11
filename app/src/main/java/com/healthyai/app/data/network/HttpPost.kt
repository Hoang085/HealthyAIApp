package com.healthyai.app.data.network

/** Kết quả một lượt POST: mã HTTP và phần thân trả về (đã đọc thành chuỗi). */
data class HttpPostResponse(val code: Int, val body: String)

/**
 * Cổng gọi **HTTP POST** ra ngoài (hiện dùng cho Gemini — mục 3 #42).
 *
 * Tách thành interface vì hai lý do đã thành nếp trong repo này (xem `ImageLabelSource` cũ,
 * `StepCounterSensor`, `SleepSensor`):
 * - tầng gọi (`GeminiFoodInsightSource`) không phụ thuộc API Android nên **unit test JVM chạy được
 *   toàn bộ luật ánh xạ mã lỗi / retry / parse** mà không cần mạng;
 * - sau này muốn đổi sang OkHttp/Retrofit hay một proxy backend thì chỉ thay một lớp.
 *
 * (Cố ý **không** khai `fun interface`: phương thức là `suspend`, khai interface thường để nơi dùng
 * luôn viết `object : HttpPost { … }` — rõ ràng hơn và không phụ thuộc vào SAM conversion cho hàm
 * `suspend`.)
 */
interface HttpPost {

    /**
     * Gửi một request POST JSON.
     *
     * @param headers header gửi kèm (ví dụ `x-goog-api-key`). Đặt khoá trong **header** chứ không nhét
     *   vào query string để khoá không lọt vào log/URL.
     * @return `null` khi **không kết nối được** (mất mạng, DNS, timeout) — tách khỏi "server trả lỗi"
     *   vì hai trường hợp này cần thông báo khác nhau cho người dùng.
     */
    suspend fun post(url: String, headers: Map<String, String>, body: String): HttpPostResponse?
}
