package com.healthyai.app.data.steps

import android.content.Context
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
 * - app không cần foreground service để giữ việc đếm;
 * - giá trị luỹ kế bao gồm cả số bước đi lúc app không mở, nên không mất dữ liệu.
 *
 * ⚠️ Từ Android 10 (API 29) trở lên, đăng ký listener của cảm biến này cần quyền
 * `ACTIVITY_RECOGNITION` — tầng gọi (`StepCounterRepository`) chỉ đăng ký sau khi đã kiểm tra quyền.
 */
@Singleton
class AndroidStepCounterSensor @Inject constructor(
    @ApplicationContext private val context: Context
) : StepCounterSensor {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean
        get() = stepCounterSensor != null

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
