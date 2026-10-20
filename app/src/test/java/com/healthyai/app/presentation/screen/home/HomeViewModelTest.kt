package com.healthyai.app.presentation.screen.home

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
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
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

/** Unit test cho trạng thái hoàn thành thói quen trên màn Home và luồng xin quyền Health Connect / thông báo. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val getTodayHabitsUseCase = mockk<GetTodayHabitsUseCase>()
    private val getHealthDataUseCase = mockk<GetHealthDataUseCase>()
    private val getHealthConnectPermissionStatusUseCase =
        mockk<GetHealthConnectPermissionStatusUseCase>()
    private val getNotificationPermissionStatusUseCase =
        mockk<GetNotificationPermissionStatusUseCase>()
    private val getStepCounterStatusUseCase = mockk<GetStepCounterStatusUseCase>()
    private val habitRepository = mockk<HabitRepository>()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val stepCounterRepository = mockk<StepCounterRepository>()
    private val sleepTrackerRepository = mockk<SleepTrackerRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val getLatestRecordedHeartRateUseCase = mockk<GetLatestRecordedHeartRateUseCase>()
    private val getStoredHealthDataUseCase = mockk<GetStoredHealthDataUseCase>()

    /** Khung giờ ngủ "trong DB" — test đổi giá trị này rồi cho `advanceUntilIdle()` để mô phỏng Room phát lại. */
    private val scheduleFlow = MutableStateFlow(SleepSchedule.DEFAULT)

    /** Thiết lập "Đồng bộ đám mây" trong DB — mặc định TẮT, đúng như lần cài đầu. */
    private val cloudSyncFlow = MutableStateFlow(CloudSyncSettings())

    /** Thiết lập nguồn dữ liệu sức khỏe trong DB — mặc định TẮT (dùng dữ liệu đã lưu trong app). */
    private val healthConnectDataFlow = MutableStateFlow(HealthConnectDataSettings())

    private val waterHabit = Habit(
        id = 1L,
        name = "Uống nước",
        category = HabitCategory.WATER,
        targetValue = 2000f,
        unit = "ml"
    )

    @Before
    fun givenHealthConnectDefaults() {
        // Mặc định: Health Connect sẵn sàng, đã cấp đủ quyền, thông báo đang bật, máy không có cảm biến đếm bước.
        givenHealthConnect(HealthConnectStatus.AVAILABLE, hasAllPermissions = true)
        givenNotificationsEnabled()
        givenStepCounter(sensorAvailable = false, permissionGranted = false)
        givenSleepTracker(hours = 0.0)
        givenRecordedHeartRate(bpm = 0.0)
        givenStoredHealthData(HealthData())
        every { settingsRepository.observeSleepSchedule() } returns scheduleFlow
        every { settingsRepository.observeCloudSync() } returns cloudSyncFlow
        every { settingsRepository.observeHealthConnectData() } returns healthConnectDataFlow
        coEvery { settingsRepository.getHealthConnectData() } answers { healthConnectDataFlow.value }
        // Ghi thiết lập nguồn dữ liệu cũng cập nhật lại Flow — mô phỏng Room phát lại sau khi ghi.
        coEvery { settingsRepository.setHealthConnectDataEnabled(any()) } answers {
            healthConnectDataFlow.value =
                HealthConnectDataSettings(enabled = invocation.args[0] as Boolean)
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `habit reaching its daily target is marked as completed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs(waterLog(1200f), waterLog(800f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.habits.single().isCompleted)
    }

    @Test
    fun `habit below its daily target is not completed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs(waterLog(1500f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.habits.single().isCompleted)
    }

    @Test
    fun `habit without any log today is not completed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.habits.single().isCompleted)
    }

    @Test
    fun `logs of another habit do not complete the habit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs(waterLog(5000f).copy(habitId = 2L))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.habits.single().isCompleted)
    }

    @Test
    fun `completion state is computed per habit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sleepHabit = Habit(
            id = 2L,
            name = "Ngủ đủ giấc",
            category = HabitCategory.SLEEP,
            targetValue = 8f,
            unit = "giờ"
        )
        givenHabits(waterHabit, sleepHabit)
        givenTodayLogs(waterLog(2000f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val habits = viewModel.uiState.value.habits
        assertTrue(habits.first { it.habit.id == 1L }.isCompleted)
        assertFalse(habits.first { it.habit.id == 2L }.isCompleted)
    }

    // Xin quyền Health Connect

    @Test
    fun `missing permissions show the permission card without a red error banner`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(
            status = HealthConnectStatus.AVAILABLE,
            hasAllPermissions = false,
            requiredPermissions = REQUIRED_PERMISSIONS
        )
        givenHealthConnectData(enabled = true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showHealthConnectCard)
        assertTrue(state.healthConnect.needsPermission)
        assertEquals(REQUIRED_PERMISSIONS, state.healthConnect.requiredPermissions)
        assertNull(state.error)
        // Nguồn Health Connect đang BẬT → vẫn đọc số liệu (thiếu quyền thì Health Connect trả 0).
        coVerify(exactly = 1) { getHealthDataUseCase() }
    }

    @Test
    fun `health connect not installed shows the install card, not a red error banner`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(HealthConnectStatus.NOT_INSTALLED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HealthConnectStatus.NOT_INSTALLED, state.healthConnect.status)
        assertTrue(state.showHealthConnectCard)
        assertNull(state.error)
        // Không có Health Connect thì không đọc dữ liệu: mọi chỉ số về 0.
        assertEquals(0L, state.healthData.steps)
        assertEquals(0.0, state.healthData.sleepHours, 0.001)
        assertEquals(0.0, state.healthData.heartRate, 0.001)
        assertEquals(0.0, state.healthData.activeCalories, 0.001)
        coVerify(exactly = 0) { getHealthDataUseCase() }
    }

    @Test
    fun `health connect needing an update shows the update card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(HealthConnectStatus.UPDATE_REQUIRED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HealthConnectStatus.UPDATE_REQUIRED, state.healthConnect.status)
        assertTrue(state.showHealthConnectCard)
        assertNull(state.error)
    }

    @Test
    fun `granting every permission hides the card and loads health data`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(
            status = HealthConnectStatus.AVAILABLE,
            hasAllPermissions = false,
            requiredPermissions = REQUIRED_PERMISSIONS
        )
        givenHealthConnectData(enabled = true)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showHealthConnectCard)

        // Người dùng cấp đủ quyền trong hộp thoại Health Connect
        givenHealthConnect(HealthConnectStatus.AVAILABLE, hasAllPermissions = true)
        viewModel.onHealthConnectPermissionsResult(REQUIRED_PERMISSIONS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showHealthConnectCard)
        assertTrue(state.healthConnect.hasAllPermissions)
        assertNull(state.error)
        coVerify(exactly = 2) { getHealthDataUseCase() }
    }

    @Test
    fun `granting only some permissions keeps the card visible`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(
            status = HealthConnectStatus.AVAILABLE,
            hasAllPermissions = false,
            requiredPermissions = REQUIRED_PERMISSIONS
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onHealthConnectPermissionsResult(setOf("android.permission.health.READ_STEPS"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showHealthConnectCard)
        assertFalse(state.healthConnect.hasAllPermissions)
    }

    @Test
    fun `refreshing after the user grants permission in health connect hides the card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnect(
            status = HealthConnectStatus.AVAILABLE,
            hasAllPermissions = false,
            requiredPermissions = REQUIRED_PERMISSIONS
        )
        givenHealthConnectData(enabled = true)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showHealthConnectCard)

        // Người dùng cấp quyền trực tiếp trong app Health Connect rồi quay lại app
        givenHealthConnect(HealthConnectStatus.AVAILABLE, hasAllPermissions = true)
        viewModel.refreshHealthConnect()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showHealthConnectCard)
        coVerify(exactly = 2) { getHealthDataUseCase() }
    }

    // Xin quyền thông báo nhắc nhở

    @Test
    fun `blocked notifications show the reminder card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenNotificationPermission(notificationsEnabled = false, canRequestPermission = true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showReminderNotificationCard)
        assertTrue(state.notificationPermission.canRequestPermission)
        assertFalse(state.showHealthConnectCard)
    }

    @Test
    fun `notifications turned off in settings still show the card without request option`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenNotificationPermission(notificationsEnabled = false, canRequestPermission = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showReminderNotificationCard)
        assertFalse(state.notificationPermission.canRequestPermission)
    }

    @Test
    fun `enabled notifications hide the reminder card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showReminderNotificationCard)
    }

    @Test
    fun `granting notification permission hides the reminder card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenNotificationPermission(notificationsEnabled = false, canRequestPermission = true)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showReminderNotificationCard)

        // Người dùng bấm "Cho phép" trong hộp thoại POST_NOTIFICATIONS
        givenNotificationsEnabled()
        viewModel.onNotificationPermissionResult()

        assertFalse(viewModel.uiState.value.showReminderNotificationCard)
    }

    @Test
    fun `refresh re-checks notifications enabled in settings`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenNotificationPermission(notificationsEnabled = false, canRequestPermission = false)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showReminderNotificationCard)

        // Người dùng bật lại thông báo trong Settings rồi quay lại app
        givenNotificationsEnabled()
        viewModel.refreshSystemPermissions()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showReminderNotificationCard)
    }

    // Đếm bước bằng cảm biến của máy

    @Test
    fun `sensor steps are used when the app has no stored steps`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = false)
        givenStoredHealthData(HealthData()) // Bản chụp trong app chưa có bước nào
        givenStepCounter(sensorAvailable = true, permissionGranted = true, steps = 4_230L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4_230L, state.sensorSteps)
        assertEquals(
            "Nguồn TẮT và bản chụp trống → lấy số bước live từ cảm biến của máy",
            4_230L,
            state.displaySteps
        )
        assertFalse(state.showStepCounterCard)
    }

    @Test
    fun `steps come from the stored snapshot when the health connect source is off`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = false)
        givenStoredHealthData(HealthData(steps = 3_000L))
        givenStepCounter(sensorAvailable = true, permissionGranted = true, steps = 1_200L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn TẮT → lấy bản chụp đã lưu trong app; cảm biến chỉ là dự phòng",
            3_000L,
            state.displaySteps
        )
        coVerify(exactly = 0) { getHealthDataUseCase() }
    }

    @Test
    fun `steps come from health connect when the health connect source is on`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = true)
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(steps = 3_000L))
        givenStoredHealthData(HealthData(steps = 1_000L))
        givenStepCounter(sensorAvailable = true, permissionGranted = true, steps = 1_200L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn BẬT → ưu tiên số của Health Connect thay vì bản chụp / cảm biến",
            3_000L,
            state.displaySteps
        )
    }

    @Test
    fun `health connect steps are used when the app has no stored steps and no usable sensor`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = true)
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(steps = 3_000L))
        givenStoredHealthData(HealthData())
        givenStepCounter(sensorAvailable = false, permissionGranted = false, steps = 0L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn BẬT, bản chụp trống và máy không có cảm biến → lấy số của Health Connect",
            3_000L,
            state.displaySteps
        )
    }

    @Test
    fun `a device with a step sensor but no permission shows the step counter card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenStepCounter(sensorAvailable = true, permissionGranted = false, steps = 999L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showStepCounterCard)
        assertEquals("Chưa có quyền thì không hiển thị số bước nào", 0L, state.sensorSteps)
        coVerify(exactly = 0) { stepCounterRepository.observeTodaySteps() }
    }

    @Test
    fun `a device without a step sensor never shows the step counter card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenStepCounter(sensorAvailable = false, permissionGranted = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse("Máy không có cảm biến thì không có gì để người dùng bật", viewModel.uiState.value.showStepCounterCard)
    }

    @Test
    fun `granting step permission hides the card and starts counting`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenStepCounter(sensorAvailable = true, permissionGranted = false)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showStepCounterCard)

        // Người dùng bấm "Cho phép" trong hộp thoại ACTIVITY_RECOGNITION
        givenStepCounter(sensorAvailable = true, permissionGranted = true, steps = 1_250L)
        viewModel.onStepPermissionResult()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showStepCounterCard)
        assertEquals(1_250L, state.sensorSteps)
        assertTrue(state.displaySteps == 1_250L)
    }

    @Test
    fun `refresh re-checks the step permission granted in settings`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenStepCounter(sensorAvailable = true, permissionGranted = false)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showStepCounterCard)

        // Người dùng bật quyền trong Settings rồi quay lại app
        givenStepCounter(sensorAvailable = true, permissionGranted = true, steps = 800L)
        viewModel.refreshSystemPermissions()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showStepCounterCard)
        assertEquals(800L, state.sensorSteps)
    }

    // Tự ước lượng giờ ngủ bằng cảm biến của máy

    @Test
    fun `device sleep estimate is used when the app has no stored sleep data`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = false)
        givenStoredHealthData(HealthData(sleepHours = 0.0))
        givenSleepTracker(hours = 6.5)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(6.5, state.sensorSleepHours, 0.001)
        assertEquals(
            "Nguồn TẮT và bản chụp trống → lấy ước lượng giờ ngủ của app",
            6.5,
            state.displaySleepHours,
            0.001
        )
    }

    @Test
    fun `stored sleep is used when the health connect source is off`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = false)
        givenStoredHealthData(HealthData(sleepHours = 7.0))
        givenSleepTracker(hours = 6.0)
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(sleepHours = 8.0))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn TẮT → lấy giờ ngủ của bản chụp trong app, không đọc Health Connect",
            7.0,
            state.displaySleepHours,
            0.001
        )
        coVerify(exactly = 0) { getHealthDataUseCase() }
    }

    @Test
    fun `health connect sleep wins when the health connect source is on`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = true)
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(sleepHours = 7.5))
        givenSleepTracker(hours = 6.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn BẬT → số của Health Connect (đồng hồ đeo tay) được ưu tiên; bộ ước lượng chỉ là dự phòng",
            7.5,
            state.displaySleepHours,
            0.001
        )
    }

    @Test
    fun `no sleep at all keeps the circle at zero`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(sleepHours = 0.0))
        givenSleepTracker(hours = 0.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.0, state.displaySleepHours, 0.001)
    }

    @Test
    fun `refresh re-reads the sleep estimate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(sleepHours = 0.0))
        givenSleepTracker(hours = 0.0)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0.0, viewModel.uiState.value.sensorSleepHours, 0.001)

        // Worker đã lấy đủ mẫu trong lúc app đóng → mở lại màn Home thấy số mới
        givenSleepTracker(hours = 7.2)
        viewModel.refreshSystemPermissions()
        advanceUntilIdle()

        assertEquals(7.2, viewModel.uiState.value.sensorSleepHours, 0.001)
    }

    // Nhịp tim ghi trên máy (đo bằng camera)

    @Test
    fun `the heart rate recorded on the device is shown when the app has no stored heart rate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = false)
        givenStoredHealthData(HealthData())
        givenRecordedHeartRate(bpm = 88.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn TẮT và bản chụp không có nhịp tim → lấy số đo trong ngày trên máy",
            88.0,
            state.displayHeartRate,
            0.001
        )
    }

    @Test
    fun `health connect heart rate wins when the health connect source is on`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        givenHealthConnectData(enabled = true)
        // Health Connect có thể có số của đồng hồ đeo tay nên được ưu tiên
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(heartRate = 62.0))
        givenRecordedHeartRate(bpm = 88.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Nguồn BẬT → số của Health Connect được ưu tiên",
            62.0,
            state.displayHeartRate,
            0.001
        )
    }

    @Test
    fun `a new day with no measurement yet keeps the heart circle empty`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        // Sang ngày mới: chỉ đọc số của hôm nay nên UI hiện "-- bpm", không hiện nhịp tim cũ
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(heartRate = 0.0))
        givenRecordedHeartRate(bpm = 0.0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.displayHeartRate, 0.001)
    }

    @Test
    fun `coming back from a measurement re-reads the recorded heart rate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(heartRate = 0.0))
        givenRecordedHeartRate(bpm = 0.0)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0.0, viewModel.uiState.value.displayHeartRate, 0.001)

        // Người dùng đo nhịp tim bằng camera rồi quay lại màn Trang chủ → thấy số vừa đo ngay
        givenRecordedHeartRate(bpm = 76.0)
        viewModel.refreshSystemPermissions()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(76.0, state.displayHeartRate, 0.001)
    }

    // Khung giờ ngủ do người dùng đặt

    @Test
    fun `the default window is 23 to 7 when the user has not set anything`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SleepSchedule.DEFAULT, viewModel.uiState.value.sleepSchedule)
        assertEquals(LocalTime.of(23, 0), viewModel.uiState.value.sleepSchedule.bedtime)
        assertEquals(LocalTime.of(7, 0), viewModel.uiState.value.sleepSchedule.wakeTime)
    }

    @Test
    fun `the window stored in the database is the one shown on home`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        val custom = SleepSchedule(LocalTime.of(22, 30), LocalTime.of(6, 15))
        scheduleFlow.value = custom

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(custom, viewModel.uiState.value.sleepSchedule)
    }

    @Test
    fun `opening the editor starts from the window currently in use`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        val custom = SleepSchedule(LocalTime.of(22, 0), LocalTime.of(6, 0))
        scheduleFlow.value = custom

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSleepScheduleEditorOpen)
        viewModel.openSleepScheduleEditor()

        assertTrue(viewModel.uiState.value.isSleepScheduleEditorOpen)
        assertEquals(custom, viewModel.uiState.value.sleepScheduleDraft)
    }

    @Test
    fun `saving a valid window stores it and closes the editor`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        coJustRun { settingsRepository.setSleepSchedule(any()) }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val newWindow = SleepSchedule(LocalTime.of(22, 45), LocalTime.of(6, 30))
        viewModel.openSleepScheduleEditor()
        viewModel.onSleepScheduleDraftChange(newWindow)
        viewModel.saveSleepSchedule()
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setSleepSchedule(newWindow) }
        assertFalse("Lưu xong thì đóng hộp thoại", viewModel.uiState.value.isSleepScheduleEditorOpen)
        assertNull(viewModel.uiState.value.sleepScheduleError)
    }

    @Test
    fun `saving the same bedtime and wake time keeps the editor open with an error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openSleepScheduleEditor()
        viewModel.onSleepScheduleDraftChange(SleepSchedule(LocalTime.of(7, 0), LocalTime.of(7, 0)))
        viewModel.saveSleepSchedule()
        advanceUntilIdle()

        assertTrue("Khung rỗng thì phải báo lỗi", viewModel.uiState.value.sleepScheduleError != null)
        assertTrue("Hộp thoại vẫn mở để người dùng sửa", viewModel.uiState.value.isSleepScheduleEditorOpen)
        coVerify(exactly = 0) { settingsRepository.setSleepSchedule(any()) }
    }

    @Test
    fun `changing the draft clears the previous error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openSleepScheduleEditor()
        viewModel.onSleepScheduleDraftChange(SleepSchedule(LocalTime.of(7, 0), LocalTime.of(7, 0)))
        viewModel.saveSleepSchedule()
        assertTrue(viewModel.uiState.value.sleepScheduleError != null)

        viewModel.onSleepScheduleDraftChange(SleepSchedule(LocalTime.of(23, 0), LocalTime.of(6, 45)))

        assertNull("Chọn giờ xong thì lỗi cũ biến mất", viewModel.uiState.value.sleepScheduleError)
    }

    @Test
    fun `dismissing the editor discards the draft`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openSleepScheduleEditor()
        viewModel.onSleepScheduleDraftChange(SleepSchedule(LocalTime.of(21, 0), LocalTime.of(5, 0)))
        viewModel.dismissSleepScheduleEditor()

        val state = viewModel.uiState.value
        assertFalse(state.isSleepScheduleEditorOpen)
        assertNull(state.sleepScheduleError)
        assertEquals("Huỷ thì khung đang dùng không đổi", SleepSchedule.DEFAULT, state.sleepSchedule)
        coVerify(exactly = 0) { settingsRepository.setSleepSchedule(any()) }
    }

    // Lời chào: tên thật, không hard-code

    @Test
    fun `greeting name comes from the signed in user`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        every { authRepository.currentUserName } returns "alex.healthyai"

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("alex.healthyai", viewModel.uiState.value.userName)
    }

    @Test
    fun `guest mode has no greeting name`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits(waterHabit)
        givenTodayLogs()
        every { authRepository.currentUserName } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull("Chế độ khách không hiện tên nào (không còn 'Alex' cứng)", viewModel.uiState.value.userName)
    }

    // Helpers

    // Đồng bộ đám mây

    @Test
    fun `cloud sync is off by default`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(
            "Chưa bật thì app không được tải dữ liệu lên đám mây",
            viewModel.uiState.value.cloudSync.enabled
        )
    }

    @Test
    fun `the stored cloud sync setting is reflected in the state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        val viewModel = createViewModel()
        advanceUntilIdle()

        cloudSyncFlow.value = CloudSyncSettings(enabled = true, consentAtMillis = 1_700_000_000_000L)
        advanceUntilIdle()

        assertEquals(1_700_000_000_000L, viewModel.uiState.value.cloudSync.consentAtMillis)
        assertTrue(viewModel.uiState.value.cloudSync.enabled)
    }

    @Test
    fun `toggling cloud sync writes it through the settings repository`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        coJustRun { settingsRepository.setCloudSyncEnabled(any(), any()) }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setCloudSyncEnabled(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setCloudSyncEnabled(true, any()) }
    }

    // Nguồn dữ liệu sức khỏe của Trang chủ (switch)

    @Test
    fun `the health connect source is off by default`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(
            "Mặc định TẮT → thẻ Tóm tắt sức khỏe dùng dữ liệu đã lưu trong app",
            viewModel.uiState.value.healthConnectData.enabled
        )
    }

    @Test
    fun `the stored source setting is reflected in the state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        givenHealthConnectData(enabled = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.healthConnectData.enabled)
    }

    @Test
    fun `toggling the health data source writes it through the settings repository`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setHealthConnectDataEnabled(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setHealthConnectDataEnabled(true) }
    }

    @Test
    fun `turning the health connect source on reloads health connect data`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        givenStoredHealthData(HealthData(steps = 111L))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Nguồn đang TẮT: chưa đọc Health Connect lần nào.
        coVerify(exactly = 0) { getHealthDataUseCase() }

        coEvery { getHealthDataUseCase() } returns Result.success(HealthData(steps = 999L))
        viewModel.setHealthConnectDataEnabled(true)
        advanceUntilIdle()

        assertEquals(
            "Bật switch → nạp ngay số của Health Connect",
            999L,
            viewModel.uiState.value.displaySteps
        )
        coVerify(exactly = 1) { getHealthDataUseCase() }
    }

    @Test
    fun `a signed in user is reported as signed in`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        every { authRepository.currentUserId } returns "uid-tester"
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun `a guest is not reported as signed in`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenHabits()
        givenTodayLogs()
        every { authRepository.currentUserId } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    private fun createViewModel() = HomeViewModel(
        getTodayHabitsUseCase = getTodayHabitsUseCase,
        getHealthDataUseCase = getHealthDataUseCase,
        getHealthConnectPermissionStatusUseCase = getHealthConnectPermissionStatusUseCase,
        getNotificationPermissionStatusUseCase = getNotificationPermissionStatusUseCase,
        getStepCounterStatusUseCase = getStepCounterStatusUseCase,
        habitRepository = habitRepository,
        authRepository = authRepository,
        stepCounterRepository = stepCounterRepository,
        sleepTrackerRepository = sleepTrackerRepository,
        settingsRepository = settingsRepository,
        getLatestRecordedHeartRateUseCase = getLatestRecordedHeartRateUseCase,
        getStoredHealthDataUseCase = getStoredHealthDataUseCase
    )

    private fun givenHabits(vararg habits: Habit) {
        every { getTodayHabitsUseCase() } returns flowOf(habits.toList())
    }

    private fun givenTodayLogs(vararg logs: HabitLog) {
        every { habitRepository.getHabitLogsForPeriod(0) } returns flowOf(logs.toList())
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData())
    }

    private fun givenHealthConnect(
        status: HealthConnectStatus,
        hasAllPermissions: Boolean = false,
        requiredPermissions: Set<String> = emptySet()
    ) {
        coEvery { getHealthConnectPermissionStatusUseCase() } returns HealthConnectPermissionStatus(
            status = status,
            requiredPermissions = requiredPermissions,
            hasAllPermissions = hasAllPermissions
        )
    }

    private fun givenNotificationsEnabled() {
        givenNotificationPermission(notificationsEnabled = true, canRequestPermission = false)
    }

    private fun givenNotificationPermission(
        notificationsEnabled: Boolean,
        canRequestPermission: Boolean = false
    ) {
        every { getNotificationPermissionStatusUseCase() } returns NotificationPermissionStatus(
            notificationsEnabled = notificationsEnabled,
            canRequestPermission = canRequestPermission
        )
    }

    private fun givenStepCounter(
        sensorAvailable: Boolean,
        permissionGranted: Boolean,
        steps: Long = 0L
    ) {
        every { getStepCounterStatusUseCase() } returns StepCounterStatus(
            sensorAvailable = sensorAvailable,
            permissionGranted = permissionGranted
        )
        every { stepCounterRepository.observeTodaySteps() } returns flowOf(steps)
    }

    private fun givenSleepTracker(hours: Double) {
        coEvery { sleepTrackerRepository.readLastNightSleepHours() } returns hours
    }

    /** Nhịp tim ghi gần nhất trên máy (`health_data_history`) mà màn Trang chủ đọc được. */
    private fun givenRecordedHeartRate(bpm: Double) {
        coEvery { getLatestRecordedHeartRateUseCase() } returns bpm
    }

    /** Bản chụp sức khỏe hôm nay đã lưu trong app (`health_data_history`) mà màn Trang chủ đọc được. */
    private fun givenStoredHealthData(data: HealthData) {
        coEvery { getStoredHealthDataUseCase() } returns data
    }

    /** Đổi nguồn dữ liệu như thể DB đã đổi; gọi TRƯỚC `createViewModel()` để áp dụng cho lần nạp đầu. */
    private fun givenHealthConnectData(enabled: Boolean) {
        healthConnectDataFlow.value = HealthConnectDataSettings(enabled = enabled)
    }

    private fun waterLog(value: Float) = HabitLog(
        habitId = 1L,
        habitName = "Uống nước",
        category = HabitCategory.WATER,
        value = value,
        unit = "ml",
        targetValueAtTime = 2000f
    )

    private companion object {
        val REQUIRED_PERMISSIONS = setOf(
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_SLEEP",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED"
        )
    }
}
