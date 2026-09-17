package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import java.time.Instant
import javax.inject.Inject

/**
 * UseCase để lấy dữ liệu sức khỏe tổng hợp từ Health Connect.
 */
class GetHealthDataUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): Result<HealthData> = runCatching {
        if (!repository.isAvailable()) {
            return Result.failure(Exception("Health Connect không khả dụng trên thiết bị này"))
        }
        
        repository.getHealthData(
            start = Instant.now(), // Repository tự xử lý dải thời gian theo nghiệp vụ
            end = Instant.now()
        )
    }
}
