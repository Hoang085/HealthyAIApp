package com.healthyai.app.domain.usecase.habit

import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Tính mức hoàn thành theo từng ngày trong [days] ngày gần nhất; mẫu số mỗi ngày chỉ gồm thói quen đã tồn tại (createdAt). */
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

                // Chỉ tính thói quen đã tạo trước mốc đầu ngày đang xét (không tính ngược về quá khứ).
                val habitsOfThatDay = habits.filter { it.createdAt < dayEnd }

                val completed = habitsOfThatDay.count { habit ->
                    dayLogs.filter { it.habitId == habit.id }
                        .sumOf { it.value.toDouble() } >= habit.targetValue
                }

                DailyConsistency(
                    date = date,
                    completedHabits = completed,
                    totalHabits = habitsOfThatDay.size
                )
            }
        }
    }
}
