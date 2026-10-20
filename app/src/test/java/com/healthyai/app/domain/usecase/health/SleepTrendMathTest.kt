package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.SleepQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Test cho [SleepTrendMath]: dựng xu hướng 7 đêm và đánh giá chất lượng theo số giờ ngủ trung bình. */
class SleepTrendMathTest {

    private val today = LocalDate.of(2026, 9, 28)

    @Test
    fun `builds one night per day keeping oldest to newest order`() {
        val days = (6 downTo 0).map {
            DailyHealthSummary(date = today.minusDays(it.toLong()), sleepHours = 7.0 + it * 0.1)
        }

        val trend = SleepTrendMath.fromHealthHistory(days)

        assertEquals(7, trend.nights.size)
        assertEquals(today.minusDays(6), trend.nights.first().date)
        assertEquals(today, trend.nights.last().date)
        assertTrue(trend.hasData)
    }

    @Test
    fun `average ignores nights without data`() {
        val days = listOf(
            DailyHealthSummary(date = today.minusDays(2), sleepHours = 6.0),
            DailyHealthSummary(date = today.minusDays(1), sleepHours = 0.0), // chưa có dữ liệu
            DailyHealthSummary(date = today, sleepHours = 8.0)
        )

        val trend = SleepTrendMath.fromHealthHistory(days)

        assertEquals(2, trend.nightsWithData.size)
        assertEquals(7.0, trend.averageHours, 0.0001)
        assertFalse(trend.nights[1].hasData)
    }

    @Test
    fun `trend without any sleep hours has no data`() {
        val days = (6 downTo 0).map { DailyHealthSummary(date = today.minusDays(it.toLong())) }

        val trend = SleepTrendMath.fromHealthHistory(days)

        assertFalse(trend.hasData)
        assertEquals(0.0, trend.averageHours, 0.0001)
    }

    @Test
    fun `quality labels follow the recommended range`() {
        assertEquals(SleepQuality.SHORT, SleepTrendMath.qualityOf(5.9))
        assertEquals(SleepQuality.FAIR, SleepTrendMath.qualityOf(6.0))
        assertEquals(SleepQuality.FAIR, SleepTrendMath.qualityOf(6.9))
        assertEquals(SleepQuality.GOOD, SleepTrendMath.qualityOf(7.0))
        assertEquals(SleepQuality.GOOD, SleepTrendMath.qualityOf(9.0))
        assertEquals(SleepQuality.LONG, SleepTrendMath.qualityOf(9.1))
    }
}
