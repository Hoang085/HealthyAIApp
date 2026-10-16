package com.healthyai.app.presentation.screen.habits

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit test cho [parseLogValue]: đọc giá trị ô "Số lượng" của hộp thoại ghi nhận. */
class HabitLogInputTest {

    @Test
    fun `numbers are accepted, whitespace is trimmed`() {
        assertEquals(LogValueResult.Valid(500f), parseLogValue("500"))
        assertEquals(LogValueResult.Valid(7.5f), parseLogValue(" 7.5 "))
        assertEquals(LogValueResult.Valid(2000f), parseLogValue("2000"))
    }

    @Test
    fun `blank input asks for a value`() {
        assertEquals(
            LogValueResult.Invalid("Vui lòng nhập số lượng"),
            parseLogValue("")
        )
        assertEquals(
            LogValueResult.Invalid("Vui lòng nhập số lượng"),
            parseLogValue("   ")
        )
    }

    @Test
    fun `text that is not a number is rejected with a message`() {
        assertEquals(LogValueResult.Invalid("Số lượng phải là số"), parseLogValue("abc"))
        assertEquals(LogValueResult.Invalid("Số lượng phải là số"), parseLogValue("5 lần"))
    }

    @Test
    fun `NaN and infinity are rejected so no invalid log is written`() {
        assertEquals(LogValueResult.Invalid("Số lượng phải là số"), parseLogValue("NaN"))
        assertEquals(LogValueResult.Invalid("Số lượng phải là số"), parseLogValue("Infinity"))
    }

    @Test
    fun `zero and negative values are rejected`() {
        // Ghi số âm/0 sẽ làm tiến độ trong ngày bị giảm hoặc vô nghĩa
        assertEquals(LogValueResult.Invalid("Số lượng phải lớn hơn 0"), parseLogValue("0"))
        assertEquals(LogValueResult.Invalid("Số lượng phải lớn hơn 0"), parseLogValue("0.0"))
        assertEquals(LogValueResult.Invalid("Số lượng phải lớn hơn 0"), parseLogValue("-100"))
        assertEquals(LogValueResult.Invalid("Số lượng phải lớn hơn 0"), parseLogValue(" -0.5 "))
    }
}
