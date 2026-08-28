package com.healthyai.app.presentation.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.LogHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true,
    val showLogSuccessFor: Long? = null, // habitId vừa log thành công
    val error: String? = null
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val logHabitUseCase: LogHabitUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayHabitsUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { habits -> _uiState.update { it.copy(habits = habits, isLoading = false) } }
        }
    }

    fun logHabit(habit: Habit, value: Float) {
        viewModelScope.launch {
            runCatching {
                logHabitUseCase(
                    HabitLog(
                        habitId = habit.id,
                        habitName = habit.name,
                        category = habit.category,
                        value = value,
                        unit = habit.unit
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(showLogSuccessFor = habit.id) }
                // Reset sau 2 giây
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(showLogSuccessFor = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi ghi nhận: ${e.message}") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
