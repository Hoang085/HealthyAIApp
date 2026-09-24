package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.StepCounterRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthDataRepositoryImplTest {

    private lateinit var repository: HealthDataRepositoryImpl
    private lateinit var fakeDataSource: FakeHealthConnectDataSource
    private val healthDataDao = mockk<HealthDataDao>(relaxed = true)
    private val stepCounterRepository = mockk<StepCounterRepository>()

    @Before
    fun setup() {
        fakeDataSource = FakeHealthConnectDataSource()
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 0L
        repository = HealthDataRepositoryImpl(fakeDataSource, healthDataDao, stepCounterRepository)
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

    // ── Đồng bộ vào health_data_history (bước chân có thể đến từ cảm biến của máy) ──

    @Test
    fun `sync uses the device step sensor when health connect is unavailable`() = runTest {
        // Arrange — máy chưa có Health Connect, nhưng cảm biến đếm bước cho 4 200 bước
        fakeDataSource.available = false
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 4_200L
        coEvery { healthDataDao.getLatestSince(any()) } returns null
        val saved = slot<HealthDataEntity>()
        coJustRun { healthDataDao.insertHealthData(capture(saved)) }

        // Act
        val result = repository.syncHealthData()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(4_200L, saved.captured.steps)
        assertTrue("Phải ghi hàng của hôm nay", saved.captured.recordedAt >= startOfToday())
    }

    @Test
    fun `sync prefers health connect steps over the device sensor`() = runTest {
        // Arrange
        fakeDataSource.steps = 9_000L
        fakeDataSource.sleepHours = 7.5
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 4_200L
        coEvery { healthDataDao.getLatestSince(any()) } returns null
        val saved = slot<HealthDataEntity>()
        coJustRun { healthDataDao.insertHealthData(capture(saved)) }

        // Act
        repository.syncHealthData()

        // Assert
        assertEquals(9_000L, saved.captured.steps)
        assertEquals(7.5, saved.captured.sleepHours, 0.01)
    }

    @Test
    fun `sync merges into today's row instead of inserting a duplicate`() = runTest {
        // Arrange — trong ngày đã có hàng (bước chân 1 000, ngủ 7 giờ); lần này cảm biến trả 3 500
        val existing = HealthDataEntity(
            id = 7L,
            steps = 1_000L,
            sleepHours = 7.0,
            heartRate = 60.0,
            activeCalories = 200.0,
            recordedAt = startOfToday() + 1000L
        )
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 3_500L
        coEvery { healthDataDao.getLatestSince(any()) } returns existing
        val updated = slot<HealthDataEntity>()
        coJustRun { healthDataDao.updateHealthData(capture(updated)) }

        // Act
        repository.syncHealthData()

        // Assert — gộp vào hàng cũ: bước chân lấy số lớn hơn, dữ liệu cũ của Health Connect được giữ
        assertEquals(7L, updated.captured.id)
        assertEquals(3_500L, updated.captured.steps)
        assertEquals(7.0, updated.captured.sleepHours, 0.01)
        coVerify(exactly = 0) { healthDataDao.insertHealthData(any()) }
    }

    @Test
    fun `sync writes nothing when there is no real health data at all`() = runTest {
        // Arrange — Health Connect không khả dụng và cảm biến không có số (chưa cấp quyền / máy không có cảm biến)
        fakeDataSource.available = false
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 0L

        // Act
        val result = repository.syncHealthData()

        // Assert — không ghi hàng toàn số 0 (sẽ làm nhiễu cảnh báo AI ở tab Phân tích)
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { healthDataDao.insertHealthData(any()) }
        coVerify(exactly = 0) { healthDataDao.updateHealthData(any()) }
    }

    @Test
    fun `sync keeps health connect data already recorded today when a later sync has no permissions`() = runTest {
        // Arrange — hàng hôm nay đã có ngủ/nhịp tim từ Health Connect; lần này Health Connect thiếu quyền,
        // chỉ có bước chân từ cảm biến → không được xoá dữ liệu cũ
        val existing = HealthDataEntity(
            id = 9L,
            steps = 500L,
            sleepHours = 6.5,
            heartRate = 58.0,
            activeCalories = 150.0,
            recordedAt = startOfToday() + 1000L
        )
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 2_000L
        coEvery { healthDataDao.getLatestSince(any()) } returns existing
        val updated = slot<HealthDataEntity>()
        coJustRun { healthDataDao.updateHealthData(capture(updated)) }

        // Act
        repository.syncHealthData()

        // Assert
        assertEquals(2_000L, updated.captured.steps)
        assertEquals(6.5, updated.captured.sleepHours, 0.01)
        assertEquals(58.0, updated.captured.heartRate, 0.01)
        assertEquals(150.0, updated.captured.activeCalories, 0.01)
    }

    private fun startOfToday(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Fake implementation for testing
    private class FakeHealthConnectDataSource : HealthConnectDataSource {
        var steps: Long = 0
        var sleepHours: Double = 0.0
        var heartRate: Double = 0.0
        var calories: Double = 0.0
        var sleepStages: SleepStageBreakdown = SleepStageBreakdown()

        /** `false` = máy chưa cài Health Connect. */
        var available: Boolean = true

        override fun isAvailable(): Boolean = available
        override fun getStatus(): HealthConnectStatus =
            if (available) HealthConnectStatus.AVAILABLE else HealthConnectStatus.NOT_INSTALLED
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
