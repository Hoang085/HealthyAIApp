package com.healthyai.app.data.local.security

import java.security.GeneralSecurityException
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [DatabasePassphraseProvider] — passphrase SQLCipher sinh một lần rồi đọc lại
 * (mục 3 #17, phụ lục C #13).
 *
 * Bối cảnh: trước đây `DatabaseModule` hard-code `"REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE"` nên khoá
 * mở DB nằm ngay trong APK. Test chốt lại 4 tính chất của bản sửa lỗi:
 * 1. máy mới → sinh passphrase ngẫu nhiên 32 byte và **lưu lại**;
 * 2. mở lại app → đọc đúng passphrase đã lưu, **không** sinh khoá mới (không làm DB cũ thành vô dụng);
 * 3. thiết bị mất khoá Keystore (khôi phục backup, reset khoá) → sinh khoá mới thay vì crash;
 * 4. không rò rỉ khoá qua `toString()` (log/crash report).
 *
 * Android Keystore chỉ có trên thiết bị nên ở đây dùng vault/cipher giả; luồng mã hoá thật
 * (`AndroidKeystorePassphraseCipher`, AES-256/GCM) được kiểm tra theo **TC-DB-11**.
 */
class DatabasePassphraseProviderTest {

    private companion object {
        /** Bản mã "rác" mô phỏng prefs còn dữ liệu nhưng khoá Keystore đã mất. */
        const val CORRUPT_STORED_VALUE = "bản-mã-không-giải-mã-được"
    }

    /** Vault trong bộ nhớ, đếm số lần ghi để phát hiện việc ghi đè khoá ngoài ý muốn. */
    private class InMemoryVault(private var stored: String? = null) : PassphraseVault {
        var writeCount = 0
            private set

        override fun read(): String? = stored

        override fun write(encoded: String) {
            stored = encoded
            writeCount++
        }
    }

    /**
     * Cipher giả "không mã hoá" (Base64) — chỉ để chạy trên JVM; [failDecrypt] mô phỏng khoá
     * Keystore bị mất nên không giải mã được bản mã đã lưu.
     */
    private class FakeCipher(private val failDecrypt: Boolean = false) : PassphraseCipher {
        override fun encrypt(plain: ByteArray): String = Base64.getEncoder().encodeToString(plain)

        override fun decrypt(encoded: String): ByteArray {
            if (failDecrypt) throw GeneralSecurityException("Khoá Keystore không dùng được")
            return Base64.getDecoder().decode(encoded)
        }
    }

    @Test
    fun `máy mới sinh passphrase ngẫu nhiên 32 byte và lưu lại`() {
        val vault = InMemoryVault()

        val passphrase = DatabasePassphraseProvider(vault, FakeCipher()).getOrCreate()

        assertTrue(passphrase.newlyCreated)
        assertEquals(DatabasePassphraseProvider.PASSPHRASE_LENGTH_BYTES, passphrase.bytes.size)
        assertEquals("phải lưu bản mã đúng 1 lần", 1, vault.writeCount)
        assertNotNull("passphrase đã được lưu để lần sau dùng lại", vault.read())
    }

    @Test
    fun `mở lại app đọc đúng passphrase đã lưu và không sinh khoá mới`() {
        val vault = InMemoryVault()

        val first = DatabasePassphraseProvider(vault, FakeCipher()).getOrCreate()
        val second = DatabasePassphraseProvider(vault, FakeCipher()).getOrCreate()

        assertArrayEquals(first.bytes, second.bytes)
        assertFalse(second.newlyCreated)
        assertEquals("không ghi lại vault khi đã có passphrase", 1, vault.writeCount)
    }

    @Test
    fun `vault rỗng thì hai máy khác nhau nhận hai passphrase khác nhau`() {
        val first = DatabasePassphraseProvider(InMemoryVault(), FakeCipher()).getOrCreate()
        val second = DatabasePassphraseProvider(InMemoryVault(), FakeCipher()).getOrCreate()

        assertFalse(
            "passphrase phải ngẫu nhiên, không phải hằng số dùng chung như bản hard-code",
            first.bytes.contentEquals(second.bytes)
        )
    }

    @Test
    fun `mất khoá Keystore thì sinh khoá mới thay vì crash`() {
        val vault = InMemoryVault(stored = CORRUPT_STORED_VALUE)

        val passphrase = DatabasePassphraseProvider(vault, FakeCipher(failDecrypt = true)).getOrCreate()

        assertTrue("phải báo newlyCreated để tầng gọi dọn DB không mở được", passphrase.newlyCreated)
        assertEquals(DatabasePassphraseProvider.PASSPHRASE_LENGTH_BYTES, passphrase.bytes.size)
        assertEquals("bản mã cũ phải bị ghi đè", 1, vault.writeCount)
        val storedAfterRecovery = vault.read()
        assertNotNull(storedAfterRecovery)
        assertTrue(
            "bản mã cũ phải được thay bằng bản mã của khoá mới",
            storedAfterRecovery != CORRUPT_STORED_VALUE
        )
    }

    @Test
    fun `passphrase đã lưu không bao giờ lộ qua toString`() {
        val vault = InMemoryVault()
        val provider = DatabasePassphraseProvider(vault, FakeCipher())

        val created = provider.getOrCreate()
        val reused = provider.getOrCreate()

        val rawHex = created.bytes.joinToString("") { "%02x".format(it) }
        assertFalse(created.toString().contains(rawHex))
        assertFalse(reused.toString().contains(rawHex))
        assertTrue(created.toString().contains("***"))
    }

    @Test
    fun `so sánh passphrase theo nội dung byte chứ không theo tham chiếu`() {
        val first = DatabasePassphrase(ByteArray(32) { 1.toByte() }, newlyCreated = true)
        val sameBytes = DatabasePassphrase(ByteArray(32) { 1.toByte() }, newlyCreated = true)
        val otherBytes = DatabasePassphrase(ByteArray(32) { 2.toByte() }, newlyCreated = true)
        val otherOrigin = DatabasePassphrase(ByteArray(32) { 1.toByte() }, newlyCreated = false)

        assertEquals(first, sameBytes)
        assertEquals(first.hashCode(), sameBytes.hashCode())
        assertNotEquals(first, otherBytes)
        assertNotEquals(first, otherOrigin)
    }
}
