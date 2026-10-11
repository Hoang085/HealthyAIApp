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

/**
 * Đếm bước từ cảm biến của máy: kiểm tra quyền `ACTIVITY_RECOGNITION` rồi chuyển từng mẫu luỹ kế của
 * cảm biến thành "số bước hôm nay" qua [StepCounterStore] (bên trong dùng `StepCounterMath`).
 *
 * Chỉ **phần đếm được trong lúc app đang chạy** mới được cộng vào: app đang mở hoặc còn nằm trong tác
 * vụ (chạy nền) thì đếm tiếp bình thường, còn đóng hẳn app rồi mở lại thì khoảng thời gian ở giữa
 * **không** được tính (xem `StepCounterMath` / `StepCounterStore`).
 */
@Singleton
class StepCounterRepositoryImpl @Inject constructor(
    private val sensor: StepCounterSensor,
    private val store: StepCounterStore,
    @ApplicationContext private val context: Context
) : StepCounterRepository {

    override fun getStatus(): StepCounterStatus = StepCounterStatus(
        sensorAvailable = sensor.isAvailable,
        permissionGranted = hasActivityRecognitionPermission()
    )

    /**
     * `ACTIVITY_RECOGNITION` chỉ được yêu cầu từ Android 10 (API 29); các bản cũ hơn dùng cảm biến
     * đếm bước không cần quyền nên luôn coi là đã có.
     */
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
            .collect { cumulative -> send(store.onSample(cumulative)) }
    }.distinctUntilChanged()

    /**
     * Đọc một lần cho worker ghi lịch sử: chờ mẫu cảm biến đầu tiên trong [timeoutMs], hết thời gian
     * chờ thì dùng giá trị đã lưu của hôm nay (0 nếu đã sang ngày mới hoặc chưa từng đọc).
     */
    override suspend fun readTodaySteps(timeoutMs: Long): Long {
        if (!getStatus().isReady) return store.readStoredTodaySteps(LocalDate.now().toString())

        return withTimeoutOrNull(timeoutMs) {
            sensor.observeCumulativeSteps()
                .map { cumulative -> store.onSample(cumulative) }
                .first()
        } ?: store.readStoredTodaySteps(LocalDate.now().toString())
    }
}
