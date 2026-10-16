package com.healthyai.app.data.sleep

import com.healthyai.app.domain.model.SleepEnvironment

/** Nguồn tín hiệu tự lực để suy ra giấc ngủ (ánh sáng, gia tốc, màn hình); tách interface để unit test JVM thay bằng giả lập. */
interface SleepSensor {

    /** Máy có cảm biến gia tốc hay không — thiếu nó thì không thể biết điện thoại có bất động không. */
    val isAvailable: Boolean

    /** Đọc một "chụp" môi trường trong [sampleWindowMs]; trả `null` nếu máy không có cảm biến gia tốc. */
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
