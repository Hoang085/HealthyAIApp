package com.healthyai.app.domain.model

/**
 * Tổng thời lượng giấc ngủ trong khoảng thời gian đang xét, đọc trực tiếp từ Health Connect
 * (`SleepSessionRecord`) chứ không lưu cục bộ nên luôn phản ánh dữ liệu mới nhất của thiết bị.
 *
 * Có **hai mức chi tiết** vì không phải thiết bị nào cũng chia pha giấc ngủ (phụ lục C #25):
 *
 * - [deepMinutes] / [lightMinutes] / [remMinutes]: tổng thời lượng từng **pha** — chỉ có khi thiết bị
 *   ghi được `STAGE_TYPE_DEEP/LIGHT/REM` (thường là đồng hồ đeo tay).
 * - [unstagedMinutes]: tổng thời lượng của các **phiên ngủ không chia pha** — máy chỉ báo
 *   `STAGE_TYPE_SLEEPING`. Trước đây những phiên này bị bỏ qua hoàn toàn nên thẻ "Xu hướng chất lượng
 *   giấc ngủ" hiện trạng thái rỗng dù Health Connect **có** dữ liệu ngủ.
 */
data class SleepStageBreakdown(
    val deepMinutes: Long = 0,
    val lightMinutes: Long = 0,
    val remMinutes: Long = 0,
    val unstagedMinutes: Long = 0
) {
    /** Tổng thời lượng **đã chia pha** (Sâu + Nông + REM). */
    val totalMinutes: Long
        get() = deepMinutes + lightMinutes + remMinutes

    /** `true` khi thiết bị có chia pha giấc ngủ (có số liệu Sâu/Nông/REM để vẽ thanh chi tiết). */
    val hasStages: Boolean
        get() = totalMinutes > 0L

    /**
     * `true` khi có **bất kỳ** dữ liệu ngủ nào từ Health Connect — chia pha hoặc chỉ tổng thời lượng.
     * Dùng để phân biệt "máy không chia pha" (vẫn có số liệu) với "không có dữ liệu ngủ".
     */
    val hasData: Boolean
        get() = hasStages || unstagedMinutes > 0L
}
