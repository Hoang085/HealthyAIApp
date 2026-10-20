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
│   ├── model/                  ← Habit, HabitLog, HealthData, HeartRate, Suggestion, HealthConnectDataSettings
│   ├── repository/             ← Repository interfaces (Habit, HealthData, Auth)
│   └── usecase/                ← GetTodayHabits, LogHabit, HabitRuleEngine (AI)
│       ├── steps/              ← StepCounterMath (tự đếm bước từ cảm biến của máy — **chỉ tính lúc app đang chạy**)
│       ├── sleep/              ← SleepTrackerMath (tự ước lượng giờ ngủ từ cảm biến của máy)
│       ├── food/               ← FoodAnalysis (prompt + làm sạch kết quả AI cho màn Ghi chép bữa ăn)
│       └── heartrate/          ← HeartRateMath + PpgAnalyzer (tự đo nhịp tim từ camera + đèn flash)
├── data/                       ← Data Sources
│   ├── local/                  ← Room Database + DAO + Entity
│   │   ├── migration/          ← Room Migration 1→2, 2→3, 3→4 (nâng version không mất dữ liệu)
│   │   ├── security/           ← Passphrase SQLCipher (Android Keystore) + rekey DB bản cũ
│   │   ├── steps/              ← StepCounterStore (prefs `healthy_ai_step_counter` + `session_id` = phiên chạy của tiến trình)
│   │   ├── sleep/              ← SleepTrackerStore (prefs `healthy_ai_sleep_tracker`)
│   │   └── {dao,entity}/settings/ ← Bảng `app_settings` (thiết lập người dùng: khung giờ ngủ, nguồn dữ liệu sức khỏe của Trang chủ)
│   ├── healthconnect/          ← Health Connect API wrapper
│   ├── firebase/               ← Firestore sync data source
│   ├── steps/                  ← StepCounterSensor (TYPE_STEP_COUNTER)
│   ├── sleep/                  ← SleepSensor (TYPE_LIGHT + TYPE_ACCELEROMETER + màn hình)
│   ├── network/                ← HttpPost → HttpUrlConnectionPost (cổng HTTP POST ra ngoài)
│   ├── vision/                 ← FoodInsightSource → GeminiFoodInsightSource (**AI Gemini**: tên món + lợi ích/điều nên lưu ý) + ImageJpegEncoder (nén JPEG) + GalleryImageLoader (thu nhỏ + xoay ảnh chọn từ thư viện)
│   ├── heartrate/              ← CameraPpgAnalyzer (đọc kênh đỏ vùng giữa khung hình camera)
│   └── repository/             ← Repository implementations (…, SettingsRepositoryImpl)
├── presentation/               ← UI Layer
│   ├── ui/theme/               ← Material You theme (Color, Theme, Type)
│   ├── navigation/             ← Cổng xác thực + Compose Navigation + Bottom Nav
│   └── screen/                 ← Home, Habits, Camera, Insights + màn Đăng nhập / Đo nhịp tim
│       ├── auth/               ← LoginScreen + LoginViewModel (Firebase Auth)
│       ├── home/               ← HomeScreen + HomeViewModel + thẻ "Khung giờ ngủ" + switch "Dùng dữ liệu Health Connect" (HealthDataSourceCard)
│       ├── habits/             ← HabitsScreen + HabitsViewModel
│       ├── camera/             ← CameraScreen + CameraViewModel (**AI Gemini** phân tích món ăn + hộp thoại "Đang phân tích")
│       ├── heartrate/          ← HeartRateScreen + HeartRateViewModel (CameraX + đèn flash)
│       └── insights/           ← InsightsScreen + InsightsViewModel
└── worker/                     ← WorkManager
    ├── SyncWorker.kt           ← Đồng bộ Firebase (6h/lần, bỏ qua khi chưa đăng nhập)
    ├── HealthSyncWorker.kt     ← Đồng bộ dữ liệu sức khỏe (6h/lần)
    ├── SleepTrackingWorker.kt  ← Lấy mẫu giấc ngủ từ cảm biến (15 phút/lần)
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
| **AI (on-device)** | Rule Engine thuần Kotlin (gợi ý BLOOM AI, chỉ số sinh lực) |
| **AI (cloud)** | Gemini API cho màn Ghi chép bữa ăn — model chính `gemini-3.8-flash` (`thinkingLevel: low`), model dự phòng `gemini-flash-lite-latest` khi Google trả 503/429 |
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
- **Lời chào ở màn Trang chủ dùng tên thật của phiên** (`AuthRepository.currentUserName` → `HomeUiState.userName`): `displayName` của Firebase, nếu trống (tài khoản tạo trong app) thì lấy **phần trước `@` của email**; ở **chế độ khách** không hiện dòng tên nào — không còn tên hard-code.

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

- `AppDatabase` đang ở `version = 4`; các bước tăng version nằm ở **`data/local/migration/DatabaseMigrations.kt`**:
  - `MIGRATION_1_2`: thêm `habits.timeOfDay`, `habits.frequency`, `habit_logs.targetValueAtTime` và **backfill** `targetValueAtTime` từ mục tiêu của thói quen (dữ liệu cũ không bị tính thành 0%).
  - `MIGRATION_2_3`: tạo bảng `health_data_history`.
  - `MIGRATION_3_4`: tạo bảng `app_settings` (thiết lập của người dùng, dạng **khoá – giá trị** — thêm thiết lập mới sau này **không cần** migration nữa).
- `di/DatabaseModule.kt` dùng `.addMigrations(*DatabaseMigrations.ALL.toTypedArray())` — **đã bỏ** `fallbackToDestructiveMigration()` nên cài đè bản mới lên bản cũ **không** xoá thói quen/nhật ký.
- Migration chỉ dùng `ALTER TABLE … ADD COLUMN … DEFAULT` / `CREATE TABLE` (không DROP/RENAME bảng) nên **không** kích hoạt `ON DELETE CASCADE` của `habit_logs`.
- **Khi tăng version**: tăng `version` trong `data/local/AppDatabase.kt`, thêm migration vào `DatabaseMigrations.ALL`, cập nhật `DatabaseMigrations.DATABASE_VERSION` — test `DatabaseMigrationsTest` sẽ fail nếu dãy migration bị thiếu bước.
- DDL trong migration phải **khớp từng ký tự** với `createSql` mà Room sinh ra trong `app/schemas/…/<version>.json`; cách kiểm chứng không cần emulator: dựng DB ở version cũ bằng `sqlite3` từ schema cũ, chạy các câu lệnh migration, rồi so `PRAGMA table_info/index_list/foreign_key_list` với schema mới.
- Kiểm chứng thủ công: **TC-DB-08**, **TC-DB-15**, **TC-DB-17** trong `TESTCASE_HealthyAI.md`.

---

## Chạy Unit Tests

```bash
# Repo CHƯA commit Gradle wrapper (không có gradlew/gradlew.bat) → dùng Gradle đã cài,
# hoặc mở Android Studio → Gradle tool window. Bắt buộc JDK 21.
gradle testDebugUnitTest        # chạy từ E:\HealthyAIApp (thư mục chứa settings.gradle.kts)
```

Hoặc trong Android Studio: Click chuột phải vào `HabitRuleEngineTest` → **Run Tests**

Bộ test hiện tại: **39 class / 380 test** (JVM, không cần emulator) — xem **TC-ENV-03** trong `TESTCASE_HealthyAI.md`.

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

## Đếm bước chân bằng cảm biến của máy (nguồn "tự lực" của app)

- Ngoài Health Connect, app có nguồn số bước **thứ hai**: đọc **cảm biến đếm bước phần cứng** của điện thoại (`Sensor.TYPE_STEP_COUNTER`). Nguồn này **không cần Health Connect** và không cần ứng dụng sức khỏe nào khác; cảm biến đếm ở phần cứng nên gần như không tốn pin và **vẫn đếm khi màn hình tắt**. Lưu ý: cảm biến **vẫn đếm** cả lúc app đã bị đóng, nhưng app **chỉ tính phần đếm được trong lúc app đang chạy** (xem mục *"Quy đổi"* bên dưới).
- **Quyền:** từ Android 10 (API 29) trở lên cần quyền **`ACTIVITY_RECOGNITION`**. Màn Trang chủ hiện thẻ **"Bật đếm bước chân"** khi máy **có** cảm biến mà app **chưa** được cấp quyền; cấp quyền xong thẻ biến mất và số bước được nạp ngay (không cần mở lại app). Thẻ có thêm nút phụ *"Không thấy hộp thoại? Mở cài đặt quyền"* cho trường hợp Android không hiện hộp thoại nữa.
- **Nguồn hiển thị trên thẻ "Tóm tắt sức khỏe" do switch quyết định** (xem mục *"Chọn nguồn dữ liệu cho thẻ Tóm tắt sức khỏe"*): switch **TẮT** (mặc định) → lấy **bản chụp đã lưu trong app**; switch **BẬT** → lấy **Health Connect** (dữ liệu có thể gồm cả đồng hồ đeo tay). Nguồn đang chọn **không có số** thì dùng **cảm biến của điện thoại** làm dự phòng (nguồn live). Thẻ **không** còn dòng chú thích nguồn nào dưới 3 vòng tròn (đã bỏ cho gọn — xem mục *"Tóm tắt sức khỏe đọc lại theo ngày"*).
- **Quy đổi** con số "luỹ kế từ lúc bật máy" thành "số bước hôm nay" nằm ở `domain/usecase/steps/StepCounterMath.kt` (hàm thuần, test được trên JVM). **Luật hiện tại (mục 3 #44): chỉ tính phần quan sát được trong lúc app đang chạy** — mỗi lần **tiến trình app được tạo** là một **phiên**:
  - **app đang mở** hoặc **còn nằm trong tác vụ** (chạy nền) → **cùng phiên** → **đếm tiếp** bình thường;
  - **app bị đóng hẳn** (vuốt khỏi tác vụ, tiến trình bị hệ thống thu hồi) → lần mở sau là **phiên mới**: baseline đặt lại theo giá trị luỹ kế **hiện tại** ⇒ bước đi lúc app không chạy **không** bị cộng vào, nhưng số **đã đếm trong ngày vẫn giữ** (`offset`);
  - **sang ngày mới khi app còn chạy** → **nội suy tuyến tính** giá trị luỹ kế tại 00:00 giữa mẫu cuối ngày hôm trước và mẫu đầu tiên của hôm nay (chỉ dùng khi hai mẫu thuộc **cùng một phiên**);
  - **máy khởi động lại** (bộ đếm reset về 0) → phần đã đi trong ngày được giữ trong `offset` nên số bước không bị tụt;
  - **lần chạy đầu tiên** (chưa có dữ liệu lưu, kể cả data của bản cũ thiếu khoá phiên) → **không** suy diễn ngược về trước: bắt đầu đếm từ chính mẫu đó;
  - `StepCounterMathTest` chốt **12** ca, trong đó có *"bước đi lúc app đã đóng không bao giờ được cộng"* và *"đóng app rồi mở lại trong cùng ngày vẫn giữ số đã đếm"*.
- **Lưu trạng thái:** `SharedPreferences` `healthy_ai_step_counter` (`data/local/steps/StepCounterStore.kt`) giữ **phiên chạy của tiến trình** (khoá `session_id`, sinh một lần cho mỗi tiến trình app) cùng mốc ngày / baseline / offset. Đổi ngày / đổi phiên / đổi baseline / đổi offset ghi bằng **`commit()`** để không mất mốc khi tiến trình bị hệ thống thu hồi; mẫu cảm biến thường ghi `apply()` và tối đa mỗi **15 giây** (cảm biến phát rất dày, không cần ghi từng mẫu). Dữ liệu này không nhạy cảm nên **không** bọc Keystore như passphrase database. Việc thêm khoá `session_id` **không** cần migration (khoá thiếu được đọc là chuỗi rỗng = phiên khác).
- **Vòng đời:** listener đăng ký khi màn Trang chủ còn sống (`HomeViewModel` **không** bị xoá khi app xuống nền), nên app ở **nền mà còn trong tác vụ** thì **vẫn đếm**; app bị **đóng hẳn** thì dừng hẳn — đúng luật đếm ở trên.
- **Giới hạn đã biết:** emulator (và một số máy rẻ tiền) **không có** cảm biến đếm bước → app không hiện thẻ xin quyền, số bước vẫn lấy từ Health Connect (muốn test trên emulator thì phải bơm dữ liệu — xem `KICHBAN_TEST_KHACH_HANG_HealthyAI.md`). Vì chỉ đếm lúc app chạy, số hiển thị **nhỏ hơn** số bước thật trong ngày nếu người dùng đóng app nhiều lần — đây là **chủ ý** của mục 3 #44. Bộ kịch bản đóng vai khách hàng có thêm **bản Excel để thực thi và ghi kết quả**: `KICHBAN_TEST_KHACH_HANG_HealthyAI.xlsx` — **một khách hàng duy nhất tên Quỳnh**, 11 hành trình A–K, 80 kịch bản KB-01…KB-80, cột `KQ` chọn Đạt/Không đạt/Chưa test được/Không áp dụng, sheet **Tổng kết** tự đếm theo trạng thái · mức độ · hành trình.

---

## Tự tính số giờ ngủ bằng cảm biến của máy (nguồn "tự lực" thứ hai)

- Android **không** có cảm biến "đang ngủ" như `TYPE_STEP_COUNTER` của bước chân, nên app **tự suy ra** giấc ngủ từ ba tín hiệu đọc được **không cần quyền nào**:
  - `PowerManager.isInteractive` → màn hình có đang được dùng không;
  - `Sensor.TYPE_LIGHT` → phòng có tối không;
  - `Sensor.TYPE_ACCELEROMETER` → điện thoại có bất động không.
- **Khung giờ ngủ do NGƯỜI DÙNG đặt** (`domain/model/SleepSchedule.kt`): app **không** tự quyết định giờ ngủ nữa. Thẻ **"Khung giờ ngủ"** ngay dưới thẻ "Tóm tắt sức khỏe" hiển thị khung đang dùng và mở hộp thoại đặt **giờ đi ngủ** + **giờ thức**; chưa đặt gì thì dùng mặc định **23:00 → 07:00**. Khung vắt qua nửa đêm (23:00 → 07:00) cũng như khung nằm gọn trong một ngày (01:00 → 08:00) đều được; nửa mở — **có** giờ đi ngủ, **không** có giờ thức. Hai mốc trùng nhau (khung rỗng) bị chặn ngay trong hộp thoại, không ghi vào DB.
- **Lưu thiết lập:** bảng Room `app_settings` (dạng **khoá – giá trị**) qua `SettingsRepository` → `data/repository/SettingsRepositoryImpl.kt`; `MIGRATION_3_4` tạo bảng này. Màn Trang chủ đọc bằng `Flow` nên đổi khung là thẻ cập nhật ngay; giá trị thiếu/hỏng đều rơi về khung mặc định.
- **Luật phân loại "đang ngủ"** (`SleepTrackerMath.isAsleep`, hàm thuần test được trên JVM): **đang trong khung giờ ngủ của người dùng** **và** màn hình tắt **và** độ sáng < 5 lux **và** độ lệch gia tốc < 0,05g.
- **Ai lấy mẫu?** `SleepTrackingWorker` chạy **15 phút/lần** (khoảng định kỳ nhỏ nhất WorkManager cho phép) và chỉ nghe cảm biến trong **3 giây** rồi gỡ listener — nên **không cần foreground service**, không tốn pin suốt đêm. Job chạy cả khi app đóng (`periodic_sleep_tracking_worker`, xem ở Background Task Inspector).
- **Cộng dồn:** `SleepTrackerMath.sample` cộng **toàn bộ** khoảng thời gian giữa hai mẫu **"đang ngủ"** liên tiếp; gặp mẫu "đang thức" thì dừng. Nhờ vậy khoảng vài giờ mà WorkManager bị hoãn (Doze) **không mất** thời gian ngủ. Một "đêm" `D` là khoảng `[giờ đi ngủ hôm D-1, giờ thức hôm D)` và **khoá đêm chỉ nhảy tại giờ đi ngủ** — nên suốt ngày (sau giờ thức) khoá vẫn là **đêm vừa kết thúc**, số của đêm đó **không bị xoá**. Khi sang đêm mới, tổng của đêm cũ được chốt lại (prefs `previous_night` / `previous_sleep_millis`) để ô "Ngủ" vẫn hiển thị **đêm vừa qua** cho tới khi đêm mới đủ dài (≥ 45 phút) thay thế — con số không nhảy về `0` vào buổi tối (`displaySleepMinutes`). Ra khỏi khung (sau giờ thức) thì hiển thị **đúng** số của đêm vừa kết thúc, kể cả khi dưới ngưỡng (thức trắng thì phải hiện `0`, không lấy số đêm cũ ra thay).
- **Lọc nhiễu:** dưới **45 phút** không coi là một giấc ngủ (`toSleepHours` trả `0.0`); trên **12 giờ** thì cắt bớt (chặn dữ liệu vô lý khi điện thoại bị bỏ quên ở nơi tối).
- **Nguồn hiển thị do switch quyết định**: TẮT (mặc định) → giờ ngủ trong **bản chụp đã lưu trong app**; BẬT → **Health Connect** — vì dữ liệu giấc ngủ có thể đến từ **đồng hồ đeo tay** và còn có chi tiết pha Sâu/Nông/REM. Nguồn đang chọn **không có số** thì app mới dùng **ước lượng từ cảm biến** của mình (không còn dòng chú thích nguồn trên thẻ — xem mục *"Tóm tắt sức khỏe đọc lại theo ngày"*). Số giờ ngủ này cũng được ghi vào `health_data_history` mỗi lần `HealthSyncWorker` chạy, nên tab Phân tích (Rule Engine + thẻ "Lịch sử sức khỏe 7 ngày") dùng được.
- **Lưu trạng thái đo:** `SharedPreferences` `healthy_ai_sleep_tracker` (`data/local/sleep/SleepTrackerStore.kt`), ghi bằng `commit()` — mẫu cách nhau cả chục phút nên chi phí không đáng kể, mà mốc đêm không bị mất khi tiến trình bị thu hồi. **Khung giờ ngủ không** nằm ở đây (đó là thiết lập của người dùng, ở bảng `app_settings`).
- **Giới hạn đã biết:** đây là **ước lượng từ điện thoại**, không thay được thiết bị chuyên dụng. Nếu điện thoại bị bỏ quên ở nơi tối và bất động **trong khung giờ ngủ** (ví dụ trong túi khi đang ở ngoài), khoảng đó bị tính là ngủ — **chọn khung giờ sát thực tế giúp giảm hẳn** loại sai số này. Máy **không có** cảm biến ánh sáng thì app vẫn hoạt động (coi như "tối") nhưng chỉ dựa vào hai tín hiệu còn lại. Thức dậy **muộn hơn giờ thức đã đặt** thì phần sau giờ thức không được tính. Cần số liệu chính xác theo pha giấc ngủ thì phải dùng đồng hồ đeo tay.

---

## Đo nhịp tim bằng camera + đèn flash (phép đo PPG)

- App **tự đo được nhịp tim** ngay trên điện thoại, không cần Health Connect hay thiết bị đeo: ô **"Tim"** trên thẻ "Tóm tắt sức khỏe" có nút **"Đo ngay"** → mở màn **"Đo nhịp tim"** (`presentation/screen/heartrate/`).
- **Nguyên lý đo:** đèn flash chiếu sáng xuyên qua mô ngón tay → camera ghi lại **sự thay đổi rất nhỏ về sắc độ đỏ/độ sáng** giữa các khung hình theo từng đợt máu dồn về đầu ngón tay → thuật toán phân tích chu kỳ đổi màu để quy ra nhịp.
- **Lấy tín hiệu:** `data/heartrate/CameraPpgAnalyzer.kt` là một `ImageAnalysis.Analyzer` của CameraX; mỗi khung hình nó tính **giá trị đỏ trung bình của vùng giữa khung** (chỉ đọc **hai mặt phẳng `Y` và `V`** rồi suy ra kênh đỏ `R = Y + 1,402·(V − 128)` — YUV→RGB là phép biến đổi tuyến tính nên **không cần đổi từng điểm ảnh**). Camera chạy ở `STRATEGY_KEEP_ONLY_LATEST` để không bị dồn khung hình, và chỉ mở khi **màn đo đang hiện** (rời màn là tắt đèn + gỡ camera trong `finally`).
- **Thuật toán** nằm ở `domain/usecase/heartrate/` (hàm thuần, **test được trên JVM** — `HeartRateMathTest` 20 test + `PpgAnalyzerTest` 15 test):
  1. **lọc mượt AC** bằng EMA (độ trễ không đổi nên **không** làm lệch khoảng cách giữa hai nhịp, mà giảm ~42% nhiễu);
  2. **bỏ thành phần một chiều (DC)** bằng trung bình trượt ~1 giây → phần còn lại là "từng đợt máu dồn về";
  3. **tìm đỉnh mạch**: đỉnh cục bộ vượt ngưỡng **thích ứng theo nhiễu** (`trung bình + 1,2 × độ lệch chuẩn` của 2 giây gần nhất), có **chống dội** (phải rơi xuống dưới mức trung bình rồi mới nhận nhịp tiếp theo) và **nội suy parabol** để mốc thời gian đỉnh chính xác hơn khoảng cách giữa hai khung hình (camera chỉ ~30 hình/giây);
  4. **kiểm tra tính chu kỳ bằng tự tương quan** trên ~8 giây gần nhất: mạch đập thật cho tương quan cao ở đúng chu kỳ tim, còn nhiễu/rung tay thì gần như không tương quan — đây là bước phân biệt "mạch đập" với "nhiễu";
  5. **chốt nhịp** bằng **trung vị các khoảng RR** (đã loại ngoại lệ lệch > 25%) → bpm; **chất lượng** = kết hợp *tính chu kỳ* và *độ đều của nhịp* (cả hai đều **không phụ thuộc biên độ**, nên camera tự chỉnh phơi sáng không làm sai).
- **Phần hướng dẫn người dùng** nằm ở `HeartRateScreen.kt`: **trước khi đo** là thẻ *"Cách đo nhịp tim"* 3 bước (che kín camera + đèn flash → giữ yên, ấn vừa phải → giữ 15–30 giây); **trong lúc đo** là thẻ hướng dẫn **đổi theo tình trạng thật** — *"Chưa thấy ngón tay"* → *"Tín hiệu yếu"* → *"Đang đo nhịp tim"* → *"Sắp xong"* — kèm **vòng tròn tiến độ 15–30 giây**, **số nhịp hiện dần**, **dạng sóng mạch đập** và **thanh "Tín hiệu"** (Mạnh / Trung bình / Yếu).
- **Thời lượng & cách chốt:** đo tối thiểu **15 giây** (`MIN_MEASURE_MS`), tự dừng ngay khi nhịp đã đều và tín hiệu đủ rõ; nếu chưa rõ thì tiếp tục tới **30 giây** (`MAX_MEASURE_MS`) rồi báo **"Chưa đo được nhịp tim"** — **không** hiện và **không** lưu một con số sai (đây là số liệu sức khỏe).
- **Lưu kết quả:** `SaveMeasuredHeartRateUseCase` ghi vào **`health_data_history`** (cột `heartRate` sẵn có — **không cần migration**) theo đúng luật gộp của `HealthSyncWorker`: chưa có hàng của hôm nay thì thêm mới, đã có thì **chỉ thay nhịp tim**, giữ nguyên bước chân/giờ ngủ/calo. Nhờ vậy số vừa đo hiện ngay ở ô "Tim" và **Rule Engine ở tab Phân tích** dùng được.
- **Nguồn hiển thị ở ô "Tim":** do switch quyết định — TẮT (mặc định) → nhịp tim trong **bản chụp đã lưu**; BẬT → **nhịp trung bình của Health Connect từ 00:00 hôm nay tới hiện tại** (`getTodayHeartRate()`), không còn lấy "1 giờ gần nhất". Nguồn đang chọn **không có số** thì dùng **nhịp ghi trong ngày** (`getLatestRecordedHeartRate()`) — tức lần đo bằng camera. Ô "Tim" **không** còn dòng chú thích nguồn; hôm nay chưa có số nào thì hiện **`-- bpm`** (không hiện `0 bpm`). Màn Trang chủ đọc lại số này mỗi lần hiện ra nên đo xong quay về là thấy ngay.
- **Quyền:** dùng chung quyền **`CAMERA`** với màn Ghi chép; chưa cấp thì màn đo hiện thẻ *"Cần quyền Camera"*. Máy **không có đèn flash** (ví dụ emulator) vẫn vào được nhưng app **cảnh báo trước** là kết quả kém chính xác.
- ⚠️ **Giới hạn đã biết:** đây là **ước lượng** từ camera điện thoại, **không phải thiết bị y tế**. Tay rung, ấn ngón tay quá mạnh (chặn dòng máu), che hờ camera, ánh sáng phòng nhấp nháy hoặc thiếu đèn flash đều làm sai kết quả; nhịp tim thật sự loạn nhịp cũng sẽ bị app từ chối kết luận (app đòi hỏi nhịp **đều**). Kiểm thử trên máy thật theo **TC-HR-01…TC-HR-13** trong `TESTCASE_HealthyAI.md`.
- **Lưu ý build:** CameraX trả về `ListenableFuture` của Guava (`ProcessCameraProvider.getInstance`, `CameraControl.enableTorch`), nhưng Gradle mặc định chọn gói **rỗng** `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` → Kotlin không biên dịch được lời gọi nào trả về kiểu đó. `app/build.gradle.kts` **ép** module này về bản `1.0` (`resolutionStrategy.force`, không dùng `strictly` vì một phụ thuộc khác yêu cầu thẳng bản rỗng) để phần đèn flash hoạt động.

---

## Chọn nguồn dữ liệu cho thẻ "Tóm tắt sức khỏe" (switch trên Trang chủ)

- Thẻ **"Dùng dữ liệu Health Connect"** (`presentation/screen/home/HealthDataSourceCard.kt`) nằm **ngay dưới** thẻ "Tóm tắt sức khỏe" và quyết định **nguồn số** cho cả 3 ô Bước chân / Ngủ / Tim:
  - **TẮT (mặc định)** → dùng **dữ liệu đã lưu trong app**: bản chụp sức khỏe của **hôm nay** trong `health_data_history` (hàng mới nhất kể từ 00:00), do `HealthSyncWorker` đồng bộ hoặc lần đo nhịp tim bằng camera ghi ra.
  - **BẬT** → đọc **trực tiếp từ Health Connect** (`getTodayHealthData()`), có thể gồm cả dữ liệu của **đồng hồ đeo tay**.
- **Dự phòng:** nguồn đang chọn **không có số** cho một chỉ số thì ô đó lấy **nguồn tự đo của máy** — bước chân từ **cảm biến đếm bước**, giờ ngủ từ **bộ ước lượng giấc ngủ**, nhịp tim từ **lần đo bằng camera** trong ngày; hôm nay chưa có gì thì ô "Tim" hiện **`-- bpm`**.
- **Chỉ đụng tới nguồn HIỂN THỊ của màn Trang chủ.** Đổi switch **không** xoá dữ liệu và **không** đổi `HealthSyncWorker` (worker vẫn đồng bộ Health Connect vào `health_data_history` 6 giờ/lần bất kể switch). **Widget "Sức khỏe hôm nay" dùng chung luật với thẻ "Tóm tắt sức khỏe"**: widget đọc **đúng nguồn đang chọn** qua `TodayHealthSummary.build` nên hai nơi luôn ra **cùng số** (khác biệt duy nhất: widget đọc nguồn đó một lần lúc **vẽ lại**, không theo `Flow` như màn Trang chủ).
- **Lưu thiết lập:** bảng Room `app_settings`, khoá **`health_connect_data_enabled`** (`true`/`false`), qua `SettingsRepository` → `data/repository/SettingsRepositoryImpl.kt`; chưa có khoá hoặc giá trị hỏng ⇒ rơi về **TẮT** (hướng an toàn — không tự đọc Health Connect khi người dùng chưa bật). Màn Trang chủ đọc bằng `Flow` nên switch cập nhật ngay.
- **Nạp lại khi đổi nguồn:** bật/tắt switch gọi `HomeViewModel.setHealthConnectDataEnabled()` → ghi thiết lập rồi **đọc lại ngay** nguồn vừa chọn; mỗi lần **quay về** màn Trang chủ (`ON_RESUME`) cũng đọc lại **nguồn đang chọn** nên số luôn khớp nguồn.
- Vì mặc định **TẮT**, khi máy **chưa cài Health Connect** thẻ "Tóm tắt sức khỏe" **vẫn** có số từ dữ liệu trong app + cảm biến của máy (không phụ thuộc Health Connect).

---

## Tóm tắt sức khỏe đọc lại theo ngày & lịch sử 7 ngày (tab Phân tích)

- **Nhịp tim cũng "đọc lại từ đầu ngày" như bước chân và giờ ngủ.** Trước đây `getLatestRecordedHeartRate()` đọc hàng **mới nhất trong cả bảng** (không lọc ngày), nên sang ngày mới ô **"Tim"** vẫn hiện số của **hôm qua**; `getRecentHeartRate()` cũng chỉ lấy **1 giờ gần nhất** nên 00:05 còn dính số của tối hôm trước. Nay:
  - Health Connect: `getRecentHeartRate()` → **`getTodayHeartRate()`** — nhịp **trung bình từ 00:00 hôm nay tới hiện tại** (`HealthConnectDataSourceImpl`), cùng mốc thời gian với `getTodaySteps()` / `getTodayActiveCalories()`.
  - Bản ghi trên máy: `HealthDataDao.getLatestHeartRateSince(fromTs)` + `HealthDataRepositoryImpl.startOfToday()` → chỉ đọc hàng có `recordedAt >= 00:00 hôm nay`.
  - Ô "Tim" hôm nay chưa có số nào → hiện **`-- bpm`** (`formatHeartRate()` trong `HomeScreen.kt`), **không** hiện `0 bpm` vì số 0 sẽ bị đọc là "nhịp tim bằng 0". Số của hôm qua **không mất** — nó nằm trong lịch sử 7 ngày bên dưới.
- **Thẻ "Tóm tắt sức khỏe" bỏ các dòng chú thích nguồn** (*"Số bước đọc trực tiếp từ cảm biến…"*, *"Giờ ngủ ước lượng từ cảm biến…"*, *"Nhịp tim ghi gần nhất trên máy…"*). Thẻ chỉ còn tiêu đề + 3 vòng tròn chỉ số (Bước chân / Ngủ / Tim) + nút **"Đo ngay"** ở ô Tim — gọn hơn và không phải cuộn qua phần giải thích dài.
- **Lịch sử tóm tắt sức khỏe 7 ngày nằm ở tab Phân tích** — thẻ **"Lịch sử sức khỏe 7 ngày"** (ngay dưới "Chỉ số sinh lực"):
  - **Biểu đồ cột số bước** của 7 ngày: mốc 100% là **ngày đi nhiều nhất trong tuần**, trên mỗi cột là số bước rút gọn, dưới là nhãn thứ (`T2…CN`), **hôm nay** được tô xanh đậm.
  - **Bảng 7 dòng** — mỗi ngày một dòng: **Ngày · Bước · Ngủ · Tim**, thứ tự **cũ → mới**. Ngày **chưa ghi được gì** hiện **"Chưa có dữ liệu"**; ô **thiếu riêng một chỉ số** hiện **`—`** (không hiện số `0`, vì 0 bị hiểu là "ngày đó đi bộ 0 bước / ngủ 0 giờ").
  - **Nguồn số:** `domain/usecase/health/GetRecentHealthSummaryUseCase.kt` → `HealthDataRepository.getHealthHistorySince(fromTs)` → `HealthDataDao.getHealthDataSince` (**cắt theo mốc thời gian**, không cắt theo số hàng — dữ liệu cũ có thể có nhiều hàng trong cùng một ngày) → `domain/usecase/health/HealthSummaryHistory.kt` (hàm **thuần Kotlin**, test trên JVM) gom thành `DailyHealthSummary` cho đúng 7 ngày, mỗi ngày lấy **hàng mới nhất** (hàng đã gộp là hàng đầy đủ nhất — cùng luật với `HabitRuleEngine.latestRowPerDay`).
  - **Cùng một nguồn số với AI:** đây chính là dữ liệu `HabitRuleEngine` đọc để sinh gợi ý ở đầu màn Phân tích, nên con số người dùng nhìn thấy và lý do AI gợi ý luôn khớp nhau. Thẻ đọc bằng `Flow` của Room nên vừa đo nhịp tim hoặc vừa đồng bộ xong là cập nhật ngay.
  - ⚠️ Lịch sử **chỉ có dòng cho ngày thật sự ghi được số liệu** (`syncHealthData()` không ghi hàng toàn số 0) → máy không có nguồn dữ liệu vài hôm thì các hôm đó hiện "Chưa có dữ liệu" thay vì số sai.

---

## Thông báo nhắc nhở (`ReminderWorker`)- Job `daily_reminder_worker` chạy mỗi 24h và gửi thông báo **"HealthyAI 💪 Nhắc nhở thói quen"** (kênh `healthy_ai_reminders`).
- Trên **Android 13+**, app cần quyền `POST_NOTIFICATIONS`: màn Trang chủ hiện thẻ **"Bật nhắc nhở hàng ngày"** → bấm **"Bật thông báo"** để mở hộp thoại quyền của hệ thống.
- Nếu người dùng đã **tắt thông báo ở cấp app** trong Settings (quyền đã cấp), thẻ đổi thành **"Thông báo đang bị tắt"** + nút **"Mở cài đặt thông báo"**.
- Thẻ tự biến mất khi thông báo đã bật; trạng thái được kiểm tra lại mỗi khi quay về màn Trang chủ.
- Chưa cấp quyền thì `ReminderWorker` vẫn trả `success` nhưng không có thông báo (`SecurityException` được bắt) — app không crash.

---

## Ngôn ngữ & chuỗi hiển thị (i18n)

- **Mọi chuỗi hiển thị nằm ở `app/src/main/res/values/strings.xml`** (221 `<string>`) và được đọc bằng
  `stringResource(R.string.…)` trong Compose, `context.getString(R.string.…)` ở Glance widget
  (`HealthWidget` dùng `androidx.glance.LocalContext`), `ReminderWorker` (tên/mô tả kênh thông báo,
  tiêu đề/nội dung) và các `Toast` trong `LaunchedEffect` (`HabitsScreen`, `AddEditHabitScreen`).
- **Không** còn chuỗi tiếng Việt viết cứng trong màn hình: nhãn 4 tab bottom bar nay là
  `Screen.titleRes` (`@StringRes`) + `R.string.nav_*`; bộ lọc màn Thói quen nay là **enum `HabitFilter`**
  (`labelRes` + `timeOfDay`) thay cho `listOf("Tất cả", "Sáng", "Chiều", "Tối")`; tên widget trên màn
  hình chính dùng `@string/widget_label`.
- **Chữ hiển thị không đổi** so với trước — chỉ đổi nguồn lấy chuỗi. Muốn thêm tiếng Anh: tạo
  `res/values-en/strings.xml`, **không** phải sửa code Kotlin (hiện chưa có `values-<locale>` nào).
- **Chưa** đưa vào `strings.xml` (xem phụ lục C #41): thông báo lỗi sinh trong **ViewModel**, nội dung gợi
  ý AI (`SuggestionCatalog`), tên danh mục/buổi/xếp loại trong **domain model** (`HabitCategory.displayName`,
  `HabitTimeOfDay.displayName`, `SleepQuality.label`…), nhãn tuần `T2…CN`, các hàm định dạng số
  (`TimeDisplay`, `HabitProgressText`) và dữ liệu mẫu trong `@Preview`.

---

## Những điều CHƯA làm (chủ ý) và đã chốt với team

- **Phát hành bản `release`** (`signingConfig` + `isShrinkResources` + ProGuard keep cho Firebase/Glance):
  **không làm** vì app chỉ **chạy thử nghiệm, không phát hành** (phụ lục C #19, #38). Bản `debug` build và
  cài bình thường.
- **Kiểm thử phần CameraX/`PreviewView`** (chụp ảnh, bind lifecycle) vẫn cần **emulator/máy thật** — không
  chạy được trên JVM (phụ lục C #23); các phần còn lại của màn Ghi chép đã có unit test
  (`CameraViewModelTest`, `FoodAnalysisTest`, `GeminiFoodInsightSourceTest`).
- **Thẻ "Xu hướng chất lượng giấc ngủ"** đọc lại Health Connect khi **quay về** màn Phân tích (không đọc
  liên tục trong lúc màn đang mở) — Health Connect không cung cấp Flow cho `SleepSessionRecord.stages`.
- **Quyền thừa trong manifest** (đã ghi nhận, chưa dọn — phụ lục C #45, #46): `SCHEDULE_EXACT_ALARM`
  không được dùng ở đâu (không có `AlarmManager`/`setExact*`) và `RECEIVE_BOOT_COMPLETED` trùng khai báo
  của `work-runtime`. Cả hai **không ảnh hưởng build/hành vi**; dọn bằng cách xoá 2 dòng trong manifest.
- **API đọc lịch sử sức khỏe theo SỐ HÀNG** (`getRecentHealthHistory`, `getRecentHealthHistoryFlow`) và
  `InsightsUiState.lastAnalyzedDays` hiện **không còn nơi dùng** trong code sản phẩm (sau khi #42 chuyển
  sang `getHealthHistorySince`) — giữ lại làm API dự phòng, chưa xoá (phụ lục C #47).

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
- [x] **Module 3 (AI)**: Rule Engine on-device + AI Camera setup
- [x] **Privilege/network của AI camera**: đọc key Gemini từ `local.properties` (gitignore) → `BuildConfig`; thiếu key thì app vẫn chạy và báo rõ thay vì crash
- [x] **Đăng nhập/Đăng ký**: Firebase Auth (email + mật khẩu) + chế độ khách
- [ ] **Module 2 (Cloud)**: Firebase Firestore sync (optional)
- [x] **CameraX Preview**: `LifecycleCameraController` + `PreviewView` trong `AndroidView`, nút "Phân tích món ăn" chụp ảnh qua CameraX → **AI (Gemini)** (`CameraScreen`)
- [x] **Chọn ảnh từ thư viện để phân tích**: nút **"Chọn ảnh từ thư viện"** ở màn Ghi chép mở **photo picker của hệ thống** (`PickVisualMedia`, **không** cần quyền đọc thư viện) → `GalleryImageLoader` đọc ảnh **thu nhỏ** (cạnh dài ~1280 px, tránh `OutOfMemoryError` với ảnh 12–50 MP) + **xoay theo EXIF** → dùng đúng luồng phân tích AI nên kết quả vẫn là **tên món tiếng Việt**; nút này **không** cần quyền Camera, huỷ chọn ảnh thì im lặng, ảnh đọc lỗi thì báo ở thẻ lỗi (`GalleryImageLoader`, `CameraScreen`, `CameraViewModel`)
- [x] **AI phân tích món ăn (bỏ ML Kit)**: ~~`FoodRecognition` lọc nhãn ảnh ML Kit~~ → **đã xoá**: ML Kit chỉ gán **nhãn ảnh chung** (~400 nhãn tiếng Anh) nên chụp **đĩa cơm** có lúc ra **"Pizza"**, và kể cả khi đúng thì cũng chỉ có **tên món** — không nói được *"ăn vào lợi/hại gì"*. Nay màn Ghi chép gọi **Gemini**: prompt tiếng Việt (`FoodAnalysis.buildPrompt()` — ưu tiên món **Việt/Á**, phủ **trái cây/đồ uống/đồ ăn vặt**, ghi **"(có thể)"** khi không chắc, **cấm** chẩn đoán/chữa bệnh) + `responseSchema` ép JSON, kết quả được **làm sạch** (`toInsight`: ≤3 gạch/mục, ≤120 ký tự, bỏ rỗng/trùng); UI hiện **tên món + "Có lợi cho cơ thể" + "Nên lưu ý" + miễn trừ trách nhiệm**; lỗi được phân loại (mất mạng / AI quá tải / khoá sai / chưa cấu hình) và có nút **"Thử lại"**. **Riêng tư:** ảnh **rời thiết bị** khi người dùng **chủ động bấm phân tích** — nói rõ trong `privacy_camera_body` (chi tiết + thay đổi sau này: xem mục 3 #43) (`domain/usecase/food/`, `data/network/`, `data/vision/`, `CameraViewModel`, `CameraScreen`)
- [x] **Thêm kết quả camera vào nhật ký**: nút **"Thêm vào nhật ký"** ở thẻ kết quả nhận diện → chuyển sang tab Thói quen và **mở sẵn hộp thoại ghi nhận** của thói quen **Dinh dưỡng** (dùng chung `AppNavigation.openHabitLogFor` với nút gợi ý AI); chưa có thói quen thì Toast nhắc tạo mới (`CameraScreen`, `AppNavigation`, `HabitsViewModel`)
- [x] **Gợi ý vận động đúng theo đơn vị**: log đơn vị **bước** giữ ngưỡng 5 000/8 000 bước; thói quen đo bằng đơn vị khác (phút, km…) so theo **tỉ lệ hoàn thành mục tiêu** (< 50% → HIGH, < 80% → LOW) — trước đây "30 phút" bị đọc thành "30 bước" (`HabitRuleEngine.analyzeExerciseLogs`)
- [x] **Dữ liệu Health Connect gom theo NGÀY**: `health_data_history` ghi 1 hàng mỗi lần sync 6 giờ → rule engine lấy hàng mới nhất của từng ngày, cảnh báo thiếu ngủ cần **3 ngày liên tiếp** và "nhịp tim trung bình 7 ngày" nay đúng là 7 ngày (`HabitRuleEngine.analyzeHealthTrends`)
- [x] **Glance Widget — "Sức khỏe hôm nay" (bước chân · giấc ngủ · nhịp tim)**: ~~widget hiển thị tiến độ thói quen (`TodayHabitProgress`: dòng `x/y thói quen đạt mục tiêu` + thanh tiến độ + tối đa 3 dòng `✓/○ tên — đã ghi/mục tiêu`)~~ — **đã thay**: bố cục đó phải nhồi tiêu đề + dòng `x/y` + thanh tiến độ + danh sách vào khung 150×140dp nên rất chật, màu lại lấy từ `GlanceTheme` (mặc định là **màu động** của hệ thống ⇒ trên Android 12+ hiện tông tím lệch bản sắc xanh của app). Widget mới hiện **3 cột chỉ số của hôm nay** — icon vector riêng (có bản `drawable-night/`), số đo cỡ lớn đậm, nhãn nhỏ đúng màu nhấn — dưới dòng tiêu đề **"Healthy AI · Hôm nay"**; màu lấy từ palette app qua `ColorProvider(sáng, tối)`; khung mặc định **250×130dp**, bo góc 24dp, `sizeMode = SizeMode.Exact` nên kéo rộng là ba cột nở ra theo; thứ tự ưu tiên nguồn dùng **chung một hàm** với app (`TodayHealthSummary.build`) và widget đọc **đúng nguồn mà thẻ "Tóm tắt sức khỏe" đang chọn** — switch **"Dùng dữ liệu Health Connect"** BẬT ⇒ widget đọc trực tiếp Health Connect, TẮT (mặc định) ⇒ widget đọc **bản chụp đã lưu trong app** (xem mục *"Chọn nguồn dữ liệu cho thẻ Tóm tắt sức khỏe"*) — nên số trên widget **khớp** số trên app; nguồn đang chọn không có số cho chỉ số nào thì ô đó mới rơi về **số app tự đo** (bước chân từ cảm biến của máy, giờ ngủ ước lượng, nhịp đo bằng camera), chưa có số nào thì hiện `--` / `-- bpm` thay vì `0`; tự vẽ lại **ngay** khi app ghi dữ liệu sức khỏe (đồng bộ xong / vừa đo nhịp tim: `HealthDataRepositoryImpl` → `HealthWidgetRefresher`), **ngay khi số bước đổi** trong lúc app đang đếm bước (`StepCounterRepositoryImpl` → `StepWidgetRefreshThrottle`, chặn nhịp tối đa một lần mỗi phút) và định kỳ 30 phút (`updatePeriodMillis`). Thay đổi ở **thói quen** không còn vẽ lại widget (`HabitRepositoryImpl` bỏ `HabitWidgetRefresher`) vì nội dung widget không còn dữ liệu thói quen — `HealthWidget`, `TodayHealthSummary`, `HealthWidgetRefresher`, `res/xml/health_widget_info.xml`, `res/drawable*/ic_widget_*.xml`
- [x] **Migration**: Room database migration khi tăng version — `MIGRATION_1_2`, `MIGRATION_2_3` trong `data/local/migration/DatabaseMigrations.kt`; đã **bỏ** `fallbackToDestructiveMigration()` nên nâng version schema **không** còn xoá thói quen/nhật ký (`DatabaseMigrationsTest` chặn việc bỏ sót bước migration)
- [x] **Android Keystore**: passphrase SQLCipher **không còn hard-code** — sinh ngẫu nhiên 32 byte ở lần chạy đầu, bọc bằng khoá AES-256/GCM trong Android Keystore, bản mã lưu ở prefs `healthy_ai_secure_store`; máy đã cài bản cũ được **rekey một lần** sang khoá mới nên không mất dữ liệu (`data/local/security/`)
- [x] **Đếm bước bằng cảm biến của máy**: app **tự đếm bước** bằng `Sensor.TYPE_STEP_COUNTER` (không cần Health Connect hay app sức khỏe khác) — xin quyền `ACTIVITY_RECOGNITION` bằng thẻ ở Trang chủ, tự xử lý **khởi động lại máy** (bộ đếm reset) và **sang ngày mới** (nội suy mốc 00:00); Health Connect vẫn được ưu tiên khi có số, không có thì hiển thị số từ cảm biến (`domain/usecase/steps/`, `data/steps/`, `StepCounterPermissionCard`). **Xem thêm** bullet *"Đếm bước chỉ khi app đang chạy"* bên dưới (mục 3 #44) — luật đếm đã được siết lại.
- [x] **Tự tính số giờ ngủ bằng cảm biến của máy**: app tự suy ra giấc ngủ từ **ánh sáng + gia tốc + trạng thái màn hình** (không cần quyền nào) — `SleepTrackerWorker` lấy mẫu 15 phút/lần, `SleepTrackerMath` cộng dồn theo khung ngủ (gắn với ngày thức dậy); số hiển thị theo **switch "Dùng dữ liệu Health Connect"** (BẬT → Health Connect/đồng hồ đeo tay; TẮT → bản chụp trong app), nguồn đang chọn không có số thì dùng ước lượng và ghi vào `health_data_history` (`domain/usecase/sleep/`, `data/sleep/`, `data/local/sleep/`)
- [x] **Khung giờ ngủ do người dùng đặt**: thẻ **"Khung giờ ngủ"** ở Trang chủ cho người dùng chọn *giờ đi ngủ* + *giờ thức* (mặc định 23:00 → 07:00) thay vì app tự quyết định khung đêm 18:00–12:00; lưu ở bảng Room **`app_settings`** (khoá – giá trị, `MIGRATION_3_4`) qua `SettingsRepository`, màn Home đọc bằng `Flow` nên đổi là cập nhật ngay (`SleepSchedule`, `SleepScheduleCard`, `HomeViewModel`)
- [x] **Đo nhịp tim bằng camera + đèn flash (PPG)**: ô **"Tim"** ở Trang chủ có nút **"Đo ngay"** mở màn **"Đo nhịp tim"** — đèn flash chiếu xuyên mô ngón tay, camera đọc **kênh đỏ từng khung hình** (`CameraPpgAnalyzer`), thuật toán tự viết (`PpgAnalyzer`: lọc mượt AC → bỏ DC → tìm đỉnh mạch bằng ngưỡng thích ứng + nội suy parabol → **kiểm tra tính chu kỳ bằng tự tương quan**) quy ra nhịp; **UI hướng dẫn theo tình trạng đo** (chưa thấy ngón tay / tín hiệu yếu / đang đo / sắp xong) + vòng tròn tiến độ 15–30 giây + dạng sóng mạch đập; kết quả **tự lưu** vào `health_data_history` (được dùng khi nguồn đang chọn không có số), app **không** kết luận khi tín hiệu nhiễu (`domain/usecase/heartrate/`, `data/heartrate/`, `presentation/screen/heartrate/`, `Screen.MeasureHeartRate`)
- [x] **Nhịp tim đọc lại theo NGÀY + lịch sử sức khỏe 7 ngày ở tab Phân tích**: nhịp tim nay "đọc lại từ đầu ngày" như bước chân/giờ ngủ — `getRecentHeartRate()` → **`getTodayHeartRate()`** (trung bình **00:00 → hiện tại**) và `getLatestRecordedHeartRate()` chỉ đọc hàng ghi **trong hôm nay** (`HealthDataDao.getLatestHeartRateSince`), nên sang ngày mới ô **"Tim"** hiện **`-- bpm`** thay vì số của hôm qua; thẻ "Tóm tắt sức khỏe" **bỏ 3 dòng chú thích nguồn** cho gọn; tab Phân tích thêm thẻ **"Lịch sử sức khỏe 7 ngày"** (**biểu đồ cột bước chân** + **bảng 7 dòng** ngày · bước chân · giờ ngủ · nhịp tim) đọc từ `health_data_history` qua `GetRecentHealthSummaryUseCase` + `HealthSummaryHistory` (mỗi ngày lấy hàng mới nhất, luôn đủ 7 ngày, ngày trống hiện "Chưa có dữ liệu") — **cùng nguồn số** mà Rule Engine dùng để sinh gợi ý AI (`domain/usecase/health/`, `data/local/dao/health/`, `presentation/screen/insights/`, `presentation/screen/home/`)
- [x] **i18n — chuỗi hiển thị ra `strings.xml`**: **233** chuỗi ở `res/values/strings.xml`, **~215** chỗ đọc bằng `stringResource`/`context.getString` (Compose + Glance widget + worker thông báo + Toast); nhãn tab đổi sang `Screen.titleRes`; bộ lọc màn Thói quen thành enum `HabitFilter` (`labelRes`); tên widget dùng `@string/widget_label`; **chữ trên màn hình không đổi** (phụ lục C #41, TC-ENV-08)
- [x] **Không nuốt `CancellationException`**: 14 chỗ `catch (e: Exception)` trong hàm `suspend`/worker nay rethrow `CancellationException` trước, và **18** chỗ `runCatching` bọc lời gọi `suspend`/I/O đổi sang `util/runCatchingCancellable` — huỷ coroutine không còn bị biến thành "lỗi"/"thành công giả" (phụ lục C #39, TC-WM-17)
- [x] **Hợp đồng dữ liệu sức khỏe rõ ràng**: `getHealthData(start, end)` (bỏ qua cả hai tham số) → **`getTodayHealthData()`**; tab Phân tích đọc lịch sử sức khỏe bằng `getHealthHistorySince` (**cắt theo mốc thời gian**, đủ 7 ngày) thay vì `getRecentHealthHistoryFlow(7)` (**cắt theo số hàng**) nên luật "3 ngày liên tiếp" của Rule Engine luôn thấy đủ dữ liệu (phụ lục C #42)
- [x] **Tab Phân tích không còn im lặng khi lỗi**: `InsightsUiState` có `error` + `sleepStagesError` (+ `visibleError`) và hiện **thẻ lỗi đỏ** đóng được; `HealthSyncWorker` trả **`retry`** (tối đa 3 lần) rồi **`failure`** khi đồng bộ lỗi thay vì luôn `success`; thêm **`InsightsViewModelTest` (6 test)** (phụ lục C #43, TC-INS-39)
- [x] **Thẻ giấc ngủ có số liệu trên mọi máy**: máy chỉ báo `STAGE_TYPE_SLEEPING` (không chia pha) nay hiện **tổng thời gian ngủ** (`SleepStageBreakdown.unstagedMinutes` + `hasStages`) thay vì thẻ rỗng; và **tự đọc lại** mỗi lần **quay về** màn Phân tích (`LifecycleResumeEffect` → `refresh()`) (phụ lục C #25, #26, TC-INS-37, TC-INS-38)
- [x] **Biểu đồ nhất quán tuần là snapshot theo ngày**: mẫu số mỗi ngày chỉ tính thói quen **đã tồn tại** ngày đó (`habit.createdAt`), nên tạo thói quen mới **không** làm tụt tỉ lệ các ngày trước và xoá thói quen **không** làm tỉ lệ quá khứ vọt lên (phụ lục C #27, TC-INS-21)
- [x] **Manifest chỉ xin quyền ĐỌC Health Connect**: bỏ `WRITE_STEPS`/`WRITE_SLEEP` cho khớp đúng 4 quyền app thật sự dùng (phụ lục C #44, TC-HC-13)
- [x] **Feed "BLOOM AI GỢI Ý" là banner trượt tự động**: tab Phân tích không còn xếp dọc các thẻ gợi ý làm màn hình dài — cả feed nằm trong **một banner cao cố định 240dp** chỉ hiện **một gợi ý mỗi lúc**, **tự trượt** sang gợi ý kế tiếp sau ~5 giây, **vuốt tay** xem lại được và có **chấm chỉ báo**; bộ mẹo vẫn **xoay vòng theo ngày** nên **mỗi ngày một bộ khác, không trùng nhau** (`AiTipBanner` trong `InsightsScreen` — mục 3 #41, phụ lục C #48, TC-INS-40, TC-INS-41)
- [x] **AI phân tích món ăn trả lời nhanh hơn**: đổi model mặc định sang **`gemini-3.8-flash`** và **hạ mức suy luận** bằng `thinkingConfig.thinkingLevel = "low"` — Gemini 3 **mặc định `high`**, đo thật một lượt đọc ảnh mất **~42 giây**; `"minimal"` **bị model này từ chối** (`400 INVALID_ARGUMENT`) nên `"low"` là mức thấp nhất dùng được, và nếu model không nhận `thinkingConfig` thì lớp gọi **tự gửi lại không kèm tham số** đó (mục 3 #43; `GeminiFoodInsightSource`, `app/build.gradle.kts`, `local.properties`)
- [x] **Model dự phòng khi Google quá tải**: `503 UNAVAILABLE` (*"high demand"*) và `429 RESOURCE_EXHAUSTED` (*"exceeded your current quota"*) đã gặp thật rất thường xuyên ⇒ model chính chạy trước, hỏng vì quá tải thì **tự chuyển sang `gemini-flash-lite-latest`** (bản nhẹ, phản hồi nhanh) thay vì chỉ thử lại chính model vừa hỏng; lỗi không tự khỏi (sai khoá 401/403) thì **không** đổi model cho đỡ tốn thời gian (mục 3 #43, TC-CAM-25, TC-CAM-26)
- [x] **Màn Ghi chép bữa ăn gọn hơn + phản hồi rõ khi AI đang đọc ảnh**: bỏ **công tắc "Phân tích món ăn bằng AI"** và 2 dòng ghi chú "Đang BẬT/TẮT…" (cùng cổng `food_ai_enabled` trong `SettingsRepository`) — bấm "Phân tích món ăn" là chạy luôn; thay vào đó có **hộp thoại "Đang phân tích món ăn"** (vòng xoay + mô tả, không tắt được bằng Back/chạm ra ngoài) để người dùng biết AI đang làm việc; lối vào **Chính sách bảo mật** chuyển từ màn Ghi chép sang **tab Phân tích AI** (thẻ "AI & quyền riêng tư"), `privacy_camera_body`/`privacy_intro`/`privacy_no_sale_body` viết lại cho khớp (`CameraScreen`, `CameraViewModel`, `SettingsRepository`, `InsightsScreen`, `AppNavigation`, `strings.xml` — mục 3 #43, TC-CAM-05, TC-CAM-06, TC-CAM-21, TC-CAM-25, TC-INS-42, TC-PRIV-01, TC-PRIV-04)
- [x] **Đếm bước chỉ khi app đang chạy**: `TYPE_STEP_COUNTER` **vẫn đếm** cả lúc app đã bị đóng, mà bản cũ lấy thẳng hiệu số (thậm chí **suy diễn ngược**: chưa có dữ liệu + máy khởi động trong hôm nay ⇒ `baseline = 0`; sang ngày mới ⇒ **nội suy** mốc 00:00) nên mở app là số bước **"tự nhảy"** thêm theo phần đã đi lúc app không chạy. Nay mỗi lần **tiến trình app được tạo** là một **phiên** (`StepCounterStore.sessionId`, lưu ở prefs khoá `session_id` — **không** cần migration): **phiên đổi** ⇒ `baseline = cumulative` (đếm lại từ mẫu hiện tại) nhưng **giữ `offset`** = số đã đếm trong ngày, và **nội suy mốc 00:00 chỉ còn dùng khi hai mẫu thuộc cùng một phiên**. Kết quả: app **đang mở** hoặc **còn nằm trong tác vụ** (chạy nền) → **đếm tiếp**; **đóng hẳn app** → khoảng thời gian ở giữa **không** được cộng vào nhưng số đã đếm trong ngày **vẫn giữ**; `sample()` bỏ tham số `bootTimeMs` (cùng `deviceBootTimeMs()`/`SystemClock`), mô tả thẻ xin quyền `ACTIVITY_RECOGNITION` nói rõ luật này (`StepCounterMath`, `StepCounterStore`, `AndroidStepCounterSensor`, `StepCounterRepositoryImpl`, `strings.xml` — mục 3 #44, TC-STEP-01…TC-STEP-10, TC-REG-32)
- [x] **Chọn nguồn dữ liệu cho thẻ "Tóm tắt sức khỏe" (switch trên Trang chủ)**: thêm thẻ **"Dùng dữ liệu Health Connect"** ngay dưới thẻ tóm tắt — **BẬT** ⇒ 3 ô Bước chân / Ngủ / Tim đọc **trực tiếp Health Connect** (`getTodayHealthData()`); **TẮT (mặc định)** ⇒ lấy **dữ liệu đã lưu trong app** (`health_data_history` của hôm nay qua `GetStoredHealthDataUseCase` → `HealthDataRepository.getTodayStoredHealthData()`), và app **không** gọi Health Connect để lấy số; nguồn đang chọn không có số thì rơi về **cảm biến tự đo của máy** (bước chân / ước lượng giấc ngủ / nhịp đo bằng camera). Lưu ở `app_settings` khoá **`health_connect_data_enabled`** (mặc định TẮT, giá trị hỏng ⇒ TẮT), đọc bằng `Flow`; bật/tắt switch — hoặc quay về màn Home — **nạp lại ngay nguồn vừa chọn**. `HealthSyncWorker` giữ nguyên luật cũ (vẫn đồng bộ Health Connect vào `health_data_history` 6 giờ/lần bất kể switch), còn **widget "Sức khỏe hôm nay" đọc đúng nguồn đang chọn** (`TodayHealthSummary` + `GetStoredHealthDataUseCase`) nên số trên widget **khớp** thẻ tóm tắt (`HealthConnectDataSettings`, `HealthDataSourceCard`, `GetStoredHealthDataUseCase`, `HomeViewModel`, `HomeScreen`, `SettingsRepository`, `HealthDataRepository`, `strings.xml`)
