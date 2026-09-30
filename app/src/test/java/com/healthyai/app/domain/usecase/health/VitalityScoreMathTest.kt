package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Test hàm thuần [VitalityScoreMath] — chạy trên JVM với **ngày/múi giờ cố định** nên tất định.
 *
 * Công thức: mỗi ngày trong 7 ngày gần nhất có điểm = tỉ lệ thói quen đạt mục tiêu (trọng số 0.4) +
 * giờ ngủ / 8h (0.3) + bước chân / 8000 (0.3), rồi chuẩn hoá lại theo các thành phần **thật sự có
 * nguồn trên máy**; điểm cuối là trung bình các ngày (ngày thiếu dữ liệu đóng góp 0).
 */
class VitalityScoreMathTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 28)

    private val sleepHabit = Habit(
        id = 1L,
        name = "Giấc ngủ",
        category = HabitCategory.SLEEP,
        targetValue = 8f,
        unit = "giờ"
    )

    private val waterHabit = Habit(
        id = 2L,
        name = "Uống nước",
        category = HabitCategory.WATER,
        targetValue = 2_000f,
        unit = "ml"
    )

    @Test
    fun `returns null when there is no source at all`() {
        val score = VitalityScoreMath.compute(
            activeHabits = emptyList(),
            habitLogs = emptyList(),
            healthHistory = emptyList(),
            today = today,
            zone = zone
        )

        assertNull(score)
    }

    @Test
    fun `habits completed every day score 100`() {
        val logs = (6 downTo 0).map { logAt(today.minusDays(it.toLong()), value = 8f) }

        val score = VitalityScoreMath.compute(listOf(sleepHabit), logs, emptyList(), today, zone)

        assertEquals(100, score)
    }

    @Test
    fun `skipped days drag the score down`() {
        // Chỉ 3 trong 7 ngày có log đạt mục tiêu → 3/7 ≈ 43
        val logs = (2 downTo 0).map { logAt(today.minusDays(it.toLong()), value = 8f) }

        val score = VitalityScoreMath.compute(listOf(sleepHabit), logs, emptyList(), today, zone)

        assertEquals(43, score)
    }

    @Test
    fun `half completion every day scores 50`() {
        // Mỗi ngày chỉ hoàn thành 1 trong 2 thói quen → tỉ lệ hoàn thành 0,5.
        val logs = (6 downTo 0).map { logAt(today.minusDays(it.toLong()), value = 8f) }

        val score = VitalityScoreMath.compute(
            activeHabits = listOf(sleepHabit, waterHabit),
            habitLogs = logs,
            healthHistory = emptyList(),
            today = today,
            zone = zone
        )

        assertEquals(50, score)
    }

    @Test
    fun `habits only renormalize the weights`() {
        // Chỉ có thành phần thói quen (máy không có bước chân/giờ ngủ) → điểm = tỉ lệ hoàn thành,
        // KHÔNG bị kéo xuống vì thiếu hai thành phần kia.
        val logs = (6 downTo 0).map { logAt(today.minusDays(it.toLong()), value = 8f) }

        val score = VitalityScoreMath.compute(listOf(sleepHabit), logs, emptyList(), today, zone)

        assertEquals(100, score)
    }

    @Test
    fun `sleep at target alone scores 100`() {
        val history = (6 downTo 0).map { rowAt(daysAgo = it, sleepHours = 8.0) }

        val score = VitalityScoreMath.compute(emptyList(), emptyList(), history, today, zone)

        assertEquals(100, score)
    }

    @Test
    fun `sleep and steps below target score proportionally`() {
        // Ngủ 4/8h = 0,5; bước 4000/8000 = 0,5 → (0,3*0,5 + 0,3*0,5)/0,6 = 0,5
        val history = (6 downTo 0).map { rowAt(daysAgo = it, steps = 4_000L, sleepHours = 4.0) }

        val score = VitalityScoreMath.compute(emptyList(), emptyList(), history, today, zone)

        assertEquals(50, score)
    }

    @Test
    fun `a missing day counts as zero for that component`() {
        // 7 ngày bước chân đủ mục tiêu, nhưng chỉ 1 ngày có giờ ngủ đủ 8h → thành phần ngủ chỉ đóng
        // góp 1/7.
        val history = (6 downTo 0).map { daysAgo ->
            if (daysAgo == 0) {
                rowAt(daysAgo = daysAgo, steps = 8_000L, sleepHours = 8.0)
            } else {
                rowAt(daysAgo = daysAgo, steps = 8_000L)
            }
        }

        val score = VitalityScoreMath.compute(emptyList(), emptyList(), history, today, zone)

        // (0,3*1 + 0,3*(1/7)) / 0,6 = (0,3 + 0,042857) / 0,6 = 0,57142 → 57
        assertEquals(57, score)
    }

    // ── hasObservations ────────────────────────────────────────

    @Test
    fun `hasObservations is true when there is a habit log in the window`() {
        val logs = listOf(logAt(today.minusDays(1), value = 8f))

        assertTrue(VitalityScoreMath.hasObservations(logs, emptyList(), today, zone))
    }

    @Test
    fun `hasObservations is false for an empty window`() {
        assertFalse(VitalityScoreMath.hasObservations(emptyList(), emptyList(), today, zone))
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun logAt(date: LocalDate, value: Float) = HabitLog(
        habitId = 1L,
        habitName = "Giấc ngủ",
        category = HabitCategory.SLEEP,
        value = value,
        unit = "giờ",
        targetValueAtTime = 8f,
        loggedAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    )

    private fun rowAt(daysAgo: Int, steps: Long = 0L, sleepHours: Double = 0.0) = HealthData(
        steps = steps,
        sleepHours = sleepHours,
        recordedAt = today.minusDays(daysAgo.toLong())
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    )
}
