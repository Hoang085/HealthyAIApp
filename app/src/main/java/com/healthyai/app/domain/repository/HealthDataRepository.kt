package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface cho dữ liệu Health Connect.
 */
interface HealthDataRepository {
    /** Kiểm tra Health Connect có sẵn sàng trên thiết bị không */
    suspend fun isAvailable(): Boolean

    /** Trạng thái Health Connect: sẵn sàng / chưa cài / cần cập nhật */
    suspend fun getStatus(): HealthConnectStatus

    /** Người dùng đã cấp đủ quyền đọc cần thiết chưa */
    suspend fun hasAllPermissions(): Boolean

    /** Trả về danh sách permissions cần xin */
    suspend fun getRequiredPermissions(): Set<String>

    suspend fun getTodaySteps(): Long
    suspend fun getTodaySleepHours(): Double
    suspend fun getRecentHeartRate(): Double
    suspend fun getHealthData(start: Instant, end: Instant): HealthData
    
    /**
     * Lưu một "bản chụp" dữ liệu sức khỏe vào `health_data_history` cho **hôm nay**.
     *
     * Nguồn dữ liệu: bước chân lấy từ Health Connect, nếu Health Connect trống thì lấy từ **cảm biến
     * đếm bước của máy**; giấc ngủ / nhịp tim / calo chỉ có từ Health Connect.
     *
     * - Chưa có hàng nào của hôm nay → thêm mới.
     * - Đã có hàng của hôm nay → **gộp** vào hàng đó (bước chân lấy số lớn hơn) thay vì thêm hàng trùng.
     * - Không có chỉ số nào thật (khác 0) → **không ghi gì** để không tạo hàng toàn số 0 làm nhiễu AI.
     */
    suspend fun syncHealthData(): Result<Unit>

    /** Lấy danh sách dữ liệu sức khỏe lịch sử */
    suspend fun getRecentHealthHistory(limit: Int): List<HealthData>

    /** Flow danh sách dữ liệu sức khỏe gần đây (Reactive) */
    fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>>

    /**
     * Tổng thời lượng từng pha giấc ngủ (Deep/Light/REM) trong [daysBack] ngày gần nhất,
     * đọc trực tiếp từ Health Connect (không qua bảng `health_data_history`).
     * Trả về [SleepStageBreakdown] rỗng khi Health Connect không khả dụng hoặc thiếu quyền.
     */
    suspend fun getSleepStageBreakdown(daysBack: Int): SleepStageBreakdown
}
