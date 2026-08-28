package com.healthyai.app.di

import com.healthyai.app.data.repository.HabitRepositoryImpl
import com.healthyai.app.data.repository.HealthDataRepositoryImpl
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.HealthDataRepository
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
}
