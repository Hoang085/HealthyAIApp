package com.healthyai.app.data.local.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Lưu passphrase SQLCipher đã mã hoá vào SharedPreferences riêng; ghi đồng bộ bằng commit() để không mất khoá trước khi Room mở DB. */
class SharedPreferencesPassphraseVault(context: Context) : PassphraseVault {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_PASSPHRASE, null)

    override fun write(encoded: String) {
        // commit(): đảm bảo bản mã đã nằm trên đĩa trước khi DB được tạo bằng passphrase này.
        prefs.edit().putString(KEY_PASSPHRASE, encoded).commit()
    }

    companion object {
        const val PREFS_NAME = "healthy_ai_secure_store"
        private const val KEY_PASSPHRASE = "sqlcipher_passphrase_v1"
    }
}

/** Bọc passphrase bằng khoá AES-256 sinh và giữ trong Android Keystore, không rời khỏi thiết bị. */
class AndroidKeystorePassphraseCipher : PassphraseCipher {

    override fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
    }

    override fun decrypt(encoded: String): ByteArray {
        val raw = try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Bản mã passphrase trong prefs không hợp lệ", e)
        }
        if (raw.size <= GCM_IV_LENGTH_BYTES) {
            throw GeneralSecurityException("Bản mã passphrase trong prefs quá ngắn")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, raw, 0, GCM_IV_LENGTH_BYTES)
        )
        return cipher.doFinal(raw, GCM_IV_LENGTH_BYTES, raw.size - GCM_IV_LENGTH_BYTES)
    }

    /** Lấy khoá Keystore, tạo mới nếu máy chưa có (idempotent). */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
        }.generateKey()
    }

    companion object {
        /** Alias khoá Keystore; đổi alias sẽ mất khả năng giải mã passphrase cũ. */
        const val KEY_ALIAS = "healthy_ai_sqlcipher_key_v1"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
