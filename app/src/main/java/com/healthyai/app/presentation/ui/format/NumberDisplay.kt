package com.healthyai.app.presentation.ui.format

/**
 * Chuỗi hiển thị "sạch" cho **mục tiêu** của thói quen — dùng ở ô "Mục tiêu hàng ngày" của màn Sửa
 * và ở giá trị mặc định của hộp thoại ghi nhận.
 *
 * Bỏ đuôi thập phân khi giá trị là số nguyên (`2000f` → `"2000"` thay vì `"2000.0"`, `8f` → `"8"`)
 * nhưng giữ nguyên phần thập phân khi có (`7.5f` → `"7.5"`).
 *
 * Không dùng `String.format`/`DecimalFormat` vì chúng phụ thuộc locale — máy đặt `vi-VN` sẽ cho ra
 * `"7,5"`, và `toFloatOrNull()` lúc lưu sẽ trả `null` (mục tiêu bị coi là không hợp lệ).
 */
fun formatTargetValue(value: Float): String =
    if (value % 1f == 0f) value.toLong().toString() else value.toString()
