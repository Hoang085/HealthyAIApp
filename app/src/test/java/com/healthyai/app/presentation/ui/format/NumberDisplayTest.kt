package com.healthyai.app.presentation.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test cho [formatTargetValue] — chuỗi hiển thị mục tiêu thói quen.
 *
 * Bối cảnh (mục 3, #11 và #12): trước đây ô mục tiêu ở màn Sửa và giá trị mặc định của hộp thoại
 * ghi nhận dùng `Float.toString()` nên hiện `2000.0`, `8.0` — vừa xấu vừa dễ bị hiểu là số thập
 * phân. Test chốt lại: số nguyên bỏ đuôi `.0`, số thập phân giữ nguyên, và **luôn dùng dấu `.`**
 * (không phụ thuộc locale) để `toFloatOrNull()` đọc lại được.
 */
class NumberDisplayTest {

    @Test
    fun `whole numbers drop the decimal tail`() {
        assertEquals("2000", formatTargetValue(2000f))
        assertEquals("8", formatTargetValue(8f))
        assertEquals("30", formatTargetValue(30f))
        assertEquals("1", formatTargetValue(1f))
    }

    @Test
    fun `fractional targets keep their decimals`() {
        assertEquals("7.5", formatTargetValue(7.5f))
        assertEquals("0.5", formatTargetValue(0.5f))
        assertEquals("2.25", formatTargetValue(2.25f))
    }

    @Test
    fun `the decimal separator is always a dot so the text can be parsed back`() {
        val formatted = formatTargetValue(7.5f)

        // Màn Sửa lưu bằng `targetValue.toFloatOrNull()` → chuỗi phải luôn dùng dấu "."
        assertEquals(7.5f, formatted.toFloatOrNull())
        assertEquals(2000f, formatTargetValue(2000f).toFloatOrNull())
    }
}
