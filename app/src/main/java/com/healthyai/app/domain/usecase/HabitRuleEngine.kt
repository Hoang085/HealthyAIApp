package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
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
 *
 * Cách mở rộng:
 *  - Thêm rule mới: tạo hàm analyze[Category] rồi gọi trong generateSuggestions()
 *  - Tích hợp ML: thay các ngưỡng cố định bằng model TFLite sau khi có đủ dữ liệu
 */
@Singleton
class HabitRuleEngine @Inject constructor() {

    fun generateSuggestions(recentLogs: List<HabitLog>): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        suggestions += analyzeSleep(recentLogs.filter { it.category == HabitCategory.SLEEP })
        suggestions += analyzeWater(recentLogs.filter { it.category == HabitCategory.WATER })
        suggestions += analyzeExercise(recentLogs.filter { it.category == HabitCategory.EXERCISE })
        // TODO: Thêm analyzeNutrition(), analyzeMindfulness() ở đây
        return suggestions.sortedByDescending { it.priority.ordinal }
    }

    // ── Rule 1: Thiếu ngủ ─────────────────────────────────────
    // Trigger: ngủ dưới 6 tiếng trong 3 ngày liên tiếp → HIGH priority
    // Trigger: ngủ trung bình dưới 7 tiếng → MEDIUM priority
    private fun analyzeSleep(sleepLogs: List<HabitLog>): List<Suggestion> {
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
    private fun analyzeWater(waterLogs: List<HabitLog>): List<Suggestion> {
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
    // Trigger: < 5000 bước/ngày trong 5 ngày liên tiếp → HIGH
    // Trigger: < 8000 bước/ngày trung bình 7 ngày → LOW
    private fun analyzeExercise(exerciseLogs: List<HabitLog>): List<Suggestion> {
        val recent5 = exerciseLogs.sortedByDescending { it.loggedAt }.take(5)

        if (recent5.size >= 5 && recent5.all { it.value < 5000f }) {
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

        val recent7 = exerciseLogs.sortedByDescending { it.loggedAt }.take(7)
        if (recent7.size >= 5 && recent7.averageOf { it.value.toDouble() } < 8000) {
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

    // ── Extension helper ──────────────────────────────────────
    private fun <T> List<T>.averageOf(selector: (T) -> Double): Double {
        if (isEmpty()) return 0.0
        return sumOf(selector) / size
    }
}
