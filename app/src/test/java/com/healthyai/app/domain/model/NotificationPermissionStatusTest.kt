package com.healthyai.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cho [NotificationPermissionStatus.from] — phần quyết định "có hiện thẻ **Bật nhắc nhở hàng
 * ngày** hay không, và thẻ có nút nào".
 *
 * Ba lối vào người dùng gặp (KB-36/37/38) đều nằm ở đây, vì `NotificationPermissionRepositoryImpl`
 * đọc `Build`/`Context` nên **không** chạy được trong unit test JVM:
 *
 * - **KB-36** — Android 13+, app chưa từng xin `POST_NOTIFICATIONS`: thẻ phải **hiện** (nút "Bật thông
 *   báo"). Đây là ca đã vỡ trước khi sửa: `areNotificationsEnabled()` trả `true` khi app-op chưa được
 *   ghi, nên nếu chỉ tin tín hiệu đó thì thẻ không bao giờ hiện.
 * - **KB-37** — từ chối quyền: thẻ vẫn còn để nhắc lại.
 * - **KB-38** — quyền đã cấp nhưng người dùng tắt thông báo trong Settings: thẻ vẫn hiện nhưng **không**
 *   xin lại quyền nữa (nút phải là "Mở cài đặt thông báo").
 *
 * Cộng thêm nhánh Android ≤ 12 (không có quyền runtime) — dễ làm sai nếu so sánh `sdkInt` lệch một đơn
 * vị, nên chốt luôn mốc 32/33.
 */
class NotificationPermissionStatusTest {

    @Test
    fun `android 13 before ever asking shows the card with the request button`() {
        // Máy thật: quyền chưa cấp, nhưng areNotificationsEnabled() lại trả true (chưa có channel nào)
        val status = NotificationPermissionStatus.from(
            sdkInt = 33,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = true
        )

        assertTrue("Thẻ phải hiện để người dùng còn bấm 'Bật thông báo' (KB-36)", status.isBlocked)
        assertTrue("Còn xin được bằng hộp thoại hệ thống", status.canRequestPermission)
    }

    @Test
    fun `android 13 before ever asking shows the card even when the app toggle reads false`() {
        val status = NotificationPermissionStatus.from(
            sdkInt = 34,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = false
        )

        assertTrue(status.isBlocked)
        assertTrue(status.canRequestPermission)
    }

    @Test
    fun `denying the permission keeps the card visible`() {
        // Người dùng bấm "Không cho phép" → quyền vẫn chưa cấp → thẻ phải còn (KB-37)
        val status = NotificationPermissionStatus.from(
            sdkInt = 33,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = true
        )

        assertTrue(status.isBlocked)
        assertTrue(
            "Thẻ còn nhắc lại được vì quyền vẫn chưa cấp",
            status.canRequestPermission
        )
    }

    @Test
    fun `granting the permission hides the card immediately`() {
        // Sau khi bấm Cho phép: quyền đã cấp + thông báo cấp app đang bật → thẻ biến mất (KB-36)
        val status = NotificationPermissionStatus.from(
            sdkInt = 33,
            postNotificationsPermissionGranted = true,
            appNotificationsEnabled = true
        )

        assertFalse("Đã cấp quyền thì không còn gì để nhắc", status.isBlocked)
        assertFalse(status.canRequestPermission)
    }

    @Test
    fun `notifications turned off in settings keeps the card without asking for permission again`() {
        // KB-38: quyền đã cấp, người dùng tắt công tắc thông báo của app trong Settings
        val status = NotificationPermissionStatus.from(
            sdkInt = 33,
            postNotificationsPermissionGranted = true,
            appNotificationsEnabled = false
        )

        assertTrue("Thẻ 'Thông báo đang bị tắt' phải hiện", status.isBlocked)
        assertFalse(
            "Quyền đã cấp nên xin lại bằng hộp thoại vô nghĩa — phải mở Settings",
            status.canRequestPermission
        )
    }

    @Test
    fun `android 12 without the runtime permission is not blocked`() {
        // API 32 < 33: chưa có quyền runtime để xin → chỉ công tắc cấp app quyết định
        val status = NotificationPermissionStatus.from(
            sdkInt = 32,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = true
        )

        assertFalse("Máy cũ không có quyền runtime nên không được coi là bị chặn", status.isBlocked)
        assertFalse(status.canRequestPermission)
    }

    @Test
    fun `android 12 with notifications switched off shows the card pointing at settings`() {
        val status = NotificationPermissionStatus.from(
            sdkInt = 31,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = false
        )

        assertTrue(status.isBlocked)
        assertFalse(status.canRequestPermission)
    }

    @Test
    fun `api 33 is the first version that needs the runtime permission`() {
        val justBelow = NotificationPermissionStatus.from(
            sdkInt = 32,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = false
        )
        val exactly = NotificationPermissionStatus.from(
            sdkInt = 33,
            postNotificationsPermissionGranted = false,
            appNotificationsEnabled = false
        )

        assertFalse("API 32 chưa cần quyền runtime nên chỉ công tắc cấp app quyết định", justBelow.canRequestPermission)
        assertTrue("API 33 là bản đầu tiên xin được bằng hộp thoại", exactly.canRequestPermission)
        assertEquals(justBelow.isBlocked, exactly.isBlocked)
    }
}
