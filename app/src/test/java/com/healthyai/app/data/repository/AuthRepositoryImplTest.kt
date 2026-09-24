package com.healthyai.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [AuthRepositoryImpl.signOut] (mục 3 #19).
 *
 * Cần có `signOut()` ở tầng repository vì trước đây app **không có UI đăng xuất**: `AuthGateViewModel`
 * chỉ chuyển về màn đăng nhập khi `authState` phát `null`, mà điều đó chỉ xảy ra nếu phiên Firebase
 * thực sự bị xoá — xem **TC-AUTH-15/16**.
 */
class AuthRepositoryImplTest {

    @Test
    fun `signOut uỷ quyền cho FirebaseAuth và trả về thành công`() = runTest {
        val auth = mockk<FirebaseAuth>(relaxed = true)
        val repository = AuthRepositoryImpl(auth)

        val result = repository.signOut()

        assertTrue("signOut phải thành công", result.isSuccess)
        verify(exactly = 1) { auth.signOut() }
    }

    // ── Tên hiển thị trên lời chào (phụ lục C #30) ─────────────

    @Test
    fun `currentUserName dùng displayName khi có`() {
        val repository = AuthRepositoryImpl(
            firebaseAuthWith(displayName = "Nguyễn Văn A", email = "nguyenvana@example.com")
        )

        assertEquals("Nguyễn Văn A", repository.currentUserName)
    }

    @Test
    fun `currentUserName lấy phần trước a-còng của email khi displayName trống`() {
        // Tài khoản tạo trong app không đặt displayName (null) → dùng phần trước '@'
        val nullDisplayName = AuthRepositoryImpl(
            firebaseAuthWith(displayName = null, email = "alex.healthyai@gmail.com")
        )
        assertEquals("alex.healthyai", nullDisplayName.currentUserName)

        val blankDisplayName = AuthRepositoryImpl(
            firebaseAuthWith(displayName = "   ", email = "alex.healthyai@gmail.com")
        )
        assertEquals("alex.healthyai", blankDisplayName.currentUserName)
    }

    @Test
    fun `currentUserName là null khi chưa đăng nhập`() {
        val repository = AuthRepositoryImpl(mockk<FirebaseAuth>(relaxed = true))

        assertNull("Chế độ khách không có tên hiển thị", repository.currentUserName)
    }

    /** `FirebaseAuth` giả có đúng 1 người dùng với [displayName]/[email] cho trước. */
    private fun firebaseAuthWith(displayName: String?, email: String?): FirebaseAuth {
        val user = mockk<FirebaseUser>(relaxed = true)
        every { user.displayName } returns displayName
        every { user.email } returns email
        val auth = mockk<FirebaseAuth>(relaxed = true)
        every { auth.currentUser } returns user
        return auth
    }
}
