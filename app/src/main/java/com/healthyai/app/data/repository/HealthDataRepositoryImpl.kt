package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectManager
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val manager: HealthConnectManager
) : HealthDataRepository {

    override suspend fun isAvailable(): Boolean = manager.isAvailable()
    override suspend fun getRequiredPermissions(): Set<String> = manager.requiredPermissions
    override suspend fun getTodaySteps(): Long = manager.getTodaySteps()
    override suspend fun getTodaySleepHours(): Double = manager.getLastNightSleepHours()
    override suspend fun getRecentHeartRate(): Double = manager.getRecentHeartRate()

    override suspend fun getHealthData(start: Instant, end: Instant): HealthData = HealthData(
        steps = manager.getTodaySteps(),
        sleepHours = manager.getLastNightSleepHours(),
        heartRate = manager.getRecentHeartRate()
    )
}
