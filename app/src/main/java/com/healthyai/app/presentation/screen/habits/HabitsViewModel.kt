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

import com.healthyai.app.domain.model.HabitTimeOfDay

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val filteredHabits: List<Habit> = emptyList(),
    val selectedFilter: String = "Tất cả",
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
                .collect { habits -> 
                    _uiState.update { state -> 
                        state.copy(
                            habits = habits, 
                            filteredHabits = applyFilter(habits, state.selectedFilter),
                            isLoading = false
                        ) 
                    } 
                }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredHabits = applyFilter(state.habits, filter)
            )
        }
    }

    private fun applyFilter(habits: List<Habit>, filter: String): List<Habit> {
        if (filter == "Tất cả") return habits
        
        val timeOfDay = when (filter) {
            "Sáng" -> HabitTimeOfDay.MORNING
            "Chiều" -> HabitTimeOfDay.AFTERNOON
            "Tối" -> HabitTimeOfDay.EVENING
            else -> null
        }
        
        return if (timeOfDay != null) {
            habits.filter { it.timeOfDay == timeOfDay || it.timeOfDay == HabitTimeOfDay.ALL_DAY }
        } else {
            habits
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
                        unit = habit.unit,
                        targetValueAtTime = habit.targetValue
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
