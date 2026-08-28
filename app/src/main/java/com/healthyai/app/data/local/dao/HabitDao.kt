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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    /** Soft delete — không xóa thật, chỉ đánh dấu isActive = false */
    @Query("UPDATE habits SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteHabit(id: Long)
}
