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

/** Đọc cảm biến đếm bước (`TYPE_STEP_COUNTER`, giá trị luỹ kế); `isAvailable` kiểm thêm cờ phần cứng vì cảm biến bị ẩn khi chưa có quyền. */
@Singleton
class AndroidStepCounterSensor @Inject constructor(
    @ApplicationContext private val context: Context
) : StepCounterSensor {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /** Máy có phần cứng đếm bước hay không — cố ý không phụ thuộc quyền, để màn Trang chủ vẫn hiện thẻ xin quyền (lỗi). */
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
