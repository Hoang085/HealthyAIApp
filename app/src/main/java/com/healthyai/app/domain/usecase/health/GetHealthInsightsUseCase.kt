package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.usecase.HabitRuleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * UseCase tổng hợp: Kết hợp dữ liệu từ nhiều nguồn để tạo ra Insight cá nhân hóa.
 * Luồng Domain: Repository -> UseCase -> Rule Engine -> Presentation
 */
class GetHealthInsightsUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val healthDataRepository: HealthDataRepository,
    private val ruleEngine: HabitRuleEngine
) {
    /**
     * Lấy danh sách gợi ý sức khỏe dựa trên **thói quen người dùng đang theo dõi**, 7 ngày nhật ký
     * thói quen và dữ liệu sức khỏe cứng (Health Connect / cảm biến) lưu trong Room.
     *
     * Feed gợi ý bám theo danh mục thói quen người dùng có và xoay vòng mẹo theo ngày (xem
     * [HabitRuleEngine.generateSuggestions]).
     *
     * Lịch sử sức khỏe được cắt theo **mốc thời gian** ([HealthDataRepository.getHealthHistorySince])
     * chứ không theo **số hàng**: bảng `health_data_history` có thể có nhiều hàng trong cùng một ngày,
     * cắt theo số hàng sẽ làm thiếu ngày và khiến luật "3 ngày liên tiếp" của Rule Engine **âm thầm
     * không chạy** (phụ lục C #42). Cùng cửa sổ 7 ngày với thẻ "Lịch sử sức khỏe 7 ngày".
     */
    operator fun invoke(): Flow<List<Suggestion>> {
        val windowDays = INSIGHT_WINDOW_DAYS
        val zone = ZoneId.systemDefault()
        val firstDay = LocalDate.now(zone).minusDays((windowDays - 1).toLong())
        val fromTs = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            habitRepository.getAllActiveHabits(),
            habitRepository.getHabitLogsForPeriod(windowDays - 1),
            healthDataRepository.getHealthHistorySince(fromTs)
        ) { habits, habitLogs, healthHistory ->
            ruleEngine.generateSuggestions(
                activeHabits = habits,
                recentLogs = habitLogs,
                healthHistory = healthHistory
            )
        }
    }

    private companion object {
        /** Cửa sổ phân tích: **7 ngày gần nhất tính cả hôm nay**. */
        const val INSIGHT_WINDOW_DAYS = 7
    }
}
