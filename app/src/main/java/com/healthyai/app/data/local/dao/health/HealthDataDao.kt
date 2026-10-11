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

    /**
     * Nhịp tim **ghi gần nhất từ** [fromTs] trở đi (bỏ qua các hàng chưa có nhịp tim), dùng cho ô
     * "Tim" ở màn Trang chủ khi Health Connect chưa có số.
     *
     * Màn Trang chủ truyền **đầu ngày hôm nay** để nhịp tim cũng được "đọc lại từ đầu ngày" như bước
     * chân và giờ ngủ: sang ngày mới mà chưa đo lại thì trả `null` thay vì hiện lại số của hôm qua.
     *
     * Trả `null` khi khoảng đó chưa có hàng nào có nhịp tim.
     */
    @Query("SELECT heartRate FROM health_data_history WHERE heartRate > 0 AND recordedAt >= :fromTs ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestHeartRateSince(fromTs: Long): Double?

    /**
     * Toàn bộ hàng lịch sử **ghi từ** [fromTs] trở đi, mới nhất trước — nguồn cho thẻ "Lịch sử sức
     * khỏe 7 ngày" ở tab Phân tích.
     *
     * Khác [getRecentHealthData] (giới hạn theo **số hàng**), truy vấn này giới hạn theo **mốc thời
     * gian**: dữ liệu cũ có thể có nhiều hàng trong cùng một ngày nên cắt theo số hàng sẽ làm thiếu
     * ngày, còn tầng dùng luôn gom lại theo ngày.
     */
    @Query("SELECT * FROM health_data_history WHERE recordedAt >= :fromTs ORDER BY recordedAt DESC")
    fun getHealthDataSince(fromTs: Long): Flow<List<HealthDataEntity>>

    /** Xóa dữ liệu cũ hơn 30 ngày để tiết kiệm bộ nhớ */
    @Query("DELETE FROM health_data_history WHERE recordedAt < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
}
