# HealthyAI App

> App theo dõi & cải thiện thói quen sức khỏe cá nhân hóa bằng AI
> Android • Kotlin • Jetpack Compose • Clean Architecture / MVVM

---

## Mở project trong Android Studio

1. Mở **Android Studio** → **File > Open** → chọn thư mục `HealthyAIApp`
2. Đợi **Gradle Sync** tự chạy (cần Internet, 5-10 phút lần đầu)
3. Tạo **AVD Emulator**: `Pixel 7, API 34, with Google Play` (bắt buộc để test Health Connect)
4. Run app ▶️

---

## Cấu trúc project

```
app/src/main/java/com/healthyai/app/
├── HiltApplication.kt          ← Application entry point
├── MainActivity.kt             ← Single Activity
├── di/                         ← Dependency Injection (Hilt)
│   ├── AppModule.kt            ← Bind Repository interfaces
│   └── DatabaseModule.kt       ← Room + SQLCipher setup
├── domain/                     ← Business Logic (không phụ thuộc framework)
│   ├── model/                  ← Habit, HabitLog, HealthData, Suggestion
│   ├── repository/             ← Repository interfaces
│   └── usecase/                ← GetTodayHabits, LogHabit, HabitRuleEngine (AI)
├── data/                       ← Data Sources
│   ├── local/                  ← Room Database + DAO + Entity
│   ├── healthconnect/          ← Health Connect API wrapper
│   └── repository/             ← Repository implementations
├── presentation/               ← UI Layer
│   ├── ui/theme/               ← Material You theme (Color, Theme, Type)
│   ├── navigation/             ← Compose Navigation + Bottom Nav
│   └── screen/                 ← 4 màn hình: Home, Habits, Camera, Insights
│       ├── home/               ← HomeScreen + HomeViewModel
│       ├── habits/             ← HabitsScreen + HabitsViewModel
│       ├── camera/             ← CameraScreen + CameraViewModel (ML Kit)
│       └── insights/           ← InsightsScreen + InsightsViewModel
└── worker/                     ← WorkManager
    ├── SyncWorker.kt           ← Đồng bộ Firebase (6h/lần)
    └── ReminderWorker.kt       ← Nhắc nhở hàng ngày
```

---

## Tech Stack

| Layer | Technology |
|:------|:-----------|
| **UI** | Jetpack Compose + Material You (Dynamic Color) |
| **Architecture** | MVVM + Clean Architecture (3 layers) |
| **DI** | Dagger Hilt |
| **Database** | Room + **SQLCipher** (AES-256 encryption) |
| **Health Data** | Health Connect API (Android IPC, không qua mạng) |
| **On-device AI** | ML Kit Image Labeling + Rule Engine |
| **Camera** | CameraX |
| **Background** | WorkManager + Kotlin Coroutines/Flow |
| **Cloud (optional)** | Firebase Firestore + Auth |
| **Widget** | Jetpack Glance |
| **Testing** | JUnit, MockK, Coroutines Test |

---

## Bật Firebase (optional)

1. Tạo project tại [console.firebase.google.com](https://console.firebase.google.com)
2. Tải `google-services.json` → đặt vào `app/`
3. Bỏ comment các dòng Firebase trong:
   - `gradle/libs.versions.toml` → `firebase-bom`, `firebase-firestore`, `firebase-auth`
   - `build.gradle.kts` (root) → `google-services` plugin
   - `app/build.gradle.kts` → Firebase dependencies + `google-services` plugin
4. Gradle Sync lại

---

## Chạy Unit Tests

```bash
./gradlew test
```

Hoặc trong Android Studio: Click chuột phải vào `HabitRuleEngineTest` → **Run Tests**

---

## Test Health Connect trên Emulator

1. Cài **Health Connect Toolbox** từ Google Play (trên emulator có Google Play)
2. Bơm dữ liệu giả: bước chân, giấc ngủ, nhịp tim
3. Chạy app → màn hình Home hiển thị dữ liệu từ Health Connect

---

## Lộ trình phát triển

- [x] **Module 1**: Core UI (Compose, Material You, Navigation, Room + SQLCipher)
- [x] **Module 2**: Health Connect integration, WorkManager
- [x] **Module 3 (AI)**: Rule Engine on-device + ML Kit Camera setup
- [ ] **Module 2 (Cloud)**: Firebase Firestore sync (optional)
- [ ] **CameraX Preview**: Tích hợp thật AndroidView + PreviewView
- [ ] **Glance Widget**: Home screen widget hiển thị tiến độ ngày
- [ ] **Migration**: Room database migration khi tăng version
- [ ] **Android Keystore**: Bảo mật passphrase SQLCipher đúng chuẩn production
