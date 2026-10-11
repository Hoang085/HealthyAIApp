package com.healthyai.app.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.healthyai.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test cho [AuthGateViewModel] — "cổng xác thực" quyết định màn hình gốc của app.
 *
 * Trước bản sửa lỗi mục 3 #19, app **không có UI đăng xuất**: sau khi đăng nhập (hoặc bấm
 * "Bỏ qua") người dùng chỉ còn cách xoá dữ liệu app để quay lại màn đăng nhập. Test chốt lại
 * hành vi của [AuthGateViewModel.signOut] — xem **TC-AUTH-16**:
 * 1. chưa có phiên → `SignedOut` (màn đăng nhập);
 * 2. có phiên Firebase → `SignedIn` (vào thẳng app);
 * 3. bấm "Bỏ qua" → `Guest`;
 * 4. **đăng xuất ở chế độ khách** → xoá cờ khách → `SignedOut`;
 * 5. **đăng xuất khi đã đăng nhập** → gọi `AuthRepository.signOut()` → `SignedOut`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `chưa có phiên nào thì hiện màn đăng nhập`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = AuthGateViewModel(FakeAuthRepository(), SavedStateHandle())

        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `đã có phiên Firebase thì vào thẳng app`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = AuthGateViewModel(FakeAuthRepository(userId = "uid-1"), SavedStateHandle())

        advanceUntilIdle()

        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)
    }

    @Test
    fun `bấm bỏ qua thì vào chế độ khách`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = AuthGateViewModel(FakeAuthRepository(), SavedStateHandle())

        viewModel.continueAsGuest()
        advanceUntilIdle()

        assertEquals(SessionState.Guest, viewModel.sessionState.value)
    }

    @Test
    fun `đăng xuất khi đang ở chế độ khách thì quay lại màn đăng nhập`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAuthRepository()
        val viewModel = AuthGateViewModel(repository, SavedStateHandle())

        viewModel.continueAsGuest()
        advanceUntilIdle()
        assertEquals(SessionState.Guest, viewModel.sessionState.value)

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(
            "cờ chế độ khách phải bị xoá, nếu không màn đăng nhập không hiện lại",
            SessionState.SignedOut,
            viewModel.sessionState.value
        )
    }

    @Test
    fun `đăng xuất khi đã đăng nhập thì xoá phiên Firebase và quay lại màn đăng nhập`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAuthRepository(userId = "uid-1")
        val viewModel = AuthGateViewModel(repository, SavedStateHandle())

        advanceUntilIdle()
        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(1, repository.signOutCount)
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `đăng nhập lại sau khi đăng xuất vẫn vào được app`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAuthRepository(userId = "uid-1")
        val viewModel = AuthGateViewModel(repository, SavedStateHandle())

        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)

        repository.setUserId("uid-1")
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)
    }

    /** Repository giả trong bộ nhớ: không cần Firebase/Android. */
    private class FakeAuthRepository(
        userId: String? = null,
        /** Tên hiển thị của phiên (phụ lục C #30) — chỉ dùng cho màn Trang chủ. */
        private val userName: String? = null
    ) : AuthRepository {

        private val state = MutableStateFlow(userId)

        var signOutCount = 0
            private set

        override val currentUserId: String?
            get() = state.value

        override val authState: Flow<String?> = state

        override val currentUserName: String?
            get() = userName

        override suspend fun signIn(email: String, password: String): Result<String> =
            Result.success("uid-fake")

        override suspend fun signUp(email: String, password: String): Result<String> =
            Result.success("uid-fake")

        override suspend fun signOut(): Result<Unit> {
            signOutCount++
            state.value = null
            return Result.success(Unit)
        }

        /** Mô phỏng đăng nhập thành công ở tầng Firebase (phát uid mới). */
        fun setUserId(userId: String?) {
            state.value = userId
        }
    }
}
