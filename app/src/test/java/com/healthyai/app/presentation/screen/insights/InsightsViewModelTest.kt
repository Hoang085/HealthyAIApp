package com.healthyai.app.presentation.screen.insights

import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.usecase.health.GetHealthInsightsUseCase
import com.healthyai.app.domain.usecase.health.GetRecentHealthSummaryUseCase
import com.healthyai.app.domain.usecase.health.GetSleepStageBreakdownUseCase
import com.healthyai.app.domain.usecase.health.GetVitalityScoreUseCase
import com.healthyai.app.domain.usecase.habit.GetWeeklyConsistencyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Test cho `InsightsViewModel`: màn Phân tích phải báo lỗi thay vì im lặng ở trạng thái rỗng. */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val getHealthInsightsUseCase = mockk<GetHealthInsightsUseCase>()
    private val getVitalityScoreUseCase = mockk<GetVitalityScoreUseCase>()
    private val getWeeklyConsistencyUseCase = mockk<GetWeeklyConsistencyUseCase>()
    private val getSleepStageBreakdownUseCase =
        mockk<GetSleepStageBreakdownUseCase>(relaxed = true)
    private val getRecentHealthSummaryUseCase = mockk<GetRecentHealthSummaryUseCase>()

    private val today: LocalDate = LocalDate.now()

    @Before
    fun givenDefaults() {
        givenSuggestions()
        givenVitality(VitalityScore())
        givenConsistency()
        givenHealthHistory()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `every source ends up in the ui state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val suggestion = Suggestion(
            id = 1L,
            title = "Uống nước",
            message = "Hãy uống thêm nước",
            priority = SuggestionPriority.MEDIUM,
            category = HabitCategory.WATER
        )
        givenSuggestions(suggestion)
        givenVitality(VitalityScore(score = 72, delta = 4, hasData = true))
        givenConsistency(DailyConsistency(today, completedHabits = 1, totalHabits = 2))
        givenHealthHistory(DailyHealthSummary(date = today, steps = 4_000L, sleepHours = 7.5))
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(suggestion), state.suggestions)
        assertEquals(72, state.vitality.score)
        assertEquals(1, state.weeklyConsistency.size)
        assertEquals(1, state.healthHistory.size)
        assertTrue("Xu hướng giấc ngủ dựng từ chính lịch sử sức khỏe", state.sleepTrend.hasData)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `isLoading stays true until the first snapshot arrives`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // Flow chưa phát giá trị nào → màn hình còn ở trạng thái loading
        every { getHealthInsightsUseCase() } returns flow { }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a failing data flow is reported instead of being swallowed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { getHealthInsightsUseCase() } returns flow { error("DB hỏng") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull("Màn Phân tích phải báo lỗi thay vì im lặng ở trạng thái rỗng", state.error)
        assertTrue(state.error!!.contains("DB hỏng"))
    }

    @Test
    fun `dismissing the error clears it`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { getHealthInsightsUseCase() } returns flow { error("DB hỏng") }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sleep stage details are read at start and again on refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        givenSleepStages(SleepStageBreakdown(deepMinutes = 60, lightMinutes = 180, remMinutes = 30))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.sleepStages.totalMinutes / 60)

        viewModel.refresh() // mỗi lần quay lại màn Phân tích
        advanceUntilIdle()

        coVerify(exactly = 2) { getSleepStageBreakdownUseCase() }
    }

    @Test
    fun `a failing sleep stage read is reported`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { getSleepStageBreakdownUseCase(any()) } throws IllegalStateException("thiếu quyền")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Lỗi đọc chi tiết giấc ngủ phải hiện ra, không nuốt im lặng",
            state.visibleError!!.contains("thiếu quyền")
        )
        // Luồng dữ liệu chính vẫn đọc được → lỗi của nó không bị lẫn với lỗi đọc chi tiết giấc ngủ
        assertNull(state.error)
        assertEquals(SleepStageBreakdown(), state.sleepStages)
    }

    private fun createViewModel() = InsightsViewModel(
        getHealthInsightsUseCase = getHealthInsightsUseCase,
        getVitalityScoreUseCase = getVitalityScoreUseCase,
        getWeeklyConsistencyUseCase = getWeeklyConsistencyUseCase,
        getSleepStageBreakdownUseCase = getSleepStageBreakdownUseCase,
        getRecentHealthSummaryUseCase = getRecentHealthSummaryUseCase
    )

    private fun givenSuggestions(vararg suggestions: Suggestion) {
        every { getHealthInsightsUseCase() } returns flowOf(suggestions.toList())
    }

    private fun givenVitality(vitality: VitalityScore) {
        every { getVitalityScoreUseCase() } returns flowOf(vitality)
    }

    private fun givenConsistency(vararg days: DailyConsistency) {
        every { getWeeklyConsistencyUseCase(any()) } returns flowOf(days.toList())
    }

    private fun givenHealthHistory(vararg days: DailyHealthSummary) {
        every { getRecentHealthSummaryUseCase(any()) } returns flowOf(days.toList())
    }

    private fun givenSleepStages(breakdown: SleepStageBreakdown) {
        coEvery { getSleepStageBreakdownUseCase(any()) } returns breakdown
    }
}
