package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * UseCase tổng hợp: Kết hợp dữ liệu từ nhiều nguồn để tạo ra Insight cá nhân hóa.
 * Luồng Domain: Repository -> UseCase -> Rule Engine -> Presentation
 */
class GetHealthInsightsUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val healthDataRepository: HealthDataRepository,
    private val ruleEngine: HabitRuleEngine
) {
    /**
     * Lấy danh sách gợi ý sức khỏe dựa trên 7 ngày nhật ký thói quen 
     * và dữ liệu sức khỏe cứng (Health Connect) lưu trong Room.
     */
    operator fun invoke(): Flow<List<Suggestion>> {
        return combine(
            habitRepository.getHabitLogsForPeriod(7),
            healthDataRepository.getRecentHealthHistoryFlow(7)
        ) { habitLogs, healthHistory ->
            ruleEngine.generateSuggestions(habitLogs, healthHistory)
        }
    }
}
