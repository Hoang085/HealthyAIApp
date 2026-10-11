package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData

/**
 * Ba chỉ số sức khỏe **của hôm nay** hiển thị trên widget màn hình chính.
 *
 * Quy tắc chọn nguồn **giống hệt** thẻ "Tóm tắt sức khỏe" ở màn Trang chủ (`HomeUiState.displaySteps`
 * / `displaySleepHours` / `displayHeartRate`) để widget và app không bao giờ nói hai số khác nhau:
 *
 * - **Bước chân** — ưu tiên **cảm biến của chính điện thoại** khi đã đủ điều kiện ([sensorStepsReady]),
 *   vì đó là nguồn *live*; chỉ dùng Health Connect khi máy **không có cảm biến** hoặc **chưa được cấp
 *   quyền** `ACTIVITY_RECOGNITION`. Đánh đổi giống màn Trang chủ: khi máy có cảm biến thì số của **đồng
 *   hồ đeo tay** (nằm trong Health Connect) không còn được ưu tiên.
 * - **Giấc ngủ** — ưu tiên **Health Connect** (dữ liệu có thể đến từ đồng hồ đeo tay, và còn có chi tiết
 *   pha Sâu/Nông/REM); chỉ khi Health Connect **không có số** mới dùng ước lượng của app từ cảm biến.
 *   Giấc ngủ của đêm đã qua **không đổi trong ngày** nên việc widget chỉ đọc một lần lúc vẽ không làm
 *   con số bị "đóng băng".
 * - **Nhịp tim** — ưu tiên **Health Connect** (nhịp trung bình từ 00:00 hôm nay); chưa có thì dùng nhịp
 *   tim **ghi trong ngày** ([recordedHeartRate]) — lần đo bằng camera ở màn "Đo nhịp tim".
 *
 * Giá trị `0` nghĩa là **hôm nay chưa có số nào**, không phải "chỉ số bằng 0"; phần hiển thị quyết định
 * cách nói điều đó (xem `HealthWidget`: `--` thay vì `0`) để không bị đọc thành "nhịp tim bằng 0".
 *
 * Hàm thuần, không phụ thuộc Android ⇒ test được trên JVM (`TodayHealthSummaryTest`).
 */
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
         * Ghép ba chỉ số từ **các nguồn thô** mà widget đọc được.
         *
         * @param healthConnect bản chụp hôm nay từ Health Connect, `null` khi Health Connect không khả
         *   dụng hoặc chưa được cấp đủ quyền (lúc đó hai chỉ số giấc ngủ / nhịp tim rơi về nguồn tự đo).
         * @param sensorStepsReady cảm biến đếm bước của máy đã sẵn sàng chưa (có cảm biến **và** đã được
         *   cấp quyền) — quyết định bước chân lấy từ cảm biến hay từ [healthConnect].
         * @param sensorSteps số bước hôm nay do app tự đếm; chỉ dùng khi [sensorStepsReady].
         * @param sensorSleepHours số giờ ngủ do app tự ước lượng từ cảm biến; chỉ dùng khi Health Connect
         *   không có số.
         * @param recordedHeartRate nhịp tim ghi trong `health_data_history` của hôm nay; chỉ dùng khi
         *   Health Connect không có số.
         */
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
