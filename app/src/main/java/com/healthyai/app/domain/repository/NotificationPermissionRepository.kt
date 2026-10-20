package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.NotificationPermissionStatus

/** Repository chỉ đọc trạng thái quyền thông báo; việc xin quyền do UI thực hiện. */
interface NotificationPermissionRepository {
    /** Trạng thái hiện tại của quyền thông báo (không mở hộp thoại hệ thống). */
    fun getStatus(): NotificationPermissionStatus
}
