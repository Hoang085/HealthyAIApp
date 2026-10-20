package com.healthyai.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HealthConnectDataSettings
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.model.SleepSchedule
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.health.GetHealthConnectPermissionStatusUseCase
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.health.GetLatestRecordedHeartRateUseCase
import com.healthyai.app.domain.usecase.health.GetStoredHealthDataUseCase
import com.healthyai.app.domain.usecase.health.TodayHealthSummary
import com.healthyai.app.domain.usecase.notification.GetNotificationPermissionStatusUseCase
import com.healthyai.app.domain.usecase.steps.GetStepCounterStatusUseCase
import com.healthyai.app.util.runCatchingCancellable
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
    /** Bản chụp sức khỏe hôm nay đã lưu trong app, dùng khi tắt switch Health Connect; rỗng nghĩa là chưa ghi được gì. */
    val storedHealthData: HealthData = HealthData(),
    /** Nguồn dữ liệu thẻ "Tóm tắt sức khỏe": bật = Health Connect, tắt (mặc định) = dữ liệu đã lưu trong app. */
    val healthConnectData: HealthConnectDataSettings = HealthConnectDataSettings(),
    val healthConnect: HealthConnectPermissionStatus = HealthConnectPermissionStatus(),
    val notificationPermission: NotificationPermissionStatus = NotificationPermissionStatus(),
    /** Tên hiển thị trên lời chào: displayName Firebase, nếu trống lấy phần trước @ của email; null ở chế độ khách. */
    val userName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Trạng thái cảm biến đếm bước của máy (quyền `ACTIVITY_RECOGNITION` có thể chưa được cấp). */
    val stepCounter: StepCounterStatus = StepCounterStatus(),
    /** Số bước hôm nay đọc trực tiếp từ cảm biến (0 khi chưa đủ điều kiện). */
    val sensorSteps: Long = 0L,
    /** Số giờ ngủ đêm vừa qua app tự ước lượng từ cảm biến (0 khi chưa đủ dữ liệu). */
    val sensorSleepHours: Double = 0.0,
    /** Nhịp tim ghi trong ngày hôm nay (đo camera hoặc đồng bộ Health Connect); về 0 khi sang ngày mới. */
    val recordedHeartRate: Double = 0.0,
    /** Khung giờ ngủ người dùng đặt; app chỉ đo giấc ngủ trong khung này (mặc định 23:00 – 07:00). */
    val sleepSchedule: SleepSchedule = SleepSchedule.DEFAULT,
    /** Bản nháp đang sửa trong hộp thoại "Khung giờ ngủ"; `null` khi hộp thoại đóng. */
    val sleepScheduleDraft: SleepSchedule? = null,
    /** Lỗi của lần lưu khung giờ ngủ gần nhất (ví dụ hai mốc trùng nhau); `null` khi không có lỗi. */
    val sleepScheduleError: String? = null,
    /** Trạng thái đồng bộ đám mây (mặc định TẮT); thẻ "Đồng bộ đám mây" bật/tắt giá trị này. */
    val cloudSync: CloudSyncSettings = CloudSyncSettings(),
    /** Đã đăng nhập hay đang ở chế độ khách; đồng bộ đám mây chỉ có tác dụng khi có tài khoản (có uid). */
    val isSignedIn: Boolean = false
) {
    /** Hiện thẻ hướng dẫn Health Connect khi thiếu quyền đọc, chưa cài hoặc cần cập nhật. */
    val showHealthConnectCard: Boolean
        get() = healthConnect.needsPermission ||
            healthConnect.status == HealthConnectStatus.NOT_INSTALLED ||
            healthConnect.status == HealthConnectStatus.UPDATE_REQUIRED

    /** Hiện thẻ "Bật nhắc nhở hàng ngày"; ReminderWorker chỉ gửi được thông báo khi quyền thông báo đang bật. */
    val showReminderNotificationCard: Boolean
        get() = notificationPermission.isBlocked

    /** Hiện thẻ xin quyền đếm bước khi máy có cảm biến nhưng app chưa được cấp quyền. */
    val showStepCounterCard: Boolean
        get() = stepCounter.sensorAvailable && !stepCounter.permissionGranted

    /** Nguồn đang chọn cho thẻ "Tóm tắt sức khỏe": switch BẬT = Health Connect, TẮT (mặc định) = bản chụp đã lưu trong app. */
    private val primaryHealthData: HealthData
        get() = if (healthConnectData.enabled) healthData else storedHealthData

    /**
     * Ba chỉ số hiển thị, ghép bằng đúng luật dùng chung với widget
     * ([TodayHealthSummary.build]): nguồn đang chọn trước, không có số mới rơi về số app tự đo.
     */
    private val displaySummary: TodayHealthSummary
        get() = TodayHealthSummary.build(
            primary = primaryHealthData,
            sensorSteps = sensorSteps,
            sensorSleepHours = sensorSleepHours,
            recordedHeartRate = recordedHeartRate
        )

    /** Số bước hiển thị: theo switch (Health Connect hoặc bản chụp đã lưu), không có số thì lấy cảm biến đếm bước. */
    val displaySteps: Long
        get() = displaySummary.steps

    /** Số giờ ngủ hiển thị: theo switch, không có số thì dùng ước lượng từ cảm biến. */
    val displaySleepHours: Double
        get() = displaySummary.sleepHours

    /** Nhịp tim hiển thị: theo switch, không có số thì dùng nhịp tim ghi gần nhất trong ngày; 0 nghĩa là hôm nay chưa có. */
    val displayHeartRate: Double
        get() = displaySummary.heartRateBpm

    /** Hộp thoại "Khung giờ ngủ" đang mở hay không. */
    val isSleepScheduleEditorOpen: Boolean
        get() = sleepScheduleDraft != null
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
    private val stepCounterRepository: StepCounterRepository, // Đếm bước bằng cảm biến của máy
    private val sleepTrackerRepository: SleepTrackerRepository, // Tự ước lượng giờ ngủ bằng cảm biến
    private val settingsRepository: SettingsRepository, // Khung giờ ngủ người dùng đặt + nguồn dữ liệu sức khỏe
    private val getLatestRecordedHeartRateUseCase: GetLatestRecordedHeartRateUseCase, // Nhịp tim đã ghi trên máy
    private val getStoredHealthDataUseCase: GetStoredHealthDataUseCase // Bản chụp sức khỏe hôm nay đã lưu trong app
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
        loadSleepTracking()
        loadRecordedHeartRate()
        observeSleepSchedule()
        observeCloudSync()
        observeHealthConnectData()
        loadSignedInState()
    }

    /** Lấy tên hiển thị của phiên hiện tại; chế độ khách trả null nên lời chào không kèm tên. */
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

    /** Kiểm tra trạng thái Health Connect rồi nạp số liệu theo nguồn đang chọn; chỉ đọc lại khi trạng thái/quyền đổi hoặc forceDataRead. */
    private fun loadHealthConnect(showLoading: Boolean, forceDataRead: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }

            val previousStatus = _uiState.value.healthConnect
            val status = getHealthConnectPermissionStatusUseCase()
            _uiState.update { it.copy(healthConnect = status, error = null) }

            val shouldReadData = showLoading || forceDataRead || status != previousStatus

            if (status.status != HealthConnectStatus.AVAILABLE) {
                // Không có Health Connect để đọc: xoá số liệu cũ và để thẻ hướng dẫn giải thích lý do.
                _uiState.update { it.copy(healthData = HealthData(), isLoading = false) }
                // Nguồn "dữ liệu đã lưu trong app" vẫn đọc được kể cả khi máy không có Health Connect.
                if (shouldReadData) loadStoredHealthData()
                return@launch
            }

            if (shouldReadData) {
                readActiveHealthData()
            }
        }
    }

    /** Đọc số liệu cho nguồn đang chọn; đọc thẳng từ SettingsRepository để không phụ thuộc thời điểm Flow phát giá trị. */
    private suspend fun readActiveHealthData() {
        if (settingsRepository.getHealthConnectData().enabled) {
            readHealthData()
        } else {
            loadStoredHealthData()
        }
    }

    /** Đọc bản chụp sức khỏe hôm nay đã lưu trong app; dùng khi switch Health Connect tắt (lỗi được quy về bản chụp rỗng). */
    private suspend fun loadStoredHealthData() {
        val stored = getStoredHealthDataUseCase()
        _uiState.update { it.copy(storedHealthData = stored, isLoading = false) }
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

    /** Kiểm tra lại quyền Health Connect; gọi khi màn Home hiện lại vì người dùng có thể đã cấp quyền bên ngoài. */
    fun refreshHealthConnect() = loadHealthConnect(showLoading = false)

    /** Đọc lại trạng thái quyền thông báo (không đụng tới hộp thoại hệ thống). */
    private fun loadNotificationPermission() {
        _uiState.update { it.copy(notificationPermission = getNotificationPermissionStatusUseCase()) }
    }

    /** Kiểm tra lại quyền/trạng thái hệ thống và nạp lại số liệu theo nguồn đang chọn khi màn Home hiện lại. */
    fun refreshSystemPermissions() {
        loadHealthConnect(showLoading = false, forceDataRead = true)
        loadNotificationPermission()
        loadStepCounter()
        loadSleepTracking()
        loadRecordedHeartRate()
    }

    /** Đọc nhịp tim ghi trong ngày hôm nay; đọc lại mỗi khi màn Trang chủ hiện ra để ô "Tim" cập nhật ngay sau khi đo. */
    private fun loadRecordedHeartRate() {
        viewModelScope.launch {
            _uiState.update { it.copy(recordedHeartRate = getLatestRecordedHeartRateUseCase()) }
        }
    }

    // Đếm bước bằng cảm biến của máy

    /** Đọc lại trạng thái cảm biến đếm bước và bắt đầu nghe cảm biến nếu đủ điều kiện; huỷ job cũ để không có hai listener. */
    private fun loadStepCounter() {
        val status = getStepCounterStatusUseCase()
        _uiState.update { it.copy(stepCounter = status) }

        if (!status.isReady) {
            stepCounterJob?.cancel()
            stepCounterJob = null
            _uiState.update { it.copy(sensorSteps = 0L) }
            return
        }

        if (stepCounterJob?.isActive == true) return

        stepCounterJob = viewModelScope.launch {
            stepCounterRepository.observeTodaySteps().collect { steps ->
                _uiState.update { it.copy(sensorSteps = steps) }
            }
        }
    }

    /** Nhận kết quả xin quyền ACTIVITY_RECOGNITION; luôn đọc lại trạng thái từ hệ thống thay vì tin tham số granted. */
    fun onStepPermissionResult() = loadStepCounter()

    // Tự ước lượng giờ ngủ bằng cảm biến của máy

    /** Đọc số giờ ngủ đêm vừa qua app tự ước lượng (đọc một lần, không phải cảm biến live); SleepTrackingWorker lấy mẫu mỗi 15 phút. */
    private fun loadSleepTracking() {
        viewModelScope.launch {
            val hours = runCatchingCancellable { sleepTrackerRepository.readLastNightSleepHours() }
                .getOrDefault(0.0)
            _uiState.update { it.copy(sensorSleepHours = hours) }
        }
    }

    // Khung giờ ngủ do người dùng đặt

    /** Theo dõi khung giờ ngủ trong app_settings bằng Flow để luôn khớp giá trị đã lưu; chưa có thì mặc định 23:00 – 07:00. */
    private fun observeSleepSchedule() {
        settingsRepository.observeSleepSchedule()
            .catch { }
            .onEach { schedule -> _uiState.update { it.copy(sleepSchedule = schedule) } }
            .launchIn(viewModelScope)
    }

    // Đồng bộ đám mây (mặc định TẮT)

    /** Theo dõi trạng thái "Đồng bộ đám mây" bằng Flow; lỗi đọc giữ nguyên mặc định TẮT thay vì bật ngoài ý muốn. */
    private fun observeCloudSync() {
        settingsRepository.observeCloudSync()
            .catch { }
            .onEach { cloudSync -> _uiState.update { it.copy(cloudSync = cloudSync) } }
            .launchIn(viewModelScope)
    }

    /** Bật/tắt đồng bộ đám mây; việc ghi mốc đồng ý nằm trong repository nên UI chỉ truyền ý định. */
    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCloudSyncEnabled(enabled) }
    }

    // Nguồn dữ liệu sức khỏe của Trang chủ (mặc định TẮT)

    /** Theo dõi nguồn dữ liệu sức khỏe trong app_settings để switch Trang chủ luôn khớp; lỗi đọc giữ mặc định TẮT. */
    private fun observeHealthConnectData() {
        settingsRepository.observeHealthConnectData()
            .catch { }
            .onEach { settings -> _uiState.update { it.copy(healthConnectData = settings) } }
            .launchIn(viewModelScope)
    }

    /** Bật/tắt switch "Dùng dữ liệu Health Connect"; ghi vào app_settings rồi nạp lại ngay nguồn vừa chọn. */
    fun setHealthConnectDataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHealthConnectDataEnabled(enabled)
            loadHealthConnect(showLoading = false, forceDataRead = true)
        }
    }

    /** Cho biết người dùng đã đăng nhập hay đang ở chế độ khách; đọc một lần lúc khởi tạo là đủ. */
    private fun loadSignedInState() {
        _uiState.update { it.copy(isSignedIn = authRepository.currentUserId != null) }
    }

    /** Mở hộp thoại "Khung giờ ngủ" với bản nháp bắt đầu từ khung đang dùng. */
    fun openSleepScheduleEditor() = _uiState.update {
        it.copy(sleepScheduleDraft = it.sleepSchedule, sleepScheduleError = null)
    }

    /** Đóng hộp thoại và bỏ mọi thay đổi chưa lưu. */
    fun dismissSleepScheduleEditor() = _uiState.update {
        it.copy(sleepScheduleDraft = null, sleepScheduleError = null)
    }

    /** Cập nhật bản nháp (mỗi lần người dùng chọn giờ xong) và xoá lỗi cũ. */
    fun onSleepScheduleDraftChange(draft: SleepSchedule) = _uiState.update {
        it.copy(sleepScheduleDraft = draft, sleepScheduleError = null)
    }

    /** Lưu khung giờ ngủ; khung không hợp lệ (trùng giờ) thì báo lỗi và giữ hộp thoại mở. */
    fun saveSleepSchedule() {
        val draft = _uiState.value.sleepScheduleDraft ?: return

        if (!draft.isValid) {
            _uiState.update { it.copy(sleepScheduleError = SLEEP_SCHEDULE_ERROR_EMPTY) }
            return
        }

        viewModelScope.launch {
            settingsRepository.setSleepSchedule(draft)
            _uiState.update { it.copy(sleepScheduleDraft = null, sleepScheduleError = null) }
        }
    }

    /** Nhận kết quả xin quyền POST_NOTIFICATIONS; đọc lại trạng thái thật thay vì tin tham số granted. */
    fun onNotificationPermissionResult() = loadNotificationPermission()

    /** Nhận kết quả xin quyền Health Connect; tham số granted chỉ quyết định có nạp lại số liệu ngay hay không. */
    fun onHealthConnectPermissionsResult(granted: Set<String>) {
        val required = _uiState.value.healthConnect.requiredPermissions

        loadHealthConnect(
            showLoading = false,
            forceDataRead = required.isNotEmpty() && granted.containsAll(required)
        )
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private companion object {
        /** Lỗi khi giờ đi ngủ trùng giờ thức (khung rỗng, không đo được gì). */
        const val SLEEP_SCHEDULE_ERROR_EMPTY = "Giờ đi ngủ và giờ thức không được trùng nhau"
    }
}