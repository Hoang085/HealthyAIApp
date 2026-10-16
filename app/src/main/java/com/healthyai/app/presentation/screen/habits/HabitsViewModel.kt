package com.healthyai.app.presentation.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase
import com.healthyai.app.domain.usecase.LogHabitUseCase
import com.healthyai.app.util.runCatchingCancellable
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

/** Yêu cầu mở sẵn hộp thoại ghi nhận gửi từ màn khác (thẻ gợi ý AI / nút "Thêm vào nhật ký"). */
sealed interface LogRequest {
    /** Có thói quen đang hoạt động thuộc [category] → mở hộp thoại ghi nhận cho [habit]. */
    data class Open(val habit: Habit) : LogRequest

    /** Chưa có thói quen nào thuộc [category] → nhắc người dùng tạo mới. */
    data class Missing(val category: HabitCategory) : LogRequest
}

/** Khoá SavedStateHandle để màn khác gửi danh mục cần ghi nhận sang; phải đọc ở tầng composable vì handle thuộc từng NavBackStackEntry. */
const val KEY_LOG_CATEGORY = "logCategory"

data class HabitsUiState(
    val habits: List<HabitWithProgress> = emptyList(),
    val filteredHabits: List<HabitWithProgress> = emptyList(),
    val selectedFilter: HabitFilter = HabitFilter.ALL,
    val isLoading: Boolean = true,
    /** Id thói quen vừa ghi nhận — sự kiện một lần; UI gọi [HabitsViewModel.onLogSuccessShown] sau khi hiện xong. */
    val showLogSuccessFor: Long? = null,
    /** Yêu cầu ghi nhận từ màn khác — sự kiện một lần; UI xử lý xong thì gọi [HabitsViewModel.onLogRequestHandled]. */
    val logRequest: LogRequest? = null,
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

    /** Nhận yêu cầu ghi nhận từ màn khác: có thói quen đúng danh mục → LogRequest.Open, chưa có → LogRequest.Missing. */
    fun onLogRequest(categoryName: String) {
        val category = runCatching { HabitCategory.valueOf(categoryName) }.getOrNull()
            ?: return // Tên danh mục không hợp lệ → bỏ qua

        viewModelScope.launch {
            val habit = habitRepository.getAllActiveHabits()
                .first()
                .firstOrNull { it.category == category }

            _uiState.update {
                it.copy(
                    logRequest = if (habit != null) LogRequest.Open(habit)
                    else LogRequest.Missing(category)
                )
            }
        }
    }

    /** UI đã mở hộp thoại / hiện lời nhắc cho yêu cầu ghi nhận → xoá sự kiện một lần. */
    fun onLogRequestHandled() = _uiState.update { it.copy(logRequest = null) }

    fun setFilter(filter: HabitFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredHabits = applyFilter(state.habits, filter)
            )
        }
    }

    /** Lọc theo buổi đang chọn, cộng thói quen "Cả ngày" (ALL_DAY) vì không gắn buổi cụ thể. */
    private fun applyFilter(
        habits: List<HabitWithProgress>,
        filter: HabitFilter
    ): List<HabitWithProgress> {
        val timeOfDay = filter.timeOfDay ?: return habits

        return habits.filter {
            (it.habit.timeOfDay == timeOfDay) || (it.habit.timeOfDay == HabitTimeOfDay.ALL_DAY)
        }
    }
    /** Ghi nhận [value] cho [habit]; [value] phải dương hữu hạn (số âm làm giảm tiến độ, NaN phá phép tính). */
    fun logHabit(habit: Habit, value: Float) {
        if (!value.isFinite() || value <= 0f) {
            _uiState.update { it.copy(error = "Số lượng phải lớn hơn 0") }
            return
        }

        viewModelScope.launch {
            runCatchingCancellable {
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
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi ghi nhận: ${e.message}") }
            }
        }
    }

    /** UI đã hiện phản hồi "Đã ghi nhận" → xoá sự kiện một lần để lần ghi sau lại kích hoạt được. */
    fun onLogSuccessShown() = _uiState.update { it.copy(showLogSuccessFor = null) }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
