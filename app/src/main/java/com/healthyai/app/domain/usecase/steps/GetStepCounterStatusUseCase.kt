package com.healthyai.app.domain.usecase.steps

import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.domain.repository.StepCounterRepository
import javax.inject.Inject

/** Đọc trạng thái đếm bước; không bao giờ ném ngoại lệ — máy không có cảm biến vẫn trả "chưa sẵn sàng". */
class GetStepCounterStatusUseCase @Inject constructor(
    private val repository: StepCounterRepository
) {
    operator fun invoke(): StepCounterStatus = repository.getStatus()
}
