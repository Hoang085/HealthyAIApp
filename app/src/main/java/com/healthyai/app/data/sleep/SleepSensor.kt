package com.healthyai.app.data.sleep

import com.healthyai.app.domain.model.SleepEnvironment

/**
 * Nguồn tín hiệu **tự lực** để app suy ra giấc ngủ: ánh sáng môi trường, gia tốc, trạng thái màn hình.
 *
 * Tách thành interface để tầng trên không phụ thuộc `SensorManager`/`PowerManager`, và để unit test
 * JVM có thể thay bằng giả lập (giống `StepCounterSensor` của phần đếm bước).
 */
interface SleepSensor {

    /** Máy có cảm biến gia tốc hay không — thiếu nó thì không thể biết điện thoại có bất động không. */
    val isAvailable: Boolean

    /**
     * Đọc một "chụp" môi trường: nghe cảm biến trong [sampleWindowMs] rồi trả về giá trị tổng hợp.
     *
     * @return `null` khi máy không có cảm biến gia tốc (không thể kết luận gì).
     */
    suspend fun readEnvironment(
        sampleWindowMs: Long = DEFAULT_SAMPLE_WINDOW_MS
    ): SleepEnvironment?

    companion object {
        /** Cửa sổ nghe cảm biến gia tốc — đủ dài để bắt được một cử động nhỏ, đủ ngắn để không tốn pin. */
        const val DEFAULT_SAMPLE_WINDOW_MS = 3_000L

        /** Thời gian chờ cảm biến ánh sáng phát giá trị đầu tiên. */
        const val LIGHT_SETTLE_MS = 300L
    }
}
