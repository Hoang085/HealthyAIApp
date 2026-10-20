package com.healthyai.app.presentation.widget

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.health.GetLatestRecordedHeartRateUseCase
import com.healthyai.app.domain.usecase.health.GetStoredHealthDataUseCase
import com.healthyai.app.domain.usecase.health.TodayHealthSummary
import com.healthyai.app.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Đọc ba chỉ số của widget "Sức khỏe hôm nay" tại **thời điểm gọi**.
 *
 * Tách khỏi [HealthWidget] vì có **hai** nơi cần số liệu:
 * - lần vẽ đầu tiên của widget ([HealthWidget.provideGlance] — không được đọc dữ liệu lại trong
 *   composition), và
 * - mỗi lần app muốn vẽ lại (bước chân đổi, đồng bộ xong, vừa đo nhịp tim) —
 *   [HealthWidgetRefresherImpl.refresh], nơi không có composition nào để đọc dữ liệu.
 *
 * Nhờ vậy hai đường đi dùng **chung một luật** với thẻ "Tóm tắt sức khỏe" trên Trang chủ.
 */
@Singleton
class HealthWidgetSummaryReader @Inject constructor(
    private val stepCounterRepository: StepCounterRepository,
    private val sleepTrackerRepository: SleepTrackerRepository,
    private val settingsRepository: SettingsRepository,
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val getStoredHealthDataUseCase: GetStoredHealthDataUseCase,
    private val getLatestRecordedHeartRateUseCase: GetLatestRecordedHeartRateUseCase
) {

    /**
     * Ghép ba chỉ số bằng đúng luật của thẻ "Tóm tắt sức khỏe" ([TodayHealthSummary.build]): đọc
     * **cùng nguồn** mà thẻ đang chọn — switch "Dùng dữ liệu Health Connect" BẬT thì đọc thẳng Health
     * Connect, TẮT (mặc định) thì đọc bản chụp đã lưu trong app — rồi mới rơi về số app tự đo.
     */
    suspend fun read(): TodayHealthSummary {
        val stepCounterStatus = stepCounterRepository.getStatus()

        // `NO_WAIT_MS`: lấy ngay số đã đếm, không chờ mẫu cảm biến mới (nơi gọi không được phép chờ).
        val sensorSteps = if (stepCounterStatus.isReady) {
            stepCounterRepository.readTodaySteps(StepCounterRepository.NO_WAIT_MS)
        } else {
            0L
        }
        val sensorSleepHours = runCatchingCancellable {
            sleepTrackerRepository.readLastNightSleepHours()
        }.getOrDefault(0.0)

        return TodayHealthSummary.build(
            primary = readPrimaryHealthData(),
            sensorSteps = sensorSteps,
            sensorSleepHours = sensorSleepHours,
            recordedHeartRate = getLatestRecordedHeartRateUseCase()
        )
    }

    /**
     * Nguồn số liệu đang chọn trên Trang chủ. Đọc lỗi coi như rỗng để widget vẫn hiện được phần số
     * app tự đo (bước chân từ cảm biến, giờ ngủ ước lượng, nhịp tim đã ghi) thay vì trắng cả widget.
     */
    private suspend fun readPrimaryHealthData(): HealthData {
        val useHealthConnect = runCatchingCancellable {
            settingsRepository.getHealthConnectData().enabled
        }.getOrDefault(false)

        return if (useHealthConnect) {
            runCatchingCancellable { getHealthDataUseCase() }
                .getOrNull()?.getOrNull() ?: HealthData()
        } else {
            runCatchingCancellable { getStoredHealthDataUseCase() }.getOrDefault(HealthData())
        }
    }
}
