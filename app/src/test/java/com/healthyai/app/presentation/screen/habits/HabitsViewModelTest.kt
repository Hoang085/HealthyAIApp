package com.healthyai.app.presentation.screen.habits

import androidx.lifecycle.SavedStateHandle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.LogHabitUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho phản hồi "Đã ghi nhận" ở màn Thói quen.
 *
 * Trước đây `HabitsViewModel` đặt `showLogSuccessFor` nhưng **UI không dùng** nên người dùng không
 * nhận được phản hồi nào sau khi ghi nhận. Test này chốt lại hợp đồng giữa ViewModel và UI:
 * ghi thành công → phát sự kiện một lần; UI hiện xong → gọi `onLogSuccessShown()` để xoá sự kiện.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    private val getTodayHabitsUseCase = mockk<GetTodayHabitsUseCase>()
    private val logHabitUseCase = mockk<LogHabitUseCase>()
    private val habitRepository = mockk<HabitRepository>()

    private val waterHabit = Habit(
        id = 1L,
        name = "Uống nước",
        category = HabitCategory.WATER,
        targetValue = 2000f,
        unit = "ml"
    )

    @Before
    fun givenHabits() {
        every { getTodayHabitsUseCase() } returns flowOf(listOf(waterHabit))
        every { habitRepository.getAllActiveHabits() } returns flowOf(listOf(waterHabit))
        every { habitRepository.getHabitLogsForPeriod(0) } returns flowOf(emptyList())
        coEvery { logHabitUseCase(any()) } just Runs
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful log emits the ack event for that habit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.showLogSuccessFor)

        viewModel.logHabit(waterHabit, 500f)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.showLogSuccessFor)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `the ack event is cleared once the UI has shown it`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.logHabit(waterHabit, 500f)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.showLogSuccessFor)

        viewModel.onLogSuccessShown()

        assertNull(viewModel.uiState.value.showLogSuccessFor)
    }

    @Test
    fun `logging the same habit twice in a row shows the ack twice`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.logHabit(waterHabit, 500f)
        advanceUntilIdle()
        viewModel.onLogSuccessShown()
        assertNull(viewModel.uiState.value.showLogSuccessFor)

        viewModel.logHabit(waterHabit, 700f)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.showLogSuccessFor)
        coVerify(exactly = 2) { logHabitUseCase(any()) }
    }

    @Test
    fun `failed log shows an error instead of the ack event`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { logHabitUseCase(any()) } throws IllegalStateException("DB lỗi")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.logHabit(waterHabit, 500f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.showLogSuccessFor)
        assertEquals("Lỗi ghi nhận: DB lỗi", state.error)
    }

    @Test
    fun `dismissError clears the error shown on the habits screen`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { logHabitUseCase(any()) } throws IllegalStateException("DB lỗi")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.logHabit(waterHabit, 500f)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }

    // ── Chặn giá trị không hợp lệ (phụ lục C #11) ────

    @Test
    fun `logging a zero or negative value is rejected before writing`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.logHabit(waterHabit, 0f)
        viewModel.logHabit(waterHabit, -100f)
        advanceUntilIdle()

        assertEquals("Số lượng phải lớn hơn 0", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.showLogSuccessFor)
        coVerify(exactly = 0) { logHabitUseCase(any()) }
    }

    // ── Yêu cầu ghi nhận từ màn Phân tích (mục 3 #14) ────

    @Test
    fun `log request opens the dialog for the matching category`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val handle = SavedStateHandle(mapOf(KEY_LOG_CATEGORY to HabitCategory.WATER.name))
        val viewModel = createViewModel(handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LogRequest.Open(waterHabit), state.logRequest)
        // Đã xoá khoá trong SavedStateHandle → không mở lại hộp thoại khi màn hình tạo lại
        assertNull(handle.get<String>(KEY_LOG_CATEGORY))

        viewModel.onLogRequestHandled()
        assertNull(viewModel.uiState.value.logRequest)
    }

    @Test
    fun `log request without a matching habit asks the user to create one`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val handle = SavedStateHandle(mapOf(KEY_LOG_CATEGORY to HabitCategory.MINDFULNESS.name))
        val viewModel = createViewModel(handle)
        advanceUntilIdle()

        // Chỉ có thói quen Uống nước → gợi ý về Thiền định không có thói quen tương ứng
        assertEquals(
            LogRequest.Missing(HabitCategory.MINDFULNESS),
            viewModel.uiState.value.logRequest
        )
    }

    @Test
    fun `unknown category in the log request is ignored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val handle = SavedStateHandle(mapOf(KEY_LOG_CATEGORY to "KHONG_CO_TON_TAI"))
        val viewModel = createViewModel(handle)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.logRequest)
        assertNull(handle.get<String>(KEY_LOG_CATEGORY))
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = HabitsViewModel(
        getTodayHabitsUseCase = getTodayHabitsUseCase,
        logHabitUseCase = logHabitUseCase,
        habitRepository = habitRepository,
        savedStateHandle = savedStateHandle
    )
}
