package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/** Đọc trạng thái quyền Health Connect; lỗi được coi như "không khả dụng" để UI không crash. */
class GetHealthConnectPermissionStatusUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): HealthConnectPermissionStatus {
        val status = runCatchingCancellable { repository.getStatus() }
            .getOrDefault(HealthConnectStatus.NOT_INSTALLED)

        if (status != HealthConnectStatus.AVAILABLE) {
            return HealthConnectPermissionStatus(status = status)
        }

        return HealthConnectPermissionStatus(
            status = status,
            requiredPermissions = runCatchingCancellable { repository.getRequiredPermissions() }
                .getOrDefault(emptySet()),
            hasAllPermissions = runCatchingCancellable { repository.hasAllPermissions() }
                .getOrDefault(false)
        )
    }
}
