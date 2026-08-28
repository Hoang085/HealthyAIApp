package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.HealthData
import java.time.Instant

/**
 * Repository interface cho dữ liệu Health Connect.
 * Health Connect là API cục bộ (Android IPC) — không phải server, không qua mạng.
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
}
