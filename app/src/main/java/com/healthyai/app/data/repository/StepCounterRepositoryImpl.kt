package com.healthyai.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.healthyai.app.data.local.steps.StepCounterStore
import com.healthyai.app.data.steps.StepCounterSensor
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Đếm bước từ cảm biến của máy; chỉ phần đếm trong lúc app đang chạy mới được cộng vào. */
@Singleton
class StepCounterRepositoryImpl @Inject constructor(
    private val sensor: StepCounterSensor,
    private val store: StepCounterStore,
    private val healthWidgetRefresher: HealthWidgetRefresher,
    @ApplicationContext private val context: Context
) : StepCounterRepository {

    /**
     * Nhịp vẽ lại widget "Sức khỏe hôm nay" theo bước chân: widget chỉ tự vẽ lại 30 phút/lần nên
     * không có cổng này thì ô bước chân trên màn hình chính sẽ tụt lại sau số đang hiện trên app.
     */
    private val widgetRefreshThrottle = StepWidgetRefreshThrottle()

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
        send(store.readStoredTodaySteps(LocalDate.now().toString()))

        // 2) Chưa đủ điều kiện (thiếu quyền / không có cảm biến) → dừng, UI hiện thẻ xin quyền.
        if (!getStatus().isReady) return@channelFlow

        // 3) Cảm biến phát mẫu → quy đổi thành số bước hôm nay và phát tiếp.
        sensor.observeCumulativeSteps()
            // Lỗi cảm biến chỉ làm dừng cập nhật, không được làm crash màn Trang chủ.
            .catch { }
            .collect { cumulative ->
                val todaySteps = store.onSample(cumulative)
                send(todaySteps)
                refreshWidgetIfStepsChanged(todaySteps)
            }
    }.distinctUntilChanged()

    /** Vẽ lại widget khi số bước đổi; cổng chặn giữ nhịp tối đa một lần mỗi phút để không tốn pin. */
    private suspend fun refreshWidgetIfStepsChanged(todaySteps: Long) {
        if (!widgetRefreshThrottle.shouldRefresh(todaySteps, System.currentTimeMillis())) return

        // refresh() không bao giờ ném lỗi (impl bắt hết), nên luồng cảm biến không bị ảnh hưởng.
        healthWidgetRefresher.refresh()
    }

    /** Đọc một lần cho worker ghi lịch sử: chờ mẫu cảm biến trong [timeoutMs], hết hạn thì dùng giá trị đã lưu của hôm nay. */
    override suspend fun readTodaySteps(timeoutMs: Long): Long {
        if (!getStatus().isReady) return store.readStoredTodaySteps(LocalDate.now().toString())

        return withTimeoutOrNull(timeoutMs) {
            sensor.observeCumulativeSteps()
                .map { cumulative -> store.onSample(cumulative) }
                .first()
        } ?: store.readStoredTodaySteps(LocalDate.now().toString())
    }
}
