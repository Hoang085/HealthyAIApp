package com.healthyai.app.domain.usecase.heartrate

import com.healthyai.app.domain.model.HeartRateZone

/**
 * Toán và ngưỡng dùng chung cho phép **đo nhịp tim bằng camera + đèn flash** (PPG —
 * photoplethysmography).
 *
 * Nguyên lý: **đèn flash chiếu sáng xuyên qua mô ngón tay**, camera ghi lại **sự thay đổi rất nhỏ về
 * sắc độ đỏ/độ sáng** giữa các khung hình theo từng đợt máu dồn về đầu ngón tay; chu kỳ đổi màu đó
 * chính là nhịp tim. Việc phân tích dòng khung hình nằm ở [PpgAnalyzer] (bỏ thành phần một chiều,
 * tìm các đỉnh mạch, kiểm tra tính chu kỳ bằng tự tương quan).
 *
 * File này chỉ chứa **hàm thuần và các ngưỡng** — không phụ thuộc Android — nên chạy được trong unit
 * test JVM (`HeartRateMathTest`, `PpgAnalyzerTest`); phần đọc khung hình thật nằm ở
 * `data/heartrate/CameraPpgAnalyzer`.
 */
object HeartRateMath {

    // ── Giới hạn nhịp tim hợp lý ───────────────────────────────

    /** Nhịp chậm nhất/nhanh nhất còn coi là nhịp tim thật (bpm). Ngoài khoảng này là nhiễu/nhận sai. */
    const val MIN_BPM = 40
    const val MAX_BPM = 200

    /** Khoảng cách ngắn nhất giữa hai nhịp = 200 bpm; gần hơn thì là cùng một nhịp (hoặc nhiễu). */
    const val MIN_INTERVAL_MS = 300L

    /** Khoảng cách dài nhất giữa hai nhịp = 40 bpm; xa hơn thì đã bỏ sót nhịp ở giữa. */
    const val MAX_INTERVAL_MS = 1_500L

    // ── Thời lượng và số liệu tối thiểu để kết luận ─────────────

    /** Đo ít nhất ngần này mới đủ nhịp để kết luận (ngắn hơn thì kết quả rất dễ sai). */
    const val MIN_MEASURE_MS = 15_000L

    /** Quá thời gian này mà vẫn không ra kết quả thì dừng và báo chưa đọc được nhịp. */
    const val MAX_MEASURE_MS = 30_000L

    /** Số khoảng cách giữa hai nhịp (RR) tối thiểu để chốt kết quả — tương ứng 6 nhịp liên tiếp. */
    const val MIN_INTERVALS = 5

    /** Số khoảng RR giữ lại để lấy trung vị (cửa sổ trượt). */
    const val INTERVAL_WINDOW = 5

    // ── Ngưỡng chất lượng ─────────────────────────────────────

    /** Tự tương quan ở bước trễ tốt nhất phải đạt mức này mới coi là "có mạch đập thật". */
    const val MIN_PERIODICITY = 0.4f

    /** Chất lượng tối thiểu để **tự chốt** kết quả khi đã đo đủ thời gian tối thiểu. */
    const val MIN_QUALITY = 0.5f

    /**
     * Độ lệch (MAD/trung vị) của các khoảng RR mà từ mức này trở lên coi là **nhịp không đều**
     * (chất lượng 0). Người nghỉ ngơi bình thường có độ lệch vài phần trăm; nhiễu thì cao hơn hẳn.
     */
    const val MAX_DISPERSION = 0.15

    // ── Ngưỡng tín hiệu thô (chỉ để nhắc người dùng) ────────────

    /**
     * Giá trị **đỏ trung bình** của vùng giữa khung hình (thang 0..255) từ mức này trở lên coi như
     * ngón tay đã che camera + đèn flash đang sáng.
     *
     * ⚠️ Đây là ngưỡng **heuristic**: máy ảnh tự chỉnh phơi sáng nên giá trị tuyệt đối thay đổi theo
     * thiết bị. Ngón tay che đèn flash cho ảnh đỏ rực (giá trị cao); không che thì ảnh tối/trung tính
     * (giá trị thấp). Ngưỡng này chỉ dùng để **hướng dẫn người dùng**, không dùng để tính nhịp tim.
     */
    const val MIN_FINGER_RED = 80.0

    /** Tỉ lệ biên độ AC/DC dưới mức này coi như tín hiệu quá yếu. */
    const val MIN_AC_DC_RATIO = 0.0002

    /** Tỉ lệ biên độ AC/DC từ mức này trở lên coi như tín hiệu mạnh (dùng để vẽ thanh "tín hiệu"). */
    const val GOOD_AC_DC_RATIO = 0.002

    /** Hệ số YUV→RGB (BT.601) cho kênh đỏ: `R = Y + 1.402·(V − 128)`. */
    const val RED_FROM_V = 1.402

    /** Ngưỡng phân loại nhịp nghỉ (bpm). */
    const val RESTING_LOW_BPM = 60
    const val RESTING_HIGH_BPM = 100

    // ── Hàm thuần ─────────────────────────────────────────────

    /** Nhịp/phút suy từ khoảng cách giữa hai nhịp (ms). */
    fun bpmOf(intervalMs: Double): Double = 60_000.0 / intervalMs

    /** Trung vị của [values] (không làm thay đổi danh sách truyền vào). */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }

    /**
     * Độ lệch tuyệt đối trung vị (MAD) — thước đo "độ tản" **chịu được ngoại lệ** tốt hơn độ lệch
     * chuẩn, phù hợp khi chỉ có vài khoảng RR và có thể lẫn một nhịp nhận sai.
     */
    fun medianAbsoluteDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val center = median(values)
        return median(values.map { kotlin.math.abs(it - center) })
    }

    /**
     * Độ "đều" của các khoảng RR trong khoảng `0..1`: `1` = các nhịp cách nhau đều tăm tắp,
     * `0` = lệch từ [MAX_DISPERSION] trở lên (nhịp không đều hoặc nhận sai nhịp).
     *
     * Trả `0` khi chưa đủ [MIN_INTERVALS] khoảng — lúc đó chưa có cơ sở để nói về độ đều.
     */
    fun intervalRegularity(intervals: List<Double>): Float {
        if (intervals.size < MIN_INTERVALS) return 0f
        val center = median(intervals)
        if (center <= 0.0) return 0f
        val dispersion = medianAbsoluteDeviation(intervals) / center
        return (1.0 - dispersion / MAX_DISPERSION).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Độ mạnh **tương đối** của tín hiệu PPG: biên độ xoay chiều (đỉnh–đáy) so với mức một chiều.
     *
     * Dùng tỉ lệ AC/DC (chứ không dùng biên độ tuyệt đối) vì máy ảnh tự chỉnh phơi sáng: khi hình
     * sáng hơn thì cả DC lẫn AC cùng lớn lên, tỉ lệ mới phản ánh đúng chất lượng tín hiệu.
     */
    fun signalStrength(acPeakToPeak: Double, dc: Double): Float {
        if (dc <= 0.0 || acPeakToPeak <= 0.0) return 0f
        val ratio = acPeakToPeak / dc
        return ((ratio - MIN_AC_DC_RATIO) / (GOOD_AC_DC_RATIO - MIN_AC_DC_RATIO))
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    /**
     * Đỉnh thật của một nhịp lệch bao nhiêu **mẫu** so với mẫu đang được coi là đỉnh, bằng cách nội
     * suy parabol qua ba mẫu liên tiếp `(previous, peak, next)`.
     *
     * Camera chỉ cho ~30 khung hình/giây, nên nếu chỉ lấy mốc thời gian của khung hình có giá trị
     * cao nhất thì mỗi nhịp đã bị làm tròn tới ±33 ms — đủ để sai vài bpm. Nội suy đỉnh (kỹ thuật
     * chuẩn khi xử lý tín hiệu rời rạc) cho mốc thời gian chính xác hơn nhiều.
     *
     * Kết quả nằm trong `-1..1` (đỉnh nằm lệch sang trái/phải nhiều nhất là nửa chu kỳ lấy mẫu).
     */
    fun parabolicPeakOffset(previous: Double, peak: Double, next: Double): Double {
        val denominator = previous - 2.0 * peak + next
        if (denominator == 0.0) return 0.0
        return (0.5 * (previous - next) / denominator).coerceIn(-1.0, 1.0)
    }

    /**
     * Đổi **trung bình** hai mặt phẳng `Y` (độ sáng) và `V` (sắc độ đỏ − độ sáng) của ảnh
     * `YUV_420_888` sang **trung bình kênh đỏ** — chính là tín hiệu PPG.
     *
     * YUV→RGB là phép biến đổi **tuyến tính**, nên trung bình của kênh đỏ suy được từ trung bình của
     * `Y` và `V` mà **không cần đổi từng điểm ảnh** (`R = Y + 1.402·(V − 128)`): nhờ vậy chỉ phải đọc
     * hai mặt phẳng và cộng số nguyên, chạy kịp ở 30 khung hình/giây.
     *
     * Giá trị trả về **không bị cắt** về `0..255`: phần chênh lệch nhỏ giữa các khung hình mới là
     * thứ cần đo, cắt biên sẽ làm mất đúng phần tín hiệu đó.
     */
    fun redFromYuvMeans(luma: Double, chromaV: Double): Double = luma + RED_FROM_V * (chromaV - 128.0)

    /** Phân loại nhịp tim nghỉ (tham khảo, không phải chẩn đoán). */
    fun zoneOf(bpm: Int): HeartRateZone = when {
        bpm < RESTING_LOW_BPM -> HeartRateZone.LOW
        bpm > RESTING_HIGH_BPM -> HeartRateZone.HIGH
        else -> HeartRateZone.NORMAL
    }

    /** Số nhịp tối thiểu phải nhận được để chốt kết quả ([MIN_INTERVALS] + 1 đỉnh đầu tiên). */
    const val MIN_BEATS = MIN_INTERVALS + 1

    /** Khoảng giá trị hợp lệ của một khoảng RR (khoảng cách giữa hai nhịp). */
    fun isValidInterval(intervalMs: Double): Boolean =
        intervalMs >= MIN_INTERVAL_MS.toDouble() && intervalMs <= MAX_INTERVAL_MS.toDouble()
}
