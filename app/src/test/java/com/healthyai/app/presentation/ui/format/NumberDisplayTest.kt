package com.healthyai.app.presentation.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test cho các hàm định dạng số ở `ui/format/NumberDisplay.kt`.
 *
 * Bối cảnh (mục 3, #11 và #12): trước đây ô mục tiêu ở màn Sửa và giá trị mặc định của hộp thoại
 * ghi nhận dùng `Float.toString()` nên hiện `2000.0`, `8.0` — vừa xấu vừa dễ bị hiểu là số thập
 * phân. Test chốt lại: số nguyên bỏ đuôi `.0`, số thập phân giữ nguyên, và **luôn dùng dấu `.`**
 * (không phụ thuộc locale) để `toFloatOrNull()` đọc lại được.
 *
 * Phần cuối là các định dạng dùng cho **widget màn hình chính "Sức khỏe hôm nay"** — cùng bộ hàm với
 * thẻ "Tóm tắt sức khỏe" ở màn Trang chủ, nhờ vậy hai nơi không lệch nhau về cách viết số.
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

    // ── formatStepsExact — số bước đầy đủ ở ô lớn trên widget ──

    @Test
    fun `exact step counts use a dot as the thousands separator`() {
        assertEquals("0", formatStepsExact(0L))
        assertEquals("312", formatStepsExact(312L))
        assertEquals("1.000", formatStepsExact(1_000L))
        assertEquals("6.250", formatStepsExact(6_250L))
        assertEquals("12.345", formatStepsExact(12_345L))
        assertEquals("1.234.567", formatStepsExact(1_234_567L))
    }

    @Test
    fun `a negative step count is displayed as zero in the exact form too`() {
        assertEquals("0", formatStepsExact(-5L))
    }

    // ── formatSleepDuration — giờ ngủ trên widget và ở thẻ "Tóm tắt sức khỏe" ──

    @Test
    fun `sleep hours are shown as hours and minutes`() {
        assertEquals("7h 30m", formatSleepDuration(7.5))
        assertEquals("8h 0m", formatSleepDuration(8.0))
        assertEquals("0h 0m", formatSleepDuration(0.0))
        assertEquals("6h 15m", formatSleepDuration(6.25))
    }

    @Test
    fun `a negative sleep duration is displayed as zero hours`() {
        assertEquals("0h 0m", formatSleepDuration(-1.5))
    }

    // ── formatHeartRate — nhịp tim trên widget và ở thẻ "Tóm tắt sức khỏe" ──

    @Test
    fun `a heart rate is rounded down to whole beats per minute`() {
        assertEquals("72 bpm", formatHeartRate(72.0))
        assertEquals("72 bpm", formatHeartRate(72.9))
        assertEquals("58 bpm", formatHeartRate(58.4))
    }

    @Test
    fun `an unknown heart rate reads as a placeholder instead of zero bpm`() {
        // "0 bpm" sẽ bị đọc thành "nhịp tim bằng 0", trong khi ý nghĩa thật là hôm nay chưa đo.
        assertEquals("-- bpm", formatHeartRate(0.0))
        assertEquals("-- bpm", formatHeartRate(-3.0))
    }
}
