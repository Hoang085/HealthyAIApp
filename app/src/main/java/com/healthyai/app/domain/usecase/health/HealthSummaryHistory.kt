package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.HealthData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Gom `health_data_history` thành một dòng mỗi ngày trong [days] ngày gần nhất, trả về theo thứ tự cũ → mới. */
object HealthSummaryHistory {

    /** Số ngày mặc định của phần "lịch sử tóm tắt sức khỏe" (khớp cửa sổ 7 ngày mà AI đang phân tích). */
    const val DEFAULT_DAYS = 7

    /** [today] và [zone] truyền vào để test được mà không phụ thuộc giờ máy. */
    fun summarize(
        history: List<HealthData>,
        days: Int = DEFAULT_DAYS,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DailyHealthSummary> {
        val windowDays = days.coerceAtLeast(1)

        val latestRowPerDay = history
            .groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }
            .mapValues { (_, rowsInDay) -> rowsInDay.maxBy { it.recordedAt } }

        return (windowDays - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val row = latestRowPerDay[date]
            DailyHealthSummary(
                date = date,
                steps = row?.steps ?: 0L,
                sleepHours = row?.sleepHours ?: 0.0,
                heartRate = row?.heartRate ?: 0.0
            )
        }
    }
}
