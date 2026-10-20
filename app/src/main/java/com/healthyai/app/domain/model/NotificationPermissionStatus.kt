package com.healthyai.app.domain.model

/** Trạng thái cho phép hiển thị thông báo (nhắc nhở hàng ngày) của app. */
data class NotificationPermissionStatus(
    val notificationsEnabled: Boolean = true,
    val canRequestPermission: Boolean = false
) {
    /** Thông báo đang bị chặn → UI cần hiện thẻ nhắc người dùng bật lại. */
    val isBlocked: Boolean
        get() = !notificationsEnabled

    companion object {
        /** Android 13 (API 33) — bản Android đầu tiên có quyền runtime `POST_NOTIFICATIONS`. */
        private const val ANDROID_13_API = 33

        /** Suy ra trạng thái từ các tín hiệu thô của hệ thống; hàm thuần nên test được trên JVM. */
        fun from(
            sdkInt: Int,
            postNotificationsPermissionGranted: Boolean,
            appNotificationsEnabled: Boolean
        ): NotificationPermissionStatus {
            val requiresRuntimePermission = sdkInt >= ANDROID_13_API

            // Android ≤ 12 không có quyền runtime → coi như đã có, chỉ còn công tắc cấp app.
            val permissionGranted = !requiresRuntimePermission || postNotificationsPermissionGranted

            return NotificationPermissionStatus(
                notificationsEnabled = permissionGranted && appNotificationsEnabled,
                canRequestPermission = requiresRuntimePermission && !permissionGranted
            )
        }
    }
}
