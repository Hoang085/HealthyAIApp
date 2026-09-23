package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test cho HabitRuleEngine — trái tim của tính năng AI on-device.
 *
 * Chiến lược test:
 * - Dữ liệu giả (fake data) không cần Room, không cần thiết bị thật
 * - Chạy trong <1ms mỗi test vì không có I/O, không có network
 * - Dễ thêm test case mới khi thêm rule mới
 */
class HabitRuleEngineTest {

    private lateinit var ruleEngine: HabitRuleEngine

    @Before
    fun setup() {
        ruleEngine = HabitRuleEngine()
    }

    // ── SLEEP TESTS ────────────────────────────────────────────

    @Test
    fun `should generate HIGH priority sleep warning when sleeping less than 6 hours for 3 days`() {
        val logs = listOf(
            fakeSleepLog(value = 4.5f, daysAgo = 0),
            fakeSleepLog(value = 5.0f, daysAgo = 1),
            fakeSleepLog(value = 5.5f, daysAgo = 2),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)

        assertTrue("Should have sleep suggestion", suggestions.any { it.category == HabitCategory.SLEEP })
        val sleepSug = suggestions.first { it.category == HabitCategory.SLEEP }
        assertEquals(SuggestionPriority.HIGH, sleepSug.priority)
    }

    @Test
    fun `should NOT generate sleep warning when sleeping enough (8 hours)`() {
        val logs = listOf(
            fakeSleepLog(value = 8f, daysAgo = 0),
            fakeSleepLog(value = 7.5f, daysAgo = 1),
            fakeSleepLog(value = 8f, daysAgo = 2),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        assertTrue("Should have no HIGH sleep warning", suggestions.none { it.category == HabitCategory.SLEEP && it.priority == SuggestionPriority.HIGH })
    }

    @Test
    fun `should generate MEDIUM sleep warning when average sleep below 7 hours`() {
        val logs = listOf(
            fakeSleepLog(value = 6.5f, daysAgo = 0),
            fakeSleepLog(value = 6.0f, daysAgo = 1),
            fakeSleepLog(value = 6.5f, daysAgo = 2),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        val sleepSug = suggestions.firstOrNull { it.category == HabitCategory.SLEEP }
        assertNotNull("Should have sleep suggestion", sleepSug)
        assertEquals(SuggestionPriority.MEDIUM, sleepSug!!.priority)
    }

    // ── WATER TESTS ────────────────────────────────────────────

    @Test
    fun `should generate CRITICAL water warning when average intake below 1000ml`() {
        val logs = listOf(
            fakeWaterLog(value = 500f, daysAgo = 0),
            fakeWaterLog(value = 800f, daysAgo = 1),
            fakeWaterLog(value = 600f, daysAgo = 2),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        val waterSug = suggestions.first { it.category == HabitCategory.WATER }
        assertEquals(SuggestionPriority.CRITICAL, waterSug.priority)
    }

    @Test
    fun `should generate MEDIUM water warning when average intake between 1000-1500ml`() {
        val logs = listOf(
            fakeWaterLog(value = 1200f, daysAgo = 0),
            fakeWaterLog(value = 1300f, daysAgo = 1),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        val waterSug = suggestions.first { it.category == HabitCategory.WATER }
        assertEquals(SuggestionPriority.MEDIUM, waterSug.priority)
    }

    @Test
    fun `should NOT generate water warning when drinking enough (2000ml+)`() {
        val logs = listOf(
            fakeWaterLog(value = 2000f, daysAgo = 0),
            fakeWaterLog(value = 2200f, daysAgo = 1),
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        assertTrue("Should have no water warning", suggestions.none { it.category == HabitCategory.WATER })
    }

    // ── EXERCISE TESTS ─────────────────────────────────────────

    @Test
    fun `should generate HIGH exercise warning when steps below 5000 for 5 days`() {
        val logs = (0..4).map { fakeExerciseLog(value = 3000f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)
        val exerciseSug = suggestions.firstOrNull { it.category == HabitCategory.EXERCISE }
        assertNotNull("Should have exercise suggestion", exerciseSug)
        assertEquals(SuggestionPriority.HIGH, exerciseSug!!.priority)
    }

    @Test
    fun `should NOT generate HIGH exercise warning when steps above 5000`() {
        val logs = (0..4).map { fakeExerciseLog(value = 6000f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)
        assertTrue("Should have no HIGH exercise warning",
            suggestions.none { it.category == HabitCategory.EXERCISE && it.priority == SuggestionPriority.HIGH })
    }

    // ── EXERCISE TESTS: đơn vị KHÔNG phải bước (phụ lục C #17) ──────────────────────────
    // Trước đây rule so `value` với 5000/8000 bất kể đơn vị, nên thói quen "Chạy bộ 30 phút" ghi
    // 10 phút bị đọc thành "10 bước" và sinh gợi ý vô nghĩa.

    @Test
    fun `should generate HIGH exercise warning when minutes are below half of the habit target`() {
        // 10 phút / mục tiêu 30 phút = 33% → dưới 50% → HIGH
        val logs = (0..4).map { fakeExerciseMinutesLog(value = 10f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)

        val exerciseSug = suggestions.firstOrNull { it.category == HabitCategory.EXERCISE }
        assertNotNull("Should have exercise suggestion", exerciseSug)
        assertEquals(SuggestionPriority.HIGH, exerciseSug!!.priority)
        assertTrue("Nội dung phải nói theo đơn vị phút, không phải bước chân",
            exerciseSug.message.contains("10 phút") && !exerciseSug.message.contains("bước"))
    }

    @Test
    fun `should NOT generate exercise warning when minutes reach the habit target`() {
        // Ghi đủ 30/30 phút: giá trị 30 < 5000 nên bản cũ vẫn bắn gợi ý HIGH sai
        val logs = (0..4).map { fakeExerciseMinutesLog(value = 30f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)

        assertTrue("Đạt đủ mục tiêu thì không có gợi ý vận động",
            suggestions.none { it.category == HabitCategory.EXERCISE })
    }

    @Test
    fun `should generate LOW exercise warning when minutes are slightly below target`() {
        // 21/30 phút = 70% → từ 50% đến dưới 80% → LOW
        val logs = (0..4).map { fakeExerciseMinutesLog(value = 21f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)

        val exerciseSug = suggestions.firstOrNull { it.category == HabitCategory.EXERCISE }
        assertNotNull("Should have exercise suggestion", exerciseSug)
        assertEquals(SuggestionPriority.LOW, exerciseSug!!.priority)
    }

    @Test
    fun `should NOT generate exercise warning when there are fewer than 3 logs`() {
        // Chưa đủ dữ liệu để nói "xu hướng" khi so theo mục tiêu của thói quen
        val logs = (0..1).map { fakeExerciseMinutesLog(value = 1f, daysAgo = it) }
        val suggestions = ruleEngine.generateSuggestions(logs)

        assertTrue("2 lần ghi chưa đủ để kết luận xu hướng",
            suggestions.none { it.category == HabitCategory.EXERCISE })
    }

    // ── PRIORITY ORDERING ──────────────────────────────────────

    @Test
    fun `suggestions should be sorted by priority descending`() {
        val logs = listOf(
            fakeWaterLog(value = 500f, daysAgo = 0),   // → CRITICAL
            fakeSleepLog(value = 4f, daysAgo = 0),
            fakeSleepLog(value = 4f, daysAgo = 1),
            fakeSleepLog(value = 4f, daysAgo = 2),      // → HIGH
        )
        val suggestions = ruleEngine.generateSuggestions(logs)
        if (suggestions.size > 1) {
            assertTrue("First suggestion should have highest priority",
                suggestions[0].priority.ordinal >= suggestions[1].priority.ordinal)
        }
    }

    // ── Empty data ─────────────────────────────────────────────

    @Test
    fun `should return empty suggestions when no logs available`() {
        val suggestions = ruleEngine.generateSuggestions(emptyList())
        assertTrue("Should return empty list", suggestions.isEmpty())
    }

    // ── Helper factory functions ───────────────────────────────

    private fun fakeSleepLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 1L, habitName = "Giấc ngủ",
        category = HabitCategory.SLEEP, value = value, unit = "giờ",
        targetValueAtTime = 8f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    private fun fakeWaterLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 2L, habitName = "Uống nước",
        category = HabitCategory.WATER, value = value, unit = "ml",
        targetValueAtTime = 2000f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    private fun fakeExerciseLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 3L, habitName = "Vận động",
        category = HabitCategory.EXERCISE, value = value, unit = "bước",
        targetValueAtTime = 10000f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    /** Thói quen vận động đo bằng **phút** (Data set A: "Chạy bộ — 30 phút"). */
    private fun fakeExerciseMinutesLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 4L, habitName = "Chạy bộ",
        category = HabitCategory.EXERCISE, value = value, unit = "phút",
        targetValueAtTime = 30f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )
}
