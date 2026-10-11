package com.healthyai.app.data.repository

import com.healthyai.app.data.local.sleep.SleepTrackerStore
import com.healthyai.app.data.sleep.SleepSensor
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.usecase.sleep.SleepTrackerMath
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [SleepTrackerRepository]: đọc môi trường bằng [SleepSensor], lấy **khung giờ ngủ người dùng
 * đặt** từ [SettingsRepository], phân loại "đang ngủ" bằng `SleepTrackerMath.isAsleep` rồi cộng dồn vào
 * [SleepTrackerStore].
 *
 * Không ném ngoại lệ ra ngoài: máy không có cảm biến gia tốc (hoặc cảm biến không trả về gì) thì chỉ
 * trả số đã lưu — nhờ vậy `SleepTrackingWorker` và `HealthSyncWorker` không bao giờ thất bại vì tính
 * năng ước lượng này (cùng nguyên tắc với `StepCounterRepositoryImpl.readTodaySteps`).
 */
@Singleton
class SleepTrackerRepositoryImpl @Inject constructor(
    private val sensor: SleepSensor,
    private val store: SleepTrackerStore,
    private val settingsRepository: SettingsRepository
) : SleepTrackerRepository {

    override suspend fun readLastNightSleepHours(): Double =
        SleepTrackerMath.toSleepHours(
            store.readDisplaySleepMinutes(schedule = settingsRepository.getSleepSchedule())
        )

    override suspend fun sampleAndStore(): Long {
        val schedule = settingsRepository.getSleepSchedule()
        if (!sensor.isAvailable) return store.readState()?.sleepMinutes ?: 0L

        val environment = sensor.readEnvironment() ?: return store.readState()?.sleepMinutes ?: 0L
        val asleepNow = SleepTrackerMath.isAsleep(
            environment = environment,
            nowMs = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            schedule = schedule
        )

        return store.onSample(asleepNow = asleepNow, schedule = schedule)
    }
}
