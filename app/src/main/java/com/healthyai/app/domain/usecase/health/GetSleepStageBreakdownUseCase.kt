package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.repository.HealthDataRepository
import javax.inject.Inject

/** Lấy tổng thời lượng từng pha giấc ngủ (Deep/Light/REM) trực tiếp từ Health Connect; trả rỗng khi thiếu quyền. */
class GetSleepStageBreakdownUseCase @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    suspend operator fun invoke(daysBack: Int = 7): SleepStageBreakdown =
        healthDataRepository.getSleepStageBreakdown(daysBack)
}
