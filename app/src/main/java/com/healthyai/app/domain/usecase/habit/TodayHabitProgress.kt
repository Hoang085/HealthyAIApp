package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HabitTimeOfDay
import java.time.LocalTime

/**
 * Một dòng thói quen trên widget màn hình chính: thói quen + **tổng giá trị đã ghi trong ngày**.
 *
 * `isCompleted` dùng đúng quy tắc của `HomeViewModel` / `HabitsViewModel`: tổng log trong ngày
 * ≥ `targetValue` của thói quen.
 */
data class HabitProgressLine(
    val habit: Habit,
    val loggedValue: Float
) {
    val isCompleted: Boolean get() = loggedValue >= habit.targetValue
}

/**
 * Tiến độ **ngày** cho widget màn hình chính — thay cho thanh tiến độ tĩnh ~40% trước đây
 * (README: "Home screen widget hiển thị tiến độ ngày").
 */
data class DailyHabitProgress(
    val lines: List<HabitProgressLine>
) {
    val completedCount: Int get() = lines.count { it.isCompleted }
    val totalCount: Int get() = lines.size

    /** Tỉ lệ thói quen đạt mục tiêu (0f..1f); `0f` khi chưa có thói quen nào. */
    val ratio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

/**
 * Tính [DailyHabitProgress] từ dữ liệu thô của `HabitRepository` (hàm thuần → test được trên JVM).
 *
 * **Thứ tự hiển thị** giữ tinh thần "theo buổi hiện tại" ở mục 3 #16, nhưng từ bản sửa này widget
 * hiển thị cả **danh sách** thói quen chứ không chọn 1 thói quen nữa — nên buổi chỉ còn quyết định
 * **thứ tự** (quan trọng vì widget chỉ hiển thị tối đa vài dòng):
 * 1. thói quen đúng **buổi hiện tại** (sáng 05:00–11:59, chiều 12:00–17:59, tối 18:00–04:59);
 * 2. thói quen **Cả ngày** (luôn phù hợp mọi buổi);
 * 3. các thói quen còn lại.
 *
 * Trong mỗi nhóm: thói quen **mới tạo nhất** trước (`createdAt` giảm dần).
 */
object TodayHabitProgress {

    /** Buổi của thời điểm [now]: 05:00–11:59 → sáng, 12:00–17:59 → chiều, còn lại → tối. */
    fun currentTimeOfDay(now: LocalTime): HabitTimeOfDay = when (now.hour) {
        in 5..11 -> HabitTimeOfDay.MORNING
        in 12..17 -> HabitTimeOfDay.AFTERNOON
        else -> HabitTimeOfDay.EVENING
    }

    /**
     * Ghép danh sách thói quen đang hoạt động với log **hôm nay** ([logs] lấy từ
     * `HabitRepository.getHabitLogsForPeriod(0)`).
     *
     * Log của thói quen đã xóa mềm (không còn trong [habits]) bị bỏ qua; thói quen chưa ghi lần nào
     * có `loggedValue = 0f`.
     */
    fun build(
        habits: List<Habit>,
        logs: List<HabitLog>,
        now: LocalTime = LocalTime.now()
    ): DailyHabitProgress {
        val loggedByHabit: Map<Long, Float> = logs
            .groupBy { it.habitId }
            .mapValues { (_, entries) -> entries.sumOf { it.value.toDouble() }.toFloat() }

        return DailyHabitProgress(
            lines = orderForWidget(habits, now).map { habit ->
                HabitProgressLine(habit = habit, loggedValue = loggedByHabit[habit.id] ?: 0f)
            }
        )
    }

    /** Xếp [habits] theo thứ tự hiển thị trên widget (xem KDoc của [TodayHabitProgress]). */
    fun orderForWidget(habits: List<Habit>, now: LocalTime = LocalTime.now()): List<Habit> {
        val currentTimeOfDay = currentTimeOfDay(now)
        return habits.sortedWith(
            compareBy<Habit> { priorityOf(it.timeOfDay, currentTimeOfDay) }
                .thenByDescending { it.createdAt }
        )
    }

    private fun priorityOf(timeOfDay: HabitTimeOfDay, currentTimeOfDay: HabitTimeOfDay): Int =
        when (timeOfDay) {
            currentTimeOfDay -> 0
            HabitTimeOfDay.ALL_DAY -> 1
            else -> 2
        }
}
