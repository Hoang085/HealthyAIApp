package com.healthyai.app.domain.model

import java.time.LocalDate

/**
 * Giờ ngủ của **một đêm** trong thẻ "Xu hướng chất lượng giấc ngủ".
 *
 * @property date Ngày (theo múi giờ thiết bị) mà số giờ ngủ được ghi nhận — cùng cách gắn ngày với
 *   `health_data_history` ở [DailyHealthSummary].
 * @property hours Số giờ ngủ; `0.0` nghĩa là **đêm đó chưa có dữ liệu** (khác hẳn "ngủ 0 giờ").
 */
data class SleepNight(
    val date: LocalDate,
    val hours: Double = 0.0
) {
    val hasData: Boolean get() = hours > 0.0
}

/**
 * Xu hướng giấc ngủ **7 đêm gần nhất** — dữ liệu cho biểu đồ cột giờ ngủ ở tab Phân tích.
 *
 * Nguồn là `health_data_history` (cùng nguồn với thẻ "Lịch sử sức khỏe 7 ngày"), gồm cả giờ ngủ do
 * **app tự ước lượng từ cảm biến** khi máy không có Health Connect — nhờ vậy thẻ giấc ngủ hoạt động
 * trên mọi máy, không còn "đợi" dữ liệu pha Sâu/Nông/REM từ Health Connect.
 */
data class SleepTrend(
    /** Danh sách đêm theo thứ tự **cũ → mới**, luôn đủ số đêm của cửa sổ. */
    val nights: List<SleepNight> = emptyList()
) {
    val hasData: Boolean get() = nights.any { it.hasData }

    /** Các đêm có số liệu thật (bỏ đêm chưa ghi được gì) — dùng để tính trung bình. */
    val nightsWithData: List<SleepNight> get() = nights.filter { it.hasData }

    /** Số giờ ngủ trung bình mỗi đêm (chỉ tính đêm có dữ liệu); `0.0` khi chưa có đêm nào. */
    val averageHours: Double
        get() = if (nightsWithData.isEmpty()) 0.0 else nightsWithData.sumOf { it.hours } / nightsWithData.size
}

/**
 * Đánh giá chất lượng giấc ngủ theo **số giờ ngủ trung bình mỗi đêm**.
 *
 * Đây là đánh giá theo thời lượng (không phải theo pha) nên áp dụng được cho mọi máy; khi có dữ liệu
 * pha từ Health Connect thì UI hiển thị thêm phần Sâu/Nông/REM.
 */
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
