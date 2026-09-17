package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRuleEnginePersonalizedTest {

    private val engine = HabitRuleEngine()

    @Test
    fun `analyzeHealthTrends detects 3 days of sleep deprivation`() {
        // Arrange
        val history = listOf(
            HealthData(sleepHours = 5.5),
            HealthData(sleepHours = 4.0),
            HealthData(sleepHours = 5.0)
        )

        // Act
        val suggestions = engine.generateSuggestions(emptyList(), history)

        // Assert
        val sleepDeprivation = suggestions.find { it.title.contains("xu hướng thiếu ngủ") }
        assertTrue(sleepDeprivation != null)
        assertEquals(SuggestionPriority.CRITICAL, sleepDeprivation?.priority)
    }

    @Test
    fun `analyzeHealthTrends detects high resting heart rate`() {
        // Arrange
        val history = List(7) { HealthData(heartRate = 90.0) }

        // Act
        val suggestions = engine.generateSuggestions(emptyList(), history)

        // Assert
        val heartRateSuggestion = suggestions.find { it.title.contains("Nhịp tim nghỉ ngơi hơi cao") }
        assertTrue(heartRateSuggestion != null)
        assertEquals(SuggestionPriority.MEDIUM, heartRateSuggestion?.priority)
    }

    @Test
    fun `generateSuggestions combines logs and health trends correctly`() {
        // Arrange
        val logs = listOf(
            HabitLog(
                habitId = 1L, habitName = "Nước", category = HabitCategory.WATER,
                value = 500f, unit = "ml", targetValueAtTime = 2000f
            )
        )
        val history = listOf(HealthData(sleepHours = 4.0), HealthData(sleepHours = 4.0), HealthData(sleepHours = 4.0))

        // Act
        val suggestions = engine.generateSuggestions(logs, history)

        // Assert
        // Phải có cả gợi ý về nước (Critical/High) và thiếu ngủ (Critical)
        assertTrue(suggestions.any { it.category == HabitCategory.WATER })
        assertTrue(suggestions.any { it.category == HabitCategory.SLEEP })
    }
}
