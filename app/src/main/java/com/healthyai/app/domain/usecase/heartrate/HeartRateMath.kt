package com.healthyai.app.domain.usecase.heartrate

import com.healthyai.app.domain.model.HeartRateZone

/** Toán và ngưỡng dùng chung cho phép đo nhịp tim bằng camera + đèn flash (PPG — photoplethysmography). */
object HeartRateMath {

    // Giới hạn nhịp tim hợp lý

    /** Nhịp chậm nhất/nhanh nhất còn coi là nhịp tim thật (bpm). Ngoài khoảng này là nhiễu/nhận sai. */
    const val MIN_BPM = 40
    const val MAX_BPM = 200

    /** Khoảng cách ngắn nhất giữa hai nhịp = 200 bpm; gần hơn thì là cùng một nhịp (hoặc nhiễu). */
    const val MIN_INTERVAL_MS = 300L

    /** Khoảng cách dài nhất giữa hai nhịp = 40 bpm; xa hơn thì đã bỏ sót nhịp ở giữa. */
    const val MAX_INTERVAL_MS = 1_500L

    // Thời lượng và số liệu tối thiểu để kết luận

    /** Đo ít nhất ngần này mới đủ nhịp để kết luận (ngắn hơn thì kết quả rất dễ sai). */
    const val MIN_MEASURE_MS = 15_000L

    /** Quá thời gian này mà vẫn không ra kết quả thì dừng và báo chưa đọc được nhịp. */
    const val MAX_MEASURE_MS = 30_000L

    /** Số khoảng cách giữa hai nhịp (RR) tối thiểu để chốt kết quả — tương ứng 6 nhịp liên tiếp. */
    const val MIN_INTERVALS = 5

    /** Số khoảng RR giữ lại để lấy trung vị (cửa sổ trượt). */
    const val INTERVAL_WINDOW = 5

    // Ngưỡng chất lượng

    /** Tự tương quan ở bước trễ tốt nhất phải đạt mức này mới coi là "có mạch đập thật". */
    const val MIN_PERIODICITY = 0.4f

    /** Chất lượng tối thiểu để tự chốt kết quả khi đã đo đủ thời gian tối thiểu. */
    const val MIN_QUALITY = 0.5f

    /** Độ lệch MAD/trung vị của khoảng RR từ mức này trở lên coi là nhịp không đều (chất lượng 0). */
    const val MAX_DISPERSION = 0.15

    // Ngưỡng tín hiệu thô (chỉ để nhắc người dùng)

    /** Đỏ trung bình (0..255) từ mức này coi như ngón tay đã che camera; chỉ để hướng dẫn, không dùng tính nhịp. */
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

    // Hàm thuần

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

    /** Độ lệch tuyệt đối trung vị (MAD) — thước đo độ tản chịu được ngoại lệ tốt hơn độ lệch chuẩn. */
    fun medianAbsoluteDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val center = median(values)
        return median(values.map { kotlin.math.abs(it - center) })
    }

    /** Độ đều của khoảng RR trong `0..1`; trả `0` khi chưa đủ [MIN_INTERVALS] khoảng. */
    fun intervalRegularity(intervals: List<Double>): Float {
        if (intervals.size < MIN_INTERVALS) return 0f
        val center = median(intervals)
        if (center <= 0.0) return 0f
        val dispersion = medianAbsoluteDeviation(intervals) / center
        return (1.0 - dispersion / MAX_DISPERSION).coerceIn(0.0, 1.0).toFloat()
    }

    /** Độ mạnh tương đối của tín hiệu PPG (AC/DC) — dùng tỉ lệ vì máy ảnh tự chỉnh phơi sáng. */
    fun signalStrength(acPeakToPeak: Double, dc: Double): Float {
        if (dc <= 0.0 || acPeakToPeak <= 0.0) return 0f
        val ratio = acPeakToPeak / dc
        return ((ratio - MIN_AC_DC_RATIO) / (GOOD_AC_DC_RATIO - MIN_AC_DC_RATIO))
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    /** Nội suy parabol đỉnh qua ba mẫu liên tiếp để chính xác hơn (kết quả trong `-1..1`). */
    fun parabolicPeakOffset(previous: Double, peak: Double, next: Double): Double {
        val denominator = previous - 2.0 * peak + next
        if (denominator == 0.0) return 0.0
        return (0.5 * (previous - next) / denominator).coerceIn(-1.0, 1.0)
    }

    /** Trung bình kênh đỏ từ trung bình hai mặt phẳng Y, V (YUV→RGB tuyến tính); không cắt về 0..255. */
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
