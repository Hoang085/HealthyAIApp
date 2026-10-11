package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/**
 * UseCase để lấy bản chụp dữ liệu sức khỏe **của hôm nay** từ Health Connect.
 *
 * Trước đây use case này truyền `Instant.now()` cho cả `start` và `end` trong khi repository bỏ qua
 * hai tham số đó (luôn trả số của hôm nay) — một hợp đồng gây hiểu nhầm. Nay gọi thẳng
 * [HealthDataRepository.getTodayHealthData] để tên hàm nói đúng việc nó làm (phụ lục C #42).
 *
 * Dữ liệu của **các ngày đã qua** vẫn đọc được qua `health_data_history`
 * ([GetRecentHealthSummaryUseCase]) — đó cũng là nguồn AI ở tab Phân tích dùng.
 */
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
