package com.healthyai.app.domain.model

/**
 * Ảnh chụp trạng thái quyền Health Connect tại một thời điểm, dùng cho UI xin quyền.
 *
 * @property status Health Connect có sẵn sàng trên thiết bị hay không.
 * @property requiredPermissions Các quyền cần xin (truyền thẳng vào
 *   `PermissionController.createRequestPermissionResultContract()`).
 * @property hasAllPermissions Người dùng đã cấp đủ [requiredPermissions] chưa.
 */
data class HealthConnectPermissionStatus(
    val status: HealthConnectStatus = HealthConnectStatus.UNKNOWN,
    val requiredPermissions: Set<String> = emptySet(),
    val hasAllPermissions: Boolean = false
) {
    /**
     * Có cần hiện thẻ **"Cấp quyền Health Connect"** không.
     * Chỉ đúng khi Health Connect đã sẵn sàng nhưng người dùng chưa cấp đủ quyền —
     * hai trường hợp chưa cài / cần cập nhật được UI xử lý riêng theo [status].
     */
    val needsPermission: Boolean
        get() = status == HealthConnectStatus.AVAILABLE && !hasAllPermissions
}
