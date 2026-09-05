package com.healthyai.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.healthyai.app.data.local.AppDatabase
import com.healthyai.app.data.local.entity.HabitEntity
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitFrequency
import com.healthyai.app.domain.model.HabitTimeOfDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var habitDao: HabitDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-memory database for testing
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        habitDao = db.habitDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetHabit() = runBlocking {
        val habit = HabitEntity(
            name = "Test Habit",
            category = HabitCategory.WATER,
            targetValue = 2000f,
            unit = "ml",
            timeOfDay = HabitTimeOfDay.MORNING,
            frequency = HabitFrequency.DAILY
        )
        val id = habitDao.insertHabit(habit)
        val retrievedHabit = habitDao.getHabitById(id)
        assertEquals(habit.name, retrievedHabit?.name)
        assertEquals(HabitCategory.WATER, retrievedHabit?.category)
    }

    @Test
    @Throws(Exception::class)
    fun getAllActiveHabits() = runBlocking {
        val habit1 = HabitEntity(name = "Active", category = HabitCategory.WATER, targetValue = 1f, unit = "u", isActive = true)
        val habit2 = HabitEntity(name = "Inactive", category = HabitCategory.WATER, targetValue = 1f, unit = "u", isActive = false)
        
        habitDao.insertHabit(habit1)
        habitDao.insertHabit(habit2)
        
        val activeHabits = habitDao.getAllActiveHabits().first()
        assertEquals(1, activeHabits.size)
        assertEquals("Active", activeHabits[0].name)
    }

    @Test
    @Throws(Exception::class)
    fun softDeleteHabit() = runBlocking {
        val habit = HabitEntity(name = "To Delete", category = HabitCategory.WATER, targetValue = 1f, unit = "u", isActive = true)
        val id = habitDao.insertHabit(habit)
        
        habitDao.softDeleteHabit(id)
        
        val retrievedHabit = habitDao.getHabitById(id)
        assertEquals(false, retrievedHabit?.isActive)
        
        val activeHabits = habitDao.getAllActiveHabits().first()
        assertTrue(activeHabits.isEmpty())
    }
}
