package com.healthyai.app.data.local.entity.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Một thiết lập của người dùng, lưu dạng **khoá – giá trị** trong bảng `app_settings`.
 *
 * Chọn dạng khoá – giá trị (thay vì một hàng có cột cố định) để **thêm thiết lập mới không cần
 * migration nữa** — chỉ cần một khoá mới. Giá trị lưu dạng TEXT; tầng repository chịu trách nhiệm
 * chuyển đổi sang kiểu của domain.
 */
@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String
)
