package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import kotlinx.coroutines.flow.Flow

/** Repository cho Habit và HabitLog; tầng Domain chỉ biết interface này, không quan tâm Room/Firebase/API. */
interface HabitRepository {
    /** Stream danh sách thói quen đang hoạt động (reactive, tự cập nhật khi DB thay đổi) */
    fun getAllActiveHabits(): Flow<List<Habit>>

    /** Stream lịch sử ghi nhận trong [daysBack] ngày qua */
    fun getHabitLogsForPeriod(daysBack: Int): Flow<List<HabitLog>>

    /** Tạo mới thói quen (`habit.id == 0`) và trả về id hàng vừa tạo. */
    suspend fun insertHabit(habit: Habit): Long

    /** Cập nhật theo `habit.id` mà không xoá hàng cũ; nếu dùng REPLACE, khoá ngoại CASCADE sẽ xoá sạch log và tiến độ về 0. */
    suspend fun updateHabit(habit: Habit)

    suspend fun logHabit(log: HabitLog)

    /** Xoá mềm thói quen (chỉ đặt `isActive = 0`); hàng và `habit_logs` vẫn còn cho Insights. */
    suspend fun deleteHabit(habitId: Long)
    
    /** Lấy danh sách thói quen chưa đồng bộ */
    suspend fun getUnsyncedHabitLogs(): List<HabitLog>
    
    /** Đánh dấu một log đã đồng bộ thành công */
    suspend fun markHabitLogSynced(id: Long)
}
