package com.healthyai.app.presentation.ui.format

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory

/**
 * Chuỗi hiển thị **tiến độ đã ghi / mục tiêu** của một thói quen — dùng chung cho thẻ thói quen ở màn
 * Thói quen (`HabitsScreen`) và từng dòng trên **widget màn hình chính** (`HabitWidget`), nhờ vậy hai
 * nơi không lệch định dạng.
 *
 * Định dạng theo danh mục:
 * - `WATER` — quy đổi sang lít: `1.2L / 2.0L`.
 * - `EXERCISE` — phút, số nguyên: `10 / 30 phút`.
 * - `SLEEP` — giờ, một chữ số thập phân: `5.0 / 8.0 giờ`.
 * - các danh mục khác — số nguyên kèm đơn vị của thói quen: `500 / 2000 ml`.
 */
fun formatHabitProgress(habit: Habit, current: Float): String {
    return when (habit.category) {
        HabitCategory.WATER -> "${"%.1f".format(current)}L / ${"%.1f".format(habit.targetValue / 1000f)}L"
        HabitCategory.EXERCISE -> "${current.toInt()} / ${habit.targetValue.toInt()} phút"
        HabitCategory.SLEEP -> "${"%.1f".format(current)} / ${"%.1f".format(habit.targetValue)} giờ"
        else -> "${current.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}"
    }
}
