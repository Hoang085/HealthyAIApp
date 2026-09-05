package com.healthyai.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.healthyai.app.data.local.AppDatabase
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.data.local.entity.HabitLogEntity
import com.healthyai.app.domain.model.HabitCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class HabitLogDaoTest {

    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        habitDao = db.habitDao()
        habitLogDao = db.habitLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetLogs() = runBlocking {
        // Must insert a habit first due to ForeignKey
        val habitId = habitDao.insertHabit(
            HabitEntity(name = "Water", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml")
        )

        val log = HabitLogEntity(
            habitId = habitId,
            habitName = "Water",
            category = HabitCategory.WATER,
            value = 250f,
            unit = "ml",
            targetValueAtTime = 2000f,
            loggedAt = System.currentTimeMillis()
        )
        
        habitLogDao.insertLog(log)
        
        val logs = habitLogDao.getLogsFromTimestamp(System.currentTimeMillis() - 1000).first()
        assertEquals(1, logs.size)
        assertEquals(250f, logs[0].value)
    }

    @Test
    @Throws(Exception::class)
    fun syncWorkflow() = runBlocking {
        val habitId = habitDao.insertHabit(
            HabitEntity(name = "Water", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml")
        )

        val log = HabitLogEntity(
            habitId = habitId,
            habitName = "Water",
            category = HabitCategory.WATER,
            value = 250f,
            unit = "ml",
            targetValueAtTime = 2000f,
            isDirty = true
        )
        
        val logId = habitLogDao.insertLog(log)
        
        var unsynced = habitLogDao.getUnsyncedLogs()
        assertEquals(1, unsynced.size)
        
        habitLogDao.markAsSynced(logId)
        
        unsynced = habitLogDao.getUnsyncedLogs()
        assertEquals(0, unsynced.size)
    }
}
