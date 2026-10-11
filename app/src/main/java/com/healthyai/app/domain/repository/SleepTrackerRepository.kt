package com.healthyai.app.domain.repository

/**
 * **Đo số giờ ngủ** bằng cảm biến của thiết bị (ánh sáng + gia tốc + trạng thái màn hình), **trong khung
 * giờ ngủ do người dùng đặt** (`SleepSchedule`, lưu ở bảng `app_settings`).
 *
 * Cùng vai trò với `StepCounterRepository` ở phần đếm bước chân: đây là nguồn **"tự lực"** của app,
 * nên tab Phân tích và thẻ "Tóm tắt sức khỏe" vẫn có số giờ ngủ ngay cả khi máy **không cài** ứng
 * dụng sức khỏe nào và người dùng **không cấp** quyền Health Connect.
 *
 * Số liệu của **đồng hồ đeo tay** (nằm trong Health Connect) luôn được **ưu tiên** khi có — repository
 * này chỉ là nguồn dự phòng (xem `HealthDataRepositoryImpl.syncHealthData`).
 *
 * Toàn bộ phép tính nằm trong hàm thuần `SleepTrackerMath`; lớp này chỉ ghép cảm biến ↔ bộ nhớ.
 */
interface SleepTrackerRepository {

    /**
     * Số giờ ngủ của **đêm vừa qua** (đêm gắn với ngày thức dậy, xem `SleepTrackerMath.nightIsoOf`).
     *
     * Trả `0.0` khi máy không có cảm biến, chưa lấy được mẫu nào, hoặc số phút ghi nhận còn dưới
     * ngưỡng coi là một giấc ngủ.
     */
    suspend fun readLastNightSleepHours(): Double

    /**
     * Đọc môi trường hiện tại và cập nhật phiên ngủ (dùng cho `SleepTrackingWorker`), theo khung giờ
     * ngủ người dùng đang đặt.
     *
     * @return số phút ngủ của đêm đang xét sau mẫu này.
     */
    suspend fun sampleAndStore(): Long
}
