package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Triển khai [HealthDataRepository]: ưu tiên Health Connect, thiếu số thì dùng cảm biến của máy; mọi lần ghi hôm nay đều vẽ lại widget. */
@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val dataSource: HealthConnectDataSource,
    private val healthDataDao: HealthDataDao,
    private val stepCounterRepository: StepCounterRepository,
    private val sleepTrackerRepository: SleepTrackerRepository,
    private val healthWidgetRefresher: HealthWidgetRefresher
) : HealthDataRepository {

    override suspend fun isAvailable(): Boolean = dataSource.isAvailable()
    override suspend fun getStatus(): HealthConnectStatus = dataSource.getStatus()
    override suspend fun hasAllPermissions(): Boolean = dataSource.hasAllPermissions()
    override suspend fun getRequiredPermissions(): Set<String> = dataSource.requiredPermissions
    override suspend fun getTodaySteps(): Long = dataSource.getTodaySteps()
    override suspend fun getTodaySleepHours(): Double = dataSource.getTodaySleepHours()
    override suspend fun getTodayHeartRate(): Double = dataSource.getTodayHeartRate()

    override suspend fun getTodayHealthData(): HealthData = HealthData(
        steps = dataSource.getTodaySteps(),
        sleepHours = dataSource.getTodaySleepHours(),
        heartRate = dataSource.getTodayHeartRate(),
        activeCalories = dataSource.getTodayActiveCalories()
    )

    override suspend fun syncHealthData(): Result<Unit> = runCatchingCancellable {
        val healthConnect = readHealthConnectSnapshot()
        val sensorSteps = stepCounterRepository.readTodaySteps()
        val sensorSleepHours = sleepTrackerRepository.readLastNightSleepHours()

        // Bước chân và giấc ngủ đều ưu tiên Health Connect; không có số mới dùng cảm biến của máy (SleepTrackerRepository).
        val snapshot = HealthData(
            steps = (healthConnect?.steps ?: 0L).takeIf { it > 0L } ?: sensorSteps,
            sleepHours = (healthConnect?.sleepHours ?: 0.0).takeIf { it > 0.0 } ?: sensorSleepHours,
            heartRate = healthConnect?.heartRate ?: 0.0,
            activeCalories = healthConnect?.activeCalories ?: 0.0
        )

        // Không có dữ liệu thật → không ghi hàng toàn số 0 để tránh làm nhiễu phân tích AI ở tab Phân tích.
        if (!snapshot.hasAnyData()) {
            android.util.Log.d(
                "HealthDataRepository",
                "Bỏ qua đồng bộ: chưa có dữ liệu sức khỏe thật (Health Connect trống và cảm biến không có số)"
            )
            return@runCatchingCancellable
        }

        upsertToday(snapshot)

        // Dọn dẹp dữ liệu cũ (hơn 30 ngày)
        healthDataDao.deleteOldData(System.currentTimeMillis() - RETENTION_MS)

        // Vẽ lại widget "Sức khỏe hôm nay" để bước chân / giấc ngủ / nhịp tim vừa đồng bộ hiện ra ngay.
        healthWidgetRefresher.refresh()

        android.util.Log.d(
            "HealthDataRepository",
            "Đồng bộ xong và lưu vào Room: ${snapshot.steps} bước, " +
                "${snapshot.sleepHours} giờ ngủ hôm nay"
        )
    }

    /** Đọc bản chụp từ Health Connect; trả null khi không khả dụng hoặc chưa đủ quyền (khi đó bước chân lấy từ cảm biến). */
    private suspend fun readHealthConnectSnapshot(): HealthData? {
        if (!dataSource.isAvailable()) return null
        if (!dataSource.hasAllPermissions()) return null

        return HealthData(
            steps = dataSource.getTodaySteps(),
            sleepHours = dataSource.getTodaySleepHours(),
            heartRate = dataSource.getTodayHeartRate(),
            activeCalories = dataSource.getTodayActiveCalories()
        )
    }

    /** Ghi bản chụp vào hàng hôm nay (gộp): bước chân lấy số lớn hơn, chỉ số khác lấy giá trị mới hơn nếu có số. */
    private suspend fun upsertToday(snapshot: HealthData) {
        val existing = healthDataDao.getLatestSince(startOfToday())

        if (existing == null) {
            healthDataDao.insertHealthData(HealthDataEntity.fromDomain(snapshot))
            return
        }

        healthDataDao.updateHealthData(
            existing.copy(
                steps = maxOf(existing.steps, snapshot.steps),
                sleepHours = if (snapshot.sleepHours > 0.0) snapshot.sleepHours else existing.sleepHours,
                heartRate = if (snapshot.heartRate > 0.0) snapshot.heartRate else existing.heartRate,
                activeCalories = if (snapshot.activeCalories > 0.0) snapshot.activeCalories else existing.activeCalories,
                recordedAt = System.currentTimeMillis()
            )
        )
    }

    /** Mốc 00:00 của hôm nay theo múi giờ thiết bị (epoch millis) — mọi mốc "hôm nay" đều dùng chung. */
    private fun startOfToday(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** Bản chụp có ít nhất một chỉ số thật (khác 0) hay không. */
    private fun HealthData.hasAnyData(): Boolean =
        steps > 0L || sleepHours > 0.0 || heartRate > 0.0 || activeCalories > 0.0

    private companion object {
        /** Giữ lịch sử sức khỏe 30 ngày (dọn mỗi lần đồng bộ). */
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }

    override suspend fun getRecentHealthHistory(limit: Int): List<HealthData> {
        return healthDataDao.getRecentHealthDataList(limit.coerceAtLeast(1)).map { it.toDomain() }
    }

    override suspend fun getLatestRecordedHeartRate(): Double? =
        healthDataDao.getLatestHeartRateSince(startOfToday())?.takeIf { it > 0.0 }

    /** Lưu nhịp tim vừa đo vào hàng hôm nay qua [upsertToday] (không ghi đè chỉ số khác) rồi vẽ lại widget ngay, tránh ô Nhịp tim cũ tới 30 phút. */
    override suspend fun saveMeasuredHeartRate(bpm: Double): Result<Unit> = runCatchingCancellable {
        require(bpm > 0.0) { "Nhịp tim đo được phải lớn hơn 0 (nhận được $bpm)" }
        upsertToday(HealthData(heartRate = bpm, recordedAt = System.currentTimeMillis()))
        healthWidgetRefresher.refresh()
    }

    /** Dòng mới nhất của mỗi ngày, tối đa [limit] ngày; bảng ghi 1 hàng mỗi lần đồng bộ nên tầng dùng vẫn phải gom theo ngày. */
    override fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>> {
        return healthDataDao.getRecentHealthData(limit.coerceAtLeast(1)).map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Bản chụp hôm nay đã ghi trong health_data_history (rỗng nếu chưa có); nguồn cho thẻ Tóm tắt sức khỏe khi tắt switch Health Connect. */
    override suspend fun getTodayStoredHealthData(): HealthData =
        healthDataDao.getLatestSince(startOfToday())?.toDomain() ?: HealthData()

    override fun getHealthHistorySince(fromTs: Long): Flow<List<HealthData>> =
        healthDataDao.getHealthDataSince(fromTs).map { list -> list.map { it.toDomain() } }

    /** Đọc chi tiết pha giấc ngủ [daysBack] ngày trực tiếp từ Health Connect (căn đầu ngày), không qua bảng Room nên không cần đổi schema. */
    override suspend fun getSleepStageBreakdown(daysBack: Int): SleepStageBreakdown {
        if (!dataSource.isAvailable()) return SleepStageBreakdown()

        val zone = ZoneId.systemDefault()
        val end = Instant.now()
        val start = LocalDate.now(zone)
            .minusDays(daysBack.coerceAtLeast(0).toLong())
            .atStartOfDay(zone)
            .toInstant()

        return dataSource.getSleepStageBreakdown(start, end)
    }
}
