package com.healthyai.app.domain.model

/**
 * Trạng thái cho phép hiển thị thông báo (nhắc nhở hàng ngày) của app.
 *
 * @property notificationsEnabled App **thực sự** được phép hiện thông báo không: đã có quyền
 *   `POST_NOTIFICATIONS` (Android 13+) **và** công tắc thông báo cấp app đang bật
 *   (`NotificationManagerCompat.areNotificationsEnabled()`). Chỉ kiểm công tắc thôi là không đủ trên
 *   Android 13+: app chưa từng xin quyền thì công tắc chưa bị đặt và hàm đó trả `true`.
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

    companion object {
        /** Android 13 (API 33) — bản Android đầu tiên có quyền runtime `POST_NOTIFICATIONS`. */
        private const val ANDROID_13_API = 33

        /**
         * Suy ra trạng thái từ **các tín hiệu thô** của hệ thống.
         *
         * Hàm **thuần** (không đụng `android.os.Build` hay `Context`) vì lớp đọc hệ thống thật —
         * `NotificationPermissionRepositoryImpl` — không chạy được trong unit test JVM. Toàn bộ phần
         * quyết định nằm ở đây để ba lối vào của thẻ "Bật nhắc nhở hàng ngày" (KB-36/37/38) khoá lại
         * được bằng test:
         *
         * 1. **Android 13+, chưa cấp quyền** → `isBlocked = true`, `canRequestPermission = true`
         *    (KB-36: thẻ có nút "Bật thông báo"; KB-37: từ chối thì thẻ vẫn còn).
         * 2. **Đã cấp quyền nhưng tắt thông báo trong Settings** → `isBlocked = true`,
         *    `canRequestPermission = false` (KB-38 → thẻ "Thông báo đang bị tắt" + nút mở Settings).
         * 3. **Android ≤ 12** → không có quyền runtime; chỉ công tắc cấp app quyết định.
         *
         * @param sdkInt `Build.VERSION.SDK_INT` — nhận thẳng số để tầng domain không phụ thuộc framework.
         * @param postNotificationsPermissionGranted quyền runtime `POST_NOTIFICATIONS` đã được cấp chưa.
         *   Trên Android ≤ 12 quyền này không tồn tại nên giá trị truyền vào được **bỏ qua**.
         * @param appNotificationsEnabled công tắc thông báo cấp app
         *   (`NotificationManagerCompat.areNotificationsEnabled()`).
         */
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
