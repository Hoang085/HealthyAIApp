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
 *
 * **Mẫu số là "snapshot" của từng ngày** (phụ lục C #27): mỗi ngày chỉ tính những thói quen **đã tồn
 * tại từ ngày đó trở về trước** (`Habit.createdAt`). Trước đây mẫu số luôn là số thói quen đang hoạt
 * động **hiện tại**, nên tạo thói quen mới hôm nay sẽ làm tụt tỉ lệ của cả 6 ngày trước đó (và xoá
 * thói quen sẽ làm tỉ lệ quá khứ vọt lên) — biểu đồ không còn phản ánh đúng lịch sử.
 *
 * Ghi chú: **mục tiêu** vẫn so với `Habit.targetValue` hiện tại (không dùng `targetValueAtTime` của
 * từng log) — giữ nguyên nghiệp vụ cũ, chỉ sửa mẫu số.
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

                // Thói quen đã tồn tại trong ngày đó: tạo trước ngày đang xét (mốc đầu ngày) — thói quen
                // tạo hôm nay không bị tính ngược về các ngày trước đó.
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
