package com.healthyai.app.data.local.dao

import androidx.room.*
import com.healthyai.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    /** Lấy tất cả log từ timestamp trở đi, sắp xếp mới nhất trước */
    @Query("SELECT * FROM habit_logs WHERE loggedAt >= :fromTimestamp ORDER BY loggedAt DESC")
    fun getLogsFromTimestamp(fromTimestamp: Long): Flow<List<HabitLogEntity>>

    /** Lấy các bản ghi chưa đồng bộ lên cloud (cho SyncWorker) */
    @Query("SELECT * FROM habit_logs WHERE isDirty = 1")
    suspend fun getUnsyncedLogs(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity): Long

    /** Đánh dấu đã sync thành công */
    @Query("UPDATE habit_logs SET isDirty = 0 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    /** Lấy N bản ghi gần nhất theo category — dùng cho Rule Engine */
    @Query("""
        SELECT * FROM habit_logs 
        WHERE category = :category 
        ORDER BY loggedAt DESC 
        LIMIT :limit
    """)
    suspend fun getRecentLogsByCategory(category: String, limit: Int = 7): List<HabitLogEntity>
}
