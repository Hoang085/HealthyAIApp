package com.healthyai.app.domain.model

/**
 * Trạng thái của Health Connect trên thiết bị.
 *
 * Dùng để quyết định màn Home hiển thị thẻ nào: xin quyền, cài Health Connect, hay cập nhật.
 */
enum class HealthConnectStatus {
    /** Chưa kiểm tra xong (màn hình đang tải). */
    UNKNOWN,

    /** Health Connect đã sẵn sàng — chỉ cần người dùng cấp quyền đọc. */
    AVAILABLE,

    /** Máy chưa cài Health Connect (Android < 14 và chưa tải app, hoặc đã gỡ ứng dụng). */
    NOT_INSTALLED,

    /** Có Health Connect nhưng phiên bản cũ, cần cập nhật trước khi dùng. */
    UPDATE_REQUIRED
}
