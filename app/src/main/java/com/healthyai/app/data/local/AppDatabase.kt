package com.healthyai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity

/**
 * Room Database — được mã hóa bởi SQLCipher (xem DatabaseModule.kt).
 * exportSchema = true để version control schema, phục vụ Migration sau này.
 *
 * Khi cần nâng version: tăng version lên và implement Migration thay vì fallbackToDestructiveMigration.
 */
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao

    companion object {
        const val DATABASE_NAME = "healthy_ai_encrypted.db"
    }
}
