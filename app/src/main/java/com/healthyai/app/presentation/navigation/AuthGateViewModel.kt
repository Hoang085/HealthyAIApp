package com.healthyai.app.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Trạng thái phiên đăng nhập ở cấp điều hướng, quyết định màn hình gốc của app.
 */
sealed interface SessionState {
    /** Đang chờ FirebaseAuth trả về trạng thái đã lưu (tránh nháy màn login). */
    data object Loading : SessionState

    /** Chưa đăng nhập và chưa chọn chế độ khách → hiện màn đăng nhập. */
    data object SignedOut : SessionState

    /** Người dùng bấm "Bỏ qua" → dùng app không cần tài khoản. */
    data object Guest : SessionState

    /** Đã đăng nhập → vào thẳng app. */
    data object SignedIn : SessionState
}

/**
 * Quan sát `AuthRepository.authState` để bật/tắt màn hình đăng nhập.
 *
 * Cờ "chế độ khách" được lưu trong [SavedStateHandle] nên sống sót qua xoay màn hình
 * và cả khi tiến trình bị hệ thống thu hồi rồi khôi phục.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = combine(
        authRepository.authState,
        savedStateHandle.getStateFlow(KEY_GUEST_MODE, false)
    ) { userId, isGuest ->
        when {
            userId != null -> SessionState.SignedIn
            isGuest -> SessionState.Guest
            else -> SessionState.SignedOut
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionState.Loading
    )

    fun continueAsGuest() {
        savedStateHandle[KEY_GUEST_MODE] = true
    }

    /**
     * Đăng xuất (mục 3 #19): xoá phiên Firebase **và** cờ chế độ khách → cổng xác thực trả về
     * [SessionState.SignedOut] nên màn đăng nhập hiện lại.
     *
     * **Không** xoá dữ liệu cục bộ: thói quen/nhật ký vẫn nằm trong Room (SQLCipher) trên máy, và
     * đăng nhập lại bằng tài khoản cũ sẽ thấy nguyên dữ liệu.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // Phải xoá cờ khách **kể cả** khi không có phiên Firebase (người dùng đang ở chế độ
            // khách): nếu không `combine` vẫn trả về Guest và màn đăng nhập không bao giờ hiện ra.
            savedStateHandle[KEY_GUEST_MODE] = false
        }
    }

    private companion object {
        const val KEY_GUEST_MODE = "auth_gate_guest_mode"
    }
}
