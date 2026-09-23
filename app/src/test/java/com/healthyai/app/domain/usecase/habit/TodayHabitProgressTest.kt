package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HabitTimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Test cho [TodayHabitProgress] — tiến độ **ngày** hiển thị trên widget màn hình chính
 * (mục 3 #16, #20 — TC-WG-03, TC-WG-10, TC-WG-12).
 *
 * Bug cũ: widget chỉ hiển thị **1 thói quen mới tạo nhất** và vẽ thanh tiến độ **tĩnh ~40%** bằng
 * `Row` chia cứng weight, không liên quan gì tới dữ liệu đã ghi.
 */
class TodayHabitProgressTest {

    // ── Bảng giờ → buổi (quyết định thứ tự hiển thị) ──────────

    @Test
    fun `maps hours to the expected time of day`() {
        assertEquals(HabitTimeOfDay.MORNING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(5, 0)))
        assertEquals(HabitTimeOfDay.MORNING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(11, 59)))
        assertEquals(HabitTimeOfDay.AFTERNOON, TodayHabitProgress.currentTimeOfDay(LocalTime.of(12, 0)))
        assertEquals(HabitTimeOfDay.AFTERNOON, TodayHabitProgress.currentTimeOfDay(LocalTime.of(17, 59)))
        assertEquals(HabitTimeOfDay.EVENING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(18, 0)))
        assertEquals(HabitTimeOfDay.EVENING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(23, 59)))
        assertEquals(HabitTimeOfDay.EVENING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(0, 0)))
        assertEquals(HabitTimeOfDay.EVENING, TodayHabitProgress.currentTimeOfDay(LocalTime.of(4, 59)))
    }

    // ── Thứ tự hiển thị: buổi hiện tại → Cả ngày → còn lại ────

    /**
     * Điểm cốt lõi của quy tắc buổi: widget chỉ hiển thị vài dòng đầu, nên thói quen của **buổi hiện
     * tại** phải lên trước — kể cả khi thói quen khác mới được tạo hơn.
     */
    @Test
    fun `orders habits of the current time of day first`() {
        val newestEvening = habit(1L, "Thiền tối", HabitTimeOfDay.EVENING, createdAt = 9_000L)
        val olderMorning = habit(2L, "Chạy bộ sáng", HabitTimeOfDay.MORNING, createdAt = 1_000L)
        val allDay = habit(3L, "Uống nước", HabitTimeOfDay.ALL_DAY, createdAt = 5_000L)

        val ordered = TodayHabitProgress.orderForWidget(
            listOf(newestEvening, olderMorning, allDay),
            now = LocalTime.of(7, 0)
        )

        assertEquals(listOf(2L, 3L, 1L), ordered.map { it.id })
    }

    @Test
    fun `orders newest first inside each group`() {
        val olderMorning = habit(1L, "Chạy bộ sáng", HabitTimeOfDay.MORNING, createdAt = 1_000L)
        val newerMorning = habit(2L, "Uống nước sáng", HabitTimeOfDay.MORNING, createdAt = 3_000L)

        val ordered = TodayHabitProgress.orderForWidget(
            listOf(olderMorning, newerMorning),
            now = LocalTime.of(8, 0)
        )

        assertEquals(listOf(2L, 1L), ordered.map { it.id })
    }

    // ── Tiến độ thật từ habit_logs hôm nay ────────────────────

    @Test
    fun `sums today logs per habit and marks completion`() {
        val water = habit(1L, "Uống nước", HabitTimeOfDay.ALL_DAY, target = 2000f)
        val run = habit(2L, "Chạy bộ", HabitTimeOfDay.MORNING, target = 30f)
        val meditation = habit(3L, "Thiền", HabitTimeOfDay.EVENING, target = 15f)

        val progress = TodayHabitProgress.build(
            habits = listOf(water, run, meditation),
            logs = listOf(
                log(1L, 1200f), log(1L, 800f), // 2000/2000 → đạt
                log(2L, 10f)                   // 10/30 → chưa đạt
            ),
            now = LocalTime.of(8, 0)
        )

        val byId = progress.lines.associateBy { it.habit.id }
        assertEquals(2000f, byId.getValue(1L).loggedValue, 0.001f)
        assertTrue(byId.getValue(1L).isCompleted)
        assertEquals(10f, byId.getValue(2L).loggedValue, 0.001f)
        assertFalse(byId.getValue(2L).isCompleted)
        // Thói quen chưa ghi lần nào → 0
        assertEquals(0f, byId.getValue(3L).loggedValue, 0.001f)

        assertEquals(1, progress.completedCount)
        assertEquals(3, progress.totalCount)
        assertEquals(1f / 3f, progress.ratio, 0.001f)
    }

    @Test
    fun `all habits completed gives a full ratio`() {
        val water = habit(1L, "Uống nước", HabitTimeOfDay.ALL_DAY, target = 2000f)

        val progress = TodayHabitProgress.build(
            habits = listOf(water),
            logs = listOf(log(1L, 2000f)),
            now = LocalTime.of(8, 0)
        )

        assertEquals(1, progress.completedCount)
        assertEquals(1f, progress.ratio, 0.001f)
    }

    /** Log của thói quen đã xoá mềm (không còn trong danh sách hoạt động) không được tính. */
    @Test
    fun `logs of habits that are no longer active are ignored`() {
        val water = habit(1L, "Uống nước", HabitTimeOfDay.ALL_DAY, target = 2000f)

        val progress = TodayHabitProgress.build(
            habits = listOf(water),
            logs = listOf(log(habitId = 99L, value = 5000f)),
            now = LocalTime.of(8, 0)
        )

        assertEquals(1, progress.totalCount)
        assertEquals(0, progress.completedCount)
        assertEquals(0f, progress.ratio, 0.001f)
    }

    @Test
    fun `no habit gives an empty list and zero ratio`() {
        val progress = TodayHabitProgress.build(emptyList(), emptyList(), now = LocalTime.of(8, 0))

        assertTrue(progress.lines.isEmpty())
        assertEquals(0, progress.totalCount)
        assertEquals(0f, progress.ratio, 0.001f)
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun habit(
        id: Long,
        name: String,
        timeOfDay: HabitTimeOfDay,
        createdAt: Long = 1_000L,
        target: Float = 2000f
    ) = Habit(
        id = id,
        name = name,
        category = HabitCategory.WATER,
        targetValue = target,
        unit = "ml",
        timeOfDay = timeOfDay,
        createdAt = createdAt
    )

    private fun log(habitId: Long, value: Float) = HabitLog(
        habitId = habitId,
        habitName = "log",
        category = HabitCategory.WATER,
        value = value,
        unit = "ml",
        targetValueAtTime = value
    )
}
