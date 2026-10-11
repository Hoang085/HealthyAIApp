import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

/**
 * Thiết lập cục bộ (`sdk.dir` + khoá Gemini) — `local.properties` đã bị `.gitignore` nên khoá **không**
 * bao giờ vào git. File thiếu cũng không sao: các khoá rơi về giá trị mặc định (rỗng).
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.healthyai.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.healthyai.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Gemini: AI phân tích món ăn (mục 3 #42, #43) ─────────────────────────
        // Khoá đọc từ `local.properties` (file đã có trong `.gitignore`) nên KHÔNG bao giờ vào git.
        // Máy chưa cấu hình khoá → chuỗi rỗng → app báo rõ "chưa cấu hình khoá AI", không crash.
        // LƯU Ý: khoá nhúng trong APK là trích xuất được — chỉ hợp với app chạy thử nghiệm cá nhân;
        // muốn phát hành thì phải gọi Gemini qua một proxy backend thay vì nhúng khoá vào app.
        // Model KHÔNG hardcode trong code Kotlin: đổi tên model chỉ cần sửa `local.properties`.
        // `gemini.fallback.model` chỉ được gọi khi model chính trả 503/429 (Google quá tải/hết quota).
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("gemini.api.key").orEmpty()}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${localProps.getProperty("gemini.model") ?: "gemini-3.8-flash"}\"")
        buildConfigField(
            "String",
            "GEMINI_FALLBACK_MODEL",
            "\"${localProps.getProperty("gemini.fallback.model") ?: "gemini-flash-lite-latest"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            // Lưu ý: Nếu google-services.json chưa add package "com.healthyai.app.debug"
            // trên Firebase Console, hãy tạm thời comment dòng dưới để tránh lỗi build:
            // applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        // AGP 8 mặc định TẮT BuildConfig; bật để dùng `GEMINI_API_KEY`/`GEMINI_MODEL` (mục 3 #42).
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Cho phép unit test JVM gọi API framework (ví dụ `android.util.Log` trong worker/repository):
            // trả về giá trị mặc định thay vì ném "Method d in android.util.Log not mocked".
            isReturnDefaultValues = true
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties"
            )
        }
    }
}

// Room KSP — Xuất schema
ksp {
    arg("room.schemaLocation", "${project.projectDir}/schemas")
    arg("room.incremental", "true")
}

/**
 * Guava chỉ cần ở **lúc biên dịch** (`compileOnly`).
 *
 * CameraX trả về `com.google.common.util.concurrent.ListenableFuture` (mở camera, bật/tắt đèn flash),
 * nhưng trên classpath chỉ có gói **rỗng** `com.google.guava:listenablefuture:9999.0-empty-to-avoid-
 * conflict-with-guava` — gói này tồn tại đúng để tránh trùng lớp khi project **đã có** Guava. Ở đây
 * Guava thật **đã có trong APK** (kéo theo bởi Firebase Firestore, chỉ ở phạm vi runtime nên Kotlin
 * không thấy khi biên dịch) ⇒ chỉ cần thêm Guava cho lúc biên dịch là đủ; **không** dùng
 * `implementation` vì như vậy sẽ **trùng lớp** `ListenableFuture` với Guava ở runtime.
 *
 * Nhờ vậy `CameraControl.enableTorch` (bật đèn flash cho phép đo nhịp tim) biên dịch được, mà APK
 * không bị thêm Guava lần thứ hai. Xem ghi chú cùng nội dung trong `dependencies { }`.
 */

dependencies {
    // Themes
    implementation("com.google.android.material:material:1.12.0")

    // ── Firebase (Thêm mới) ─────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    // ── AndroidX Core ──────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ── Jetpack Compose ─────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Dagger Hilt ─────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Hilt cho WorkManager
    implementation(libs.hilt.work)
    // LƯU Ý: Khai báo này trong libs.versions.toml là 'hilt-compiler'
    ksp(libs.hilt.compiler)

    // ── Room Database + SQLCipher ───────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.ktx)

    // ── Health Connect API ──────────────────────────────────────
    implementation(libs.health.connect.client)

    // ── CameraX (chụp ảnh + preview) ────────────────────────────
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ── AI phân tích món ăn: gọi Gemini qua HTTPS (mục 3 #42) ────
    // Chỉ cần Gson để đọc JSON trả về; HTTP dùng `HttpURLConnection` của JDK nên không thêm OkHttp.
    implementation(libs.gson)

    // Đọc hướng xoay (EXIF) của ảnh chọn từ thư viện — ảnh chụp dọc thường được lưu ở dạng ngang kèm
    // cờ EXIF, thiếu bước này thì ảnh gửi cho AI bị quay 90°.
    implementation(libs.androidx.exifinterface)

    // CameraX (mở camera, bật/tắt đèn flash cho phép đo nhịp tim) trả về `ListenableFuture` của Guava.
    // Gradle đang chọn bản `listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` — jar RỖNG mà
    // các thư viện khác kéo vào để tránh trùng lớp khi có Guava đầy đủ — nên Kotlin **không thấy**
    // interface này và không gọi được `enableTorch`/`ProcessCameraProvider.getInstance`.
    // CameraX (mở camera, bật/tắt đèn flash cho phép đo nhịp tim) trả về `ListenableFuture` của Guava,
    // mà classpath chỉ có gói `listenablefuture` **rỗng** (gói này tồn tại để tránh trùng lớp khi đã có
    // Guava — và Guava thật đã nằm trong APK qua Firebase Firestore) ⇒ thêm Guava cho **lúc biên dịch**.
    // `isTransitive = false`: chỉ cần chính jar Guava; các gói kéo theo (j2objc-annotations, jsr305,
    // checker-qual…) không liên quan tới API CameraX dùng đến.
    compileOnly(libs.guava) {
        isTransitive = false
    }

    // ── WorkManager & Coroutines ────────────────────────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Glance Widget ───────────────────────────────────────────
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // ── Testing ─────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.work.testing)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}