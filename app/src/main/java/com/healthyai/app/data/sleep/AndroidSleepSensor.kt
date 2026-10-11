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

/**
 * Đọc cảm biến thật của thiết bị để suy ra giấc ngủ — **không cần quyền nào** (khác quyền
 * `ACTIVITY_RECOGNITION` của phần đếm bước):
 *
 * - `Sensor.TYPE_LIGHT` → độ sáng phòng;
 * - `Sensor.TYPE_ACCELEROMETER` → điện thoại có bất động không;
 * - `PowerManager.isInteractive` → màn hình có đang được dùng không.
 *
 * ⚠️ Nghe cảm biến liên tục sẽ tốn pin, nên lớp này **chỉ lấy mẫu theo yêu cầu**: nghe trong
 * [sampleWindowMs] rồi **gỡ listener** trong `finally` (kể cả khi coroutine bị huỷ) — nhờ vậy worker
 * có thể chạy thưa (15 phút/lần) mà không giữ cảm biến suốt đêm.
 */
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

        // Máy không có cảm biến ánh sáng (hoặc không kịp phát giá trị) → coi như "tối"; khi đó hai
        // tín hiệu còn lại (màn hình tắt + bất động) vẫn phải đúng mới kết luận là ngủ.
        val lightLux = readLastValue(manager, lightSensor, SleepSensor.LIGHT_SETTLE_MS) ?: NO_LIGHT_LUX
        val maxMotionG = readPeakMotion(manager, accel, sampleWindowMs)

        return SleepEnvironment(
            screenInteractive = powerManager?.isInteractive ?: true,
            lightLux = lightLux,
            maxMotionG = maxMotionG
        )
    }

    /** Giá trị **cuối cùng** cảm biến phát trong [windowMs]; `null` nếu cảm biến không có/không phát gì. */
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

    /**
     * Độ lệch **lớn nhất** khỏi 1g (gia tốc trọng trường) trong [windowMs].
     *
     * Điện thoại nằm yên cho giá trị rất nhỏ; cầm lên, đi lại hay rung lắc cho giá trị lớn. Dùng
     * giá trị **lớn nhất** (không phải trung bình) để một cử động ngắn cũng đủ kết luận "đang thức".
     */
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
