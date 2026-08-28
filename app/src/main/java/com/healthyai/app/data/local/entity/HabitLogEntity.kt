package com.healthyai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity — bảng "habit_logs".
 * isDirty = true: chưa đồng bộ lên cloud (Last Write Wins strategy cho conflict resolution).
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE // Xóa habit → tự xóa log liên quan
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["loggedAt"])]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val habitName: String,
    val category: String,
    val value: Float,
    val unit: String,
    val loggedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val isDirty: Boolean = true  // Offline-first: true = chưa sync lên Firebase
)
