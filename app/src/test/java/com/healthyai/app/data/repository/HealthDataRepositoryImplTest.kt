package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
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
import org.junit.Assert.assertNull
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
    private val sleepTrackerRepository = mockk<SleepTrackerRepository>()

    /** Widget màn hình chính phải được vẽ lại mỗi khi dữ liệu sức khỏe hôm nay thay đổi. */
    private val widgetRefresher = mockk<HealthWidgetRefresher>(relaxed = true)

    @Before
    fun setup() {
        fakeDataSource = FakeHealthConnectDataSource()
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 0L
        coEvery { sleepTrackerRepository.readLastNightSleepHours() } returns 0.0
        repository = HealthDataRepositoryImpl(
            fakeDataSource,
            healthDataDao,
            stepCounterRepository,
            sleepTrackerRepository,
            widgetRefresher
        )
    }

    @Test
    fun `getTodayHealthData returns correct data from data source`() = runTest {
        // Arrange — tên hàm đã nói rõ "hôm nay"; start/end (nếu còn) bị bỏ qua
        fakeDataSource.steps = 1000
        fakeDataSource.sleepHours = 7.5
        fakeDataSource.heartRate = 72.0
        fakeDataSource.calories = 300.0

        // Act
        val result = repository.getTodayHealthData()

        // Assert
        assertEquals(1000L, result.steps)
        assertEquals(7.5, result.sleepHours, 0.01)
        assertEquals(72.0, result.heartRate, 0.01)
        assertEquals(300.0, result.activeCalories, 0.01)
    }

    @Test
    fun `getTodayStoredHealthData returns today's snapshot from the app database`() = runTest {
        // Arrange — hàng của hôm nay đã ghi trong `health_data_history`
        coEvery { healthDataDao.getLatestSince(any()) } returns HealthDataEntity(
            id = 1L,
            steps = 3_210L,
            sleepHours = 7.25,
            heartRate = 68.0,
            activeCalories = 240.0,
            recordedAt = System.currentTimeMillis()
        )

        // Act
        val result = repository.getTodayStoredHealthData()

        // Assert
        assertEquals(3_210L, result.steps)
        assertEquals(7.25, result.sleepHours, 0.01)
        assertEquals(68.0, result.heartRate, 0.01)
        assertEquals(240.0, result.activeCalories, 0.01)
    }

    @Test
    fun `getTodayStoredHealthData returns empty data when nothing was stored today`() = runTest {
        coEvery { healthDataDao.getLatestSince(any()) } returns null

        assertEquals(HealthData(), repository.getTodayStoredHealthData())
    }

    @Test
    fun `getRecentHealthHistoryFlow asks the dao for the requested limit`() = runTest {
        // Arrange — DAO từng hard-code LIMIT 30 nên tham số limit từng bị bỏ qua
        every { healthDataDao.getRecentHealthData(7) } returns flowOf(emptyList<HealthDataEntity>())

        // Act
        val result = repository.getRecentHealthHistoryFlow(7).first()

        // Assert
        assertTrue(result.isEmpty())
        verify { healthDataDao.getRecentHealthData(7) }
    }

    // Đồng bộ vào health_data_history (bước chân có thể đến từ cảm biến của máy)

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
    fun `sync and stored snapshot ignore health connect steps when the source switch is off`() = runTest {
        val settingsRepository = mockk<com.healthyai.app.domain.repository.SettingsRepository>()
        coEvery { settingsRepository.getHealthConnectData() } returns
            com.healthyai.app.domain.model.HealthConnectDataSettings(enabled = false)
        every { stepCounterRepository.getStatus() } returns
            StepCounterStatus(sensorAvailable = true, permissionGranted = true)
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 27L
        fakeDataSource.steps = 1_200L
        coEvery { healthDataDao.getLatestSince(any()) } returns HealthDataEntity(
            id = 1L,
            steps = 1_200L,
            sleepHours = 0.0,
            heartRate = 0.0,
            activeCalories = 0.0,
            recordedAt = startOfToday() + 1_000L
        )
        val updated = slot<HealthDataEntity>()
        coJustRun { healthDataDao.updateHealthData(capture(updated)) }

        val repoWithSettings = HealthDataRepositoryImpl(
            fakeDataSource,
            healthDataDao,
            stepCounterRepository,
            sleepTrackerRepository,
            widgetRefresher,
            settingsRepository
        )

        repoWithSettings.syncHealthData()
        val stored = repoWithSettings.getTodayStoredHealthData()

        assertEquals(27L, updated.captured.steps)
        assertEquals(27L, stored.steps)
    }

    // Giờ ngủ cũng có nguồn "tự lực" từ cảm biến của máy

    @Test
    fun `sync uses the device sleep estimate when health connect has no sleep`() = runTest {
        // Arrange — máy không có ứng dụng sức khỏe ghi giấc ngủ, nhưng bộ ước lượng từ cảm biến cho 6,5 giờ
        fakeDataSource.steps = 3_000L
        fakeDataSource.sleepHours = 0.0
        coEvery { sleepTrackerRepository.readLastNightSleepHours() } returns 6.5
        coEvery { healthDataDao.getLatestSince(any()) } returns null
        val saved = slot<HealthDataEntity>()
        coJustRun { healthDataDao.insertHealthData(capture(saved)) }

        // Act
        val result = repository.syncHealthData()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(6.5, saved.captured.sleepHours, 0.01)
    }

    @Test
    fun `sync prefers health connect sleep over the device estimate`() = runTest {
        // Arrange — Health Connect có giấc ngủ của đồng hồ đeo tay (7,5 giờ), bộ ước lượng nói 6 giờ
        fakeDataSource.steps = 3_000L
        fakeDataSource.sleepHours = 7.5
        coEvery { sleepTrackerRepository.readLastNightSleepHours() } returns 6.0
        coEvery { healthDataDao.getLatestSince(any()) } returns null
        val saved = slot<HealthDataEntity>()
        coJustRun { healthDataDao.insertHealthData(capture(saved)) }

        // Act
        repository.syncHealthData()

        // Assert — số của Health Connect (đồng hồ đeo tay) thắng
        assertEquals(7.5, saved.captured.sleepHours, 0.01)
    }

    @Test
    fun `sync keeps the sleep estimate already recorded today when a later sync has none`() = runTest {
        // Arrange — hàng hôm nay đã có giờ ngủ; lần đồng bộ này cả Health Connect lẫn bộ ước lượng đều trả 0
        val existing = HealthDataEntity(
            id = 11L,
            steps = 1_200L,
            sleepHours = 6.5,
            heartRate = 0.0,
            activeCalories = 0.0,
            recordedAt = startOfToday() + 1000L
        )
        fakeDataSource.steps = 1_800L
        fakeDataSource.sleepHours = 0.0
        coEvery { healthDataDao.getLatestSince(any()) } returns existing
        val updated = slot<HealthDataEntity>()
        coJustRun { healthDataDao.updateHealthData(capture(updated)) }

        // Act
        repository.syncHealthData()

        // Assert
        assertEquals(6.5, updated.captured.sleepHours, 0.01)
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
        // Arrange — hàng hôm nay đã có ngủ/nhịp tim; lần này chỉ có bước chân từ cảm biến, không được xoá dữ liệu cũ
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

    // Vẽ lại widget màn hình chính "Sức khỏe hôm nay"

    /** Widget chỉ đọc dữ liệu một lần lúc vẽ và nhịp vẽ 30 phút, nên phải vẽ lại ngay sau khi đồng bộ. */
    @Test
    fun `a successful sync refreshes the home screen widget`() = runTest {
        // Arrange — có dữ liệu thật để lần đồng bộ này thực sự ghi vào Room
        fakeDataSource.steps = 1_500L

        // Act
        repository.syncHealthData()

        // Assert
        coVerify(exactly = 1) { widgetRefresher.refresh() }
    }

    /** Đồng bộ không có số liệu thì không ghi DB và cũng không cần vẽ lại widget. */
    @Test
    fun `a sync without any data does not refresh the home screen widget`() = runTest {
        fakeDataSource.available = false
        coEvery { stepCounterRepository.readTodaySteps(any()) } returns 0L

        repository.syncHealthData()

        coVerify(exactly = 0) { widgetRefresher.refresh() }
    }

    // Nhịp tim do app tự đo (màn "Đo nhịp tim" bằng camera)

    @Test
    fun `a measurement is saved into today's row when there is none yet`() = runTest {
        // Arrange — hôm nay chưa có hàng nào
        coEvery { healthDataDao.getLatestSince(any()) } returns null
        val saved = slot<HealthDataEntity>()
        coJustRun { healthDataDao.insertHealthData(capture(saved)) }

        // Act
        val result = repository.saveMeasuredHeartRate(78.0)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(78.0, saved.captured.heartRate, 0.01)
        assertTrue("Phải ghi vào hàng của hôm nay", saved.captured.recordedAt >= startOfToday())
    }

    @Test
    fun `a measurement only replaces the heart rate of today's row`() = runTest {
        // Arrange — hàng hôm nay đã có bước chân, giờ ngủ và calo
        val existing = HealthDataEntity(
            id = 21L,
            steps = 4_200L,
            sleepHours = 7.5,
            heartRate = 60.0,
            activeCalories = 350.0,
            recordedAt = startOfToday() + 1000L
        )
        coEvery { healthDataDao.getLatestSince(any()) } returns existing
        val updated = slot<HealthDataEntity>()
        coJustRun { healthDataDao.updateHealthData(capture(updated)) }

        // Act
        repository.saveMeasuredHeartRate(92.0)

        // Assert — chỉ nhịp tim đổi, các chỉ số khác của ngày giữ nguyên
        assertEquals(21L, updated.captured.id)
        assertEquals(92.0, updated.captured.heartRate, 0.01)
        assertEquals(4_200L, updated.captured.steps)
        assertEquals(7.5, updated.captured.sleepHours, 0.01)
        assertEquals(350.0, updated.captured.activeCalories, 0.01)
        coVerify(exactly = 0) { healthDataDao.insertHealthData(any()) }
    }

    @Test
    fun `an invalid measurement is refused instead of being written`() = runTest {
        val result = repository.saveMeasuredHeartRate(0.0)

        assertTrue("Nhịp 0 không phải số đo thật", result.isFailure)
        coVerify(exactly = 0) { healthDataDao.insertHealthData(any()) }
        coVerify(exactly = 0) { healthDataDao.updateHealthData(any()) }
    }

    /** Đo nhịp tim xong phải vẽ lại widget để ô "Nhịp tim" hiện số mới ngay, không chờ 30 phút. */
    @Test
    fun `a saved measurement refreshes the home screen widget`() = runTest {
        coEvery { healthDataDao.getLatestSince(any()) } returns null

        repository.saveMeasuredHeartRate(78.0)

        coVerify(exactly = 1) { widgetRefresher.refresh() }
    }

    /** Số đo không hợp lệ bị từ chối trước khi ghi nên cũng không có gì mới để vẽ lại widget. */
    @Test
    fun `an invalid measurement does not refresh the home screen widget`() = runTest {
        repository.saveMeasuredHeartRate(-1.0)

        coVerify(exactly = 0) { widgetRefresher.refresh() }
    }

    @Test
    fun `the recorded heart rate is read from today's rows only`() = runTest {
        // Arrange — sang ngày mới, số của hôm qua không được tính (nhịp tim cũng "đọc lại từ đầu ngày")
        coEvery { healthDataDao.getLatestHeartRateSince(any()) } returns 76.0

        val recorded = repository.getLatestRecordedHeartRate()

        assertEquals(76.0, recorded!!, 0.01)
        val askedFrom = slot<Long>()
        coVerify { healthDataDao.getLatestHeartRateSince(capture(askedFrom)) }
        assertEquals(
            "Phải hỏi đúng từ đầu ngày hôm nay",
            startOfToday(),
            askedFrom.captured
        )
    }

    @Test
    fun `no recorded heart rate today reads as null`() = runTest {
        coEvery { healthDataDao.getLatestHeartRateSince(any()) } returns null

        assertNull(repository.getLatestRecordedHeartRate())
    }

    @Test
    fun `a zero stored heart rate counts as no heart rate`() = runTest {
        // Hàng cũ có thể mang nhịp tim 0 (chưa từng đo, Health Connect cũng không có) → coi như chưa có
        coEvery { healthDataDao.getLatestHeartRateSince(any()) } returns 0.0

        assertNull(repository.getLatestRecordedHeartRate())
    }

    // Lịch sử tóm tắt sức khỏe 7 ngày (thẻ ở tab Phân tích)

    @Test
    fun `the health history window reads every row written since the start of the window`() = runTest {
        // Arrange — cắt theo mốc thời gian chứ không theo số hàng, để đủ dữ liệu cả 7 ngày
        val rows = listOf(
            HealthDataEntity(
                id = 1L,
                steps = 4_200L,
                sleepHours = 7.0,
                heartRate = 70.0,
                activeCalories = 300.0,
                recordedAt = startOfToday() + 1000L
            )
        )
        every { healthDataDao.getHealthDataSince(any()) } returns flowOf(rows)

        // Act
        val history = repository.getHealthHistorySince(startOfToday()).first()

        // Assert
        assertEquals(1, history.size)
        assertEquals(4_200L, history.single().steps)
        assertEquals(70.0, history.single().heartRate, 0.01)
        verify { healthDataDao.getHealthDataSince(startOfToday()) }
    }

    // Lớp giả (fake) phục vụ kiểm thử
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
        override suspend fun getTodayHeartRate(): Double = heartRate
        override suspend fun getTodayActiveCalories(): Double = calories

        override suspend fun getSleepStageBreakdown(
            start: Instant,
            end: Instant
        ): SleepStageBreakdown = sleepStages
    }
}
