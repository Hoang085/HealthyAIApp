package com.healthyai.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test cho [NotificationPermissionStatus.from] — khi nào hiện thẻ "Bật nhắc nhở hàng ngày" và thẻ có nút nào. */
class NotificationPermissionStatusTest {

    @Test
    fun `android 13 before ever asking shows the card with the request button`() {
        // Quyền chưa cấp nhưng areNotificationsEnabled() trả true (chưa có channel nào)
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
        // Bấm "Không cho phép": quyền chưa cấp nên thẻ vẫn còn để nhắc lại
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
        // Bấm Cho phép: quyền đã cấp và thông báo cấp app đang bật → thẻ biến mất
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
        // Quyền đã cấp nhưng người dùng tắt công tắc thông báo của app trong Settings
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
