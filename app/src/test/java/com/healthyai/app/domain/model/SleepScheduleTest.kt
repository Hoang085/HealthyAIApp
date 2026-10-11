package com.healthyai.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Unit test cho [SleepSchedule] — khung giờ ngủ do người dùng đặt: khung nào "chứa" một thời điểm, khung
 * có vắt qua nửa đêm hay không, và độ dài khung.
 */
class SleepScheduleTest {

    private val night = SleepSchedule(LocalTime.of(23, 0), LocalTime.of(7, 0))
    private val lateShift = SleepSchedule(LocalTime.of(1, 0), LocalTime.of(8, 0))
    private val shortNap = SleepSchedule(LocalTime.of(13, 0), LocalTime.of(13, 45))

    @Test
    fun `a window that starts before midnight and ends after it wraps around`() {
        assertTrue(night.wrapsMidnight)
        assertFalse(lateShift.wrapsMidnight)
        assertFalse(shortNap.wrapsMidnight)
    }

    @Test
    fun `a wrapping window covers late evening and early morning only`() {
        assertFalse("22:59 chưa vào khung", night.contains(LocalTime.of(22, 59)))
        assertTrue("23:00 bắt đầu khung", night.contains(LocalTime.of(23, 0)))
        assertTrue("nửa đêm vẫn trong khung", night.contains(LocalTime.of(0, 0)))
        assertTrue("06:59 còn trong khung", night.contains(LocalTime.of(6, 59)))
        assertFalse("07:00 đã hết khung (nửa mở)", night.contains(LocalTime.of(7, 0)))
        assertFalse("buổi trưa ngoài khung", night.contains(LocalTime.of(12, 0)))
    }

    @Test
    fun `a window inside one day covers only that span`() {
        assertFalse("00:30 chưa vào khung", lateShift.contains(LocalTime.of(0, 30)))
        assertTrue("01:00 bắt đầu khung", lateShift.contains(LocalTime.of(1, 0)))
        assertTrue("07:59 còn trong khung", lateShift.contains(LocalTime.of(7, 59)))
        assertFalse("08:00 đã hết khung", lateShift.contains(LocalTime.of(8, 0)))
        assertFalse("buổi tối ngoài khung", lateShift.contains(LocalTime.of(20, 0)))
    }

    @Test
    fun `duration is counted clockwise from the bedtime to the wake time`() {
        assertEquals("23:00 → 07:00 là 8 giờ", 8L * 60L, night.durationMinutes)
        assertEquals("01:00 → 08:00 là 7 giờ", 7L * 60L, lateShift.durationMinutes)
        assertEquals("13:00 → 13:45 là 45 phút", 45L, shortNap.durationMinutes)
    }

    @Test
    fun `a window with the same bedtime and wake time is invalid`() {
        val empty = SleepSchedule(LocalTime.of(7, 0), LocalTime.of(7, 0))

        assertFalse("Khung rỗng 0 phút thì không dùng được", empty.isValid)
        assertEquals("Khung rỗng được coi là trọn một ngày", 24L * 60L, empty.durationMinutes)
        assertTrue("Mọi khung khác đều hợp lệ", night.isValid)
        assertTrue(lateShift.isValid)
        assertTrue("Khung 1 phút vẫn là lựa chọn của người dùng", SleepSchedule(LocalTime.of(23, 0), LocalTime.of(23, 1)).isValid)
    }

    @Test
    fun `the default window is 23 to 7 until the user changes it`() {
        assertEquals(LocalTime.of(23, 0), SleepSchedule.DEFAULT.bedtime)
        assertEquals(LocalTime.of(7, 0), SleepSchedule.DEFAULT.wakeTime)
        assertTrue(SleepSchedule.DEFAULT.isValid)
    }
}
