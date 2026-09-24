package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test cho rule **xu hướng ít vận động theo số bước chân** và cho hai **lưới an toàn** đi kèm:
 *
 * `health_data_history` nay được app tự ghi (bước chân lấy từ Health Connect **hoặc** cảm biến của máy),
 * nên có những hàng chỉ có bước chân và `sleepHours = 0` / `heartRate = 0`. Các hàng đó phải được hiểu là
 * **"chưa có dữ liệu"** — nếu không, AI sẽ bắn cảnh báo thiếu ngủ (`0 < 6 giờ`) và kéo trung bình nhịp
 * tim xuống sai.
 */
class HabitRuleEngineStepTrendTest {

    private val engine = HabitRuleEngine()

    // ── Rule mới: mấy ngày liền đi bộ rất ít ────────────────────

    @Test
    fun `alerts when three consecutive days have very few steps`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 1_200L),
            stepRow(daysAgo = 1, steps = 900L),
            stepRow(daysAgo = 2, steps = 2_500L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        val stepTrend = suggestions.find { it.title.contains("đi bộ rất ít") }
        assertTrue("Phải có gợi ý vận động theo bước chân", stepTrend != null)
        assertEquals(SuggestionPriority.MEDIUM, stepTrend?.priority)
        assertEquals(HabitCategory.EXERCISE, stepTrend?.category)
        assertEquals("Ghi nhận vận động", stepTrend?.actionLabel)
    }

    @Test
    fun `does NOT alert when the steps are above the threshold`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 4_000L),
            stepRow(daysAgo = 1, steps = 6_500L),
            stepRow(daysAgo = 2, steps = 3_100L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(suggestions.none { it.title.contains("đi bộ rất ít") })
    }

    @Test
    fun `does NOT alert when the low-step days are not consecutive`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 1_000L),
            stepRow(daysAgo = 2, steps = 1_000L),
            stepRow(daysAgo = 3, steps = 1_000L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(
            "Ngày cách quãng thì không được coi là xu hướng",
            suggestions.none { it.title.contains("đi bộ rất ít") }
        )
    }

    @Test
    fun `does NOT alert when rows have no step data at all`() {
        // Hàng chỉ có giấc ngủ (0 bước) → 0 nghĩa là "chưa có dữ liệu bước chân", không phải "không đi bộ"
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 7.0),
            healthRow(daysAgo = 1, sleepHours = 7.0),
            healthRow(daysAgo = 2, sleepHours = 7.0)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(suggestions.none { it.title.contains("đi bộ rất ít") })
    }

    @Test
    fun `does NOT alert when there are fewer than three days of history`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 500L),
            stepRow(daysAgo = 1, steps = 500L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(suggestions.none { it.title.contains("đi bộ rất ít") })
    }

    // ── Lưới an toàn cho các rule cũ ────────────────────────────

    @Test
    fun `rows without sleep data never trigger the sleep deprivation alert`() {
        // 3 ngày liên tiếp chỉ có bước chân (sleepHours = 0) — trước đây sẽ bị hiểu là "ngủ 0 giờ" → CRITICAL sai
        val history = listOf(
            stepRow(daysAgo = 0, steps = 5_000L),
            stepRow(daysAgo = 1, steps = 5_000L),
            stepRow(daysAgo = 2, steps = 5_000L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(
            "0 giờ ngủ nghĩa là chưa có dữ liệu, không phải thiếu ngủ",
            suggestions.none { it.title.contains("xu hướng thiếu ngủ") }
        )
    }

    @Test
    fun `heart rate average ignores rows without heart rate data`() {
        // 2 ngày có nhịp tim 60 bpm + 5 ngày chỉ có bước chân (không có nhịp tim)
        // → trung bình phải là 60, KHÔNG bị kéo xuống bởi các hàng 0
        val history = listOf(
            healthRow(daysAgo = 0, steps = 5_000L, heartRate = 60.0),
            healthRow(daysAgo = 1, steps = 5_000L, heartRate = 60.0),
            stepRow(daysAgo = 2, steps = 5_000L),
            stepRow(daysAgo = 3, steps = 5_000L),
            stepRow(daysAgo = 4, steps = 5_000L),
            stepRow(daysAgo = 5, steps = 5_000L),
            stepRow(daysAgo = 6, steps = 5_000L)
        )

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(
            "Nhịp tim 60 không được coi là 'hơi cao' chỉ vì có hàng thiếu dữ liệu",
            suggestions.none { it.title.contains("Nhịp tim nghỉ ngơi hơi cao") }
        )
    }

    @Test
    fun `high heart rate still alerts when every day has heart rate data`() {
        val history = (0..6).map { healthRow(daysAgo = it, steps = 5_000L, heartRate = 92.0) }

        val suggestions = engine.generateSuggestions(emptyList(), history)

        assertTrue(suggestions.any { it.title.contains("Nhịp tim nghỉ ngơi hơi cao") })
    }

    // ── Helpers ────────────────────────────────────────────────

    /** Hàng chỉ có **bước chân** (nguồn: cảm biến của máy hoặc Health Connect). */
    private fun stepRow(daysAgo: Int, steps: Long): HealthData = HealthData(
        steps = steps,
        sleepHours = 0.0,
        heartRate = 0.0,
        recordedAt = noonOf(daysAgo)
    )

    /** Hàng đầy đủ (nguồn: Health Connect). */
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
