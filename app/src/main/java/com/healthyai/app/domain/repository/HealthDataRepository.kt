package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface cho dữ liệu Health Connect.
 */
interface HealthDataRepository {
    /** Kiểm tra Health Connect có sẵn sàng trên thiết bị không */
    suspend fun isAvailable(): Boolean

    /** Trạng thái Health Connect: sẵn sàng / chưa cài / cần cập nhật */
    suspend fun getStatus(): HealthConnectStatus

    /** Người dùng đã cấp đủ quyền đọc cần thiết chưa */
    suspend fun hasAllPermissions(): Boolean

    /** Trả về danh sách permissions cần xin */
    suspend fun getRequiredPermissions(): Set<String>

    suspend fun getTodaySteps(): Long
    suspend fun getTodaySleepHours(): Double

    /**
     * Nhịp tim **trung bình của ngày hôm nay** (00:00 → hiện tại) đọc từ Health Connect, `0.0` khi
     * thiếu quyền hoặc hôm nay chưa có mẫu nào.
     */
    suspend fun getTodayHeartRate(): Double

    /**
     * Bản chụp dữ liệu sức khỏe **của hôm nay** đọc trực tiếp từ Health Connect (bước chân, giờ ngủ,
     * nhịp tim, calo).
     *
     * Trước đây hàm này nhận hai tham số `start`/`end` nhưng **bỏ qua cả hai** (luôn trả số của hôm
     * nay) trong khi `GetHealthDataUseCase` truyền `Instant.now()` hai lần — một hợp đồng sai khiến
     * người đọc tưởng có thể lấy được dải thời gian bất kỳ (phụ lục C #42). Nay tên hàm nói đúng việc
     * nó làm: **dữ liệu hôm nay**. Muốn dữ liệu theo ngày đã qua thì đọc `health_data_history` qua
     * [getHealthHistorySince].
     */
    suspend fun getTodayHealthData(): HealthData
    /**
     * Lưu một "bản chụp" dữ liệu sức khỏe vào `health_data_history` cho **hôm nay**.
     *
     * Nguồn dữ liệu: bước chân và **giờ ngủ** lấy từ Health Connect trước, nếu Health Connect trống
     * thì lấy từ nguồn **tự đo của app** (**cảm biến đếm bước của máy** cho bước chân,
     * **bộ ước lượng giấc ngủ từ cảm biến** cho giờ ngủ); nhịp tim / calo chỉ có từ Health Connect.
     *
     * - Chưa có hàng nào của hôm nay → thêm mới.
     * - Đã có hàng của hôm nay → **gộp** vào hàng đó (bước chân lấy số lớn hơn) thay vì thêm hàng trùng.
     * - Không có chỉ số nào thật (khác 0) → **không ghi gì** để không tạo hàng toàn số 0 làm nhiễu AI.
     */
    suspend fun syncHealthData(): Result<Unit>

    /** Lấy danh sách dữ liệu sức khỏe lịch sử */
    suspend fun getRecentHealthHistory(limit: Int): List<HealthData>

    /**
     * Nhịp tim **ghi gần nhất trong ngày hôm nay** (lấy từ `health_data_history`), `null` khi hôm nay
     * chưa có số nào.
     *
     * Nguồn của con số này có thể là **lần đo bằng camera của app** hoặc **lần đồng bộ Health Connect
     * trong ngày** — bảng lịch sử không phân biệt hai nguồn. Màn Trang chủ dùng nó làm **dự phòng** khi
     * Health Connect hiện không trả về nhịp tim nào.
     *
     * Con số được giới hạn trong **ngày hôm nay** để nhịp tim cũng "đọc lại từ đầu ngày" như bước chân
     * và giờ ngủ: sang ngày mới mà chưa đo lại thì ô "Tim" về "chưa có số" thay vì hiện lại số của hôm
     * qua (số cũ vẫn nằm nguyên trong lịch sử 7 ngày ở tab Phân tích).
     */
    suspend fun getLatestRecordedHeartRate(): Double?

    /**
     * Lưu nhịp tim **do app tự đo** (camera + đèn flash) vào `health_data_history` của **hôm nay**.
     *
     * Ghi theo đúng luật gộp của [syncHealthData]: chưa có hàng của hôm nay thì thêm mới, đã có thì
     * **chỉ thay nhịp tim** — bước chân, giờ ngủ và calo đã ghi trước đó trong ngày **không** bị xoá.
     * Nhờ vậy số nhịp tim này dùng được ngay cho tab Phân tích (Rule Engine đọc lịch sử sức khỏe).
     *
     * Trả về `Result.failure` khi [bpm] không hợp lệ (≤ 0) hoặc ghi DB thất bại.
     */
    suspend fun saveMeasuredHeartRate(bpm: Double): Result<Unit>

    /** Flow danh sách dữ liệu sức khỏe gần đây (Reactive) */
    fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>>

    /**
     * Bản chụp dữ liệu sức khỏe **đã lưu trong app** cho **hôm nay** (`health_data_history`), trả
     * [HealthData] rỗng khi hôm nay chưa có hàng nào.
     *
     * Đây là nguồn cho thẻ "Tóm tắt sức khỏe" khi người dùng **tắt** switch "Dùng dữ liệu Health
     * Connect": các ô lấy số từ bản chụp app đã ghi (do `HealthSyncWorker` đồng bộ, hoặc lần đo nhịp tim
     * bằng camera), không đọc Health Connect tại chỗ.
     */
    suspend fun getTodayStoredHealthData(): HealthData

    /**
     * Flow **toàn bộ** hàng lịch sử sức khỏe ghi từ [fromTs] trở đi (mới nhất trước).
     *
     * Khác [getRecentHealthHistoryFlow] (cắt theo **số hàng**), truy vấn này cắt theo **mốc thời gian**
     * nên luôn đủ dữ liệu của cả một khoảng ngày: dữ liệu cũ có thể có nhiều hàng trong cùng một ngày,
     * cắt theo số hàng sẽ làm thiếu ngày. Tầng dùng tự gom lại theo ngày — xem
     * [com.healthyai.app.domain.usecase.health.GetRecentHealthSummaryUseCase].
     */
    fun getHealthHistorySince(fromTs: Long): Flow<List<HealthData>>

    /**
     * Tổng thời lượng từng pha giấc ngủ (Deep/Light/REM) trong [daysBack] ngày gần nhất,
     * đọc trực tiếp từ Health Connect (không qua bảng `health_data_history`).
     * Trả về [SleepStageBreakdown] rỗng khi Health Connect không khả dụng hoặc thiếu quyền.
     */
    suspend fun getSleepStageBreakdown(daysBack: Int): SleepStageBreakdown
}
