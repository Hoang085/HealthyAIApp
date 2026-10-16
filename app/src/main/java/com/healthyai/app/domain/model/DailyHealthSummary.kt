package com.healthyai.app.domain.model

import java.time.LocalDate

/** Tóm tắt sức khỏe một ngày — một dòng thẻ "Lịch sử sức khỏe 7 ngày" (cùng nguồn HealthSyncWorker ghi vào health_data_history). */
data class DailyHealthSummary(
    /** Ngày (theo múi giờ thiết bị) mà số liệu được ghi nhận. */
    val date: LocalDate,
    val steps: Long = 0L,
    val sleepHours: Double = 0.0,
    val heartRate: Double = 0.0
) {
    /** Ngày này có số liệu thật không; false là chưa ghi hàng nào (không phải đi bộ 0 bước), để UI hiện "Chưa có dữ liệu". */
    val hasData: Boolean
        get() = steps > 0L || sleepHours > 0.0 || heartRate > 0.0
}
