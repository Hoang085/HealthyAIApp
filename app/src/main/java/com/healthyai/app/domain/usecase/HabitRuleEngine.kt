package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.Suggestion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Engine gợi ý thói quen chạy offline (rule-based), chọn mẹo xoay vòng theo ngày. */
@Singleton
class HabitRuleEngine @Inject constructor() {

    /** Sinh danh sách gợi ý cho tab Phân tích; rotationSeed đổi mẹo xoay vòng theo ngày. */
    fun generateSuggestions(
        activeHabits: List<Habit>,
        recentLogs: List<HabitLog>,
        healthHistory: List<HealthData> = emptyList(),
        rotationSeed: Int = defaultRotationSeed()
    ): List<Suggestion> {
        val flagged = flaggedCategories(recentLogs, healthHistory)

        // Danh mục gợi ý = thói quen người dùng ∪ danh mục có vấn đề; nếu trống thì mở toàn bộ kho mẹo.
        val userCategories = activeHabits.map { it.category }.toSet()
        val relevant = SuggestionCatalog.categories.filter { it in userCategories || it in flagged }
            .ifEmpty { SuggestionCatalog.categories }

        // Danh mục đang có vấn đề lên đầu để lời khuyên sát vấn đề hiện tại hơn.
        val ordered = relevant.sortedByDescending { it in flagged }

        return ordered.mapIndexed { offset, category ->
            val focus = if (category in flagged) {
                SuggestionCatalog.TipFocus.CONCERN
            } else {
                SuggestionCatalog.TipFocus.GENERAL
            }
            val pool = if (focus == SuggestionCatalog.TipFocus.CONCERN) {
                SuggestionCatalog.concernTips(category)
            } else {
                SuggestionCatalog.generalTips(category)
            }
            val tipIndex = Math.floorMod(rotationSeed + offset, pool.size)
            pool[tipIndex].toSuggestion(id = category.ordinal * 100L + tipIndex)
        }
            .sortedByDescending { it.priority.ordinal }
            .take(MAX_SUGGESTIONS)
    }

    /** Các danh mục đang cần chú ý — chỉ để chọn nhóm mẹo, không dựng thẻ cảnh báo. */
    private fun flaggedCategories(
        recentLogs: List<HabitLog>,
        healthHistory: List<HealthData>
    ): Set<HabitCategory> {
        val flagged = mutableSetOf<HabitCategory>()

        // Từ nhật ký thói quen
        val recentSleep = recentLogs.filter { it.category == HabitCategory.SLEEP }
            .sortedByDescending { it.loggedAt }
            .take(7)
        if (recentSleep.size >= MIN_LOGS_FOR_TREND && recentSleep.averageOf { it.value.toDouble() } < SLEEP_ENOUGH_HOURS) {
            flagged += HabitCategory.SLEEP
        }

        val recentWater = recentLogs.filter { it.category == HabitCategory.WATER }
            .sortedByDescending { it.loggedAt }
            .take(7)
        if (recentWater.size >= MIN_LOGS_FOR_TREND && recentWater.averageOf { it.value.toDouble() } < WATER_ENOUGH_ML) {
            flagged += HabitCategory.WATER
        }

        if (isExerciseBelowTarget(recentLogs)) flagged += HabitCategory.EXERCISE

        // Từ dữ liệu sức khỏe cứng (Health Connect / cảm biến máy)
        val dailyHistory = latestRowPerDay(healthHistory)

        val recent3Sleep = dailyHistory.take(3)
        if (recent3Sleep.size >= 3 &&
            areConsecutiveDays(recent3Sleep.map { it.localDate() }) &&
            recent3Sleep.all { it.sleepHours > 0.0 && it.sleepHours < SLEEP_DEPRIVED_HOURS }
        ) {
            flagged += HabitCategory.SLEEP
        }

        val recentSteps = dailyHistory.take(STEP_TREND_DAYS)
        if (recentSteps.size >= STEP_TREND_DAYS &&
            areConsecutiveDays(recentSteps.map { it.localDate() }) &&
            recentSteps.all { it.steps > 0L && it.steps < STEP_TREND_LOW }
        ) {
            flagged += HabitCategory.EXERCISE
        }

        val heartRateRows = dailyHistory.take(7).filter { it.heartRate > 0.0 }
        if (heartRateRows.isNotEmpty() &&
            heartRateRows.averageOf { it.heartRate } > HEART_RATE_HIGH_BPM
        ) {
            flagged += HabitCategory.MINDFULNESS
        }

        return flagged
    }

    /** Vận động dưới mục tiêu: log "bước" so ngưỡng WHO, đơn vị khác so tỉ lệ value/targetValueAtTime. */
    private fun isExerciseBelowTarget(exerciseLogs: List<HabitLog>): Boolean {
        val byNewestFirst = exerciseLogs.filter { it.category == HabitCategory.EXERCISE }
            .sortedByDescending { it.loggedAt }

        val stepLogs = byNewestFirst.filter { it.isStepUnit() }
        val recent5Steps = stepLogs.take(5)
        if (recent5Steps.size >= 5 && recent5Steps.all { it.value < STEP_DAILY_LOW }) return true
        val recent7Steps = stepLogs.take(7)
        if (recent7Steps.size >= 5 && recent7Steps.averageOf { it.value.toDouble() } < STEP_DAILY_RECOMMENDED) {
            return true
        }

        val targetLogs = byNewestFirst.filterNot { it.isStepUnit() }
            .filter { it.targetValueAtTime > 0f }
            .take(5)
        if (targetLogs.size >= MIN_LOGS_FOR_TREND) {
            val avgRatio = targetLogs.averageOf {
                (it.value / it.targetValueAtTime).toDouble().coerceIn(0.0, 1.0)
            }
            if (avgRatio < TARGET_COMPLETION_MEDIUM) return true
        }

        return false
    }

    /** Mỗi ngày chỉ giữ hàng mới nhất (đầy đủ dữ liệu nhất) vì history ghi 1 hàng mỗi lần sync. */
    private fun latestRowPerDay(history: List<HealthData>): List<HealthData> =
        history.groupBy { it.localDate() }
            .map { (_, rowsInDay) -> rowsInDay.maxByOrNull { it.recordedAt }!! }
            .sortedByDescending { it.recordedAt }

    /** [days] phải theo thứ tự mới → cũ và cách nhau đúng 1 ngày (ba ngày liên tiếp). */
    private fun areConsecutiveDays(days: List<LocalDate>): Boolean =
        days.zipWithNext().all { (newer, older) -> ChronoUnit.DAYS.between(older, newer) == 1L }

    /** Ngày (theo múi giờ thiết bị) mà dữ liệu sức khỏe được ghi nhận. */
    private fun HealthData.localDate(): LocalDate =
        Instant.ofEpochMilli(recordedAt).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Đơn vị của log có phải "bước" không (ngưỡng 5000/8000 chỉ có nghĩa với đơn vị này). */
    private fun HabitLog.isStepUnit(): Boolean =
        unit.trim().lowercase() in STEP_UNITS

    private fun SuggestionCatalog.Tip.toSuggestion(id: Long): Suggestion = Suggestion(
        id = id,
        title = title,
        message = message,
        priority = priority,
        category = category,
        actionLabel = actionLabel
    )

    private fun <T> List<T>.averageOf(selector: (T) -> Double): Double {
        if (isEmpty()) return 0.0
        return sumOf(selector) / size
    }

    private companion object {
        /** Số thẻ gợi ý tối đa hiển thị — đủ đa dạng mà không ngợp. */
        const val MAX_SUGGESTIONS = 4

        /** Số lần ghi tối thiểu để coi là "xu hướng". */
        const val MIN_LOGS_FOR_TREND = 3

        /** Ngủ dưới mức này (giờ) coi là chưa đủ. */
        const val SLEEP_ENOUGH_HOURS = 7.0

        /** Ngủ dưới mức này (giờ) trong 3 ngày liền coi là thiếu ngủ. */
        const val SLEEP_DEPRIVED_HOURS = 6.0

        /** Uống dưới mức này (ml) coi là thiếu nước. */
        const val WATER_ENOUGH_ML = 1500.0

        /** Ngưỡng khuyến nghị bước chân mỗi ngày (WHO). */
        const val STEP_DAILY_LOW = 5000f
        const val STEP_DAILY_RECOMMENDED = 8000

        /** Các cách viết đơn vị "bước" được chấp nhận. */
        val STEP_UNITS = setOf("bước", "bước chân", "bước/ngày", "steps", "step")

        /** Số ngày liên tiếp đi bộ dưới ngưỡng thì nhắc nhở. */
        const val STEP_TREND_DAYS = 3

        /** Dưới ngưỡng bước/ngày này (và có dữ liệu) thì coi là ít vận động. */
        const val STEP_TREND_LOW = 3_000L

        /** Nhịp tim nghỉ trung bình trên mức này (bpm) thì gợi ý thư giãn. */
        const val HEART_RATE_HIGH_BPM = 85.0

        /** Tỉ lệ hoàn thành mục tiêu dưới mức này (và chưa tới mức khuyến nghị) thì coi là thiếu. */
        const val TARGET_COMPLETION_MEDIUM = 0.8

        /** Mốc xoay vòng mẹo mặc định — đổi mỗi ngày (ngày trong năm). */
        fun defaultRotationSeed(): Int = LocalDate.now().dayOfYear
    }
}
