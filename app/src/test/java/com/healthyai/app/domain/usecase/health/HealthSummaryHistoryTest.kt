package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Unit test cho HealthSummaryHistory.summarize (ngày/múi giờ cố định) — gom lịch sử thành một dòng mỗi ngày. */
class HealthSummaryHistoryTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today: LocalDate = LocalDate.of(2026, 9, 24)

    @Test
    fun `the window always has the requested number of days, oldest first`() {
        val days = HealthSummaryHistory.summarize(emptyList(), days = 7, today = today, zone = zone)

        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.first().date)
        assertEquals(today, days.last().date)
    }

    @Test
    fun `a day with no row at all is empty rather than a day of zeros`() {
        val days = HealthSummaryHistory.summarize(emptyList(), days = 7, today = today, zone = zone)

        val yesterday = days.single { it.date == today.minusDays(1) }
        assertEquals(0L, yesterday.steps)
        assertEquals(0.0, yesterday.sleepHours, 0.001)
        assertEquals(0.0, yesterday.heartRate, 0.001)
        assertFalse("Chưa có hàng nào ⇒ chưa có dữ liệu, không phải '0 bước'", yesterday.hasData)
    }

    @Test
    fun `a row is placed on the day it was recorded`() {
        val history = listOf(row(today.minusDays(2), steps = 6_120L, sleepHours = 7.5, heartRate = 72.0))

        val days = HealthSummaryHistory.summarize(history, days = 7, today = today, zone = zone)

        val day = days.single { it.date == today.minusDays(2) }
        assertEquals(6_120L, day.steps)
        assertEquals(7.5, day.sleepHours, 0.001)
        assertEquals(72.0, day.heartRate, 0.001)
        assertTrue(day.hasData)
        assertEquals("Những ngày khác vẫn trống", 6, days.count { !it.hasData })
    }

    @Test
    fun `the newest row of a day wins because it holds the merged data`() {
        // Chuỗi ghi thật trong ngày: hàng mới nhất là hàng đầy đủ nhất (đã gộp dữ liệu).
        val history = listOf(
            row(today.minusDays(1), steps = 1_200L, at = LocalTime.of(8, 0)),
            row(today.minusDays(1), steps = 4_500L, sleepHours = 6.0, at = LocalTime.of(20, 0)),
            row(today.minusDays(1), steps = 4_500L, sleepHours = 6.0, heartRate = 71.0, at = LocalTime.of(22, 30))
        )

        val day = HealthSummaryHistory.summarize(history, days = 7, today = today, zone = zone)
            .single { it.date == today.minusDays(1) }

        assertEquals(4_500L, day.steps)
        assertEquals(6.0, day.sleepHours, 0.001)
        assertEquals(71.0, day.heartRate, 0.001)
    }

    @Test
    fun `rows older than the window are ignored`() {
        val history = listOf(
            row(today.minusDays(7), steps = 9_999L),
            row(today.minusDays(30), steps = 9_999L)
        )

        val days = HealthSummaryHistory.summarize(history, days = 7, today = today, zone = zone)

        assertTrue("Dữ liệu ngoài 7 ngày không được lọt vào thẻ", days.none { it.hasData })
    }

    @Test
    fun `a row is grouped by the device time zone, not by UTC`() {
        // 23:30 giờ Việt Nam (UTC+7) = 16:30 UTC cùng ngày → phải nằm ở đúng ngày hôm nay
        val history = listOf(row(today, steps = 500L, at = LocalTime.of(23, 30)))

        val day = HealthSummaryHistory.summarize(history, days = 7, today = today, zone = zone)
            .single { it.date == today }

        assertEquals(500L, day.steps)
    }

    @Test
    fun `a shorter window is respected`() {
        val days = HealthSummaryHistory.summarize(emptyList(), days = 3, today = today, zone = zone)

        assertEquals(3, days.size)
        assertEquals(today.minusDays(2), days.first().date)
    }

    @Test
    fun `a day with only one indicator still counts as having data`() {
        val history = listOf(row(today, steps = 0L, sleepHours = 0.0, heartRate = 74.0))

        val day = HealthSummaryHistory.summarize(history, days = 7, today = today, zone = zone)
            .single { it.date == today }

        assertTrue("Chỉ có nhịp tim (lần đo bằng camera) vẫn là một ngày có dữ liệu", day.hasData)
        assertEquals(74.0, day.heartRate, 0.001)
    }

    private fun row(
        date: LocalDate,
        steps: Long = 0L,
        sleepHours: Double = 0.0,
        heartRate: Double = 0.0,
        at: LocalTime = LocalTime.NOON
    ) = HealthData(
        steps = steps,
        sleepHours = sleepHours,
        heartRate = heartRate,
        activeCalories = 0.0,
        recordedAt = date.atTime(at).atZone(zone).toInstant().toEpochMilli()
    )
}
