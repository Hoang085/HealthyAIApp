package com.healthyai.app.domain.model

import java.time.LocalDate

/** Mức hoàn thành thói quen một ngày — nguồn cho biểu đồ "Sự nhất quán hàng tuần" (đạt khi tổng log >= mục tiêu). */
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
