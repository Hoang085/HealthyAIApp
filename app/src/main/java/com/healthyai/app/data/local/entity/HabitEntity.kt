package com.healthyai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity — bảng "habits".
 * Ánh xạ 1-1 với bảng trong SQLite database (được mã hóa bởi SQLCipher).
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,       // Lưu dạng String của enum HabitCategory
    val targetValue: Float,
    val unit: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
