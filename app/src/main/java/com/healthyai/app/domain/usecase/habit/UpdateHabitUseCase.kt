package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.repository.HabitRepository
import javax.inject.Inject

class UpdateHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    /** Sửa thói quen qua updateHabit (UPDATE theo khoá chính); không dùng insertHabit vì REPLACE+CASCADE sẽ xoá sạch log. */
    suspend operator fun invoke(habit: Habit) = repository.updateHabit(habit)
}
