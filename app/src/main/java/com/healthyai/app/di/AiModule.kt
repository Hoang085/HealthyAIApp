package com.healthyai.app.di

import com.healthyai.app.BuildConfig
import com.healthyai.app.data.vision.GeminiFoodInsightSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Cấu hình cho tính năng **AI phân tích món ăn** (Gemini — mục 3 #42, #43).
 *
 * Giá trị lấy từ `BuildConfig`, tức là từ `local.properties` lúc build (`gemini.api.key`,
 * `gemini.model`, `gemini.fallback.model` — xem `app/build.gradle.kts`). Khoá **không** nằm trong mã
 * nguồn và `local.properties` đã bị `.gitignore`, nên khoá không bao giờ vào git.
 *
 * Máy chưa cấu hình khoá ⇒ [BuildConfig.GEMINI_API_KEY] là chuỗi rỗng ⇒
 * `GeminiFoodInsightSource` trả `NOT_CONFIGURED` và màn Ghi chép báo *"chưa cấu hình khoá AI"* thay vì
 * gọi mạng rồi thất bại khó hiểu.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Named(GeminiFoodInsightSource.NAMED_API_KEY)
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Named(GeminiFoodInsightSource.NAMED_MODEL)
    fun provideGeminiModel(): String = BuildConfig.GEMINI_MODEL

    /**
     * Model **dự phòng**, chỉ dùng khi model chính trả 503/429 (Google quá tải / hết quota — đã gặp
     * thật rất thường xuyên). Bản nhẹ nên phản hồi nhanh; rỗng thì lớp gọi chỉ dùng model chính.
     */
    @Provides
    @Named(GeminiFoodInsightSource.NAMED_FALLBACK_MODEL)
    fun provideGeminiFallbackModel(): String = BuildConfig.GEMINI_FALLBACK_MODEL
}
