package com.healthyai.app.data.healthconnect

import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.SleepStageBreakdown
import java.time.Instant

/**
 * Interface cho Data Source đọc dữ liệu từ Health Connect.
 */
interface HealthConnectDataSource {
    /** Kiểm tra Health Connect có sẵn sàng trên thiết bị không */
    fun isAvailable(): Boolean

    /** Trạng thái Health Connect: sẵn sàng / chưa cài / cần cập nhật */
    fun getStatus(): HealthConnectStatus

    /** Trả về danh sách permissions cần xin */
    val requiredPermissions: Set<String>

    /** Kiểm tra xem tất cả permissions cần thiết đã được cấp chưa */
    suspend fun hasAllPermissions(): Boolean

    /** Đọc tổng số bước chân từ đầu ngày hôm nay */
    suspend fun getTodaySteps(): Long

    /** Đọc tổng thời gian ngủ từ chiều tối qua đến hiện tại (giờ) */
    suspend fun getTodaySleepHours(): Double

    /**
     * Đọc nhịp tim **trung bình của ngày hôm nay** (từ 00:00 tới hiện tại).
     *
     * Cùng mốc thời gian với [getTodaySteps]/[getTodayActiveCalories] nên sang ngày mới là số được
     * đọc lại từ đầu ngày, không còn dính số của hôm qua.
     */
    suspend fun getTodayHeartRate(): Double

    /** Đọc tổng calories tiêu thụ trong ngày hôm nay */
    suspend fun getTodayActiveCalories(): Double

    /**
     * Đọc toàn bộ phiên ngủ trong khoảng [start, end] và cộng dồn thời lượng:
     * **theo từng pha** (Deep / Light / REM) khi thiết bị có chia pha, và **theo phiên** khi thiết bị
     * chỉ báo `STAGE_TYPE_SLEEPING` — nhờ vậy thẻ giấc ngủ không rỗng dù máy không chia pha
     * (phụ lục C #25). Trả về [SleepStageBreakdown] rỗng khi thiếu quyền hoặc lỗi.
     */
    suspend fun getSleepStageBreakdown(start: Instant, end: Instant): SleepStageBreakdown
}
