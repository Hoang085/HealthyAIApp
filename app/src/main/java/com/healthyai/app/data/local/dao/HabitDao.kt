package com.healthyai.app.data.local.dao

import androidx.room.*
import com.healthyai.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    /** Flow tự cập nhật mỗi khi dữ liệu trong DB thay đổi */
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    /**
     * Tạo mới thói quen.
     *
     * `REPLACE` chỉ an toàn cho hàng **mới** (`id = 0` → `autoGenerate` nên không bao giờ trùng khoá).
     * Tuyệt đối **không** dùng hàm này cho thói quen đã có: `REPLACE` = DELETE + INSERT, và khoá ngoại
     * `habit_logs.habitId → habits.id` có `onDelete = CASCADE` nên sẽ xoá sạch log của thói quen đó
     * (lỗi KB-13). Đường cập nhật phải đi qua [updateHabit].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    /** Cập nhật thói quen theo khoá chính — giữ nguyên hàng cũ và mọi `habit_logs` của nó. */
    @Update
    suspend fun updateHabit(habit: HabitEntity)

    /** Soft delete — không xóa thật, chỉ đánh dấu isActive = false */
    @Query("UPDATE habits SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteHabit(id: Long)
}
