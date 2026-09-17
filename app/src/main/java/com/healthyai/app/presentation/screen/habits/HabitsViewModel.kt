package com.healthyai.app.presentation.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.LogHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.healthyai.app.domain.model.HabitTimeOfDay

data class HabitWithProgress(
    val habit: Habit,
    val progress: Float,
    val isCompleted: Boolean,
)

data class HabitsUiState(
    val habits: List<HabitWithProgress> = emptyList(),
    val filteredHabits: List<HabitWithProgress> = emptyList(),
    val selectedFilter: String = "Tất cả",
    val isLoading: Boolean = true,
    val showLogSuccessFor: Long? = null,
    val error: String? = null
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val logHabitUseCase: LogHabitUseCase,
    private val habitRepository: HabitRepository // Cần lấy logs để tính progress
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
    }

    private fun observeHabits() {
        // Tối ưu hiệu năng: Sử dụng Sequence cho các chuỗi biến đổi dữ liệu lớn
        combine(
            getTodayHabitsUseCase(),
            habitRepository.getHabitLogsForPeriod(0) // 0 ngày trước = hôm nay
        ) { habits, logs ->
            habits.asSequence().map { habit ->
                val totalLogged = logs.asSequence()
                    .filter { it.habitId == habit.id }
                    .sumOf { it.value.toDouble() }
                    .toFloat()
                
                HabitWithProgress(
                    habit = habit,
                    progress = totalLogged,
                    isCompleted = totalLogged >= habit.targetValue
                )
            }.toList()
        }.onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        .onEach { habitsWithProgress ->
            _uiState.update { state ->
                state.copy(
                    habits = habitsWithProgress,
                    filteredHabits = applyFilter(habitsWithProgress, state.selectedFilter),
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredHabits = applyFilter(state.habits, filter)
            )
        }
    }

    private fun applyFilter(habits: List<HabitWithProgress>, filter: String): List<HabitWithProgress> {
        if (filter == "Tất cả") return habits
        
        val timeOfDay = when (filter) {
            "Sáng" -> HabitTimeOfDay.MORNING
            "Chiều" -> HabitTimeOfDay.AFTERNOON
            "Tối" -> HabitTimeOfDay.EVENING
            else -> null
        }
        
        return if (timeOfDay != null) {
            habits.filter { 
                (it.habit.timeOfDay == timeOfDay) || (it.habit.timeOfDay == HabitTimeOfDay.ALL_DAY) 
            }
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
