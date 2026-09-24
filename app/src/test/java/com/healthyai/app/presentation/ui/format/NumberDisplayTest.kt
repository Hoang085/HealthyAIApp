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

    // ── formatStepCount — số bước ở vòng tròn "Bước chân" (mục 3 #23, phụ lục C #32) ──

    @Test
    fun `step counts below one thousand are shown exactly`() {
        // Đi 30–40 bước phải thấy số thật, không còn bị làm tròn thành "0,0k".
        assertEquals("0", formatStepCount(0L))
        assertEquals("7", formatStepCount(7L))
        assertEquals("49", formatStepCount(49L))
        assertEquals("312", formatStepCount(312L))
        assertEquals("999", formatStepCount(999L))
    }

    @Test
    fun `step counts from one thousand use the shortened k form`() {
        assertEquals("1,0k", formatStepCount(1_000L))
        assertEquals("1,0k", formatStepCount(1_049L))
        assertEquals("1,2k", formatStepCount(1_249L))
        assertEquals("2,0k", formatStepCount(1_950L))
        assertEquals("12,3k", formatStepCount(12_345L))
    }

    @Test
    fun `the rounded k form never produces a tenths digit of ten`() {
        // 1 950 + (50) = 2 000 → "2,0k" chứ không phải "1,10k".
        assertEquals("2,0k", formatStepCount(1_950L))
        assertEquals("10,0k", formatStepCount(9_950L))
    }

    @Test
    fun `a negative step count is displayed as zero`() {
        assertEquals("0", formatStepCount(-5L))
    }
}
