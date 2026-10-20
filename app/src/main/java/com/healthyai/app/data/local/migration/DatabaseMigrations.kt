package com.healthyai.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migration Room theo từng bước version; thêm bước mới phải cập nhật [ALL] và [DATABASE_VERSION]. */
object DatabaseMigrations {

    /** Version schema hiện tại; phải khớp `@Database(version = …)`. */
    const val DATABASE_VERSION = 4

    /** 1 → 2: thêm `habits.timeOfDay`, `habits.frequency`, `habit_logs.targetValueAtTime`. */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            STATEMENTS_1_2.forEach(db::execSQL)
        }
    }

    /** 2 → 3: thêm bảng `health_data_history` (lịch sử dữ liệu Health Connect). */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            STATEMENTS_2_3.forEach(db::execSQL)
        }
    }

    /** 3 → 4: thêm bảng `app_settings` (thiết lập người dùng, dạng khoá – giá trị). */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            STATEMENTS_3_4.forEach(db::execSQL)
        }
    }

    /** Toàn bộ migration theo thứ tự — dùng cho `Room.databaseBuilder(...).addMigrations(*ALL)`. */
    val ALL: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

    /** Giá trị mặc định khớp model domain: thói quen cũ coi là cả ngày và hàng ngày. */
    internal val STATEMENTS_1_2: List<String> = listOf(
        "ALTER TABLE `habits` ADD COLUMN `timeOfDay` TEXT NOT NULL DEFAULT 'ALL_DAY'",
        "ALTER TABLE `habits` ADD COLUMN `frequency` TEXT NOT NULL DEFAULT 'DAILY'",
        "ALTER TABLE `habit_logs` ADD COLUMN `targetValueAtTime` REAL NOT NULL DEFAULT 0",
        // Log cũ chưa lưu mục tiêu → lấy mục tiêu hiện tại của thói quen để "Chỉ số sinh lực" không bị 0%.
        """
        UPDATE `habit_logs`
        SET `targetValueAtTime` = COALESCE(
            (SELECT `targetValue` FROM `habits` WHERE `habits`.`id` = `habit_logs`.`habitId`),
            0
        )
        WHERE `targetValueAtTime` = 0
        """.trimIndent()
    )

    /** DDL của `health_data_history` lấy đúng theo `app/schemas/…/3.json`. */
    internal val STATEMENTS_2_3: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `health_data_history` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`steps` INTEGER NOT NULL, " +
            "`sleepHours` REAL NOT NULL, " +
            "`heartRate` REAL NOT NULL, " +
            "`activeCalories` REAL NOT NULL, " +
            "`recordedAt` INTEGER NOT NULL)"
    )

    /** DDL của `app_settings`, phải khớp từng ký tự với `createSql` trong schema 4.json. */
    internal val STATEMENTS_3_4: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `app_settings` (" +
            "`settingKey` TEXT NOT NULL, " +
            "`settingValue` TEXT NOT NULL, " +
            "PRIMARY KEY(`settingKey`))"
    )
}
