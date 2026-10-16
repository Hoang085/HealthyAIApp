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

/** Cung cấp Room Database được mã hoá bởi SQLCipher. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // sqlcipher-android không tự nạp native lib — phải System.loadLibrary("sqlcipher") 1 lần trước khi mở DB (gọi lặp an toàn).
        System.loadLibrary("sqlcipher")

        // Passphrase sinh ngẫu nhiên 32 byte, bọc bằng khoá AES-256 trong Android Keystore; các lần sau đọc lại (không hard-code).
        val passphrase = DatabasePassphraseProvider(
            vault = SharedPreferencesPassphraseVault(context),
            cipher = AndroidKeystorePassphraseCipher()
        ).getOrCreate()

        if (passphrase.newlyCreated) {
            // Có passphrase mới ⇒ file DB cũ dùng khoá hard-code; rekey sang khoá mới để không mất dữ liệu.
            LegacyDatabaseKeyMigration.rekeyLegacyDatabaseIfPresent(
                context = context,
                databaseName = AppDatabase.DATABASE_NAME,
                newPassphrase = passphrase.bytes
            )
        }

        // sqlcipher-android 4.x bỏ SQLiteDatabase.getBytes() công khai — truyền thẳng byte[] như hướng dẫn Zetetic.
        val factory = SupportOpenHelperFactory(passphrase.bytes)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)             // mã hoá SQLCipher AES-256
            // Không dùng fallbackToDestructiveMigration(): nâng version schema chạy Migration giữ dữ liệu thay vì xoá sạch bảng.
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
