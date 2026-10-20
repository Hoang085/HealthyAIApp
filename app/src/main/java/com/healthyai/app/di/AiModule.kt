package com.healthyai.app.di

import com.healthyai.app.BuildConfig
import com.healthyai.app.data.vision.GeminiFoodInsightSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/** Cấu hình AI (Gemini) lấy từ BuildConfig/local.properties (không vào git); thiếu khoá thì báo "chưa cấu hình" thay vì lỗi mạng. */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Named(GeminiFoodInsightSource.NAMED_API_KEY)
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Named(GeminiFoodInsightSource.NAMED_MODEL)
    fun provideGeminiModel(): String = BuildConfig.GEMINI_MODEL

    /** Model dự phòng, chỉ dùng khi model chính trả 503/429 (Google quá tải/hết quota); rỗng thì chỉ dùng model chính. */
    @Provides
    @Named(GeminiFoodInsightSource.NAMED_FALLBACK_MODEL)
    fun provideGeminiFallbackModel(): String = BuildConfig.GEMINI_FALLBACK_MODEL
}
