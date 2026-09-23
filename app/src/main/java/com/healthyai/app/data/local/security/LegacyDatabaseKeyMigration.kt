package com.healthyai.app.data.local.security

import android.content.Context
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Chuyển DB của bản cũ sang khoá Keystore mới.
 *
 * Trước bản sửa lỗi này, `di/DatabaseModule` mở DB bằng passphrase hard-code
 * [LEGACY_PASSPHRASE]. Sau khi đổi sang passphrase sinh từ Android Keystore, máy đã cài bản cũ vẫn
 * còn file `healthy_ai_encrypted.db` mã hoá bằng khoá cũ → mở bằng khoá mới sẽ lỗi *"file is not a
 * database"* ngay khi Room chạy truy vấn đầu tiên, và `fallbackToDestructiveMigration()` **không**
 * cứu được (nó chỉ xử lý lệch `version` schema, còn `version` ở đây không đổi) → app crash vĩnh
 * viễn cho tới khi người dùng xoá dữ liệu app.
 *
 * Vì vậy đúng **một lần** trên mỗi máy (lần đầu chạy bản mới), ta mở DB cũ bằng [LEGACY_PASSPHRASE]
 * rồi đổi sang khoá mới bằng `PRAGMA rekey` → toàn bộ thói quen/nhật ký được giữ nguyên.
 */
object LegacyDatabaseKeyMigration {

    private const val TAG = "DatabaseKeyMigration"

    /**
     * Passphrase hard-code của bản cũ — **chỉ còn đúng một công dụng**: mở DB cũ một lần để rekey
     * sang khoá Keystore. Nó **không** còn được dùng để mở DB cho app chạy bình thường.
     *
     * Có thể xoá hằng số này (và cả file) khi không còn máy nào chạy bản cũ — ví dụ sau khi mọi bản
     * test đã `pm clear` / cài mới hoàn toàn.
     */
    const val LEGACY_PASSPHRASE = "REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE"

    /**
     * Gọi **chỉ khi** passphrase vừa được sinh mới (`DatabasePassphrase.newlyCreated == true`): lúc
     * đó mọi file DB còn sót lại trên máy chắc chắn thuộc bản cũ.
     */
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
        } catch (e: Exception) {
            // Không mở/rekey được DB cũ (file hỏng, khoá lạ, DB do bản build khác tạo…). Giữ lại
            // file sẽ làm app crash vĩnh viễn mỗi lần Room mở → xoá để app tạo DB mới sạch, đúng
            // hành vi người dùng đã gặp trước đây khi đổi schema.
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
            // 1) đẩy hết frame trong WAL vào file chính;
            // 2) thoát WAL — rekey khi còn frame trong WAL có nguy cơ sinh file trộn trang khoá
            //    cũ/khoá mới (hỏng DB);
            // 3) đổi khoá;
            // 4) bật lại WAL như Room vẫn dùng.
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
