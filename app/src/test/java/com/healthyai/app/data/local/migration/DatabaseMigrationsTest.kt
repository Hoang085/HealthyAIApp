package com.healthyai.app.data.local.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test [DatabaseMigrations]; không chạy được SQLite trên JVM nên chỉ kiểm tra cấu trúc dãy migration. */
class DatabaseMigrationsTest {

    @Test
    fun `migration chain covers every version from 1 to the current database version`() {
        val expectedSteps = (1 until DatabaseMigrations.DATABASE_VERSION).map { it }

        assertEquals(
            "Số bước migration phải bằng (DATABASE_VERSION - 1)",
            expectedSteps.size,
            DatabaseMigrations.ALL.size
        )

        DatabaseMigrations.ALL.forEachIndexed { index, migration ->
            val from = expectedSteps[index]
            assertEquals("Thứ tự/điểm bắt đầu của migration thứ ${index + 1}", from, migration.startVersion)
            assertEquals("Điểm kết thúc của migration thứ ${index + 1}", from + 1, migration.endVersion)
        }
    }

    @Test
    fun `each migration executes at least one statement`() {
        assertTrue(DatabaseMigrations.STATEMENTS_1_2.isNotEmpty())
        assertTrue(DatabaseMigrations.STATEMENTS_2_3.isNotEmpty())
        assertTrue(DatabaseMigrations.STATEMENTS_3_4.isNotEmpty())
        DatabaseMigrations.STATEMENTS_1_2.forEach { assertTrue(it.isNotBlank()) }
        DatabaseMigrations.STATEMENTS_2_3.forEach { assertTrue(it.isNotBlank()) }
        DatabaseMigrations.STATEMENTS_3_4.forEach { assertTrue(it.isNotBlank()) }
    }

    /** 1 → 2 chỉ thêm cột, không dựng lại `habit_logs` (FK CASCADE tới `habits`) nên không mất dữ liệu. */
    @Test
    fun `migration 1 to 2 only alters tables and backfills target value`() {
        assertTrue(
            DatabaseMigrations.STATEMENTS_1_2.none { it.contains("DROP TABLE", ignoreCase = true) }
        )
        assertTrue(
            DatabaseMigrations.STATEMENTS_1_2.any { it.startsWith("UPDATE `habit_logs`") }
        )
    }

    /** 3 → 4 chỉ tạo thêm bảng `app_settings`, không đụng bảng cũ nên không mất dữ liệu. */
    @Test
    fun `migration 3 to 4 creates the settings table without touching existing tables`() {
        assertTrue(
            DatabaseMigrations.STATEMENTS_3_4.none { it.contains("DROP", ignoreCase = true) }
        )
        assertTrue(
            DatabaseMigrations.STATEMENTS_3_4.none { it.contains("ALTER TABLE", ignoreCase = true) }
        )
        assertTrue(
            DatabaseMigrations.STATEMENTS_3_4.single().startsWith("CREATE TABLE IF NOT EXISTS `app_settings`")
        )
    }
}
