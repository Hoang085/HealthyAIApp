package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Toán tính **Chỉ số sinh lực** (0–100) trên cửa sổ [WINDOW_DAYS] ngày gần nhất.
 *
 * Điểm được ghép từ **ba thành phần** và lấy trung bình theo **từng ngày** (ngày nào cũng có trọng số
 * như nhau):
 * - **Thói quen** — tỉ lệ thói quen đạt mục tiêu trong ngày;
 * - **Giờ ngủ** — giờ ngủ đêm đó so với mục tiêu [SLEEP_TARGET_HOURS];
 * - **Bước chân** — số bước ngày đó so với mục tiêu [STEPS_TARGET].
 *
 * Nhờ tính **theo ngày**, điểm phản ánh đúng *hiện tại*: ngày không ghi gì / không đi bộ / không có
 * giờ ngủ đóng góp **0** cho thành phần tương ứng → bỏ thói quen vài ngày là điểm **tụt**, thay vì
 * giữ nguyên như khi lấy trung bình cả tuần với mặc định 50/70.
 *
 * Nguyên tắc với dữ liệu thiếu:
 * - Chỉ tính các thành phần **thực sự có nguồn trên máy** (ví dụ máy không có Health Connect và không
 *   bật cảm biến bước chân ⇒ chỉ còn thành phần thói quen), rồi **chuẩn hoá lại trọng số** để điểm
 *   không bị kéo xuống chỉ vì thiếu nguồn.
 * - Cửa sổ **không có thành phần nào** (chưa có thói quen, chưa có bước chân, chưa có giờ ngủ) → trả
 *   `null` — nghĩa là "chưa đủ dữ liệu để chấm điểm", UI hiện lời nhắc thay vì một con số ảo.
 *
 * Hàm thuần Kotlin (không phụ thuộc Android/Room) nên test được trên JVM.
 */
object VitalityScoreMath {

    /** Số ngày của cửa sổ chấm điểm (khớp cửa sổ mà tab Phân tích đang phân tích). */
    const val WINDOW_DAYS = 7

    /** Mục tiêu giờ ngủ mỗi đêm (giờ) — ngủ đủ mức này thì thành phần giấc ngủ đạt điểm tối đa. */
    const val SLEEP_TARGET_HOURS = 8.0

    /** Mục tiêu bước chân mỗi ngày (bước) — khuyến nghị WHO. */
    const val STEPS_TARGET = 8_000.0

    private const val HABIT_WEIGHT = 0.4
    private const val SLEEP_WEIGHT = 0.3
    private const val STEPS_WEIGHT = 0.3

    /**
     * Điểm sinh lực của cửa sổ `windowDays` ngày kết thúc ở [today].
     *
     * @return 0–100, hoặc `null` khi cửa sổ **không có thành phần nào** để chấm.
     */
    fun compute(
        activeHabits: List<Habit>,
        habitLogs: List<HabitLog>,
        healthHistory: List<HealthData>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        windowDays: Int = WINDOW_DAYS
    ): Int? {
        val window = windowOf(today, windowDays)
        val rowsByDate = healthHistory.groupBy { capturedDate(it, zone) }

        val hasHabits = activeHabits.isNotEmpty()
        val hasSleep = window.any { date -> rowsByDate[date].orEmpty().any { it.sleepHours > 0.0 } }
        val hasSteps = window.any { date -> rowsByDate[date].orEmpty().any { it.steps > 0L } }

        if (!hasHabits && !hasSleep && !hasSteps) return null

        val weightSum = (if (hasHabits) HABIT_WEIGHT else 0.0) +
            (if (hasSleep) SLEEP_WEIGHT else 0.0) +
            (if (hasSteps) STEPS_WEIGHT else 0.0)

        val dayScores = window.map { date ->
            val dayLogs = habitLogs.filter { it.loggedAt in dayOf(date, zone) }

            var acc = 0.0
            if (hasHabits) acc += HABIT_WEIGHT * habitCompletion(activeHabits, dayLogs)
            if (hasSleep) {
                val hours = rowsByDate[date].orEmpty().latest()?.sleepHours ?: 0.0
                acc += SLEEP_WEIGHT * ratio(hours, SLEEP_TARGET_HOURS)
            }
            if (hasSteps) {
                val steps = rowsByDate[date].orEmpty().latest()?.steps ?: 0L
                acc += STEPS_WEIGHT * ratio(steps.toDouble(), STEPS_TARGET)
            }
            acc / weightSum
        }

        return Math.round(dayScores.average() * 100.0).toInt().coerceIn(0, 100)
    }

    /**
     * Cửa sổ có **quan sát nào** không (có log thói quen, hoặc có hàng bước chân/giờ ngủ) — dùng để
     * quyết định có hiển thị phần so sánh với tuần trước hay không (`delta`).
     */
    fun hasObservations(
        habitLogs: List<HabitLog>,
        healthHistory: List<HealthData>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        windowDays: Int = WINDOW_DAYS
    ): Boolean {
        val window = windowOf(today, windowDays)
        val from = startOf(window.first(), zone)
        val to = startOf(window.last().plusDays(1), zone)

        if (habitLogs.any { it.loggedAt in from until to }) return true
        return healthHistory.any {
            val ms = it.recordedAt
            ms in from until to && (it.sleepHours > 0.0 || it.steps > 0L)
        }
    }

    /** Tỉ lệ thói quen **đạt mục tiêu** trong [dayLogs] trên tổng số thói quen đang theo dõi. */
    private fun habitCompletion(habits: List<Habit>, dayLogs: List<HabitLog>): Double {
        if (habits.isEmpty()) return 0.0
        val completed = habits.count { habit ->
            dayLogs.filter { it.habitId == habit.id }
                .sumOf { it.value.toDouble() } >= habit.targetValue
        }
        return completed.toDouble() / habits.size
    }

    private fun ratio(value: Double, target: Double): Double =
        if (target <= 0.0) 0.0 else (value / target).coerceIn(0.0, 1.0)

    private fun List<HealthData>.latest(): HealthData? = maxByOrNull { it.recordedAt }

    /** Cửa sổ ngày **cũ → mới**, kết thúc ở [today]. */
    private fun windowOf(today: LocalDate, windowDays: Int): List<LocalDate> =
        (windowDays.coerceAtLeast(1) - 1 downTo 0).map { today.minusDays(it.toLong()) }

    private fun dayOf(date: LocalDate, zone: ZoneId): LongRange {
        val start = startOf(date, zone)
        return start until startOf(date.plusDays(1), zone)
    }

    private fun startOf(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun capturedDate(data: HealthData, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(data.recordedAt).atZone(zone).toLocalDate()
}
