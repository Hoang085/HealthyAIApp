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
    val timeOfDay: HabitTimeOfDay = HabitTimeOfDay.ALL_DAY,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
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

enum class HabitTimeOfDay(val displayName: String) {
    MORNING("Buổi sáng"),
    AFTERNOON("Buổi chiều"),
    EVENING("Buổi tối"),
    ALL_DAY("Cả ngày")
}

enum class HabitFrequency(val displayName: String) {
    DAILY("Hàng ngày"),
    WEEKLY("Hàng tuần"),
    MONTHLY("Hàng tháng")
}
