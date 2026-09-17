package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.HealthData
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface cho dữ liệu Health Connect.
 */
interface HealthDataRepository {
    /** Kiểm tra Health Connect có sẵn sàng trên thiết bị không */
    suspend fun isAvailable(): Boolean

    /** Trả về danh sách permissions cần xin */
    suspend fun getRequiredPermissions(): Set<String>

    suspend fun getTodaySteps(): Long
    suspend fun getTodaySleepHours(): Double
    suspend fun getRecentHeartRate(): Double
    suspend fun getHealthData(start: Instant, end: Instant): HealthData
    
    /** Đồng bộ dữ liệu từ Health Connect vào local database (nếu cần) */
    suspend fun syncHealthData(): Result<Unit>

    /** Lấy danh sách dữ liệu sức khỏe lịch sử */
    suspend fun getRecentHealthHistory(limit: Int): List<HealthData>

    /** Flow danh sách dữ liệu sức khỏe gần đây (Reactive) */
    fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>>
}
