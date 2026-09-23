package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.repository.HealthDataRepository
import javax.inject.Inject

/**
 * Đọc trạng thái quyền Health Connect để màn Home quyết định hiển thị thẻ nào:
 * xin quyền, cài Health Connect, hay cập nhật Health Connect.
 *
 * Không bao giờ ném ngoại lệ — lỗi của Health Connect được coi như "không khả dụng"
 * để UI vẫn hiển thị được thẻ hướng dẫn thay vì crash.
 */
class GetHealthConnectPermissionStatusUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): HealthConnectPermissionStatus {
        val status = runCatching { repository.getStatus() }
            .getOrDefault(HealthConnectStatus.NOT_INSTALLED)

        if (status != HealthConnectStatus.AVAILABLE) {
            return HealthConnectPermissionStatus(status = status)
        }

        return HealthConnectPermissionStatus(
            status = status,
            requiredPermissions = runCatching { repository.getRequiredPermissions() }
                .getOrDefault(emptySet()),
            hasAllPermissions = runCatching { repository.hasAllPermissions() }
                .getOrDefault(false)
        )
    }
}
