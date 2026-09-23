package com.healthyai.app.data.repository

import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitLog
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.widget.HabitWidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val habitWidgetRefresher: HabitWidgetRefresher
) : HabitRepository {

    override fun getAllActiveHabits(): Flow<List<Habit>> =
        habitDao.getAllActiveHabits().map { list -> list.map { it.toDomain() } }

    override fun getHabitLogsForPeriod(daysBack: Int): Flow<List<HabitLog>> {
        val fromTs = startOfDayEpochMillis(daysBack)
        return habitLogDao.getLogsFromTimestamp(fromTs)
            .map { list -> list.map { it.toDomain() } }
    }

    /**
     * Mốc bắt đầu khoảng thời gian, **căn theo đầu ngày (00:00)** của [daysBack] ngày trước.
     *
     * - `daysBack = 0` → từ 00:00 hôm nay (nghĩa là "hôm nay").
     * - `daysBack = 7` → từ 00:00 của 7 ngày trước.
     *
     * Trước đây mốc được tính là `now - daysBack ngày`, nên với `daysBack = 0` mốc bằng đúng
     * thời điểm gọi hàm → mọi log đã ghi trong ngày (trước thời điểm đó) đều bị loại, khiến
     * tiến độ/trạng thái hoàn thành về 0 mỗi lần mở lại màn hình.
     */
    private fun startOfDayEpochMillis(daysBack: Int): Long =
        LocalDate.now()
            .minusDays(daysBack.coerceAtLeast(0).toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * Ghi thói quen mới **hoặc** cập nhật thói quen đã có (`OnConflictStrategy.REPLACE`).
     *
     * Sau khi ghi DB phải gọi [HabitWidgetRefresher.refresh] để widget màn hình chính vẽ lại: Glance
     * chỉ đọc dữ liệu một lần lúc vẽ và `updatePeriodMillis = 0` nên widget sẽ đứng yên ở dữ liệu cũ
     * nếu không có ai yêu cầu cập nhật (mục 3 #15 — TC-WG-04).
     */
    override suspend fun insertHabit(habit: Habit): Long {
        val id = habitDao.insertHabit(habit.toEntity())
        habitWidgetRefresher.refresh()
        return id
    }

    /** Ghi nhận một lần thực hiện thói quen rồi vẽ lại widget (widget đọc tiến độ từ `habit_logs`). */
    override suspend fun logHabit(log: HabitLog) {
        habitLogDao.insertLog(log.toEntity())
        habitWidgetRefresher.refresh()
    }

    /** Xóa mềm (`isActive = 0`) rồi vẽ lại widget — widget chuyển sang thói quen còn hoạt động khác. */
    override suspend fun deleteHabit(habitId: Long) {
        habitDao.softDeleteHabit(habitId)
        habitWidgetRefresher.refresh()
    }

    override suspend fun getUnsyncedHabitLogs(): List<HabitLog> =
        habitLogDao.getUnsyncedLogs().map { it.toDomain() }

    override suspend fun markHabitLogSynced(id: Long) =
        habitLogDao.markAsSynced(id)

    // ── Mappers (Entity ↔ Domain) ───────────────────────────
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
