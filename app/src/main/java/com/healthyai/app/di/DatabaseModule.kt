package com.healthyai.app.di

import android.content.Context
import androidx.room.Room
import com.healthyai.app.data.local.AppDatabase
import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.dao.health.HealthDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * DatabaseModule — Cung cấp Room Database được mã hóa bởi SQLCipher.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // sqlcipher-android KHÔNG tự nạp native lib — phải gọi tường minh 1 lần
        // trước khi mở database (an toàn khi gọi lặp lại nhiều lần).
        System.loadLibrary("sqlcipher")

        // sqlcipher-android 4.x không còn SQLiteDatabase.getBytes() công khai —
        // dùng UTF-8 bytes như hướng dẫn chính thức của Zetetic.
        val passphrase = "REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE".toByteArray(Charsets.UTF_8)
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)             // SQLCipher AES-256 encryption
            .fallbackToDestructiveMigration()       // Cập nhật: Không truyền tham số true
            .build()
    }

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitLogDao(db: AppDatabase): HabitLogDao = db.habitLogDao()

    @Provides
    fun provideHealthDataDao(db: AppDatabase): HealthDataDao = db.healthDataDao()
}