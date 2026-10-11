package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/**
 * Đọc **bản chụp dữ liệu sức khỏe của hôm nay đã lưu trong app** (`health_data_history`) để màn Trang
 * chủ hiển thị khi người dùng **tắt** switch "Dùng dữ liệu Health Connect".
 *
 * Bản chụp này do app tự ghi: `HealthSyncWorker` đồng bộ định kỳ, và lần đo nhịp tim bằng camera cũng
 * ghi vào cùng bảng. Nguồn [HealthData] rỗng nghĩa là hôm nay app chưa ghi được gì — lúc đó màn Trang
 * chủ vẫn còn nguồn tự đo của máy (cảm biến đếm bước / bộ ước lượng giấc ngủ) làm dự phòng.
 *
 * Không bao giờ ném ngoại lệ: đọc lỗi được coi như "chưa có số" ([HealthData] rỗng) để màn Trang chủ
 * vẫn hiển thị được (ngoại lệ duy nhất là `CancellationException`, phụ lục C #39).
 */
class GetStoredHealthDataUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    suspend operator fun invoke(): HealthData =
        runCatchingCancellable { repository.getTodayStoredHealthData() }
            .getOrDefault(HealthData())
}
