package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/** Đọc bản chụp dữ liệu sức khỏe hôm nay đã lưu trong app; lỗi đọc coi như rỗng để màn Trang chủ không crash. */
class GetStoredHealthDataUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): HealthData =
        runCatchingCancellable { repository.getTodayStoredHealthData() }
            .getOrDefault(HealthData())
}
