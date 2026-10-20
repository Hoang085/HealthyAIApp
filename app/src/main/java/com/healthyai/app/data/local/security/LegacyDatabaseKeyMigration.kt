package com.healthyai.app.data.local.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import net.zetetic.database.sqlcipher.SQLiteDatabase

/** Chuyển DB bản cũ (khoá hard-code) sang khoá Keystore mới một lần để giữ nguyên dữ liệu. */
object LegacyDatabaseKeyMigration {

    private const val TAG = "DatabaseKeyMigration"

    /** Passphrase hard-code bản cũ; chỉ dùng một lần để mở DB cũ rekey sang khoá Keystore. */
    const val LEGACY_PASSPHRASE = "REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE"

    /** Gọi chỉ khi passphrase vừa sinh mới, lúc đó file DB còn sót lại chắc chắn thuộc bản cũ. */
    fun rekeyLegacyDatabaseIfPresent(
        context: Context,
        databaseName: String,
        newPassphrase: ByteArray,
    ) {
        val databaseFile = context.getDatabasePath(databaseName)
        if (!databaseFile.exists()) return

        try {
            rekey(databaseFile.absolutePath, newPassphrase)
            Log.i(TAG, "Đã đổi khoá '$databaseName' từ passphrase hard-code sang khoá Android Keystore")
        } catch (e: CancellationException) {
            // Không nuốt tín hiệu huỷ coroutine dù hàm không suspend: có thể được gọi trong ngữ cảnh đã huỷ.
            throw e
        } catch (e: Exception) {
            // Không rekey được DB cũ → giữ lại sẽ crash mỗi lần Room mở, nên xoá để tạo DB mới sạch.
            Log.w(TAG, "Không rekey được DB cũ → xoá để tạo DB mới (mất dữ liệu cục bộ)", e)
            context.deleteDatabase(databaseName)
        }
    }

    private fun rekey(path: String, newPassphrase: ByteArray) {
        val database = SQLiteDatabase.openDatabase(
            path,
            LEGACY_PASSPHRASE.toByteArray(Charsets.UTF_8),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null
        )
        try {
            // Checkpoint WAL rồi thoát WAL trước khi rekey (tránh file trộn trang khoá cũ/mới), sau đó bật lại WAL.
            database.execPragma("PRAGMA wal_checkpoint(TRUNCATE)")
            database.execPragma("PRAGMA journal_mode=DELETE")
            database.changePassword(newPassphrase)
            database.execPragma("PRAGMA journal_mode=WAL")
        } finally {
            database.close()
        }
    }

    /** Chạy 1 PRAGMA trả về dòng kết quả (`journal_mode`, `wal_checkpoint`, …). */
    private fun SQLiteDatabase.execPragma(sql: String) {
        query(sql).use { it.moveToFirst() }
    }
}
