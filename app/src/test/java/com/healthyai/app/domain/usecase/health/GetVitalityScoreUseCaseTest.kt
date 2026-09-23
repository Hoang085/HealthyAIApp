package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test cho điểm sinh lực kèm phần so sánh với tuần trước (thay cho dòng hard-code
 * "+5 so với tuần trước").
 *
 * Công thức điểm: `sleep*0.4 + water*0.3 + exercise*0.3`; danh mục không có log tính 70 điểm.
 */
class GetVitalityScoreUseCaseTest {

    private val habitRepository = mockk<HabitRepository>()
    private val useCase = GetVitalityScoreUseCase(habitRepository, HabitRuleEngine())

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now(zone)

    @Test
    fun `delta is null when there is no log in the previous week`() = runTest {
        givenLogs(sleepLog(value = 8f, date = today)) // 100% → 100*0.4 + 70*0.3 + 70*0.3 = 82

        val result = useCase().first()

        assertEquals(82, result.score)
        assertNull("Chưa có dữ liệu tuần trước thì phải ẩn phần so sánh", result.delta)
    }

    @Test
    fun `delta is positive when current week is better than previous week`() = runTest {
        givenLogs(
            sleepLog(value = 8f, date = today),              // tuần này: 100% → 82
            sleepLog(value = 4f, date = today.minusDays(8))  // tuần trước: 50% → 62
        )

        val result = useCase().first()

        assertEquals(82, result.score)
        assertEquals(20, result.delta)
    }

    @Test
    fun `delta is negative when current week is worse than previous week`() = runTest {
        givenLogs(
            sleepLog(value = 4f, date = today),              // tuần này: 50% → 62
            sleepLog(value = 8f, date = today.minusDays(8))  // tuần trước: 100% → 82
        )

        val result = useCase().first()

        assertEquals(62, result.score)
        assertEquals(-20, result.delta)
    }

    @Test
    fun `log at the start of the seven day window belongs to the current week`() = runTest {
        givenLogs(
            sleepLogAtEpochMillis(
                value = 8f,
                epochMillis = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        )

        val result = useCase().first()

        assertEquals("Log ở đúng mốc đầu cửa sổ 7 ngày phải thuộc tuần hiện tại", 82, result.score)
        assertNull(result.delta)
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun givenLogs(vararg logs: HabitLog) {
        every { habitRepository.getHabitLogsForPeriod(13) } returns flowOf(logs.toList())
    }

    private fun sleepLog(value: Float, date: LocalDate) = sleepLogAtEpochMillis(
        value = value,
        // giữa trưa để chắc chắn nằm trong ngày
        epochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 60 * 60 * 1000L
    )

    private fun sleepLogAtEpochMillis(value: Float, epochMillis: Long) = HabitLog(
        habitId = 1L,
        habitName = "Giấc ngủ",
        category = HabitCategory.SLEEP,
        value = value,
        unit = "giờ",
        targetValueAtTime = 8f,
        loggedAt = epochMillis
    )
}
