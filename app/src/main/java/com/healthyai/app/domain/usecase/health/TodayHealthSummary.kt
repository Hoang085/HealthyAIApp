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

        /**
         * Ghép ba chỉ số từ nguồn đang chọn trên Trang chủ ([primary]) và các số app tự đo.
         *
         * Đây là **luật dùng chung** của thẻ "Tóm tắt sức khỏe" và widget: nguồn đang chọn ưu tiên
         * trước (Health Connect hoặc bản chụp đã lưu trong app — tuỳ switch), chỉ số nào nguồn đó
         * không có số mới rơi về số app tự đo (bước chân từ cảm biến của máy, giờ ngủ ước lượng,
         * nhịp tim đã ghi trong ngày). Nhờ vậy hai nơi không còn lệch nhau khi cảm biến chỉ đếm
         * được phần đi bộ trong lúc app đang chạy.
         */
        fun build(
            primary: HealthData,
            sensorSteps: Long,
            sensorSleepHours: Double,
            recordedHeartRate: Double
        ): TodayHealthSummary = TodayHealthSummary(
            steps = primary.steps.takeIf { it > 0L } ?: sensorSteps,
            sleepHours = primary.sleepHours.takeIf { it > 0.0 } ?: sensorSleepHours,
            heartRateBpm = primary.heartRate.takeIf { it > 0.0 } ?: recordedHeartRate
        )
    }
}
