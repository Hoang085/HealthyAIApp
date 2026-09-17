package com.healthyai.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
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
    private val getHealthDataUseCase: GetHealthDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
        loadHealthData()
    }

    private fun observeHabits() {
        getTodayHabitsUseCase()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            .onEach { habits -> _uiState.update { it.copy(habits = habits, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHealthDataUseCase()
                .onSuccess { data ->
                    _uiState.update { 
                        it.copy(
                            healthData = data, 
                            isHealthConnectAvailable = true,
                            isLoading = false 
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            error = "Không thể đọc dữ liệu sức khỏe: ${e.message}",
                            isHealthConnectAvailable = false,
                            isLoading = false
                        ) 
                    }
                }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
