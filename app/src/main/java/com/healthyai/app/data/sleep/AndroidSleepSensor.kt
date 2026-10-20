package com.healthyai.app.data.sleep

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import com.healthyai.app.domain.model.SleepEnvironment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Đọc cảm biến thật (ánh sáng, gia tốc, màn hình) suy ra giấc ngủ, không cần quyền; chỉ lấy mẫu trong [sampleWindowMs] rồi gỡ listener để không tốn pin. */
@Singleton
class AndroidSleepSensor @Inject constructor(
    @ApplicationContext context: Context
) : SleepSensor {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val accelerometer: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val lightSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    override val isAvailable: Boolean
        get() = accelerometer != null

    override suspend fun readEnvironment(sampleWindowMs: Long): SleepEnvironment? {
        val manager = sensorManager ?: return null
        val accel = accelerometer ?: return null

        // Không có cảm biến ánh sáng (hoặc không kịp phát) → coi như "tối"; vẫn phải hội đủ màn hình tắt + bất động mới kết luận là ngủ.
        val lightLux = readLastValue(manager, lightSensor, SleepSensor.LIGHT_SETTLE_MS) ?: NO_LIGHT_LUX
        val maxMotionG = readPeakMotion(manager, accel, sampleWindowMs)

        return SleepEnvironment(
            screenInteractive = powerManager?.isInteractive ?: true,
            lightLux = lightLux,
            maxMotionG = maxMotionG
        )
    }

    /** Giá trị cuối cùng cảm biến phát trong [windowMs]; `null` nếu cảm biến không có/không phát gì. */
    private suspend fun readLastValue(
        manager: SensorManager,
        sensor: Sensor?,
        windowMs: Long
    ): Float? {
        val target = sensor ?: return null

        var last: Float? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isNotEmpty()) last = event.values[0]
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, target, SensorManager.SENSOR_DELAY_NORMAL)
        try {
            delay(windowMs)
        } finally {
            manager.unregisterListener(listener)
        }
        return last
    }

    /** Độ lệch lớn nhất khỏi 1g trong [windowMs]; dùng giá trị lớn nhất để một cử động ngắn cũng đủ kết luận "đang thức". */
    private suspend fun readPeakMotion(manager: SensorManager, sensor: Sensor, windowMs: Long): Float {
        var peak = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                peak = max(peak, abs(magnitude - GRAVITY_M_S2))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        try {
            delay(windowMs.coerceAtLeast(MIN_SAMPLE_WINDOW_MS))
        } finally {
            manager.unregisterListener(listener)
        }
        return peak
    }

    private companion object {
        /** Gia tốc trọng trường tiêu chuẩn (m/s²) — điện thoại nằm yên cho độ lớn ≈ giá trị này. */
        const val GRAVITY_M_S2 = 9.80665f

        /** Sàn cửa sổ lấy mẫu: ngắn hơn thì gia tốc có thể không kịp phát mẫu nào. */
        const val MIN_SAMPLE_WINDOW_MS = 500L

        /** Không đọc được ánh sáng → coi như tối (xem `readEnvironment`). */
        const val NO_LIGHT_LUX = 0f
    }
}
