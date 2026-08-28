package com.healthyai.app.domain.model

/**
 * Gợi ý cá nhân hóa được tạo ra bởi HabitRuleEngine (on-device AI).
 * Mỗi Suggestion có priority để hiển thị theo độ quan trọng.
 */
data class Suggestion(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val priority: SuggestionPriority,
    val category: HabitCategory,
    val actionLabel: String? = null
)

enum class SuggestionPriority {
    LOW,      // Thông tin thêm
    MEDIUM,   // Nên chú ý
    HIGH,     // Cần hành động sớm
    CRITICAL  // Cảnh báo khẩn cấp
}
