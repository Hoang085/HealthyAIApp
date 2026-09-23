package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * UseCase lấy điểm Vitality của **7 ngày gần nhất** kèm **mức thay đổi so với 7 ngày trước đó**.
 *
 * - Điểm số cập nhật realtime theo nhật ký thói quen (Flow từ Room).
 * - `delta = null` khi chưa có log nào ở 7 ngày trước → UI ẩn phần so sánh thay vì hiển thị
 *   một con số cố định.
 */
class GetVitalityScoreUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val ruleEngine: HabitRuleEngine
) {
    operator fun invoke(): Flow<VitalityScore> {
        val zone = ZoneId.systemDefault()

        // 13 ngày trước hôm nay → phủ 14 ngày: 7 ngày tuần này + 7 ngày tuần trước
        return habitRepository.getHabitLogsForPeriod(13).map { logs ->
            val startOfCurrentWeek = LocalDate.now(zone)
                .minusDays(6)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()

            val (currentWeekLogs, previousWeekLogs) = logs.partition { it.loggedAt >= startOfCurrentWeek }

            val score = ruleEngine.calculateVitalityScore(currentWeekLogs)
            val delta = if (previousWeekLogs.isEmpty()) {
                null // chưa đủ dữ liệu tuần trước để so sánh
            } else {
                score - ruleEngine.calculateVitalityScore(previousWeekLogs)
            }

            VitalityScore(score = score, delta = delta)
        }
    }
}
