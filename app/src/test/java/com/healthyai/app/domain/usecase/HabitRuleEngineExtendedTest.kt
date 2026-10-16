package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test nhánh phân tích theo đơn vị của chính thói quen và việc không còn thẻ cảnh báo cố định. */
class HabitRuleEngineExtendedTest {

    private val engine = HabitRuleEngine()

    @Test
    fun `minutes far below the habit target flag exercise`() {
        // 10 phút / mục tiêu 30 phút = 33% → dưới 80% → danh mục Vận động "cần chú ý"
        val logs = (0..4).map { runLogMinutes(value = 10f, daysAgo = it) }

        val suggestions = engine.generateSuggestions(
            activeHabits = listOf(runHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertTrue("Phải chọn mẹo CONCERN cho Vận động", suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `minutes reaching the habit target are not flagged`() {
        // Ghi đủ 30/30 phút → không còn là "cần chú ý" (bản cũ vẫn bắn cảnh báo vì 30 < 5000)
        val logs = (0..4).map { runLogMinutes(value = 30f, daysAgo = it) }

        val suggestions = engine.generateSuggestions(
            activeHabits = listOf(runHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertTrue("Đạt mục tiêu thì dùng mẹo duy trì", suggestions.isGeneralTipFor(HabitCategory.EXERCISE))
        assertFalse(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `fewer than three logs are not treated as a trend`() {
        val logs = (0..1).map { runLogMinutes(value = 1f, daysAgo = it) }

        val suggestions = engine.generateSuggestions(
            activeHabits = listOf(runHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertFalse("2 lần ghi chưa đủ để kết luận xu hướng", suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `feed never contains a critical card`() {
        val habits = HabitCategory.entries.map {
            Habit(id = it.ordinal.toLong() + 1, name = it.displayName, category = it, targetValue = 2000f, unit = "ml")
        }
        val logs = (0..4).map { waterLog(value = 200f, daysAgo = it) }

        val suggestions = engine.generateSuggestions(habits, logs, rotationSeed = 1)

        assertTrue(suggestions.none { it.priority == SuggestionPriority.CRITICAL })
    }

    // Helpers

    private fun List<Suggestion>.isConcernTipFor(category: HabitCategory): Boolean =
        any { suggestion ->
            suggestion.category == category &&
                SuggestionCatalog.concernTips(category).any { it.title == suggestion.title }
        }

    private fun List<Suggestion>.isGeneralTipFor(category: HabitCategory): Boolean =
        any { suggestion ->
            suggestion.category == category &&
                SuggestionCatalog.generalTips(category).any { it.title == suggestion.title }
        }

    /** Thói quen vận động đo bằng phút. */
    private fun runHabit() = Habit(
        id = 4L,
        name = "Chạy bộ",
        category = HabitCategory.EXERCISE,
        targetValue = 30f,
        unit = "phút"
    )

    private fun runLogMinutes(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 4L, habitName = "Chạy bộ",
        category = HabitCategory.EXERCISE, value = value, unit = "phút",
        targetValueAtTime = 30f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

    private fun waterLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 2L, habitName = "Uống nước",
        category = HabitCategory.WATER, value = value, unit = "ml",
        targetValueAtTime = 2000f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )
}
