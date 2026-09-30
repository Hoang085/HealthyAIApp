package com.healthyai.app.domain.usecase.heartrate

import java.util.Random
import kotlin.math.exp

/** Một khung hình giả lập: giá trị đỏ trung bình và mốc thời gian của khung hình. */
internal data class PpgFrame(val red: Double, val timestampMs: Long)

/**
 * Sinh tín hiệu PPG giả lập cho unit test.
 *
 * Không thể cắm ngón tay vào unit test, nên tín hiệu được dựng theo **đúng vật lý của phép đo**: một
 * nền đỏ lớn (DC — ánh sáng đèn flash xuyên qua mô ngón tay) cộng một **đỉnh tâm thu nhọn** mỗi chu kỳ
 * tim cùng một gợn dicrotic nhỏ hơn. Đỉnh tâm thu rộng ~55 ms và **không co lại theo nhịp tim** —
 * đúng như mạch đập thật, nhờ vậy phép thử vẫn khó như thật ở nhịp nhanh.
 *
 * Khung hình được phát ở 30 hình/giây — tốc độ thường dùng của `ImageAnalysis`.
 */
internal object PpgTestSignal {

    /** Số ms giữa hai khung hình ở 30 hình/giây. */
    const val FRAME_MS = 33L

    /** Mốc thời gian gốc (một giá trị lớn như đồng hồ đơn điệu thật của camera). */
    const val BASE_TIMESTAMP_MS = 1_700_000_000_000L

    /** Mức nền đỏ khi ngón tay đã che camera + đèn flash đang sáng. */
    const val FINGER_DC = 200.0

    /** Mức nền khi **không** có ngón tay che (chỉ còn ánh sáng phòng). */
    const val DARK_DC = 20.0

    /** Biên độ đỉnh tâm thu thường dùng: ~0,6% của [FINGER_DC] — cỡ tín hiệu thật. */
    const val SYSTOLIC_AMPLITUDE = 1.2

    /**
     * Các khung hình của một đoạn đo.
     *
     * @param bpm `null` = không có mạch đập (chỉ có nền + nhiễu) — dùng để kiểm tra khả năng từ chối.
     * @param noise Độ lệch chuẩn của nhiễu cộng thêm (rung tay, nhiễu cảm biến).
     */
    fun frames(
        durationMs: Long,
        bpm: Double?,
        dc: Double = FINGER_DC,
        amplitude: Double = SYSTOLIC_AMPLITUDE,
        noise: Double = 0.0,
        startMs: Long = BASE_TIMESTAMP_MS,
        random: Random = Random(42)
    ): List<PpgFrame> {
        val frames = ArrayList<PpgFrame>(durationMs.toInt() / FRAME_MS.toInt() + 1)
        var timestamp = startMs
        val end = startMs + durationMs
        while (timestamp <= end) {
            val signal = if (bpm == null) dc else redAt(timestamp, bpm, dc, amplitude)
            frames.add(
                PpgFrame(
                    red = if (noise > 0.0) signal + random.nextGaussian() * noise else signal,
                    timestampMs = timestamp
                )
            )
            timestamp += FRAME_MS
        }
        return frames
    }

    /** Giá trị "đỏ trung bình" của một khung hình khi có mạch đập. */
    fun redAt(timestampMs: Long, bpm: Double, dc: Double, amplitude: Double): Double {
        val periodMs = 60_000.0 / bpm
        val phaseMs = timestampMs % periodMs

        val systolic = gaussian(phaseMs - SYSTOLIC_CENTER_FRACTION * periodMs, SYSTOLIC_WIDTH_MS)
        val dicrotic = DICROTIC_RATIO * gaussian(phaseMs - DICROTIC_CENTER_FRACTION * periodMs, DICROTIC_WIDTH_MS)

        return dc + amplitude * (systolic + dicrotic - BASELINE_OFFSET)
    }

    private fun gaussian(offsetMs: Double, width: Double): Double {
        val z = offsetMs / width
        return exp(-(z * z))
    }

    /** Vị trí đỉnh tâm thu trong chu kỳ (tỉ lệ chu kỳ). */
    private const val SYSTOLIC_CENTER_FRACTION = 0.18

    /** Độ rộng đỉnh tâm thu (ms) — cố định theo thời gian, không theo tỉ lệ chu kỳ. */
    private const val SYSTOLIC_WIDTH_MS = 55.0

    /** Vị trí và độ rộng của gợn dicrotic (nhỏ hơn nhiều so với đỉnh tâm thu). */
    private const val DICROTIC_CENTER_FRACTION = 0.45
    private const val DICROTIC_WIDTH_MS = 70.0
    private const val DICROTIC_RATIO = 0.3

    /** Mức nền của dạng sóng (để đáy chu kỳ nằm dưới mức trung bình, như mạch đập thật). */
    private const val BASELINE_OFFSET = 0.18
}
