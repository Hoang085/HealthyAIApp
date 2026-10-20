package com.healthyai.app.presentation.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.SleepTrend
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.usecase.health.GetHealthInsightsUseCase
import com.healthyai.app.domain.usecase.health.GetRecentHealthSummaryUseCase
import com.healthyai.app.domain.usecase.health.GetSleepStageBreakdownUseCase
import com.healthyai.app.domain.usecase.health.GetVitalityScoreUseCase
import com.healthyai.app.domain.usecase.health.SleepTrendMath
import com.healthyai.app.domain.usecase.habit.GetWeeklyConsistencyUseCase
import com.healthyai.app.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "InsightsViewModel"

data class InsightsUiState(
    val suggestions: List<Suggestion> = emptyList(),
    val vitality: VitalityScore = VitalityScore(),
    val weeklyConsistency: List<DailyConsistency> = emptyList(),
    val sleepStages: SleepStageBreakdown = SleepStageBreakdown(),
    /** Xu hướng giờ ngủ 7 đêm gần nhất (cũ → mới), dựng từ healthHistory nên thẻ hoạt động trên mọi máy. */
    val sleepTrend: SleepTrend = SleepTrend(),
    /** Lịch sử tóm tắt sức khỏe 7 ngày gần nhất (cũ → mới, luôn đủ 7 dòng). */
    val healthHistory: List<DailyHealthSummary> = emptyList(),
    val isLoading: Boolean = true,
    val lastAnalyzedDays: Int = 7,
    /** Lỗi đọc luồng dữ liệu chính; màn Phân tích hiện thẻ lỗi đỏ thay vì im lặng, xoá khi đọc được dữ liệu trở lại. */
    val error: String? = null,
    /** Lỗi đọc chi tiết pha giấc ngủ, tách khỏi error vì là phép đọc một lần không thuộc luồng dữ liệu chính. */
    val sleepStagesError: String? = null
) {
    /** Lỗi đang hiển thị trên màn hình (ưu tiên lỗi của luồng dữ liệu chính). */
    val visibleError: String?
        get() = error ?: sleepStagesError
}

/** Gói 4 nguồn dữ liệu của màn Phân tích lại để truyền qua `combine` mà không lồng `Triple`. */
private data class InsightsSnapshot(
    val suggestions: List<Suggestion>,
    val vitality: VitalityScore,
    val weeklyConsistency: List<DailyConsistency>,
    val healthHistory: List<DailyHealthSummary>
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getHealthInsightsUseCase: GetHealthInsightsUseCase,
    private val getVitalityScoreUseCase: GetVitalityScoreUseCase,
    private val getWeeklyConsistencyUseCase: GetWeeklyConsistencyUseCase,
    private val getSleepStageBreakdownUseCase: GetSleepStageBreakdownUseCase,
    private val getRecentHealthSummaryUseCase: GetRecentHealthSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeHealthData()
        loadSleepStages()
    }

    private fun observeHealthData() {
        combine(
            getHealthInsightsUseCase(),
            getVitalityScoreUseCase(),
            getWeeklyConsistencyUseCase(),
            getRecentHealthSummaryUseCase()
        ) { suggestions, vitality, consistency, healthHistory ->
            InsightsSnapshot(suggestions, vitality, consistency, healthHistory)
        }.onStart { _uiState.update { it.copy(isLoading = true) } }
        .catch { e ->
            // Huỷ scope không phải lỗi dữ liệu: ném tiếp thay vì hiện thẻ lỗi.
            if (e is CancellationException) throw e

            android.util.Log.e(TAG, "Không đọc được dữ liệu phân tích", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Không đọc được dữ liệu phân tích: ${e.message ?: "lỗi không xác định"}"
                )
            }
        }.onEach { snapshot ->
            _uiState.update {
                it.copy(
                    suggestions = snapshot.suggestions,
                    vitality = snapshot.vitality,
                    weeklyConsistency = snapshot.weeklyConsistency,
                    sleepTrend = SleepTrendMath.fromHealthHistory(snapshot.healthHistory),
                    healthHistory = snapshot.healthHistory,
                    isLoading = false,
                    // Đọc được dữ liệu ⇒ xoá thẻ lỗi cũ (màn hình không giữ lỗi đã hết)
                    error = null
                )
            }
        }.launchIn(viewModelScope)
    }

    /** Health Connect không cung cấp Flow nên đọc chi tiết pha giấc ngủ một lần (và mỗi lần refresh); lỗi được báo ra UI. */
    private fun loadSleepStages() {
        viewModelScope.launch {
            runCatchingCancellable { getSleepStageBreakdownUseCase() }
                .onSuccess { stages ->
                    _uiState.update { it.copy(sleepStages = stages, sleepStagesError = null) }
                }
                .onFailure { e ->
                    android.util.Log.e(TAG, "Không đọc được chi tiết pha giấc ngủ", e)
                    _uiState.update {
                        it.copy(
                            sleepStages = SleepStageBreakdown(),
                            sleepStagesError = "Không đọc được chi tiết giấc ngủ từ Health Connect: " +
                                (e.message ?: "lỗi không xác định")
                        )
                    }
                }
        }
    }

    /** Đọc lại chi tiết pha giấc ngủ (gọi khi màn Phân tích được mở lại). */
    fun refresh() {
        loadSleepStages()
    }

    /** Người dùng đã đọc thẻ lỗi → xoá đi. */
    fun dismissError() = _uiState.update { it.copy(error = null, sleepStagesError = null) }
}
