package com.healthyai.app.presentation.screen.habits

/** Kết quả đọc giá trị người dùng nhập trong hộp thoại ghi nhận thói quen. */
sealed interface LogValueResult {
    /** Chuỗi hợp lệ → [value] là số lượng sẽ được ghi nhận. */
    data class Valid(val value: Float) : LogValueResult

    /** Giá trị nhập không hợp lệ → [message] hiện dưới ô nhập và không ghi log. */
    data class Invalid(val message: String) : LogValueResult
}

/** Đọc giá trị ô "Số lượng"; mọi giá trị không dùng được trả [LogValueResult.Invalid] để UI giữ hộp thoại mở và báo lỗi. */
fun parseLogValue(input: String): LogValueResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return LogValueResult.Invalid("Vui lòng nhập số lượng")

    val value = trimmed.toFloatOrNull() ?: return LogValueResult.Invalid("Số lượng phải là số")
    if (!value.isFinite()) return LogValueResult.Invalid("Số lượng phải là số")
    if (value <= 0f) return LogValueResult.Invalid("Số lượng phải lớn hơn 0")

    return LogValueResult.Valid(value)
}
