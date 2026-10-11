package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.repository.HealthDataRepository
import javax.inject.Inject

/**
 * Lưu nhịp tim **do app tự đo bằng camera + đèn flash** vào lịch sử sức khỏe của hôm nay
 * (`health_data_history`).
 *
 * Nhờ ghi vào đúng bảng mà `HealthSyncWorker` dùng, số đo vừa xong **xuất hiện ngay** ở ô "Tim" trên
 * màn Trang chủ (khi Health Connect chưa có nhịp tim) và được **Rule Engine ở tab Phân tích** dùng cho
 * các gợi ý/cảnh báo về nhịp tim.
 *
 * Nhịp tim không hợp lệ (≤ 0) bị từ chối ngay tại đây để không bao giờ ghi một hàng vô nghĩa vào DB.
 */
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
