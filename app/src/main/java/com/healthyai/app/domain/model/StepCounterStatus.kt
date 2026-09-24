package com.healthyai.app.domain.model

/**
 * Trạng thái đếm bước bằng **cảm biến của chính thiết bị** (`TYPE_STEP_COUNTER`) — nguồn dữ liệu
 * "tự lực" của app, không phụ thuộc ứng dụng sức khỏe bên ngoài hay Health Connect.
 *
 * @property sensorAvailable Máy có cảm biến đếm bước phần cứng hay không (emulator thì không có).
 * @property permissionGranted Đã có quyền `ACTIVITY_RECOGNITION` chưa. Từ Android 10 (API 29) trở lên
 *   mới bắt buộc quyền này; các phiên bản cũ hơn luôn được coi là đã có quyền.
 */
data class StepCounterStatus(
    val sensorAvailable: Boolean = false,
    val permissionGranted: Boolean = false
) {
    /** Đủ điều kiện đếm bước: máy có cảm biến **và** app đã được cấp quyền. */
    val isReady: Boolean get() = sensorAvailable && permissionGranted
}
