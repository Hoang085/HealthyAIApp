package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.NotificationPermissionStatus

/**
 * Repository đọc trạng thái quyền hiển thị thông báo của app.
 *
 * Chỉ đọc — việc xin quyền do UI thực hiện qua `ActivityResultContracts.RequestPermission()`.
 */
interface NotificationPermissionRepository {
    /** Trạng thái hiện tại của quyền thông báo (không mở hộp thoại hệ thống). */
    fun getStatus(): NotificationPermissionStatus
}
