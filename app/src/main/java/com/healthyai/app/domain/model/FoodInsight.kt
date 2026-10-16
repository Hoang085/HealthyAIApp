package com.healthyai.app.domain.model

/** Kết quả AI (Gemini) phân tích món ăn từ ảnh cho màn ghi chép bữa ăn; lợi ích/cảnh báo do AI sinh chỉ mang tính tham khảo, không phải tư vấn y tế. */
data class FoodInsight(
    val isFood: Boolean = false,
    val dishName: String = "",
    val emoji: String = "",
    val benefits: List<String> = emptyList(),
    val cautions: List<String> = emptyList()
) {
    /** `true` khi AI gọi được tên món; is_food = true nhưng tên rỗng vẫn coi là chưa nhận diện được. */
    val hasDish: Boolean
        get() = isFood && dishName.isNotBlank()
}
