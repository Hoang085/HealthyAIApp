package com.healthyai.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration cho Room Database (phụ lục C #14).
 *
 * Trước bản sửa này `di/DatabaseModule` dùng `fallbackToDestructiveMigration()`: mỗi lần tăng
 * `version` của `@Database`, Room **xoá sạch mọi bảng** rồi tạo lại → người dùng mất toàn bộ thói
 * quen và nhật ký. Nay mỗi bước tăng version có một `Migration` giữ nguyên dữ liệu.
 *
 * Bản đồ version (đối chiếu `app/schemas/com.healthyai.app.data.local.AppDatabase/<n>.json`):
 * - **1**: `habits` chưa có `timeOfDay`/`frequency`; `habit_logs` chưa có `targetValueAtTime`;
 *   chưa có bảng `health_data_history`.
 * - **2**: thêm 3 cột trên.
 * - **3**: thêm bảng `health_data_history`.
 * - **4**: thêm bảng `app_settings` (thiết lập của người dùng, dạng khoá – giá trị).
 *
 * ⚠️ **Khi tăng version**: thêm `MIGRATION_<n>_<n+1>` vào [ALL] và cập nhật [DATABASE_VERSION] cho
 * khớp `@Database(version = …)` trong `data/local/AppDatabase.kt`. Test `DatabaseMigrationsTest`
 * sẽ fail nếu [ALL] không phủ liên tục từ 1 tới [DATABASE_VERSION] (không được bỏ trống bước nào).
 *
 * Các câu lệnh dùng `ALTER TABLE … ADD COLUMN … DEFAULT …` chứ **không** tạo bảng mới/drop bảng:
 * giữ nguyên `habit_logs` (bảng có FK `ON DELETE CASCADE` trỏ tới `habits`) nên không có nguy cơ
 * mất log, và đã kiểm chứng Room không coi giá trị DEFAULT này là lệch schema (Room chỉ so sánh
 * default khi **cả hai** phía đều được tạo từ entity, không so với cột đọc lên từ database).
 */
object DatabaseMigrations {

    /**
     * Version schema hiện tại của `AppDatabase` — **phải khớp** `@Database(version = …)`.
     * Dùng cho test kiểm tra dãy migration không bị thiếu bước.
     */
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

    /**
     * 3 → 4: thêm bảng `app_settings` (thiết lập của người dùng, dạng **khoá – giá trị**).
     *
     * Bảng khoá – giá trị được chọn để các thiết lập thêm sau này **không cần** migration nữa.
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            STATEMENTS_3_4.forEach(db::execSQL)
        }
    }

    /** Toàn bộ migration theo thứ tự — dùng cho `Room.databaseBuilder(...).addMigrations(*ALL)`. */
    val ALL: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

    /**
     * Giá trị mặc định khớp với default trong model domain: thói quen cũ được coi là "Cả ngày"
     * (`HabitTimeOfDay.ALL_DAY`) và "Hàng ngày" (`HabitFrequency.DAILY`) — đúng như trước khi có
     * 2 trường này, mọi thói quen đều được hiểu là hàng ngày, cả ngày.
     */
    internal val STATEMENTS_1_2: List<String> = listOf(
        "ALTER TABLE `habits` ADD COLUMN `timeOfDay` TEXT NOT NULL DEFAULT 'ALL_DAY'",
        "ALTER TABLE `habits` ADD COLUMN `frequency` TEXT NOT NULL DEFAULT 'DAILY'",
        "ALTER TABLE `habit_logs` ADD COLUMN `targetValueAtTime` REAL NOT NULL DEFAULT 0",
        // Log cũ chưa lưu mục tiêu tại thời điểm ghi → lấy mục tiêu hiện tại của chính thói quen đó
        // để "Chỉ số sinh lực" không bị tính thành 0% cho toàn bộ dữ liệu trước khi nâng cấp.
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

    /**
     * DDL của `app_settings` — **phải khớp từng ký tự** với `createSql` mà Room sinh ra trong
     * `app/schemas/…/4.json`, nếu không Room sẽ báo lệch schema khi mở database
     * (`Migration didn't properly handle: app_settings`).
     */
    internal val STATEMENTS_3_4: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `app_settings` (" +
            "`settingKey` TEXT NOT NULL, " +
            "`settingValue` TEXT NOT NULL, " +
            "PRIMARY KEY(`settingKey`))"
    )
}
