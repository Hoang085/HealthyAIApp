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

    /** Tạo mới thói quen (`habit.id == 0`) và trả về id hàng vừa tạo. */
    suspend fun insertHabit(habit: Habit): Long

    /**
     * Cập nhật thói quen đã có theo `habit.id` — **không** xoá hàng cũ rồi ghi lại.
     *
     * Đây là điểm khác biệt sống còn so với `insertHabit`: đường ghi cũ dùng
     * `OnConflictStrategy.REPLACE` (SQLite hiểu là DELETE + INSERT), mà `habit_logs` có khoá ngoại
     * `onDelete = CASCADE` → mỗi lần sửa thói quen là xoá sạch log của nó và tiến độ trong ngày tụt
     * về 0 (lỗi KB-13).
     */
    suspend fun updateHabit(habit: Habit)

    suspend fun logHabit(log: HabitLog)

    /**
     * Xóa thói quen — **xóa mềm**: chỉ đặt `isActive = 0` để thói quen biến mất khỏi danh sách,
     * hàng dữ liệu và toàn bộ `habit_logs` của nó vẫn còn trong DB (Insights vẫn đọc được log).
     */
    suspend fun deleteHabit(habitId: Long)
    
    /** Lấy danh sách thói quen chưa đồng bộ */
    suspend fun getUnsyncedHabitLogs(): List<HabitLog>
    
    /** Đánh dấu một log đã đồng bộ thành công */
    suspend fun markHabitLogSynced(id: Long)
}
