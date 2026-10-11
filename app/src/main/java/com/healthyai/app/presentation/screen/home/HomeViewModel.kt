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
    /**
     * Bản chụp dữ liệu sức khỏe **của hôm nay đã lưu trong app** (`health_data_history`) — nguồn cho thẻ
     * "Tóm tắt sức khỏe" khi người dùng **TẮT** switch "Dùng dữ liệu Health Connect".
     *
     * Rỗng (`HealthData()`) nghĩa là hôm nay app chưa ghi được gì; lúc đó thẻ vẫn còn nguồn tự đo của
     * máy (cảm biến đếm bước / bộ ước lượng giấc ngủ) làm dự phòng.
     */
    val storedHealthData: HealthData = HealthData(),
    /**
     * Nguồn dữ liệu của thẻ "Tóm tắt sức khỏe": người dùng có đang dùng **Health Connect** hay không
     * (switch trên Trang chủ). Mặc định **TẮT** → dùng dữ liệu đã lưu trong app.
     */
    val healthConnectData: HealthConnectDataSettings = HealthConnectDataSettings(),
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
    val sensorSteps: Long = 0L,
    /** Số giờ ngủ của **đêm vừa qua** do app tự ước lượng từ cảm biến (0 khi chưa đủ dữ liệu). */
    val sensorSleepHours: Double = 0.0,
    /**
     * Nhịp tim **ghi trong ngày hôm nay** (`health_data_history`) — do lần đo bằng camera của app hoặc
     * lần đồng bộ Health Connect trong ngày; 0 khi hôm nay chưa có số nào.
     *
     * Giới hạn trong ngày để nhịp tim cũng "đọc lại từ đầu ngày" như bước chân/giờ ngủ: sang ngày mới
     * thì ô "Tim" về "chưa có số" cho tới khi đo lại (số cũ vẫn nằm trong lịch sử 7 ngày ở tab Phân tích).
     */
    val recordedHeartRate: Double = 0.0,
    /**
     * Khung giờ ngủ **người dùng đặt** — app chỉ đo giấc ngủ trong khung này (mặc định 23:00 – 07:00
     * khi người dùng chưa đặt gì).
     */
    val sleepSchedule: SleepSchedule = SleepSchedule.DEFAULT,
    /** Bản nháp đang sửa trong hộp thoại "Khung giờ ngủ"; `null` khi hộp thoại đóng. */
    val sleepScheduleDraft: SleepSchedule? = null,
    /** Lỗi của lần lưu khung giờ ngủ gần nhất (ví dụ hai mốc trùng nhau); `null` khi không có lỗi. */
    val sleepScheduleError: String? = null,
    /**
     * Trạng thái **đồng bộ đám mây** (mặc định TẮT — mục 3 #31). Thẻ "Đồng bộ đám mây" hiển thị và
     * bật/tắt giá trị này.
     */
    val cloudSync: CloudSyncSettings = CloudSyncSettings(),
    /**
     * Người dùng đã đăng nhập hay đang ở chế độ khách. Dùng để thẻ "Đồng bộ đám mây" nói rõ rằng
     * đồng bộ chỉ có tác dụng khi có tài khoản (chế độ khách không có `uid` để gắn dữ liệu).
     */
    val isSignedIn: Boolean = false
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
     * Số bước hiển thị trên thẻ "Tóm tắt sức khỏe".
     *
     * Nguồn **chính** do switch quyết định:
     * - BẬT → số của **Health Connect** ([healthData], có thể gồm cả đồng hồ đeo tay).
     * - TẮT → số trong **bản chụp đã lưu trong app** ([storedHealthData]).
     *
     * Nguồn chính **không có số** thì lấy **cảm biến đếm bước của chính điện thoại** ([sensorSteps]) làm
     * dự phòng — đây là nguồn **live**, cập nhật liên tục khi người dùng đi bộ.
     */
    val displaySteps: Long
        get() = primarySteps.takeIf { it > 0L } ?: sensorSteps

    private val primarySteps: Long
        get() = if (healthConnectData.enabled) healthData.steps else storedHealthData.steps

    /**
     * Số giờ ngủ hiển thị trên thẻ "Tóm tắt sức khỏe": nguồn chính theo switch (Health Connect / bản
     * chụp đã lưu trong app); khi nguồn đó **không có số** mới dùng ước lượng của app từ cảm biến
     * ([sensorSleepHours]).
     *
     * Giờ ngủ **không đổi trong ngày** nên việc mỗi nguồn chỉ được đọc ở thời điểm nạp không làm con số
     * bị "đóng băng".
     */
    val displaySleepHours: Double
        get() = primarySleepHours.takeIf { it > 0.0 } ?: sensorSleepHours

    private val primarySleepHours: Double
        get() = if (healthConnectData.enabled) healthData.sleepHours else storedHealthData.sleepHours

    /**
     * Nhịp tim hiển thị trên thẻ "Tóm tắt sức khỏe": nguồn chính theo switch (Health Connect / bản chụp
     * đã lưu trong app); khi nguồn đó **không có số của hôm nay** mới dùng **nhịp tim ghi gần nhất trong
     * ngày** ([recordedHeartRate]) — tức lần đo bằng camera ở màn "Đo nhịp tim".
     *
     * Giá trị `0.0` nghĩa là **hôm nay chưa có số nào**; UI hiện `-- bpm` (xem `HealthSummaryCard`) chứ
     * không hiện `0 bpm` (sẽ bị đọc thành "nhịp tim bằng 0").
     */
    val displayHeartRate: Double
        get() = primaryHeartRate.takeIf { it > 0.0 } ?: recordedHeartRate

    private val primaryHeartRate: Double
        get() = if (healthConnectData.enabled) healthData.heartRate else storedHealthData.heartRate

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
     * Kiểm tra trạng thái Health Connect rồi nạp số liệu sức khỏe **theo nguồn đang chọn**.
     *
     * Switch "Dùng dữ liệu Health Connect" quyết định đọc nguồn nào (xem [readActiveHealthData]):
     * - **BẬT** → đọc Health Connect (bước chân, giờ ngủ, nhịp tim, calo).
     * - **TẮT** (mặc định) → đọc bản chụp đã lưu trong app; **không** gọi Health Connect để lấy số.
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

    /**
     * Đọc số liệu cho **nguồn đang được chọn** rồi cập nhật state.
     *
     * Nguồn đọc thẳng từ [SettingsRepository] (chứ không suy từ state) để không phụ thuộc thời điểm Flow
     * thiết lập phát giá trị:
     * - BẬT → [readHealthData] (Health Connect).
     * - TẮT → [loadStoredHealthData] (bản chụp hôm nay đã ghi trong app).
     */
    private suspend fun readActiveHealthData() {
        if (settingsRepository.getHealthConnectData().enabled) {
            readHealthData()
        } else {
            loadStoredHealthData()
        }
    }

    /**
     * Đọc **bản chụp dữ liệu sức khỏe của hôm nay đã lưu trong app** (`health_data_history`).
     *
     * Dùng khi switch "Dùng dữ liệu Health Connect" đang TẮT. Đọc lỗi được use case quy về bản chụp
     * rỗng nên không bao giờ ném ra ngoài.
     */
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
     *
     * Số liệu được đọc lại theo **nguồn đang chọn**: BẬT → đọc Health Connect (số mới nhất, kể cả bước
     * chân của đồng hồ đeo tay); TẮT → đọc lại bản chụp đã lưu trong app (chỉ đọc DB, **không** gọi
     * Health Connect). Nhờ vậy khi quay lại màn Home, thẻ "Tóm tắt sức khỏe" luôn khớp với nguồn người
     * dùng chọn.
     */
    fun refreshSystemPermissions() {
        loadHealthConnect(showLoading = false, forceDataRead = true)
        loadNotificationPermission()
        loadStepCounter()
        loadSleepTracking()
        loadRecordedHeartRate()
    }

    /**
     * Đọc **nhịp tim ghi trong ngày hôm nay** (`health_data_history`).
     *
     * Đọc lại mỗi khi màn Trang chủ hiện ra — nhờ vậy vừa đo xong ở màn "Đo nhịp tim" rồi quay về là ô
     * "Tim" đã có số mới, không cần mở lại app. Sang ngày mới, số trả về là 0 cho tới khi có lần đo mới
     * trong ngày (xem `HealthDataRepository.getLatestRecordedHeartRate`).
     */
    private fun loadRecordedHeartRate() {
        viewModelScope.launch {
            _uiState.update { it.copy(recordedHeartRate = getLatestRecordedHeartRateUseCase()) }
        }
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

    // ── Tự ước lượng giờ ngủ bằng cảm biến của máy ─────────────

    /**
     * Đọc lại số giờ ngủ của **đêm vừa qua** do app tự ước lượng.
     *
     * Đây là phép đọc **một lần** (không phải cảm biến "live" như bước chân): giấc ngủ của đêm đã qua
     * không đổi trong ngày. Việc lấy mẫu do `SleepTrackingWorker` làm mỗi 15 phút, kể cả khi app đóng.
     */
    private fun loadSleepTracking() {
        viewModelScope.launch {
            val hours = runCatchingCancellable { sleepTrackerRepository.readLastNightSleepHours() }
                .getOrDefault(0.0)
            _uiState.update { it.copy(sensorSleepHours = hours) }
        }
    }

    // ── Khung giờ ngủ do người dùng đặt ────────────────────────

    /**
     * Theo dõi khung giờ ngủ trong bảng `app_settings`.
     *
     * Đọc bằng **Flow** (chứ không đọc một lần) để thẻ "Khung giờ ngủ" và mọi nơi khác luôn khớp với
     * giá trị đã lưu trong DB — kể cả sau khi người dùng vừa lưu, hoặc khi dữ liệu bị đổi từ nơi khác.
     * Chưa có gì trong DB thì repository trả về mặc định 23:00 – 07:00.
     */
    private fun observeSleepSchedule() {
        settingsRepository.observeSleepSchedule()
            .catch { }
            .onEach { schedule -> _uiState.update { it.copy(sleepSchedule = schedule) } }
            .launchIn(viewModelScope)
    }

    // ── Đồng bộ đám mây (mặc định TẮT — mục 3 #31) ─────────────

    /**
     * Theo dõi trạng thái "Đồng bộ đám mây" trong bảng `app_settings`.
     *
     * Đọc bằng **Flow** để thẻ luôn khớp với giá trị đã lưu (kể cả khi dữ liệu bị đổi từ nơi khác);
     * lỗi đọc cũng chỉ giữ nguyên giá trị mặc định **TẮT** thay vì bật đồng bộ ngoài ý muốn.
     */
    private fun observeCloudSync() {
        settingsRepository.observeCloudSync()
            .catch { }
            .onEach { cloudSync -> _uiState.update { it.copy(cloudSync = cloudSync) } }
            .launchIn(viewModelScope)
    }

    /**
     * Bật/tắt đồng bộ đám mây.
     *
     * Việc ghi mốc đồng ý nằm trong repository (bật = ghi mốc chặn "chỉ tải log tạo sau đây"), nên
     * tầng UI chỉ cần truyền ý định của người dùng.
     */
    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCloudSyncEnabled(enabled) }
    }

    // ── Nguồn dữ liệu sức khỏe của Trang chủ (mặc định TẮT) ─────

    /**
     * Theo dõi nguồn dữ liệu sức khỏe trong bảng `app_settings` để switch trên Trang chủ luôn khớp với
     * giá trị đã lưu (kể cả khi thiết lập bị đổi từ nơi khác); lỗi đọc giữ nguyên mặc định **TẮT**.
     *
     * Việc **nạp lại số liệu** không đặt ở đây mà do [setHealthConnectDataEnabled] và
     * [loadHealthConnect] đảm nhiệm — hai chỗ này đọc thẳng thiết lập nên không phụ thuộc thời điểm Flow
     * phát giá trị (tránh đọc trùng khi vừa mở màn).
     */
    private fun observeHealthConnectData() {
        settingsRepository.observeHealthConnectData()
            .catch { }
            .onEach { settings -> _uiState.update { it.copy(healthConnectData = settings) } }
            .launchIn(viewModelScope)
    }

    /**
     * Bật/tắt switch "Dùng dữ liệu Health Connect" trên Trang chủ.
     *
     * Ghi vào `app_settings` rồi **nạp lại ngay** nguồn vừa chọn để thẻ "Tóm tắt sức khỏe" đổi số tức
     * thì: BẬT → đọc Health Connect, TẮT → đọc lại bản chụp đã lưu trong app.
     */
    fun setHealthConnectDataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHealthConnectDataEnabled(enabled)
            loadHealthConnect(showLoading = false, forceDataRead = true)
        }
    }

    /**
     * Người dùng đã đăng nhập hay đang ở chế độ khách.
     *
     * Đọc một lần lúc khởi tạo là đủ: đổi phiên (đăng nhập/đăng xuất) làm `AppNavigation` dựng lại
     * màn Home, giống cách [loadUserName] đọc tên hiển thị.
     */
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

    /**
     * Lưu khung giờ ngủ.
     *
     * Khung không hợp lệ (giờ đi ngủ **trùng** giờ thức) → **không** ghi gì, giữ hộp thoại mở và báo
     * lỗi để người dùng sửa. Hợp lệ → ghi vào `app_settings` rồi đóng hộp thoại.
     */
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

    private companion object {
        /** Lỗi khi người dùng đặt giờ đi ngủ **trùng** giờ thức → khung rỗng, không đo được gì. */
        const val SLEEP_SCHEDULE_ERROR_EMPTY = "Giờ đi ngủ và giờ thức không được trùng nhau"
    }
}