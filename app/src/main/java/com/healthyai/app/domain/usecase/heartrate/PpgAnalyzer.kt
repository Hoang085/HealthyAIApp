package com.healthyai.app.domain.usecase.heartrate

import kotlin.math.abs
import kotlin.math.roundToInt

/** Bộ phân tích dòng khung hình camera thành nhịp tim (PPG — photoplethysmography). */
class PpgAnalyzer {

    private data class Sample(val timestampMs: Long, val value: Double)

    private val dcWindow = ArrayDeque<Sample>()
    private var dcSum = 0.0

    /** Cửa sổ 2 giây gần nhất để tính ngưỡng đỉnh thích ứng. */
    private val acWindow = ArrayDeque<Sample>()

    /** Cửa sổ ~8 giây gần nhất để tính tự tương quan và vẽ dạng sóng. */
    private val recent = ArrayDeque<Sample>()

    /** Vài mẫu quanh mẫu đang xét, đủ để biết một mẫu có phải đỉnh cục bộ hay không. */
    private val pending = ArrayDeque<Sample>()

    private val intervals = ArrayDeque<Double>()

    private var firstTimestampMs: Long? = null
    private var lastPeakTimestampMs: Double? = null

    /** Số đỉnh mạch đã xác nhận trong chuỗi đang đo (mất ngón tay là bắt đầu lại). */
    private var beats = 0

    private var periodicity = 0f
    private var correlationBpm: Int? = null
    private var samplesSincePeriodicity = 0

    /** Tín hiệu AC đã lọc mượt (EMA) — xem [AC_SMOOTHING_ALPHA]. */
    private var smoothedAc = 0.0
    private var smoothingStarted = false

    /** Cờ cho phép nhận nhịp tiếp theo — đợi tín hiệu rơi dưới mức đặt lại để tránh đỉnh giả do rung tay. */
    private var armed = true

    /** Xoá toàn bộ trạng thái để bắt đầu một lần đo mới. */
    fun reset() {
        dcWindow.clear()
        acWindow.clear()
        dcSum = 0.0
        firstTimestampMs = null
        samplesSincePeriodicity = 0
        forgetSignal()
    }

    /** Nạp một khung hình mới (giá trị đỏ + mốc thời gian) và trả về tình trạng đo hiện tại. */
    fun addSample(red: Double, timestampMs: Long): PpgProgress {
        if (firstTimestampMs == null) firstTimestampMs = timestampMs

        // 1) Ước lượng thành phần một chiều (DC) bằng trung bình trượt ~1 giây.
        dcWindow.addLast(Sample(timestampMs, red))
        dcSum += red
        while (dcWindow.size > 1 && timestampMs - dcWindow.first().timestampMs > DC_WINDOW_MS) {
            dcSum -= dcWindow.removeFirst().value
        }
        val dc = dcSum / dcWindow.size

        // 2) Bỏ DC → thành phần xoay chiều (AC) là "từng đợt máu dồn về đầu ngón tay".
        val ac = red - dc

        // Lọc mượt bằng EMA: độ trễ không đổi nên khoảng cách hai nhịp không lệch, nhiễu tần số cao giảm.
        val smoothed = if (smoothingStarted) {
            smoothedAc + AC_SMOOTHING_ALPHA * (ac - smoothedAc)
        } else {
            smoothingStarted = true
            ac
        }
        smoothedAc = smoothed

        push(acWindow, Sample(timestampMs, smoothed), AC_WINDOW_MS)
        push(recent, Sample(timestampMs, smoothed), RECENT_WINDOW_MS)

        val fingerDetected = dc >= HeartRateMath.MIN_FINGER_RED
        if (!fingerDetected) {
            // Mất ngón tay giữa chừng: ánh sáng phòng (hoặc bàn tay che hờ) không phải mạch đập.
            forgetSignal()
        }

        // 3) Tìm đỉnh cục bộ của AC.
        pending.addLast(Sample(timestampMs, smoothed))
        while (pending.size > PEAK_WINDOW_SAMPLES) pending.removeFirst()
        if (fingerDetected && pending.size == PEAK_WINDOW_SAMPLES) {
            detectPeak()
        }

        // 4) Tính lại tính chu kỳ theo từng nhịp khung hình (không cần mỗi khung).
        samplesSincePeriodicity += 1
        if (samplesSincePeriodicity >= PERIODICITY_EVERY_SAMPLES) {
            samplesSincePeriodicity = 0
            updatePeriodicity(timestampMs)
        }

        val elapsedMs = timestampMs - (firstTimestampMs ?: timestampMs)
        val regularity = HeartRateMath.intervalRegularity(intervals)
        val quality = if (intervals.size >= HeartRateMath.MIN_INTERVALS) {
            minOf(periodicity, regularity)
        } else {
            periodicity
        }
        val bpm = estimateBpm() ?: correlationBpm

        return PpgProgress(
            elapsedMs = elapsedMs,
            beats = beats,
            bpm = bpm,
            quality = quality,
            signalStrength = HeartRateMath.signalStrength(acPeakToPeak(), dc),
            fingerDetected = fingerDetected,
            waveform = waveformSnapshot(),
            ready = elapsedMs >= HeartRateMath.MIN_MEASURE_MS &&
                intervals.size >= HeartRateMath.MIN_INTERVALS &&
                bpm != null &&
                quality >= HeartRateMath.MIN_QUALITY
        )
    }

    // Tìm đỉnh mạch

    private fun detectPeak() {
        val index = PEAK_LOOKAHEAD
        val previous = pending[index - 1]
        val peak = pending[index]
        val next = pending[index + 1]

        // Ngưỡng thích ứng = mean + [PEAK_SNR_RATIO]·độ lệch chuẩn; dùng độ lệch chuẩn (không dùng biên độ) vì biên độ bị nhiễu kéo rộng gây đỉnh giả.
        val (mean, deviation) = acStatistics()
        if (deviation <= 0.0) return
        val threshold = mean + PEAK_SNR_RATIO * deviation

        // Đã rơi xuống dưới mức trung bình ⇒ lại được phép nhận nhịp tiếp theo.
        if (peak.value < mean) {
            armed = true
        }
        if (!armed) return

        val isLocalMaximum = peak.value > previous.value && peak.value > next.value
        if (!isLocalMaximum) return
        if (peak.value < threshold) return

        val offset = HeartRateMath.parabolicPeakOffset(previous.value, peak.value, next.value)
        val frameMs = (next.timestampMs - peak.timestampMs).toDouble()
        registerBeat(peak.timestampMs + offset * frameMs)
        armed = false
    }

    /** Ghi nhận một nhịp ở mốc thời gian [timestampMs] và cập nhật danh sách khoảng RR. */
    private fun registerBeat(timestampMs: Double) {
        val previous = lastPeakTimestampMs
        lastPeakTimestampMs = timestampMs
        beats += 1

        if (previous == null) return
        val interval = timestampMs - previous
        if (!HeartRateMath.isValidInterval(interval)) return

        intervals.addLast(interval)
        while (intervals.size > HeartRateMath.INTERVAL_WINDOW) intervals.removeFirst()
    }

    /** Nhịp từ trung vị khoảng RR sau khi loại ngoại lệ (>25% lệch trung vị); `null` khi chưa đủ khoảng. */
    private fun estimateBpm(): Int? {
        if (intervals.size < HeartRateMath.MIN_INTERVALS) return null

        val center = HeartRateMath.median(intervals)
        if (center <= 0.0) return null

        val tolerance = center * OUTLIER_TOLERANCE
        val cleaned = intervals.filter { abs(it - center) <= tolerance }
        val used = if (cleaned.size >= MIN_CLEANED_INTERVALS) cleaned else intervals.toList()

        return HeartRateMath.bpmOf(HeartRateMath.median(used)).roundToInt()
    }

    /** Quên tín hiệu đang có (mất ngón tay hoặc bắt đầu đo mới) — xoá cả dạng sóng và tính chu kỳ. */
    private fun forgetSignal() {
        pending.clear()
        recent.clear()
        intervals.clear()
        lastPeakTimestampMs = null
        beats = 0
        periodicity = 0f
        correlationBpm = null
        armed = true
    }

    // Tự tương quan (tính chu kỳ)

    /** Tính tự tương quan chuẩn hoá của dạng sóng ~8 giây, lấy cực đại làm [periodicity] (0..1). */
    private fun updatePeriodicity(nowMs: Long) {
        periodicity = 0f
        correlationBpm = null

        if (recent.size < MIN_RECENT_SAMPLES) return
        val span = (nowMs - recent.first().timestampMs).toDouble()
        if (span < MIN_PERIODICITY_WINDOW_MS) return

        // Các khung hình không cách đều nhau tuyệt đối → lấy lại mẫu trên lưới thời gian đều nhau.
        val step = span / (recent.size - 1)
        if (step <= 0.0) return
        val count = ((span / step).roundToInt() + 1).coerceAtMost(MAX_CORRELATION_SAMPLES)
        if (count < MIN_RECENT_SAMPLES) return

        val series = resample(step, count, nowMs)
        val mean = series.average()
        var energy = 0.0
        for (i in series.indices) {
            series[i] -= mean
            energy += series[i] * series[i]
        }
        if (energy <= 0.0) return

        val minLag = (HeartRateMath.MIN_INTERVAL_MS / step).roundToInt().coerceAtLeast(1)
        val maxLag = (HeartRateMath.MAX_INTERVAL_MS / step).roundToInt().coerceAtMost(count - 2)
        if (maxLag <= minLag) return

        var bestLag = minLag
        var best = Double.NEGATIVE_INFINITY
        for (lag in minLag..maxLag) {
            val correlation = correlation(series, lag, energy)
            if (correlation > best) {
                best = correlation
                bestLag = lag
            }
        }

        if (best <= 0.0) return
        periodicity = best.coerceIn(0.0, 1.0).toFloat()

        // Chỉ dùng ước lượng từ tự tương quan khi chu kỳ đã đủ rõ — tránh hiện một con số vô nghĩa.
        if (periodicity >= HeartRateMath.MIN_PERIODICITY) {
            correlationBpm = HeartRateMath.bpmOf(bestLag * step).roundToInt()
        }
    }

    /** Tự tương quan chuẩn hoá ở bước trễ [lag] (nằm trong `-1..1`). */
    private fun correlation(series: DoubleArray, lag: Int, energy: Double): Double {
        var sum = 0.0
        for (i in lag until series.size) {
            sum += series[i] * series[i - lag]
        }
        return sum / energy
    }

    /** Lấy lại [count] mẫu cách đều [step] ms bằng nội suy tuyến tính (khoảng cách khung hình dao động). */
    private fun resample(step: Double, count: Int, endMs: Long): DoubleArray {
        val samples = recent.toList()
        val out = DoubleArray(count)
        var index = 0
        for (i in 0 until count) {
            val target = endMs - (count - 1 - i) * step
            while (index < samples.size - 2 && samples[index + 1].timestampMs <= target) index += 1
            val a = samples[index]
            val b = samples[minOf(index + 1, samples.size - 1)]
            val span = (b.timestampMs - a.timestampMs).toDouble()
            out[i] = if (span <= 0.0) {
                a.value
            } else {
                val fraction = ((target - a.timestampMs) / span).coerceIn(0.0, 1.0)
                a.value + (b.value - a.value) * fraction
            }
        }
        return out
    }

    // Tiện ích cửa sổ trượt

    private fun push(window: ArrayDeque<Sample>, sample: Sample, windowMs: Long) {
        window.addLast(sample)
        while (window.size > 1 && sample.timestampMs - window.first().timestampMs > windowMs) {
            window.removeFirst()
        }
    }

    private fun acMinimum(): Double = acWindow.minOf { it.value }

    private fun acMaximum(): Double = acWindow.maxOf { it.value }

    private fun acRange(): Double =
        if (acWindow.isEmpty()) 0.0 else acMaximum() - acMinimum()

    private fun acPeakToPeak(): Double = acRange()

    /** Trung bình và độ lệch chuẩn của tín hiệu AC trong cửa sổ ngưỡng (một lượt duyệt). */
    private fun acStatistics(): Pair<Double, Double> {
        if (acWindow.isEmpty()) return 0.0 to 0.0

        var sum = 0.0
        for (sample in acWindow) sum += sample.value
        val mean = sum / acWindow.size

        var squares = 0.0
        for (sample in acWindow) {
            val delta = sample.value - mean
            squares += delta * delta
        }
        return mean to kotlin.math.sqrt(squares / acWindow.size)
    }

    /** Dạng sóng AC gần đây chuẩn hoá về `-1..1`, giảm còn tối đa [WAVEFORM_POINTS] điểm để vẽ. */
    private fun waveformSnapshot(): List<Float> {
        if (recent.size < 2) return emptyList()
        val peak = recent.maxOf { abs(it.value) }
        if (peak <= 0.0) return emptyList()

        val stride = (recent.size / WAVEFORM_POINTS).coerceAtLeast(1)
        val out = ArrayList<Float>(recent.size / stride + 1)
        var index = 0
        for (sample in recent) {
            if (index % stride == 0) {
                out.add((sample.value / peak).toFloat().coerceIn(-1f, 1f))
            }
            index += 1
        }
        return out
    }

    private companion object {
        /** Cửa sổ trung bình trượt để bỏ DC (~1 giây ở 30 khung hình/giây). */
        const val DC_WINDOW_MS = 1_000L

        /** Cửa sổ tính ngưỡng đỉnh thích ứng (~2 giây). */
        const val AC_WINDOW_MS = 2_000L

        /** Hệ số làm mượt AC (EMA): giảm nhiễu trắng nhưng không làm lệch khoảng cách giữa hai nhịp. */
        const val AC_SMOOTHING_ALPHA = 0.5

        /** Cửa sổ để tính tự tương quan (~8 giây — đủ chứa ~5–10 nhịp). */
        const val RECENT_WINDOW_MS = 8_000L

        /** Số mẫu mỗi bên khi xét đỉnh cục bộ (1 mẫu ≈ 33 ms ở 30 khung hình/giây). */
        const val PEAK_LOOKAHEAD = 2
        const val PEAK_WINDOW_SAMPLES = PEAK_LOOKAHEAD * 2 + 1

        /** Đỉnh phải cao hơn trung bình ngần này lần độ lệch chuẩn của tín hiệu mới coi là nhịp. */
        const val PEAK_SNR_RATIO = 1.2

        /** Tính lại tính chu kỳ mỗi ngần này khung hình (≈6 lần/giây) để không tốn CPU vô ích. */
        const val PERIODICITY_EVERY_SAMPLES = 5

        /** Chưa đủ thời lượng này thì chưa tính tự tương quan (chưa có đủ nhịp để nói gì). */
        const val MIN_PERIODICITY_WINDOW_MS = 4_000.0

        /** Số mẫu tối thiểu trong cửa sổ tự tương quan. */
        const val MIN_RECENT_SAMPLES = 60

        /** Trần số điểm đưa vào tự tương quan (chặn chi phí trên máy chạy 60 khung hình/giây). */
        const val MAX_CORRELATION_SAMPLES = 400

        /** Số điểm tối đa của dạng sóng đưa lên giao diện. */
        const val WAVEFORM_POINTS = 150

        /** Khoảng RR lệch quá tỉ lệ này so với trung vị bị coi là ngoại lệ. */
        const val OUTLIER_TOLERANCE = 0.25

        /** Sau khi loại ngoại lệ còn ít nhất ngần này khoảng thì mới dùng danh sách đã lọc. */
        const val MIN_CLEANED_INTERVALS = 3
    }
}

/** Tình trạng đo sau một khung hình — đủ để màn đo hiển thị số nhịp, dạng sóng và hướng dẫn. */
data class PpgProgress(
    val elapsedMs: Long,
    val beats: Int,
    val bpm: Int?,
    val quality: Float,
    val signalStrength: Float,
    val fingerDetected: Boolean,
    val waveform: List<Float>,
    val ready: Boolean
)
