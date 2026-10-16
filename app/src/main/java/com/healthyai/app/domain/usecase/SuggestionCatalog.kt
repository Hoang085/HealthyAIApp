package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.SuggestionPriority

/** Kho mẹo cải thiện sức khỏe theo danh mục thói quen — nguồn cho feed gợi ý ở tab Phân tích. */
object SuggestionCatalog {

    /** Nhóm mẹo: nhắm vào vấn đề đang có, hay khuyên duy trì/nâng cao. */
    enum class TipFocus { CONCERN, GENERAL }

    /** Một mẹo: nội dung + danh mục + mức ưu tiên (màu thẻ) + nhãn nút hành động (null nếu chỉ để đọc). */
    data class Tip(
        val focus: TipFocus,
        val category: HabitCategory,
        val priority: SuggestionPriority,
        val title: String,
        val message: String,
        val actionLabel: String? = null
    )

    /** Các danh mục có kho mẹo (giữ đúng thứ tự khai báo của [HabitCategory]). */
    val categories: List<HabitCategory> = HabitCategory.entries.toList()

    /** Mẹo cho danh mục đang có vấn đề; rơi về [generalTips] nếu chưa có mẹo CONCERN. */
    fun concernTips(category: HabitCategory): List<Tip> =
        (tips[category].orEmpty().filter { it.focus == TipFocus.CONCERN })
            .ifEmpty { generalTips(category) }

    /** Mẹo duy trì/nâng cao cho danh mục đang ổn. */
    fun generalTips(category: HabitCategory): List<Tip> =
        tips[category].orEmpty().filter { it.focus == TipFocus.GENERAL }

    private val sleepTips = listOf(
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.SLEEP,
            priority = SuggestionPriority.HIGH,
            title = "🌙 Ngủ sớm hơn 30 phút tối nay",
            message = "Dịch giờ đi ngủ sớm hơn 30 phút là cách dễ nhất để bù lại giấc ngủ đang thiếu. " +
                "Bắt đầu ngay tối nay nhé.",
            actionLabel = "Đặt nhắc nhở đi ngủ"
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.SLEEP,
            priority = SuggestionPriority.MEDIUM,
            title = "🛏️ Làm dịu phòng ngủ",
            message = "Giảm ánh sáng, để phòng mát và tắt màn hình 30 phút trước khi ngủ giúp bạn vào " +
                "giấc nhanh và sâu hơn."
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.SLEEP,
            priority = SuggestionPriority.MEDIUM,
            title = "☕ Ngừng cà phê sau 15:00",
            message = "Caffeine có thể còn trong cơ thể tới 6 giờ. Ngừng đồ uống chứa caffeine sau " +
                "15:00 để tối nay ngủ ngon hơn."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.SLEEP,
            priority = SuggestionPriority.LOW,
            title = "😴 Giữ giờ ngủ đều đặn",
            message = "Đi ngủ và thức dậy cùng một giờ mỗi ngày giúp đồng hồ sinh học ổn định. " +
                "Bạn đang làm tốt, hãy tiếp tục!"
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.SLEEP,
            priority = SuggestionPriority.LOW,
            title = "📵 Rời điện thoại 30 phút trước ngủ",
            message = "Thay vì lướt điện thoại, hãy đọc sách hoặc thư giãn nhẹ để giấc ngủ sâu hơn."
        )
    )

    private val waterTips = listOf(
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.WATER,
            priority = SuggestionPriority.HIGH,
            title = "💧 Uống một ly nước ngay bây giờ",
            message = "Cơ thể bạn đang thiếu nước. Một ly 250ml ngay lúc này là bước khởi đầu dễ nhất.",
            actionLabel = "Ghi nhận uống nước"
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.WATER,
            priority = SuggestionPriority.MEDIUM,
            title = "🍶 Mang theo bình nước",
            message = "Đặt một bình nước trong tầm mắt giúp bạn uống đều đặn hơn suốt cả ngày."
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.WATER,
            priority = SuggestionPriority.MEDIUM,
            title = "⏰ Uống nước theo mốc cố định",
            message = "Uống một ly ngay khi thức dậy và vào mỗi bữa ăn để đủ khoảng 2000ml mỗi ngày."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.WATER,
            priority = SuggestionPriority.LOW,
            title = "💧 Duy trì thói quen uống nước",
            message = "Bạn đang uống đủ nước. Hãy tiếp tục để cơ thể luôn được cấp ẩm tốt."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.WATER,
            priority = SuggestionPriority.LOW,
            title = "🍋 Thêm chút hương vị cho nước",
            message = "Thêm lát chanh hoặc lá bạc hà vào nước giúp bạn thích uống nước hơn."
        )
    )

    private val exerciseTips = listOf(
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.EXERCISE,
            priority = SuggestionPriority.HIGH,
            title = "🚶 Đi bộ 10 phút sau bữa tối",
            message = "Một vòng đi bộ ngắn sau bữa tối là cách nhẹ nhàng để tăng vận động mỗi ngày — " +
                "nhỏ nhưng tạo thói quen lớn!",
            actionLabel = "Ghi nhận vận động"
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.EXERCISE,
            priority = SuggestionPriority.MEDIUM,
            title = "🪜 Tận dụng cầu thang",
            message = "Chọn cầu thang thay vì thang máy vài lần mỗi ngày giúp bạn vận động nhiều hơn " +
                "mà không cần thời gian riêng."
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.EXERCISE,
            priority = SuggestionPriority.MEDIUM,
            title = "🎯 Chia nhỏ mục tiêu vận động",
            message = "Chia mục tiêu mỗi ngày thành các đoạn 10 phút để dễ hoàn thành hơn và đỡ ngại " +
                "bắt đầu."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.EXERCISE,
            priority = SuggestionPriority.LOW,
            title = "🏃 Giữ đà vận động",
            message = "Bạn đang vận động tốt. Hãy duy trì đều đặn để sức khỏe tim mạch luôn ổn định."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.EXERCISE,
            priority = SuggestionPriority.LOW,
            title = "🤸 Thêm vài phút giãn cơ",
            message = "Vài phút giãn cơ sau khi vận động giúp cơ thể phục hồi nhanh và ít mỏi hơn."
        )
    )

    private val nutritionTips = listOf(
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.NUTRITION,
            priority = SuggestionPriority.MEDIUM,
            title = "🥗 Thêm rau vào mỗi bữa",
            message = "Một phần rau xanh trong bữa ăn giúp bạn no lâu và nạp đủ chất xơ.",
            actionLabel = "Ghi nhận dinh dưỡng"
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.NUTRITION,
            priority = SuggestionPriority.MEDIUM,
            title = "🍽️ Ăn chậm, nhai kỹ",
            message = "Ăn chậm giúp bạn cảm nhận độ no tốt hơn và dừng lại vừa đủ."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.NUTRITION,
            priority = SuggestionPriority.LOW,
            title = "🥗 Dinh dưỡng cân bằng",
            message = "Bạn đang chú ý đến bữa ăn của mình. Hãy duy trì đa dạng thực phẩm mỗi ngày."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.NUTRITION,
            priority = SuggestionPriority.LOW,
            title = "🥜 Chọn bữa phụ lành mạnh",
            message = "Thay đồ ngọt bằng trái cây hoặc các loại hạt để bữa phụ bổ dưỡng hơn."
        )
    )

    private val mindfulnessTips = listOf(
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.MINDFULNESS,
            priority = SuggestionPriority.MEDIUM,
            title = "🧘 Thử 5 phút thiền",
            message = "Chỉ 5 phút hít thở sâu cũng giúp giảm căng thẳng và ổn định nhịp tim.",
            actionLabel = "Bắt đầu thiền"
        ),
        Tip(
            focus = TipFocus.CONCERN,
            category = HabitCategory.MINDFULNESS,
            priority = SuggestionPriority.MEDIUM,
            title = "🌬️ Hít thở 4-7-8",
            message = "Hít vào 4 giây, giữ 7 giây rồi thở ra 8 giây — vài vòng là thấy nhẹ người hơn."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.MINDFULNESS,
            priority = SuggestionPriority.LOW,
            title = "🧘 Giữ thói quen tĩnh tâm",
            message = "Bạn đang dành thời gian cho sự tĩnh tâm. Hãy tiếp tục để tinh thần thoải mái hơn."
        ),
        Tip(
            focus = TipFocus.GENERAL,
            category = HabitCategory.MINDFULNESS,
            priority = SuggestionPriority.LOW,
            title = "📖 Viết 3 điều biết ơn",
            message = "Ghi lại 3 điều tốt đẹp mỗi tối giúp tinh thần nhẹ nhàng và dễ ngủ hơn."
        )
    )

    private val tips: Map<HabitCategory, List<Tip>> = buildMap {
        put(HabitCategory.SLEEP, sleepTips)
        put(HabitCategory.WATER, waterTips)
        put(HabitCategory.EXERCISE, exerciseTips)
        put(HabitCategory.NUTRITION, nutritionTips)
        put(HabitCategory.MINDFULNESS, mindfulnessTips)
    }
}
