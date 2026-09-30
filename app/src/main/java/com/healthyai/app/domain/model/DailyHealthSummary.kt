package com.healthyai.app.domain.model

import java.time.LocalDate

/**
 * **Tóm tắt sức khỏe của một ngày** — một dòng của thẻ "Lịch sử sức khỏe 7 ngày" ở tab Phân tích.
 *
 * Đây chính là bản chụp mà `HealthSyncWorker` ghi vào `health_data_history` (bước chân, giờ ngủ, nhịp
 * tim) gom lại **theo ngày**, nên người dùng nhìn và AI ở tab Phân tích phân tích trên **cùng một
 * nguồn số** — không có đường tính riêng nào cho giao diện.
 */
data class DailyHealthSummary(
    /** Ngày (theo múi giờ thiết bị) mà số liệu được ghi nhận. */
    val date: LocalDate,
    val steps: Long = 0L,
    val sleepHours: Double = 0.0,
    val heartRate: Double = 0.0
) {
    /**
     * Ngày này có số liệu thật hay không.
     *
     * `false` nghĩa là **chưa có hàng nào được ghi** cho ngày đó (app chưa kịp đồng bộ, máy không có
     * nguồn dữ liệu nào) — **không** có nghĩa ngày đó người dùng đi bộ 0 bước hay ngủ 0 giờ. UI dựa vào
     * cờ này để hiện "Chưa có dữ liệu" thay vì một loạt số 0 gây hiểu nhầm.
     */
    val hasData: Boolean
        get() = steps > 0L || sleepHours > 0.0 || heartRate > 0.0
}
