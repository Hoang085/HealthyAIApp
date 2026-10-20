package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/** Đọc nhịp tim ghi gần nhất trong hôm nay; sang ngày mới hoặc đọc lỗi thì trả 0.0. */
class GetLatestRecordedHeartRateUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    /** Nhịp tim (bpm); 0.0 khi chưa có số hoặc không đọc được. */
    suspend operator fun invoke(): Double =
        runCatchingCancellable { repository.getLatestRecordedHeartRate() ?: 0.0 }.getOrDefault(0.0)
}
