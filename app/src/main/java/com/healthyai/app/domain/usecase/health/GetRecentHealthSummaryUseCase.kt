package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.repository.HealthDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Đọc lịch sử tóm tắt sức khỏe [days] ngày gần nhất; trả Flow nên thẻ tự cập nhật khi có hàng mới. */
class GetRecentHealthSummaryUseCase @Inject constructor(
    private val repository: HealthDataRepository
) {
    operator fun invoke(days: Int = HealthSummaryHistory.DEFAULT_DAYS): Flow<List<DailyHealthSummary>> {
        val windowDays = days.coerceAtLeast(1)
        val zone = ZoneId.systemDefault()
        val firstDay = LocalDate.now(zone).minusDays((windowDays - 1).toLong())
        val fromTs = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()

        return repository.getHealthHistorySince(fromTs).map { history ->
            // Tính lại today mỗi lần phát để cửa sổ ngày vẫn đúng khi app mở qua đêm.
            HealthSummaryHistory.summarize(
                history = history,
                days = windowDays,
                today = LocalDate.now(zone),
                zone = zone
            )
        }
    }
}
