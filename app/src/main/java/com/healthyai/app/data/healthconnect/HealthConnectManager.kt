package com.healthyai.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthConnectManager — Wrapper cho Health Connect API.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            false
        }
    }

    /** 
     * Đọc tổng số bước chân từ đầu ngày hôm nay.
     * Sử dụng aggregate API để đảm bảo không bị trùng lặp dữ liệu từ nhiều nguồn.
     */
    suspend fun getTodaySteps(): Long {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** 
     * Đọc tổng thời gian ngủ từ chiều tối qua đến hiện tại.
     * Thường tính từ 6h tối hôm qua đến hiện tại để bao quát giấc ngủ đêm.
     */
    suspend fun getLastNightSleepHours(): Double {
        val now = Instant.now()
        val startOfYesterday = LocalDate.now().minusDays(1)
            .atTime(LocalTime.of(18, 0)) // 18:00 hôm qua
            .atZone(ZoneId.systemDefault()).toInstant()

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfYesterday, now)
                )
            )
            val duration = response[SleepSessionRecord.SLEEP_DURATION_TOTAL]
            duration?.toMinutes()?.toDouble()?.div(60.0) ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /** 
     * Đọc nhịp tim trung bình trong 1 giờ gần nhất.
     */
    suspend fun getRecentHeartRate(): Double {
        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3_600)

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(oneHourAgo, now)
                )
            )
            response[HeartRateRecord.BPM_AVG]?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
