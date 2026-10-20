package com.healthyai.app.data.steps

import kotlinx.coroutines.flow.Flow

/** Cảm biến đếm bước phần cứng (`Sensor.TYPE_STEP_COUNTER`); tách interface để unit test JVM thay bằng giả lập. */
interface StepCounterSensor {

    /** Máy có cảm biến đếm bước không (emulator và một số máy rẻ tiền thì không có). */
    val isAvailable: Boolean

    /** Phát giá trị luỹ kế từ lúc khởi động máy mỗi khi cảm biến cập nhật; chỉ nghe khi app hiển thị, số bước lúc app đóng vẫn còn vì cảm biến đếm ở phần cứng. */
    fun observeCumulativeSteps(): Flow<Long>
}
