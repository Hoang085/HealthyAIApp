package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import kotlinx.coroutines.flow.Flow

/** Repository interface cho dữ liệu Health Connect. */
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

    /** Nhịp tim trung bình hôm nay (00:00 → hiện tại) từ Health Connect; `0.0` khi thiếu quyền hoặc chưa có mẫu. */
    suspend fun getTodayHeartRate(): Double

    /** Bản chụp dữ liệu sức khỏe hôm nay đọc trực tiếp từ Health Connect (bước chân, giờ ngủ, nhịp tim, calo). */
    suspend fun getTodayHealthData(): HealthData
    /** Ghi bản chụp sức khỏe hôm nay (ưu tiên Health Connect, thiếu thì dùng nguồn tự đo); gộp vào hàng sẵn có, bỏ qua khi mọi chỉ số đều 0. */
    suspend fun syncHealthData(): Result<Unit>

    /** Lấy danh sách dữ liệu sức khỏe lịch sử */
    suspend fun getRecentHealthHistory(limit: Int): List<HealthData>

    /** Nhịp tim ghi gần nhất trong hôm nay (từ `health_data_history`); `null` nếu chưa có, dùng làm dự phòng khi Health Connect không trả về. */
    suspend fun getLatestRecordedHeartRate(): Double?

    /** Lưu nhịp tim do app tự đo (camera) vào bản chụp hôm nay, chỉ thay nhịp tim và giữ nguyên các chỉ số khác; lỗi khi [bpm] ≤ 0 hoặc ghi DB thất bại. */
    suspend fun saveMeasuredHeartRate(bpm: Double): Result<Unit>

    /** Flow danh sách dữ liệu sức khỏe gần đây (Reactive) */
    fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>>

    /** Bản chụp sức khỏe đã lưu trong app cho hôm nay; rỗng khi chưa có hàng, là nguồn cho thẻ "Tóm tắt sức khỏe" khi tắt Health Connect. */
    suspend fun getTodayStoredHealthData(): HealthData

    /** Flow toàn bộ hàng sức khỏe ghi từ [fromTs] trở đi (mới nhất trước); cắt theo mốc thời gian để không thiếu ngày như cắt theo số hàng. */
    fun getHealthHistorySince(fromTs: Long): Flow<List<HealthData>>

    /** Tổng thời lượng từng pha ngủ (Deep/Light/REM) trong [daysBack] ngày, đọc trực tiếp từ Health Connect; rỗng khi không khả dụng hoặc thiếu quyền. */
    suspend fun getSleepStageBreakdown(daysBack: Int): SleepStageBreakdown
}
