package com.healthyai.app.domain.model

import java.time.LocalDate

/** Giờ ngủ của một đêm trong thẻ "Xu hướng chất lượng giấc ngủ". */
data class SleepNight(
    val date: LocalDate,
    val hours: Double = 0.0
) {
    val hasData: Boolean get() = hours > 0.0
}

/** Xu hướng giấc ngủ 7 đêm gần nhất — dữ liệu cho biểu đồ cột giờ ngủ ở tab Phân tích. */
data class SleepTrend(
    /** Danh sách đêm theo thứ tự cũ → mới, luôn đủ số đêm của cửa sổ. */
    val nights: List<SleepNight> = emptyList()
) {
    val hasData: Boolean get() = nights.any { it.hasData }

    /** Các đêm có số liệu thật (bỏ đêm chưa ghi được gì) — dùng để tính trung bình. */
    val nightsWithData: List<SleepNight> get() = nights.filter { it.hasData }

    /** Số giờ ngủ trung bình mỗi đêm (chỉ tính đêm có dữ liệu); `0.0` khi chưa có đêm nào. */
    val averageHours: Double
        get() = if (nightsWithData.isEmpty()) 0.0 else nightsWithData.sumOf { it.hours } / nightsWithData.size
}

/** Đánh giá chất lượng giấc ngủ theo số giờ ngủ trung bình mỗi đêm. */
enum class SleepQuality(val label: String) {
    /** Trung bình dưới 6 giờ/đêm. */
    SHORT("Thiếu ngủ"),

    /** Trung bình 6–7 giờ/đêm. */
    FAIR("Chưa đủ"),

    /** Trung bình 7–9 giờ/đêm. */
    GOOD("Tốt"),

    /** Trung bình trên 9 giờ/đêm. */
    LONG("Hơi dài")
}
