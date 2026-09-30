package com.healthyai.app.domain.usecase.heartrate

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bộ phân tích **dòng khung hình camera** thành nhịp tim (PPG — photoplethysmography).
 *
 * Đầu vào là **giá trị đỏ trung bình** của vùng giữa mỗi khung hình kèm mốc thời gian; đầu ra là
 * [PpgProgress] sau mỗi khung hình (nhịp hiện tại, chất lượng, dạng sóng để vẽ, và cảnh báo cho phần
 * hướng dẫn người dùng).
 *
 * ## Các bước xử lý
 * 1. **Bỏ thành phần một chiều (DC)** — trừ trung bình trượt ~1 giây: phần còn lại (AC) chính là
 *    "từng đợt máu dồn về đầu ngón tay". Trung bình trượt đối xứng nên **không làm lệch mốc thời
 *    gian của các đỉnh**, vì vậy phép lọc này không ảnh hưởng tới nhịp đo được.
 * 2. **Tìm đỉnh mạch** — đỉnh cục bộ của AC cao hơn ngưỡng thích ứng (45% biên độ của 2 giây gần
 *    nhất), có chống dội (không nhận hai đỉnh gần hơn [HeartRateMath.MIN_INTERVAL_MS]) và mốc thời
 *    gian được **nội suy parabol** để chính xác hơn khoảng cách giữa hai khung hình.
 * 3. **Kiểm tra tính chu kỳ bằng tự tương quan** — đây là bước trả lời câu hỏi "chuỗi đổi màu này có
 *    lặp lại đều đặn không, hay chỉ là nhiễu?". Tín hiệu thật cho tự tương quan cao ở bước trễ đúng
 *    bằng một chu kỳ tim; nhiễu thì gần như không tương quan ở mọi bước trễ.
 * 4. **Chốt nhịp** — trung vị các khoảng RR (đã loại ngoại lệ) rồi suy ra bpm; chất lượng là sự kết
 *    hợp của **tính chu kỳ** và **độ đều của nhịp**, cả hai đều là đại lượng **không phụ thuộc biên
 *    độ** nên không bị ảnh hưởng khi camera tự chỉnh phơi sáng.
 *
 * Lớp này **chỉ dùng toán thuần** (không phụ thuộc Android) nên test được trên JVM — xem
 * `PpgAnalyzerTest` với tín hiệu mạch đập giả lập ở nhịp đã biết.
 *
 * ⚠️ **Giới hạn đã biết:** đây là **ước lượng** từ camera điện thoại, không phải thiết bị y tế.
 * Chuyển động tay, ấn ngón tay quá mạnh (dòng máu bị chặn), hoặc ánh sáng phòng nhấp nháy đều làm
 * sai kết quả — đó là lý do app chỉ kết luận khi nhịp **đều** và tín hiệu **có chu kỳ rõ**.
 */
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

    /** Số đỉnh mạch đã xác nhận **trong chuỗi đang đo** (mất ngón tay là bắt đầu lại). */
    private var beats = 0

    private var periodicity = 0f
    private var correlationBpm: Int? = null
    private var samplesSincePeriodicity = 0

    /** Tín hiệu AC đã lọc mượt (EMA) — xem [AC_SMOOTHING_ALPHA]. */
    private var smoothedAc = 0.0
    private var smoothingStarted = false

    /**
     * Cờ "được phép nhận nhịp tiếp theo": sau mỗi nhịp phải đợi tín hiệu **rơi xuống dưới mức đặt
     * lại** rồi mới tìm đỉnh mới. Không có bước này thì nhiễu (rung tay) tạo thêm đỉnh giả giữa hai
     * nhịp, làm nhịp đo được bị "nhanh" giả tạo.
     */
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

    /**
     * Nạp một khung hình mới và trả về tình trạng đo hiện tại.
     *
     * @param red Giá trị đỏ trung bình của vùng giữa khung hình (xem
     *   [HeartRateMath.redFromYuvMeans]).
     * @param timestampMs Mốc thời gian của khung hình (ms, đồng hồ đơn điệu của camera).
     */
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

        // Lọc mượt bằng trung bình động hàm mũ (EMA). EMA có **độ trễ không đổi** nên mọi đỉnh đều
        // bị dịch đi đúng một khoảng như nhau — khoảng cách giữa hai nhịp **không** bị lệch (khác với
        // bộ lọc phi tuyến), trong khi nhiễu tần số cao bị giảm khoảng một nửa (rung tay không còn
        // sinh ra đỉnh giả giữa hai nhịp).
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

    // ── Tìm đỉnh mạch ─────────────────────────────────────────

    private fun detectPeak() {
        val index = PEAK_LOOKAHEAD
        val previous = pending[index - 1]
        val peak = pending[index]
        val next = pending[index + 1]

        // Ngưỡng **thích ứng theo nhiễu**: đỉnh phải cao hơn trung bình 2 giây gần nhất ít nhất
        // [PEAK_SNR_RATIO] lần độ lệch chuẩn của chính tín hiệu đó.
        //
        // Vì sao không dùng `min + tỉ lệ × biên độ`: biên độ (max − min) bị **chính nhiễu** kéo rộng
        // ra, nên ngưỡng tụt xuống và nhiễu lại tạo thêm đỉnh giả — đúng lỗi làm nhịp đo ra bị
        // "nhanh" giả tạo khi tay rung. Lấy ngưỡng theo độ lệch chuẩn thì tín hiệu yếu (biên độ nhỏ)
        // tự động bị đòi hỏi khắt khe hơn, còn tín hiệu mạnh vẫn dễ vượt ngưỡng.
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

    /**
     * Nhịp suy từ trung vị các khoảng RR, sau khi **loại ngoại lệ** (khoảng lệch quá 25% so với trung
     * vị — thường là nhịp bị nhận hai lần hoặc bỏ sót một nhịp).
     *
     * `null` khi chưa đủ [HeartRateMath.MIN_INTERVALS] khoảng.
     */
    private fun estimateBpm(): Int? {
        if (intervals.size < HeartRateMath.MIN_INTERVALS) return null

        val center = HeartRateMath.median(intervals)
        if (center <= 0.0) return null

        val tolerance = center * OUTLIER_TOLERANCE
        val cleaned = intervals.filter { abs(it - center) <= tolerance }
        val used = if (cleaned.size >= MIN_CLEANED_INTERVALS) cleaned else intervals.toList()

        return HeartRateMath.bpmOf(HeartRateMath.median(used)).roundToInt()
    }

    /**
     * Quên tín hiệu đang có — gọi khi **mất ngón tay che camera** (không còn là tín hiệu mạch đập)
     * hoặc khi bắt đầu lần đo mới.
     *
     * Xoá cả **dạng sóng gần đây** và **tính chu kỳ** (không chỉ các khoảng RR): khi không còn ngón
     * tay thì mọi số liệu cũ — kể cả "nhịp ước lượng" — đều không còn đúng nữa, giữ lại sẽ khiến màn
     * đo hiện một con số cũ mà người dùng tưởng là số đang đo.
     */
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

    // ── Tự tương quan (tính chu kỳ) ────────────────────────────

    /**
     * Tính **tự tương quan chuẩn hoá** của dạng sóng ~8 giây gần nhất ở mọi bước trễ ứng với nhịp
     * 40–200 bpm, lấy giá trị lớn nhất làm [periodicity] (`0..1`).
     *
     * Giá trị này **không phụ thuộc biên độ** (đã chuẩn hoá theo năng lượng tín hiệu), nên là thước
     * đo đáng tin để phân biệt "mạch đập thật" với "nhiễu + chuyển động": nhiễu cho tương quan rất
     * thấp ở mọi bước trễ, còn mạch đập cho tương quan cao ở đúng chu kỳ tim.
     */
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

    /** Tự tương quan **chuẩn hoá** ở bước trễ [lag]: `Σ xᵢ·xᵢ₋ₗₐg / Σ xᵢ²` (nằm trong `-1..1`). */
    private fun correlation(series: DoubleArray, lag: Int, energy: Double): Double {
        var sum = 0.0
        for (i in lag until series.size) {
            sum += series[i] * series[i - lag]
        }
        return sum / energy
    }

    /**
     * Lấy lại [count] mẫu cách đều nhau [step] ms (kết thúc ở khung hình mới nhất) bằng nội suy
     * tuyến tính — cần thiết vì khoảng cách giữa các khung hình của camera có dao động.
     */
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

    // ── Tiện ích cửa sổ trượt ─────────────────────────────────

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

    /**
     * Dạng sóng AC gần đây đã **chuẩn hoá** về `-1..1` (chia cho biên độ lớn nhất trong cửa sổ) để
     * màn đo vẽ được đường tín hiệu mà không phải lo về đơn vị của từng thiết bị.
     *
     * Số điểm được giảm bớt (tối đa [WAVEFORM_POINTS]) cho nhẹ việc vẽ.
     */
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

        /**
         * Hệ số làm mượt AC. EMA chỉ giữ được khoảng `α/(2−α) = 1/3` phương sai của nhiễu trắng (giảm
         * độ lệch chuẩn ~42%), trong khi độ trễ không đổi nên **không** làm lệch khoảng cách giữa hai
         * nhịp.
         */
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

/**
 * Tình trạng đo sau một khung hình — đủ để màn đo hiển thị số nhịp, dạng sóng và **hướng dẫn** người
 * dùng đang đo.
 *
 * @property elapsedMs Thời gian đã đo (ms, tính từ khung hình đầu tiên).
 * @property beats Số nhịp đã xác nhận trong chuỗi đang đo (mất ngón tay là đếm lại từ đầu).
 * @property bpm Nhịp ước lượng hiện tại; `null` khi chưa đủ dữ liệu hoặc tín hiệu không đủ rõ.
 * @property quality Độ tin cậy `0..1` = kết hợp **tính chu kỳ** của tín hiệu và **độ đều** của nhịp.
 * @property signalStrength Độ mạnh tương đối của tín hiệu `0..1` (chỉ để nhắc người dùng ấn/giữ tay).
 * @property fingerDetected Ngón tay đã che camera + đèn flash chưa (theo ngưỡng
 *   [HeartRateMath.MIN_FINGER_RED]).
 * @property waveform Dạng sóng AC gần đây, đã chuẩn hoá `-1..1`, để vẽ.
 * @property ready Đã đủ dữ liệu để **chốt** kết quả (đủ thời gian tối thiểu, đủ nhịp, chất lượng đạt).
 */
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
