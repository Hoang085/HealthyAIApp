package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.SleepNight
import com.healthyai.app.domain.model.SleepQuality
import com.healthyai.app.domain.model.SleepTrend

/**
 * Dựng [SleepTrend] (7 đêm) và đánh giá chất lượng giấc ngủ từ **số giờ ngủ mỗi đêm**.
 *
 * Nguồn là danh sách [DailyHealthSummary] (`health_data_history` gom theo ngày) — **cùng nguồn** với
 * thẻ "Lịch sử sức khỏe 7 ngày" và với Rule Engine, nên con số trên thẻ giấc ngủ luôn khớp phần còn
 * lại của màn Phân tích. Nhờ dùng `health_data_history` (đã gồm giờ ngủ do app tự ước lượng từ cảm
 * biến), thẻ giấc ngủ **không còn phụ thuộc** vào việc Health Connect có dữ liệu pha hay không.
 *
 * Hàm thuần Kotlin nên test được trên JVM.
 */
object SleepTrendMath {

    /** Ngủ trung bình từ mức này trở lên (giờ) thì coi là đủ. */
    const val GOOD_MIN_HOURS = 7.0

    /** Ngủ trung bình dưới mức này (giờ) thì coi là thiếu ngủ. */
    const val SHORT_SLEEP_HOURS = 6.0

    /** Ngủ trung bình trên mức này (giờ) thì coi là hơi dài. */
    const val GOOD_MAX_HOURS = 9.0

    /** Dựng xu hướng giấc ngủ từ lịch sử tóm tắt sức khỏe theo ngày (giữ nguyên thứ tự cũ → mới). */
    fun fromHealthHistory(days: List<DailyHealthSummary>): SleepTrend =
        SleepTrend(days.map { SleepNight(date = it.date, hours = it.sleepHours) })

    /** Đánh giá chất lượng theo **số giờ ngủ trung bình mỗi đêm**. */
    fun qualityOf(averageHours: Double): SleepQuality = when {
        averageHours < SHORT_SLEEP_HOURS -> SleepQuality.SHORT
        averageHours < GOOD_MIN_HOURS -> SleepQuality.FAIR
        averageHours <= GOOD_MAX_HOURS -> SleepQuality.GOOD
        else -> SleepQuality.LONG
    }
}
