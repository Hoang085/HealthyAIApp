package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData

/** Ba chỉ số sức khỏe của hôm nay hiển thị trên widget màn hình chính. */
data class TodayHealthSummary(
    /** Số bước chân hôm nay; `0` khi chưa có nguồn nào có số. */
    val steps: Long,
    /** Số giờ ngủ của đêm vừa qua; `0.0` khi chưa có nguồn nào có số. */
    val sleepHours: Double,
    /** Nhịp tim (bpm) của hôm nay; `0.0` khi hôm nay chưa có số nào. */
    val heartRateBpm: Double
) {
    companion object {

        /** Ghép ba chỉ số từ các nguồn thô mà widget đọc được. */
        fun build(
            healthConnect: HealthData?,
            sensorStepsReady: Boolean,
            sensorSteps: Long,
            sensorSleepHours: Double,
            recordedHeartRate: Double
        ): TodayHealthSummary = TodayHealthSummary(
            steps = if (sensorStepsReady) sensorSteps else healthConnect?.steps ?: 0L,
            sleepHours = (healthConnect?.sleepHours ?: 0.0).takeIf { it > 0.0 } ?: sensorSleepHours,
            heartRateBpm = (healthConnect?.heartRate ?: 0.0).takeIf { it > 0.0 } ?: recordedHeartRate
        )
    }
}
