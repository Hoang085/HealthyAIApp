package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.HealthConnectDataSettings
import com.healthyai.app.domain.model.SleepSchedule
import kotlinx.coroutines.flow.Flow

/** Thiết lập người dùng lưu trong bảng Room `app_settings` dạng khoá–giá trị, thêm thiết lập mới không cần nâng schema. */
interface SettingsRepository {

    /** Khung giờ ngủ đã đặt, phát lại khi thiết lập đổi; mặc định [SleepSchedule.DEFAULT] (23:00 → 07:00). */
    fun observeSleepSchedule(): Flow<SleepSchedule>

    /** Đọc một lần khung giờ ngủ (dùng cho worker, không cần theo dõi thay đổi). */
    suspend fun getSleepSchedule(): SleepSchedule

    /** Lưu khung giờ ngủ; bỏ qua nếu không hợp lệ (`SleepSchedule.isValid` = `false`) để không phá dữ liệu đang đo. */
    suspend fun setSleepSchedule(schedule: SleepSchedule)

    /** Trạng thái đồng bộ nhật ký lên đám mây, phát lại khi thiết lập đổi; mặc định tắt nên không tải dữ liệu lên Firestore. */
    fun observeCloudSync(): Flow<CloudSyncSettings>

    /** Đọc một lần trạng thái đồng bộ đám mây (dùng cho worker, không cần theo dõi thay đổi). */
    suspend fun getCloudSync(): CloudSyncSettings

    /** Bật/tắt đồng bộ đám mây; khi bật chỉ đồng bộ nhật ký tạo sau [nowMillis] (để test), khi tắt không xoá dữ liệu đã tải lên. */
    suspend fun setCloudSyncEnabled(enabled: Boolean, nowMillis: Long = System.currentTimeMillis())

    /** Nguồn dữ liệu cho thẻ "Tóm tắt sức khỏe", phát lại khi thiết lập đổi; mặc định tắt nên dùng dữ liệu đã lưu trong app. */
    fun observeHealthConnectData(): Flow<HealthConnectDataSettings>

    /** Đọc một lần nguồn dữ liệu đang chọn, cho nơi cần quyết định ngay mà không phụ thuộc thời điểm Flow phát. */
    suspend fun getHealthConnectData(): HealthConnectDataSettings

    /** Bật/tắt việc dùng dữ liệu Health Connect cho thẻ "Tóm tắt sức khỏe". */
    suspend fun setHealthConnectDataEnabled(enabled: Boolean)
}
