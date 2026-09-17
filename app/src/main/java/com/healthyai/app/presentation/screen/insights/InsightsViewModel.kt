package com.healthyai.app.presentation.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.usecase.health.GetHealthInsightsUseCase
import com.healthyai.app.domain.usecase.health.GetVitalityScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val suggestions: List<Suggestion> = emptyList(),
    val vitalityScore: Int = 0,
    val isLoading: Boolean = true,
    val lastAnalyzedDays: Int = 7
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getHealthInsightsUseCase: GetHealthInsightsUseCase,
    private val getVitalityScoreUseCase: GetVitalityScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeHealthData()
    }

    private fun observeHealthData() {
        combine(
            getHealthInsightsUseCase(),
            getVitalityScoreUseCase()
        ) { suggestions, score ->
            _uiState.update { 
                it.copy(
                    suggestions = suggestions,
                    vitalityScore = score,
                    isLoading = false
                )
            }
        }.onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e ->
            _uiState.update { it.copy(isLoading = false) }
            // Log error
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        // Since flows are reactive, manual refresh might not be strictly needed,
        // but we can re-trigger if UI requires it.
    }
}
