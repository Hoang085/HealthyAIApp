package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.repository.HealthDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Đọc **lịch sử tóm tắt sức khỏe** của [days] ngày gần nhất (mặc định 7, tính cả hôm nay) từ
 * `health_data_history` — nguồn cho thẻ "Lịch sử sức khỏe 7 ngày" ở tab Phân tích.
 *
 * Trả về `Flow` nên thẻ tự cập nhật khi có hàng mới được ghi (lần đồng bộ của `HealthSyncWorker`, một
 * lần đo nhịp tim bằng camera…) mà không cần mở lại màn hình.
 *
 * Đây cũng là **cùng dữ liệu** mà `HabitRuleEngine` dùng để sinh gợi ý AI, nên phần "vì sao AI gợi ý
 * thế" trên màn Phân tích luôn khớp với con số người dùng nhìn thấy.
 */
class GetRecentHealthSummaryUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    operator fun invoke(days: Int = HealthSummaryHistory.DEFAULT_DAYS): Flow<List<DailyHealthSummary>> {
        val windowDays = days.coerceAtLeast(1)
        val zone = ZoneId.systemDefault()
        val firstDay = LocalDate.now(zone).minusDays((windowDays - 1).toLong())
        val fromTs = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()

        return repository.getHealthHistorySince(fromTs).map { history ->
            // `today` được tính lại ở mỗi lần phát để cửa sổ 7 ngày vẫn đúng nếu app mở qua đêm.
            HealthSummaryHistory.summarize(
                history = history,
                days = windowDays,
                today = LocalDate.now(zone),
                zone = zone
            )
        }
    }
}
