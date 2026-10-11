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
    /**
     * Mốc tạo thói quen, đọc từ hàng đang sửa.
     *
     * Phải mang theo để "Cập nhật" ghi lại đúng giá trị cũ: `Habit` mặc định `createdAt = now`, mà
     * danh sách thói quen sắp xếp theo `createdAt DESC` — nếu không giữ, mỗi lần sửa là thói quen
     * nhảy lên đầu danh sách. Ở chế độ thêm mới, giá trị này là thời điểm mở màn (chính là lúc tạo).
     */
    val createdAt: Long = System.currentTimeMillis(),
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    /**
     * Đang ghi thói quen vào DB (tạo mới hoặc cập nhật). UI dùng cờ này để **vô hiệu hoá nút đáy**
     * và hiện vòng xoay; ViewModel dùng nó để **chặn double-submit** (bấm "Tạo thói quen" / "Cập
     * nhật" 2 lần thật nhanh chỉ tạo/cập nhật 1 lần).
     */
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

    /**
     * Lưu thói quen đang tạo/sửa.
     *
     * Kiểm tra lần lượt: đang lưu (chặn double-submit) → tên trống → mục tiêu không hợp lệ →
     * **tên bị trùng** với một thói quen đang hoạt động khác (mục 3 #13). Chỉ khi qua hết mới ghi
     * vào DB. Cờ [AddEditHabitUiState.isSaving] được bật **trước khi** phát coroutine ghi DB nên
     * lần bấm thứ hai luôn nhìn thấy nó, và luôn được tắt ở `finally` (không bị kẹt "đang lưu").
     */
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
            // Tần suất hiện chỉ có "Hàng ngày" (mục 3 #30): mọi logic tiến độ/nhất quán/chỉ số sinh
            // lực đều xét theo ngày. Ghi thẳng DAILY để thói quen cũ lỡ lưu "Hàng tuần"/"Hàng tháng"
            // (chọn được ở bản trước nhưng không có tác dụng) được chuẩn hoá lại khi người dùng lưu.
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

    /**
     * Đã có thói quen **đang hoạt động** khác mang tên này chưa?
     *
     * So sánh sau khi cắt khoảng trắng và **không phân biệt hoa/thường** ("Uống nước" ≈ "uống NƯỚC").
     * [exceptId] là id của chính thói quen đang sửa — nhờ đó giữ nguyên tên cũ khi bấm "Cập nhật"
     * vẫn hợp lệ. Thói quen đã xóa mềm (`isActive = 0`) không được tính, nên tên đã xóa dùng lại được.
     */
    private suspend fun isNameTaken(name: String, exceptId: Long): Boolean {
        val normalized = name.trim()
        return repository.getAllActiveHabits()
            .first()
            .any { it.id != exceptId && it.name.trim().equals(normalized, ignoreCase = true) }
    }

    /**
     * Xóa **mềm** thói quen đang sửa: `HabitDao.softDeleteHabit` chỉ đặt `isActive = 0` nên hàng
     * `habits` và toàn bộ `habit_logs` đã ghi vẫn còn trong DB (Insights vẫn đọc được log này).
     * UI gọi hàm này sau khi người dùng xác nhận ở dialog.
     */
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
