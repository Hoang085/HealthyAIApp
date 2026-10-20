package com.healthyai.app.domain.model

/** Bản ghi thói quen theo ngày; isDirty = true nghĩa là chưa đồng bộ lên Firebase cloud. */
data class HabitLog(
    val id: Long = 0,
    val habitId: Long,
    val habitName: String,
    val category: HabitCategory,
    val value: Float,
    val unit: String,
    val targetValueAtTime: Float, // Mục tiêu tại thời điểm log, phục vụ phân tích lịch sử
    val loggedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val isDirty: Boolean = true // Cờ cho chiến lược sync offline-first
)
