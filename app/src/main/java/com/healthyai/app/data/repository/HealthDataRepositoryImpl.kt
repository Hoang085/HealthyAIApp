package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val dataSource: HealthConnectDataSource,
    private val healthDataDao: HealthDataDao
) : HealthDataRepository {

    override suspend fun isAvailable(): Boolean = dataSource.isAvailable()
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
        if (!dataSource.isAvailable()) return@runCatching
        if (!dataSource.hasAllPermissions()) return@runCatching

        val steps = dataSource.getTodaySteps()
        val sleep = dataSource.getTodaySleepHours()
        val heartRate = dataSource.getRecentHeartRate()
        val calories = dataSource.getTodayActiveCalories()

        val healthData = HealthData(
            steps = steps,
            sleepHours = sleep,
            heartRate = heartRate,
            activeCalories = calories
        )

        // Lưu vào Room Database để theo dõi lịch sử
        healthDataDao.insertHealthData(HealthDataEntity.fromDomain(healthData))
        
        // Dọn dẹp dữ liệu cũ (hơn 30 ngày)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        healthDataDao.deleteOldData(thirtyDaysAgo)

        android.util.Log.d("HealthDataRepository", "Background sync completed and saved to Room: $steps steps today")
    }

    override suspend fun getRecentHealthHistory(limit: Int): List<HealthData> {
        return healthDataDao.getRecentHealthDataList(limit).map { it.toDomain() }
    }

    override fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>> {
        return healthDataDao.getRecentHealthData().map { list ->
            list.map { it.toDomain() }
        }
    }
}
