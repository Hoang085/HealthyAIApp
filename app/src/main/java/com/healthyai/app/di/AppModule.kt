package com.healthyai.app.di

import com.healthyai.app.data.healthconnect.HealthConnectDataSource
import com.healthyai.app.data.healthconnect.HealthConnectDataSourceImpl
import com.healthyai.app.data.repository.AuthRepositoryImpl
import com.healthyai.app.data.repository.HabitRepositoryImpl
import com.healthyai.app.data.repository.HealthDataRepositoryImpl
import com.healthyai.app.data.repository.NotificationPermissionRepositoryImpl
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
import com.healthyai.app.domain.repository.NotificationPermissionRepository
import com.healthyai.app.domain.widget.HabitWidgetRefresher
import com.healthyai.app.presentation.widget.HabitWidgetRefresherImpl
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
     * Widget màn hình chính: Domain gọi [HabitWidgetRefresher.refresh] mỗi khi dữ liệu thói quen đổi,
     * implementation thật (Glance `updateAll`) nằm ở tầng Presentation (mục 3 #15).
     */
    @Binds
    @Singleton
    abstract fun bindHabitWidgetRefresher(impl: HabitWidgetRefresherImpl): HabitWidgetRefresher
}
