package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        val fromTs = System.currentTimeMillis() - (daysBack * 86_400_000L)
        return habitLogDao.getLogsFromTimestamp(fromTs)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertHabit(habit: Habit): Long =
        habitDao.insertHabit(habit.toEntity())

    override suspend fun logHabit(log: HabitLog) {
        habitLogDao.insertLog(log.toEntity())
    }

    override suspend fun deleteHabit(habitId: Long) =
        habitDao.softDeleteHabit(habitId)

    // ── Mappers (Entity ↔ Domain) ───────────────────────────
    private fun HabitEntity.toDomain() = Habit(
        id = id, name = name,
        category = HabitCategory.valueOf(category),
        targetValue = targetValue, unit = unit,
        isActive = isActive, createdAt = createdAt
    )

    private fun Habit.toEntity() = HabitEntity(
        id = id, name = name, category = category.name,
        targetValue = targetValue, unit = unit,
        isActive = isActive, createdAt = createdAt
    )

    private fun HabitLogEntity.toDomain() = HabitLog(
        id = id, habitId = habitId, habitName = habitName,
        category = HabitCategory.valueOf(category),
        value = value, unit = unit,
        loggedAt = loggedAt, note = note, isDirty = isDirty
    )

    private fun HabitLog.toEntity() = HabitLogEntity(
        id = id, habitId = habitId, habitName = habitName,
        category = category.name,
        value = value, unit = unit,
        loggedAt = loggedAt, note = note, isDirty = isDirty
    )
}
