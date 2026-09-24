package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [HealthDataRepository].
 *
 * Ngoài Health Connect, repository này còn dùng **cảm biến đếm bước của máy** ([StepCounterRepository])
 * làm nguồn dự phòng cho bước chân — nhờ vậy `health_data_history` vẫn có dữ liệu bước chân (và AI ở
 * tab Phân tích dùng được) ngay cả khi máy không cài ứng dụng sức khỏe nào.
 */
@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val dataSource: HealthConnectDataSource,
    private val healthDataDao: HealthDataDao,
    private val stepCounterRepository: StepCounterRepository
) : HealthDataRepository {

    override suspend fun isAvailable(): Boolean = dataSource.isAvailable()
    override suspend fun getStatus(): HealthConnectStatus = dataSource.getStatus()
    override suspend fun hasAllPermissions(): Boolean = dataSource.hasAllPermissions()
    override suspend fun getRequiredPermissions(): Set<String> = dataSource.requiredPermissions
    override suspend fun getTodaySteps(): Long = dataSource.getTodaySteps()
    override suspend fun getTodaySleepHours(): Double = dataSource.getTodaySleepHours()
    override suspend fun getRecentHeartRate(): Double = dataSource.getRecentHeartRate()

    override suspend fun getHealthData(start: Instant, end: Instant): HealthData = HealthData(
        steps = dataSource.getTodaySteps(),
        sleepHours = dataSource.getTodaySleepHours(),
        heartRate = dataSource.getRecentHeartRate(),
        activeCalories = dataSource.getTodayActiveCalories()
    )

    override suspend fun syncHealthData(): Result<Unit> = runCatching {
        val healthConnect = readHealthConnectSnapshot()
        val sensorSteps = stepCounterRepository.readTodaySteps()

        // Bước chân: Health Connect trước (có thể gồm cả đồng hồ đeo tay), không có số thì dùng cảm biến của máy.
        val snapshot = HealthData(
            steps = (healthConnect?.steps ?: 0L).takeIf { it > 0L } ?: sensorSteps,
            sleepHours = healthConnect?.sleepHours ?: 0.0,
            heartRate = healthConnect?.heartRate ?: 0.0,
            activeCalories = healthConnect?.activeCalories ?: 0.0
        )

        // Không có dữ liệu thật (không có cảm biến, không cấp quyền, Health Connect trống) → không ghi
        // hàng toàn số 0, tránh làm nhiễu phân tích AI ở tab Phân tích.
        if (!snapshot.hasAnyData()) {
            android.util.Log.d(
                "HealthDataRepository",
                "Bỏ qua đồng bộ: chưa có dữ liệu sức khỏe thật (Health Connect trống và cảm biến không có số)"
            )
            return@runCatching
        }

        upsertToday(snapshot)

        // Dọn dẹp dữ liệu cũ (hơn 30 ngày)
        healthDataDao.deleteOldData(System.currentTimeMillis() - RETENTION_MS)

        android.util.Log.d(
            "HealthDataRepository",
            "Đồng bộ xong và lưu vào Room: ${snapshot.steps} bước hôm nay"
        )
    }

    /**
     * Đọc một "bản chụp" từ Health Connect; trả `null` khi Health Connect không khả dụng hoặc chưa được
     * cấp đủ quyền — lúc đó phần bước chân vẫn có thể đến từ cảm biến của máy.
     */
    private suspend fun readHealthConnectSnapshot(): HealthData? {
        if (!dataSource.isAvailable()) return null
        if (!dataSource.hasAllPermissions()) return null

        return HealthData(
            steps = dataSource.getTodaySteps(),
            sleepHours = dataSource.getTodaySleepHours(),
            heartRate = dataSource.getRecentHeartRate(),
            activeCalories = dataSource.getTodayActiveCalories()
        )
    }

    /**
     * Ghi bản chụp vào hàng của **hôm nay**: chưa có thì thêm mới, đã có thì **gộp** vào hàng đó.
     *
     * Gộp thay vì thêm hàng mới vì `HealthSyncWorker` chạy 4 lần/ngày; nếu thêm mỗi lần một hàng thì
     * lịch sử chỉ toàn dữ liệu trùng của cùng một ngày. Quy tắc gộp: **bước chân lấy số lớn hơn**
     * (số bước trong ngày chỉ tăng), các chỉ số khác lấy giá trị **mới hơn nếu có số** — nhờ vậy lần
     * đồng bộ không có quyền Health Connect **không** xoá mất dữ liệu giấc ngủ/nhịp tim đã ghi trước đó.
     */
    private suspend fun upsertToday(snapshot: HealthData) {
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val existing = healthDataDao.getLatestSince(startOfToday)

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

    /**
     * Dòng mới nhất của mỗi ngày, tối đa [limit] ngày gần nhất.
     *
     * Lưu ý dữ liệu: `health_data_history` ghi 1 hàng cho mỗi lần **đồng bộ** (6 giờ/lần, đã gộp vào
     * hàng của ngày) nên tầng dùng (Rule Engine) vẫn phải gom theo ngày — xem
     * `HabitRuleEngine.analyzeHealthTrends` (phụ lục C #16).
     */
    override fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>> {
        return healthDataDao.getRecentHealthData(limit.coerceAtLeast(1)).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Đọc chi tiết pha giấc ngủ trong [daysBack] ngày gần nhất **trực tiếp từ Health Connect**
     * (không qua bảng `health_data_history`) để thẻ "Xu hướng chất lượng giấc ngủ" luôn có
     * dữ liệu mới nhất mà không cần đổi schema Room.
     *
     * Khoảng thời gian được căn theo đầu ngày (00:00), thống nhất với truy vấn log thói quen.
     */
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
