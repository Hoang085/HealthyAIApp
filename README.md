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
│   ├── repository/             ← Repository interfaces (Habit, HealthData, Auth)
│   └── usecase/                ← GetTodayHabits, LogHabit, HabitRuleEngine (AI)
├── data/                       ← Data Sources
│   ├── local/                  ← Room Database + DAO + Entity
│   │   ├── migration/          ← Room Migration 1→2, 2→3 (nâng version không mất dữ liệu)
│   │   └── security/           ← Passphrase SQLCipher (Android Keystore) + rekey DB bản cũ
│   ├── healthconnect/          ← Health Connect API wrapper
│   ├── firebase/               ← Firestore sync data source
│   └── repository/             ← Repository implementations
├── presentation/               ← UI Layer
│   ├── ui/theme/               ← Material You theme (Color, Theme, Type)
│   ├── navigation/             ← Cổng xác thực + Compose Navigation + Bottom Nav
│   └── screen/                 ← Home, Habits, Camera, Insights + màn Đăng nhập
│       ├── auth/               ← LoginScreen + LoginViewModel (Firebase Auth)
│       ├── home/               ← HomeScreen + HomeViewModel
│       ├── habits/             ← HabitsScreen + HabitsViewModel
│       ├── camera/             ← CameraScreen + CameraViewModel (ML Kit)
│       └── insights/           ← InsightsScreen + InsightsViewModel
└── worker/                     ← WorkManager
    ├── SyncWorker.kt           ← Đồng bộ Firebase (6h/lần, bỏ qua khi chưa đăng nhập)
    └── ReminderWorker.kt       ← Nhắc nhở hàng ngày
```

---

## Tech Stack

| Layer | Technology |
|:------|:-----------|
| **UI** | Jetpack Compose + Material You (Dynamic Color) |
| **Architecture** | MVVM + Clean Architecture (3 layers) |
| **DI** | Dagger Hilt |
| **Database** | Room + **SQLCipher** (AES-256 encryption, passphrase sinh ngẫu nhiên & bọc bằng **Android Keystore**) |
| **Health Data** | Health Connect API (Android IPC, không qua mạng) |
| **On-device AI** | ML Kit Image Labeling + Rule Engine |
| **Camera** | CameraX |
| **Background** | WorkManager + Kotlin Coroutines/Flow |
| **Cloud (optional)** | Firebase Firestore + Auth |
| **Widget** | Jetpack Glance |
| **Testing** | JUnit, MockK, Coroutines Test |

---

## Bật Firebase (optional)

Firebase đã được bật sẵn trong project (`app/build.gradle.kts` + `gradle/libs.versions.toml`), cần `app/google-services.json`.

1. Tạo project tại [console.firebase.google.com](https://console.firebase.google.com)
2. Tải `google-services.json` → đặt vào `app/` (đúng package `com.healthyai.app`)
3. Bật đăng nhập email/mật khẩu: **Authentication → Sign-in method → Email/Password → Enable**
4. Gradle Sync lại

**Nếu không có Firebase:** app vẫn chạy bình thường — màn đăng nhập cho phép bấm **"Bỏ qua, dùng ở chế độ khách"**,
mọi dữ liệu lưu cục bộ; chỉ `SyncWorker` (đẩy log lên Firestore) là không hoạt động.

---

## Đăng nhập / Đăng ký

- Màn hình đầu tiên của app là **Đăng nhập** (`presentation/screen/auth/LoginScreen.kt`), hỗ trợ **Đăng nhập** và **Đăng ký** bằng email + mật khẩu.
- `AuthGateViewModel` (`presentation/navigation/`) quyết định màn hình gốc: `Loading` (đang đọc phiên đã lưu) → `SignedOut` (hiện màn đăng nhập) → `SignedIn` / `Guest` (vào NavHost chính).
- Có thể bấm **"Bỏ qua, dùng ở chế độ khách"**: dùng toàn bộ tính năng offline (thói quen, Health Connect, widget, gợi ý AI); `SyncWorker` sẽ bỏ qua (`Result.success()`) cho tới khi có phiên đăng nhập.
- Phiên đăng nhập do FirebaseAuth tự lưu → mở lại app vào thẳng Home.

---

## Bảo mật dữ liệu cục bộ (Room + SQLCipher)

- DB `healthy_ai_encrypted.db` được mã hoá AES-256 bằng SQLCipher, passphrase **không còn hard-code**:
  - lần chạy đầu tiên app sinh **32 byte ngẫu nhiên** cho riêng máy đó;
  - passphrase được bọc bằng khoá **AES-256/GCM nằm trong Android Keystore** (alias `healthy_ai_sqlcipher_key_v1`), chỉ **bản mã** `Base64(IV‖ciphertext)` được lưu ở `shared_prefs/healthy_ai_secure_store.xml`;
  - các lần mở sau đọc lại đúng passphrase đã lưu (không sinh khoá mới).
- **Nâng cấp từ bản cũ** (bản còn hard-code `"REPLACE_WITH_SECURE_KEY_FROM_KEYSTORE"`): lần mở đầu tiên sau khi cập nhật, app **rekey** DB cũ sang khoá Keystore → thói quen và nhật ký **giữ nguyên**, không cần `pm clear`. Theo dõi bằng `adb logcat -s DatabaseKeyMigration`.
- Nếu **không mở được DB** (khoá Keystore mất do khôi phục backup sang máy khác, file DB hỏng…), app **xoá DB và tạo mới** rồi chạy bình thường thay vì crash lặp lại — dữ liệu cục bộ khi đó mất, đây là giới hạn kỹ thuật khi khoá đã mất hẳn.
- ⚠️ **Database Inspector không đọc được** file DB đã mã hoá; các bước kiểm tra nằm ở **TC-DB-11…14** trong `TESTCASE_HealthyAI.md`.
- Passphrase được ghi xuống `SharedPreferences` bằng **`commit()` (đồng bộ)** ngay trước khi DB được tạo — tránh trường hợp tiến trình bị kill khi bản ghi còn nằm trong hàng đợi `apply()`, làm lần mở sau không tìm thấy khoá và phải tạo DB mới (mất dữ liệu). Xem **phụ lục C #29** / **TC-DB-16**.

---

## Nâng cấp schema & không mất dữ liệu (Room Migration)

- `AppDatabase` đang ở `version = 3`; các bước tăng version nằm ở **`data/local/migration/DatabaseMigrations.kt`**:
  - `MIGRATION_1_2`: thêm `habits.timeOfDay`, `habits.frequency`, `habit_logs.targetValueAtTime` và **backfill** `targetValueAtTime` từ mục tiêu của thói quen (dữ liệu cũ không bị tính thành 0%).
  - `MIGRATION_2_3`: tạo bảng `health_data_history`.
- `di/DatabaseModule.kt` dùng `.addMigrations(*DatabaseMigrations.ALL.toTypedArray())` — **đã bỏ** `fallbackToDestructiveMigration()` nên cài đè bản mới lên bản cũ **không** xoá thói quen/nhật ký.
- Migration chỉ dùng `ALTER TABLE … ADD COLUMN … DEFAULT` / `CREATE TABLE` (không DROP/RENAME bảng) nên **không** kích hoạt `ON DELETE CASCADE` của `habit_logs`.
- **Khi tăng version**: tăng `version` trong `data/local/AppDatabase.kt`, thêm migration vào `DatabaseMigrations.ALL`, cập nhật `DatabaseMigrations.DATABASE_VERSION` — test `DatabaseMigrationsTest` sẽ fail nếu dãy migration bị thiếu bước.
- Kiểm chứng thủ công: **TC-DB-08**, **TC-DB-15** trong `TESTCASE_HealthyAI.md`.

---

## Chạy Unit Tests

```bash
# Repo CHƯA commit Gradle wrapper (không có gradlew/gradlew.bat) → dùng Gradle đã cài,
# hoặc mở Android Studio → Gradle tool window. Bắt buộc JDK 21.
gradle testDebugUnitTest        # chạy từ E:\HealthyAIApp (thư mục chứa settings.gradle.kts)
```

Hoặc trong Android Studio: Click chuột phải vào `HabitRuleEngineTest` → **Run Tests**

Bộ test hiện tại: **22 class / 138 test** (JVM, không cần emulator) — xem **TC-ENV-03** trong `TESTCASE_HealthyAI.md`.

---

## Test Health Connect trên Emulator

1. Cài **Health Connect Toolbox** từ Google Play (trên emulator có Google Play)
2. Bơm dữ liệu giả: bước chân, giấc ngủ, nhịp tim
3. Chạy app → màn hình Home hiển thị dữ liệu từ Health Connect

### Quyền Health Connect

- Lần đầu mở tab **Trang chủ**, app hiện thẻ **"Cấp quyền Health Connect"** (ngay dưới "Tóm tắt sức khỏe") → bấm để mở hộp thoại quyền của Health Connect và bật 4 quyền đọc:
  Steps, Sleep, Heart rate, Active calories burned.
- Cấp đủ quyền → thẻ biến mất và số liệu được nạp lại ngay (không cần mở lại app). Cấp thiếu → thẻ vẫn hiện để nhắc.
- Nếu máy **chưa cài** hoặc cần **cập nhật** Health Connect, thẻ đổi thành nút mở Play Store tương ứng.
- Màn Home **không** còn hiện banner lỗi đỏ cho hai trường hợp này — chỉ còn khi Health Connect sẵn sàng nhưng lời gọi đọc dữ liệu thất bại.

---

## Thông báo nhắc nhở (`ReminderWorker`)

- Job `daily_reminder_worker` chạy mỗi 24h và gửi thông báo **"HealthyAI 💪 Nhắc nhở thói quen"** (kênh `healthy_ai_reminders`).
- Trên **Android 13+**, app cần quyền `POST_NOTIFICATIONS`: màn Trang chủ hiện thẻ **"Bật nhắc nhở hàng ngày"** → bấm **"Bật thông báo"** để mở hộp thoại quyền của hệ thống.
- Nếu người dùng đã **tắt thông báo ở cấp app** trong Settings (quyền đã cấp), thẻ đổi thành **"Thông báo đang bị tắt"** + nút **"Mở cài đặt thông báo"**.
- Thẻ tự biến mất khi thông báo đã bật; trạng thái được kiểm tra lại mỗi khi quay về màn Trang chủ.
- Chưa cấp quyền thì `ReminderWorker` vẫn trả `success` nhưng không có thông báo (`SecurityException` được bắt) — app không crash.

---

## Lộ trình phát triển

- [x] **Module 1**: Core UI (Compose, Material You, Navigation, Room + SQLCipher)
- [x] **Module 2**: Health Connect integration, WorkManager
- [x] **Quyền Health Connect**: xin quyền ngay trong app (thẻ ở Home) + mời cài/cập nhật Health Connect
- [x] **Quyền thông báo**: xin `POST_NOTIFICATIONS` trong app + mở Settings khi thông báo bị tắt
- [x] **Xóa mềm thói quen**: dialog xác nhận + Toast phản hồi, giữ lại hàng `habits` và toàn bộ nhật ký (`isActive = 0`)
- [x] **Phản hồi ghi nhận thói quen**: Toast **"Đã ghi nhận: `<tên thói quen>`"** sau khi lưu nhật ký (`HabitsScreen` đọc `showLogSuccessFor`)
- [x] **Số hiển thị gọn gàng**: ô "Mục tiêu hàng ngày" (màn Sửa) và giá trị mặc định của hộp thoại ghi nhận hiển thị `2000` thay vì `2000.0` (vẫn giữ `7.5` khi có phần thập phân) — `presentation/ui/format/NumberDisplay.kt`
- [x] **Validate hộp thoại ghi nhận**: nhập rỗng / không phải số → giữ hộp thoại mở, hiện lỗi dưới ô nhập, không ghi log (trước đây đóng im lặng) — `presentation/screen/habits/HabitLogInput.kt`
- [x] **Chống trùng tên thói quen**: tạo/đổi tên thành tên đã có (không phân biệt hoa/thường, bỏ khoảng trắng thừa) → hộp thoại **"Tên thói quen đã tồn tại"**, không ghi DB; tên của thói quen đã xóa mềm dùng lại được
- [x] **Chống double-submit khi lưu thói quen**: cờ `isSaving` bỏ qua lần bấm thứ hai, nút đáy mờ đi + hiện vòng xoay trong lúc ghi DB (`AddEditHabitViewModel`, `AddEditHabitScreen`)
- [x] **Nút hành động trong thẻ gợi ý AI**: bấm "Ghi nhận uống nước" / "Bắt đầu thiền"… → chuyển sang tab Thói quen và **mở sẵn hộp thoại ghi nhận** của thói quen đúng danh mục; chưa có thói quen thì Toast nhắc tạo mới (`InsightsScreen` → `AppNavigation` → `HabitsViewModel`)
- [x] **Chặn số lượng không hợp lệ khi ghi nhận**: nhập `0` / số âm / `NaN` → hộp thoại giữ nguyên và báo **"Số lượng phải lớn hơn 0"**, không ghi log (hộp thoại + lưới an toàn ở `HabitsViewModel.logHabit`)
- [x] **Hiển thị lỗi ở màn Thói quen**: ghi nhận thất bại → **thẻ lỗi đỏ** ngay dưới tiêu đề, đóng được (`ErrorStateCard` dùng chung với màn Trang chủ)
- [x] **Module 3 (AI)**: Rule Engine on-device + ML Kit Camera setup
- [x] **Đăng nhập/Đăng ký**: Firebase Auth (email + mật khẩu) + chế độ khách
- [ ] **Module 2 (Cloud)**: Firebase Firestore sync (optional)
- [x] **CameraX Preview**: `LifecycleCameraController` + `PreviewView` trong `AndroidView`, nút "Phân tích món ăn" chụp ảnh qua CameraX → ML Kit (`CameraScreen`)
- [x] **Thêm kết quả camera vào nhật ký**: nút **"Thêm vào nhật ký"** ở thẻ kết quả nhận diện → chuyển sang tab Thói quen và **mở sẵn hộp thoại ghi nhận** của thói quen **Dinh dưỡng** (dùng chung `AppNavigation.openHabitLogFor` với nút gợi ý AI); chưa có thói quen thì Toast nhắc tạo mới (`CameraScreen`, `AppNavigation`, `HabitsViewModel`)
- [x] **Gợi ý vận động đúng theo đơn vị**: log đơn vị **bước** giữ ngưỡng 5 000/8 000 bước; thói quen đo bằng đơn vị khác (phút, km…) so theo **tỉ lệ hoàn thành mục tiêu** (< 50% → HIGH, < 80% → LOW) — trước đây "30 phút" bị đọc thành "30 bước" (`HabitRuleEngine.analyzeExerciseLogs`)
- [x] **Dữ liệu Health Connect gom theo NGÀY**: `health_data_history` ghi 1 hàng mỗi lần sync 6 giờ → rule engine lấy hàng mới nhất của từng ngày, cảnh báo thiếu ngủ cần **3 ngày liên tiếp** và "nhịp tim trung bình 7 ngày" nay đúng là 7 ngày (`HabitRuleEngine.analyzeHealthTrends`)
- [x] **Glance Widget — tiến độ ngày**: widget hiển thị **x/y thói quen đạt mục tiêu** + thanh tiến độ **thật** (tính từ `habit_logs` hôm nay) và danh sách tối đa 3 thói quen (dòng `✓/○ tên — đã ghi/mục tiêu`, thêm dòng "+N thói quen khác" khi nhiều hơn); xếp thói quen của **buổi hiện tại** lên đầu; tự vẽ lại khi dữ liệu thói quen đổi (`HabitWidgetRefresher`) và định kỳ 30 phút (`updatePeriodMillis`) — `TodayHabitProgress`, `HabitWidget`
- [x] **Migration**: Room database migration khi tăng version — `MIGRATION_1_2`, `MIGRATION_2_3` trong `data/local/migration/DatabaseMigrations.kt`; đã **bỏ** `fallbackToDestructiveMigration()` nên nâng version schema **không** còn xoá thói quen/nhật ký (`DatabaseMigrationsTest` chặn việc bỏ sót bước migration)
- [x] **Android Keystore**: passphrase SQLCipher **không còn hard-code** — sinh ngẫu nhiên 32 byte ở lần chạy đầu, bọc bằng khoá AES-256/GCM trong Android Keystore, bản mã lưu ở prefs `healthy_ai_secure_store`; máy đã cài bản cũ được **rekey một lần** sang khoá mới nên không mất dữ liệu (`data/local/security/`)
