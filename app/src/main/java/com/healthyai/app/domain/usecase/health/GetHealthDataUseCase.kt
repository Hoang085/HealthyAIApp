package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/** Lấy bản chụp dữ liệu sức khỏe của hôm nay từ Health Connect. */
class GetHealthDataUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): Result<HealthData> = runCatchingCancellable {
        if (!repository.isAvailable()) {
            return Result.failure(Exception("Health Connect không khả dụng trên thiết bị này"))
        }

        repository.getTodayHealthData()
    }
}
