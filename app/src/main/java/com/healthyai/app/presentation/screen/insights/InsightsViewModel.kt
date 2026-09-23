package com.healthyai.app.presentation.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.usecase.health.GetHealthInsightsUseCase
import com.healthyai.app.domain.usecase.health.GetSleepStageBreakdownUseCase
import com.healthyai.app.domain.usecase.health.GetVitalityScoreUseCase
import com.healthyai.app.domain.usecase.habit.GetWeeklyConsistencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val suggestions: List<Suggestion> = emptyList(),
    val vitality: VitalityScore = VitalityScore(),
    val weeklyConsistency: List<DailyConsistency> = emptyList(),
    val sleepStages: SleepStageBreakdown = SleepStageBreakdown(),
    val isLoading: Boolean = true,
    val lastAnalyzedDays: Int = 7
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getHealthInsightsUseCase: GetHealthInsightsUseCase,
    private val getVitalityScoreUseCase: GetVitalityScoreUseCase,
    private val getWeeklyConsistencyUseCase: GetWeeklyConsistencyUseCase,
    private val getSleepStageBreakdownUseCase: GetSleepStageBreakdownUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeHealthData()
        loadSleepStages()
    }

    private fun observeHealthData() {
        combine(
            getHealthInsightsUseCase(),
            getVitalityScoreUseCase(),
            getWeeklyConsistencyUseCase()
        ) { suggestions, vitality, consistency ->
            Triple(suggestions, vitality, consistency)
        }.onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e ->
            _uiState.update { it.copy(isLoading = false) }
            // Log error
        }.onEach { (suggestions, vitality, consistency) ->
            _uiState.update {
                it.copy(
                    suggestions = suggestions,
                    vitality = vitality,
                    weeklyConsistency = consistency,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Health Connect không cung cấp Flow → đọc chi tiết pha giấc ngủ một lần khi mở màn hình
     * (và mỗi lần [refresh]).
     */
    private fun loadSleepStages() {
        viewModelScope.launch {
            val stages = runCatching { getSleepStageBreakdownUseCase() }
                .getOrDefault(SleepStageBreakdown())
            _uiState.update { it.copy(sleepStages = stages) }
        }
    }

    fun refresh() {
        loadSleepStages()
    }
}
