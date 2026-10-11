package com.healthyai.app.domain.usecase.notification

import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.repository.NotificationPermissionRepository
import javax.inject.Inject

/**
 * Đọc trạng thái quyền thông báo để màn Home quyết định có hiện thẻ
 * "Bật nhắc nhở hàng ngày" hay không (`ReminderWorker` chỉ gửi được thông báo khi quyền này bật).
 */
class GetNotificationPermissionStatusUseCase @Inject constructor(
    private val repository: NotificationPermissionRepository
) {
    operator fun invoke(): NotificationPermissionStatus = repository.getStatus()
}
