package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Điểm Vitality 7 ngày gần nhất kèm mức thay đổi so với 7 ngày trước; delta null khi kỳ trước không có dữ liệu. */
class GetVitalityScoreUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val healthDataRepository: HealthDataRepository
) {
    operator fun invoke(): Flow<VitalityScore> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // 14 ngày: 7 ngày tuần này + 7 ngày tuần trước
        val daysBack = 2 * VitalityScoreMath.WINDOW_DAYS - 1
        val fromTs = today.minusDays(daysBack.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            habitRepository.getAllActiveHabits(),
            habitRepository.getHabitLogsForPeriod(daysBack),
            healthDataRepository.getHealthHistorySince(fromTs)
        ) { habits, logs, healthHistory ->
            val current = VitalityScoreMath.compute(habits, logs, healthHistory, today, zone)
            val previousDay = today.minusDays(VitalityScoreMath.WINDOW_DAYS.toLong())
            val previousHasObservations = VitalityScoreMath.hasObservations(
                habitLogs = logs,
                healthHistory = healthHistory,
                today = previousDay,
                zone = zone
            )
            val delta = if (current != null && previousHasObservations) {
                val previous = VitalityScoreMath.compute(habits, logs, healthHistory, previousDay, zone)
                if (previous != null) current - previous else null
            } else {
                null
            }

            VitalityScore(score = current ?: 0, delta = delta, hasData = current != null)
        }
    }
}
