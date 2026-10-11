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

/**
 * Yêu cầu **mở sẵn hộp thoại ghi nhận** gửi từ màn khác khi người dùng bấm nút hành động trong thẻ gợi ý
 * AI ở màn Phân tích (mục 3 #14), hoặc nút "Thêm vào nhật ký" ở màn Ghi chép bữa ăn (phụ lục C #22).
 */
sealed interface LogRequest {
    /** Có thói quen đang hoạt động thuộc [category] → mở hộp thoại ghi nhận cho [habit]. */
    data class Open(val habit: Habit) : LogRequest

    /** Chưa có thói quen nào thuộc [category] → nhắc người dùng tạo mới. */
    data class Missing(val category: HabitCategory) : LogRequest
}

/**
 * Khoá trong `SavedStateHandle` của destination Thói quen mà màn Phân tích / màn Ghi chép dùng để gửi
 * danh mục cần ghi nhận sang.
 *
 * Người **đọc** khoá này là `AppNavigation` (chính chỗ có `NavBackStackEntry` đang hiện), rồi mới gọi
 * [HabitsViewModel.onLogRequest]. Trước đây ViewModel tự nghe khoá này bằng `getStateFlow` —
 * nhưng destination Thói quen được giữ trạng thái (`saveState`/`restoreState` ở bottom bar) nên
 * `HabitsViewModel` **không** bị huỷ khi rời tab, trong khi `SavedStateHandle` là của **từng
 * `NavBackStackEntry`**. Khi entry được dựng lại, ViewModel cũ vẫn nghe handle của entry cũ ⇒ giá trị
 * mới ghi vào entry hiện tại **không bao giờ** tới được ViewModel (lỗi KB-45: "nhảy sang tab Thói quen
 * mà không mở hộp thoại nào"). Đọc ở tầng composable thì luôn là entry đang hiện.
 */
const val KEY_LOG_CATEGORY = "logCategory"

data class HabitsUiState(
    val habits: List<HabitWithProgress> = emptyList(),
    val filteredHabits: List<HabitWithProgress> = emptyList(),
    val selectedFilter: HabitFilter = HabitFilter.ALL,
    val isLoading: Boolean = true,
    /**
     * Id của thói quen vừa được ghi nhận thành công — **sự kiện một lần** để UI hiện phản hồi
     * "Đã ghi nhận". UI phải gọi [HabitsViewModel.onLogSuccessShown] sau khi hiện xong, nhờ vậy
     * ghi nhận cùng một thói quen nhiều lần liên tiếp vẫn kích hoạt phản hồi mỗi lần.
     */
    val showLogSuccessFor: Long? = null,
    /**
     * Yêu cầu ghi nhận đến từ màn Phân tích (mục 3 #14) — cũng là **sự kiện một lần**, UI xử lý
     * xong (mở hộp thoại hoặc nhắc tạo mới) thì gọi [HabitsViewModel.onLogRequestHandled].
     */
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

    /**
     * Nhận yêu cầu ghi nhận từ màn khác (mục 3 #14 — nút trong thẻ gợi ý AI; phụ lục C #22 — nút
     * "Thêm vào nhật ký" của màn Ghi chép bữa ăn).
     *
     * [categoryName] là **tên enum** [HabitCategory] do màn gọi gửi sang. Tên không hợp lệ thì bỏ qua.
     * Sau đó tìm thói quen đang hoạt động thuộc danh mục đó: có → [LogRequest.Open] để UI mở sẵn hộp
     * thoại ghi nhận; chưa có → [LogRequest.Missing] để UI nhắc người dùng tạo mới.
     *
     * Ai gọi: `AppNavigation` — chính chỗ đọc được `SavedStateHandle` của `NavBackStackEntry` **đang
     * hiện**. Không thể để ViewModel tự nghe handle đó: xem ghi chú ở [KEY_LOG_CATEGORY].
     */
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

    /**
     * Lọc theo **buổi trong ngày**: thói quen thuộc buổi đang chọn, cộng thói quen "Cả ngày"
     * (`ALL_DAY`) vì chúng không gắn với một buổi cụ thể (phụ lục C #41 — khoá lọc là enum
     * [HabitFilter], không còn là chuỗi tiếng Việt viết cứng).
     */
    private fun applyFilter(
        habits: List<HabitWithProgress>,
        filter: HabitFilter
    ): List<HabitWithProgress> {
        val timeOfDay = filter.timeOfDay ?: return habits

        return habits.filter {
            (it.habit.timeOfDay == timeOfDay) || (it.habit.timeOfDay == HabitTimeOfDay.ALL_DAY)
        }
    }
    /**
     * Ghi nhận [value] cho [habit].
     *
     * Lưu thành công → đặt [HabitsUiState.showLogSuccessFor] để UI hiện phản hồi "Đã ghi nhận"
     * (xem [onLogSuccessShown]). Lưu thất bại → hiện thông báo lỗi (UI hiển thị ở
     * `HabitsScreen` dưới dạng thẻ lỗi đỏ).
     *
     * [value] phải là số **dương hữu hạn**: ghi số âm sẽ làm tiến độ trong ngày bị giảm, còn `NaN`
     * sẽ phá mọi phép tính tổng. Hộp thoại đã chặn trước khi gọi (xem `parseLogValue`), đây là lưới
     * an toàn cho mọi đường vào khác (phụ lục C #11).
     */
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
