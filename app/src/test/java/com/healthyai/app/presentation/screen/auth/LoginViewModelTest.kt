package com.healthyai.app.presentation.screen.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.healthyai.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit test màn hình đăng nhập: validate trước khi gọi Firebase, dịch lỗi, chống double-submit, hỗ trợ Đăng nhập/Đăng ký. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val authRepository = mockk<AuthRepository>()

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank email is rejected before calling firebase`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Vui lòng nhập email", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `malformed email is rejected before calling firebase`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("alex.healthyai.app")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Email không hợp lệ", viewModel.uiState.value.error)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `password shorter than six characters is rejected`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("alex@healthyai.app")
        viewModel.onPasswordChange("123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", viewModel.uiState.value.error)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `sign in uses the trimmed email and clears error on success`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { authRepository.signIn("alex@healthyai.app", "123456") } returns Result.success("uid-1")
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("  alex@healthyai.app  ")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.signIn("alex@healthyai.app", "123456") }
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `register mode creates a new account instead of signing in`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { authRepository.signUp("alex@healthyai.app", "123456") } returns Result.success("uid-2")
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("alex@healthyai.app")
        viewModel.toggleMode() // Đổi sang chế độ Đăng ký trước khi gõ mật khẩu (toggleMode xoá mật khẩu)
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRegisterMode)
        coVerify(exactly = 1) { authRepository.signUp("alex@healthyai.app", "123456") }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `toggle mode switches label state and clears the typed password`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        viewModel.onPasswordChange("123456")
        viewModel.toggleMode()

        assertTrue(viewModel.uiState.value.isRegisterMode)
        assertEquals("", viewModel.uiState.value.password)

        viewModel.toggleMode()

        assertFalse(viewModel.uiState.value.isRegisterMode)
    }

    @Test
    fun `password visibility can be toggled`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun `wrong credentials are reported in vietnamese`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { authRepository.signIn(any(), any()) } returns
            Result.failure(mockk<FirebaseAuthInvalidCredentialsException>())
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("alex@healthyai.app")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Email hoặc mật khẩu không đúng", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `second submit is ignored while the first request is still running`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        coEvery { authRepository.signIn(any(), any()) } coAnswers {
            gate.await()
            Result.success("uid-1")
        }
        val viewModel = LoginViewModel(authRepository)

        viewModel.onEmailChange("alex@healthyai.app")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle() // Coroutine đầu đang chờ `gate` → isSubmitting = true

        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.submit() // Lần bấm thứ hai phải bị chặn
        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.signIn("alex@healthyai.app", "123456") }
    }

    @Test
    fun `typing again clears the previous error message`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(authRepository)

        viewModel.submit()
        advanceUntilIdle()
        assertEquals("Vui lòng nhập email", viewModel.uiState.value.error)

        viewModel.onEmailChange("alex@healthyai.app")

        assertNull(viewModel.uiState.value.error)
    }

    // Dịch lỗi Firebase Auth
    // Firebase exception tạo bằng MockK vì constructor thật gọi `android.text.TextUtils` (không có trên JVM).

    @Test
    fun `weak password error is not confused with wrong credentials`() {
        val error = mockk<FirebaseAuthWeakPasswordException>()

        assertEquals("Mật khẩu quá yếu, cần ít nhất 6 ký tự", mapAuthError(error))
    }

    @Test
    fun `duplicated email error suggests signing in`() {
        val error = mockk<FirebaseAuthUserCollisionException>()

        assertEquals("Email này đã được đăng ký, hãy đăng nhập", mapAuthError(error))
    }

    @Test
    fun `network failure is reported as a connection problem`() {
        val error = mockk<FirebaseNetworkException>()

        assertEquals("Không có kết nối mạng, vui lòng thử lại", mapAuthError(error))
    }

    @Test
    fun `email password provider not enabled points to firebase console`() {
        val error = mockk<FirebaseAuthException> {
            every { errorCode } returns "ERROR_OPERATION_NOT_ALLOWED"
        }

        assertEquals(
            "Phương thức Email/Mật khẩu chưa được bật trên Firebase Console",
            mapAuthError(error)
        )
    }

    @Test
    fun `unknown error keeps a readable fallback message`() {
        assertEquals("Đăng nhập thất bại: boom", mapAuthError(IllegalStateException("boom")))
    }
}
