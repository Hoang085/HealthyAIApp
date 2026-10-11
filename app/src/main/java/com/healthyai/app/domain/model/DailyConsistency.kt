package com.healthyai.app.domain.model

import java.time.LocalDate

/**
 * Mức độ hoàn thành thói quen của **một ngày** — nguồn dữ liệu cho biểu đồ "Sự nhất quán hàng tuần".
 *
 * Một thói quen được coi là đạt trong ngày khi tổng giá trị log của nó trong ngày đó
 * >= mục tiêu hiện tại (`Habit.targetValue`).
 */
data class DailyConsistency(
    val date: LocalDate,
    val completedHabits: Int,
    val totalHabits: Int
) {
    /** Tỉ lệ thói quen đạt mục tiêu trong ngày, luôn nằm trong 0f..1f. */
    val ratio: Float
        get() = if (totalHabits <= 0) 0f
        else (completedHabits.toFloat() / totalHabits).coerceIn(0f, 1f)
}
