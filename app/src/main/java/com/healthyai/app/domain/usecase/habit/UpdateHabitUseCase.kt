package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.repository.HabitRepository
import javax.inject.Inject

class UpdateHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    /**
     * Sửa thói quen đã có.
     *
     * Đi qua `updateHabit` (UPDATE theo khoá chính) chứ **không** qua `insertHabit`: đường ghi cũ dùng
     * `OnConflictStrategy.REPLACE` (= DELETE + INSERT) nên khoá ngoại `habit_logs → habits` với
     * `onDelete = CASCADE` xoá sạch log của thói quen, làm tiến độ trong ngày về 0 sau khi đổi mục
     * tiêu (lỗi KB-13).
     */
    suspend operator fun invoke(habit: Habit) = repository.updateHabit(habit)
}
