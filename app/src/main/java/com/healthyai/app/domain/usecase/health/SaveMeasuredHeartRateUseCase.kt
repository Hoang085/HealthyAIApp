package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.repository.HealthDataRepository
import javax.inject.Inject

/** Lưu nhịp tim do app tự đo (camera + đèn flash) vào `health_data_history` của hôm nay. */
class SaveMeasuredHeartRateUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    /** @param bpm nhịp tim vừa đo (nhịp/phút), phải lớn hơn 0. */
    suspend operator fun invoke(bpm: Int): Result<Unit> {
        if (bpm <= 0) {
            return Result.failure(IllegalArgumentException("Nhịp tim phải lớn hơn 0 (nhận được $bpm)"))
        }
        return repository.saveMeasuredHeartRate(bpm.toDouble())
    }
}
