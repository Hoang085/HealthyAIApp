package com.healthyai.app.data.local.dao.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthyai.app.data.local.entity.settings.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    /** Toàn bộ thiết lập, đọc một lần (dùng cho worker). */
    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<SettingEntity>

    /** Toàn bộ thiết lập, phát lại mỗi khi bảng đổi (dùng để UI luôn khớp với DB). */
    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<SettingEntity>>

    /** Ghi/đè một hoặc nhiều thiết lập trong **một** câu lệnh. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(settings: List<SettingEntity>)
}
