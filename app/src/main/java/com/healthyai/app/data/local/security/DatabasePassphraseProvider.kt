package com.healthyai.app.data.local.security

import java.security.GeneralSecurityException
import java.security.SecureRandom

/** Passphrase mở Room DB qua SQLCipher; newlyCreated cho biết passphrase vừa sinh nên phải dọn file DB cũ. */
class DatabasePassphrase(val bytes: ByteArray, val newlyCreated: Boolean) {

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is DatabasePassphrase &&
                newlyCreated == other.newlyCreated &&
                bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + newlyCreated.hashCode()

    /** Cố tình không in nội dung khoá ra log/crash report. */
    override fun toString(): String = "DatabasePassphrase(***, newlyCreated=$newlyCreated)"
}

/** Nơi lưu passphrase đã mã hoá; tách interface để test được sinh/đọc lại trên JVM. */
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

    /** @throws GeneralSecurityException khi bản mã hỏng hoặc khoá Keystore không còn dùng được. */
    fun decrypt(encoded: String): ByteArray
}

/** Nguồn passphrase SQLCipher; sinh đúng một lần mỗi máy, bọc bằng khoá Android Keystore. */
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
                // Khoá Keystore mất/không giải mã được → sinh khoá mới, báo newlyCreated=true để tầng gọi dọn DB cũ.
            }
        }

        val fresh = ByteArray(PASSPHRASE_LENGTH_BYTES).also(random::nextBytes)
        // Ghi trước khi trả khoá để lần mở sau vẫn đọc lại được nếu tiến trình chết ngay sau khi tạo DB.
        vault.write(cipher.encrypt(fresh))
        return DatabasePassphrase(fresh, newlyCreated = true)
    }

    companion object {
        /** 32 byte = 256-bit entropy, khớp chuẩn mã hoá AES-256 của SQLCipher. */
        const val PASSPHRASE_LENGTH_BYTES = 32
    }
}
