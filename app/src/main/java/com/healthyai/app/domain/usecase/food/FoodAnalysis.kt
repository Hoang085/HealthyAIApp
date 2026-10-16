package com.healthyai.app.domain.usecase.food

import com.healthyai.app.domain.model.FoodInsight

/** Luật thuần Kotlin cho tính năng "phân tích món ăn bằng AI": dựng prompt và làm sạch phản hồi model. */
object FoodAnalysis {

    /** Số gạch đầu dòng tối đa cho mỗi mục ("Có lợi" và "Nên lưu ý"). */
    const val MAX_BULLETS = 3

    /** Độ dài tối đa của một gạch đầu dòng — dài hơn thì cắt, để thẻ kết quả không thành một trang văn. */
    const val MAX_BULLET_LENGTH = 120

    /** Độ dài tối đa của tên món. */
    const val MAX_DISH_NAME_LENGTH = 60

    /** Emoji dùng khi AI không trả về emoji hợp lệ. */
    const val DEFAULT_EMOJI = "🍽️"

    /** Emoji dài hơn mức này gần như chắc chắn là văn bản bị đặt nhầm ô → bỏ, dùng mặc định. */
    private const val MAX_EMOJI_LENGTH = 8

    /** Câu lệnh tiếng Việt gửi Gemini; đầu ra bị ép theo responseSchema nên chỉ cần nêu rõ nội dung mong muốn. */
    fun buildPrompt(): String = """
        Bạn là chuyên gia dinh dưỡng. Hãy xem ảnh và trả lời bằng TIẾNG VIỆT.

        1. Nếu ảnh KHÔNG có gì ăn được (bàn ghế, phong cảnh, màn hình máy tính…): đặt is_food = false,
           để dish_name và emoji rỗng, benefits và cautions là mảng rỗng.
        2. Nếu ảnh có món ăn, đồ uống, trái cây hoặc đồ ăn vặt: đặt is_food = true và:
           - dish_name: tên món NGẮN GỌN bằng tiếng Việt. Ưu tiên tên món Việt Nam/châu Á nếu ảnh hợp
             (ví dụ: cơm tấm, phở bò, bún chả, bánh mì, cháo, cơm rang, canh, món xào, trái cây…).
             Nếu chỉ đoán được gần đúng thì ghi kèm "(có thể)" (ví dụ: "Cơm rang (có thể)") — KHÔNG
             đoán bừa một món khác hẳn.
           - emoji: đúng MỘT emoji minh hoạ món đó.
           - benefits: 1–3 gạch đầu dòng, mỗi gạch KHÔNG quá 120 ký tự, nói món này mang lại gì cho
             cơ thể (chất xơ, đạm, vitamin, năng lượng, tốt cho tiêu hoá…).
           - cautions: 1–3 gạch đầu dòng, mỗi gạch KHÔNG quá 120 ký tự, nói điều nên lưu ý (nhiều dầu
             mỡ, nhiều đường, nhiều muối, nhiều tinh bột, cay, dễ đầy bụng, không phù hợp cho người
             có bệnh nền nào…). Nếu món lành thì có thể nói người ăn kiêng/tiểu đường cần chú ý khẩu phần.
        3. TUYỆT ĐỐI KHÔNG chẩn đoán bệnh, KHÔNG nói tới thuốc hay việc chữa bệnh, KHÔNG khuyên ngừng
           thuốc. Chỉ nêu thông tin dinh dưỡng tham khảo, ngắn gọn, dễ hiểu.
    """.trimIndent()

    /** Làm sạch phản hồi AI thành FoodInsight; trả rỗng khi ảnh không có đồ ăn hoặc thiếu tên món. */
    fun toInsight(
        isFood: Boolean,
        dishName: String?,
        emoji: String?,
        benefits: List<String>?,
        cautions: List<String>?
    ): FoodInsight {
        if (!isFood) return FoodInsight()

        val name = dishName.orEmpty().trim().take(MAX_DISH_NAME_LENGTH)
        if (name.isEmpty()) return FoodInsight()

        return FoodInsight(
            isFood = true,
            dishName = name,
            emoji = sanitizeEmoji(emoji),
            benefits = sanitizeBullets(benefits),
            cautions = sanitizeBullets(cautions)
        )
    }

    /** Emoji hợp lệ của AI, hoặc [DEFAULT_EMOJI] khi thiếu/rỗng/quá dài (khả năng cao là văn bản). */
    private fun sanitizeEmoji(emoji: String?): String {
        val trimmed = emoji.orEmpty().trim()
        return if (trimmed.isEmpty() || trimmed.length > MAX_EMOJI_LENGTH) DEFAULT_EMOJI else trimmed
    }

    /** Chuẩn hoá gạch đầu dòng: cắt khoảng trắng, bỏ mục rỗng, cắt độ dài, bỏ trùng, giới hạn MAX_BULLETS. */
    private fun sanitizeBullets(bullets: List<String>?): List<String> =
        bullets.orEmpty()
            .map { it.trim().take(MAX_BULLET_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_BULLETS)
}
