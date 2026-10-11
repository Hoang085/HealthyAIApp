package com.healthyai.app.data.repository

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.entity.health.HealthDataEntity
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [HealthDataRepository].
 *
 * Ngoài Health Connect, repository này còn dùng hai nguồn **"tự lực" của chính app** làm dự phòng:
 * **cảm biến đếm bước của máy** ([StepCounterRepository]) cho bước chân và **bộ ước lượng giấc ngủ từ
 * cảm biến** ([SleepTrackerRepository]) cho giờ ngủ — nhờ vậy `health_data_history` vẫn có dữ liệu
 * (và AI ở tab Phân tích dùng được) ngay cả khi máy không cài ứng dụng sức khỏe nào.
 *
 * Thứ tự ưu tiên luôn là **Health Connect trước** (dữ liệu có thể gồm cả đồng hồ đeo tay, và với giấc
 * ngủ còn có chi tiết pha Sâu/Nông/REM), chỉ khi Health Connect không có số mới dùng nguồn tự đo.
 *
 * Repository này cũng là nơi **báo cho widget màn hình chính vẽ lại**: mọi lần ghi dữ liệu sức khỏe của
 * hôm nay đều đi qua [upsertToday] — sau đó gọi [HealthWidgetRefresher.refresh] để widget "Sức khỏe hôm
 * nay" hiện số mới ngay, không phải chờ tới nhịp vẽ định kỳ 30 phút.
 */
@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val dataSource: HealthConnectDataSource,
    private val healthDataDao: HealthDataDao,
    private val stepCounterRepository: StepCounterRepository,
    private val sleepTrackerRepository: SleepTrackerRepository,
    private val healthWidgetRefresher: HealthWidgetRefresher
) : HealthDataRepository {

    override suspend fun isAvailable(): Boolean = dataSource.isAvailable()
    override suspend fun getStatus(): HealthConnectStatus = dataSource.getStatus()
    override suspend fun hasAllPermissions(): Boolean = dataSource.hasAllPermissions()
    override suspend fun getRequiredPermissions(): Set<String> = dataSource.requiredPermissions
    override suspend fun getTodaySteps(): Long = dataSource.getTodaySteps()
    override suspend fun getTodaySleepHours(): Double = dataSource.getTodaySleepHours()
    override suspend fun getTodayHeartRate(): Double = dataSource.getTodayHeartRate()

    override suspend fun getTodayHealthData(): HealthData = HealthData(
        steps = dataSource.getTodaySteps(),
        sleepHours = dataSource.getTodaySleepHours(),
        heartRate = dataSource.getTodayHeartRate(),
        activeCalories = dataSource.getTodayActiveCalories()
    )

    override suspend fun syncHealthData(): Result<Unit> = runCatchingCancellable {
        val healthConnect = readHealthConnectSnapshot()
        val sensorSteps = stepCounterRepository.readTodaySteps()
        val sensorSleepHours = sleepTrackerRepository.readLastNightSleepHours()

        // Bước chân: Health Connect trước (có thể gồm cả đồng hồ đeo tay), không có số thì dùng cảm biến của máy.
        // Giấc ngủ: Health Connect trước (có cả chi tiết pha Sâu/Nông/REM), không có thì dùng ước lượng từ
        // cảm biến ánh sáng + gia tốc của chính điện thoại (SleepTrackerRepository).
        val snapshot = HealthData(
            steps = (healthConnect?.steps ?: 0L).takeIf { it > 0L } ?: sensorSteps,
            sleepHours = (healthConnect?.sleepHours ?: 0.0).takeIf { it > 0.0 } ?: sensorSleepHours,
            heartRate = healthConnect?.heartRate ?: 0.0,
            activeCalories = healthConnect?.activeCalories ?: 0.0
        )

        // Không có dữ liệu thật (không có cảm biến, không cấp quyền, Health Connect trống) → không ghi
        // hàng toàn số 0, tránh làm nhiễu phân tích AI ở tab Phân tích.
        if (!snapshot.hasAnyData()) {
            android.util.Log.d(
                "HealthDataRepository",
                "Bỏ qua đồng bộ: chưa có dữ liệu sức khỏe thật (Health Connect trống và cảm biến không có số)"
            )
            return@runCatchingCancellable
        }

        upsertToday(snapshot)

        // Dọn dẹp dữ liệu cũ (hơn 30 ngày)
        healthDataDao.deleteOldData(System.currentTimeMillis() - RETENTION_MS)

        // Vẽ lại widget "Sức khỏe hôm nay" để bước chân / giấc ngủ / nhịp tim vừa đồng bộ hiện ra ngay.
        healthWidgetRefresher.refresh()

        android.util.Log.d(
            "HealthDataRepository",
            "Đồng bộ xong và lưu vào Room: ${snapshot.steps} bước, " +
                "${snapshot.sleepHours} giờ ngủ hôm nay"
        )
    }

    /**
     * Đọc một "bản chụp" từ Health Connect; trả `null` khi Health Connect không khả dụng hoặc chưa được
     * cấp đủ quyền — lúc đó phần bước chân vẫn có thể đến từ cảm biến của máy.
     */
    private suspend fun readHealthConnectSnapshot(): HealthData? {
        if (!dataSource.isAvailable()) return null
        if (!dataSource.hasAllPermissions()) return null

        return HealthData(
            steps = dataSource.getTodaySteps(),
            sleepHours = dataSource.getTodaySleepHours(),
            heartRate = dataSource.getTodayHeartRate(),
            activeCalories = dataSource.getTodayActiveCalories()
        )
    }

    /**
     * Ghi bản chụp vào hàng của **hôm nay**: chưa có thì thêm mới, đã có thì **gộp** vào hàng đó.
     *
     * Gộp thay vì thêm hàng mới vì `HealthSyncWorker` chạy 4 lần/ngày; nếu thêm mỗi lần một hàng thì
     * lịch sử chỉ toàn dữ liệu trùng của cùng một ngày. Quy tắc gộp: **bước chân lấy số lớn hơn**
     * (số bước trong ngày chỉ tăng), các chỉ số khác lấy giá trị **mới hơn nếu có số** — nhờ vậy lần
     * đồng bộ không có quyền Health Connect **không** xoá mất dữ liệu giấc ngủ/nhịp tim đã ghi trước đó.
     */
    private suspend fun upsertToday(snapshot: HealthData) {
        val existing = healthDataDao.getLatestSince(startOfToday())

        if (existing == null) {
            healthDataDao.insertHealthData(HealthDataEntity.fromDomain(snapshot))
            return
        }

        healthDataDao.updateHealthData(
            existing.copy(
                steps = maxOf(existing.steps, snapshot.steps),
                sleepHours = if (snapshot.sleepHours > 0.0) snapshot.sleepHours else existing.sleepHours,
                heartRate = if (snapshot.heartRate > 0.0) snapshot.heartRate else existing.heartRate,
                activeCalories = if (snapshot.activeCalories > 0.0) snapshot.activeCalories else existing.activeCalories,
                recordedAt = System.currentTimeMillis()
            )
        )
    }

    /** Mốc 00:00 của **hôm nay** theo múi giờ thiết bị (epoch millis) — mọi mốc "hôm nay" đều dùng chung. */
    private fun startOfToday(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** Bản chụp có ít nhất một chỉ số thật (khác 0) hay không. */
    private fun HealthData.hasAnyData(): Boolean =
        steps > 0L || sleepHours > 0.0 || heartRate > 0.0 || activeCalories > 0.0

    private companion object {
        /** Giữ lịch sử sức khỏe 30 ngày (dọn mỗi lần đồng bộ). */
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }

    override suspend fun getRecentHealthHistory(limit: Int): List<HealthData> {
        return healthDataDao.getRecentHealthDataList(limit.coerceAtLeast(1)).map { it.toDomain() }
    }

    override suspend fun getLatestRecordedHeartRate(): Double? =
        healthDataDao.getLatestHeartRateSince(startOfToday())?.takeIf { it > 0.0 }

    /**
     * Lưu nhịp tim vừa đo vào hàng của **hôm nay**.
     *
     * Dùng lại [upsertToday] nên giữ nguyên luật gộp: các chỉ số khác của hôm nay (bước chân, giờ ngủ,
     * calo) không bị ghi đè bởi hàng mới chỉ có nhịp tim.
     *
     * Sau khi ghi phải vẽ lại widget màn hình chính, nếu không ô "Nhịp tim" trên widget vẫn là số của
     * lần đo trước cho tới nhịp vẽ định kỳ kế tiếp (tối đa 30 phút).
     */
    override suspend fun saveMeasuredHeartRate(bpm: Double): Result<Unit> = runCatchingCancellable {
        require(bpm > 0.0) { "Nhịp tim đo được phải lớn hơn 0 (nhận được $bpm)" }
        upsertToday(HealthData(heartRate = bpm, recordedAt = System.currentTimeMillis()))
        healthWidgetRefresher.refresh()
    }

    /**
     * Dòng mới nhất của mỗi ngày, tối đa [limit] ngày gần nhất.
     *
     * Lưu ý dữ liệu: `health_data_history` ghi 1 hàng cho mỗi lần **đồng bộ** (6 giờ/lần, đã gộp vào
     * hàng của ngày) nên tầng dùng (Rule Engine) vẫn phải gom theo ngày — xem
     * `HabitRuleEngine.analyzeHealthTrends` (phụ lục C #16).
     */
    override fun getRecentHealthHistoryFlow(limit: Int): Flow<List<HealthData>> {
        return healthDataDao.getRecentHealthData(limit.coerceAtLeast(1)).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Bản chụp sức khỏe của **hôm nay** đã ghi trong `health_data_history` (hàng mới nhất được ghi từ
     * đầu ngày), hoặc [HealthData] rỗng khi hôm nay chưa có hàng nào.
     *
     * Nguồn cho thẻ "Tóm tắt sức khỏe" khi người dùng **tắt** switch "Dùng dữ liệu Health Connect".
     */
    override suspend fun getTodayStoredHealthData(): HealthData =
        healthDataDao.getLatestSince(startOfToday())?.toDomain() ?: HealthData()

    override fun getHealthHistorySince(fromTs: Long): Flow<List<HealthData>> =
        healthDataDao.getHealthDataSince(fromTs).map { list -> list.map { it.toDomain() } }

    /**
     * Đọc chi tiết pha giấc ngủ trong [daysBack] ngày gần nhất **trực tiếp từ Health Connect**
     * (không qua bảng `health_data_history`) để thẻ "Xu hướng chất lượng giấc ngủ" luôn có
     * dữ liệu mới nhất mà không cần đổi schema Room.
     *
     * Khoảng thời gian được căn theo đầu ngày (00:00), thống nhất với truy vấn log thói quen.
     */
    override suspend fun getSleepStageBreakdown(daysBack: Int): SleepStageBreakdown {
        if (!dataSource.isAvailable()) return SleepStageBreakdown()

        val zone = ZoneId.systemDefault()
        val end = Instant.now()
        val start = LocalDate.now(zone)
            .minusDays(daysBack.coerceAtLeast(0).toLong())
            .atStartOfDay(zone)
            .toInstant()

        return dataSource.getSleepStageBreakdown(start, end)
    }
}
