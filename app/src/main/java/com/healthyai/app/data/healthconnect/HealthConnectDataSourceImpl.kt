package com.healthyai.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.SleepStageBreakdown
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthConnectDataSourceImpl — Implementation của HealthConnectDataSource.
 */
@Singleton
class HealthConnectDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HealthConnectDataSource {

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    override fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override fun getStatus(): HealthConnectStatus =
        sdkStatusToHealthConnectStatus(HealthConnectClient.getSdkStatus(context))

    override suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            false
        }
    }

    /** 
     * Đọc tổng số bước chân từ đầu ngày hôm nay.
     */
    override suspend fun getTodaySteps(): Long {
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
     */
    override suspend fun getTodaySleepHours(): Double {
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
    override suspend fun getRecentHeartRate(): Double {
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

    /**
     * Đọc tổng calories tiêu thụ trong ngày hôm nay.
     */
    override suspend fun getTodayActiveCalories(): Double {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Đọc toàn bộ phiên ngủ trong khoảng thời gian và cộng dồn thời lượng theo từng pha.
     *
     * Lưu ý: chỉ những phiên ngủ có dữ liệu phân chia pha (Deep/Light/REM) mới được tính;
     * thiết bị chỉ báo `STAGE_TYPE_SLEEPING` (không chia pha) sẽ cho kết quả rỗng — UI sẽ
     * hiển thị trạng thái "chưa có dữ liệu chi tiết" thay vì số liệu sai.
     */
    override suspend fun getSleepStageBreakdown(start: Instant, end: Instant): SleepStageBreakdown {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            var deepMinutes = 0L
            var lightMinutes = 0L
            var remMinutes = 0L

            response.records.forEach { session ->
                session.stages.forEach { stage ->
                    val minutes = Duration.between(stage.startTime, stage.endTime).toMinutes()
                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += minutes
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMinutes += minutes
                        SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += minutes
                    }
                }
            }

            SleepStageBreakdown(
                deepMinutes = deepMinutes,
                lightMinutes = lightMinutes,
                remMinutes = remMinutes
            )
        } catch (e: Exception) {
            SleepStageBreakdown()
        }
    }
}

/**
 * Dịch mã trạng thái của `HealthConnectClient.getSdkStatus()` sang enum của domain.
 *
 * Tách riêng để test được trên JVM (không cần Context/thiết bị):
 * `SDK_AVAILABLE = 3`, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED = 2`, `SDK_UNAVAILABLE = 1`.
 */
internal fun sdkStatusToHealthConnectStatus(sdkStatus: Int): HealthConnectStatus = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
    else -> HealthConnectStatus.NOT_INSTALLED
}
