package com.healthyai.app.domain.model

/**
 * Tổng thời lượng từng pha giấc ngủ (tính bằng phút) trong khoảng thời gian đang xét.
 *
 * Đọc trực tiếp từ Health Connect (`SleepSessionRecord.stages`) chứ không lưu cục bộ,
 * nên giá trị luôn phản ánh dữ liệu mới nhất của thiết bị.
 */
data class SleepStageBreakdown(
    val deepMinutes: Long = 0,
    val lightMinutes: Long = 0,
    val remMinutes: Long = 0
) {
    val totalMinutes: Long
        get() = deepMinutes + lightMinutes + remMinutes

    /** false khi thiết bị không báo chi tiết pha giấc ngủ (hoặc thiếu quyền / không có dữ liệu). */
    val hasData: Boolean
        get() = totalMinutes > 0L
}
