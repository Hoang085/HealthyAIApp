package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * UseCase lấy điểm Vitality hiện tại dựa trên nhật ký 7 ngày gần nhất.
 * Đảm bảo UI luôn cập nhật điểm số theo thời gian thực (Reactive).
 */
class GetVitalityScoreUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val ruleEngine: HabitRuleEngine
) {
    operator fun invoke(): Flow<Int> {
        return habitRepository.getHabitLogsForPeriod(7).map { logs ->
            ruleEngine.calculateVitalityScore(logs)
        }
    }
}
