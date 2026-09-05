package com.healthyai.app.presentation.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GenerateSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.healthyai.app.domain.usecase.HabitRuleEngine

data class InsightsUiState(
    val suggestions: List<Suggestion> = emptyList(),
    val vitalityScore: Int = 0,
    val isLoading: Boolean = true,
    val lastAnalyzedDays: Int = 7
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val generateSuggestionsUseCase: GenerateSuggestionsUseCase,
    private val ruleEngine: HabitRuleEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init { loadSuggestions() }

    private fun loadSuggestions() {
        viewModelScope.launch {
            habitRepository.getHabitLogsForPeriod(daysBack = 7)
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { logs ->
                    val suggestions = generateSuggestionsUseCase(logs)
                    val score = ruleEngine.calculateVitalityScore(logs)
                    _uiState.update { it.copy(
                        suggestions = suggestions,
                        vitalityScore = score,
                        isLoading = false
                    ) }
                }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadSuggestions()
    }
}
