plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    // alias(libs.plugins.google.services)  // Bỏ comment sau khi thêm google-services.json
}

android {
    namespace   = "com.healthyai.app"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.healthyai.app"
        minSdk          = 26
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room KSP — xuất schema để version control
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // ── AndroidX Core ──────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ── Jetpack Compose (BOM quản lý version) ──────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ── Lifecycle / ViewModel ───────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Dagger Hilt (Dependency Injection) ─────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── Room Database + SQLCipher Encryption ───────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)   // Mã hóa DB
    implementation(libs.sqlite.ktx)

    // ── Health Connect API ──────────────────────────────────────
    implementation(libs.health.connect.client)

    // ── CameraX ────────────────────────────────────────────────
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.mlkit.vision)

    // ── ML Kit (On-device AI — nhận diện món ăn) ───────────────
    implementation(libs.mlkit.image.labeling)

    // ── WorkManager (Background sync & reminders) ──────────────
    implementation(libs.work.runtime.ktx)

    // ── Kotlin Coroutines ───────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Glance Widget (Home Screen Widget) ─────────────────────
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // ── Firebase (Optional) ─────────────────────────────────────
    // Bỏ comment sau khi:
    //   1. Tạo project Firebase Console
    //   2. Tải google-services.json vào thư mục app/
    //   3. Bỏ comment plugin google-services ở trên
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.firestore)
    // implementation(libs.firebase.auth)
    // implementation(libs.kotlinx.coroutines.play.services)

    // ── Unit Testing ────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // ── Integration Testing ─────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.work.testing)

    // ── Debug only ──────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
