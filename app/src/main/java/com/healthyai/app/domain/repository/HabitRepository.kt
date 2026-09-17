package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface cho Habit và HabitLog.
 * Tầng Domain chỉ biết interface này — không quan tâm Room, Firebase hay API nào.
 * Implementation thực tế nằm ở tầng Data (HabitRepositoryImpl).
 */
interface HabitRepository {
    /** Stream danh sách thói quen đang hoạt động (reactive, tự cập nhật khi DB thay đổi) */
    fun getAllActiveHabits(): Flow<List<Habit>>

    /** Stream lịch sử ghi nhận trong [daysBack] ngày qua */
    fun getHabitLogsForPeriod(daysBack: Int): Flow<List<HabitLog>>

    suspend fun insertHabit(habit: Habit): Long
    suspend fun logHabit(log: HabitLog)
    suspend fun deleteHabit(habitId: Long)
    
    /** Lấy danh sách thói quen chưa đồng bộ */
    suspend fun getUnsyncedHabitLogs(): List<HabitLog>
    
    /** Đánh dấu một log đã đồng bộ thành công */
    suspend fun markHabitLogSynced(id: Long)
}
