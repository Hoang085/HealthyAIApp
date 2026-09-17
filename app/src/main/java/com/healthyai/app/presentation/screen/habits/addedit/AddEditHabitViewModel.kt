package com.healthyai.app.presentation.screen.habits.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitFrequency
import com.healthyai.app.domain.model.HabitTimeOfDay
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.habit.AddHabitUseCase
import com.healthyai.app.domain.usecase.habit.DeleteHabitUseCase
import com.healthyai.app.domain.usecase.habit.UpdateHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditHabitUiState(
    val id: Long = 0,
    val name: String = "",
    val category: HabitCategory = HabitCategory.WATER,
    val targetValue: String = "",
    val unit: String = "ml",
    val timeOfDay: HabitTimeOfDay = HabitTimeOfDay.ALL_DAY,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val addHabitUseCase: AddHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val repository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditHabitUiState())
    val uiState: StateFlow<AddEditHabitUiState> = _uiState.asStateFlow()

    private val habitId: Long = savedStateHandle.get<Long>("habitId") ?: -1L

    init {
        if (habitId != -1L) {
            loadHabit(habitId)
        }
    }

    private fun loadHabit(id: Long) {
        repository.getAllActiveHabits()
            .take(1) // Chỉ lấy emit đầu tiên
            .map { list -> list.find { it.id == id } }
            .onEach { habit ->
                habit?.let { h ->
                    _uiState.update { 
                        it.copy(
                            id = h.id,
                            name = h.name,
                            category = h.category,
                            targetValue = h.targetValue.toString(),
                            unit = h.unit,
                            timeOfDay = h.timeOfDay,
                            frequency = h.frequency,
                            isEditMode = true
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onCategoryChange(category: HabitCategory) = _uiState.update { 
        it.copy(
            category = category,
            unit = when(category) {
                HabitCategory.WATER -> "ml"
                HabitCategory.SLEEP -> "giờ"
                HabitCategory.EXERCISE -> "phút"
                HabitCategory.NUTRITION -> "calo"
                HabitCategory.MINDFULNESS -> "phút"
            }
        ) 
    }
    fun onTargetValueChange(value: String) = _uiState.update { it.copy(targetValue = value) }
    fun onTimeOfDayChange(time: HabitTimeOfDay) = _uiState.update { it.copy(timeOfDay = time) }
    fun onFrequencyChange(freq: HabitFrequency) = _uiState.update { it.copy(frequency = freq) }

    fun saveHabit() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Tên thói quen không được để trống") }
            return
        }
        val target = state.targetValue.toFloatOrNull() ?: 0f
        if (target <= 0f) {
            _uiState.update { it.copy(error = "Mục tiêu phải là số dương") }
            return
        }

        val habit = Habit(
            id = if (state.isEditMode) state.id else 0,
            name = state.name,
            category = state.category,
            targetValue = target,
            unit = state.unit,
            timeOfDay = state.timeOfDay,
            frequency = state.frequency
        )

        viewModelScope.launch {
            runCatching {
                if (state.isEditMode) updateHabitUseCase(habit)
                else addHabitUseCase(habit)
            }.onSuccess {
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi khi lưu: ${e.message}") }
            }
        }
    }

    fun deleteHabit() {
        if (habitId == -1L) return
        viewModelScope.launch {
            runCatching {
                deleteHabitUseCase(habitId)
            }.onSuccess {
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi khi xóa: ${e.message}") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
