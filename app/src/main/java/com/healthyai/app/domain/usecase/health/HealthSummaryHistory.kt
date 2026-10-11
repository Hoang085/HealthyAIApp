package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.HealthData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Gom lịch sử `health_data_history` thành **một dòng cho mỗi ngày** trong [days] ngày gần nhất (tính cả
 * hôm nay) — dữ liệu cho thẻ "Lịch sử sức khỏe 7 ngày" ở tab Phân tích.
 *
 * Hàm thuần Kotlin (không phụ thuộc Android/Room) nên test được trên JVM bằng danh sách `HealthData`
 * dựng sẵn.
 *
 * Hai quy tắc, giống `HabitRuleEngine.latestRowPerDay` để giao diện và AI không lệch nhau:
 * 1. **Mỗi ngày chỉ lấy hàng mới nhất** (`recordedAt` lớn nhất) — hàng mới nhất trong ngày là hàng đầy
 *    đủ dữ liệu nhất vì luật gộp giữ bước chân lớn nhất và các chỉ số khác mới nhất (phụ lục C #16).
 * 2. **Luôn trả đủ [days] dòng, kể cả ngày không có dữ liệu** (`hasData = false`) — nhờ vậy biểu đồ và
 *    bảng không bị co lại thành vài ngày rời rạc khi máy thiếu nguồn dữ liệu vài hôm.
 *
 * Thứ tự trả về là **cũ → mới** (trái sang phải khi vẽ), cùng chiều với `GetWeeklyConsistencyUseCase`.
 */
object HealthSummaryHistory {

    /** Số ngày mặc định của phần "lịch sử tóm tắt sức khỏe" (khớp cửa sổ 7 ngày mà AI đang phân tích). */
    const val DEFAULT_DAYS = 7

    /**
     * @param history các hàng `health_data_history` trong khoảng ngày cần xem (thứ tự bất kỳ).
     * @param days số ngày cần trả về, tính cả [today]; nhỏ hơn 1 được coi là 1.
     * @param today ngày được coi là "hôm nay" — truyền vào để test được mà không phụ thuộc giờ máy.
     * @param zone múi giờ dùng để quy `recordedAt` ra ngày.
     */
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
