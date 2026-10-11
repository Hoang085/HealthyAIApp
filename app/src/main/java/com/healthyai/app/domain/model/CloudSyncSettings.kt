package com.healthyai.app.domain.model

/**
 * Trạng thái **đồng bộ nhật ký thói quen lên đám mây** (Firestore) — mặc định **TẮT**.
 *
 * Đây là thiết lập **phải do người dùng bật** (opt-in, mục 3 #31): app **chỉ** tải lên khi
 * [enabled] = `true`, và **chỉ** tải những log được tạo **từ** [consentAtMillis] trở đi — nhật ký đã
 * ghi trước đó (ví dụ ghi ở chế độ khách) **không** bị đẩy lên đám mây.
 *
 * @property enabled Người dùng đã bật đồng bộ đám mây hay chưa.
 * @property consentAtMillis Thời điểm (epoch millis) người dùng **bật** đồng bộ lần gần nhất; `null`
 *   khi chưa từng bật. Là **mốc chặn**: chỉ log có `loggedAt >= consentAtMillis` mới được tải lên.
 */
data class CloudSyncSettings(
    val enabled: Boolean = false,
    val consentAtMillis: Long? = null
)
