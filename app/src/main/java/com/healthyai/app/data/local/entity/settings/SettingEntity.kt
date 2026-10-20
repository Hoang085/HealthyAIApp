package com.healthyai.app.data.local.entity.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Thiết lập người dùng dạng khoá – giá trị; thêm thiết lập mới không cần migration. */
@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String
)
