package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test cho [TodayHealthSummary] — ba chỉ số hiển thị trên widget màn hình chính.
 *
 * Điểm cần chốt là **thứ tự ưu tiên nguồn**, vì widget và thẻ "Tóm tắt sức khỏe" ở màn Trang chủ phải
 * luôn nói cùng một con số (xem KDoc của `TodayHealthSummary`):
 *
 * - bước chân ưu tiên **cảm biến của máy** khi cảm biến đã sẵn sàng;
 * - giấc ngủ / nhịp tim ưu tiên **Health Connect**, chỉ rơi về nguồn tự đo khi Health Connect trống.
 */
class TodayHealthSummaryTest {

    private val healthConnect = HealthData(steps = 8_000L, sleepHours = 6.5, heartRate = 74.0)

    @Test
    fun `steps come from the device sensor while the sensor is ready`() {
        val summary = TodayHealthSummary.build(
            healthConnect = healthConnect,
            sensorStepsReady = true,
            sensorSteps = 1_200L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        // Cảm biến đã sẵn sàng ⇒ KHÔNG lấy 8 000 bước của Health Connect dù nó lớn hơn.
        assertEquals(1_200L, summary.steps)
    }

    @Test
    fun `steps fall back to health connect when the sensor is not ready`() {
        val summary = TodayHealthSummary.build(
            healthConnect = healthConnect,
            sensorStepsReady = false,
            sensorSteps = 1_200L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(8_000L, summary.steps)
    }

    @Test
    fun `steps are zero when neither source has a number`() {
        val summary = TodayHealthSummary.build(
            healthConnect = null,
            sensorStepsReady = false,
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(0L, summary.steps)
    }

    @Test
    fun `sleep prefers health connect and only falls back to the sensors estimate`() {
        val fromHealthConnect = TodayHealthSummary.build(
            healthConnect = healthConnect,
            sensorStepsReady = true,
            sensorSteps = 0L,
            sensorSleepHours = 7.5,
            recordedHeartRate = 0.0
        )
        val fromSensor = TodayHealthSummary.build(
            healthConnect = healthConnect.copy(sleepHours = 0.0),
            sensorStepsReady = true,
            sensorSteps = 0L,
            sensorSleepHours = 7.5,
            recordedHeartRate = 0.0
        )

        assertEquals(6.5, fromHealthConnect.sleepHours, 0.001)
        assertEquals(7.5, fromSensor.sleepHours, 0.001)
    }

    @Test
    fun `heart rate prefers health connect and only falls back to the recorded measurement`() {
        val fromHealthConnect = TodayHealthSummary.build(
            healthConnect = healthConnect,
            sensorStepsReady = true,
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 68.0
        )
        val fromHistory = TodayHealthSummary.build(
            healthConnect = healthConnect.copy(heartRate = 0.0),
            sensorStepsReady = true,
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 68.0
        )

        assertEquals(74.0, fromHealthConnect.heartRateBpm, 0.001)
        assertEquals(68.0, fromHistory.heartRateBpm, 0.001)
    }

    @Test
    fun `a snapshot without health connect still uses both self measured sources`() {
        val summary = TodayHealthSummary.build(
            healthConnect = null,
            sensorStepsReady = true,
            sensorSteps = 4_321L,
            sensorSleepHours = 6.0,
            recordedHeartRate = 70.0
        )

        assertEquals(4_321L, summary.steps)
        assertEquals(6.0, summary.sleepHours, 0.001)
        assertEquals(70.0, summary.heartRateBpm, 0.001)
    }

    @Test
    fun `missing numbers are reported as zero so the widget can show a placeholder`() {
        val summary = TodayHealthSummary.build(
            healthConnect = null,
            sensorStepsReady = false,
            sensorSteps = 0L,
            sensorSleepHours = 0.0,
            recordedHeartRate = 0.0
        )

        assertEquals(0L, summary.steps)
        assertEquals(0.0, summary.sleepHours, 0.001)
        assertEquals(0.0, summary.heartRateBpm, 0.001)
    }
}
