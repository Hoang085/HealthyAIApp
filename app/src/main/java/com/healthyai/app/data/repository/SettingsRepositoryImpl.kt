package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.settings.SettingsDao
import com.healthyai.app.data.local.entity.settings.SettingEntity
import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.SleepSchedule
import com.healthyai.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [SettingsRepository] trên bảng Room `app_settings`.
 *
 * Giá trị lưu dưới dạng **phút trong ngày** (`0..1439`) chứ không phải `LocalTime.toString()` — đọc lại
 * bằng số nên không phụ thuộc locale/định dạng, và cũng không cần `TypeConverter`.
 *
 * Mọi trường hợp dữ liệu thiếu/hỏng đều rơi về [SleepSchedule.DEFAULT]: người dùng chưa đặt gì, khoá
 * chưa có, giá trị không phải số, ngoài khoảng `0..1439`, hoặc hai mốc trùng nhau.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun observeSleepSchedule(): Flow<SleepSchedule> =
        settingsDao.observeAll().map { it.toSleepSchedule() }

    override suspend fun getSleepSchedule(): SleepSchedule =
        settingsDao.getAll().toSleepSchedule()

    override suspend fun setSleepSchedule(schedule: SleepSchedule) {
        // Không ghi khung rỗng (hai mốc trùng nhau) để không tự phá dữ liệu đang đo.
        if (!schedule.isValid) return

        settingsDao.putAll(
            listOf(
                SettingEntity(KEY_SLEEP_BEDTIME_MINUTES, schedule.bedtime.toMinutesOfDay().toString()),
                SettingEntity(KEY_SLEEP_WAKE_MINUTES, schedule.wakeTime.toMinutesOfDay().toString())
            )
        )
    }

    private fun List<SettingEntity>.toSleepSchedule(): SleepSchedule {
        val bedtime = minutesOf(KEY_SLEEP_BEDTIME_MINUTES)
        val wakeTime = minutesOf(KEY_SLEEP_WAKE_MINUTES)
        if (bedtime == null || wakeTime == null) return SleepSchedule.DEFAULT

        return SleepSchedule(bedtime, wakeTime).takeIf { it.isValid } ?: SleepSchedule.DEFAULT
    }

    private fun List<SettingEntity>.minutesOf(key: String): LocalTime? =
        firstOrNull { it.settingKey == key }
            ?.settingValue
            ?.toIntOrNull()
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime.of(it / 60, it % 60) }

    private fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

    // ── Đồng bộ đám mây (mặc định TẮT — mục 3 #31) ─────────────

    override fun observeCloudSync(): Flow<CloudSyncSettings> =
        settingsDao.observeAll().map { it.toCloudSyncSettings() }

    override suspend fun getCloudSync(): CloudSyncSettings =
        settingsDao.getAll().toCloudSyncSettings()

    override suspend fun setCloudSyncEnabled(enabled: Boolean, nowMillis: Long) {
        val rows = mutableListOf(SettingEntity(KEY_CLOUD_SYNC_ENABLED, enabled.toString()))
        // Mốc chặn chỉ ghi khi BẬT: lần bật mới nhất quyết định "chỉ log tạo sau đây mới được tải lên".
        if (enabled) rows += SettingEntity(KEY_CLOUD_SYNC_CONSENT_AT, nowMillis.toString())
        settingsDao.putAll(rows)
    }

    /**
     * Đọc trạng thái đồng bộ đám mây từ bảng thiết lập.
     *
     * Thiếu khoá / giá trị hỏng đều rơi về **TẮT** — chọn hướng an toàn (không tải gì lên đám mây)
     * thay vì tải dữ liệu khi chưa được người dùng đồng ý.
     */
    private fun List<SettingEntity>.toCloudSyncSettings(): CloudSyncSettings {
        val enabled = valueOf(KEY_CLOUD_SYNC_ENABLED)?.toBooleanStrictOrNull() ?: false
        val consentAtMillis = valueOf(KEY_CLOUD_SYNC_CONSENT_AT)?.toLongOrNull()
        return CloudSyncSettings(enabled = enabled, consentAtMillis = consentAtMillis)
    }

    private fun List<SettingEntity>.valueOf(key: String): String? =
        firstOrNull { it.settingKey == key }?.settingValue

    companion object {
        /** Khoá trong bảng `app_settings`. */
        internal const val KEY_SLEEP_BEDTIME_MINUTES = "sleep_bedtime_minutes"
        internal const val KEY_SLEEP_WAKE_MINUTES = "sleep_wake_minutes"

        /** Khoá thiết lập đồng bộ đám mây (mục 3 #31). */
        internal const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        internal const val KEY_CLOUD_SYNC_CONSENT_AT = "cloud_sync_consent_at"

        private const val MINUTES_PER_DAY = 24 * 60
    }
}
