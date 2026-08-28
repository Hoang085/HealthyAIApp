package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: Lấy danh sách thói quen đang hoạt động hôm nay */
class GetTodayHabitsUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    operator fun invoke(): Flow<List<Habit>> = habitRepository.getAllActiveHabits()
}
