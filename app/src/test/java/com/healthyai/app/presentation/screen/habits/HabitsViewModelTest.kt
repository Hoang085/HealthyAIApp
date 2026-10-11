package com.healthyai.app.presentation.screen.habits

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

    // ── Yêu cầu ghi nhận từ màn Phân tích / màn Ghi chép (mục 3 #14, phụ lục C #22) ────

    @Test
    fun `log request opens the dialog for the matching category`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Màn Phân tích gửi yêu cầu ghi nhận cho danh mục "Dinh dưỡng"
        viewModel.onLogRequest(HabitCategory.NUTRITION.name)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LogRequest.Missing(HabitCategory.NUTRITION), state.logRequest)

        viewModel.onLogRequestHandled()
        assertNull(viewModel.uiState.value.logRequest)
    }

    @Test
    fun `log request for a category that has a habit opens it`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLogRequest(HabitCategory.WATER.name)
        advanceUntilIdle()

        assertEquals(LogRequest.Open(waterHabit), viewModel.uiState.value.logRequest)
    }

    @Test
    fun `log request without a matching habit asks the user to create one`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Chỉ có thói quen Uống nước → gợi ý về Thiền định không có thói quen tương ứng
        viewModel.onLogRequest(HabitCategory.MINDFULNESS.name)
        advanceUntilIdle()

        assertEquals(
            LogRequest.Missing(HabitCategory.MINDFULNESS),
            viewModel.uiState.value.logRequest
        )
    }

    @Test
    fun `unknown category in the log request is ignored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLogRequest("KHONG_CO_TON_TAI")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.logRequest)
    }

    // ── Hợp đồng của "Thêm vào nhật ký" / nút gợi ý AI (KB-45, TC-CAM-15) ────

    /**
     * KB-45 — "Đáng ngờ nếu: … **ghi nhận sai thói quen** (ví dụ ghi vào Uống nước)".
     *
     * Yêu cầu ghi nhận cho danh mục Dinh dưỡng **không được** mở hộp thoại của thói quen Uống nước
     * chỉ vì nó đứng trước trong danh sách.
     */
    @Test
    fun `log request never opens a habit from another category`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val saladHabit = Habit(
            id = 2L,
            name = "Ăn salad",
            category = HabitCategory.NUTRITION,
            targetValue = 600f,
            unit = "calo"
        )
        every { habitRepository.getAllActiveHabits() } returns flowOf(listOf(waterHabit, saladHabit))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLogRequest(HabitCategory.NUTRITION.name)
        advanceUntilIdle()

        assertEquals(
            "Phải mở đúng thói quen Dinh dưỡng, không phải 'Uống nước' đứng trước",
            LogRequest.Open(saladHabit),
            viewModel.uiState.value.logRequest
        )
    }

    /**
     * TC-CAM-15 — "Quay lại tab Ghi chép rồi bấm nút lần nữa **vẫn** hoạt động (yêu cầu là sự kiện một
     * lần, không bị 'dính')".
     *
     * Đây là hợp đồng giữ cho thẻ "Thêm vào nhật ký" dùng lại được: sau khi hộp thoại đã xử lý xong
     * (`onLogRequestHandled`), cùng một danh mục phải kích hoạt được yêu cầu **mới**.
     */
    @Test
    fun `the same log request can be triggered again after being handled`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLogRequest(HabitCategory.WATER.name)
        advanceUntilIdle()
        assertEquals(LogRequest.Open(waterHabit), viewModel.uiState.value.logRequest)

        viewModel.onLogRequestHandled()
        assertNull(viewModel.uiState.value.logRequest)

        // Bấm "Thêm vào nhật ký" lần thứ hai cho cùng danh mục → phải mở lại, không bị "dính"
        viewModel.onLogRequest(HabitCategory.WATER.name)
        advanceUntilIdle()

        assertEquals(LogRequest.Open(waterHabit), viewModel.uiState.value.logRequest)
    }

    /** Chưa có thói quen nào cả → vẫn là nhánh "nhắc tạo mới", không phải im lặng hay crash. */
    @Test
    fun `log request with no habit at all asks the user to create one`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { habitRepository.getAllActiveHabits() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLogRequest(HabitCategory.NUTRITION.name)
        advanceUntilIdle()

        assertEquals(
            LogRequest.Missing(HabitCategory.NUTRITION),
            viewModel.uiState.value.logRequest
        )
    }

    private fun createViewModel() = HabitsViewModel(
        getTodayHabitsUseCase = getTodayHabitsUseCase,
        logHabitUseCase = logHabitUseCase,
        habitRepository = habitRepository
    )
}
