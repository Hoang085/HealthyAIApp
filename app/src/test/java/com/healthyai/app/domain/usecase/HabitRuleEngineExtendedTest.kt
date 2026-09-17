package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRuleEngineExtendedTest {

    private val engine = HabitRuleEngine()

    @Test
    fun `calculateVitalityScore returns 100 when all habits are 100 percent complete`() {
        val logs = listOf(
            createLog(HabitCategory.SLEEP, 8f, 8f),
            createLog(HabitCategory.WATER, 2000f, 2000f),
            createLog(HabitCategory.EXERCISE, 10000f, 10000f)
        )
        
        val score = engine.calculateVitalityScore(logs)
        assertEquals(100, score)
    }

    @Test
    fun `calculateVitalityScore returns lower score when habits are incomplete`() {
        val logs = listOf(
            createLog(HabitCategory.SLEEP, 4f, 8f),      // 50%
            createLog(HabitCategory.WATER, 1000f, 2000f),  // 50%
            createLog(HabitCategory.EXERCISE, 5000f, 10000f) // 50%
        )
        
        val score = engine.calculateVitalityScore(logs)
        assertEquals(50, score)
    }

    @Test
    fun `generateSuggestions returns critical priority when water intake is very low`() {
        val logs = listOf(
            createLog(HabitCategory.WATER, 500f, 2000f)
        )
        
        val suggestions = engine.generateSuggestions(logs)
        assertTrue(suggestions.any { it.title.contains("nguy hiểm") })
    }

    private fun createLog(category: HabitCategory, value: Float, target: Float): HabitLog {
        return HabitLog(
            habitId = 1L,
            habitName = "Test",
            category = category,
            value = value,
            unit = "Unit",
            targetValueAtTime = target,
            loggedAt = System.currentTimeMillis()
        )
    }
}
