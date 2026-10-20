package com.healthyai.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.steps.StepCounterStore
import com.healthyai.app.data.steps.StepCounterSensor
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Đếm bước từ cảm biến của máy; chỉ phần đếm trong lúc app đang chạy mới được cộng vào. */
@Singleton
class StepCounterRepositoryImpl @Inject constructor(
    private val sensor: StepCounterSensor,
    private val store: StepCounterStore,
    private val healthDataDao: HealthDataDao,
    private val settingsRepository: SettingsRepository,
    private val healthWidgetRefresher: HealthWidgetRefresher,
    @ApplicationContext private val context: Context
) : StepCounterRepository {

    /**
     * Nhịp vẽ lại widget "Sức khỏe hôm nay" theo bước chân (2 giây/lần khi đang đi bộ để widget
     * luôn khớp tức thời khi người dùng ra màn hình chính xem).
     */
    private val widgetRefreshThrottle = StepWidgetRefreshThrottle(minIntervalMs = LIVE_WIDGET_REFRESH_INTERVAL_MS)

    override fun getStatus(): StepCounterStatus = StepCounterStatus(
        sensorAvailable = sensor.isAvailable,
        permissionGranted = hasActivityRecognitionPermission()
    )

    /** `ACTIVITY_RECOGNITION` chỉ cần từ Android 10 (API 29); bản cũ hơn coi như đã có quyền. */
    private fun hasActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

    override fun observeTodaySteps(): Flow<Long> = channelFlow {
        // 1) Hiện ngay giá trị đã biết của hôm nay (0 nếu đã sang ngày mới) để UI không trống.
        val initialSteps = store.readStoredTodaySteps(LocalDate.now().toString())
        send(initialSteps)
        if (initialSteps > 0L) {
            launch { syncRoomAndRefreshWidget(initialSteps) }
        }

        // 2) Chưa đủ điều kiện (thiếu quyền / không có cảm biến) → dừng, UI hiện thẻ xin quyền.
        if (!getStatus().isReady) return@channelFlow

        // Lần vẽ bù đang hẹn (nếu có); riêng cho mỗi lần thu, tự huỷ khi luồng thu bị huỷ.
        var trailingWidgetRefresh: Job? = null

        // 3) Cảm biến phát mẫu → quy đổi thành số bước hôm nay và phát tiếp.
        sensor.observeCumulativeSteps()
            // Lỗi cảm biến chỉ làm dừng cập nhật, không được làm crash màn Trang chủ.
            .catch { }
            .collect { cumulative ->
                val todaySteps = store.onSample(cumulative)
                send(todaySteps)

                // Đã có lần vẽ bù đang chờ → nó sẽ đọc số mới nhất lúc chạy, không cần làm gì thêm.
                if (trailingWidgetRefresh?.isActive == true) return@collect

                val waitMs = widgetRefreshThrottle.delayBeforeRefreshMs(
                    todaySteps,
                    System.currentTimeMillis()
                ) ?: return@collect

                if (waitMs == 0L) {
                    refreshWidgetIfStepsChanged(todaySteps)
                } else {
                    // Đang bị chặn: hẹn đúng một lần vẽ khi hết nhịp, để số cuối cùng luôn lên widget
                    // kể cả khi người dùng đã dừng đi (cảm biến không phát thêm mẫu nào nữa).
                    trailingWidgetRefresh = launch {
                        delay(waitMs)
                        refreshWidgetIfStepsChanged(
                            store.readStoredTodaySteps(LocalDate.now().toString())
                        )
                    }
                }
            }
    }.distinctUntilChanged()

    /** Vẽ lại widget khi số bước đổi và cập nhật luôn bản chụp hôm nay trong Room để không bị dính số cũ. */
    private suspend fun refreshWidgetIfStepsChanged(todaySteps: Long) {
        if (!widgetRefreshThrottle.shouldRefresh(todaySteps, System.currentTimeMillis())) return
        syncRoomAndRefreshWidget(todaySteps)
    }

    private suspend fun syncRoomAndRefreshWidget(todaySteps: Long) {
        if (todaySteps > 0L) {
            runCatchingCancellable {
                val useHealthConnect = runCatchingCancellable {
                    settingsRepository.getHealthConnectData().enabled
                }.getOrDefault(false)
                val startOfToday = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val existing = healthDataDao.getLatestSince(startOfToday)
                val shouldUpdate = existing != null && (
                    (!useHealthConnect && existing.steps != todaySteps) ||
                        todaySteps > existing.steps
                )
                if (shouldUpdate && existing != null) {
                    healthDataDao.updateHealthData(
                        existing.copy(
                            steps = todaySteps,
                            recordedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        healthWidgetRefresher.refresh()
    }

    /** Đọc một lần cho worker ghi lịch sử: chờ mẫu cảm biến trong [timeoutMs], hết hạn thì dùng giá trị đã lưu của hôm nay. */
    override suspend fun readTodaySteps(timeoutMs: Long): Long {
        if (!getStatus().isReady || timeoutMs <= 0L) {
            return store.readStoredTodaySteps(LocalDate.now().toString())
        }

        return withTimeoutOrNull(timeoutMs) {
            sensor.observeCumulativeSteps()
                .map { cumulative -> store.onSample(cumulative) }
                .first()
        } ?: store.readStoredTodaySteps(LocalDate.now().toString())
    }

    private companion object {
        const val LIVE_WIDGET_REFRESH_INTERVAL_MS = 2_000L
    }
}
