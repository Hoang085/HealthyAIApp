package com.healthyai.app.domain.model

/**
 * Domain model đại diện cho một thói quen của người dùng.
 * Đây là model thuần túy, không phụ thuộc vào framework nào (Room, Firebase...).
 */
data class Habit(
    val id: Long = 0,
    val name: String,
    val category: HabitCategory,
    val targetValue: Float,
    val unit: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HabitCategory(val displayName: String, val emoji: String) {
    SLEEP("Giấc ngủ", "😴"),
    WATER("Uống nước", "💧"),
    EXERCISE("Vận động", "🏃"),
    NUTRITION("Dinh dưỡng", "🥗"),
    MINDFULNESS("Thiền định", "🧘")
}
