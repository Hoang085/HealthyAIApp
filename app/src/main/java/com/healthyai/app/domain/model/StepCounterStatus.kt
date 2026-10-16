package com.healthyai.app.domain.model

/** Trạng thái đếm bước bằng cảm biến của chính thiết bị, không phụ thuộc ứng dụng sức khỏe bên ngoài hay Health Connect. */
data class StepCounterStatus(
    val sensorAvailable: Boolean = false,
    val permissionGranted: Boolean = false
) {
    /** Đủ điều kiện đếm bước: máy có cảm biến và app đã được cấp quyền. */
    val isReady: Boolean get() = sensorAvailable && permissionGranted
}
