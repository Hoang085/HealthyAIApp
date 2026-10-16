package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Tổng hợp thói quen, nhật ký và dữ liệu sức khỏe để sinh gợi ý cá nhân hóa. */
class GetHealthInsightsUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val healthDataRepository: HealthDataRepository,
    private val ruleEngine: HabitRuleEngine
) {
    /** Sinh gợi ý từ thói quen, 7 ngày nhật ký và lịch sử sức khỏe; cắt lịch sử theo mốc thời gian để không thiếu ngày. */
    operator fun invoke(): Flow<List<Suggestion>> {
        val windowDays = INSIGHT_WINDOW_DAYS
        val zone = ZoneId.systemDefault()
        val firstDay = LocalDate.now(zone).minusDays((windowDays - 1).toLong())
        val fromTs = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            habitRepository.getAllActiveHabits(),
            habitRepository.getHabitLogsForPeriod(windowDays - 1),
            healthDataRepository.getHealthHistorySince(fromTs)
        ) { habits, habitLogs, healthHistory ->
            ruleEngine.generateSuggestions(
                activeHabits = habits,
                recentLogs = habitLogs,
                healthHistory = healthHistory
            )
        }
    }

    private companion object {
        /** Cửa sổ phân tích: 7 ngày gần nhất tính cả hôm nay. */
        const val INSIGHT_WINDOW_DAYS = 7
    }
}
