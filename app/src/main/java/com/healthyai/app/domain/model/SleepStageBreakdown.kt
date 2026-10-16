package com.healthyai.app.domain.model

/** Tổng thời lượng giấc ngủ trong kỳ đang xét, đọc trực tiếp từ Health Connect; có hai mức chi tiết (chia pha và không chia pha). */
data class SleepStageBreakdown(
    val deepMinutes: Long = 0,
    val lightMinutes: Long = 0,
    val remMinutes: Long = 0,
    val unstagedMinutes: Long = 0
) {
    /** Tổng thời lượng đã chia pha (Sâu + Nông + REM). */
    val totalMinutes: Long
        get() = deepMinutes + lightMinutes + remMinutes

    /** `true` khi thiết bị có chia pha giấc ngủ (có số liệu Sâu/Nông/REM để vẽ thanh chi tiết). */
    val hasStages: Boolean
        get() = totalMinutes > 0L

    /** `true` khi có bất kỳ dữ liệu ngủ nào từ Health Connect — chia pha hoặc chỉ tổng thời lượng. */
    val hasData: Boolean
        get() = hasStages || unstagedMinutes > 0L
}
