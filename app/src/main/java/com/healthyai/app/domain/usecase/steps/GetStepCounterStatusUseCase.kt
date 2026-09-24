package com.healthyai.app.domain.usecase.steps

import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.StepCounterRepository
import javax.inject.Inject

/**
 * Đọc trạng thái đếm bước của máy để màn Trang chủ quyết định có hiện thẻ xin quyền hay không.
 *
 * Không bao giờ ném ngoại lệ — máy không có cảm biến vẫn trả về trạng thái "chưa sẵn sàng" thay vì crash.
 */
class GetStepCounterStatusUseCase @Inject constructor(
    private val repository: StepCounterRepository
) {
    operator fun invoke(): StepCounterStatus = repository.getStatus()
}
