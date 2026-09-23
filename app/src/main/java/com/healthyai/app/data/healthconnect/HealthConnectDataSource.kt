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

    /** Đọc nhịp tim trung bình trong 1 giờ gần nhất */
    suspend fun getRecentHeartRate(): Double

    /** Đọc tổng calories tiêu thụ trong ngày hôm nay */
    suspend fun getTodayActiveCalories(): Double

    /**
     * Đọc toàn bộ phiên ngủ trong khoảng [start, end] và cộng dồn thời lượng theo từng pha
     * (Deep / Light / REM). Trả về [SleepStageBreakdown] rỗng khi thiếu quyền hoặc lỗi.
     */
    suspend fun getSleepStageBreakdown(start: Instant, end: Instant): SleepStageBreakdown
}
