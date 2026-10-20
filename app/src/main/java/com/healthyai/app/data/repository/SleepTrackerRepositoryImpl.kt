package com.healthyai.app.data.repository

import com.healthyai.app.data.local.sleep.SleepTrackerStore
import com.healthyai.app.data.sleep.SleepSensor
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.usecase.sleep.SleepTrackerMath
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Triển khai [SleepTrackerRepository]: cộng dồn giờ ngủ từ cảm biến vào [SleepTrackerStore]; không ném ngoại lệ, thiếu cảm biến thì trả số đã lưu. */
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
