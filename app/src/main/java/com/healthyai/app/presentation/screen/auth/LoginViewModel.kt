package com.healthyai.app.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.healthyai.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Độ dài mật khẩu tối thiểu mà Firebase Auth yêu cầu. */
private const val MIN_PASSWORD_LENGTH = 6

/** Regex đơn giản để bắt lỗi gõ thiếu `@` / tên miền trước khi gọi Firebase. */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    /** `true` = đang ở chế độ "Đăng ký", `false` = "Đăng nhập". */
    val isRegisterMode: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    /** Bật nút gửi khi đã nhập đủ email + mật khẩu và không đang gửi request. */
    val canSubmit: Boolean
        get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value.trim(), error = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, error = null) }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    /** Đổi giữa "Đăng nhập" và "Đăng ký"; xoá lỗi cũ và mật khẩu đã gõ. */
    fun toggleMode() = _uiState.update {
        it.copy(isRegisterMode = !it.isRegisterMode, password = "", error = null)
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** Gửi đăng nhập / đăng ký; không cần điều hướng vì AuthGateViewModel theo dõi authState. */
    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return // Chống double-submit

        validate(state)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val result = if (state.isRegisterMode) {
                authRepository.signUp(state.email, state.password)
            } else {
                authRepository.signIn(state.email, state.password)
            }

            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    error = result.exceptionOrNull()?.let(::mapAuthError)
                )
            }
        }
    }

    /** Trả về thông báo lỗi nếu dữ liệu nhập chưa hợp lệ, `null` nếu hợp lệ. */
    private fun validate(state: LoginUiState): String? = when {
        state.email.isBlank() -> "Vui lòng nhập email"
        !EMAIL_REGEX.matches(state.email) -> "Email không hợp lệ"
        state.password.isBlank() -> "Vui lòng nhập mật khẩu"
        state.password.length < MIN_PASSWORD_LENGTH ->
            "Mật khẩu phải có ít nhất $MIN_PASSWORD_LENGTH ký tự"
        else -> null
    }
}

/** Dịch lỗi Firebase Auth sang tiếng Việt; lỗi lạ giữ nguyên mô tả gốc để debug. */
internal fun mapAuthError(throwable: Throwable): String = when (throwable) {
    // FirebaseAuthWeakPasswordException là lớp con của InvalidCredentialsException → phải xét trước.
    is FirebaseAuthWeakPasswordException ->
        "Mật khẩu quá yếu, cần ít nhất $MIN_PASSWORD_LENGTH ký tự"
    is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không đúng"
    is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị vô hiệu hoá"
    is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký, hãy đăng nhập"
    is FirebaseNetworkException -> "Không có kết nối mạng, vui lòng thử lại"
    is FirebaseAuthException -> when (throwable.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Phương thức Email/Mật khẩu chưa được bật trên Firebase Console"
        "ERROR_TOO_MANY_REQUESTS" -> "Bạn đã thử quá nhiều lần, vui lòng đợi rồi thử lại"
        else -> "Xác thực thất bại (${throwable.errorCode})"
    }
    else -> "Đăng nhập thất bại: ${throwable.message ?: throwable.javaClass.simpleName}"
}
