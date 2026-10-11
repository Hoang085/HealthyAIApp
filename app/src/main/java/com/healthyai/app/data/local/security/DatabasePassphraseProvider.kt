package com.healthyai.app.data.local.security

import java.security.GeneralSecurityException
import java.security.SecureRandom

/**
 * Passphrase dùng để mở Room Database qua SQLCipher.
 *
 * @property bytes khoá thô truyền thẳng cho `SupportOpenHelperFactory`.
 * @property newlyCreated `true` nếu passphrase **vừa được sinh trong lần gọi này** (chưa từng lưu
 *   trên máy) → tầng gọi phải xử lý file DB cũ còn sót lại (khoá hard-code của bản cũ), xem
 *   [LegacyDatabaseKeyMigration].
 */
class DatabasePassphrase(val bytes: ByteArray, val newlyCreated: Boolean) {

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is DatabasePassphrase &&
                newlyCreated == other.newlyCreated &&
                bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + newlyCreated.hashCode()

    /** Cố tình **không** in nội dung khoá ra log/crash report. */
    override fun toString(): String = "DatabasePassphrase(***, newlyCreated=$newlyCreated)"
}

/**
 * Nơi lưu passphrase **đã mã hoá**. Impl thật: `SharedPreferencesPassphraseVault`.
 *
 * Tách interface để logic sinh/đọc lại passphrase test được trên JVM (Android Keystore chỉ có trên
 * thiết bị nên không chạy được trong `src/test`).
 */
interface PassphraseVault {
    /** Bản mã đã lưu, hoặc `null` nếu máy chưa từng sinh passphrase. */
    fun read(): String?

    /** Ghi đè bản mã. */
    fun write(encoded: String)
}

/** Mã hoá/giải mã passphrase. Impl thật: `AndroidKeystorePassphraseCipher` (AES-256/GCM). */
interface PassphraseCipher {
    /** @throws GeneralSecurityException khi không mã hoá được. */
    fun encrypt(plain: ByteArray): String

    /**
     * @throws GeneralSecurityException khi bản mã hỏng **hoặc** khoá Keystore không còn dùng được
     *   (người dùng khôi phục từ backup, reset khoá thiết bị, xoá app data một phần…).
     */
    fun decrypt(encoded: String): ByteArray
}

/**
 * Nguồn passphrase SQLCipher của app (mục 3 #17, phụ lục C #13).
 *
 * Trước đây `DatabaseModule` hard-code `"REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE"` trong APK → bất kỳ
 * ai có file `healthy_ai_encrypted.db` (root máy, backup `adb`, file sao lưu) đều mở được vì khoá
 * nằm sẵn trong mã nguồn. Nay passphrase được **sinh ngẫu nhiên 32 byte** ở lần chạy đầu rồi bọc
 * bằng khoá AES-256 giữ trong **Android Keystore** (khoá không rời khỏi thiết bị), bản mã lưu ở
 * [PassphraseVault]. APK không còn chứa khoá nào dùng được để mở DB.
 *
 * Quy tắc: sinh đúng **một lần** trên mỗi máy và dùng lại cho các lần mở sau.
 */
class DatabasePassphraseProvider(
    private val vault: PassphraseVault,
    private val cipher: PassphraseCipher,
    private val random: SecureRandom = SecureRandom(),
) {

    fun getOrCreate(): DatabasePassphrase {
        val stored = vault.read()
        if (stored != null) {
            try {
                return DatabasePassphrase(cipher.decrypt(stored), newlyCreated = false)
            } catch (_: GeneralSecurityException) {
                // Khoá Keystore bị mất/không giải mã được → không thể đọc lại passphrase cũ.
                // Sinh khoá mới (bên dưới) và báo newlyCreated = true để tầng gọi dọn file DB
                // không mở được, tránh vòng lặp crash "file is not a database".
            }
        }

        val fresh = ByteArray(PASSPHRASE_LENGTH_BYTES).also(random::nextBytes)
        // Ghi **trước** khi trả khoá về cho tầng gọi (vault impl thật ghi đồng bộ bằng commit()):
        // nếu tiến trình chết ngay sau khi DB được tạo bằng khoá này, lần mở sau vẫn đọc lại được.
        vault.write(cipher.encrypt(fresh))
        return DatabasePassphrase(fresh, newlyCreated = true)
    }

    companion object {
        /** 32 byte = 256-bit entropy, khớp chuẩn mã hoá AES-256 của SQLCipher. */
        const val PASSPHRASE_LENGTH_BYTES = 32
    }
}
