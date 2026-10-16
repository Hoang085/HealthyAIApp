package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : HabitRepository {

    override fun getAllActiveHabits(): Flow<List<Habit>> =
        habitDao.getAllActiveHabits().map { list -> list.map { it.toDomain() } }

    override fun getHabitLogsForPeriod(daysBack: Int): Flow<List<HabitLog>> {
        val fromTs = startOfDayEpochMillis(daysBack)
        return habitLogDao.getLogsFromTimestamp(fromTs)
            .map { list -> list.map { it.toDomain() } }
    }

    /** Mốc 00:00 của [daysBack] ngày trước; cố ý căn đầu ngày vì mốc "now - daysBack ngày" từng làm log trong ngày bị loại, tiến độ về 0. */
    private fun startOfDayEpochMillis(daysBack: Int): Long =
        LocalDate.now()
            .minusDays(daysBack.coerceAtLeast(0).toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** Tạo thói quen mới; không cần vẽ lại widget vì widget chỉ hiển thị chỉ số sức khỏe, không hiển thị tiến độ thói quen. */
    override suspend fun insertHabit(habit: Habit): Long =
        habitDao.insertHabit(habit.toEntity())

    /** Cập nhật bằng UPDATE theo khoá chính; không dùng lại insertHabit vì REPLACE = DELETE + INSERT sẽ CASCADE xoá hết log của thói quen. */
    override suspend fun updateHabit(habit: Habit) =
        habitDao.updateHabit(habit.toEntity())

    /** Ghi nhận một lần thực hiện thói quen (bảng `habit_logs`) — widget không hiển thị dữ liệu này. */
    override suspend fun logHabit(log: HabitLog) {
        habitLogDao.insertLog(log.toEntity())
    }

    /** Xóa mềm thói quen (`isActive = 0`) — log và lịch sử của thói quen vẫn được giữ lại. */
    override suspend fun deleteHabit(habitId: Long) {
        habitDao.softDeleteHabit(habitId)
    }

    override suspend fun getUnsyncedHabitLogs(): List<HabitLog> =
        habitLogDao.getUnsyncedLogs().map { it.toDomain() }

    override suspend fun markHabitLogSynced(id: Long) =
        habitLogDao.markAsSynced(id)

    // Mappers (Entity ↔ Domain)
    private fun HabitEntity.toDomain() = Habit(
        id = id,
        name = name,
        category = category,
        targetValue = targetValue,
        unit = unit,
        timeOfDay = timeOfDay,
        frequency = frequency,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun Habit.toEntity() = HabitEntity(
        id = id,
        name = name,
        category = category,
        targetValue = targetValue,
        unit = unit,
        timeOfDay = timeOfDay,
        frequency = frequency,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun HabitLogEntity.toDomain() = HabitLog(
        id = id,
        habitId = habitId,
        habitName = habitName,
        category = category,
        value = value,
        unit = unit,
        targetValueAtTime = targetValueAtTime,
        loggedAt = loggedAt,
        note = note,
        isDirty = isDirty
    )

    private fun HabitLog.toEntity() = HabitLogEntity(
        id = id,
        habitId = habitId,
        habitName = habitName,
        category = category,
        value = value,
        unit = unit,
        targetValueAtTime = targetValueAtTime,
        loggedAt = loggedAt,
        note = note,
        isDirty = isDirty
    )
}
