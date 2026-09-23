package com.healthyai.app.domain.model

/**
 * Trạng thái cho phép hiển thị thông báo (nhắc nhở hàng ngày) của app.
 *
 * @property notificationsEnabled App có được phép hiện thông báo không
 *   (`NotificationManagerCompat.areNotificationsEnabled()`).
 * @property canRequestPermission Còn có thể xin lại quyền `POST_NOTIFICATIONS` bằng hộp thoại hệ thống
 *   (Android 13+ và chưa từng được cấp) — nếu `false` thì chỉ còn cách mở Settings của app.
 */
data class NotificationPermissionStatus(
    val notificationsEnabled: Boolean = true,
    val canRequestPermission: Boolean = false
) {
    /** Thông báo đang bị chặn → UI cần hiện thẻ nhắc người dùng bật lại. */
    val isBlocked: Boolean
        get() = !notificationsEnabled
}
