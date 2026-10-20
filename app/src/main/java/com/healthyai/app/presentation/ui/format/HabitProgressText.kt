package com.healthyai.app.presentation.ui.format

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory

/** Chuỗi tiến độ đã ghi / mục tiêu của thói quen, định dạng theo danh mục (WATER lít, EXERCISE phút, SLEEP giờ). */
fun formatHabitProgress(habit: Habit, current: Float): String {
    return when (habit.category) {
        HabitCategory.WATER -> "${"%.1f".format(current)}L / ${"%.1f".format(habit.targetValue / 1000f)}L"
        HabitCategory.EXERCISE -> "${current.toInt()} / ${habit.targetValue.toInt()} phút"
        HabitCategory.SLEEP -> "${"%.1f".format(current)} / ${"%.1f".format(habit.targetValue)} giờ"
        else -> "${current.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}"
    }
}
