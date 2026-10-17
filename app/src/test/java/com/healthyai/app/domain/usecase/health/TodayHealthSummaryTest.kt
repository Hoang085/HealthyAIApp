package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import org.junit.Assert.assertEquals
import org.junit.Test

/** Test cho [TodayHealthSummary]: nguồn đang chọn trên Trang chủ đi trước, số app tự đo chỉ là dự phòng. */
class TodayHealthSummaryTest {

    /** Bản chụp của nguồn đang chọn (Health Connect khi switch BẬT, dữ liệu đã lưu khi TẮT). */
    private val primary = HealthData(steps = 8_000L, sleepHours = 6.5, heartRate = 74.0)

    @Test
    fun `the selected source wins for steps even when the app counted fewer`() {
        val summary = TodayHealthSummary.build(
            primary = primary,
            sensorSteps = 1_200L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        // Cảm biến chỉ đếm được phần đi bộ trong lúc app đang chạy ⇒ KHÔNG được đè số 8 000 của nguồn đang chọn.
        assertEquals(8_000L, summary.steps)
    }

    @Test
    fun `steps fall back to the app sensor when the selected source has no number`() {
        val summary = TodayHealthSummary.build(
            primary = primary.copy(steps = 0L),
            sensorSteps = 1_200L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(1_200L, summary.steps)
    }

    @Test
    fun `steps are zero when neither source has a number`() {
        val summary = TodayHealthSummary.build(
            primary = HealthData(),
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(0L, summary.steps)
    }

    @Test
    fun `an empty selected source still uses every self measured number`() {
        val summary = TodayHealthSummary.build(
            primary = HealthData(),
            sensorSteps = 4_321L,
            sensorSleepHours = 6.0,
            recordedHeartRate = 70.0
        )

        assertEquals(4_321L, summary.steps)
        assertEquals(6.0, summary.sleepHours, 0.001)
        assertEquals(70.0, summary.heartRateBpm, 0.001)
    }

    @Test
    fun `the selected source wins for sleep and heart rate too`() {
        val summary = TodayHealthSummary.build(
            primary = primary,
            sensorSteps = 0L,
            sensorSleepHours = 7.5,
            recordedHeartRate = 68.0
        )

        assertEquals(6.5, summary.sleepHours, 0.001)
        assertEquals(74.0, summary.heartRateBpm, 0.001)
    }

    @Test
    fun `sleep and heart rate fall back to the numbers the app measured`() {
        val summary = TodayHealthSummary.build(
            primary = HealthData(),
            sensorSteps = 0L,
            sensorSleepHours = 7.5,
            recordedHeartRate = 68.0
        )

        assertEquals(7.5, summary.sleepHours, 0.001)
        assertEquals(68.0, summary.heartRateBpm, 0.001)
    }

    @Test
    fun `missing numbers are reported as zero so the widget can show a placeholder`() {
        val summary = TodayHealthSummary.build(
            primary = HealthData(),
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(0L, summary.steps)
        assertEquals(0.0, summary.sleepHours, 0.001)
        assertEquals(0.0, summary.heartRateBpm, 0.001)
    }
}
