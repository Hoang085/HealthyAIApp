package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  HabitRuleEngine — On-device AI (Rule-Based Recommendation) ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  - Chạy hoàn toàn offline trên thiết bị (Privacy-first)     ║
 * ║  - Không cần model phức tạp, hiệu quả với data ít            ║
 * ║  - Dễ viết Unit Test để chứng minh logic hoạt động chính xác ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Singleton
class HabitRuleEngine @Inject constructor() {

    fun generateSuggestions(
        recentLogs: List<HabitLog>,
        healthHistory: List<HealthData> = emptyList()
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // 1. Phân tích dựa trên nhật ký thói quen (Manual Logs)
        suggestions += analyzeSleepLogs(recentLogs.filter { it.category == HabitCategory.SLEEP })
        suggestions += analyzeWaterLogs(recentLogs.filter { it.category == HabitCategory.WATER })
        suggestions += analyzeExerciseLogs(recentLogs.filter { it.category == HabitCategory.EXERCISE })
        
        // 2. Phân tích dựa trên dữ liệu sức khỏe cứng (Health Connect History)
        suggestions += analyzeHealthTrends(healthHistory)
        
        return suggestions.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Phân tích xu hướng sức khỏe từ dữ liệu cứng (Steps, Sleep, Heart Rate).
     *
     * **Dữ liệu được gom theo NGÀY trước khi phân tích** (phụ lục C #16): `health_data_history`
     * ghi 1 hàng cho **mỗi lần sync** (6 giờ/lần), nên 3 hàng gần nhất có thể chỉ cách nhau vài
     * giờ. Trước đây `take(3)` hiểu nhầm 3 lần sync thành "3 ngày liên tiếp" ⇒ chỉ cần 1 đêm thiếu
     * ngủ là bắn cảnh báo CRITICAL, và "nhịp tim trung bình 7 ngày" thực chất chỉ là ~1,75 ngày.
     *
     * Nay mỗi ngày chỉ lấy **hàng mới nhất trong ngày** (dữ liệu đầy đủ nhất), rồi:
     * - cảnh báo thiếu ngủ cần **3 ngày gần nhất có dữ liệu và liên tiếp nhau** (cách đúng 1 ngày);
     * - nhịp tim trung bình lấy trên tối đa **7 ngày gần nhất** — khớp nội dung gợi ý.
     */
    private fun analyzeHealthTrends(history: List<HealthData>): List<Suggestion> {
        if (history.isEmpty()) return emptyList()
        val dailyHistory = latestRowPerDay(history)
        val suggestions = mutableListOf<Suggestion>()

        // Xu hướng 3 ngày thiếu ngủ (Dữ liệu cứng)
        // `sleepHours = 0` nghĩa là **chưa có dữ liệu giấc ngủ** (ví dụ máy chỉ có cảm biến đếm bước),
        // không phải "ngủ 0 giờ" — nếu không loại ra thì mọi hàng 0 đều bị coi là thiếu ngủ và bắn
        // cảnh báo CRITICAL sai.
        val recent3Sleep = dailyHistory.take(3)
        if (recent3Sleep.size >= 3 &&
            areConsecutiveDays(recent3Sleep.map { it.localDate() }) &&
            recent3Sleep.all { it.sleepHours > 0.0 && it.sleepHours < 6.0 }
        ) {
            suggestions.add(
                Suggestion(
                    title = "🚨 Phát hiện xu hướng thiếu ngủ",
                    message = "Hệ thống ghi nhận bạn đã ngủ dưới 6 giờ trong 3 ngày liên tiếp. " +
                              "Cơ thể bạn cần được phục hồi để tránh suy giảm hệ miễn dịch.",
                    priority = SuggestionPriority.CRITICAL,
                    category = HabitCategory.SLEEP,
                    actionLabel = "Tối ưu hóa phòng ngủ"
                )
            )
        }

        // Xu hướng nhịp tim cao khi nghỉ ngơi (trung bình 7 ngày gần nhất) — chỉ tính những ngày **có**
        // số nhịp tim, để hàng không có dữ liệu (0) không kéo trung bình xuống.
        val heartRateRows = dailyHistory.take(7).filter { it.heartRate > 0.0 }
        val avgHeartRate = if (heartRateRows.isEmpty()) 0.0 else heartRateRows.averageOf { it.heartRate }
        if (avgHeartRate > 85.0) {
            suggestions.add(
                Suggestion(
                    title = "💓 Nhịp tim nghỉ ngơi hơi cao",
                    message = "Nhịp tim trung bình 7 ngày qua của bạn là ${avgHeartRate.toInt()} bpm. " +
                              "Điều này có thể do căng thẳng hoặc mệt mỏi. Thử 5 phút thiền định nhé.",
                    priority = SuggestionPriority.MEDIUM,
                    category = HabitCategory.MINDFULNESS,
                    actionLabel = "Bắt đầu thiền"
                )
            )
        }

        // Xu hướng ít vận động theo **bước chân** trong lịch sử (nguồn: Health Connect hoặc cảm biến của máy)
        suggestions += analyzeStepTrend(dailyHistory)

        return suggestions
    }

    /**
     * Xu hướng ít vận động theo **số bước chân** ghi trong `health_data_history`.
     *
     * Dữ liệu này có được nhờ app tự lưu bản chụp mỗi lần đồng bộ: bước chân lấy từ Health Connect,
     * nếu Health Connect trống thì lấy từ **cảm biến đếm bước của điện thoại** — nên rule này vẫn
     * hoạt động trên máy không cài ứng dụng sức khỏe nào.
     *
     * Chỉ xét những ngày **có** dữ liệu bước chân (`steps > 0`): hàng 0 bước có thể là "chưa có nguồn
     * dữ liệu" chứ không chắc là "hôm đó không đi bộ", nên không kết luận từ chúng.
     */
    private fun analyzeStepTrend(dailyHistory: List<HealthData>): List<Suggestion> {
        val recent = dailyHistory.take(STEP_TREND_DAYS)
        if (recent.size < STEP_TREND_DAYS) return emptyList()
        if (!areConsecutiveDays(recent.map { it.localDate() })) return emptyList()
        if (!recent.all { it.steps > 0L && it.steps < STEP_TREND_LOW }) return emptyList()

        val avgSteps = recent.averageOf { it.steps.toDouble() }
        return listOf(
            Suggestion(
                title = "🚶 Mấy ngày nay bạn đi bộ rất ít",
                message = "Trung bình $STEP_TREND_DAYS ngày qua bạn chỉ đi ${avgSteps.toInt()} bước/ngày. " +
                          "Hãy bắt đầu với 10 phút đi bộ sau bữa tối — nhỏ nhưng tạo thói quen lớn!",
                priority = SuggestionPriority.MEDIUM,
                category = HabitCategory.EXERCISE,
                actionLabel = "Ghi nhận vận động"
            )
        )
    }

    /**
     * Mỗi ngày chỉ giữ **hàng mới nhất** (theo `recordedAt`), sắp xếp mới nhất → cũ nhất.
     *
     * Hàng mới nhất trong ngày là hàng đầy đủ dữ liệu nhất (bước chân/calo tích luỹ cả ngày).
     */
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

    /**
     * Tính toán "Chỉ số sinh lực" (Vitality Score) dựa trên mức độ hoàn thiện thói quen.
     * Trả về giá trị từ 0-100.
     */
    fun calculateVitalityScore(recentLogs: List<HabitLog>): Int {
        if (recentLogs.isEmpty()) return 50 // Điểm trung bình nếu chưa có dữ liệu

        val sleepScore = calculateCategoryScore(recentLogs, HabitCategory.SLEEP)
        val waterScore = calculateCategoryScore(recentLogs, HabitCategory.WATER)
        val exerciseScore = calculateCategoryScore(recentLogs, HabitCategory.EXERCISE)
        
        // Trọng số: Ngủ (40%), Nước (30%), Vận động (30%)
        val totalScore = (sleepScore * 0.4) + (waterScore * 0.3) + (exerciseScore * 0.3)
        return totalScore.toInt().coerceIn(0, 100)
    }

    private fun calculateCategoryScore(logs: List<HabitLog>, category: HabitCategory): Double {
        val categoryLogs = logs.filter { it.category == category }
        if (categoryLogs.isEmpty()) return 70.0 // Mặc định 70 nếu không có data cho category này

        // Tính tỉ lệ phần trăm hoàn thành trung bình
        val completionRatios = categoryLogs.map { log ->
            if (log.targetValueAtTime > 0) (log.value / log.targetValueAtTime).toDouble() else 0.0
        }
        
        val avgCompletion = completionRatios.average().coerceIn(0.0, 1.0)
        return avgCompletion * 100
    }

    // ── Rule 1: Thiếu ngủ ─────────────────────────────────────
    // Trigger: ngủ dưới 6 tiếng trong 3 ngày liên tiếp → HIGH priority
    // Trigger: ngủ trung bình dưới 7 tiếng → MEDIUM priority
    private fun analyzeSleepLogs(sleepLogs: List<HabitLog>): List<Suggestion> {
        val recent3 = sleepLogs.sortedByDescending { it.loggedAt }.take(3)

        if (recent3.size >= 3 && recent3.all { it.value < 6f }) {
            val avg = recent3.averageOf { it.value.toDouble() }
            return listOf(
                Suggestion(
                    title = "⚠️ Cảnh báo thiếu ngủ nghiêm trọng",
                    message = "Bạn đã ngủ trung bình ${String.format("%.1f", avg)} tiếng/đêm trong 3 ngày qua. " +
                              "Thiếu ngủ mãn tính làm giảm 40% khả năng tập trung. Hãy thử ngủ sớm hơn 30 phút tối nay.",
                    priority = SuggestionPriority.HIGH,
                    category = HabitCategory.SLEEP,
                    actionLabel = "Đặt nhắc nhở đi ngủ"
                )
            )
        }

        val recent7 = sleepLogs.sortedByDescending { it.loggedAt }.take(7)
        if (recent7.size >= 3 && recent7.averageOf { it.value.toDouble() } < 7.0) {
            return listOf(
                Suggestion(
                    title = "💤 Cải thiện chất lượng giấc ngủ",
                    message = "Chất lượng giấc ngủ của bạn có thể tốt hơn. Thử tắt thiết bị điện tử 1 tiếng trước khi ngủ.",
                    priority = SuggestionPriority.MEDIUM,
                    category = HabitCategory.SLEEP
                )
            )
        }
        return emptyList()
    }

    // ── Rule 2: Mất nước ──────────────────────────────────────
    // Trigger: trung bình < 1000ml/ngày → CRITICAL
    // Trigger: trung bình < 1500ml/ngày → MEDIUM
    private fun analyzeWaterLogs(waterLogs: List<HabitLog>): List<Suggestion> {
        if (waterLogs.isEmpty()) return emptyList()
        val recent = waterLogs.sortedByDescending { it.loggedAt }.take(7)
        val avgMl = recent.averageOf { it.value.toDouble() }

        return when {
            avgMl < 1000 -> listOf(
                Suggestion(
                    title = "🚨 Mức nước nguy hiểm thấp",
                    message = "Bạn uống trung bình ${avgMl.toInt()}ml/ngày — chỉ bằng 50% nhu cầu tối thiểu! " +
                              "Ngay lúc này hãy uống 1 ly nước đầy.",
                    priority = SuggestionPriority.CRITICAL,
                    category = HabitCategory.WATER,
                    actionLabel = "Ghi nhận uống nước"
                )
            )
            avgMl < 1500 -> listOf(
                Suggestion(
                    title = "💧 Uống thêm nước mỗi ngày",
                    message = "Lượng nước trung bình của bạn là ${avgMl.toInt()}ml/ngày. " +
                              "Mục tiêu khuyến nghị: 2000ml. Thử đặt ly nước trên bàn làm việc!",
                    priority = SuggestionPriority.MEDIUM,
                    category = HabitCategory.WATER
                )
            )
            else -> emptyList()
        }
    }

    // ── Rule 3: Ít vận động ───────────────────────────────────
    // Trigger (đơn vị là BƯỚC): < 5000 bước/ngày trong 5 ngày liên tiếp → HIGH
    //                            < 8000 bước/ngày trung bình 7 ngày → LOW
    // Trigger (đơn vị khác, ví dụ "phút"): hoàn thành < 50% mục tiêu → HIGH; < 80% → LOW
    /**
     * Gợi ý vận động.
     *
     * Phụ lục C #17: trước đây rule so `value` với **5000/8000 bước bất kể đơn vị**, nên thói quen
     * đo bằng phút/giờ (ví dụ "Chạy bộ 30 phút") ghi 30 cũng bị coi là "30 bước" ⇒ sinh thông báo
     * vô nghĩa *"Số bước chân trung bình của bạn là 30 bước trong 5 ngày qua"*.
     *
     * Nay rule chia theo **đơn vị của chính thói quen**:
     * - log có đơn vị bước → giữ nguyên ngưỡng khuyến nghị 5000/8000 bước ([STEP_DAILY_LOW],
     *   [STEP_DAILY_RECOMMENDED]) vì đó là ngưỡng tính bằng bước;
     * - log đo bằng đơn vị khác (phút, km, calo…) → so theo **tỉ lệ hoàn thành mục tiêu**
     *   (`value / targetValueAtTime` của chính thói quen), không so với con số bước chân.
     */
    private fun analyzeExerciseLogs(exerciseLogs: List<HabitLog>): List<Suggestion> {
        val byNewestFirst = exerciseLogs.sortedByDescending { it.loggedAt }

        val stepSuggestions = analyzeStepLogs(byNewestFirst.filter { it.isStepUnit() })
        if (stepSuggestions.isNotEmpty()) return stepSuggestions

        return analyzeExerciseTargetCompletion(byNewestFirst.filterNot { it.isStepUnit() })
    }

    /** Rule theo số bước — chỉ áp dụng cho log có đơn vị đo bằng bước. */
    private fun analyzeStepLogs(stepLogs: List<HabitLog>): List<Suggestion> {
        val recent5 = stepLogs.take(5)

        if (recent5.size >= 5 && recent5.all { it.value < STEP_DAILY_LOW }) {
            val avg = recent5.averageOf { it.value.toDouble() }
            return listOf(
                Suggestion(
                    title = "🏃 Tăng hoạt động thể chất ngay hôm nay",
                    message = "Số bước chân trung bình của bạn là ${avg.toInt()} bước trong 5 ngày qua. " +
                              "Hãy bắt đầu với 10 phút đi bộ sau bữa tối — nhỏ nhưng tạo thói quen lớn!",
                    priority = SuggestionPriority.HIGH,
                    category = HabitCategory.EXERCISE,
                    actionLabel = "Bắt đầu theo dõi bước chân"
                )
            )
        }

        val recent7 = stepLogs.take(7)
        if (recent7.size >= 5 && recent7.averageOf { it.value.toDouble() } < STEP_DAILY_RECOMMENDED) {
            return listOf(
                Suggestion(
                    title = "🚶 Hướng tới 8000 bước mỗi ngày",
                    message = "WHO khuyến nghị ít nhất 8000 bước/ngày để duy trì sức khỏe tim mạch. Bạn đang gần đến mục tiêu!",
                    priority = SuggestionPriority.LOW,
                    category = HabitCategory.EXERCISE
                )
            )
        }
        return emptyList()
    }

    /**
     * Rule theo **mục tiêu của thói quen** cho các log không đo bằng bước: dùng tỉ lệ
     * `value / targetValueAtTime` nên đúng với mọi đơn vị (phút, km, calo…).
     *
     * Cần tối thiểu [MIN_LOGS_FOR_TREND] lần ghi (và phải có mục tiêu tại thời điểm ghi) mới kết
     * luận là "xu hướng"; dưới ngưỡng đó chỉ là một vài lần ghi lẻ tẻ.
     */
    private fun analyzeExerciseTargetCompletion(exerciseLogs: List<HabitLog>): List<Suggestion> {
        val recent = exerciseLogs.filter { it.targetValueAtTime > 0f }.take(5)
        if (recent.size < MIN_LOGS_FOR_TREND) return emptyList()

        val avgValue = recent.averageOf { it.value.toDouble() }
        val target = recent.first().targetValueAtTime
        val unit = recent.first().unit
        val avgRatio = recent.averageOf { (it.value / it.targetValueAtTime).toDouble().coerceIn(0.0, 1.0) }

        return when {
            avgRatio < TARGET_COMPLETION_LOW -> listOf(
                Suggestion(
                    title = "🏃 Tăng hoạt động thể chất ngay hôm nay",
                    message = "Mức vận động trung bình của bạn là ${formatAmount(avgValue)} $unit trong " +
                              "${recent.size} lần ghi gần nhất — mới đạt ${(avgRatio * 100).toInt()}% mục tiêu " +
                              "${formatAmount(target.toDouble())} $unit. Hãy bắt đầu với 10 phút đi bộ sau bữa " +
                              "tối — nhỏ nhưng tạo thói quen lớn!",
                    priority = SuggestionPriority.HIGH,
                    category = HabitCategory.EXERCISE,
                    actionLabel = "Ghi nhận vận động"
                )
            )
            avgRatio < TARGET_COMPLETION_MEDIUM -> listOf(
                Suggestion(
                    title = "🚶 Tăng dần mục tiêu vận động",
                    message = "Mức vận động trung bình của bạn là ${formatAmount(avgValue)} $unit, đạt " +
                              "${(avgRatio * 100).toInt()}% mục tiêu ${formatAmount(target.toDouble())} $unit. " +
                              "Cố thêm một chút mỗi ngày để đạt mục tiêu nhé!",
                    priority = SuggestionPriority.LOW,
                    category = HabitCategory.EXERCISE
                )
            )
            else -> emptyList()
        }
    }

    /** Đơn vị của log có phải "bước" không (ngưỡng 5000/8000 chỉ có nghĩa với đơn vị này). */
    private fun HabitLog.isStepUnit(): Boolean =
        unit.trim().lowercase() in STEP_UNITS

    /** Hiển thị số gọn: `12.0` → `"12"`, `7.5` → `"7.5"`. */
    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

    // ── Extension helper ──────────────────────────────────────
    private fun <T> List<T>.averageOf(selector: (T) -> Double): Double {
        if (isEmpty()) return 0.0
        return sumOf(selector) / size
    }

    private companion object {
        /** Ngưỡng khuyến nghị tính bằng **bước chân** cho mỗi ngày (WHO). */
        const val STEP_DAILY_LOW = 5000f
        const val STEP_DAILY_RECOMMENDED = 8000

        /** Các cách viết đơn vị "bước" được chấp nhận (xem `AddEditHabitViewModel`/Data set A). */
        val STEP_UNITS = setOf("bước", "bước chân", "bước/ngày", "steps", "step")

        /** Số lần ghi tối thiểu để coi là "xu hướng" khi so theo mục tiêu của thói quen. */
        const val MIN_LOGS_FOR_TREND = 3

        /** Số ngày liên tiếp đi bộ dưới ngưỡng thì nhắc nhở (nguồn: `health_data_history`). */
        const val STEP_TREND_DAYS = 3

        /** Dưới ngưỡng bước/ngày này (và có dữ liệu) thì coi là ít vận động. */
        const val STEP_TREND_LOW = 3_000L

        /** Tỉ lệ hoàn thành mục tiêu: dưới 50% → HIGH, dưới 80% → LOW. */
        const val TARGET_COMPLETION_LOW = 0.5
        const val TARGET_COMPLETION_MEDIUM = 0.8
    }
}
