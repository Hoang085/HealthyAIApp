package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject

/**
 * Đọc **nhịp tim ghi gần nhất trong ngày hôm nay** để màn Trang chủ hiển thị khi Health Connect chưa
 * có số.
 *
 * Con số này có thể đến từ **lần đo bằng camera của app** (màn "Đo nhịp tim") hoặc từ một lần đồng bộ
 * Health Connect trong ngày — bảng `health_data_history` ghi chung một cột.
 *
 * Phạm vi **chỉ trong hôm nay**: sang ngày mới mà chưa đo lại thì trả `0.0` (ô "Tim" hiện "-- bpm")
 * thay vì hiện lại số của hôm qua — nhịp tim vì vậy cũng "đọc lại từ đầu ngày" như bước chân/giờ ngủ.
 *
 * Không bao giờ ném ngoại lệ: đọc lỗi được coi như "chưa có số" (`0.0`) để màn Trang chủ vẫn hiển thị
 * được.
 */
class GetLatestRecordedHeartRateUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    /** @return nhịp tim (bpm), `0.0` khi chưa có số nào hoặc không đọc được. */
    suspend operator fun invoke(): Double =
        runCatchingCancellable { repository.getLatestRecordedHeartRate() ?: 0.0 }.getOrDefault(0.0)
}
