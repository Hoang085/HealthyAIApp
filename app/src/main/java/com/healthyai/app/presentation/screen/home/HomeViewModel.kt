package com.healthyai.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.health.GetHealthConnectPermissionStatusUseCase
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.notification.GetNotificationPermissionStatusUseCase
import com.healthyai.app.domain.usecase.steps.GetStepCounterStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    /**
     * Tên hiển thị trên lời chào (phụ lục C #30): `displayName` Firebase, nếu trống thì phần trước
     * `@` của email. `null` khi ở **chế độ khách** → màn Trang chủ **không** hiện dòng tên (trước
     * đây luôn hard-code "Alex").
     */
    val userName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Trạng thái cảm biến đếm bước của máy (quyền `ACTIVITY_RECOGNITION` có thể chưa được cấp). */
    val stepCounter: StepCounterStatus = StepCounterStatus(),
    /** Số bước **hôm nay** đọc trực tiếp từ cảm biến của máy (0 khi chưa đủ điều kiện). */
    val sensorSteps: Long = 0L
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

    /**
     * Có nên hiện thẻ xin quyền đếm bước không: máy **có** cảm biến nhưng app **chưa** được cấp quyền.
     * Máy không có cảm biến (ví dụ emulator) thì không hiện gì — không có gì để người dùng làm.
     */
    val showStepCounterCard: Boolean
        get() = stepCounter.sensorAvailable && !stepCounter.permissionGranted

    /**
     * Số bước hiển thị trên thẻ "Tóm tắt sức khỏe": **ưu tiên cảm biến của chính điện thoại** vì đây là
     * nguồn **live** (cập nhật liên tục khi người dùng đi bộ); chỉ dùng Health Connect khi máy **không có
     * cảm biến** hoặc **chưa được cấp quyền** `ACTIVITY_RECOGNITION`.
     *
     * ⚠️ Chiều ưu tiên này là **chủ ý** (đã chốt): `healthData` chỉ được đọc **một lần** lúc mở màn Home
     * (xem `loadHealthConnect`) nên nếu để Health Connect ưu tiên thì con số trên thẻ bị **"đóng băng"** —
     * đi bộ thấy số không đổi cho tới khi ViewModel được tạo lại (mục 3 #24, phụ lục C #33). Đánh đổi:
     * khi máy có cảm biến thì số của **đồng hồ đeo tay** (nằm trong Health Connect) không còn được ưu tiên.
     */
    val displaySteps: Long
        get() = if (stepCounter.isReady) sensorSteps else healthData.steps

    /**
     * Đang hiển thị số bước từ cảm biến của máy (để UI ghi rõ nguồn) — đúng khi cảm biến **đủ điều kiện**
     * đọc, tức khi cảm biến chính là nguồn của [displaySteps].
     */
    val stepsFromSensor: Boolean
        get() = stepCounter.isReady
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val getHealthConnectPermissionStatusUseCase: GetHealthConnectPermissionStatusUseCase,
    private val getNotificationPermissionStatusUseCase: GetNotificationPermissionStatusUseCase,
    private val getStepCounterStatusUseCase: GetStepCounterStatusUseCase,
    private val habitRepository: HabitRepository, // Cần lấy logs để tính trạng thái hoàn thành
    private val authRepository: AuthRepository, // Tên hiển thị trên lời chào (không hard-code)
    private val stepCounterRepository: StepCounterRepository // Đếm bước bằng cảm biến của máy
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Job đang nghe cảm biến đếm bước; huỷ và tạo lại mỗi khi trạng thái quyền thay đổi. */
    private var stepCounterJob: Job? = null

    init {
        observeHabits()
        loadHealthConnect(showLoading = true)
        loadNotificationPermission()
        loadUserName()
        loadStepCounter()
    }

    /**
     * Lấy tên hiển thị thật của phiên hiện tại (`AuthRepository.currentUserName`).
     *
     * Chế độ khách → `null` → UI hiện lời chào **không kèm tên** (phụ lục C #30). Đọc một lần lúc
     * khởi tạo là đủ: đổi phiên (đăng xuất/đăng nhập) làm `AppNavigation` dựng lại màn Home.
     */
    private fun loadUserName() {
        _uiState.update { it.copy(userName = authRepository.currentUserName) }
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
     * quyền trong app Health Connect, bật thông báo trong Settings, hoặc cấp quyền đếm bước chân rồi
     * quay lại app.
     */
    fun refreshSystemPermissions() {
        refreshHealthConnect()
        loadNotificationPermission()
        loadStepCounter()
    }

    // ── Đếm bước bằng cảm biến của máy ─────────────────────────

    /**
     * Đọc lại trạng thái cảm biến đếm bước và (nếu đã đủ điều kiện) bắt đầu nghe cảm biến.
     *
     * Job cũ luôn bị huỷ trước khi tạo job mới để không có hai listener cùng chạy; nếu chưa có quyền
     * hoặc máy không có cảm biến thì số bước đưa về 0 và UI hiện thẻ xin quyền.
     */
    private fun loadStepCounter() {
        val status = getStepCounterStatusUseCase()
        _uiState.update { it.copy(stepCounter = status) }

        stepCounterJob?.cancel()
        if (!status.isReady) {
            _uiState.update { it.copy(sensorSteps = 0L) }
            return
        }

        stepCounterJob = viewModelScope.launch {
            stepCounterRepository.observeTodaySteps().collect { steps ->
                _uiState.update { it.copy(sensorSteps = steps) }
            }
        }
    }

    /**
     * Nhận kết quả hộp thoại xin quyền `ACTIVITY_RECOGNITION`.
     *
     * Trạng thái quyền luôn được đọc lại từ hệ thống (nguồn đúng duy nhất) thay vì tin vào tham số
     * `granted` của launcher — nhờ vậy cả trường hợp người dùng bật quyền trong Settings cũng đúng.
     */
    fun onStepPermissionResult() = loadStepCounter()

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