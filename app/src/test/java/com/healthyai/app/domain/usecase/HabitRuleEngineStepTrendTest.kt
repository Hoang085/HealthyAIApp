package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Test rule xu hướng ít vận động theo bước chân; hàng chỉ có bước chân hoặc chỉ có ngủ là "chưa có dữ liệu". */
class HabitRuleEngineStepTrendTest {

    private val engine = HabitRuleEngine()

    // Rule: mấy ngày liền đi bộ rất ít

    @Test
    fun `three consecutive low step days flag exercise`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 1_200L),
            stepRow(daysAgo = 1, steps = 900L),
            stepRow(daysAgo = 2, steps = 2_500L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertTrue(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `steps above the threshold do not flag exercise`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 4_000L),
            stepRow(daysAgo = 1, steps = 6_500L),
            stepRow(daysAgo = 2, steps = 3_100L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `low step days that are not consecutive do not flag exercise`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 1_000L),
            stepRow(daysAgo = 2, steps = 1_000L),
            stepRow(daysAgo = 3, steps = 1_000L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(
            "Ngày cách quãng thì không được coi là xu hướng",
            suggestions.isConcernTipFor(HabitCategory.EXERCISE)
        )
    }

    @Test
    fun `rows without step data do not flag exercise`() {
        // Hàng chỉ có giấc ngủ (0 bước) → 0 nghĩa là "chưa có dữ liệu bước chân", không phải "không đi bộ".
        val history = (0..2).map { healthRow(daysAgo = it, sleepHours = 7.0) }

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `fewer than three days of history do not flag exercise`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 500L),
            stepRow(daysAgo = 1, steps = 500L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `concern tips carry a higher priority than maintenance tips`() {
        val history = (0..2).map { stepRow(daysAgo = it, steps = 500L) }

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        val exercise = suggestions.first { it.category == HabitCategory.EXERCISE }
        assertTrue(
            "Mẹo cho danh mục đang có vấn đề phải nổi hơn mẹo duy trì",
            exercise.priority.ordinal >= SuggestionPriority.MEDIUM.ordinal
        )
        assertEquals(HabitCategory.EXERCISE, suggestions.first().category)
    }

    // Helpers

    private fun List<Suggestion>.isConcernTipFor(category: HabitCategory): Boolean =
        any { suggestion ->
            suggestion.category == category &&
                SuggestionCatalog.concernTips(category).any { it.title == suggestion.title }
        }

    private fun stepRow(daysAgo: Int, steps: Long): HealthData = HealthData(
        steps = steps,
        sleepHours = 0.0,
        heartRate = 0.0,
        recordedAt = noonOf(daysAgo)
    )

    private fun healthRow(
        daysAgo: Int,
        steps: Long = 0L,
        sleepHours: Double = 0.0,
        heartRate: Double = 0.0
    ): HealthData = HealthData(
        steps = steps,
        sleepHours = sleepHours,
        heartRate = heartRate,
        recordedAt = noonOf(daysAgo)
    )

    private fun noonOf(daysAgo: Int): Long = LocalDate.now()
        .minusDays(daysAgo.toLong())
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
