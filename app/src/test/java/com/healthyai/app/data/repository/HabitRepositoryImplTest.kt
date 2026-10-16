package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Test `HabitRepositoryImpl`: mốc thời gian `getHabitLogsForPeriod` phải căn theo đầu ngày (00:00) và xoá mềm thói quen. */
class HabitRepositoryImplTest {

    private val habitDao = mockk<HabitDao>(relaxed = true)
    private val habitLogDao = mockk<HabitLogDao>(relaxed = true)
    private val repository = HabitRepositoryImpl(habitDao, habitLogDao)

    private val startOfToday: Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    @Test
    fun `getHabitLogsForPeriod with 0 days queries from start of today`() = runTest {
        val fromTimestamp = slot<Long>()
        every { habitLogDao.getLogsFromTimestamp(capture(fromTimestamp)) } returns flowOf(emptyList())

        repository.getHabitLogsForPeriod(0).first()

        assertEquals(startOfToday, fromTimestamp.captured)
        assertTrue(
            "Mốc lọc phải nằm ở đầu ngày, không phải thời điểm hiện tại",
            fromTimestamp.captured <= System.currentTimeMillis()
        )
    }

    @Test
    fun `getHabitLogsForPeriod with 7 days queries from start of day 7 days ago`() = runTest {
        val fromTimestamp = slot<Long>()
        every { habitLogDao.getLogsFromTimestamp(capture(fromTimestamp)) } returns flowOf(emptyList())

        repository.getHabitLogsForPeriod(7).first()

        val expected = LocalDate.now()
            .minusDays(7)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, fromTimestamp.captured)
    }

    @Test
    fun `negative days back is clamped to today`() = runTest {
        val fromTimestamp = slot<Long>()
        every { habitLogDao.getLogsFromTimestamp(capture(fromTimestamp)) } returns flowOf(emptyList())

        repository.getHabitLogsForPeriod(-3).first()

        assertEquals(startOfToday, fromTimestamp.captured)
    }

    /** Xoá thói quen là xoá mềm: phải gọi `softDeleteHabit` (đặt `isActive = 0`), không xoá hàng để giữ log/lịch sử. */
    @Test
    fun `deleteHabit soft deletes through the dao`() = runTest {
        repository.deleteHabit(7L)

        coVerify(exactly = 1) { habitDao.softDeleteHabit(7L) }
    }

    /** Tạo/sửa thói quen (cùng đi qua `insertHabit`) vẫn ghi qua DAO và trả về id của hàng. */    @Test
    fun `insertHabit writes through the dao and returns the new id`() = runTest {
        coEvery { habitDao.insertHabit(any()) } returns 42L

        val id = repository.insertHabit(
            Habit(
                id = 0,
                name = "Uống nước",
                category = HabitCategory.WATER,
                targetValue = 2000f,
                unit = "ml"
            )
        )

        assertEquals(42L, id)
        coVerify(exactly = 1) { habitDao.insertHabit(any()) }
    }

    /** Sửa thói quen phải qua `updateHabit`, không qua REPLACE: REPLACE = DELETE+INSERT sẽ xoá log do FK CASCADE. */
    @Test
    fun `updateHabit updates through the dao and never replaces the row`() = runTest {
        repository.updateHabit(
            Habit(
                id = 5,
                name = "Chạy bộ buổi sáng",
                category = HabitCategory.EXERCISE,
                targetValue = 45f,
                unit = "phút"
            )
        )

        coVerify(exactly = 1) { habitDao.updateHabit(any()) }
        coVerify(exactly = 0) { habitDao.insertHabit(any()) }
    }

    /** Ghi nhận một lần thực hiện thói quen chỉ đi qua bảng `habit_logs`. */
    @Test
    fun `logHabit inserts through the log dao`() = runTest {
        repository.logHabit(
            HabitLog(
                habitId = 1L,
                habitName = "Uống nước",
                category = HabitCategory.WATER,
                value = 250f,
                unit = "ml",
                targetValueAtTime = 2000f
            )
        )

        coVerify(exactly = 1) { habitLogDao.insertLog(any()) }
    }
}
