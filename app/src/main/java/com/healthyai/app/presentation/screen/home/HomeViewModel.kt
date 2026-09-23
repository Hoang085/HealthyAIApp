package com.healthyai.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.health.GetHealthConnectPermissionStatusUseCase
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.notification.GetNotificationPermissionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Một thói quen hôm nay kèm trạng thái đã đạt mục tiêu hay chưa. */
data class HomeHabitItem(
    val habit: Habit,
    val isCompleted: Boolean
)

data class HomeUiState(
    val habits: List<HomeHabitItem> = emptyList(),
    val healthData: HealthData = HealthData(),
    val healthConnect: HealthConnectPermissionStatus = HealthConnectPermissionStatus(),
    val notificationPermission: NotificationPermissionStatus = NotificationPermissionStatus(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /**
     * Có nên hiện thẻ hướng dẫn Health Connect không: thiếu quyền đọc, chưa cài,
     * hoặc cần cập nhật ứng dụng Health Connect.
     */
    val showHealthConnectCard: Boolean
        get() = healthConnect.needsPermission ||
            healthConnect.status == HealthConnectStatus.NOT_INSTALLED ||
            healthConnect.status == HealthConnectStatus.UPDATE_REQUIRED

    /**
     * Có nên hiện thẻ "Bật nhắc nhở hàng ngày" không — `ReminderWorker` chỉ gửi được thông báo
     * khi quyền thông báo đang bật.
     */
    val showReminderNotificationCard: Boolean
        get() = notificationPermission.isBlocked
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val getHealthConnectPermissionStatusUseCase: GetHealthConnectPermissionStatusUseCase,
    private val getNotificationPermissionStatusUseCase: GetNotificationPermissionStatusUseCase,
    private val habitRepository: HabitRepository // Cần lấy logs để tính trạng thái hoàn thành
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
        loadHealthConnect(showLoading = true)
        loadNotificationPermission()
    }

    private fun observeHabits() {
        combine(
            getTodayHabitsUseCase(),
            habitRepository.getHabitLogsForPeriod(0) // 0 ngày trước = hôm nay
        ) { habits, todayLogs ->
            habits.map { habit ->
                val totalLogged = todayLogs.asSequence()
                    .filter { it.habitId == habit.id }
                    .sumOf { it.value.toDouble() }
                    .toFloat()

                HomeHabitItem(
                    habit = habit,
                    isCompleted = totalLogged >= habit.targetValue
                )
            }
        }.onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        .onEach { habits -> _uiState.update { it.copy(habits = habits, isLoading = false) } }
        .launchIn(viewModelScope)
    }

    /**
     * Kiểm tra trạng thái Health Connect rồi nạp số liệu sức khỏe.
     *
     * Ngoài lần nạp đầu, số liệu chỉ được đọc lại khi trạng thái/quyền **thay đổi** (hoặc khi
     * [forceDataRead] = `true`) — nhờ vậy việc kiểm tra lại quyền khi quay về màn Home không
     * kéo theo 4 truy vấn Health Connect mỗi lần.
     *
     * - Health Connect chưa cài / cần cập nhật → UI hiện thẻ hướng dẫn thay cho banner lỗi đỏ.
     * - Health Connect sẵn sàng, chưa cấp quyền → UI hiện thẻ "Cấp quyền Health Connect"
     *   (các chỉ số đọc được trả về 0, app không crash).
     *
     * @param showLoading chỉ bật cờ loading ở lần nạp đầu để tránh nháy spinner khi kiểm tra lại quyền.
     */
    private fun loadHealthConnect(showLoading: Boolean, forceDataRead: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }

            val previousStatus = _uiState.value.healthConnect
            val status = getHealthConnectPermissionStatusUseCase()
            _uiState.update { it.copy(healthConnect = status, error = null) }

            if (status.status != HealthConnectStatus.AVAILABLE) {
                // Không có Health Connect để đọc: xoá số liệu cũ và để thẻ hướng dẫn giải thích lý do.
                _uiState.update { it.copy(healthData = HealthData(), isLoading = false) }
                return@launch
            }

            if (showLoading || forceDataRead || status != previousStatus) {
                readHealthData()
            }
        }
    }

    private suspend fun readHealthData() {
        getHealthDataUseCase()
            .onSuccess { data ->
                _uiState.update {
                    it.copy(
                        healthData = data,
                        isLoading = false
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        error = "Không thể đọc dữ liệu sức khỏe: ${e.message}",
                        isLoading = false
                    )
                }
            }
    }

    /**
     * Kiểm tra lại quyền Health Connect — gọi khi màn Home hiện lại, vì người dùng có thể đã cấp
     * quyền trong ứng dụng Health Connect rồi quay lại app.
     */
    fun refreshHealthConnect() = loadHealthConnect(showLoading = false)

    /** Đọc lại trạng thái quyền thông báo (không đụng tới hộp thoại hệ thống). */
    private fun loadNotificationPermission() {
        _uiState.update { it.copy(notificationPermission = getNotificationPermissionStatusUseCase()) }
    }

    /**
     * Kiểm tra lại các quyền/trạng thái hệ thống khi màn Home hiện lại: người dùng có thể đã cấp
     * quyền trong app Health Connect hoặc bật thông báo trong Settings rồi quay lại app.
     */
    fun refreshSystemPermissions() {
        refreshHealthConnect()
        loadNotificationPermission()
    }

    /**
     * Nhận kết quả hộp thoại xin quyền `POST_NOTIFICATIONS`.
     *
     * Không dùng tham số `granted` của launcher mà đọc lại từ nguồn đúng:
     * `NotificationManagerCompat.areNotificationsEnabled()` (phản ánh cả trường hợp người dùng
     * tắt thông báo ở cấp app trong Settings).
     */
    fun onNotificationPermissionResult() = loadNotificationPermission()

    /**
     * Nhận kết quả từ hộp thoại xin quyền của Health Connect.
     *
     * Trạng thái quyền luôn được đọc lại từ Health Connect (nguồn đúng duy nhất) — tham số
     * [granted] chỉ dùng để quyết định có nạp lại số liệu ngay hay không.
     */
    fun onHealthConnectPermissionsResult(granted: Set<String>) {
        val required = _uiState.value.healthConnect.requiredPermissions

        loadHealthConnect(
            showLoading = false,
            forceDataRead = required.isNotEmpty() && granted.containsAll(required)
        )
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}