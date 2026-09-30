package com.healthyai.app.di

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.healthconnect.HealthConnectDataSourceImpl
import com.healthyai.app.data.repository.AuthRepositoryImpl
import com.healthyai.app.data.repository.HabitRepositoryImpl
import com.healthyai.app.data.repository.HealthDataRepositoryImpl
import com.healthyai.app.data.repository.NotificationPermissionRepositoryImpl
import com.healthyai.app.data.repository.SettingsRepositoryImpl
import com.healthyai.app.data.repository.SleepTrackerRepositoryImpl
import com.healthyai.app.data.repository.StepCounterRepositoryImpl
import com.healthyai.app.data.network.HttpPost
import com.healthyai.app.data.network.HttpUrlConnectionPost
import com.healthyai.app.data.sleep.AndroidSleepSensor
import com.healthyai.app.data.sleep.SleepSensor
import com.healthyai.app.data.steps.AndroidStepCounterSensor
import com.healthyai.app.data.steps.StepCounterSensor
import com.healthyai.app.data.vision.FoodInsightSource
import com.healthyai.app.data.vision.GeminiFoodInsightSource
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.repository.NotificationPermissionRepository
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.presentation.widget.HealthWidgetRefresherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule — Bind interface với implementation.
 * Hilt sẽ tự inject đúng implementation khi code yêu cầu interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindHealthDataRepository(impl: HealthDataRepositoryImpl): HealthDataRepository

    @Binds
    @Singleton
    abstract fun bindHealthConnectDataSource(impl: HealthConnectDataSourceImpl): HealthConnectDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPermissionRepository(
        impl: NotificationPermissionRepositoryImpl
    ): NotificationPermissionRepository

    /**
     * Đếm bước bằng cảm biến của máy (nguồn "tự lực" của app, không cần Health Connect).
     */
    @Binds
    @Singleton
    abstract fun bindStepCounterSensor(impl: AndroidStepCounterSensor): StepCounterSensor

    @Binds
    @Singleton
    abstract fun bindStepCounterRepository(impl: StepCounterRepositoryImpl): StepCounterRepository

    /**
     * Tự tính giờ ngủ bằng cảm biến của máy (nguồn "tự lực" thứ hai, không cần Health Connect và
     * không cần quyền nào).
     */
    @Binds
    @Singleton
    abstract fun bindSleepSensor(impl: AndroidSleepSensor): SleepSensor

    @Binds
    @Singleton
    abstract fun bindSleepTrackerRepository(impl: SleepTrackerRepositoryImpl): SleepTrackerRepository

    /**
     * Thiết lập của người dùng (bảng `app_settings`) — hiện có khung giờ ngủ.
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /**
     * Widget màn hình chính "Sức khỏe hôm nay": Domain gọi [HealthWidgetRefresher.refresh] mỗi khi app
     * ghi dữ liệu sức khỏe (đồng bộ xong / vừa đo nhịp tim), implementation thật (Glance `updateAll`)
     * nằm ở tầng Presentation.
     */
    @Binds
    @Singleton
    abstract fun bindHealthWidgetRefresher(impl: HealthWidgetRefresherImpl): HealthWidgetRefresher

    /**
     * AI phân tích món ăn ở màn "Ghi chép bữa ăn" (mục 3 #42).
     *
     * Thay ML Kit Image Labeling cũ: ML Kit chỉ gán **nhãn ảnh chung** tiếng Anh nên không trả lời được
     * "đây là món gì" (nguồn của lỗi *"chụp đĩa cơm mà ra pizza"*). Impl mới gọi Gemini để lấy đúng
     * tên món kèm lợi ích/điều nên lưu ý; việc gửi ảnh đi chỉ xảy ra khi người dùng đã bật opt-in.
     */
    @Binds
    @Singleton
    abstract fun bindFoodInsightSource(impl: GeminiFoodInsightSource): FoodInsightSource

    /** Cổng HTTP POST ra ngoài (`HttpURLConnection`) — tách interface để test JVM được. */
    @Binds
    abstract fun bindHttpPost(impl: HttpUrlConnectionPost): HttpPost
}
