package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.Suggestion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test cho nhánh đọc **dữ liệu sức khỏe cứng** của [HabitRuleEngine] (phụ lục C #16).
 *
 * `health_data_history` ghi 1 hàng cho mỗi lần sync (6 giờ/lần) chứ không phải 1 hàng/ngày, nên các
 * test ở đây dựng dữ liệu theo **ngày** (mốc 12:00 mỗi ngày) và kiểm tra rule gom theo ngày.
 *
 * Sau bản cải tiến feed gợi ý, "phát hiện xu hướng" được quan sát qua việc engine **chọn mẹo nhóm
 * CONCERN** cho danh mục tương ứng (thay vì dựng một thẻ cảnh báo cố định).
 */
class HabitRuleEnginePersonalizedTest {

    private val engine = HabitRuleEngine()

    @Test
    fun `three consecutive short sleep days flag sleep`() {
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.5),
            healthRow(daysAgo = 1, sleepHours = 4.0),
            healthRow(daysAgo = 2, sleepHours = 5.0)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertTrue(suggestions.isConcernTipFor(HabitCategory.SLEEP))
    }

    @Test
    fun `rows from the same day never flag sleep`() {
        // 3 lần sync trong CÙNG một ngày (6h/12h/18h) đều ngủ < 6 giờ.
        // Trước bản sửa #16, 3 hàng này bị hiểu nhầm là "3 ngày liên tiếp".
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 6),
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 12),
            healthRow(daysAgo = 0, sleepHours = 5.0, hourOfDay = 18)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(
            "1 ngày dữ liệu không được coi là xu hướng 3 ngày",
            suggestions.isConcernTipFor(HabitCategory.SLEEP)
        )
    }

    @Test
    fun `non consecutive days never flag sleep`() {
        val history = listOf(
            healthRow(daysAgo = 0, sleepHours = 5.0),
            healthRow(daysAgo = 2, sleepHours = 5.0),
            healthRow(daysAgo = 3, sleepHours = 5.0)
        )

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.SLEEP))
    }

    @Test
    fun `rows without sleep data never flag sleep`() {
        // 3 ngày liên tiếp chỉ có bước chân (sleepHours = 0) → "chưa có dữ liệu", không phải "ngủ 0 giờ".
        val history = (0..2).map { healthRow(daysAgo = it, steps = 5_000L) }

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.SLEEP))
    }

    @Test
    fun `seven days of high heart rate flag mindfulness`() {
        val history = (0..6).map { healthRow(daysAgo = it, sleepHours = 7.5, heartRate = 90.0) }

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertTrue(suggestions.isConcernTipFor(HabitCategory.MINDFULNESS))
    }

    @Test
    fun `heart rate average ignores rows without heart rate data`() {
        // 2 ngày có nhịp tim 60 bpm + 5 ngày chỉ có bước chân → trung bình phải là 60, KHÔNG bị kéo
        // xuống bởi các hàng 0.
        val history = listOf(
            healthRow(daysAgo = 0, steps = 5_000L, heartRate = 60.0),
            healthRow(daysAgo = 1, steps = 5_000L, heartRate = 60.0)
        ) + (2..6).map { healthRow(daysAgo = it, steps = 5_000L) }

        val suggestions = engine.generateSuggestions(emptyList(), emptyList(), history, rotationSeed = 0)

        assertFalse(suggestions.isConcernTipFor(HabitCategory.MINDFULNESS))
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun List<Suggestion>.isConcernTipFor(category: HabitCategory): Boolean =
        any { suggestion ->
            suggestion.category == category &&
                SuggestionCatalog.concernTips(category).any { it.title == suggestion.title }
        }

    private fun healthRow(
        daysAgo: Int,
        steps: Long = 0L,
        sleepHours: Double = 0.0,
        heartRate: Double = 0.0,
        hourOfDay: Int = 12
    ): HealthData = HealthData(
        steps = steps,
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
