package com.healthyai.app.di

import android.content.Context
import androidx.room.Room
import com.healthyai.app.data.local.AppDatabase
import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
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
        // SQLCipher cung cấp sẵn hàm getBytes() để chuyển charArray sang byte array an toàn hơn
        val passphrase = SQLiteDatabase.getBytes("REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE".toCharArray())
        val factory = SupportFactory(passphrase)

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
}