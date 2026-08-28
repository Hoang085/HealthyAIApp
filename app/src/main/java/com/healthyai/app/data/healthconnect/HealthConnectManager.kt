package com.healthyai.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthConnectManager — Wrapper cho Health Connect API.
 *
 * Lưu ý quan trọng cho báo cáo:
 * Health Connect là API cục bộ (Android IPC), giao tiếp với OS,
 * KHÔNG phải server — không gửi dữ liệu qua mạng.
 * Dữ liệu chảy: Samsung Health/Google Fit → Health Connect (OS) → App của bạn
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

    /** Đọc tổng số bước chân từ đầu ngày hôm nay */
    suspend fun getTodaySteps(): Long {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            0L
        }
    }

    /** Đọc tổng thời gian ngủ đêm qua (tính bằng giờ) */
    suspend fun getLastNightSleepHours(): Double {
        val now = Instant.now()
        val yesterday = now.minusSeconds(86_400)

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            )
            response.records.sumOf { record ->
                (record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 3_600_000.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    /** Đọc nhịp tim trung bình trong 1 giờ gần nhất */
    suspend fun getRecentHeartRate(): Double {
        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3_600)

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(oneHourAgo, now)
                )
            )
            val samples = response.records.flatMap { it.samples }
            if (samples.isEmpty()) 0.0
            else samples.map { it.beatsPerMinute.toDouble() }.average()
        } catch (e: Exception) {
            0.0
        }
    }
}
