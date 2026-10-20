package com.healthyai.app.presentation.screen.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.HeartRateResult
import com.healthyai.app.domain.usecase.health.SaveMeasuredHeartRateUseCase
import com.healthyai.app.domain.usecase.heartrate.HeartRateMath
import com.healthyai.app.domain.usecase.heartrate.PpgAnalyzer
import com.healthyai.app.domain.usecase.heartrate.PpgProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ba chặng của màn đo nhịp tim. */
enum class HeartRatePhase {
    /** Chưa đo: màn hình đang hướng dẫn cách đặt ngón tay và chờ bấm "Bắt đầu đo". */
    IDLE,

    /** Đang đo: camera + đèn flash đang bật và tín hiệu đang được phân tích. */
    MEASURING,

    /** Đã có kết quả (hoặc đã cố hết thời gian mà không đọc được nhịp). */
    FINISHED
}

/** Câu hướng dẫn phù hợp tình trạng đo; ViewModel chỉ giữ trạng thái nên test được trên JVM. */
enum class HeartRateGuidance {
    /** Chưa đo — đang ở thẻ hướng dẫn 3 bước. */
    READY_TO_START,

    /** Chưa thấy ngón tay che camera/đèn flash (khung hình còn tối). */
    PLACE_FINGER,

    /** Đã thấy ngón tay nhưng ánh sáng dao động quá ít — nhắc ấn vừa phải và giữ yên. */
    WEAK_SIGNAL,

    /** Đang đo bình thường, chưa đủ thời gian tối thiểu. */
    MEASURING,

    /** Đã đủ thời gian tối thiểu nhưng tín hiệu chưa đủ rõ — đang cố thêm cho tới hạn. */
    ALMOST_DONE
}

/** Trạng thái màn "Đo nhịp tim" (giai đoạn, số đo, tín hiệu, kết quả, lỗi). */
data class HeartRateUiState(
    val phase: HeartRatePhase = HeartRatePhase.IDLE,
    val elapsedMs: Long = 0L,
    val beats: Int = 0,
    val liveBpm: Int? = null,
    val quality: Float = 0f,
    val signalStrength: Float = 0f,
    val fingerDetected: Boolean = false,
    val waveform: List<Float> = emptyList(),
    val result: HeartRateResult? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    /** Tiến độ vòng tròn: đã đo được bao phần của thời gian tối thiểu (0..1). */
    val progress: Float
        get() = (elapsedMs.toFloat() / HeartRateMath.MIN_MEASURE_MS).coerceIn(0f, 1f)

    /** Số giây còn lại của thời gian tối thiểu (0 khi đã đủ). */
    val secondsToMinimum: Int
        get() = ((HeartRateMath.MIN_MEASURE_MS - elapsedMs).coerceAtLeast(0L) / 1_000L).toInt()

    /** Câu hướng dẫn phù hợp với tình trạng hiện tại. */
    val guidance: HeartRateGuidance
        get() = when {
            phase != HeartRatePhase.MEASURING -> HeartRateGuidance.READY_TO_START
            !fingerDetected -> HeartRateGuidance.PLACE_FINGER
            signalStrength < MIN_SIGNAL_STRENGTH -> HeartRateGuidance.WEAK_SIGNAL
            elapsedMs < HeartRateMath.MIN_MEASURE_MS -> HeartRateGuidance.MEASURING
            else -> HeartRateGuidance.ALMOST_DONE
        }

    companion object {
        /** Dưới mức này coi là tín hiệu yếu (ấn quá mạnh/chê hờ); màn đo dùng cùng ngưỡng cho thanh "Tín hiệu". */
        const val MIN_SIGNAL_STRENGTH = 0.35f

        /** Trên mức này coi là tín hiệu mạnh (chỉ để hiển thị). */
        const val GOOD_SIGNAL_STRENGTH = 0.7f
    }
}

/** ViewModel màn đo nhịp tim: nhận giá trị đỏ mỗi khung hình, chạy máy trạng thái (test được trên JVM), tự lưu kết quả. */
@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val saveMeasuredHeartRate: SaveMeasuredHeartRateUseCase
) : ViewModel() {

    private val analyzer = PpgAnalyzer()

    private val _uiState = MutableStateFlow(HeartRateUiState())
    val uiState: StateFlow<HeartRateUiState> = _uiState.asStateFlow()

    /** Bắt đầu (hoặc bắt đầu lại) một lần đo: xoá mọi số liệu của lần trước. */
    fun startMeasurement() {
        analyzer.reset()
        _uiState.value = HeartRateUiState(phase = HeartRatePhase.MEASURING)
    }

    /** Người dùng bấm "Dừng đo": bỏ lần đo hiện tại, không lưu gì. */
    fun stopMeasurement() {
        analyzer.reset()
        _uiState.value = HeartRateUiState()
    }

    /** Gọi lại [startMeasurement] — dùng cho nút "Đo lại" ở màn kết quả. */
    fun restart() = startMeasurement()

    /** Nhận giá trị đỏ trung bình một khung hình camera; bỏ qua khi không ở chế độ đang đo. */
    fun onPpgSample(red: Double, timestampMs: Long) {
        if (_uiState.value.phase != HeartRatePhase.MEASURING) return

        val progress = analyzer.addSample(red, timestampMs)
        if (progress.ready || progress.elapsedMs >= HeartRateMath.MAX_MEASURE_MS) {
            finish(progress)
            return
        }

        _uiState.value = _uiState.value.copy(
            elapsedMs = progress.elapsedMs,
            beats = progress.beats,
            liveBpm = progress.bpm,
            quality = progress.quality,
            signalStrength = progress.signalStrength,
            fingerDetected = progress.fingerDetected,
            waveform = progress.waveform
        )
    }

    /** Camera/đèn flash không dùng được (thiếu quyền, đang bị ứng dụng khác chiếm, thiết bị lỗi). */
    fun onCameraFailed(message: String) {
        analyzer.reset()
        _uiState.value = HeartRateUiState(error = "Không mở được camera: $message")
    }

    /** Thiết bị không có đèn flash: vẫn cho đo nhưng cảnh báo trước là kết quả dễ sai; không ghi đè lỗi đang có. */
    fun onFlashUnavailable() = _uiState.update {
        if (it.error == null) it.copy(error = NO_FLASH_WARNING) else it
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** Chốt lần đo: chỉ nhận kết quả khi có nhịp và chất lượng đạt ngưỡng, tránh hiện số sai. */
    private fun finish(progress: PpgProgress) {
        analyzer.reset()

        val bpm = progress.bpm
        val reliable = bpm != null && progress.quality >= HeartRateMath.MIN_QUALITY
        if (!reliable) {
            _uiState.value = HeartRateUiState(
                phase = HeartRatePhase.FINISHED,
                error = CANNOT_READ_ERROR
            )
            return
        }

        val result = HeartRateResult(
            bpm = bpm,
            quality = progress.quality,
            zone = HeartRateMath.zoneOf(bpm)
        )
        _uiState.value = HeartRateUiState(
            phase = HeartRatePhase.FINISHED,
            elapsedMs = progress.elapsedMs,
            beats = progress.beats,
            liveBpm = bpm,
            quality = progress.quality,
            signalStrength = progress.signalStrength,
            fingerDetected = progress.fingerDetected,
            result = result,
            isSaving = true
        )
        persist(bpm)
    }

    /** Ghi kết quả vào `health_data_history`; lỗi ghi chỉ hiện thông báo, kết quả vẫn giữ trên màn. */
    private fun persist(bpm: Int) {
        viewModelScope.launch {
            val outcome = saveMeasuredHeartRate(bpm)
            _uiState.update { state ->
                state.copy(
                    isSaving = false,
                    saved = outcome.isSuccess,
                    error = outcome.exceptionOrNull()?.let { "Chưa lưu được kết quả: ${it.message}" }
                )
            }
        }
    }

    private companion object {
        const val CANNOT_READ_ERROR =
            "Không đọc được nhịp tim rõ ràng. Hãy đặt đầu ngón tay che kín camera và đèn flash, " +
                "giữ yên tay và ấn vừa phải rồi đo lại."

        const val NO_FLASH_WARNING =
            "Máy này không có đèn flash nên phép đo chỉ dựa vào ánh sáng phòng — kết quả có thể kém " +
                "chính xác. Nên đo ở nơi đủ sáng hoặc dùng thiết bị có đèn flash."
    }
}
