package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * UseCase tính mức độ hoàn thành thói quen theo **từng ngày** trong [days] ngày gần nhất
 * (mặc định 7 ngày, bao gồm hôm nay) — nguồn dữ liệu cho biểu đồ "Sự nhất quán hàng tuần".
 *
 * Một thói quen được coi là "đạt" trong ngày khi tổng giá trị log của nó trong ngày đó
 * >= mục tiêu hiện tại của thói quen. Kết quả trả về theo thứ tự cũ → mới để vẽ từ trái sang phải.
 */
class GetWeeklyConsistencyUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    operator fun invoke(days: Int = 7): Flow<List<DailyConsistency>> {
        val windowDays = days.coerceAtLeast(1)
        val zone = ZoneId.systemDefault()

        return combine(
            habitRepository.getAllActiveHabits(),
            // days - 1 ngày trước hôm nay → vừa đủ [days] ngày tính cả hôm nay
            habitRepository.getHabitLogsForPeriod(windowDays - 1)
        ) { habits, logs ->
            val today = LocalDate.now(zone)

            (windowDays - 1 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                val dayLogs = logs.filter { it.loggedAt >= dayStart && it.loggedAt < dayEnd }

                val completed = habits.count { habit ->
                    dayLogs.filter { it.habitId == habit.id }
                        .sumOf { it.value.toDouble() } >= habit.targetValue
                }

                DailyConsistency(
                    date = date,
                    completedHabits = completed,
                    totalHabits = habits.size
                )
            }
        }
    }
}
