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
    /**
     * Lịch sử sức khỏe mới nhất, tối đa [limit] hàng.
     *
     * Phụ lục C #16: trước đây truy vấn hard-code `LIMIT 30` nên tham số `limit` của repository bị
     * bỏ qua — nơi gọi xin 7 hàng nhưng vẫn nhận tới 30.
     */
    @Query("SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentHealthData(limit: Int): Flow<List<HealthDataEntity>>

    @Query("SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT :limit")
    suspend fun getRecentHealthDataList(limit: Int): List<HealthDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthData(data: HealthDataEntity)

    /**
     * Hàng dữ liệu sức khỏe **mới nhất được ghi từ** [fromTs] trở đi — dùng để cập nhật hàng của
     * **hôm nay** thay vì thêm hàng mới mỗi lần đồng bộ (mỗi lần sync vẫn là 1 hàng, nhưng trong ngày
     * chỉ giữ 1 hàng đã gộp: bước chân lớn nhất, giấc ngủ/nhịp tim/calo mới nhất).
     */
    @Query("SELECT * FROM health_data_history WHERE recordedAt >= :fromTs ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestSince(fromTs: Long): HealthDataEntity?

    @Update
    suspend fun updateHealthData(data: HealthDataEntity)

    /** Xóa dữ liệu cũ hơn 30 ngày để tiết kiệm bộ nhớ */
    @Query("DELETE FROM health_data_history WHERE recordedAt < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
}
