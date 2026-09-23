package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.repository.HealthDataRepository
import javax.inject.Inject

/**
 * UseCase lấy tổng thời lượng từng pha giấc ngủ (Deep / Light / REM) từ Health Connect
 * để vẽ thẻ "Xu hướng chất lượng giấc ngủ" bằng dữ liệu thật.
 *
 * Không dùng Room: dữ liệu đọc trực tiếp từ Health Connect nên không cần migration DB.
 * Trả về [SleepStageBreakdown] rỗng khi thiếu quyền / Health Connect không khả dụng.
 */
class GetSleepStageBreakdownUseCase @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    suspend operator fun invoke(daysBack: Int = 7): SleepStageBreakdown =
        healthDataRepository.getSleepStageBreakdown(daysBack)
}
