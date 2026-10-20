package com.healthyai.app.data.local.dao.health

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDataDao {
    /** Lịch sử sức khỏe mới nhất, tối đa [limit] hàng. */
    @Query("SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentHealthData(limit: Int): Flow<List<HealthDataEntity>>

    @Query("SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT :limit")
    suspend fun getRecentHealthDataList(limit: Int): List<HealthDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthData(data: HealthDataEntity)

    /** Hàng sức khỏe mới nhất ghi từ [fromTs] — dùng để gộp cập nhật hàng hôm nay thay vì thêm hàng mới mỗi lần sync. */
    @Query("SELECT * FROM health_data_history WHERE recordedAt >= :fromTs ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestSince(fromTs: Long): HealthDataEntity?

    @Update
    suspend fun updateHealthData(data: HealthDataEntity)

    /** Nhịp tim ghi gần nhất từ [fromTs] (bỏ hàng không có nhịp tim); trả null nếu chưa có — dùng cho ô "Tim" ở Trang chủ. */
    @Query("SELECT heartRate FROM health_data_history WHERE heartRate > 0 AND recordedAt >= :fromTs ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestHeartRateSince(fromTs: Long): Double?

    /** Hàng lịch sử ghi từ [fromTs], mới nhất trước — cắt theo mốc thời gian (khác [getRecentHealthData] cắt theo số hàng) vì nhiều hàng có thể cùng ngày. */
    @Query("SELECT * FROM health_data_history WHERE recordedAt >= :fromTs ORDER BY recordedAt DESC")
    fun getHealthDataSince(fromTs: Long): Flow<List<HealthDataEntity>>

    /** Xóa dữ liệu cũ hơn 30 ngày để tiết kiệm bộ nhớ */
    @Query("DELETE FROM health_data_history WHERE recordedAt < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
}
