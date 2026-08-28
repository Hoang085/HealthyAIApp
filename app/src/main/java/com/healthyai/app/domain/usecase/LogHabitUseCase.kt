package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import javax.inject.Inject

/** Use Case: Ghi nhận một lần thực hiện thói quen */
class LogHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(log: HabitLog) = habitRepository.logHabit(log)
}
