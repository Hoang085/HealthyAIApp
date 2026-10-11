package com.healthyai.app.presentation.screen.habits

/** Kết quả đọc giá trị người dùng nhập trong hộp thoại ghi nhận thói quen. */
sealed interface LogValueResult {
    /** Chuỗi hợp lệ → [value] là số lượng sẽ được ghi nhận. */
    data class Valid(val value: Float) : LogValueResult

    /** Giá trị nhập không hợp lệ → [message] hiện ngay dưới ô nhập và **không** ghi log. */
    data class Invalid(val message: String) : LogValueResult
}

/**
 * Đọc giá trị từ ô "Số lượng" của hộp thoại ghi nhận (mục 3 #12, phụ lục C #11).
 *
 * Trước đây UI gọi thẳng `toFloatOrNull()` rồi bỏ qua khi trả `null`: hộp thoại vẫn đóng, **không**
 * có log nào được ghi và người dùng không biết vì sao. Nay mọi giá trị không dùng được đều trả
 * [LogValueResult.Invalid] để UI giữ hộp thoại mở và hiện thông báo lỗi dưới ô nhập:
 *
 * - rỗng → *"Vui lòng nhập số lượng"*;
 * - không phải số (kể cả `NaN`/`Infinity`) → *"Số lượng phải là số"*;
 * - **bằng 0 hoặc âm** → *"Số lượng phải lớn hơn 0"* (ghi số âm sẽ làm tiến độ trong ngày bị giảm).
 */
fun parseLogValue(input: String): LogValueResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return LogValueResult.Invalid("Vui lòng nhập số lượng")

    val value = trimmed.toFloatOrNull() ?: return LogValueResult.Invalid("Số lượng phải là số")
    if (!value.isFinite()) return LogValueResult.Invalid("Số lượng phải là số")
    if (value <= 0f) return LogValueResult.Invalid("Số lượng phải lớn hơn 0")

    return LogValueResult.Valid(value)
}
