package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Unit test cho [HabitRuleEngine]: feed bám theo danh mục người dùng, xoay mẹo theo seed, tối đa 4 thẻ ưu tiên giảm dần. */
class HabitRuleEngineTest {

    private lateinit var ruleEngine: HabitRuleEngine

    @Before
    fun setup() {
        ruleEngine = HabitRuleEngine()
    }

    // Feed bám theo danh mục người dùng có

    @Test
    fun `feed covers only the categories the user tracks`() {
        val habits = listOf(waterHabit(), sleepHabit())

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = habits,
            recentLogs = emptyList(),
            rotationSeed = 0
        )

        assertTrue("Phải có gợi ý", suggestions.isNotEmpty())
        assertTrue(
            "Chỉ gợi ý cho danh mục người dùng có thói quen",
            suggestions.all { it.category == HabitCategory.WATER || it.category == HabitCategory.SLEEP }
        )
    }

    @Test
    fun `feed is never empty even when nothing exists yet`() {
        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = emptyList(),
            recentLogs = emptyList(),
            healthHistory = emptyList(),
            rotationSeed = 0
        )

        assertTrue("App mới vẫn phải có gợi ý khởi đầu", suggestions.isNotEmpty())
        assertTrue(suggestions.size <= 4)
    }

    // Chọn nhóm mẹo theo tình trạng danh mục

    @Test
    fun `low water logs surface a concern tip for water`() {
        val logs = (0..2).map { fakeWaterLog(value = 800f, daysAgo = it) }

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = listOf(waterHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertTrue(
            "Nước uống thiếu phải chọn mẹo nhóm CONCERN",
            suggestions.isConcernTipFor(HabitCategory.WATER)
        )
    }

    @Test
    fun `healthy water habit gets a general maintenance tip`() {
        val logs = (0..2).map { fakeWaterLog(value = 2_500f, daysAgo = it) }

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = listOf(waterHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertTrue(
            "Uống đủ nước thì chọn mẹo duy trì (GENERAL)",
            suggestions.isGeneralTipFor(HabitCategory.WATER)
        )
    }

    @Test
    fun `no suggestion uses the old critical warning style`() {
        // Nước 500ml là mức từng bắn thẻ CRITICAL cố định; nay chỉ là mẹo nhóm CONCERN.
        val logs = (0..2).map { fakeWaterLog(value = 500f, daysAgo = it) }

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = listOf(waterHabit()),
            recentLogs = logs,
            rotationSeed = 0
        )

        assertTrue(
            "Nước thiếu vẫn được chú ý (mẹo CONCERN) nhưng không còn thẻ CẢNH BÁO KHẨN CẤP",
            suggestions.isConcernTipFor(HabitCategory.WATER)
        )
        assertTrue(
            "Không còn thẻ CẢNH BÁO KHẨN CẤP cố định",
            suggestions.none { it.priority == SuggestionPriority.CRITICAL }
        )
    }

    // Xoay vòng & giới hạn

    @Test
    fun `rotating the seed changes the tip within a category`() {
        val habits = listOf(sleepHabit())

        val dayA = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 0)
        val dayB = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 1)

        val titleA = dayA.first { it.category == HabitCategory.SLEEP }.title
        val titleB = dayB.first { it.category == HabitCategory.SLEEP }.title
        assertNotEquals("Sang ngày mới (seed khác) mẹo phải đổi để không lặp mãi", titleA, titleB)
    }

    @Test
    fun `the same seed returns the same tips`() {
        val habits = listOf(waterHabit(), exerciseHabit())

        val first = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 3)
        val second = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 3)

        assertEquals(
            "Cùng một ngày (cùng seed) thì feed ổn định",
            first.map { it.title },
            second.map { it.title }
        )
    }

    @Test
    fun `feed is capped at four suggestions`() {
        val habits = HabitCategory.entries.map { habitOf(it) }

        val suggestions = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 0)

        assertEquals(4, suggestions.size)
    }

    @Test
    fun `feed does not repeat the same tip twice`() {
        val habits = HabitCategory.entries.map { habitOf(it) }

        val suggestions = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 0)

        assertEquals(
            "Mỗi thẻ là một mẹo khác nhau",
            suggestions.size,
            suggestions.map { it.title }.distinct().size
        )
    }

    @Test
    fun `rotating the seed walks the whole tip pool before repeating`() {
        // Danh mục không có log ⇒ không bị "flagged" ⇒ dùng kho mẹo GENERAL của riêng nó.
        val habits = listOf(waterHabit())
        val poolSize = SuggestionCatalog.generalTips(HabitCategory.WATER).size

        val titlePerSeed = (0 until poolSize).map { seed ->
            ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = seed)
                .first { it.category == HabitCategory.WATER }.title
        }

        assertEquals(
            "Mỗi ngày phải ra một mẹo KHÁC — chỉ lặp lại sau khi đã đi hết kho mẹo",
            poolSize,
            titlePerSeed.distinct().size
        )
        assertEquals(
            "Hết một vòng xoay thì quay lại mẹo đầu tiên",
            titlePerSeed.first(),
            ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = poolSize)
                .first { it.category == HabitCategory.WATER }.title
        )
    }

    @Test
    fun `every tip of a category is reachable within one rotation cycle`() {
        HabitCategory.entries.forEach { category ->
            val pool = SuggestionCatalog.generalTips(category).map { it.title }.toSet()
            val habits = listOf(habitOf(category))

            val shown = (0 until pool.size).map { seed ->
                ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = seed)
                    .first { it.category == category }.title
            }.toSet()

            assertEquals(
                "${category.displayName}: trong một vòng xoay phải phủ HẾT kho mẹo (không mẹo nào bị bỏ quên)",
                pool,
                shown
            )
        }
    }

    // Thứ tự & khoá duy nhất của thẻ

    @Test
    fun `suggestions are ordered by priority descending`() {
        val habits = listOf(waterHabit(), sleepHabit(), nutritionHabit())
        val logs = (0..2).map { fakeWaterLog(value = 500f, daysAgo = it) } // WATER → CONCERN (HIGH)

        val suggestions = ruleEngine.generateSuggestions(habits, logs, rotationSeed = 0)

        assertTrue(suggestions.size >= 2)
        assertTrue(
            "Thẻ ưu tiên cao hơn phải nằm trên",
            suggestions.zipWithNext().all { (a, b) -> a.priority.ordinal >= b.priority.ordinal }
        )
        assertEquals(HabitCategory.WATER, suggestions.first().category)
    }

    @Test
    fun `suggestion ids are unique so list keys do not collide`() {
        val habits = HabitCategory.entries.map { habitOf(it) }

        val suggestions = ruleEngine.generateSuggestions(habits, emptyList(), rotationSeed = 2)

        assertEquals(suggestions.size, suggestions.map { it.id }.distinct().size)
    }

    // Từ dữ liệu sức khỏe cứng

    @Test
    fun `three consecutive low step days flag exercise even without a step habit`() {
        val history = listOf(
            stepRow(daysAgo = 0, steps = 1_200L),
            stepRow(daysAgo = 1, steps = 900L),
            stepRow(daysAgo = 2, steps = 2_500L)
        )

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = emptyList(),
            recentLogs = emptyList(),
            healthHistory = history,
            rotationSeed = 0
        )

        assertTrue(suggestions.isConcernTipFor(HabitCategory.EXERCISE))
    }

    @Test
    fun `high resting heart rate flags mindfulness`() {
        val history = (0..6).map { healthRow(daysAgo = it, sleepHours = 7.5, heartRate = 90.0) }

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = emptyList(),
            recentLogs = emptyList(),
            healthHistory = history,
            rotationSeed = 0
        )

        assertTrue(suggestions.isConcernTipFor(HabitCategory.MINDFULNESS))
    }

    @Test
    fun `step rows without step data never flag exercise`() {
        // Hàng chỉ có giờ ngủ (0 bước) → 0 nghĩa là "chưa có dữ liệu bước chân".
        val history = (0..2).map { healthRow(daysAgo = it, sleepHours = 7.0) }

        val suggestions = ruleEngine.generateSuggestions(
            activeHabits = emptyList(),
            recentLogs = emptyList(),
            healthHistory = history,
            rotationSeed = 0
        )

        assertFalse(
            suggestions.isConcernTipFor(HabitCategory.EXERCISE)
        )
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

    private fun habitOf(category: HabitCategory) = Habit(
        id = category.ordinal.toLong() + 1,
        name = category.displayName,
        category = category,
        targetValue = 1f,
        unit = "lần"
    )

    private fun waterHabit() = habitOf(HabitCategory.WATER)
    private fun sleepHabit() = habitOf(HabitCategory.SLEEP)
    private fun exerciseHabit() = habitOf(HabitCategory.EXERCISE)
    private fun nutritionHabit() = habitOf(HabitCategory.NUTRITION)

    private fun fakeWaterLog(value: Float, daysAgo: Int = 0) = HabitLog(
        habitId = 1L, habitName = "Uống nước",
        category = HabitCategory.WATER, value = value, unit = "ml",
        targetValueAtTime = 2000f,
        loggedAt = System.currentTimeMillis() - (daysAgo * 86_400_000L)
    )

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
