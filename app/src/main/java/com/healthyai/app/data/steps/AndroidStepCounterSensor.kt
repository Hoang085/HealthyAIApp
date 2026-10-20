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
import kotlin.math.sqrt

/**
 * Đọc cảm biến đếm bước với độ trễ thấp: kết hợp `TYPE_STEP_COUNTER` (phần cứng luỹ kế),
 * `TYPE_STEP_DETECTOR` (từng bước) và `TYPE_ACCELEROMETER` (dò nhịp bước/vung lắc tức thời),
 * đồng thời đối soát để không đếm đôi khi `TYPE_STEP_COUNTER` xả lô trễ.
 */
@Singleton
class AndroidStepCounterSensor @Inject constructor(
    @ApplicationContext private val context: Context
) : StepCounterSensor {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepDetectorSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val accelerometerSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Bộ hợp nhất tín hiệu bước chân dùng chung trong suốt vòng đời tiến trình app. */
    private val fusionTracker = StepFusionTracker()

    /** Máy có phần cứng đếm bước hay không — cố ý không phụ thuộc quyền, để màn Trang chủ vẫn hiện thẻ xin quyền (lỗi). */
    override val isAvailable: Boolean
        get() = stepCounterSensor != null ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)

    override fun observeCumulativeSteps(): Flow<Long> = callbackFlow {
        val manager = sensorManager
        val counter = stepCounterSensor
        val detector = stepDetectorSensor
        val accel = accelerometerSensor

        if (manager == null || (!isAvailable && counter == null && detector == null)) {
            close() // Máy không có cảm biến → không phát gì
            return@callbackFlow
        }

        // Phát ngay mốc luỹ kế hiện tại lúc bắt đầu nghe để StepCounterMath chốt baseline tức thì,
        // tránh việc bước chân đầu tiên bị nuốt làm baseline.
        trySend(fusionTracker.currentCumulative())

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isEmpty()) return
                val nowMs = System.currentTimeMillis()
                val updated = when (event.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER ->
                        fusionTracker.onStepCounterEvent(event.values[0].toLong(), nowMs)

                    Sensor.TYPE_STEP_DETECTOR ->
                        fusionTracker.onStepDetectorEvent(nowMs)

                    Sensor.TYPE_ACCELEROMETER -> {
                        if (event.values.size >= 3) {
                            fusionTracker.onAccelerometerSample(
                                x = event.values[0],
                                y = event.values[1],
                                z = event.values[2],
                                nowMs = nowMs
                            )
                        } else {
                            null
                        }
                    }

                    else -> null
                }
                if (updated != null) {
                    trySend(updated)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // maxReportLatencyUs = 0 yêu cầu hệ thống giao mẫu ngay, không giữ trong hàng đợi phần cứng.
        counter?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }
        detector?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }
        accel?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME, 0)
        }

        awaitClose { manager.unregisterListener(listener) }
    }
}

/**
 * Hợp nhất 3 nguồn cảm biến thành một bộ đếm luỹ kế đơn điệu cho tiến trình hiện tại:
 * - Gia tốc kế (`TYPE_ACCELEROMETER`) và `TYPE_STEP_DETECTOR` tăng số bước ngay lập tức (~150ms)
 *   khi người dùng bước đi hoặc vung lắc điện thoại.
 * - `TYPE_STEP_COUNTER` (thường trễ 5–10 giây trên Samsung/One UI) được đối soát bù trừ phần bước
 *   đã đếm live để không bao giờ nhân đôi số bước mà vẫn giữ đủ bước đi khi app ở nền.
 */
internal class StepFusionTracker {

    private var virtualCumulative: Long = 0L
    private var hwLastRaw: Long? = null
    private var unreconciledLiveSteps: Long = 0L
    private var lastLiveStepMs: Long = 0L
    private var lastAccelStepMs: Long = 0L

    private var gravity: Float = Float.NaN
    private var smoothedDynamic: Float = 0f
    private var hasPeak: Boolean = false
    private var peakTimeMs: Long = 0L

    @Synchronized
    fun currentCumulative(): Long = virtualCumulative

    @Synchronized
    fun onAccelerometerSample(x: Float, y: Float, z: Float, nowMs: Long): Long? {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        if (gravity.isNaN()) {
            gravity = magnitude
            smoothedDynamic = 0f
            return null
        }

        // Bộ lọc thông thấp tách gia tốc trọng trường (~9.81 m/s²) khi cầm nghiêng/xoay máy.
        gravity = GRAVITY_ALPHA * gravity + (1f - GRAVITY_ALPHA) * magnitude
        val rawDynamic = magnitude - gravity
        smoothedDynamic =
            DYNAMIC_SMOOTH_ALPHA * smoothedDynamic + (1f - DYNAMIC_SMOOTH_ALPHA) * rawDynamic

        if (smoothedDynamic >= PEAK_THRESHOLD_MPS2) {
            hasPeak = true
            peakTimeMs = nowMs
            return null
        }

        if (hasPeak) {
            val elapsedSincePeak = nowMs - peakTimeMs
            if (elapsedSincePeak > MAX_WAVE_DURATION_MS) {
                hasPeak = false
                return null
            }
            if (smoothedDynamic <= TROUGH_THRESHOLD_MPS2) {
                hasPeak = false
                if (lastLiveStepMs == 0L || nowMs - lastLiveStepMs >= MIN_STEP_INTERVAL_MS) {
                    lastLiveStepMs = nowMs
                    lastAccelStepMs = nowMs
                    virtualCumulative += 1L
                    unreconciledLiveSteps += 1L
                    return virtualCumulative
                }
            }
        }
        return null
    }

    @Synchronized
    fun onStepDetectorEvent(nowMs: Long): Long? {
        if (lastAccelStepMs != 0L && nowMs - lastAccelStepMs < ACCEL_DETECTOR_DEDUP_MS) {
            return null
        }
        if (lastLiveStepMs != 0L && nowMs - lastLiveStepMs < MIN_STEP_INTERVAL_MS) {
            return null
        }
        lastLiveStepMs = nowMs
        virtualCumulative += 1L
        unreconciledLiveSteps += 1L
        return virtualCumulative
    }

    @Synchronized
    fun onStepCounterEvent(hwRaw: Long, nowMs: Long): Long? {
        val previousHw = hwLastRaw
        hwLastRaw = hwRaw

        if (previousHw == null || hwRaw < previousHw) {
            unreconciledLiveSteps = 0L
            return null
        }

        val hwDelta = hwRaw - previousHw
        if (hwDelta <= 0L) return null

        if (lastLiveStepMs != 0L && nowMs - lastLiveStepMs > HW_RECONCILE_WINDOW_MS) {
            unreconciledLiveSteps = 0L
        }

        val coveredByLive = minOf(hwDelta, unreconciledLiveSteps)
        unreconciledLiveSteps -= coveredByLive
        val additionalSteps = hwDelta - coveredByLive

        if (additionalSteps > 0L) {
            virtualCumulative += additionalSteps
            return virtualCumulative
        }
        return null
    }

    companion object {
        private const val GRAVITY_ALPHA = 0.85f
        private const val DYNAMIC_SMOOTH_ALPHA = 0.45f
        private const val PEAK_THRESHOLD_MPS2 = 1.05f
        private const val TROUGH_THRESHOLD_MPS2 = -0.25f
        private const val MAX_WAVE_DURATION_MS = 650L
        private const val MIN_STEP_INTERVAL_MS = 230L
        private const val ACCEL_DETECTOR_DEDUP_MS = 500L
        private const val HW_RECONCILE_WINDOW_MS = 12_000L
    }
}

