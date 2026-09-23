package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test cho nhánh phân tích **dữ liệu Health Connect** của [HabitRuleEngine] (phụ lục C #16).
 *
 * `health_data_history` ghi 1 hàng cho mỗi lần sync (6 giờ/lần) chứ không phải 1 hàng/ngày, nên
 * các test ở đây dựng dữ liệu theo **ngày** (mốc 12:00 mỗi ngày) và kiểm tra rule gom theo ngày.
 */
class HabitRuleEnginePersonalizedTest {

    private val engine = HabitRuleEngine()

    @Test
    fun `analyzeHealthTrends detects 3 days of sleep deprivation`() {
        // Arrange — 3 ngày liên tiếp, mỗi ngày 1 hàng (đều ngủ < 6 giờ)
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.5),
            healthRow(daysAgo = 1, sleepHours = 4.0),
            healthRow(daysAgo = 2, sleepHours = 5.0)
        )

        // Act
        val suggestions = engine.generateSuggestions(emptyList(), history)

        // Assert
        val sleepDeprivation = suggestions.find { it.title.contains("xu hướng thiếu ngủ") }
        assertTrue(sleepDeprivation != null)
        assertEquals(SuggestionPriority.CRITICAL, sleepDeprivation?.priority)
    }

    @Test
    fun `analyzeHealthTrends does NOT alert sleep deprivation when rows are from the same day`() {
        // Arrange — 3 lần sync trong CÙNG một ngày (6h/12h/18h) và đều ngủ < 6 giờ.
        // Trước bản sửa #16, 3 hàng này bị hiểu nhầm là "3 ngày liên tiếp" → bắn CRITICAL sai.
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 6),
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 12),
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 18)
        )

        // Act
        val suggestions = engine.generateSuggestions(emptyList(), history)

        // Assert
        assertTrue(
            "1 ngày dữ liệu không được coi là xu hướng 3 ngày",
            suggestions.none { it.title.contains("xu hướng thiếu ngủ") }
        )
    }

    @Test
    fun `analyzeHealthTrends does NOT alert sleep deprivation when days are not consecutive`() {
        // Arrange — ngày gần nhất và 2 ngày có dữ liệu trước đó bị cách quãng (thiếu hôm qua)
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.0),
            healthRow(daysAgo = 2, sleepHours = 5.0),
            healthRow(daysAgo = 3, sleepHours = 5.0)
        )

        // Act
        val suggestions = engine.generateSuggestions(emptyList(), history)

        // Assert
        assertTrue(
            "3 ngày không liên tiếp thì không được cảnh báo '3 ngày liên tiếp'",
            suggestions.none { it.title.contains("xu hướng thiếu ngủ") }
        )
    }

    @Test
    fun `analyzeHealthTrends detects high resting heart rate`() {
        // Arrange — 7 NGÀY (không phải 7 lần sync) đều có nhịp tim 90 bpm; ngủ đủ để không lẫn
        // với cảnh báo thiếu ngủ.
        val history = (0..6).map { healthRow(daysAgo = it, sleepHours = 7.5, heartRate = 90.0) }

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
        val history = (0..2).map { healthRow(daysAgo = it, sleepHours = 4.0) }

        // Act
        val suggestions = engine.generateSuggestions(logs, history)

        // Assert
        // Phải có cả gợi ý về nước (Critical/High) và thiếu ngủ (Critical)
        assertTrue(suggestions.any { it.category == HabitCategory.WATER })
        assertTrue(suggestions.any { it.category == HabitCategory.SLEEP })
    }

    /**
     * Một hàng `health_data_history`: mốc **12:00 (giờ thiết bị)** của ngày cách hôm nay
     * [daysAgo] ngày → `recordedAt` luôn thuộc đúng ngày mong muốn (không lệch vì DST như khi trừ
     * thẳng 24 giờ).
     */
    private fun healthRow(
        daysAgo: Int,
        sleepHours: Double = 0.0,
        heartRate: Double = 0.0,
        hourOfDay: Int = 12
    ): HealthData = HealthData(
        sleepHours = sleepHours,
        heartRate = heartRate,
        recordedAt = LocalDate.now()
            .minusDays(daysAgo.toLong())
            .atTime(hourOfDay, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
}
