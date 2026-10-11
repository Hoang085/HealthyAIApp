package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test cho biểu đồ "Sự nhất quán hàng tuần": số thói quen đạt mục tiêu theo từng ngày
 * trong 7 ngày gần nhất (thay cho biểu đồ hard-code trước đây).
 */
class GetWeeklyConsistencyUseCaseTest {

    private val habitRepository = mockk<HabitRepository>()
    private val useCase = GetWeeklyConsistencyUseCase(habitRepository)

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now(zone)

    @Test
    fun `counts completed habits for each of the last seven days`() = runTest {
        givenHabits(
            habit(id = 1L, name = "Uống nước", category = HabitCategory.WATER, target = 2000f),
            habit(id = 2L, name = "Ngủ đủ giấc", category = HabitCategory.SLEEP, target = 8f)
        )
        givenLogs(
            // Hôm nay: nước đạt, ngủ chưa đạt
            log(1L, HabitCategory.WATER, 2000f, today),
            log(2L, HabitCategory.SLEEP, 5f, today),
            // Hôm qua: cả hai đạt
            log(1L, HabitCategory.WATER, 2000f, today.minusDays(1)),
            log(2L, HabitCategory.SLEEP, 8f, today.minusDays(1))
        )

        val result = useCase().first()

        assertEquals(7, result.size)
        assertEquals(today.minusDays(6), result.first().date)
        assertEquals(today, result.last().date)

        val todayRow = result.last()
        assertEquals(1, todayRow.completedHabits)
        assertEquals(2, todayRow.totalHabits)
        assertEquals(0.5f, todayRow.ratio, 0.001f)

        val yesterdayRow = result[result.size - 2]
        assertEquals(2, yesterdayRow.completedHabits)
        assertEquals(1f, yesterdayRow.ratio, 0.001f)

        val oldestRow = result.first()
        assertEquals(0, oldestRow.completedHabits)
        assertEquals(0f, oldestRow.ratio, 0.001f)
    }

    @Test
    fun `aggregates multiple logs of the same habit within a day`() = runTest {
        givenHabits(habit(id = 1L, name = "Uống nước", category = HabitCategory.WATER, target = 2000f))
        givenLogs(
            log(1L, HabitCategory.WATER, 1200f, today),
            log(1L, HabitCategory.WATER, 800f, today)
        )

        val result = useCase().first()

        assertEquals(1, result.last().completedHabits)
        assertEquals(1f, result.last().ratio, 0.001f)
    }

    @Test
    fun `logs of another day do not count towards today`() = runTest {
        givenHabits(habit(id = 1L, name = "Uống nước", category = HabitCategory.WATER, target = 2000f))
        givenLogs(log(1L, HabitCategory.WATER, 2000f, today.minusDays(2)))

        val result = useCase().first()

        assertEquals(0, result.last().completedHabits)
        assertEquals(1, result[result.size - 3].completedHabits)
    }

    @Test
    fun `ratio is zero when there is no habit`() = runTest {
        givenHabits()
        givenLogs(log(1L, HabitCategory.WATER, 2000f, today))

        val result = useCase().first()

        assertEquals(7, result.size)
        assertEquals(0, result.last().totalHabits)
        assertEquals(0f, result.last().ratio, 0.001f)
    }

    @Test
    fun `days before a habit was created do not count it in the denominator`() = runTest {
        // Phụ lục C #27 — mẫu số là snapshot từng ngày: thói quen tạo HÔM QUA chỉ được tính cho hôm qua
        // và hôm nay, không được tính ngược về các ngày trước đó.
        givenHabits(
            habit(
                id = 1L,
                name = "Uống nước",
                category = HabitCategory.WATER,
                target = 2000f,
                createdAt = startOfDay(today.minusDays(1))
            )
        )
        givenLogs(log(1L, HabitCategory.WATER, 2000f, today))

        val result = useCase().first()

        // Hôm nay: thói quen đã tồn tại và đạt mục tiêu
        assertEquals(1, result.last().totalHabits)
        assertEquals(1, result.last().completedHabits)
        // Hôm qua: thói quen đã tồn tại nhưng chưa đạt mục tiêu
        assertEquals(1, result[result.size - 2].totalHabits)
        assertEquals(0, result[result.size - 2].completedHabits)
        // 2 ngày trước trở về trước: thói quen chưa tồn tại → không nằm trong mẫu số
        assertEquals(0, result[result.size - 3].totalHabits)
        assertEquals(0f, result.first().ratio, 0.001f)
    }

    @Test
    fun `a habit created today does not lower the ratio of the previous days`() = runTest {
        // Phụ lục C #27 — chính lỗi đã báo: tạo thói quen mới hôm nay từng làm tụt tỉ lệ của 6 ngày trước
        givenHabits(
            habit(
                id = 1L,
                name = "Uống nước",
                category = HabitCategory.WATER,
                target = 2000f,
                createdAt = startOfDay(today.minusDays(3))
            ),
            habit(
                id = 2L,
                name = "Thiền",
                category = HabitCategory.MINDFULNESS,
                target = 10f,
                createdAt = System.currentTimeMillis()
            )
        )
        givenLogs(log(1L, HabitCategory.WATER, 2000f, today.minusDays(2)))

        val result = useCase().first()

        // Ngày cũ nhất trong cửa sổ (6 ngày trước): chưa có thói quen nào → mẫu số 0
        assertEquals(0, result.first().totalHabits)
        assertEquals(0f, result.first().ratio, 0.001f)
        // 3 ngày trước: thói quen nước vừa được tạo → đã nằm trong mẫu số nhưng chưa có log
        assertEquals(1, result[result.size - 4].totalHabits)
        assertEquals(0, result[result.size - 4].completedHabits)
        // 2 ngày trước: thói quen nước đạt mục tiêu, mẫu số vẫn chỉ 1
        assertEquals(1, result[result.size - 3].completedHabits)
        assertEquals(1f, result[result.size - 3].ratio, 0.001f)
        // Hôm nay: cả 2 thói quen đều đã tồn tại → mẫu số là 2
        assertEquals(2, result.last().totalHabits)
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun givenHabits(vararg habits: Habit) {
        every { habitRepository.getAllActiveHabits() } returns flowOf(habits.toList())
        every { habitRepository.getHabitLogsForPeriod(6) } returns flowOf(emptyList())
    }

    private fun givenLogs(vararg logs: HabitLog) {
        every { habitRepository.getHabitLogsForPeriod(6) } returns flowOf(logs.toList())
    }

    private fun habit(
        id: Long,
        name: String,
        category: HabitCategory,
        target: Float,
        // Mặc định: thói quen đã tồn tại từ trước cửa sổ 7 ngày (test nào cần mốc tạo riêng thì truyền vào)
        createdAt: Long = startOfDay(today.minusDays(6))
    ) = Habit(
        id = id,
        name = name,
        category = category,
        targetValue = target,
        unit = "đơn vị",
        createdAt = createdAt
    )

    /** Mốc 00:00 của [date] theo múi giờ thiết bị — dùng cho `createdAt` của thói quen. */
    private fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun log(habitId: Long, category: HabitCategory, value: Float, date: LocalDate) = HabitLog(
        habitId = habitId,
        habitName = "log",
        category = category,
        value = value,
        unit = "đơn vị",
        targetValueAtTime = value,
        // giữa trưa để chắc chắn nằm trong ngày
        loggedAt = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 60 * 60 * 1000L
    )
}
