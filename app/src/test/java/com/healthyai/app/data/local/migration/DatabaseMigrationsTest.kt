package com.healthyai.app.data.local.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cho [DatabaseMigrations] (phụ lục C #14).
 *
 * Không chạy được SQLite trên JVM nên test này kiểm tra **cấu trúc** của dãy migration — phần dễ
 * bị bỏ sót nhất khi nâng version schema: thêm `@Database(version = 4)` mà quên `MIGRATION_3_4`
 * thì Room sẽ crash (`IllegalStateException: A migration from 3 to 4 was required but not found`)
 * trên **mọi** máy đã cài bản cũ.
 */
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
        DatabaseMigrations.STATEMENTS_1_2.forEach { assertTrue(it.isNotBlank()) }
        DatabaseMigrations.STATEMENTS_2_3.forEach { assertTrue(it.isNotBlank()) }
    }

    /**
     * 1 → 2 chỉ thêm cột (không drop/tạo lại bảng) để `habit_logs` — bảng có FK
     * `ON DELETE CASCADE` trỏ tới `habits` — không bị mất dữ liệu khi nâng cấp.
     */
    @Test
    fun `migration 1 to 2 only alters tables and backfills target value`() {
        assertTrue(
            DatabaseMigrations.STATEMENTS_1_2.none { it.contains("DROP TABLE", ignoreCase = true) }
        )
        assertTrue(
            DatabaseMigrations.STATEMENTS_1_2.any { it.startsWith("UPDATE `habit_logs`") }
        )
    }
}
