package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Test điểm sinh lực ghép từ thói quen + giờ ngủ + bước chân: bỏ thói quen vài ngày thì điểm tụt. */
class GetVitalityScoreUseCaseTest {

    private val habitRepository = mockk<HabitRepository>()
    private val healthDataRepository = mockk<HealthDataRepository>()
    private val useCase = GetVitalityScoreUseCase(habitRepository, healthDataRepository)

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now(zone)

    private val sleepHabit = Habit(
        id = 1L,
        name = "Giấc ngủ",
        category = HabitCategory.SLEEP,
        targetValue = 8f,
        unit = "giờ"
    )

    @Test
    fun `no data at all reports no score`() = runTest {
        given(habits = emptyList(), logs = emptyList(), history = emptyList())

        val result = useCase().first()

        assertFalse(result.hasData)
        assertEquals(0, result.score)
        assertNull(result.delta)
    }

    @Test
    fun `a perfect week scores 100`() = runTest {
        val logs = (6 downTo 0).map { sleepLogAt(value = 8f, date = today.minusDays(it.toLong())) }
        given(habits = listOf(sleepHabit), logs = logs, history = emptyList())

        val result = useCase().first()

        assertTrue(result.hasData)
        assertEquals(100, result.score)
    }

    @Test
    fun `skipping days lowers the score`() = runTest {
        val threeDays = (2 downTo 0).map { sleepLogAt(value = 8f, date = today.minusDays(it.toLong())) }
        given(habits = listOf(sleepHabit), logs = threeDays, history = emptyList())

        val partial = useCase().first()

        given(habits = listOf(sleepHabit), logs = emptyList(), history = emptyList())
        val afterStopping = useCase().first()

        // 3/7 ngày hoàn thành → ~43; bỏ hẳn → 0. Trước đây cả hai đều giữ nguyên ở mức cao.
        assertEquals(43, partial.score)
        assertEquals(0, afterStopping.score)
        assertTrue(partial.score > afterStopping.score)
    }

    @Test
    fun `delta is null when the previous week has no observation`() = runTest {
        given(
            habits = listOf(sleepHabit),
            logs = listOf(sleepLogAt(value = 8f, date = today)),
            history = emptyList()
        )

        val result = useCase().first()

        assertNull("Chưa có quan sát tuần trước thì phải ẩn phần so sánh", result.delta)
    }

    @Test
    fun `delta compares with the previous week when it has observations`() = runTest {
        val water = Habit(
            id = 2L,
            name = "Uống nước",
            category = HabitCategory.WATER,
            targetValue = 2_000f,
            unit = "ml"
        )
        given(
            habits = listOf(sleepHabit, water),
            logs = listOf(
                // tuần này: hôm nay hoàn thành cả 2 → 1/7 ngày → 14
                sleepLogAt(value = 8f, date = today),
                waterLogAt(value = 2_000f, date = today),
                // tuần trước: 1 ngày hoàn thành 1/2 thói quen → 0,5/7 ngày → 7
                sleepLogAt(value = 8f, date = today.minusDays(8))
            ),
            history = emptyList()
        )

        val result = useCase().first()

        assertEquals(14, result.score)
        assertEquals(7, result.delta)
    }

    @Test
    fun `health data alone can score the window`() = runTest {
        // Máy không có thói quen nhưng có bước chân đủ mục tiêu mỗi ngày.
        val history = (6 downTo 0).map { stepsRow(daysAgo = it, steps = 8_000L) }
        given(habits = emptyList(), logs = emptyList(), history = history)

        val result = useCase().first()

        assertTrue(result.hasData)
        assertEquals(100, result.score)
    }

    @Test
    fun `habits and health data combine into the score`() = runTest {
        val logs = (6 downTo 0).map { sleepLogAt(value = 8f, date = today.minusDays(it.toLong())) }
        val history = (6 downTo 0).map {
            fullRow(daysAgo = it, steps = 8_000L, sleepHours = 8.0)
        }
        given(habits = listOf(sleepHabit), logs = logs, history = history)

        val result = useCase().first()

        assertEquals(100, result.score)
    }

    // Helpers

    private fun given(habits: List<Habit>, logs: List<HabitLog>, history: List<HealthData>) {
        every { habitRepository.getAllActiveHabits() } returns flowOf(habits)
        every { habitRepository.getHabitLogsForPeriod(13) } returns flowOf(logs)
        every { healthDataRepository.getHealthHistorySince(any()) } returns flowOf(history)
    }

    private fun sleepLogAt(value: Float, date: LocalDate) = HabitLog(
        habitId = 1L,
        habitName = "Giấc ngủ",
        category = HabitCategory.SLEEP,
        value = value,
        unit = "giờ",
        targetValueAtTime = 8f,
        // giữa trưa để chắc chắn nằm trong ngày
        loggedAt = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 60 * 60 * 1000L
    )

    private fun waterLogAt(value: Float, date: LocalDate) = HabitLog(
        habitId = 2L,
        habitName = "Uống nước",
        category = HabitCategory.WATER,
        value = value,
        unit = "ml",
        targetValueAtTime = 2_000f,
        loggedAt = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 60 * 60 * 1000L
    )

    private fun stepsRow(daysAgo: Int, steps: Long): HealthData = HealthData(
        steps = steps,
        recordedAt = noonOf(daysAgo)
    )

    private fun fullRow(daysAgo: Int, steps: Long, sleepHours: Double): HealthData = HealthData(
        steps = steps,
        sleepHours = sleepHours,
        recordedAt = noonOf(daysAgo)
    )

    private fun noonOf(daysAgo: Int): Long = today.minusDays(daysAgo.toLong())
        .atTime(12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
