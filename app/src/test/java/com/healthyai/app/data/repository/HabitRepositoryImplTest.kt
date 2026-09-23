package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.widget.HabitWidgetRefresher
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

/**
 * Test cho `HabitRepositoryImpl`:
 *
 * - mốc thời gian của `getHabitLogsForPeriod` — bug cũ `fromTs = now - daysBack ngày` khiến
 *   `daysBack = 0` lọc bằng đúng thời điểm gọi hàm → tiến độ trong ngày về 0 mỗi lần mở lại app.
 *   Mốc đúng phải căn theo **đầu ngày (00:00)**;
 * - xóa mềm thói quen;
 * - **widget màn hình chính phải được vẽ lại** mỗi khi dữ liệu thói quen thay đổi (mục 3 #15 —
 *   TC-WG-04): Glance chỉ đọc DB một lần lúc vẽ và `updatePeriodMillis = 0` nên nếu repository
 *   không gọi `HabitWidgetRefresher.refresh()` thì widget đứng yên ở dữ liệu cũ.
 */
class HabitRepositoryImplTest {

    private val habitDao = mockk<HabitDao>(relaxed = true)
    private val habitLogDao = mockk<HabitLogDao>(relaxed = true)
    private val widgetRefresher = mockk<HabitWidgetRefresher>(relaxed = true)
    private val repository = HabitRepositoryImpl(habitDao, habitLogDao, widgetRefresher)

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

    /**
     * Xóa thói quen là **xóa mềm**: repository phải gọi `softDeleteHabit` (đặt `isActive = 0`),
     * không được xóa hàng khỏi bảng `habits` — nếu không log và lịch sử sẽ mất.
     *
     * Đồng thời phải vẽ lại widget: thói quen vừa xóa biến mất khỏi `getAllActiveHabits()` nên widget
     * phải chuyển sang thói quen còn hoạt động khác (TC-WG-06).
     */
    @Test
    fun `deleteHabit soft deletes through the dao`() = runTest {
        repository.deleteHabit(7L)

        coVerify(exactly = 1) { habitDao.softDeleteHabit(7L) }
        coVerify(exactly = 1) { widgetRefresher.refresh() }
    }

    /** Tạo/sửa thói quen (cùng đi qua `insertHabit`) phải vẽ lại widget (TC-WG-04). */
    @Test
    fun `insertHabit refreshes the home screen widget`() = runTest {
        repository.insertHabit(
            Habit(
                id = 0,
                name = "Uống nước",
                category = HabitCategory.WATER,
                targetValue = 2000f,
                unit = "ml"
            )
        )

        coVerify(exactly = 1) { habitDao.insertHabit(any()) }
        coVerify(exactly = 1) { widgetRefresher.refresh() }
    }

    /** Ghi nhận thói quen cũng là thay đổi dữ liệu (bảng `habit_logs`) → widget phải vẽ lại. */
    @Test
    fun `logHabit refreshes the home screen widget`() = runTest {
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
        coVerify(exactly = 1) { widgetRefresher.refresh() }
    }
}
