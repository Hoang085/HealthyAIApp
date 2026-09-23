package com.healthyai.app.presentation.screen.home

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.health.GetHealthConnectPermissionStatusUseCase
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.notification.GetNotificationPermissionStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Unit test cho trạng thái hoàn thành của thói quen trên màn Home và cho luồng
 * xin quyền Health Connect / quyền thông báo.
 *
 * Trước đây `HomeScreen` gọi `HabitItem(habit, isCompleted = false)` nên Home **không bao giờ**
 * hiển thị thói quen đã đạt mục tiêu. Test này chốt lại hành vi đúng: hoàn thành được suy ra
 * từ tổng giá trị log **trong ngày** so với `Habit.targetValue`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val getTodayHabitsUseCase = mockk<GetTodayHabitsUseCase>()
    private val getHealthDataUseCase = mockk<GetHealthDataUseCase>()
    private val getHealthConnectPermissionStatusUseCase =
        mockk<GetHealthConnectPermissionStatusUseCase>()
    private val getNotificationPermissionStatusUseCase =
        mockk<GetNotificationPermissionStatusUseCase>()
    private val habitRepository = mockk<HabitRepository>()

    private val waterHabit = Habit(
        id = 1L,
        name = "Uống nước",
        category = HabitCategory.WATER,
        targetValue = 2000f,
        unit = "ml"
    )

    @Before
    fun givenHealthConnectDefaults() {
        // Mặc định: Health Connect sẵn sàng, đã cấp đủ quyền và thông báo đang bật.
        givenHealthConnect(HealthConnectStatus.AVAILABLE, hasAllPermissions = true)
        givenNotificationsEnabled()
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

    // ── Xin quyền Health Connect ────────────────────────────────

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

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showHealthConnectCard)
        assertTrue(state.healthConnect.needsPermission)
        assertEquals(REQUIRED_PERMISSIONS, state.healthConnect.requiredPermissions)
        assertNull(state.error)
        // Health Connect vẫn sẵn sàng → vẫn đọc số liệu (thiếu quyền thì Health Connect trả 0).
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

    // ── Xin quyền thông báo nhắc nhở ─────────────────────────────

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

    // ── Helpers ────────────────────────────────────────────────

    private fun createViewModel() = HomeViewModel(
        getTodayHabitsUseCase = getTodayHabitsUseCase,
        getHealthDataUseCase = getHealthDataUseCase,
        getHealthConnectPermissionStatusUseCase = getHealthConnectPermissionStatusUseCase,
        getNotificationPermissionStatusUseCase = getNotificationPermissionStatusUseCase,
        habitRepository = habitRepository
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
