package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.repository.HabitRepository
import javax.inject.Inject

class DeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long) = repository.deleteHabit(habitId)
}
