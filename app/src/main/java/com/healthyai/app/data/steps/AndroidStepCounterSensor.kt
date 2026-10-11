package com.healthyai.app.data.steps

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Đọc cảm biến đếm bước thật của thiết bị bằng `SensorManager`.
 *
 * Chọn `TYPE_STEP_COUNTER` (chứ không phải `TYPE_STEP_DETECTOR`) vì:
 * - đếm **ở phần cứng**, gần như không tốn pin và vẫn đếm khi màn hình tắt;
 * - app không cần foreground service để giữ việc đếm khi còn chạy nền;
 * - trả về **giá trị luỹ kế** nên tầng trên tự quyết định lấy phần nào — hiện chỉ tính phần quan sát
 *   được **trong lúc app đang chạy** (xem `StepCounterMath`): bước đi lúc app đã bị đóng hẳn **không**
 *   được cộng vào, dù cảm biến vẫn đếm chúng.
 *
 * ⚠️ Từ Android 10 (API 29) trở lên, đăng ký listener của cảm biến này cần quyền
 * `ACTIVITY_RECOGNITION` — tầng gọi (`StepCounterRepository`) chỉ đăng ký sau khi đã kiểm tra quyền.
 *
 * ⚠️ Cũng từ Android 10, cảm biến bước chân **bị ẩn** với app chưa có `ACTIVITY_RECOGNITION`:
 * `getDefaultSensor(TYPE_STEP_COUNTER)` trả `null`. Nếu chỉ dựa vào đó thì `isAvailable` luôn `false`
 * lúc chưa cấp quyền, và thẻ xin quyền ở màn Trang chủ **không bao giờ hiện** — người dùng rơi vào thế
 * bế tắc "muốn cấp quyền phải thấy thẻ, muốn thấy thẻ phải có quyền" (lỗi KB-77). Vì vậy `isAvailable`
 * kiểm thêm **khả năng phần cứng** qua `PackageManager.FEATURE_SENSOR_STEP_COUNTER`, vốn không phụ
 * thuộc quyền.
 */
@Singleton
class AndroidStepCounterSensor @Inject constructor(
    @ApplicationContext private val context: Context
) : StepCounterSensor {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /**
     * Máy có **phần cứng** đếm bước hay không — cố ý không phụ thuộc quyền `ACTIVITY_RECOGNITION`.
     *
     * `getDefaultSensor(TYPE_STEP_COUNTER)` trả `null` khi app chưa được cấp quyền (từ Android 10),
     * nên kiểm thêm cờ khả năng phần cứng `FEATURE_SENSOR_STEP_COUNTER`. Nhờ vậy màn Trang chủ vẫn biết
     * đây là máy "có cảm biến nhưng chưa cấp quyền" mà hiện thẻ xin quyền (lỗi KB-77), còn emulator
     * (không có cả cờ lẫn cảm biến) vẫn không hiện gì.
     */
    override val isAvailable: Boolean
        get() = stepCounterSensor != null ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)

    override fun observeCumulativeSteps(): Flow<Long> = callbackFlow {
        val manager = sensorManager
        val sensor = stepCounterSensor
        if (manager == null || sensor == null) {
            close() // Máy không có cảm biến → không phát gì
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER && event.values.isNotEmpty()) {
                    trySend(event.values[0].toLong())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { manager.unregisterListener(listener) }
    }
}
