package com.healthyai.app.di

import android.content.Context
import androidx.room.Room
import com.healthyai.app.data.local.AppDatabase
import com.healthyai.app.data.local.dao.HabitDao
import com.healthyai.app.data.local.dao.HabitLogDao
import com.healthyai.app.data.local.dao.health.HealthDataDao
import com.healthyai.app.data.local.dao.settings.SettingsDao
import com.healthyai.app.data.local.migration.DatabaseMigrations
import com.healthyai.app.data.local.security.AndroidKeystorePassphraseCipher
import com.healthyai.app.data.local.security.DatabasePassphraseProvider
import com.healthyai.app.data.local.security.LegacyDatabaseKeyMigration
import com.healthyai.app.data.local.security.SharedPreferencesPassphraseVault
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

        // Passphrase KHÔNG còn hard-code (mục 3 #17 / phụ lục C #13): sinh ngẫu nhiên 32 byte ở
        // lần chạy đầu rồi bọc bằng khoá AES-256 trong Android Keystore; các lần sau đọc lại khoá cũ.
        val passphrase = DatabasePassphraseProvider(
            vault = SharedPreferencesPassphraseVault(context),
            cipher = AndroidKeystorePassphraseCipher()
        ).getOrCreate()

        if (passphrase.newlyCreated) {
            // Máy vừa có passphrase mới ⇒ mọi file DB còn sót lại là của bản cũ (khoá hard-code).
            // Rekey sang khoá mới để không mất dữ liệu thói quen/nhật ký của người dùng.
            LegacyDatabaseKeyMigration.rekeyLegacyDatabaseIfPresent(
                context = context,
                databaseName = AppDatabase.DATABASE_NAME,
                newPassphrase = passphrase.bytes
            )
        }

        // sqlcipher-android 4.x không còn SQLiteDatabase.getBytes() công khai —
        // truyền thẳng byte[] như hướng dẫn chính thức của Zetetic.
        val factory = SupportOpenHelperFactory(passphrase.bytes)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)             // SQLCipher AES-256 encryption
            // Phụ lục C #14: KHÔNG dùng fallbackToDestructiveMigration() nữa — nâng version schema
            // sẽ chạy Migration giữ nguyên dữ liệu thay vì xoá sạch bảng.
            .addMigrations(*DatabaseMigrations.ALL.toTypedArray())
            .build()
    }

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitLogDao(db: AppDatabase): HabitLogDao = db.habitLogDao()

    @Provides
    fun provideHealthDataDao(db: AppDatabase): HealthDataDao = db.healthDataDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
