package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.SleepSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Thiết lập của người dùng, lưu trong bảng Room `app_settings` (khoá/giá trị).
 *
 * Bảng dùng dạng **khoá – giá trị** để thêm thiết lập mới sau này **không cần** nâng version schema
 * thêm lần nữa; mỗi thiết lập chỉ cần một `name` riêng.
 */
interface SettingsRepository {

    /**
     * Khung giờ ngủ người dùng đã đặt, phát lại mỗi khi thiết lập đổi.
     *
     * Chưa đặt gì → [SleepSchedule.DEFAULT] (23:00 → 07:00), nên app luôn có khung để đo.
     */
    fun observeSleepSchedule(): Flow<SleepSchedule>

    /** Đọc một lần khung giờ ngủ (dùng cho worker, không cần theo dõi thay đổi). */
    suspend fun getSleepSchedule(): SleepSchedule

    /**
     * Lưu khung giờ ngủ. Bỏ qua (không ghi gì) nếu khung không hợp lệ
     * (`SleepSchedule.isValid` = `false`) để không tự phá dữ liệu đang đo.
     */
    suspend fun setSleepSchedule(schedule: SleepSchedule)

    /**
     * Trạng thái **đồng bộ nhật ký lên đám mây**, phát lại mỗi khi thiết lập đổi.
     *
     * Mặc định [CloudSyncSettings.enabled] = `false` — người dùng chưa bật thì app **không** tải bất
     * kỳ dữ liệu nào lên Firestore (mục 3 #31).
     */
    fun observeCloudSync(): Flow<CloudSyncSettings>

    /** Đọc một lần trạng thái đồng bộ đám mây (dùng cho worker, không cần theo dõi thay đổi). */
    suspend fun getCloudSync(): CloudSyncSettings

    /**
     * Bật/tắt đồng bộ đám mây.
     *
     * - **Bật**: ghi cờ bật **và** mốc đồng ý = [nowMillis] ⇒ chỉ nhật ký tạo **sau** thời điểm này
     *   mới được tải lên (bật xong **không** đẩy lịch sử cũ lên đám mây).
     * - **Tắt**: chỉ đặt cờ tắt; **không** xoá dữ liệu đã tải lên trước đó (đã chốt với người dùng).
     *
     * @param nowMillis thời điểm bật, truyền vào để test được.
     */
    suspend fun setCloudSyncEnabled(enabled: Boolean, nowMillis: Long = System.currentTimeMillis())
}
