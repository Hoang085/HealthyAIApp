package com.healthyai.app.domain.model

/** Ảnh chụp trạng thái quyền Health Connect tại một thời điểm, dùng cho UI xin quyền. */
data class HealthConnectPermissionStatus(
    val status: HealthConnectStatus = HealthConnectStatus.UNKNOWN,
    val requiredPermissions: Set<String> = emptySet(),
    val hasAllPermissions: Boolean = false
) {
    /** Cần hiện thẻ "Cấp quyền Health Connect" không: chỉ đúng khi đã sẵn sàng nhưng chưa cấp đủ quyền. */
    val needsPermission: Boolean
        get() = status == HealthConnectStatus.AVAILABLE && !hasAllPermissions
}
