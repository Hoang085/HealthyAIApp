package com.healthyai.app.domain.model

/**
 * Bản ghi thói quen theo ngày.
 * isDirty = true nghĩa là chưa được đồng bộ lên Firebase cloud.
 */
data class HabitLog(
    val id: Long = 0,
    val habitId: Long,
    val habitName: String,
    val category: HabitCategory,
    val value: Float,
    val unit: String,
    val loggedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val isDirty: Boolean = true // Flag cho offline-first sync strategy
)
