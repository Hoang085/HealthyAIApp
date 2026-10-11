package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.settings.SettingsDao
import com.healthyai.app.data.local.entity.settings.SettingEntity
import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.SleepSchedule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Unit test cho [SettingsRepositoryImpl] — chỗ dễ sai khi thêm thiết lập mới: giá trị thiếu/hỏng phải rơi
 * về **mặc định** (23:00 → 07:00) chứ không được làm app mất khung để đo, và khung không hợp lệ thì
 * **không** được ghi vào DB.
 */
class SettingsRepositoryImplTest {

    private val settingsDao = mockk<SettingsDao>()
    private val repository = SettingsRepositoryImpl(settingsDao)

    private fun keys(bedtimeMinutes: Int, wakeMinutes: Int) = listOf(
        SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_BEDTIME_MINUTES, bedtimeMinutes.toString()),
        SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_WAKE_MINUTES, wakeMinutes.toString())
    )

    @Test
    fun `the schedule is read from the stored minutes`() = runTest {
        coEvery { settingsDao.getAll() } returns keys(bedtimeMinutes = 22 * 60 + 30, wakeMinutes = 6 * 60 + 15)

        val schedule = repository.getSleepSchedule()

        assertEquals(SleepSchedule(LocalTime.of(22, 30), LocalTime.of(6, 15)), schedule)
    }

    @Test
    fun `an empty settings table falls back to the default window`() = runTest {
        coEvery { settingsDao.getAll() } returns emptyList()

        assertEquals(SleepSchedule.DEFAULT, repository.getSleepSchedule())
    }

    @Test
    fun `a half stored or corrupt schedule falls back to the default window`() = runTest {
        coEvery { settingsDao.getAll() } returns listOf(
            SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_BEDTIME_MINUTES, "1380")
        )
        assertEquals(
            "Thiếu giờ thức → mặc định",
            SleepSchedule.DEFAULT,
            repository.getSleepSchedule()
        )

        coEvery { settingsDao.getAll() } returns keys(bedtimeMinutes = 1380, wakeMinutes = 1380)
        assertEquals(
            "Hai mốc trùng nhau (khung rỗng) → mặc định",
            SleepSchedule.DEFAULT,
            repository.getSleepSchedule()
        )

        coEvery { settingsDao.getAll() } returns listOf(
            SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_BEDTIME_MINUTES, "khong-phai-so"),
            SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_WAKE_MINUTES, "99999")
        )
        assertEquals(
            "Giá trị không phải số / ngoài khoảng 0..1439 → mặc định",
            SleepSchedule.DEFAULT,
            repository.getSleepSchedule()
        )
    }

    @Test
    fun `saving writes both marks as minutes of the day`() = runTest {
        val saved = slot<List<SettingEntity>>()
        coJustRun { settingsDao.putAll(capture(saved)) }

        repository.setSleepSchedule(SleepSchedule(LocalTime.of(23, 30), LocalTime.of(6, 45)))

        assertEquals(
            listOf(
                SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_BEDTIME_MINUTES, "1410"),
                SettingEntity(SettingsRepositoryImpl.KEY_SLEEP_WAKE_MINUTES, "405")
            ),
            saved.captured
        )
    }

    @Test
    fun `an invalid schedule is never written to the database`() = runTest {
        repository.setSleepSchedule(SleepSchedule(LocalTime.of(7, 0), LocalTime.of(7, 0)))

        coVerify(exactly = 0) { settingsDao.putAll(any()) }
    }

    @Test
    fun `observe emits the default window when nothing was set yet`() = runTest {
        every { settingsDao.observeAll() } returns flowOf(emptyList())

        assertEquals(SleepSchedule.DEFAULT, repository.observeSleepSchedule().first())
    }

    @Test
    fun `observe emits the stored window`() = runTest {
        every { settingsDao.observeAll() } returns flowOf(keys(bedtimeMinutes = 0, wakeMinutes = 8 * 60))

        assertEquals(
            SleepSchedule(LocalTime.of(0, 0), LocalTime.of(8, 0)),
            repository.observeSleepSchedule().first()
        )
    }

    // ── Đồng bộ đám mây (mục 3 #31) ─────────────────────────────

    @Test
    fun `cloud sync defaults to off when nothing was stored`() = runTest {
        coEvery { settingsDao.getAll() } returns emptyList()

        assertEquals(CloudSyncSettings(), repository.getCloudSync())
    }

    @Test
    fun `corrupt cloud sync values fall back to off`() = runTest {
        // Giá trị hỏng ⇒ TẮT (không tải dữ liệu lên khi chưa chắc người dùng đã đồng ý).
        coEvery { settingsDao.getAll() } returns listOf(
            SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_ENABLED, "co-le-nao-do"),
            SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_CONSENT_AT, "khong-phai-so")
        )

        assertEquals(CloudSyncSettings(), repository.getCloudSync())
    }

    @Test
    fun `enabling cloud sync stores the flag and the consent moment`() = runTest {
        val saved = slot<List<SettingEntity>>()
        coJustRun { settingsDao.putAll(capture(saved)) }

        repository.setCloudSyncEnabled(enabled = true, nowMillis = 1_700_000_000_000L)

        assertEquals(
            listOf(
                SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_ENABLED, "true"),
                SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_CONSENT_AT, "1700000000000")
            ),
            saved.captured
        )
    }

    @Test
    fun `disabling cloud sync only clears the flag`() = runTest {
        val saved = slot<List<SettingEntity>>()
        coJustRun { settingsDao.putAll(capture(saved)) }

        repository.setCloudSyncEnabled(enabled = false, nowMillis = 1_700_000_000_000L)

        assertEquals(
            "Tắt đồng bộ chỉ ghi cờ; KHÔNG ghi mốc đồng ý mới (và không xoá dữ liệu đã tải lên)",
            listOf(SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_ENABLED, "false")),
            saved.captured
        )
    }

    @Test
    fun `observe emits the stored cloud sync state`() = runTest {
        every { settingsDao.observeAll() } returns flowOf(
            listOf(
                SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_ENABLED, "true"),
                SettingEntity(SettingsRepositoryImpl.KEY_CLOUD_SYNC_CONSENT_AT, "1700000000000")
            )
        )

        assertEquals(
            CloudSyncSettings(enabled = true, consentAtMillis = 1_700_000_000_000L),
            repository.observeCloudSync().first()
        )
    }
}
