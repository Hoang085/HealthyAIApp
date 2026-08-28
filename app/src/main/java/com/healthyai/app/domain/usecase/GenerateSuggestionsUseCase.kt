package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.Suggestion
import javax.inject.Inject

/** Use Case: Tạo danh sách gợi ý cá nhân hóa từ Rule Engine */
class GenerateSuggestionsUseCase @Inject constructor(
    private val ruleEngine: HabitRuleEngine
) {
    operator fun invoke(recentLogs: List<HabitLog>): List<Suggestion> =
        ruleEngine.generateSuggestions(recentLogs)
}
