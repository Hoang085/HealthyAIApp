package com.healthyai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity
import com.healthyai.app.data.local.entity.health.HealthDataEntity

/**
 * Room Database — được mã hóa bởi SQLCipher (xem DatabaseModule.kt).
 * exportSchema = true để version control schema, phục vụ Migration sau này.
 *
 * Khi cần nâng version: tăng `version` ở đây và thêm `Migration` tương ứng vào
 * `data/local/migration/DatabaseMigrations.kt` (`ALL` + `DATABASE_VERSION`) — **không** dùng
 * `fallbackToDestructiveMigration()` vì sẽ mất dữ liệu của người dùng (phụ lục C #14).
 */
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, HealthDataEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun healthDataDao(): HealthDataDao

    companion object {
        const val DATABASE_NAME = "healthy_ai_encrypted.db"
    }
}
