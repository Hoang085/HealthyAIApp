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
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    private fun fakeWaterLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 2L, habitName = "Uống nước",
        category = HabitCategory.WATER, value = value, unit = "ml",
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    private fun fakeExerciseLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 3L, habitName = "Vận động",
        category = HabitCategory.EXERCISE, value = value, unit = "bước",
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )
}
