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

/** Trạng thái phiên đăng nhập ở cấp điều hướng, quyết định màn hình gốc của app. */
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

/** Quan sát AuthRepository.authState để bật/tắt màn đăng nhập; cờ chế độ khách lưu trong SavedStateHandle. */
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

    /** Đăng xuất: xoá phiên Firebase và cờ chế độ khách, nhưng không xoá dữ liệu cục bộ trong Room. */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // Xoá cờ khách kể cả khi không có phiên Firebase; nếu không combine vẫn trả Guest và màn đăng nhập không hiện.
            savedStateHandle[KEY_GUEST_MODE] = false
        }
    }

    private companion object {
        const val KEY_GUEST_MODE = "auth_gate_guest_mode"
    }
}
