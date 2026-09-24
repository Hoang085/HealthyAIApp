package com.healthyai.app.data.steps

import kotlinx.coroutines.flow.Flow

/**
 * Cảm biến đếm bước phần cứng của thiết bị (`Sensor.TYPE_STEP_COUNTER`).
 *
 * Tách thành interface để tầng trên không phụ thuộc `SensorManager`, và để unit test JVM có thể
 * thay bằng giả lập.
 */
interface StepCounterSensor {

    /** Máy có cảm biến đếm bước không (emulator và một số máy rẻ tiền thì không có). */
    val isAvailable: Boolean

    /**
     * Phát **giá trị luỹ kế từ lúc khởi động máy** mỗi khi cảm biến cập nhật.
     *
     * Chỉ nên thu thập khi app đang hiển thị: rời màn hình là flow bị huỷ và listener được gỡ.
     * Số bước đi trong lúc app đóng **không mất** vì cảm biến đếm ở phần cứng — lần đọc sau
     * giá trị luỹ kế đã bao gồm phần đó.
     */
    fun observeCumulativeSteps(): Flow<Long>
}
