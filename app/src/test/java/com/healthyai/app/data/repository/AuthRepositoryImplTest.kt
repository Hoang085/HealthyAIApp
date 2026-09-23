package com.healthyai.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
}
