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
import com.healthyai.app.presentation.ui.format.formatTargetValue
import com.healthyai.app.util.runCatchingCancellable
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
    /** Mốc tạo thói quen; phải giữ đúng giá trị cũ để "Cập nhật" không đẩy thói quen lên đầu danh sách (sắp xếp theo createdAt DESC). */
    val createdAt: Long = System.currentTimeMillis(),
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    /** Đang ghi DB (tạo/cập nhật): UI khoá nút đáy, ViewModel chặn double-submit. */
    val isSaving: Boolean = false,
    /** Đã xóa mềm (ẩn) thói quen — màn hình hiện phản hồi rồi quay lại danh sách. */
    val isDeleted: Boolean = false,
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
                            targetValue = formatTargetValue(h.targetValue),
                            unit = h.unit,
                            timeOfDay = h.timeOfDay,
                            createdAt = h.createdAt,
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

    /** Lưu thói quen: lần lượt kiểm tra đang lưu/tên trống/mục tiêu/tên trùng rồi ghi DB; isSaving bật trước khi ghi và tắt ở finally. */
    fun saveHabit() {
        val state = _uiState.value
        if (state.isSaving) return // Đang lưu → bỏ qua lần bấm thứ hai
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
            // Giữ nguyên mốc tạo của hàng đang sửa để "Cập nhật" không đẩy thói quen lên đầu danh sách.
            createdAt = state.createdAt,
            // Chỉ "Hàng ngày": ghi thẳng DAILY để chuẩn hoá lại thói quen cũ lỡ lưu tuần/tháng (vô tác dụng).
            frequency = HabitFrequency.DAILY
        )

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (isNameTaken(state.name, exceptId = habit.id)) {
                    _uiState.update { it.copy(error = "Tên thói quen đã tồn tại") }
                    return@launch
                }

                runCatchingCancellable {
                    if (state.isEditMode) updateHabitUseCase(habit)
                    else addHabitUseCase(habit)
                }.onSuccess {
                    _uiState.update { it.copy(isSaved = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = "Lỗi khi lưu: ${e.message}") }
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /** Tên đã bị thói quen đang hoạt động khác dùng chưa; cắt khoảng trắng, không phân biệt hoa/thường, bỏ qua chính nó và thói quen đã xoá mềm. */
    private suspend fun isNameTaken(name: String, exceptId: Long): Boolean {
        val normalized = name.trim()
        return repository.getAllActiveHabits()
            .first()
            .any { it.id != exceptId && it.name.trim().equals(normalized, ignoreCase = true) }
    }

    /** Xoá mềm (chỉ đặt isActive = 0): hàng habits và habit_logs vẫn còn cho Insights; UI gọi sau khi người dùng xác nhận. */
    fun deleteHabit() {
        if (habitId == -1L || _uiState.value.isDeleted) return // Chế độ thêm mới / đã xóa rồi
        viewModelScope.launch {
            runCatchingCancellable {
                deleteHabitUseCase(habitId)
            }.onSuccess {
                _uiState.update { it.copy(isDeleted = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi khi xóa: ${e.message}") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
