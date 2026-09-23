package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.SleepStageBreakdown
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HealthDataRepositoryImplTest {

    private lateinit var repository: HealthDataRepositoryImpl
    private lateinit var fakeDataSource: FakeHealthConnectDataSource
    private val healthDataDao = mockk<HealthDataDao>(relaxed = true)

    @Before
    fun setup() {
        fakeDataSource = FakeHealthConnectDataSource()
        repository = HealthDataRepositoryImpl(fakeDataSource, healthDataDao)
    }

    @Test
    fun `getHealthData returns correct data from data source`() = runTest {
        // Arrange
        fakeDataSource.steps = 1000
        fakeDataSource.sleepHours = 7.5
        fakeDataSource.heartRate = 72.0
        fakeDataSource.calories = 300.0

        // Act
        val result = repository.getHealthData(Instant.now(), Instant.now())

        // Assert
        assertEquals(1000L, result.steps)
        assertEquals(7.5, result.sleepHours, 0.01)
        assertEquals(72.0, result.heartRate, 0.01)
        assertEquals(300.0, result.activeCalories, 0.01)
    }

    @Test
    fun `getRecentHealthHistoryFlow asks the dao for the requested limit`() = runTest {
        // Arrange — phụ lục C #16: DAO từng hard-code `LIMIT 30` nên tham số limit bị bỏ qua
        every { healthDataDao.getRecentHealthData(7) } returns flowOf(emptyList<HealthDataEntity>())

        // Act
        val result = repository.getRecentHealthHistoryFlow(7).first()

        // Assert
        assertTrue(result.isEmpty())
        verify { healthDataDao.getRecentHealthData(7) }
    }

    // Fake implementation for testing
    private class FakeHealthConnectDataSource : HealthConnectDataSource {
        var steps: Long = 0
        var sleepHours: Double = 0.0
        var heartRate: Double = 0.0
        var calories: Double = 0.0
        var sleepStages: SleepStageBreakdown = SleepStageBreakdown()

        override fun isAvailable(): Boolean = true
        override fun getStatus(): HealthConnectStatus = HealthConnectStatus.AVAILABLE
        override val requiredPermissions: Set<String> = emptySet()
        override suspend fun hasAllPermissions(): Boolean = true
        override suspend fun getTodaySteps(): Long = steps
        override suspend fun getTodaySleepHours(): Double = sleepHours
        override suspend fun getRecentHeartRate(): Double = heartRate
        override suspend fun getTodayActiveCalories(): Double = calories

        override suspend fun getSleepStageBreakdown(
            start: Instant,
            end: Instant
        ): SleepStageBreakdown = sleepStages
    }
}
