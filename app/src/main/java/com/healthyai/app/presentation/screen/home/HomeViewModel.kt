package com.healthyai.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val habits: List<Habit> = emptyList(),
    val healthData: HealthData = HealthData(),
    val isHealthConnectAvailable: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
        loadHealthData()
    }

    private fun observeHabits() {
        viewModelScope.launch {
            getTodayHabitsUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { habits -> _uiState.update { it.copy(habits = habits, isLoading = false) } }
        }
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            val available = healthDataRepository.isAvailable()
            _uiState.update { it.copy(isHealthConnectAvailable = available) }
            if (available) {
                runCatching {
                    HealthData(
                        steps = healthDataRepository.getTodaySteps(),
                        sleepHours = healthDataRepository.getTodaySleepHours(),
                        heartRate = healthDataRepository.getRecentHeartRate()
                    )
                }.onSuccess { data ->
                    _uiState.update { it.copy(healthData = data) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = "Không thể đọc dữ liệu sức khỏe: ${e.message}") }
                }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
