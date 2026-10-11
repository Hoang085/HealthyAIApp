package com.healthyai.app.domain.usecase.sync

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho **cổng riêng tư** [CloudSyncPolicy] (mục 3 #31).
 *
 * Đây là một chỗ duy nhất quyết định log nào được phép rời khỏi thiết bị, nên các test ở đây chốt:
 * mặc định (chưa bật) **không** tải gì, thiếu mốc đồng ý cũng **không** tải gì, và khi đã bật thì
 * **chỉ** log tạo sau mốc đồng ý mới được tải — không bao giờ đẩy lịch sử cũ lên đám mây.
 */
class CloudSyncPolicyTest {

    private val oldLog = log(loggedAt = 1_000L, id = 1L)
    private val newLog = log(loggedAt = 5_000L, id = 2L)
    private val allLogs = listOf(oldLog, newLog)

    @Test
    fun `nothing is uploaded while cloud sync is off`() {
        val uploaded = CloudSyncPolicy.logsToUpload(allLogs, CloudSyncSettings(enabled = false))

        assertTrue("Chưa bật đồng bộ thì không tải gì lên", uploaded.isEmpty())
    }

    @Test
    fun `a consented window without a timestamp uploads nothing`() {
        // Dữ liệu thiếu/hỏng → chọn hướng an toàn: không tải gì.
        val uploaded = CloudSyncPolicy.logsToUpload(
            allLogs,
            CloudSyncSettings(enabled = true, consentAtMillis = null)
        )

        assertTrue(uploaded.isEmpty())
    }

    @Test
    fun `only logs created after the consent moment are uploaded`() {
        val uploaded = CloudSyncPolicy.logsToUpload(
            allLogs,
            CloudSyncSettings(enabled = true, consentAtMillis = 3_000L)
        )

        assertEquals(listOf(newLog), uploaded)
    }

    @Test
    fun `logs created before consent are never uploaded even when they are dirty`() {
        // Mô phỏng đúng lỗi gốc: người dùng ghi nhật ký ở chế độ khách (isDirty = true) rồi đăng nhập
        // và bật đồng bộ — lịch sử cũ KHÔNG được đẩy lên.
        val guestHistory = listOf(
            log(loggedAt = 100L, id = 10L),
            log(loggedAt = 200L, id = 11L)
        )

        val uploaded = CloudSyncPolicy.logsToUpload(
            guestHistory,
            CloudSyncSettings(enabled = true, consentAtMillis = 1_000L)
        )

        assertTrue(uploaded.isEmpty())
    }

    @Test
    fun `a log exactly at the consent moment is uploaded`() {
        val uploaded = CloudSyncPolicy.logsToUpload(
            listOf(log(loggedAt = 3_000L, id = 7L)),
            CloudSyncSettings(enabled = true, consentAtMillis = 3_000L)
        )

        assertEquals(1, uploaded.size)
    }

    private fun log(loggedAt: Long, id: Long) = HabitLog(
        id = id,
        habitId = 1L,
        habitName = "Uống nước",
        category = HabitCategory.WATER,
        value = 500f,
        unit = "ml",
        targetValueAtTime = 2000f,
        loggedAt = loggedAt,
        isDirty = true
    )
}
