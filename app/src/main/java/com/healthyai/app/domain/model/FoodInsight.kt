package com.healthyai.app.domain.model

/**
 * Kết quả **phân tích món ăn bằng AI** cho màn "Ghi chép bữa ăn" (mục 3 #42).
 *
 * Thay cho kết quả cũ (một danh sách **tên nhãn ảnh** do ML Kit trả về, kèm % tin cậy), model này trả
 * lời đúng câu hỏi người dùng đặt ra: *"ảnh này là món gì"* và *"ăn vào thì lợi/hại gì cho cơ thể"*.
 *
 * Khác biệt về bản chất so với trước:
 * - **Tên món** do AI (Gemini) đọc từ ảnh, ưu tiên gọi đúng tên tiếng Việt (món Việt/Á), phủ cả trái
 *   cây, đồ uống và đồ ăn vặt — không còn phụ thuộc bộ nhãn ảnh chung của ML Kit (nguồn của lỗi
 *   *"chụp đĩa cơm mà ra pizza"*).
 * - **Lợi ích / điều nên lưu ý** là nội dung do AI sinh, chỉ mang tính **tham khảo**, **không** phải
 *   tư vấn y tế (xem `camera_result_disclaimer` trong `strings.xml`).
 *
 * @property isFood Ảnh có món ăn/đồ uống/trái cây nào không. `false` ⇒ [dishName] rỗng và màn hình
 *   hiện thẻ "Chưa nhận diện được món ăn" thay vì kết quả giả.
 * @property dishName Tên món bằng **tiếng Việt** (ví dụ "Phở bò", "Cơm tấm", "Thanh long").
 * @property emoji Emoji minh hoạ cho món; rỗng khi AI không trả về emoji hợp lệ.
 * @property benefits 1–3 gạch đầu dòng ngắn nói **tác dụng cho cơ thể**.
 * @property cautions 1–3 gạch đầu dòng ngắn nói **điều nên lưu ý / ảnh hưởng không tốt**.
 */
data class FoodInsight(
    val isFood: Boolean = false,
    val dishName: String = "",
    val emoji: String = "",
    val benefits: List<String> = emptyList(),
    val cautions: List<String> = emptyList()
) {
    /**
     * `true` khi AI thật sự **gọi được tên món**.
     *
     * AI có thể trả `is_food = true` nhưng bỏ trống tên món (ảnh mờ, món lạ) — trường hợp đó coi như
     * **chưa nhận diện được**, vì một thẻ kết quả không có tên món thì không nói lên điều gì.
     */
    val hasDish: Boolean
        get() = isFood && dishName.isNotBlank()
}
