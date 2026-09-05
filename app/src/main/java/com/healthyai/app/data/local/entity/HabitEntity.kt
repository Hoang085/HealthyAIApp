package com.healthyai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitFrequency
import com.healthyai.app.domain.model.HabitTimeOfDay

/**
 * Room Entity — bảng "habits".
 * Ánh xạ 1-1 với bảng trong SQLite database (được mã hóa bởi SQLCipher).
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: HabitCategory,
    val targetValue: Float,
    val unit: String,
    val timeOfDay: HabitTimeOfDay = HabitTimeOfDay.ALL_DAY,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
