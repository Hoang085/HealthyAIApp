# TESTCASE — HealthyAI App (Android)

> Tài liệu test case chức năng / dữ liệu / nền (background) cho toàn bộ luồng của app HealthyAI.
> Nguồn đối chiếu: mã nguồn tại `E:\HealthyAIApp` — branch `feature/dev-ui`, commit `91c1b1b`
> (`dev logic setup app and connet heath, firebase`).

| Thông tin | Giá trị |
|:--|:--|
| Phiên bản tài liệu | 1.0 |
| Ngày tạo | 2026-09-17 |
| Ngày cập nhật gần nhất | 2026-09-23 — **(mới nhất)** đã sửa **phụ lục C #14** (thêm Room **`Migration`** 1→2 và 2→3 trong `data/local/migration/DatabaseMigrations.kt`, **bỏ** `fallbackToDestructiveMigration()` → nâng version schema không còn xoá sạch thói quen/nhật ký), **#16** (`getRecentHealthHistoryFlow(limit)` nay tôn trọng `limit`; `HabitRuleEngine` **gom dữ liệu Health Connect theo NGÀY** thay vì theo lần sync 6 giờ), **#17** (gợi ý vận động so theo **đơn vị + mục tiêu** của chính thói quen thay vì 5 000/8 000 bất kể đơn vị) và **#22** (nút **"Thêm vào nhật ký"** ở màn Ghi chép đã nối vào `habit_logs` qua hộp thoại ghi nhận thói quen Dinh dưỡng) + hàng mới **#29** (passphrase SQLCipher ghi bằng `commit()` để không mất khoá khi tiến trình bị kill). Test case mới: **TC-DB-15, TC-DB-16, TC-INS-29, TC-INS-30, TC-CAM-15**; unit test mới `DatabaseMigrationsTest` (+ 6 test mới ở `HabitRuleEngineTest`, `HabitRuleEnginePersonalizedTest`, `HealthDataRepositoryImplTest`) → **22 test class / 138 test**. Cùng ngày, trước đó: đã sửa mục 3 **#19** (thêm **nút Đăng xuất** ở màn Trang chủ: `HomeScreen`/`GreetingHeader` → `AuthGateViewModel.signOut()` xoá **cả** phiên Firebase **và** cờ chế độ khách → quay lại màn đăng nhập, **không** mất dữ liệu cục bộ) — test case mới: TC-AUTH-15, TC-AUTH-16 + 2 class test mới; phụ lục C **#28**. Cùng ngày, trước đó: đã sửa mục 3 **#18** (màu nền hard-code: nền màn hình + nền `Card`/`Surface` nay lấy từ token theme `background`/`surfaceContainerLow` thay cho `#F8F9FA`/`Color.White` → dark mode không còn thẻ trắng làm mất chữ) — test case mới: TC-EDGE-16 + unit test `ThemeSurfaceColorsTest`; phụ lục C **#18**. Cùng ngày, trước đó: **2026-09-22** — đã sửa mục 3 **#17** (passphrase SQLCipher **không còn hard-code**: sinh ngẫu nhiên 32 byte ở lần chạy đầu, bọc bằng khoá **AES-256 trong Android Keystore**, bản mã lưu ở prefs `healthy_ai_secure_store`; kèm **migration rekey** cho máy đã cài bản cũ nên **không mất dữ liệu**) — test case mới: TC-DB-11…14. Cùng ngày, rà soát lại mục 3 **#17** và chỉnh số liệu trong TC-DB-11/TC-DB-14 cho khớp code đang chạy (bản mã lưu ở prefs là **~80 ký tự** Base64 = 60 byte; `DatabasePassphraseProviderTest` có **6 test**). Cùng ngày, trước đó: mục 3 **#20** (widget vẽ thanh tiến độ **thật** + **danh sách** thói quen thay cho thanh tĩnh ~40% và 1 thói quen; quy tắc buổi chỉ còn quyết định **thứ tự**) — test case mới: TC-WG-12. Cùng ngày, trước đó: mục 3 **#16** (widget hiển thị thói quen **theo buổi hiện tại** thay vì "mới tạo nhất") + bật cập nhật định kỳ 30 phút — TC-WG-10, TC-WG-11; mục 3 **#15** (widget không tự refresh) + phụ lục C **#15** — TC-WG-09; và mục 3 **#10** (phản hồi "Đã ghi nhận"), **#11** (số mục tiêu hiển thị gọn), **#12** (validate hộp thoại ghi nhận), **#13** (chống trùng tên), **#14** (nút hành động trong thẻ gợi ý AI) + phụ lục C **#10** (hiển thị lỗi ở màn Thói quen), **#11** (chặn số âm/0 khi ghi nhận), **#12** (chống double-submit) — test case: TC-HAB-24…30, TC-ADD-23…27, TC-INS-27, TC-INS-28 |
| Người thực hiện test | .......................... |
| Build/version test | `versionCode 1`, `versionName 1.0.0` |
| Ứng dụng (package) | `com.healthyai.app` |
| Nền tảng | Android, `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Kiến trúc | MVVM + Clean Architecture (Domain / Data / Presentation), Hilt DI |
| Số module test | 14 (+ phụ lục) |

---

## 1. Quy ước

### 1.1. Mã test case

| Tiền tố | Module |
|:--|:--|
| `TC-ENV-xx` | Môi trường, build, cài đặt, smoke test |
| `TC-INIT-xx` | Khởi động app, Hilt, đăng ký WorkManager |
| `TC-AUTH-xx` | Màn hình đăng nhập / đăng ký (Firebase Auth email + mật khẩu) |
| `TC-NAV-xx` | Điều hướng, Bottom Navigation, back stack |
| `TC-HOME-xx` | Màn hình Home |
| `TC-HAB-xx` | Màn hình Thói quen (danh sách, filter, ghi nhận) |
| `TC-ADD-xx` | Màn hình Thêm/Sửa/Xóa thói quen |
| `TC-CAM-xx` | Màn hình Ghi chép (Camera + ML Kit) |
| `TC-INS-xx` | Màn hình Phân tích (Rule Engine AI) |
| `TC-HC-xx` | Health Connect |
| `TC-DB-xx` | Room + SQLCipher (lưu trữ, mã hóa, migration) |
| `TC-WM-xx` | WorkManager (job nền, thông báo) |
| `TC-WG-xx` | Widget màn hình chính (Glance) |
| `TC-FB-xx` | Firebase Firestore sync (tùy chọn) |
| `TC-EDGE-xx` | Biên, lỗi, xoay màn hình, process death, dark mode |
| `TC-REG-xx` | Smoke / regression checklist |

### 1.2. Mức ưu tiên

| Ký hiệu | Ý nghĩa |
|:--|:--|
| **P0** | Bắt buộc pass, chặn release (crash, mất dữ liệu, không vào được app) |
| **P1** | Luồng nghiệp vụ chính, phải pass |
| **P2** | Phụ, hiển thị, biên nhỏ |

### 1.3. Trạng thái kết quả

`Not Run` / `Pass` / `Fail` / `Blocked` / `N/A`

### 1.4. Cách ghi kết quả

Ghi trực tiếp vào cột **KQ** ở bảng tổng hợp cuối mỗi module, kèm: thiết bị/API level, ảnh chụp, log `logcat` (tag: `HealthSyncWorker`, `SyncWorker`, `HealthDataRepository`).

---

## 2. Môi trường & chuẩn bị (setup bắt buộc)

### 2.1. Môi trường khuyến nghị

| Hạng mục | Yêu cầu |
|:--|:--|
| JDK | 21 (`jvmToolchain(21)`, `sourceCompatibility = VERSION_21`) |
| Android Studio | Ladybug trở lên (có **App Inspection**: Database Inspector, Background Task Inspector) |
| Gradle/AGP | AGP `8.13.2`, Kotlin `2.0.21`, KSP `2.0.21-1.0.28` |
| Emulator | **Pixel 7, API 34, có Google Play** (cần Google Play để cài Health Connect Toolbox) |
| Thiết bị thật | Khuyến nghị test thêm 1 máy Android 13+ để kiểm tra `POST_NOTIFICATIONS` |
| Dữ liệu mạng | Không bắt buộc với Health Connect/Room; bắt buộc với Firebase sync |

### 2.2. Lệnh build & cài

```powershell
cd E:\HealthyAIApp
.\gradlew clean assembleDebug
.\gradlew installDebug
.\gradlew test                 # unit test (JVM)
.\gradlew connectedAndroidTest # instrumented test (cần emulator/thiết bị)
```

Cài trực tiếp:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> ⚠️ Repo hiện **không có** `gradlew` / `gradlew.bat` (chưa commit Gradle wrapper). Nếu máy chưa có wrapper thì chạy bằng Gradle đã cài (`gradle ...`), Gradle trong `~/.gradle/wrapper/dists` (ví dụ `gradle-8.13`), hoặc mở Android Studio → **Run** / **Gradle tool window**. JDK phải là **21** (dùng JBR của Android Studio cũng được).

### 2.3. Lệnh hỗ trợ test

```powershell
# Xóa sạch dữ liệu app (reset giữa các test case) — bản debug cho phép
adb shell pm clear com.healthyai.app

# Xem DB (SQLCipher — Database Inspector KHÔNG đọc được file mã hóa)
adb shell run-as com.healthyai.app ls -l databases
adb exec-out run-as com.healthyai.app cat databases/healthy_ai_encrypted.db > "$env:TEMP\db.bin"
Select-String -Path "$env:TEMP\db.bin" -Pattern "SQLite format 3" -Encoding Byte   # phải KHÔNG thấy

# Passphrase (không hard-code): chỉ có BẢN MÃ Base64, khoá AES nằm trong Android Keystore
adb shell run-as com.healthyai.app cat shared_prefs/healthy_ai_secure_store.xml      # TC-DB-11
adb logcat -s DatabaseKeyMigration                                                   # log rekey DB bản cũ (TC-DB-12)

# Kiểm tra job nền của WorkManager
adb shell dumpsys jobscheduler | Select-String "healthyai"

# Chạy tay 1 worker (lấy jobId ở lệnh dumpsys phía trên)
adb shell cmd jobscheduler run -f com.healthyai.app <jobId>

# Log
adb logcat -s HealthSyncWorker SyncWorker HealthDataRepository AndroidRuntime:E
```

### 2.4. Cách chạy tay worker không cần chờ 6 giờ

Mở **Android Studio → View → Tool Windows → App Inspection → Background Task Inspector** → chọn tiến trình `com.healthyai.app` → chọn worker (`periodic_health_sync_worker`, `periodic_sync_worker`, `daily_reminder_worker`) → **Run**.

### 2.5. Tạo dữ liệu Health Connect giả

1. Cài **Health Connect Toolbox** (Google Play) trên emulator (cần emulator có Google Play).
2. Mở Toolbox → **Insert records** → chọn loại:
   - `StepsRecord` → ví dụ 8000 bước, khoảng thời gian 00:00 → hiện tại.
   - `SleepSessionRecord` → ví dụ 23:00 hôm qua → 06:30 hôm nay (≈ 7.5 h).
   - `HeartRateRecord` → ví dụ 90 bpm trong 1 giờ gần nhất.
   - `ActiveCaloriesBurnedRecord` → ví dụ 350 kcal hôm nay.
3. Trong app: **Settings của Health Connect** (hoặc Toolbox → App permissions) → chọn `HealthyAI` → cấp đủ 4 quyền đọc: Steps, Sleep, Heart rate, Active calories burned.

> ✅ Từ bản sửa lỗi mục 3 #7, app **có UI xin quyền**: màn Trang chủ hiện thẻ **"Cấp quyền Health Connect"** với nút mở thẳng hộp thoại quyền của Health Connect (xem **TC-HC-13**). Nếu chưa cấp quyền, `HealthConnectDataSourceImpl` vẫn bắt exception và trả về `0` (không crash) — nên vẫn có thể cấp thủ công như bước 3.

### 2.6. Bộ dữ liệu mẫu nên tạo trước khi test (Data set A)

| # | Tên thói quen | Danh mục | Mục tiêu | Đơn vị | Thời gian | Tần suất |
|:--|:--|:--|:--|:--|:--|:--|
| 1 | Uống nước | Uống nước 💧 | 2000 | ml | Cả ngày | Hàng ngày |
| 2 | Ngủ đủ giấc | Giấc ngủ 😴 | 8 | giờ | Buổi tối | Hàng ngày |
| 3 | Chạy bộ | Vận động 🏃 | 30 | phút | Buổi sáng | Hàng ngày |
| 4 | Ăn salad | Dinh dưỡng 🥗 | 600 | calo | Buổi chiều | Hàng ngày |
| 5 | Thiền 15 phút | Thiền định 🧘 | 15 | phút | Buổi sáng | Hàng ngày |

Đơn vị tự đổi theo danh mục (xem `AddEditHabitViewModel.onCategoryChange`):
`WATER → ml`, `SLEEP → giờ`, `EXERCISE → phút`, `NUTRITION → calo`, `MINDFULNESS → phút`.

---

## 3. ⚠️ Hành vi hiện tại của code — ĐỌC TRƯỚC KHI TEST

Các điểm dưới đây là **hành vi thật của code hiện tại**. Khi test, hãy đối chiếu với mục "Kết quả mong đợi" của từng test case. Những mục đánh dấu `[GAP]` là **chưa được triển khai / là bug tiềm ẩn** — test case tương ứng sẽ ghi rõ "mong đợi theo thiết kế" và "thực tế hiện tại" để bạn quyết định Fail hay ghi nhận là known issue.

| # | Hành vi | Chi tiết | Loại |
|:--|:--|:--|:--|
| 1 | ~~Home không có trạng thái "hoàn thành"~~ **→ Đã sửa** | `HomeViewModel` tính `isCompleted` từ tổng log trong ngày; thẻ hoàn thành hiển thị tick xanh + **"Hoàn thành"** | ✅ Đã sửa |
| 2 | ~~Tiến độ ở màn Thói quen bị mất sau khi mở lại app~~ **→ Đã sửa** | `HabitRepositoryImpl.startOfDayEpochMillis()` căn mốc theo **00:00** của ngày thay vì `now - daysBack ngày` | ✅ Đã sửa |
| 3 | ~~Không có CameraX preview thật~~ **→ Đã sửa** | `CameraPreviewSection` dùng `LifecycleCameraController` + `PreviewView` trong `AndroidView` | ✅ Đã sửa |
| 4 | ~~Nút "Phân tích món ăn" không làm gì~~ **→ Đã sửa** | Nút chụp ảnh qua CameraX (`takePicture`) → `ImageProxy.toBitmap()` → `CameraViewModel.analyzeBitmap()`; UI hiển thị nhãn + thẻ lỗi đỏ | ✅ Đã sửa |
| 5 | ~~Insights có dữ liệu tĩnh~~ **→ Đã sửa** | Biểu đồ nhất quán tuần + delta điểm sinh lực lấy từ `habit_logs`; thẻ giấc ngủ lấy `SleepSessionRecord.stages` thật từ Health Connect | ✅ Đã sửa |
| 6 | ~~Không có màn hình đăng nhập~~ **→ Đã sửa** | `LoginScreen` + `LoginViewModel` + `AuthGateViewModel`: đăng nhập/đăng ký **email + mật khẩu**, có nút **"Bỏ qua, dùng ở chế độ khách"**; `SyncWorker` bỏ qua (trả `success`) khi chưa đăng nhập thay vì `failure` | ✅ Đã sửa |
| 7 | ~~Không xin quyền Health Connect trong app~~ **→ Đã sửa** | Màn Home hiện thẻ **"Cấp quyền Health Connect"** (`HealthConnectPermissionCard` + `PermissionController.createRequestPermissionResultContract()`); thiếu quyền → xin lại khi quay lại màn hình; chưa cài / cần cập nhật → nút mở Play Store | ✅ Đã sửa |
| 8 | ~~Không xin quyền `POST_NOTIFICATIONS`~~ **→ Đã sửa** | Màn Home hiện thẻ **"Bật nhắc nhở hàng ngày"** (`ReminderNotificationCard` + `ActivityResultContracts.RequestPermission()`); nếu người dùng đã tắt thông báo ở cấp app thì thẻ đổi thành nút **"Mở cài đặt thông báo"** | ✅ Đã sửa |
| 9 | Xóa thói quen là soft delete | `HabitDao.softDeleteHabit` → `UPDATE habits SET isActive = 0`; hàng dữ liệu và log **vẫn còn** trong DB. Từ bản sửa lỗi #9: bấm thùng rác hiện **dialog xác nhận** ("Nhật ký đã ghi vẫn được giữ lại trong máy"), xóa xong có Toast **"Đã xóa thói quen"** | Thiết kế ✅ |
| 10 | ~~Không có phản hồi "đã ghi nhận"~~ **→ Đã sửa** | `HabitsViewModel.logHabit` đặt `showLogSuccessFor` = id thói quen vừa ghi (sự kiện một lần, UI xoá bằng `onLogSuccessShown()`); `HabitsScreen` hiện **Toast "Đã ghi nhận: <tên thói quen>"** | ✅ Đã sửa |
| 11 | ~~Ô `targetValue` hiển thị dạng float thô~~ **→ Đã sửa** | Hàm dùng chung `presentation/ui/format/NumberDisplay.kt` → `formatTargetValue()`: số nguyên bỏ đuôi thập phân (`2000f` → `"2000"`, `8f` → `"8"`), số thập phân giữ nguyên (`7.5f` → `"7.5"`). Dùng ở `AddEditHabitViewModel.loadHabit` (ô "Mục tiêu hàng ngày") và ở giá trị mặc định của hộp thoại ghi nhận (`HabitsScreen`). **Không** dùng format theo locale nên không sinh dấu `,` (vi-VN) làm `toFloatOrNull()` lúc lưu trả `null` | ✅ Đã sửa |
| 12 | ~~Hộp thoại ghi nhận mặc định = mục tiêu dạng float thô, nhập sai thì im lặng~~ **→ Đã sửa** | `logValue` mặc định = `formatTargetValue(habit.targetValue)` → điền sẵn **`2000`** (trước là `2000.0`). Bấm **Lưu** khi ô rỗng / không phải số / `NaN` → hộp thoại **vẫn mở**, ô nhập viền đỏ + dòng lỗi *"Vui lòng nhập số lượng"* hoặc *"Số lượng phải là số"*, **không** ghi log; sửa lại thì lỗi tự xoá (`parseLogValue()` trong `HabitLogInput.kt`). Còn **chưa** validate số âm/0 (phụ lục C #11) | ✅ Đã sửa |
| 13 | ~~Không kiểm tra trùng tên thói quen~~ **→ Đã sửa** | `AddEditHabitViewModel.saveHabit()` gọi `isNameTaken()` trước khi ghi DB: so với các thói quen **đang hoạt động** khác sau khi cắt khoảng trắng và **không phân biệt hoa/thường** → hiện hộp thoại lỗi **"Tên thói quen đã tồn tại"**, không tạo/cập nhật. Giữ nguyên tên cũ của chính thói quen đang sửa vẫn hợp lệ; tên của thói quen đã xóa mềm dùng lại được | ✅ Đã sửa |
| 14 | ~~Nút hành động trong thẻ gợi ý AI không hoạt động~~ **→ Đã sửa** | `InsightsScreen` phát danh mục của gợi ý (`onOpenHabitLog`); `AppNavigation` chuyển sang tab Thói quen và gửi danh mục qua `SavedStateHandle` (`KEY_LOG_CATEGORY`); `HabitsViewModel` tìm thói quen đang hoạt động thuộc danh mục đó → `LogRequest.Open` (mở sẵn hộp thoại ghi nhận, có Toast "Đã ghi nhận") hoặc `LogRequest.Missing` (Toast nhắc bấm `+` tạo mới) | ✅ Đã sửa |
| 15 | ~~Widget không tự refresh~~ **→ Đã sửa** | Thêm cổng `domain/widget/HabitWidgetRefresher` + impl `presentation/widget/HabitWidgetRefresherImpl` (Glance `updateAll`), bind trong `di/AppModule`; `HabitRepositoryImpl` gọi `refresh()` sau `insertHabit` / `deleteHabit` / `logHabit` → widget đọc lại `getAllActiveHabits()` và vẽ lại. Kèm `updatePeriodMillis="1800000"` (30 phút) để qua mốc buổi widget tự chọn lại (xem #16) | ✅ Đã sửa |
| 16 | ~~Widget hiển thị thói quen **mới nhất**~~ **→ Đã sửa** | Widget **không** còn lấy `getAllActiveHabits().first()`: từ #20 nó hiển thị **danh sách** và `TodayHabitProgress.orderForWidget()` dùng buổi để **xếp thứ tự** (buổi hiện tại trước → `Cả ngày` → còn lại; trong nhóm `createdAt` giảm dần). `habit_widget_info.xml` giữ `updatePeriodMillis="1800000"` (30 phút) | ✅ Đã sửa |
| 17 | ~~Passphrase SQLCipher hard-code~~ **→ Đã sửa** | `data/local/security/DatabasePassphraseProvider.kt` sinh passphrase **ngẫu nhiên 32 byte** ở lần chạy đầu, `AndroidPassphraseStorage.kt` bọc bằng khoá **AES-256/GCM trong Android Keystore** (khoá không rời thiết bị) và lưu bản mã `Base64(IV‖ciphertext)` ở prefs `healthy_ai_secure_store`. APK **không** còn chứa khoá nào mở được DB; các lần mở sau đọc lại đúng khoá đã lưu. Máy đã cài bản cũ (DB mã hoá bằng khoá hard-code) được `LegacyDatabaseKeyMigration` **rekey một lần** sang khoá Keystore → giữ nguyên thói quen/nhật ký thay vì crash *"file is not a database"*. `fallbackToDestructiveMigration()` vẫn giữ (phụ lục C #14) | ✅ Đã sửa |
| 18 | ~~Màu nền hard-code~~ **→ Đã sửa** | ~~Các màn hình đặt `containerColor = Color(0xFFF8F9FA)` (sáng) trong khi theme có Dark scheme → dark mode tương phản kém~~. Lỗi thực tế **nặng hơn**: `Card` còn để `containerColor = Color.White` nên ở dark mode thành **thẻ trắng** trong khi chữ dùng `onSurface` sáng ⇒ **mất chữ**, không chỉ "kém tương phản". Nay **nền màn hình** và **nền thẻ/panel** đều lấy từ theme: `Scaffold(containerColor = MaterialTheme.colorScheme.background)` ở 7 chỗ (`HomeScreen`, `HabitsScreen`, `AddEditHabitScreen`, `InsightsScreen`, `CameraScreen`, `LoginScreen`, và `Surface` của `AuthLoadingScreen` trong `AppNavigation`), **12** chỗ `Card`/`InfoActionCard` đổi `Color.White` → `MaterialTheme.colorScheme.surfaceContainerLow`, thẻ gợi ý AI (`AiTipCard` trong `InsightsScreen`) bỏ 3 nền cứng `#FFFFFF`/`#FFEBEE`/`#FFF3E0` → dùng `surfaceContainerLow` + **lớp màu ưu tiên mờ 12%** (giữ tín hiệu CRITICAL/HIGH), badge "Xong" ở `HabitsScreen` dùng `surfaceVariant`. Thêm token `surfaceContainerLow` vào `Color.kt`/`Theme.kt` (sáng **`#FFFFFF`**, tối **`#23261F`**) để thẻ vẫn nổi hơn nền ở **cả hai** chế độ. Chữ trên nền thẻ đạt **WCAG AA** (sáng 17.2:1, tối 11.9:1). **Không** đụng màu chữ/icon, màu xanh thương hiệu, màu error và vệt nền thanh tiến độ (phụ lục C #18) | ✅ Đã sửa |
| 19 | ~~Chưa có nút **Đăng xuất**~~ **→ Đã sửa** | ~~Có màn đăng nhập (mục #6) nhưng không có UI đăng xuất: đã đăng nhập hoặc đã bấm "Bỏ qua" thì chỉ còn cách xoá dữ liệu app để quay lại màn đăng nhập. Nằm ngoài phạm vi đã chốt của bản sửa lỗi #6~~. Nay `AuthRepository` có `signOut()` (uỷ quyền `FirebaseAuth.signOut()`), `AuthGateViewModel.signOut()` xoá phiên **và** cờ chế độ khách → `SessionState.SignedOut`. UI: `IconButton` icon **Logout** ở góc phải lời chào màn Trang chủ (`HomeScreen.GreetingHeader`) + **hộp thoại xác nhận** ("Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên."). Áp dụng cho **cả** người đã đăng nhập lẫn chế độ khách; dữ liệu Room/SQLCipher **không** bị xoá. Chi tiết: TC-AUTH-15, TC-AUTH-16 | ✅ Đã sửa |
| 20 | ~~Widget vẽ thanh tiến độ **tĩnh ~40%** và chỉ hiển thị **1 thói quen**~~ **→ Đã sửa** | `TodayHabitProgress.build()` cộng `habit_logs` **hôm nay** (`getHabitLogsForPeriod(0)`) theo từng thói quen → widget hiển thị dòng **"x/y thói quen đạt mục tiêu"** + `LinearProgressIndicator` với `progress = completedCount / totalCount` (**thật**), rồi danh sách tối đa **3** dòng `✓/○ tên — đã ghi/mục tiêu` (`formatHabitProgress` dùng chung với màn Thói quen) và dòng `+N thói quen khác` khi nhiều hơn. Khung mặc định của widget nới từ `110dp` lên `minWidth 150dp`/`minHeight 140dp` cho đủ nội dung | ✅ Đã sửa |

---

## 4. TC-ENV — Môi trường, build, smoke test

### TC-ENV-01 — Build thành công bản debug
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã clone đủ mã nguồn, JDK 21, có Internet lần đầu.
- **Bước:**
  1. Chạy `.\gradlew clean assembleDebug` tại `E:\HealthyAIApp`.
- **Kết quả mong đợi:** `BUILD SUCCESSFUL`; sinh file `app\build\outputs\apk\debug\app-debug.apk`; không có lỗi KSP/Hilt/Room.
- **Ghi chú:** `google-services.json` phải tồn tại trong `app/` (plugin `com.google.gms.google-services` đang được áp dụng).

### TC-ENV-02 — Cài đặt lần đầu
- **Ưu tiên:** P0
- **Bước:** 1. `adb install -r ...app-debug.apk` → 2. Mở app từ launcher.
- **Kết quả mong đợi:** Cài thành công; icon app tên **HealthyAI**; app mở không crash.

### TC-ENV-03 — Unit test JVM
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test` (hoặc `.\gradlew :app:testDebugUnitTest` nếu chỉ chạy module `app`).
- **Kết quả mong đợi:** Toàn bộ test trong `app/src/test` pass — **22 test class / 138 test** (cập nhật 2026-09-23):
  - **Rule engine / AI**: `HabitRuleEngineTest` (rule ngủ/nước/vận động + **thói quen vận động đo bằng phút: dưới 50% mục tiêu → HIGH, 50–80% → LOW, đạt mục tiêu → không gợi ý, < 3 log → không kết luận** — phụ lục C #17), `HabitRuleEngineExtendedTest` (vitality score, water CRITICAL), `HabitRuleEnginePersonalizedTest` (health trends gom **theo ngày**: thiếu ngủ 3 ngày liên tiếp, **3 hàng cùng ngày → không cảnh báo**, **3 ngày cách quãng → không cảnh báo**, nhịp tim cao 7 ngày — phụ lục C #16)
  - **Domain / use case**: `GetWeeklyConsistencyUseCaseTest`, `GetVitalityScoreUseCaseTest`, `GetHealthConnectPermissionStatusUseCaseTest`, `TodayHabitProgressTest`
  - **Data**: `HabitRepositoryImplTest` (log đã đồng bộ, xóa mềm), `HealthDataRepositoryImplTest` (**truyền đúng `limit` xuống DAO** — phụ lục C #16), `HealthConnectStatusMappingTest` (map 3 → AVAILABLE, 2 → UPDATE_REQUIRED, 1/khác → NOT_INSTALLED)
  - **Bảo mật DB**: `DatabasePassphraseProviderTest` — passphrase SQLCipher sinh ngẫu nhiên 32 byte + lưu lại một lần, đọc lại đúng khoá cũ, mất khoá Keystore thì sinh khoá mới thay vì crash (mục 3 #17); `DatabaseMigrationsTest` — dãy migration phủ **liên tục** từ 1 tới `DatabaseMigrations.DATABASE_VERSION` (bỏ sót bước nào là fail) và migration 1→2 chỉ **ALTER** bảng, không drop (phụ lục C #14)
  - **ViewModel**: `HomeViewModelTest`, `HabitsViewModelTest` (phản hồi "Đã ghi nhận", **yêu cầu ghi nhận từ màn Phân tích**, **chặn số âm/0**, đóng thẻ lỗi), `AddEditHabitViewModelTest` (xóa mềm, định dạng mục tiêu, **chống trùng tên**, **chống double-submit**), `LoginViewModelTest`, `CoreTrackingViewModelTest`
  - **Hiển thị & nhập liệu**: `NumberDisplayTest` (số mục tiêu hiển thị kiểu "số sạch"), `HabitLogInputTest` (đọc/validate ô "Số lượng" của hộp thoại ghi nhận, **kể cả số âm/0**)
  - **Theme / màu nền**: `ThemeSurfaceColorsTest` (nền thẻ `surfaceContainerLow` **khác** và **sáng hơn** `background` ở cả hai chế độ, dark mode không còn thẻ trắng, chữ trên nền thẻ đạt WCAG AA — mục 3 #18, TC-EDGE-16)
  - **Phiên đăng nhập / đăng xuất**: `AuthGateViewModelTest` (chưa có phiên → `SignedOut`, có uid → `SignedIn`, "Bỏ qua" → `Guest`, **đăng xuất ở chế độ khách và khi đã đăng nhập** → `SignedOut`, đăng nhập lại được — mục 3 #19, TC-AUTH-16), `AuthRepositoryImplTest` (`signOut()` uỷ quyền `FirebaseAuth.signOut()`)
- **Báo cáo:** `app\build\reports\tests\testDebugUnitTest\index.html`
- **Ghi chú:** File `gradlew`/`gradlew.bat` **không có** trong repo hiện tại → nếu chưa sinh Gradle wrapper thì chạy test bằng Gradle đã cài (hoặc qua Android Studio); xem mục 2.2.

### TC-ENV-04 — Instrumented test (DAO)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Emulator/thiết bị đang chạy.
- **Bước:** 1. `.\gradlew connectedAndroidTest`.
- **Kết quả mong đợi:** `HabitDaoTest`, `HabitLogDaoTest` pass.

### TC-ENV-05 — Smoke test 4 màn hình
- **Ưu tiên:** P0
- **Bước:** 1. Mở app → (nếu là lần mở đầu tiên: bấm **"Bỏ qua, dùng ở chế độ khách"** ở màn đăng nhập) → 2. Lần lượt bấm 4 tab Trang chủ / Thói quen / Ghi chép / Phân tích.
- **Kết quả mong đợi:** Cả 4 màn hình render đủ nội dung, không crash, không màn hình trắng.
- **Ghi chú:** Chạy đồng thời `adb logcat -s AndroidRuntime:E` để bắt crash.

### TC-ENV-06 — Không có ANR/rò rỉ khi điều hướng nhanh
- **Ưu tiên:** P2
- **Bước:** 1. Bấm luân phiên 4 tab liên tục 20 lần.
- **Kết quả mong đợi:** Không ANR, không crash, không giật lag kéo dài.

---

## 5. TC-INIT — Khởi động app & đăng ký job nền

### TC-INIT-01 — Cold start lần đầu
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã `pm clear com.healthyai.app`.
- **Bước:** 1. Mở app.
- **Kết quả mong đợi:**
  - `HiltApplication` khởi tạo, DB Room + SQLCipher tạo mới thành công (không lỗi `System.loadLibrary("sqlcipher")`).
  - `MainActivity.onCreate` gọi `HealthSyncWorker.schedule()`, `SyncWorker.schedule()`, `ReminderWorker.schedule()` **không ném ngoại lệ** (đặc biệt không có `IllegalArgumentException: Cannot set backoff criteria on an idle mode job`).
  - Hiện **màn hình đăng nhập** (sau một nhịp splash rất ngắn khi `FirebaseAuth` đọc lại phiên đã lưu) — xem **TC-AUTH-01**.
  - Bấm **"Bỏ qua, dùng ở chế độ khách"** → vào thẳng màn Home, không có splash/login nữa.

### TC-INIT-02 — Job nền được đăng ký sau lần mở đầu tiên
- **Ưu tiên:** P1
- **Bước:** 1. Mở app → 2. `adb shell dumpsys jobscheduler | Select-String "healthyai"`.
- **Kết quả mong đợi:** Thấy 3 job periodic gắn với `com.healthyai.app/androidx.work.impl.background.systemjob.SystemJobService`.
- **Ghi chú:** Kiểm tra rõ hơn bằng **App Inspection → Background Task Inspector**: thấy `periodic_health_sync_worker`, `periodic_sync_worker`, `daily_reminder_worker`.

### TC-INIT-03 — Mở lại app không tạo job trùng
- **Ưu tiên:** P1
- **Bước:** 1. Mở app → 2. Thoát hẳn (swipe khỏi recents) → 3. Mở lại app → 4. Kiểm tra Background Task Inspector.
- **Kết quả mong đợi:** Mỗi worker vẫn chỉ có **1 bản ghi** (`ExistingPeriodicWorkPolicy.KEEP`), không nhân bản.

### TC-INIT-04 — Khởi động khi không có mạng
- **Ưu tiên:** P1
- **Bước:** 1. Bật Airplane mode → 2. `pm clear` → 3. Mở app.
- **Kết quả mong đợi:** App mở bình thường, không crash; Health Connect vẫn đọc được (dữ liệu cục bộ); không có dialog lỗi mạng.
- **Ghi chú:** `SyncWorker` có `NetworkType.CONNECTED` nên sẽ không chạy cho tới khi có mạng.

### TC-INIT-05 — Tắt/mở lại thiết bị (reboot)
- **Ưu tiên:** P2
- **Bước:** 1. Mở app ít nhất 1 lần → 2. `adb reboot` → 3. Mở app lại.
- **Kết quả mong đợi:** Dữ liệu thói quen/log được giữ nguyên; job nền vẫn tồn tại (`RECEIVE_BOOT_COMPLETED` + WorkManager tự phục hồi).

---

## 6. TC-NAV — Điều hướng

Cấu hình: `startDestination = "home"`; bottom bar **chỉ hiển thị** khi route hiện tại thuộc `bottomNavItems` (`home`, `habits`, `camera`, `insights`) — màn `add_edit_habit?habitId={habitId}` **ẩn** bottom bar.

### TC-NAV-01 — Bottom bar hiển thị đúng trên 4 màn chính
- **Ưu tiên:** P0
- **Bước:** 1. Lần lượt mở 4 tab.
- **Kết quả mong đợi:** Bottom bar hiển thị trên cả 4; nhãn lần lượt: **Trang chủ**, **Thói quen**, **Ghi chép**, **Phân tích**; icon: Home / CheckCircle / CameraAlt / AutoAwesome.

### TC-NAV-02 — Tab đang chọn được đánh dấu
- **Ưu tiên:** P2
- **Bước:** 1. Bấm lần lượt từng tab.
- **Kết quả mong đợi:** Tab hiện tại được highlight (selected), 3 tab còn lại ở trạng thái thường.

### TC-NAV-03 — Màn Thêm/Sửa thói quen ẩn bottom bar
- **Ưu tiên:** P1
- **Bước:** 1. Vào tab Thói quen → 2. Bấm FAB `+`.
- **Kết quả mong đợi:** Mở màn "Thêm thói quen mới"; **không** có bottom bar; có nút back (mũi tên trái) ở top bar.

### TC-NAV-04 — Back từ Thêm/Sửa về danh sách
- **Ưu tiên:** P0
- **Bước:** 1. Vào Thêm thói quen → 2. Bấm nút back trên top bar → 3. Lặp lại nhưng dùng phím back hệ thống.
- **Kết quả mong đợi:** Cả 2 cách đều quay về tab Thói quen, bottom bar hiện lại, danh sách giữ nguyên trạng thái filter đã chọn.

### TC-NAV-05 — Back ở tab gốc thoát app
- **Ưu tiên:** P1
- **Bước:** 1. Ở tab Trang chủ → 2. Bấm back hệ thống.
- **Kết quả mong đợi:** Thoát app (không quay về màn khác).

### TC-NAV-06 — Bảo toàn trạng thái khi đổi tab
- **Ưu tiên:** P1
- **Bước:** 1. Vào tab Thói quen → chọn filter **Sáng** → 2. Sang tab Phân tích → 3. Quay lại tab Thói quen.
- **Kết quả mong đợi:** Filter vẫn là **Sáng** (nhờ `saveState`/`restoreState`); danh sách không reset về "Tất cả".

### TC-NAV-07 — Hiệu ứng chuyển màn hình
- **Ưu tiên:** P2
- **Bước:** 1. Quan sát khi chuyển tab và khi vào/ra màn Thêm thói quen.
- **Kết quả mong đợi:** Có hiệu ứng fade + slide ngang; không nhấp nháy, không chồng màn hình.

---

## 7. TC-HOME — Màn hình Trang chủ

Thành phần: `GreetingHeader` (ngày + lời chào + tên **Alex** + avatar xám), `HealthSummaryCard` ("Tóm tắt sức khỏe" + 3 vòng tròn: **Bước chân**, **Ngủ**, **Tim**), tiêu đề **"Thói quen hôm nay"**, danh sách thẻ thói quen, khối lỗi đỏ (nếu có).

### TC-HOME-01 — Lời chào theo thời điểm
- **Ưu tiên:** P2
- **Bước:** 1. Đổi giờ hệ thống emulator vào các khoảng: 05:00–11:59, 12:00–17:59, 18:00–21:59, 22:00–04:59 (`adb shell date`) → 2. Mở tab Trang chủ.
- **Kết quả mong đợi:** Lần lượt: **"Chào buổi sáng,"**, **"Chào buổi chiều,"**, **"Chào buổi tối,"**, **"Chào buổi đêm,"**; dòng dưới luôn là **Alex**.

### TC-HOME-02 — Hiển thị ngày tháng tiếng Việt
- **Ưu tiên:** P2
- **Bước:** 1. Đặt ngôn ngữ máy = Tiếng Việt → 2. Xem dòng ngày ở đầu màn Home.
- **Kết quả mong đợi:** Định dạng `EEEE, dd 'tháng' MM`, ví dụ **"Thứ Năm, 17 tháng 09"**.

### TC-HOME-03 — Danh sách thói quen hiển thị đúng
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã tạo Data set A (5 thói quen).
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Tiêu đề **"Thói quen hôm nay"**; 5 thẻ thói quen, sắp xếp **mới tạo trước** (`ORDER BY createdAt DESC`); mỗi thẻ có icon tròn theo danh mục, tên thói quen, dòng **"Mục tiêu: 2000 ml"** (giá trị `targetValue.toInt()` + đơn vị), bên phải là vòng tròn viền xám (chưa tick).

### TC-HOME-04 — Trạng thái hoàn thành trên Home
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có thói quen "Uống nước" mục tiêu 2000 ml, chưa ghi log nào hôm nay.
- **Bước:**
  1. Mở tab Trang chủ → ghi nhận thẻ "Uống nước" đang ở trạng thái **vòng tròn viền xám** + dòng **"Mục tiêu: 2000 ml"**.
  2. Sang tab Thói quen ghi `2000` ml → 3. Quay lại tab Trang chủ.
- **Kết quả mong đợi:**
  - Trước khi đạt: vòng tròn viền xám, dòng phụ **"Mục tiêu: 2000 ml"**.
  - Sau khi đạt: icon tròn đổi nền xanh nhạt, dòng phụ đổi thành **"Hoàn thành"** (màu xanh `#4CAF50`), ô bên phải là **vòng tròn xanh đặc có dấu ✓**.
  - Ghi chưa đủ (ví dụ 1500 ml) → **không** tick, vẫn hiển thị mục tiêu.
- **Ghi chú:** Trạng thái được tính bằng tổng giá trị log **trong ngày** so với `targetValue` (`HomeViewModel.observeHabits()`), cập nhật reactive qua Flow của Room nên không cần thao tác refresh.

### TC-HOME-05 — Thẻ Tóm tắt sức khỏe: có Health Connect & có dữ liệu
- **Ưu tiên:** P1
- **Tiền điều kiện:** Emulator có Health Connect; đã bơm dữ liệu giả (2.5.3); app đã được cấp 4 quyền đọc.
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:**
  - **Bước chân:** giá trị = `steps / 1000` với 1 chữ số thập phân + `k` (ví dụ 8000 bước → **"8.0k"**).
  - **Ngủ:** định dạng `Xh Ym` (ví dụ 7.5 h → **"7h 30m"**).
  - **Tim:** `<bpm> bpm` (ví dụ 90.0 → **"90 bpm"**).
  - Không có khối lỗi đỏ nào.

### TC-HOME-06 — Thẻ Tóm tắt sức khỏe: chưa cấp quyền Health Connect
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thu hồi hết quyền Health Connect của HealthyAI (hoặc cài trên thiết bị không có Health Connect).
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Nếu Health Connect khả dụng nhưng thiếu quyền: các chỉ số hiển thị **0** (`0.0k`, `0h 0m`, `0 bpm`), app **không crash** (mọi lời gọi đều `try/catch → 0`), **và** màn Home hiện thêm thẻ **"Cấp quyền Health Connect"** ngay dưới thẻ Tóm tắt sức khỏe để người dùng cấp quyền (xem TC-HC-13).

### TC-HOME-07 — Health Connect không khả dụng → thẻ mời cài đặt
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thiết bị/emulator **không** có Health Connect (ví dụ emulator API 26–33 không Google Play) → `getSdkStatus = SDK_UNAVAILABLE (1)`.
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Thẻ Tóm tắt sức khỏe hiển thị `0` và **bên dưới là thẻ trắng** tiêu đề **"Chưa cài Health Connect"**, mô tả có ý **"Hiện tại các chỉ số đang hiển thị 0"** và nút xanh **"Cài Health Connect"**. **Không** có banner lỗi đỏ "Không thể đọc dữ liệu sức khỏe" (thẻ này thay cho banner đó).
- **Ghi chú:** Bấm nút → mở Play Store ở trang ứng dụng Health Connect; nếu máy không có Play Store → mở link web `play.google.com/store/apps/details?id=com.google.android.apps.healthdata`.

### TC-HOME-07b — Health Connect cần cập nhật
- **Ưu tiên:** P1
- **Tiền điều kiện:** `getSdkStatus = SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED (2)`.
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Thẻ tiêu đề **"Health Connect cần được cập nhật"** + nút **"Cập nhật Health Connect"** mở Play Store.

### TC-HOME-08 — Đóng banner lỗi
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đang có **banner lỗi đỏ** (chỉ còn xảy ra khi Health Connect sẵn sàng nhưng lời gọi đọc dữ liệu thất bại — trường hợp chưa cài/cần cập nhật đã chuyển sang thẻ TC-HOME-07).
- **Bước:** 1. Bấm nút **X**.
- **Kết quả mong đợi:** Banner biến mất (`dismissError()`), không hiện lại cho tới khi mở lại màn hình.

### TC-HOME-09 — Trạng thái rỗng (chưa có thói quen)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `pm clear com.healthyai.app` (DB rỗng).
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Thay vì danh sách, hiển thị thẻ trắng căn giữa: icon `AddCircle` xanh + dòng **"Thêm thói quen đầu tiên"**.
- **Ghi chú:** Thẻ này **không bấm được** (không có `clickable`) — nếu mong muốn điều hướng sang màn thêm thói quen, ghi nhận là thiếu sót.

### TC-HOME-10 — Trạng thái loading
- **Ưu tiên:** P2
- **Bước:** 1. `pm clear` → 2. Mở app và quan sát ngay tab Trang chủ.
- **Kết quả mong đợi:** Trong lúc truy vấn, tiêu đề "Thói quen hôm nay" hiển thị cùng `CircularProgressIndicator` căn giữa; sau đó chuyển sang danh sách/rỗng.

### TC-HOME-11 — Cuộn danh sách dài
- **Ưu tiên:** P2
- **Tiền điều kiện:** Tạo 15 thói quen.
- **Bước:** 1. Mở Home → 2. Cuộn xuống cuối.
- **Kết quả mong đợi:** Cuộn mượt, không cắt nội dung, không trùng thẻ.

---

## 8. TC-HAB — Màn hình Thói quen

Thành phần: header **"Theo dõi thói quen"** + dòng ngày `'Hôm nay,' EEEE, dd 'tháng' MM`; **thẻ lỗi đỏ** khi ghi nhận/đọc dữ liệu thất bại (đóng được — TC-EDGE-07); 4 chip lọc **Tất cả / Sáng / Chiều / Tối**; danh sách thẻ thói quen; FAB tròn xanh 64dp có icon `+`; hộp thoại ghi nhận (ô nhập điền sẵn mục tiêu dạng **số sạch** — TC-HAB-26; nhập rỗng/không phải số/**≤ 0** → **giữ hộp thoại mở + báo lỗi** — TC-HAB-28, TC-HAB-12); **Toast "Đã ghi nhận: tên thói quen"** sau khi lưu thành công (xem **TC-HAB-24**).

### TC-HAB-01 — Header và ngày
- **Ưu tiên:** P2
- **Bước:** 1. Mở tab Thói quen (ngôn ngữ máy = Tiếng Việt).
- **Kết quả mong đợi:** Tiêu đề **"Theo dõi thói quen"**; dòng dưới dạng **"Hôm nay, Thứ Năm, 17 tháng 09"**.

### TC-HAB-02 — Hiển thị danh sách theo danh mục
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A.
- **Bước:** 1. Mở tab Thói quen.
- **Kết quả mong đợi:** 5 thẻ, thứ tự mới nhất trước; icon tròn nền xám `#EDF2F7` chứa **emoji** của danh mục (💧 😴 🏃 🥗 🧘); tên thói quen; dòng tiến độ màu xanh đậm; nút xanh **"Ghi"** kèm dấu `+` (100×48dp).

### TC-HAB-03 — Định dạng tiến độ theo danh mục
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A.
- **Bước:** 1. Quan sát dòng tiến độ khi chưa ghi gì, sau đó ghi lần lượt và quan sát.
- **Kết quả mong đợi:**
  - **Uống nước** (WATER): `%.1fL / %.1fL` — ví dụ ghi 500 → **"0.5L / 2.0L"**.
  - **Vận động** (EXERCISE): `int / int phút` — ví dụ **"10 / 30 phút"**.
  - **Giấc ngủ** (SLEEP): `%.1f / %.1f giờ` — ví dụ **"7.0 / 8.0 giờ"**.
  - **Dinh dưỡng / Thiền định** (khác): `int / int <đơn vị>` — ví dụ **"300 / 600 calo"**.

### TC-HAB-04 — Ghi nhận thói quen: mở hộp thoại
- **Ưu tiên:** P0
- **Bước:** 1. Bấm nút **"Ghi"** trên thẻ "Uống nước".
- **Kết quả mong đợi:** Hộp thoại `AlertDialog` tiêu đề **"Ghi nhận Uống nước"**; ô nhập nhãn **"Số lượng (ml)"**; giá trị mặc định đã điền sẵn **`2000`** (số sạch, không có đuôi `.0` — xem TC-HAB-26); 2 nút **Lưu** / **Hủy**.

### TC-HAB-05 — Ghi nhận hợp lệ
- **Ưu tiên:** P0
- **Bước:** 1. Mở hộp thoại → 2. Xóa giá trị, nhập `500` → 3. Bấm **Lưu**.
- **Kết quả mong đợi:** Hộp thoại đóng; **Toast "Đã ghi nhận: Uống nước"** hiện ở đáy màn hình (xem TC-HAB-24); dòng tiến độ đổi thành **"0.5L / 2.0L"** (500 ml); không crash; bản ghi được lưu vào bảng `habit_logs`.

### TC-HAB-06 — Ghi nhiều lần cộng dồn
- **Ưu tiên:** P0
- **Bước:** 1. Ghi `500` → 2. Ghi tiếp `700` → 3. Ghi tiếp `800`.
- **Kết quả mong đợi:** Tiến độ lần lượt **0.5L → 1.2L → 2.0L** (tổng = 500+700+800); độ chính xác 1 chữ số thập phân.

### TC-HAB-07 — Đạt mục tiêu → trạng thái hoàn thành
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thói quen mục tiêu 2000 ml.
- **Bước:** 1. Ghi `2000`.
- **Kết quả mong đợi:**
  - Icon tròn đổi sang nền xanh nhạt `#C8E6C9` + dấu **✓** xanh đậm (emoji biến mất).
  - Dòng phụ đổi thành **"Hoàn thành"** (màu xanh `#4CAF50`).
  - Nút **"Ghi"** được thay bằng ô xám bo tròn chữ **"Xong"** (không bấm được).

### TC-HAB-08 — Vượt mục tiêu
- **Ưu tiên:** P1
- **Bước:** 1. Sau khi đã hoàn thành, tiếp tục ghi thêm 500 ml.
- **Kết quả mong đợi:** Vẫn ở trạng thái "Hoàn thành" (điều kiện `totalLogged >= targetValue`), không lỗi.

### TC-HAB-09 — Hủy hộp thoại không lưu
- **Ưu tiên:** P1
- **Bước:** 1. Mở hộp thoại → 2. Đổi số thành `9999` → 3. Bấm **Hủy**.
- **Kết quả mong đợi:** Hộp thoại đóng, tiến độ **không** thay đổi.
- **Bổ sung:** Bấm ra ngoài hộp thoại (hoặc phím back) cũng phải đóng và không lưu.

### TC-HAB-10 — Nhập rỗng khi ghi nhận
- **Ưu tiên:** P1
- **Bước:** 1. Mở hộp thoại → 2. Xóa hết ký tự → 3. Bấm **Lưu**.
- **Kết quả mong đợi:**
  - Hộp thoại **vẫn mở** (không đóng); ô nhập chuyển trạng thái lỗi (viền đỏ) kèm dòng chữ đỏ **"Vui lòng nhập số lượng"** ngay dưới ô.
  - **Không** có bản ghi nào được thêm, tiến độ giữ nguyên, **không** có Toast "Đã ghi nhận".
  - Nhập lại `500` → dòng lỗi biến mất ngay khi gõ → bấm **Lưu** → lưu bình thường (như TC-HAB-05).
  - Bấm **Hủy** ở trạng thái lỗi → đóng hộp thoại, không lưu (như TC-HAB-09).
- **Ghi chú:** Trước đây trường hợp này hộp thoại **đóng im lặng** và không lưu gì (mục 3 #12 — đã sửa). Nhập chữ (ví dụ `abc`) cho thông báo **"Số lượng phải là số"** (xem TC-HAB-28).

### TC-HAB-11 — Số thập phân
- **Ưu tiên:** P2
- **Tiền điều kiện:** Thói quen "Ngủ đủ giấc" (đơn vị giờ).
- **Bước:** 1. Ghi `7.5`.
- **Kết quả mong đợi:** Lưu thành công, tiến độ **"7.5 / 8.0 giờ"**.
- **Ghi chú:** Ô nhập dùng `KeyboardType.Number` (không có dấu `.`) — nhập dấu chấm có thể phải dùng bàn phím phần cứng/paste. Đây là hạn chế UX cần ghi nhận.

### TC-HAB-12 — Nhập giá trị âm
- **Ưu tiên:** P2
- **Bước:** 1. Mở hộp thoại → 2. Ghi `-100` (dùng bàn phím phần cứng nếu cần) → 3. Bấm **Lưu**. 4. Lặp lại với `0`.
- **Kết quả mong đợi:**
  - Hộp thoại **không đóng**; ô nhập viền đỏ + dòng lỗi **"Số lượng phải lớn hơn 0"**.
  - **Không** có bản ghi nào được thêm; tiến độ của ngày **không** bị giảm; không có Toast "Đã ghi nhận".
  - Nhập lại `500` → lỗi biến mất → **Lưu** → ghi nhận bình thường.
- **Ghi chú:** Đây là bản sửa lỗi **phụ lục C #11** (trước đây `-100` được lưu và làm tổng tiến độ bị giảm). Ngoài lớp kiểm tra ở hộp thoại (`parseLogValue`), `HabitsViewModel.logHabit` cũng từ chối `value <= 0` / `NaN` / `Infinity` để mọi đường vào khác đều an toàn — xem TC-HAB-30.

### TC-HAB-13 — Chip lọc "Sáng"
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A (Chạy bộ + Thiền = **Sáng**; Ăn salad = **Chiều**; Ngủ = **Tối**; Uống nước = **Cả ngày**).
- **Bước:** 1. Bấm chip **Sáng**.
- **Kết quả mong đợi:** Chip Sáng đổi màu nền xanh đậm `#1B5E20`, chữ trắng. Danh sách chỉ còn: Chạy bộ, Thiền 15 phút **và Uống nước** (vì thói quen `ALL_DAY` luôn được giữ lại).

### TC-HAB-14 — Chip lọc "Chiều" / "Tối"
- **Ưu tiên:** P1
- **Bước:** 1. Bấm **Chiều** → 2. Bấm **Tối**.
- **Kết quả mong đợi:** Chiều: Ăn salad + Uống nước. Tối: Ngủ đủ giấc + Uống nước.

### TC-HAB-15 — Chip "Tất cả"
- **Ưu tiên:** P1
- **Bước:** 1. Sau khi đang lọc **Sáng**, bấm **Tất cả**.
- **Kết quả mong đợi:** Hiển thị toàn bộ 5 thói quen.

### TC-HAB-16 — Lọc ra danh sách rỗng
- **Ưu tiên:** P2
- **Tiền điều kiện:** Chỉ có 1 thói quen thuộc **Sáng** (không có thói quen `Cả ngày`).
- **Bước:** 1. Bấm chip **Tối**.
- **Kết quả mong đợi:** Hiển thị dòng chữ xám căn giữa **"Chưa có thói quen nào"**.

### TC-HAB-17 — Chỉ hiển thị thói quen đang hoạt động
- **Ưu tiên:** P1
- **Bước:** 1. Xóa 1 thói quen (TC-ADD-09) → 2. Quan sát danh sách.
- **Kết quả mong đợi:** Thói quen đã xóa **không** còn xuất hiện (DAO lọc `isActive = 1`).

### TC-HAB-18 — Tiến độ được tính lại sau khi mở lại app
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã ghi 500 ml cho "Uống nước" ở phiên hiện tại (tiến độ hiển thị 0.5L).
- **Bước:** 1. Thoát hẳn app (swipe khỏi recents) → 2. Mở lại app → 3. Vào tab Thói quen.
- **Kết quả mong đợi:** Tiến độ vẫn là **"0.5L / 2.0L"** vì log nằm trong ngày hiện tại.
- **Ghi chú:** Mốc thời gian được căn theo **đầu ngày (00:00)** trong `HabitRepositoryImpl.startOfDayEpochMillis()`, nên log ghi trong ngày luôn được tính bất kể thời điểm mở màn hình.
- **Kiểm tra bổ sung:** Ghi log rồi ghi tiếp sau vài giờ → tiến độ phải cộng dồn đúng theo ngày, không bị reset giữa các phiên.

### TC-HAB-19 — Tiến độ cập nhật tức thời nhiều thiết bị/thao tác
- **Ưu tiên:** P2
- **Bước:** 1. Mở tab Thói quen → 2. Ghi 1 log → 3. Quan sát không cần thao tác gì thêm.
- **Kết quả mong đợi:** UI tự cập nhật ngay (Flow reactive từ Room) — không cần pull-to-refresh.

### TC-HAB-20 — Nút FAB thêm thói quen
- **Ưu tiên:** P1
- **Bước:** 1. Bấm FAB `+`.
- **Kết quả mong đợi:** Mở màn "Thêm thói quen mới" (nội dung test ở module TC-ADD).

### TC-HAB-21 — Chạm vào thẻ thói quen để sửa
- **Ưu tiên:** P1
- **Bước:** 1. Bấm vào vùng thân thẻ "Chạy bộ" (không bấm nút "Ghi").
- **Kết quả mong đợi:** Mở màn "Sửa thói quen" với dữ liệu đã điền sẵn.

### TC-HAB-22 — Bấm "Ghi" không mở màn sửa
- **Ưu tiên:** P2
- **Bước:** 1. Bấm đúng nút **"Ghi"**.
- **Kết quả mong đợi:** Chỉ mở hộp thoại ghi nhận, **không** điều hướng sang màn sửa (nút nằm trên thẻ có `clickable`).

### TC-HAB-23 — Nhiều thói quen cùng danh mục
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có 2 thói quen cùng danh mục Uống nước.
- **Bước:** 1. Ghi log cho từng thói quen.
- **Kết quả mong đợi:** Tiến độ tính riêng theo `habitId`, không lẫn nhau.

### TC-HAB-24 — Phản hồi "Đã ghi nhận" sau khi lưu (mục 3 #10)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A; tab Thói quen đang mở.
- **Bước:**
  1. Bấm **"Ghi"** trên thẻ "Uống nước" → nhập `500` → **Lưu**.
  2. Quan sát đáy màn hình trong ~2 giây.
  3. Ghi tiếp `700` cho **cùng** thói quen đó → quan sát lại.
  4. Ghi `30` cho thói quen khác (ví dụ "Chạy bộ") → quan sát lại.
- **Kết quả mong đợi:**
  - Mỗi lần lưu thành công đều hiện **Toast "Đã ghi nhận: <tên thói quen>"** (ví dụ **"Đã ghi nhận: Uống nước"**), `Toast.LENGTH_SHORT`, không chặn thao tác (không phải dialog, không che nút **Ghi**/FAB).
  - Bước 3 (ghi lại **cùng** thói quen) **vẫn** hiện Toast — `showLogSuccessFor` là sự kiện một lần, UI xoá cờ bằng `HabitsViewModel.onLogSuccessShown()` sau khi hiện, nên không cần chờ hết 2 giây.
  - Không hiện Toast khi lưu **thất bại** (trường hợp đó hiện `error` = `"Lỗi ghi nhận: …"`).
  - Không crash, tiến độ vẫn cập nhật realtime (TC-HAB-19).

### TC-HAB-25 — Unit test JVM cho phản hồi ghi nhận
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitsViewModelTest"`.
- **Kết quả mong đợi:** `HabitsViewModelTest` (4 test) pass:
  - Ghi thành công → `showLogSuccessFor` = id thói quen vừa ghi, không có `error`.
  - Gọi `onLogSuccessShown()` → cờ trở về `null`.
  - Ghi lại **cùng** thói quen sau khi đã xoá cờ → cờ được đặt lại (phản hồi hiện lại), `logHabitUseCase` được gọi 2 lần.
  - Ghi thất bại (`logHabitUseCase` ném lỗi) → **không** phát sự kiện và `error` = `"Lỗi ghi nhận: …"`.

### TC-HAB-26 — Số mặc định trong hộp thoại ghi nhận là số sạch (mục 3 #12)
- **Ưu tiên:** P2
- **Tiền điều kiện:** Data set A (mục tiêu Uống nước = 2000 ml; nếu có thói quen mục tiêu thập phân thì dùng bàn phím phần cứng/paste để nhập `7.5`).
- **Bước:**
  1. Bấm **"Ghi"** trên thẻ "Uống nước" → quan sát ô **"Số lượng (ml)"**.
  2. Đóng hộp thoại → mở lại hộp thoại ghi nhận của thói quen có mục tiêu `7.5` → quan sát lại.
- **Kết quả mong đợi:**
  - Ô nhập điền sẵn **`2000`** — **không** còn `2000.0`; với mục tiêu thập phân điền sẵn **`7.5`**.
  - Bấm **Lưu** ngay (không sửa gì) → ghi log 2000 ml bình thường, tiến độ thành "Hoàn thành" (TC-HAB-07) + Toast "Đã ghi nhận: Uống nước" (TC-HAB-24).
- **Ghi chú:** Chỉ đổi **giá trị hiển thị mặc định**. Trường hợp nhập rỗng/không phải số nay **giữ hộp thoại mở + báo lỗi** (mục 3 #12 đã sửa — xem TC-HAB-10 và TC-HAB-28).

### TC-HAB-27 — Unit test JVM cho định dạng số hiển thị
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*NumberDisplayTest" --tests "*AddEditHabitViewModelTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm:
  - `NumberDisplayTest` (`presentation/ui/format/NumberDisplay.kt`): `2000f/8f/30f/1f` → `"2000"/"8"/"30"/"1"`; `7.5f/0.5f/2.25f` → `"7.5"/"0.5"/"2.25"`; chuỗi trả về luôn parse lại được bằng `toFloatOrNull()` (dấu thập phân luôn là `.`, không phụ thuộc locale).
  - `AddEditHabitViewModelTest`: mở màn Sửa với mục tiêu `2000f` → `targetValue = "2000"`; với `7.5f` → `"7.5"`; bấm Cập nhật ngay → lưu đúng `2000f` (xem TC-ADD-24).

### TC-HAB-28 — Nhập sai trong hộp thoại ghi nhận → giữ hộp thoại + báo lỗi (mục 3 #12)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A; tab Thói quen đang mở.
- **Bước:**
  1. Bấm **"Ghi"** trên thẻ "Uống nước" → xóa hết ô "Số lượng (ml)" → bấm **Lưu**.
  2. Quan sát hộp thoại, tiến độ trên thẻ và danh sách log (Database Inspector / `habit_logs`).
  3. Nhập `abc` → bấm **Lưu**.
  4. Nhập `500` → bấm **Lưu**.
- **Kết quả mong đợi:**
  - Bước 1: hộp thoại **không** đóng; ô nhập viền đỏ + dòng chữ đỏ **"Vui lòng nhập số lượng"**; **không** có log nào được thêm; tiến độ không đổi; **không** có Toast "Đã ghi nhận".
  - Bước 3: đổi thông báo thành **"Số lượng phải là số"**; vẫn không lưu gì.
  - Mỗi lần gõ lại, thông báo lỗi **biến mất** ngay (không cần bấm gì thêm).
  - Bước 4: lưu thành công → hộp thoại đóng, tiến độ tăng như TC-HAB-05 kèm Toast "Đã ghi nhận: Uống nước".
  - App không crash. Ô nhập giữ nguyên giá trị người dùng vừa gõ khi có lỗi (không tự reset).
- **Ghi chú:** Chưa validate số **âm/0** ở bước này — nhập `-50` vẫn được lưu (phụ lục C #11, TC-HAB-12).

### TC-HAB-29 — Unit test JVM cho đọc giá trị ô "Số lượng"
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitLogInputTest"`.
- **Kết quả mong đợi:** `HabitLogInputTest` (4 test) pass — `parseLogValue()` trong `presentation/screen/habits/HabitLogInput.kt`:
  - `"500"`, `" 7.5 "`, `"2000"` → `Valid(500f / 7.5f / 2000f)` (tự cắt khoảng trắng).
  - `""` và `"   "` → `Invalid("Vui lòng nhập số lượng")`.
  - `"abc"`, `"5 lần"` → `Invalid("Số lượng phải là số")`.
  - `"NaN"`, `"Infinity"` → `Invalid("Số lượng phải là số")` (không ghi log rác).

---

## 9. TC-ADD — Thêm / Sửa / Xóa thói quen

Màn hình: top bar **"Thêm thói quen mới"** (hoặc **"Sửa thói quen"**), nút back, nút xóa (chỉ ở chế độ sửa), ô tên, 5 ô danh mục, ô mục tiêu + đơn vị, 4 chip thời gian, 3 radio tần suất, nút đáy **"Tạo thói quen"** / **"Cập nhật"** (khi đang lưu: nút mờ đi + hiện vòng xoay — chống double-submit, xem **TC-ADD-14**). Ở chế độ Sửa, ô mục tiêu điền sẵn **số sạch** — `2000` thay vì `2000.0` (xem **TC-ADD-23**). Lỗi validate (tên trống, mục tiêu ≤ 0, **tên trùng**) hiện trong hộp thoại **"Lỗi"** ngay trên màn này.

### TC-ADD-01 — Mở màn ở chế độ thêm
- **Ưu tiên:** P1
- **Bước:** 1. Tab Thói quen → FAB `+`.
- **Kết quả mong đợi:** Tiêu đề **"Thêm thói quen mới"**; **không** có nút xóa; nút đáy ghi **"Tạo thói quen"**; giá trị mặc định: tên rỗng, danh mục = **Uống nước 💧**, đơn vị = **ml**, mục tiêu rỗng, thời gian = **Cả ngày**, tần suất = **Hàng ngày**.

### TC-ADD-02 — Danh mục mặc định & đổi đơn vị tự động
- **Ưu tiên:** P0
- **Bước:** 1. Lần lượt bấm từng ô danh mục: Giấc ngủ 😴 → Uống nước 💧 → Vận động 🏃 → Dinh dưỡng 🥗 → Thiền định 🧘, mỗi lần quan sát chữ cạnh ô mục tiêu.
- **Kết quả mong đợi:** Đơn vị đổi lần lượt: **giờ → ml → phút → calo → phút**. Ô danh mục được chọn có nền xanh nhạt `#C8E6C9`, chữ xanh đậm, phóng to nhẹ (scale 1.05) và có animation.

### TC-ADD-03 — Tạo thói quen hợp lệ (đủ trường)
- **Ưu tiên:** P0
- **Bước:**
  1. Nhập tên `Uống nước buổi sáng`.
  2. Chọn danh mục **Uống nước**.
  3. Nhập mục tiêu `1500`.
  4. Chọn thời gian **Buổi sáng**.
  5. Chọn tần suất **Hàng ngày**.
  6. Bấm **Tạo thói quen**.
- **Kết quả mong đợi:** Tự động quay về tab Thói quen; thói quen mới xuất hiện **đầu danh sách**; dòng tiến độ **"0.0L / 1.5L"**; có thể lọc bằng chip **Sáng**.

### TC-ADD-04 — Validate tên rỗng
- **Ưu tiên:** P0
- **Bước:** 1. Để trống ô tên → 2. Nhập mục tiêu `1000` → 3. Bấm **Tạo thói quen**.
- **Kết quả mong đợi:** Hiện `AlertDialog` tiêu đề **"Lỗi"**, nội dung **"Tên thói quen không được để trống"**, nút **"Đóng"**. Bấm Đóng → dialog ẩn, vẫn ở màn thêm, dữ liệu đã nhập giữ nguyên.

### TC-ADD-05 — Validate tên chỉ có khoảng trắng
- **Ưu tiên:** P1
- **Bước:** 1. Nhập tên = `   ` (3 dấu cách) → 2. Mục tiêu `1000` → 3. Bấm **Tạo thói quen**.
- **Kết quả mong đợi:** Báo lỗi **"Tên thói quen không được để trống"** (`isBlank()`).

### TC-ADD-06 — Validate mục tiêu rỗng / = 0
- **Ưu tiên:** P0
- **Bước:** 1. Nhập tên `Test` → 2. Để trống mục tiêu → 3. Bấm **Tạo thói quen**. Lặp lại với mục tiêu `0`.
- **Kết quả mong đợi:** Cả 2 trường hợp hiện lỗi **"Mục tiêu phải là số dương"**; thói quen không được tạo.

### TC-ADD-07 — Validate mục tiêu không phải số
- **Ưu tiên:** P1
- **Bước:** 1. Nhập tên `Test` → 2. Nhập mục tiêu `abc` (bàn phím phần cứng/paste) → 3. Bấm **Tạo thói quen**.
- **Kết quả mong đợi:** Hiện lỗi **"Mục tiêu phải là số dương"** (parse thất bại → 0f); không tạo thói quen.

### TC-ADD-08 — Thứ tự kiểm tra lỗi
- **Ưu tiên:** P2
- **Bước:** 1. Để trống cả tên và mục tiêu → 2. Bấm **Tạo thói quen**.
- **Kết quả mong đợi:** Chỉ báo lỗi **tên** trước (**"Tên thói quen không được để trống"**).

### TC-ADD-09 — Sửa thói quen
- **Ưu tiên:** P0
- **Tiền điều kiện:** Có thói quen "Uống nước" mục tiêu 2000 ml.
- **Bước:** 1. Bấm vào thẻ "Uống nước" → 2. Quan sát dữ liệu điền sẵn → 3. Đổi tên thành `Uống đủ nước` → 4. Đổi mục tiêu thành `2500` → 5. Đổi thời gian thành **Cả ngày**, tần suất **Hàng tuần** → 6. Bấm **Cập nhật**.
- **Kết quả mong đợi:**
  - Màn mở với tiêu đề **"Sửa thói quen"**, có nút xóa đỏ ở góc phải, nút đáy **"Cập nhật"**.
  - Ô mục tiêu hiển thị **`2000`** — số nguyên không còn đuôi `.0` (mục 3 #11 đã sửa, xem **TC-ADD-23**).
  - Sau khi lưu: quay về danh sách, tên mới + đơn vị mới được áp dụng; **không** tạo bản ghi trùng (cùng `id`).

### TC-ADD-10 — Sửa danh mục đổi đơn vị
- **Ưu tiên:** P1
- **Bước:** 1. Mở sửa thói quen "Uống nước" (đơn vị ml) → 2. Chọn danh mục **Giấc ngủ** → 3. Bấm **Cập nhật**.
- **Kết quả mong đợi:** Đơn vị đổi thành **giờ**; dòng tiến độ trong danh sách hiển thị theo định dạng giấc ngủ **`X.X / 2000.0 giờ`** (giá trị mục tiêu cũ giữ nguyên, không tự đổi).

### TC-ADD-11 — Xóa thói quen (soft delete)
- **Ưu tiên:** P0
- **Bước:**
  1. Mở sửa 1 thói quen (thói quen này đã có log) → 2. Bấm nút thùng rác đỏ.
  3. Đọc dialog **"Xóa thói quen?"** → bấm **Xóa**.
- **Kết quả mong đợi:**
  - Bấm thùng rác **không xóa ngay** nữa mà hiện dialog xác nhận: tiêu đề **"Xóa thói quen?"**, nội dung nói rõ *"Thói quen sẽ bị ẩn khỏi danh sách… Nhật ký đã ghi vẫn được giữ lại trong máy."*, hai nút **Hủy** / **Xóa** (đỏ).
  - Bấm **Xóa** → hiện Toast **"Đã xóa thói quen"** và quay về danh sách; thói quen biến mất khỏi danh sách, màn Home, widget (DAO lọc `isActive = 1`).
  - **Kiểm tra DB:** bảng `habits` vẫn còn hàng đó với `isActive = 0` (xóa mềm, không xóa hàng).
  - Log của thói quen vẫn còn — xem TC-ADD-12.

### TC-ADD-12 — Log của thói quen đã xóa
- **Ưu tiên:** P2
- **Tiền điều kiện:** Thói quen đã có log, sau đó bị xóa qua UI (soft delete, xem TC-ADD-11).
- **Bước:** 1. Kiểm tra `habit_logs` trong DB → 2. Xem màn Phân tích (Insights) sau đó.
- **Kết quả mong đợi:** Log **vẫn còn** (soft delete không kích hoạt `ON DELETE CASCADE`); các rule AI vẫn đọc được log này để tính gợi ý và điểm sinh lực (giữ nguyên thiết kế — không đổi theo bản sửa lỗi #9).

### TC-ADD-13 — Tạo nhiều thói quen trùng tên
- **Ưu tiên:** P2
- **Tiền điều kiện:** Chưa có thói quen nào tên "Test".
- **Bước:** 1. FAB `+` → nhập tên `Test`, mục tiêu `2000` ml → **Tạo thói quen**. 2. FAB `+` → nhập tên `Test`, mục tiêu `1000` ml → **Tạo thói quen**.
- **Kết quả mong đợi:**
  - Bước 1 lưu thành công; bước 2: hộp thoại lỗi **"Lỗi"** với nội dung **"Tên thói quen đã tồn tại"**; **không** tạo bản ghi thứ hai, danh sách vẫn chỉ có 1 thẻ "Test", vẫn ở màn Thêm thói quen (không điều hướng về danh sách).
  - Sau khi đóng hộp thoại lỗi, đổi tên thành `Test 2` → bấm **Tạo thói quen** → lưu thành công, danh sách có 2 thẻ.
  - Kiểm tra DB: bảng `habits` chỉ có các hàng với tên khác nhau (không có 2 hàng cùng tên đang hoạt động).
- **Ghi chú:** Đây là bản sửa lỗi **mục 3 #13** (trước đây cả 2 đều được lưu). Kiểm tra không phân biệt hoa/thường và bỏ khoảng trắng thừa (`"  TEST  "` cũng bị chặn) — xem TC-ADD-25. Thói quen đã **xóa mềm** không tính là trùng nên tên đó dùng lại được (xem TC-ADD-11).

### TC-ADD-14 — Bấm nút Lưu liên tiếp (double tap)
- **Ưu tiên:** P1
- **Bước:** 1. Nhập dữ liệu hợp lệ → 2. Bấm **Tạo thói quen** 2–3 lần thật nhanh.
- **Kết quả mong đợi:**
  - Chỉ **1** thói quen được tạo (không có 2 thẻ cùng tên, `habits` chỉ thêm 1 hàng).
  - Bấm lần đầu: nút đáy chuyển sang trạng thái **đang lưu** — mờ đi (không bấm được) và hiện **vòng xoay** thay cho chữ "Tạo thói quen" (cờ `isSaving`).
  - Bấm lần 2/3 trong lúc đó **không** tạo thêm bản ghi; không crash, không màn hình trắng.
  - Xong: tự quay về danh sách như bình thường; mở lại màn Thêm thói quen thì nút trở lại bình thường (cờ đã được nhả).
- **Ghi chú:** Đây là bản sửa lỗi **phụ lục C #12** (trước đây double tap ở màn Thêm/Sửa tạo nhiều bản ghi). Cơ chế: `AddEditHabitViewModel.saveHabit()` bỏ qua lời gọi khi `isSaving == true`, cờ bật **trước khi** phát coroutine ghi DB và tắt ở `finally` — xem TC-ADD-27. Kiểm tra thêm ở màn Sửa: bấm **Cập nhật** 2 lần thật nhanh cũng chỉ cập nhật 1 lần.

### TC-ADD-15 — Tên thói quen rất dài
- **Ưu tiên:** P2
- **Bước:** 1. Nhập tên 200 ký tự → 2. Tạo.
- **Kết quả mong đợi:** Lưu được; thẻ trong danh sách không tràn layout (tên xuống dòng hoặc ellipsize tuỳ hành vi mặc định của `Text`).

### TC-ADD-16 — Tên có emoji / ký tự đặc biệt
- **Ưu tiên:** P2
- **Bước:** 1. Tạo thói quen tên `Bơi 🏊 100m <b>test</b> & "quote"`.
- **Kết quả mong đợi:** Lưu và hiển thị đúng nguyên văn, không lỗi encoding.

### TC-ADD-17 — Giữ dữ liệu đang nhập khi xoay màn hình
- **Ưu tiên:** P2
- **Bước:** 1. Vào màn Thêm → nhập tên + mục tiêu → 2. Xoay màn hình (hoặc `adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1`).
- **Kết quả mong đợi:** Dữ liệu đã nhập **giữ nguyên** (state nằm trong `AddEditHabitViewModel`).

### TC-ADD-18 — Khôi phục sau process death
- **Ưu tiên:** P2
- **Bước:** 1. Vào sửa thói quen "Uống nước" → 2. Bật "Don't keep activities" (Developer options) rồi chuyển ra ngoài app → 3. Quay lại.
- **Kết quả mong đợi:** Màn Sửa mở lại đúng thói quen đó (nhờ `SavedStateHandle["habitId"]`), dữ liệu load lại từ DB.

### TC-ADD-19 — Nút đáy luôn hiển thị khi cuộn form
- **Ưu tiên:** P2
- **Bước:** 1. Mở màn Thêm → 2. Cuộn xuống phần "Tần suất".
- **Kết quả mong đợi:** Nút **"Tạo thói quen"** vẫn nằm ở bottom bar (không bị cuộn mất).

### TC-ADD-20 — Tất cả tổ hợp tần suất
- **Ưu tiên:** P2
- **Bước:** 1. Lần lượt chọn **Hàng ngày / Hàng tuần / Hàng tháng** → 2. Tạo 3 thói quen tương ứng.
- **Kết quả mong đợi:** Mỗi radio chỉ chọn 1 giá trị tại một thời điểm; giá trị được lưu đúng vào DB.
- **Ghi chú:** Hiện `frequency` **chưa ảnh hưởng** đến logic hiển thị/tiến độ (mọi thói quen đều được tính theo ngày) — cần xác nhận với BA.

### TC-ADD-21 — Hủy dialog xác nhận xóa
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đang ở màn Sửa một thói quen đã có log.
- **Bước:** 1. Bấm nút thùng rác → 2. Bấm **Hủy** (hoặc bấm ra ngoài dialog).
- **Kết quả mong đợi:** Dialog đóng, **không** có gì bị xóa: vẫn ở màn Sửa với dữ liệu cũ; thói quen vẫn hiện trong danh sách/Home; **không** có Toast; DB không đổi (`isActive` vẫn `1`).
- **Kiểm tra thêm:** Bấm thùng rác lần nữa mở lại được dialog (trạng thái dialog chỉ là state của màn hình, không lưu vào ViewModel).

### TC-ADD-22 — Unit test JVM cho luồng xóa mềm
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*AddEditHabitViewModelTest" --tests "*HabitRepositoryImplTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm:
  - `HabitRepositoryImplTest`: `deleteHabit` gọi `habitDao.softDeleteHabit(id)` (không xóa hàng khỏi bảng).
  - `AddEditHabitViewModelTest`: xóa thành công → `isDeleted = true` (và `isSaved = false`); chế độ Thêm mới → không gọi use case; lỗi DB → giữ màn hình + thông báo "Lỗi khi xóa: …"; bấm xóa lần hai → không gọi lại; lưu thói quen vẫn bật `isSaved` (không lẫn với cờ xóa).
- **Ghi chú:** Hành vi DAO (`UPDATE habits SET isActive = 0`) đã có test instrumented `HabitDaoTest.softDeleteHabit`.

### TC-ADD-23 — Ô "Mục tiêu" hiển thị số sạch khi mở màn Sửa (mục 3 #11)
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có thói quen "Uống nước" mục tiêu **2000 ml** (và nếu muốn test số thập phân: một thói quen "Ngủ đủ giấc" mục tiêu **7.5 giờ** — tạo bằng cách nhập `7.5` qua bàn phím phần cứng/paste, xem ghi chú TC-HAB-11).
- **Bước:**
  1. Bấm vào thẻ "Uống nước" → quan sát ô **"Mục tiêu hàng ngày"**.
  2. Bấm **"Cập nhật"** ngay mà **không** sửa gì → kiểm tra lại danh sách và bảng `habits`.
  3. Lặp lại bước 1 với thói quen có mục tiêu `7.5`.
- **Kết quả mong đợi:**
  - Ô mục tiêu hiển thị **`2000`** — **không** còn `2000.0`; với mục tiêu thập phân hiển thị **`7.5`** (không mất phần thập phân, không thành `7,5`).
  - Bấm "Cập nhật" ngay vẫn lưu bình thường (**không** báo "Mục tiêu phải là số dương"): giá trị trong `habits.targetValue` **không đổi** (2000 / 7.5) — chuỗi hiển thị vẫn parse được bằng `toFloatOrNull()`.
  - Đơn vị, danh mục, thời gian, tần suất không đổi.
- **Ghi chú:** Chỉ đổi **chuỗi hiển thị** trong ô nhập, không đổi giá trị đã lưu. Ô mặc định của **hộp thoại ghi nhận** (`HabitsScreen`) cũng dùng chung hàm này nên đã hết `2000.0` (mục 3 #12; xem TC-HAB-26).

### TC-ADD-24 — Unit test JVM cho định dạng mục tiêu
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*AddEditHabitViewModelTest"`.
- **Kết quả mong đợi:** Ngoài các test luồng xóa mềm (TC-ADD-22), 3 test mới pass:
  - Thói quen mục tiêu `2000f` → `uiState.targetValue == "2000"` (không có đuôi `.0`).
  - Thói quen mục tiêu `7.5f` → `uiState.targetValue == "7.5"` (giữ phần thập phân).
  - Mở màn Sửa rồi bấm **Cập nhật** ngay (không sửa gì) → `updateHabitUseCase` được gọi với `targetValue = 2000f` (chuỗi hiển thị parse lại đúng giá trị).

### TC-ADD-25 — Chống trùng tên khi tạo và khi đổi tên (mục 3 #13)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A (có "Uống nước", "Chạy bộ").
- **Bước:**
  1. FAB `+` → nhập tên `Uống nước`, mục tiêu `500` → **Tạo thói quen**.
  2. Đóng hộp thoại lỗi → đổi tên thành `  uống NƯỚC  ` (có khoảng trắng, khác hoa/thường) → **Tạo thói quen**.
  3. Đổi tên thành `Uống nước buổi sáng` → **Tạo thói quen**.
  4. Mở sửa "Uống nước" → **Cập nhật** ngay (không đổi gì) → sau đó đổi tên thành `Chạy bộ` → **Cập nhật**.
  5. Xóa mềm "Chạy bộ" (TC-ADD-11) → tạo thói quen mới tên `Chạy bộ`.
- **Kết quả mong đợi:**
  - Bước 1 và 2: hộp thoại lỗi nội dung **"Tên thói quen đã tồn tại"** — so sánh **không phân biệt hoa/thường** và **bỏ khoảng trắng thừa**; **không** tạo bản ghi nào mới.
  - Bước 3: lưu thành công (tên khác) → về danh sách, thấy thẻ mới.
  - Bước 4: bấm Cập nhật với tên cũ **không** bị coi là trùng (so với chính nó) → lưu bình thường; đổi sang tên của thói quen khác (`Chạy bộ`) → lỗi **"Tên thói quen đã tồn tại"**, không cập nhật.
  - Bước 5: tên của thói quen **đã xóa mềm** dùng lại được (không báo trùng) → vì DAO chỉ đọc `isActive = 1`.
  - Kiểm tra DB (`habits`): không bao giờ có 2 hàng **đang hoạt động** cùng tên.
- **Ghi chú:** Việc kiểm tra diễn ra ngay trước khi ghi DB; trường hợp bấm lưu 2 lần thật nhanh (double tap) đã được chặn bằng cờ `isSaving` (phụ lục C #12 — xem TC-ADD-14, TC-ADD-27) nên không còn tạo được 2 hàng cùng tên.

### TC-ADD-26 — Unit test JVM cho chống trùng tên
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*AddEditHabitViewModelTest"`.
- **Kết quả mong đợi:** `AddEditHabitViewModelTest` pass (13 test), trong đó 5 test cho mục 3 #13:
  - Tạo mới với tên đã tồn tại → `error = "Tên thói quen đã tồn tại"`, `isSaved = false`, `addHabitUseCase` **không** được gọi.
  - Tên khác hoa/thường + khoảng trắng thừa (`"  uống NƯỚC  "`) → vẫn bị chặn.
  - Tên hoàn toàn mới → lưu thành công, `addHabitUseCase` gọi 1 lần, không có `error`.
  - Mở màn Sửa rồi Cập nhật **không đổi tên** → hợp lệ (không tự coi mình là trùng), `updateHabitUseCase` gọi 1 lần.
  - Màn Sửa đổi sang tên của thói quen khác → bị chặn, `updateHabitUseCase` **không** được gọi.

### TC-ADD-27 — Unit test JVM chống double-submit khi lưu
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*AddEditHabitViewModelTest"`.
- **Kết quả mong đợi:** `AddEditHabitViewModelTest` pass (15 test), trong đó 2 test cho phụ lục C #12:
  - Bấm **saveHabit()** lần 1 khi `addHabitUseCase` còn "đang chạy" (giữ bằng `CompletableDeferred`) → `isSaving == true`; bấm lần 2 → bị bỏ qua, `addHabitUseCase` chỉ được gọi **1 lần**; sau khi xong → `isSaving == false`, `isSaved == true`.
  - Lưu bị chặn do trùng tên → cờ `isSaving` vẫn được nhả (`false`), `isSaved == false` (không kẹt ở trạng thái "đang lưu").

---

## 10. TC-CAM — Màn hình Ghi chép (Camera + ML Kit)

Thành phần: tiêu đề **"Ghi chép bữa ăn"**, phụ đề **"Hướng camera vào món ăn để phân tích."**, thẻ **"Cần quyền Camera"** khi chưa cấp quyền, preview camera thật 400dp (CameraX `PreviewView`) + khung quét 220dp + chip **"Quét món ăn"**, nút **"Phân tích món ăn"** (chụp ảnh → ML Kit), thẻ lỗi đỏ khi có lỗi, thẻ **"Kết quả nhận diện"** khi có nhãn.

### TC-CAM-01 — Chưa cấp quyền Camera
- **Ưu tiên:** P0
- **Tiền điều kiện:** `pm clear` app (quyền Camera chưa cấp).
- **Bước:** 1. Mở tab Ghi chép.
- **Kết quả mong đợi:** Hiển thị thẻ trắng: icon camera xanh 64dp, tiêu đề **"Cần quyền Camera"**, mô tả **"Để nhận diện món ăn, ứng dụng cần truy cập camera của bạn."**, nút xanh **"Cấp quyền Camera"**. **Không** thấy preview và nút phân tích.

### TC-CAM-02 — Cấp quyền Camera
- **Ưu tiên:** P0
- **Bước:** 1. Bấm **"Cấp quyền Camera"** → 2. Trong hộp thoại hệ thống chọn **Allow / Cho phép**.
- **Kết quả mong đợi:** Thẻ xin quyền biến mất; hiện khối preview + nút **"Phân tích món ăn"**.

### TC-CAM-03 — Từ chối quyền Camera
- **Ưu tiên:** P0
- **Bước:** 1. `pm clear` → 2. Mở tab Ghi chép → 3. Bấm **"Cấp quyền Camera"** → 4. Chọn **Deny / Từ chối**.
- **Kết quả mong đợi:** Thẻ xin quyền vẫn hiển thị; app không crash; bấm lại nút sẽ hiện lại hộp thoại hệ thống (nếu Android còn cho phép hỏi).

### TC-CAM-04 — Giao diện preview camera thật
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Camera.
- **Bước:** 1. Quan sát khối preview.
- **Kết quả mong đợi:**
  - Khối bo góc 48dp cao 400dp hiển thị **hình ảnh camera trực tiếp** (CameraX `PreviewView`, `ScaleType.FILL_CENTER`), không còn là khối màu xám.
  - Có khung vuông viền trắng 220dp bo 32dp ở giữa và chip nền đen mờ chứa icon `FilterCenterFocus` + chữ **"Quét món ăn"** ở đáy.
  - Khối preview biến mất khi màn hình chuyển sang background (camera tự unbind theo lifecycle) và hoạt động lại khi quay lại.
- **Ghi chú:** Preview dùng `LifecycleCameraController` (tự quản lý init + lifecycle). Nút "Phân tích món ăn" chỉ bật khi controller đã bind xong.

### TC-CAM-05 — Nút "Phân tích món ăn"
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Camera, preview đã hiển thị (nút ở trạng thái bật).
- **Bước:** 1. Hướng camera vào món ăn → 2. Bấm nút **"Phân tích món ăn"**.
- **Kết quả mong đợi:**
  - App chụp 1 ảnh từ camera (`ImageCapture`, chế độ `CAPTURE_MODE_MINIMIZE_LATENCY`), chuyển `ImageProxy` → `Bitmap` → `CameraViewModel.analyzeBitmap()`.
  - Chữ trên nút đổi ngay thành **"Đang phân tích..."** (nút bị **disable** trong lúc xử lý).
  - Sau khi ML Kit trả kết quả: hiện thẻ **"Kết quả nhận diện"** và nút trở lại **"Phân tích món ăn"**.
- **Ghi chú:** Chỉ nhận nhãn có độ tin cậy ≥ 0.65 (`ImageLabelerOptions.setConfidenceThreshold(0.65f)`) — có thể không có nhãn nào cho ảnh khó.

### TC-CAM-06 — Trạng thái đang phân tích
- **Ưu tiên:** P2
- **Bước:** 1. Bấm **"Phân tích món ăn"** và quan sát ngay.
- **Kết quả mong đợi:** Chữ trên nút đổi thành **"Đang phân tích..."**; nút **không bấm được** cho tới khi có kết quả → không chồng nhiều lượt phân tích.

### TC-CAM-07 — Kết quả nhận diện hợp lệ
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Camera; đưa vào khung hình một món ăn rõ ràng.
- **Bước:** 1. Bấm **"Phân tích món ăn"** → 2. Chờ kết quả.
- **Kết quả mong đợi:** Thẻ **"Kết quả nhận diện"** với nhãn **"Tự động"**; mỗi nhãn là chip xanh bo 24dp: emoji (🥗 salad / 🥑 bơ / 🥚 trứng luộc / 🍲 mặc định) + tên nhãn + **% độ tin cậy** (`confidence * 100`); chỉ nhận nhãn có độ tin cậy **≥ 0.65**; nút viền **"Thêm vào nhật ký"** — bấm nút này thì mở hộp thoại ghi nhận thói quen **Dinh dưỡng** (xem **TC-CAM-15**), không còn là nút "chết".
- **Ghi chú:** ML Kit Image Labeling trả **nhãn chung của ảnh**, không phải dữ liệu dinh dưỡng chuyên biệt → nhãn có thể là "Food", "Fruit", "Table"… Cần xác nhận nghiệp vụ có chấp nhận hay phải thay bằng model nhận diện món ăn riêng (phụ lục C #21, vẫn đang mở).

### TC-CAM-08 — Ảnh không có món ăn
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đã cấp quyền Camera.
- **Bước:** 1. Hướng camera vào bàn trống → 2. Bấm **"Phân tích món ăn"**.
- **Kết quả mong đợi:** Không có nhãn nào vượt ngưỡng 0.65 → `detectedLabels` rỗng → **không** hiển thị thẻ "Kết quả nhận diện"; nút trở lại trạng thái thường; app không crash.

### TC-CAM-09 — Lỗi khi phân tích
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đã cấp quyền Camera.
- **Bước:** 1. Gây lỗi khi phân tích (ví dụ ảnh hỏng/`labeler.process` ném ngoại lệ).
- **Kết quả mong đợi:** `uiState.error = "Lỗi phân tích: <message>"`, `isAnalyzing = false`; UI hiển thị **thẻ lỗi đỏ** (`CameraErrorCard`) ngay dưới nút; không crash. Bấm phân tích lại → thẻ lỗi tự xoá khi bắt đầu lượt phân tích mới.

### TC-CAM-10 — Thu hồi quyền Camera khi đang mở app
- **Ưu tiên:** P2
- **Bước:** 1. Đang ở tab Ghi chép (đã cấp quyền) → 2. Vào Settings thu hồi quyền Camera → 3. Quay lại app.
- **Kết quả mong đợi:** Trở về thẻ xin quyền sau khi màn hình được tạo lại; không crash.

### TC-CAM-11 — Nút phân tích bị disable khi camera chưa sẵn sàng
- **Ưu tiên:** P1
- **Bước:** 1. Vừa cấp quyền Camera → 2. Bấm **"Phân tích món ăn"** ngay lập tức (trước khi preview hiện hình).
- **Kết quả mong đợi:** Trong khoảng thời gian camera đang khởi tạo, nút ở trạng thái **mờ/không bấm được**; sau khi preview hiện hình thì nút bật và bấm được. Không crash, không xảy ra lỗi chụp ảnh.
- **Ghi chú:** Nút bật khi `captureController != null` (đã bind lifecycle xong).

### TC-CAM-12 — Không mở được camera
- **Ưu tiên:** P1
- **Tiền điều kiện:** Trường hợp camera bị chiếm bởi app khác, hoặc emulator không có camera ảo.
- **Bước:** 1. Mở tab Ghi chép (đã cấp quyền) khi camera không khả dụng.
- **Kết quả mong đợi:** Hiện **thẻ lỗi đỏ** (`CameraErrorCard`) với nội dung **"Không mở được camera: <message>"** thay cho khối preview; app **không crash**; nút phân tích ở trạng thái không bấm được.

### TC-CAM-13 — Chụp và phân tích liên tiếp nhiều lần
- **Ưu tiên:** P2
- **Bước:** 1. Bấm phân tích → 2. Chờ kết quả → 3. Đổi món ăn trước camera → 4. Bấm phân tích lại (lặp 3 lần).
- **Kết quả mong đợi:** Mỗi lượt: danh sách nhãn cũ bị xoá ngay khi bắt đầu lượt mới, kết quả mới thay thế kết quả cũ; không rò rỉ `ImageProxy` (không có log `ImageProxy` chưa close); không giảm FPS/treo sau nhiều lượt.

### TC-CAM-14 — Preview hoạt động lại sau khi quay lại màn hình
- **Ưu tiên:** P2
- **Bước:** 1. Ở tab Ghi chép → 2. Chuyển sang tab khác → 3. Quay lại tab Ghi chép.
- **Kết quả mong đợi:** Preview hiển thị lại bình thường, nút phân tích bật lại; không có thông báo "Camera in use by another application".

### TC-CAM-15 — Nút "Thêm vào nhật ký" ghi kết quả vào `habit_logs` (phụ lục C #22)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Camera; có ít nhất 1 thói quen **Dinh dưỡng** đang hoạt động (Data set A: "Ăn salad — 600 calo").
- **Bước:**
  1. Tab **Ghi chép** → chụp ảnh món ăn → chờ thẻ **"Kết quả nhận diện"** (TC-CAM-07).
  2. Bấm nút viền **"Thêm vào nhật ký"** (dưới danh sách chip).
- **Kết quả mong đợi:**
  - App chuyển sang tab **Thói quen**; **hộp thoại ghi nhận mở sẵn** cho thói quen đúng **danh mục Dinh dưỡng** (ô "Số lượng" điền sẵn mục tiêu dạng số sạch — TC-HAB-26) — cùng luồng với nút hành động ở màn Phân tích (TC-INS-11).
  - Bấm **Lưu** → log được ghi vào `habit_logs` + Toast **"Đã ghi nhận: Ăn salad"** (TC-HAB-24); tab **Phân tích** cập nhật "Chỉ số sinh lực"/"Sự nhất quán" theo log mới.
  - Bấm **Hủy**/ra ngoài → chỉ đóng hộp thoại, vẫn ở tab Thói quen, **không** ghi log nào.
  - **Chưa có** thói quen Dinh dưỡng nào → Toast **"Chưa có thói quen phù hợp — bấm nút + để tạo mới."** và **không** mở hộp thoại (nhánh `LogRequest.Missing`).
  - Quay lại tab Ghi chép rồi bấm nút lần nữa **vẫn** hoạt động (yêu cầu là sự kiện một lần, không bị "dính").
- **Ghi chú:** Đây là bản sửa lỗi **phụ lục C #22** (trước đây `onClick` là `/* TODO */`). Kết quả nhận diện của ML Kit **không** tự điền vào log (vẫn là nhãn ảnh, phụ lục C #21) — người dùng nhập số lượng/nội dung ở hộp thoại ghi nhận.

---

## 11. TC-INS — Màn hình Phân tích (Rule Engine AI)

Thành phần: tiêu đề **"Phân tích AI"** + phụ đề **"Xu hướng sức khỏe của bạn, được phân tích để phát triển."**; danh sách thẻ gợi ý (nút hành động **chuyển sang tab Thói quen và mở sẵn hộp thoại ghi nhận** — TC-INS-11); thẻ **"Mọi thứ đang rất tốt!"** khi không có gợi ý; **"Chỉ số sinh lực"** (kèm so sánh với tuần trước); **"Sự nhất quán hàng tuần"** (biểu đồ cột 7 ngày); **"Xu hướng chất lượng giấc ngủ"** (Sâu/Nông/REM từ Health Connect). Cả 3 thẻ đều lấy **dữ liệu thật**, không còn số liệu hard-code.

### TC-INS-01 — Trạng thái rỗng (không có log)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `pm clear` (không có log nào).
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - Thẻ **"Mọi thứ đang rất tốt!"** với icon 🎉 + dòng **"Bạn đang duy trì thói quen rất đều đặn. Hãy tiếp tục phát huy nhé!"**.
  - **Chỉ số sinh lực = 50** (không có log → mặc định 50); vòng tròn tô **4/8** cung.
  - Có đủ 2 thẻ biểu đồ (Sự nhất quán hàng tuần + Xu hướng chất lượng giấc ngủ) — cả hai đều lấy dữ liệu thật.

### TC-INS-02 — Cảnh báo uống nước CRITICAL
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thói quen Uống nước; ghi log **500 ml** (trung bình < 1000).
- **Bước:** 1. Ghi 1 log 500 ml cho "Uống nước" → 2. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - Thẻ nền **theo theme** (`surfaceContainerLow` của Material **+** lớp `Color.Red` mờ 12% phủ từ cạnh trái), **viền đỏ**, nhãn **"CẢNH BÁO KHẨN CẤP"** màu đỏ, icon `WaterDrop` đỏ. *(Trước bản sửa lỗi #18 nền là `#FFEBEE` cứng — xem phụ lục C #18.)*
  - Tiêu đề **"🚨 Mức nước nguy hiểm thấp"**; nội dung chứa **"Bạn uống trung bình 500ml/ngày — chỉ bằng 50% nhu cầu tối thiểu!"**; nút đỏ **"Ghi nhận uống nước"**.

### TC-INS-03 — Gợi ý uống nước MEDIUM
- **Ưu tiên:** P0
- **Tiền điều kiện:** Log nước nằm trong khoảng **1000 ≤ trung bình < 1500**.
- **Bước:** 1. Ghi log **1200 ml** → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ nền **theo theme** (`surfaceContainerLow` **+** lớp xanh `#2E7D32` mờ 12%, không còn `Color.White` cứng), nhãn **"BLOOM AI GỢI Ý"** màu xanh, tiêu đề **"💧 Uống thêm nước mỗi ngày"**, nội dung chứa **"Lượng nước trung bình của bạn là 1200ml/ngày."** và **"Mục tiêu khuyến nghị: 2000ml"**. **Không** có action label (không có nút).

### TC-INS-04 — Không có gợi ý nước khi đủ
- **Ưu tiên:** P1
- **Tiền điều kiện:** Log nước trung bình ≥ 1500 ml.
- **Bước:** 1. Ghi log nước `2000` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Không xuất hiện thẻ nào thuộc danh mục nước.
- **Lưu ý:** Nếu đồng thời có log ngủ/vận động bất thường thì các thẻ khác vẫn hiển thị.

### TC-INS-05 — Cảnh báo thiếu ngủ HIGH (rule log thủ công)
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thói quen giấc ngủ (đơn vị giờ); ghi 3 log **< 6** trong 3 lần gần nhất.
- **Bước:** 1. Ghi lần lượt `5`, `5.5`, `4.5` cho thói quen giấc ngủ → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ nền `#FFF3E0` (cam nhạt), nhãn **"BLOOM AI GỢI Ý"** màu cam đậm `#E65100`, tiêu đề **"⚠️ Cảnh báo thiếu ngủ nghiêm trọng"**, nội dung chứa **"Bạn đã ngủ trung bình 5.0 tiếng/đêm trong 3 ngày qua."** và câu về giảm 40% khả năng tập trung; nút cam **"Đặt nhắc nhở đi ngủ"**.

### TC-INS-06 — Gợi ý ngủ MEDIUM
- **Ưu tiên:** P0
- **Tiền điều kiện:** 3 log ngủ gần nhất đều **≥ 6** nhưng trung bình **< 7**.
- **Bước:** 1. Ghi `6.5`, `6.5`, `6.5` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ nền trắng, tiêu đề **"💤 Cải thiện chất lượng giấc ngủ"**, nội dung **"Chất lượng giấc ngủ của bạn có thể tốt hơn. Thử tắt thiết bị điện tử 1 tiếng trước khi ngủ."**; **không** có nút hành động.
- **Lưu ý:** Điều kiện là `recent7.size >= 3` (không phải ≥ 5).

### TC-INS-07 — Không có gợi ý khi ngủ đủ
- **Ưu tiên:** P1
- **Bước:** 1. Ghi `8`, `8`, `7.5` cho giấc ngủ → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Không có thẻ thuộc danh mục Sleep.

### TC-INS-08 — Cảnh báo ít vận động HIGH (đơn vị **bước**) — phụ lục C #17
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thói quen vận động có **đơn vị bước** (`bước`/`bước chân`); 5 log gần nhất đều **< 5000**.
- **Bước:** 1. Ghi `1000`, `2000`, `3000`, `4000`, `4000` cho thói quen vận động → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ cam, tiêu đề **"🏃 Tăng hoạt động thể chất ngay hôm nay"**, nội dung chứa **"Số bước chân trung bình của bạn là 2800 bước trong 5 ngày qua."**; nút cam **"Bắt đầu theo dõi bước chân"**.
- **Ghi chú:** Ngưỡng 5000/8000 chỉ áp dụng cho log có **đơn vị bước** — đúng nghĩa "bước/ngày" của WHO. Thói quen đo bằng đơn vị khác (ví dụ "phút") đi theo rule **so với mục tiêu của chính thói quen** — xem **TC-INS-29** (đã sửa phụ lục C #17: trước đây rule so `value` với 5000/8000 **bất kể đơn vị**).

### TC-INS-09 — Gợi ý vận động LOW (đơn vị **bước**) — phụ lục C #17
- **Ưu tiên:** P1
- **Tiền điều kiện:** 5 log gần nhất **có ít nhất 1 log ≥ 5000**, trung bình 7 log **< 8000**.
- **Bước:** 1. Ghi `6000`, `6000`, `6000`, `6000`, `6000` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ nền trắng, nhãn **"BLOOM AI GỢI Ý"** màu xám, tiêu đề **"🚶 Hướng tới 8000 bước mỗi ngày"**, nội dung nhắc khuyến nghị WHO 8000 bước/ngày; không có nút.

### TC-INS-10 — Sắp xếp gợi ý theo mức độ ưu tiên
- **Ưu tiên:** P1
- **Tiền điều kiện:** Data set A; ghi log: nước `500` (CRITICAL), ngủ `5/5.5/4.5` (HIGH), vận động `1000×5` (HIGH).
- **Bước:** 1. Mở tab Phân tích → 2. Quan sát thứ tự thẻ.
- **Kết quả mong đợi:** Sắp xếp giảm dần theo mức độ: **CRITICAL** (nước) → **HIGH** (ngủ/vận động) → **MEDIUM** → **LOW** → thấp nhất. Thẻ CRITICAL luôn nằm trên cùng.

### TC-INS-11 — Thẻ gợi ý: nút hành động
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đang có thẻ CRITICAL nước (TC-INS-02) và có thói quen "Uống nước" đang hoạt động.
- **Bước:** 1. Bấm nút **"Ghi nhận uống nước"**.
- **Kết quả mong đợi:**
  - Ứng dụng chuyển sang tab **Thói quen**, tab đó được đánh dấu trên bottom bar (không mở màn hình mới ngoài luồng tab).
  - **Hộp thoại "Ghi nhận Uống nước" mở sẵn** (ô nhập điền sẵn mục tiêu dạng số sạch), không cần người dùng tự tìm thẻ và bấm "Ghi".
  - Bấm **Lưu** → ghi log bình thường + Toast **"Đã ghi nhận: Uống nước"** (TC-HAB-24); bấm **Hủy**/ra ngoài → chỉ đóng hộp thoại, vẫn ở tab Thói quen.
  - Quay lại tab Phân tích rồi bấm nút lần nữa **vẫn** mở lại được hộp thoại (yêu cầu là sự kiện một lần, không bị "dính").
- **Ghi chú:** Đây là bản sửa lỗi **mục 3 #14** (trước đây `onClick` là TODO, bấm không có gì xảy ra). Chi tiết nhánh "chưa có thói quen tương ứng" ở TC-INS-27.

### TC-INS-12 — Chỉ số sinh lực: hoàn thành 100%
- **Ưu tiên:** P0
- **Tiền điều kiện:** Ghi log đạt 100% mục tiêu cho cả 3 danh mục Ngủ, Nước, Vận động (trong 7 ngày).
- **Bước:** 1. Ghi: ngủ `8` (target 8), nước `2000` (target 2000), vận động `30` (target 30) → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** **Chỉ số sinh lực = 100**; vòng tròn tô đủ **8/8** cung; hiển thị `100` + `/100`.

### TC-INS-13 — Chỉ số sinh lực: hoàn thành 50%
- **Ưu tiên:** P0
- **Tiền điều kiện:** Log = 50% mục tiêu cho cả 3 danh mục.
- **Bước:** 1. Ghi: ngủ `4`, nước `1000`, vận động `15` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Điểm = **50** (4/8 cung).
- **Công thức đối chiếu:** `sleep*0.4 + water*0.3 + exercise*0.3`.

### TC-INS-14 — Chỉ số sinh lực khi thiếu 1–2 danh mục
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chỉ có log nước đạt 100% (không có log ngủ/vận động).
- **Bước:** 1. Ghi nước `2000` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Điểm = **79**, do danh mục không có dữ liệu được tính mặc định **70**: `100*0.3 + 70*0.4 + 70*0.3 = 30 + 28 + 21 = 79`.

### TC-INS-15 — Điểm sinh lực cập nhật realtime
- **Ưu tiên:** P1
- **Bước:** 1. Mở tab Phân tích (ghi nhớ điểm hiện tại) → 2. Sang tab Thói quen ghi thêm đủ mục tiêu nước → 3. Quay lại tab Phân tích.
- **Kết quả mong đợi:** Điểm tăng tương ứng (Flow reactive, không cần refresh thủ công).

### TC-INS-16 — Biên trên của điểm
- **Ưu tiên:** P2
- **Bước:** 1. Ghi log gấp 5 lần mục tiêu ở cả 3 danh mục.
- **Kết quả mong đợi:** Điểm **không vượt 100** (`coerceIn(0, 100)`), vòng tròn không vẽ quá 8 cung, không crash.

### TC-INS-17 — Biên dưới của điểm
- **Ưu tiên:** P2
- **Bước:** 1. Ghi log = 0 hoặc giá trị rất nhỏ.
- **Kết quả mong đợi:** Điểm **không âm**, nằm trong [0, 100].

### TC-INS-18 — Gợi ý từ dữ liệu Health Connect (thiếu ngủ 3 ngày) — phụ lục C #16
- **Ưu tiên:** P1
- **Tiền điều kiện:** Emulator có Health Connect; đã cấp đủ 4 quyền; đã bơm `SleepSessionRecord` **< 6 giờ** trong khoảng "18:00 hôm qua → hiện tại".
- **Bước:**
  1. Mở app (đăng ký `periodic_health_sync_worker`).
  2. App Inspection → Background Task Inspector → **Run** `periodic_health_sync_worker` **3 lần**, nhưng **mỗi lần lùi lại 1 ngày**: cách đơn giản nhất là **đổi ngày hệ thống của emulator** (Settings → System → Date) sang hôm qua rồi run, rồi lùi tiếp 2 ngày trước rồi run (mỗi lần ghi 1 hàng vào `health_data_history`, mỗi hàng thuộc **một ngày khác nhau**).
  3. Mở tab Phân tích.
- **Kết quả mong đợi:** Xuất hiện thẻ **CRITICAL** tiêu đề **"🚨 Phát hiện xu hướng thiếu ngủ"**, nội dung **"Hệ thống ghi nhận bạn đã ngủ dưới 6 giờ trong 3 ngày liên tiếp. Cơ thể bạn cần được phục hồi để tránh suy giảm hệ miễn dịch."**, nút **"Tối ưu hóa phòng ngủ"**.
- **Ghi chú:** Truy vấn dùng `health_data_history` sắp xếp `recordedAt DESC`, nhưng `HabitRuleEngine.analyzeHealthTrends` **gom theo ngày** (mỗi ngày lấy hàng mới nhất) rồi mới xét: cần **3 ngày gần nhất có dữ liệu và liên tiếp nhau**, mỗi ngày `sleepHours < 6`. Vì vậy **3 lần sync trong cùng một ngày KHÔNG** tạo ra cảnh báo (test `HabitRuleEnginePersonalizedTest`, TC-INS-30).

### TC-INS-19 — Gợi ý nhịp tim cao — phụ lục C #16
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã bơm `HeartRateRecord` ~**90 bpm** trong 1 giờ gần nhất; đã cấp quyền.
- **Bước:** 1. Run `periodic_health_sync_worker` **7 lần ở 7 ngày khác nhau** (lùi ngày hệ thống như TC-INS-18) → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **MEDIUM** tiêu đề **"💓 Nhịp tim nghỉ ngơi hơi cao"**, nội dung chứa **"Nhịp tim trung bình 7 ngày qua của bạn là 90 bpm."**, icon `Psychology`, nút **"Bắt đầu thiền"**.
- **Ghi chú:** Nếu dữ liệu < 85 bpm → **không** có gợi ý này. Con số trung bình nay đúng là "7 **ngày** gần nhất" (trước bản sửa #16 là 7 **lần sync** ≈ 1,75 ngày nhưng thông báo vẫn ghi "7 ngày qua").

### TC-INS-20 — Kết hợp gợi ý log thủ công + dữ liệu Health Connect
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có log nước thấp (TC-INS-02) + dữ liệu ngủ < 6h (TC-INS-18).
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:** Hiển thị **cả hai** thẻ (Water và Sleep).

### TC-INS-21 — Biểu đồ "Sự nhất quán hàng tuần" (dữ liệu thật)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có 2 thói quen đang hoạt động: "Uống nước" (2000 ml) và "Ngủ đủ giấc" (8 giờ).
- **Bước:**
  1. Hôm nay ghi `2000` ml cho Uống nước, `5` giờ cho Ngủ → 2. Mở tab Phân tích → 3. Quan sát thẻ "Sự nhất quán hàng tuần".
- **Kết quả mong đợi:**
  - Chú giải chấm tròn xanh + **"Thói quen đạt được"**; 7 cột tương ứng 7 ngày gần nhất (cũ → mới), nhãn dưới mỗi cột là **T2…CN**.
  - Cột **hôm nay**: nhãn **"50%"** (1/2 thói quen đạt), cột cao bằng 50% chiều cao cột nền, màu xanh đậm `#1B5E20`; nhãn ngày hôm nay cũng in đậm.
  - Các cột ngày khác không có log: chỉ thấy **cột nền xám** + nhãn **"0%"**.
  - Có dòng chú thích **"Tỉ lệ thói quen đạt mục tiêu theo từng ngày (7 ngày gần nhất)"**.
- **Kiểm tra biên:** ghi đủ cả 2 thói quen hôm nay → nhãn **"100%"**, cột đầy; ghi thêm log cùng ngày (ví dụ 1200 + 800 ml) vẫn tính là đạt (cộng dồn trong ngày).

### TC-INS-22 — Loading màn Phân tích
- **Ưu tiên:** P2
- **Bước:** 1. `pm clear` → 2. Mở ngay tab Phân tích.
- **Kết quả mong đợi:** Hiện `CircularProgressIndicator` màu xanh `#2E7D32` căn giữa trong lúc tổng hợp; sau đó hiển thị nội dung.

### TC-INS-23 — Thẻ giấc ngủ chưa có dữ liệu
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thu hồi quyền Giấc ngủ của HealthyAI trong Health Connect (hoặc không bơm `SleepSessionRecord` có pha).
- **Bước:** 1. Mở tab Phân tích → 2. Xem thẻ "Xu hướng chất lượng giấc ngủ".
- **Kết quả mong đợi:** Thay vì 3 thanh số liệu, hiển thị dòng **"Chưa có dữ liệu chi tiết pha giấc ngủ. Cần cấp quyền Giấc ngủ cho Health Connect và thiết bị phải ghi được pha Sâu/Nông/REM."**; app **không crash**.

### TC-INS-24 — Thẻ giấc ngủ với dữ liệu pha thật
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Giấc ngủ; bơm `SleepSessionRecord` trong **7 ngày gần nhất** với các pha, ví dụ Deep 2h15m, Light 4h30m, REM 1h10m.
- **Bước:** 1. Mở tab Phân tích (mở lại màn hình nếu đang mở) → 2. Xem thẻ "Xu hướng chất lượng giấc ngủ".
- **Kết quả mong đợi:**
  - 3 thanh **Sâu / Nông / REM** với thanh tiến độ = tỉ lệ phút của pha đó trên tổng, màu lần lượt `#1B5E20`, `#2E7D32`, `#81C784`; số phút hiển thị dạng **"2h 15m"**.
  - Dòng cuối: **"Tổng 7h 55m trong 7 đêm gần nhất"**.
- **Ghi chú:** Giá trị đọc **trực tiếp từ Health Connect** mỗi lần mở màn hình (không qua Room). Thiết bị chỉ báo `STAGE_TYPE_SLEEPING` (không chia pha) → rơi vào trạng thái của TC-INS-23.

### TC-INS-25 — Delta điểm sinh lực so với tuần trước
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chỉ có log trong 7 ngày gần nhất (không có log nào ở 7 ngày trước đó).
- **Bước:** 1. Mở tab Phân tích → 2. Xem thẻ "Chỉ số sinh lực".
- **Kết quả mong đợi:** Chỉ hiển thị điểm + `/100`, **không** có dòng so sánh (không còn dòng "+5 so với tuần trước" cố định).

### TC-INS-26 — Delta tăng / giảm / không đổi
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có log ở **cả** tuần này và 7 ngày trước đó (ví dụ: tuần này ngủ đủ `8` giờ; tuần trước chỉ ngủ `4` giờ so với mục tiêu 8).
- **Bước:** 1. Mở tab Phân tích → 2. Xem dòng dưới điểm sinh lực.
- **Kết quả mong đợi:**
  - Tuần này tốt hơn tuần trước → **"+20 so với tuần trước"** màu xanh `#2E7D32` (con số = điểm tuần này − điểm tuần trước).
  - Tuần này kém hơn → **"-20 so với tuần trước"** màu đỏ `#C62828`.
  - Bằng nhau → **"Không đổi so với tuần trước"** màu xám.
- **Đối chiếu công thức điểm:** `sleep*0.4 + water*0.3 + exercise*0.3`, danh mục không có log tính 70 điểm.

### TC-INS-27 — Nút hành động khi chưa có thói quen tương ứng (mục 3 #14)
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có gợi ý **Thiền định** (nhịp tim cao — TC-INS-19) nhưng **chưa** có thói quen nào thuộc danh mục Thiền định.
- **Bước:** 1. Mở tab Phân tích → 2. Bấm nút **"Bắt đầu thiền"** trên thẻ gợi ý đó.
- **Kết quả mong đợi:**
  - Chuyển sang tab **Thói quen** và hiện Toast **"Chưa có thói quen nào cho gợi ý này — bấm nút + để tạo mới."** (không mở hộp thoại ghi nhận vì không có thói quen nào để ghi).
  - Bấm FAB **+** → tạo thói quen Thiền định (danh mục mặc định là Uống nước → nhớ chọn **Thiền định 🧘**) → quay lại tab Phân tích, bấm lại nút **"Bắt đầu thiền"** → **lần này** hộp thoại "Ghi nhận …" mở sẵn (TC-INS-11).
  - App không crash ở cả hai nhánh.
- **Ghi chú:** Nút chỉ ẩn/hiện theo `actionLabel`, không phụ thuộc việc có thói quen hay không — nhánh "chưa có thói quen" được xử lý bằng lời nhắc thay vì im lặng.

### TC-INS-28 — Unit test JVM cho yêu cầu ghi nhận từ màn Phân tích
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitsViewModelTest"`.
- **Kết quả mong đợi:** `HabitsViewModelTest` pass (7 test), trong đó 3 test cho mục 3 #14:
  - `SavedStateHandle` có `logCategory = WATER` → `uiState.logRequest == LogRequest.Open(thói quen nước)` và khoá được **xoá** khỏi `SavedStateHandle` (không mở lại hộp thoại khi màn hình tạo lại); gọi `onLogRequestHandled()` → `logRequest` về `null`.
  - `logCategory = MINDFULNESS` nhưng chỉ có thói quen nước → `uiState.logRequest == LogRequest.Missing(MINDFULNESS)`.
  - `logCategory = "KHONG_CO_TON_TAI"` (giá trị lạ) → bỏ qua im lặng, `logRequest == null`, khoá vẫn được xoá.

### TC-INS-29 — Gợi ý vận động theo **mục tiêu** cho thói quen không đo bằng bước (phụ lục C #17)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thói quen **"Chạy bộ"** đang hoạt động, đơn vị **phút**, mục tiêu **30** phút (Data set A).
- **Bước:**
  1. Ghi **5 log `10`** phút cho "Chạy bộ" → 2. Mở tab Phân tích.
  3. Xoá log, ghi **5 log `21`** phút → 4. Mở tab Phân tích.
  5. Xoá log, ghi **5 log `30`** phút → 6. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - 5 log `10` phút (33% mục tiêu) → thẻ **HIGH** tiêu đề **"🏃 Tăng hoạt động thể chất ngay hôm nay"**, nội dung nói **theo phút** và nêu rõ tỉ lệ: *"Mức vận động trung bình của bạn là 10 phút trong 5 lần ghi gần nhất — mới đạt 33% mục tiêu 30 phút. …"*; có nút cam **"Ghi nhận vận động"**. **Không** còn câu *"Số bước chân trung bình của bạn là 10 bước…"*.
  - 5 log `21` phút (70%) → thẻ **LOW** tiêu đề **"🚶 Tăng dần mục tiêu vận động"**, **không** có nút.
  - 5 log `30` phút (100%) → **không** có thẻ vận động nào (bản cũ vẫn bắn HIGH vì `30 < 5000`).
  - Chỉ **2 log** (dù giá trị rất thấp) → **không** có thẻ vận động (chưa đủ dữ liệu để kết luận xu hướng).
- **Ghi chú:** Rule nay chia theo **đơn vị của chính thói quen**: log có đơn vị bước giữ ngưỡng 5000/8000 bước (TC-INS-08/09), log đơn vị khác (phút, km, calo…) so **tỉ lệ `value / targetValueAtTime`** — dưới 50% → HIGH, 50–80% → LOW.

### TC-INS-30 — Unit test JVM cho rule engine sau khi gom theo NGÀY và theo ĐƠN VỊ (phụ lục C #16, #17)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitRuleEngineTest" --tests "*HabitRuleEnginePersonalizedTest" --tests "*HealthDataRepositoryImplTest"`.
- **Kết quả mong đợi:** Cả 3 class pass (**14 + 5 + 2 = 21 test**), trong đó:
  - `HabitRuleEngineTest` (14 test, 4 test mới cho #17): 5 log `10/30` phút → HIGH + message chứa `"10 phút"` và **không** chứa `"bước"`; 5 log `30/30` phút → **không** có gợi ý vận động; 5 log `21/30` phút → LOW; chỉ 2 log → không có gợi ý; log đơn vị `bước` giữ nguyên hành vi 5000/8000.
  - `HabitRuleEnginePersonalizedTest` (5 test): **3 hàng cùng một ngày** → **không** cảnh báo thiếu ngủ (chống hồi quy #16); 3 ngày **cách quãng** → không cảnh báo; 3 ngày **liên tiếp** < 6h → CRITICAL; 7 **ngày** nhịp tim ≥ 90 → MEDIUM.
  - `HealthDataRepositoryImplTest` (2 test): `getRecentHealthHistoryFlow(7)` gọi DAO với đúng `limit = 7` (mockk `verify`) — phụ lục C #16.
- **Ghi chú:** Dữ liệu test dựng theo **ngày** (mốc 12:00 giờ thiết bị) thay vì "N hàng liên tiếp", đúng với cách `health_data_history` ghi 1 hàng mỗi lần sync.

### TC-HAB-30 — Unit test JVM cho validate số lượng & hiển thị lỗi
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitLogInputTest" --tests "*HabitsViewModelTest"`.
- **Kết quả mong đợi:** Toàn bộ pass, trong đó:
  - `HabitLogInputTest` (5 test): `"0"`, `"0.0"`, `"-100"`, `" -0.5 "` → `Invalid("Số lượng phải lớn hơn 0")` (phụ lục C #11); rỗng → *"Vui lòng nhập số lượng"*; chữ/`NaN`/`Infinity` → *"Số lượng phải là số"*; `500`, ` 7.5 ` → `Valid`.
  - `HabitsViewModelTest` (9 test): `logHabit(habit, 0f)` và `logHabit(habit, -100f)` → `error = "Số lượng phải lớn hơn 0"`, `logHabitUseCase` **không** được gọi, không phát sự kiện "Đã ghi nhận"; `dismissError()` → `error` về `null` (thẻ lỗi ở màn Thói quen đóng lại — phụ lục C #10).

---

## 12. TC-HC — Health Connect

### TC-HC-01 — Phát hiện Health Connect khả dụng
- **Ưu tiên:** P0
- **Tiền điều kiện:** Emulator API 34 có Google Play (Health Connect được tích hợp sẵn) **và** đã cấp đủ 4 quyền đọc.
- **Bước:** 1. Mở app → xem màn Home.
- **Kết quả mong đợi:** Không có banner lỗi đỏ, **không** có thẻ "Cấp quyền / Cài / Cập nhật Health Connect"; thẻ Tóm tắt sức khỏe hiển thị số liệu.

### TC-HC-02 — Phát hiện Health Connect không khả dụng
- **Ưu tiên:** P1
- **Tiền điều kiện:** Emulator **không** có Health Connect (ví dụ API 30 AOSP) hoặc đã gỡ ứng dụng Health Connect.
- **Bước:** 1. Mở app → 2. Xem màn Home.
- **Kết quả mong đợi:** Thẻ **"Chưa cài Health Connect"** + nút **"Cài Health Connect"** (mở Play Store); số liệu hiển thị 0 (xem TC-HOME-07). Nếu máy báo cần cập nhật → thẻ **"Health Connect cần được cập nhật"**.

### TC-HC-03 — Đọc số bước chân hôm nay
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã bơm `StepsRecord` **8000 bước** trong ngày; đã cấp quyền đọc Steps.
- **Bước:** 1. Mở app → 2. Xem chỉ số Bước chân ở Home.
- **Kết quả mong đợi:** Hiển thị **"8.0k"**.
- **Kiểm tra biên:** Bơm 4000 bước → **"4.0k"**; 0 bước → **"0.0k"**.

### TC-HC-04 — Đọc thời gian ngủ
- **Ưu tiên:** P0
- **Tiền điều kiện:** `SleepSessionRecord` từ **23:00 hôm qua → 06:30 hôm nay** (7.5 giờ); đã cấp quyền.
- **Bước:** 1. Mở app → 2. Xem chỉ số Ngủ.
- **Kết quả mong đợi:** **"7h 30m"**.

### TC-HC-05 — Ngủ ngoài khung thời gian
- **Ưu tiên:** P2
- **Tiền điều kiện:** `SleepSessionRecord` chỉ trong khoảng **sáng nay 08:00–10:00** (khung truy vấn bắt đầu 18:00 hôm qua nên vẫn nằm trong khung).
- **Bước:** 1. Mở app → 2. Xem chỉ số Ngủ.
- **Kết quả mong đợi:** **"2h 0m"** (mọi phiên ngủ trong khung 18:00 hôm qua → hiện tại đều được tính).

### TC-HC-06 — Đọc nhịp tim gần nhất
- **Ưu tiên:** P1
- **Tiền điều kiện:** `HeartRateRecord` 72 bpm trong 1 giờ gần nhất; đã cấp quyền.
- **Bước:** 1. Mở app → 2. Xem chỉ số Tim.
- **Kết quả mong đợi:** **"72 bpm"**.

### TC-HC-07 — Không có dữ liệu nhịp tim
- **Ưu tiên:** P2
- **Tiền điều kiện:** Không có `HeartRateRecord` nào trong 1 giờ gần nhất.
- **Bước:** 1. Mở app → 2. Xem chỉ số Tim.
- **Kết quả mong đợi:** **"0 bpm"** (không lỗi, không để trống).

### TC-HC-08 — Calories tiêu thụ (Active calories)
- **Ưu tiên:** P2
- **Tiền điều kiện:** `ActiveCaloriesBurnedRecord` ~350 kcal hôm nay; đã cấp quyền.
- **Bước:** 1. Mở app → 2. Kiểm tra giá trị calories.
- **Kết quả mong đợi:** Giá trị được đọc đúng (kcal). **Lưu ý:** màn Home hiện **không** hiển thị calories, chỉ dùng cho `syncHealthData()`/Insights — cần dev bổ sung nếu nghiệm thu yêu cầu hiển thị.

### TC-HC-09 — Ghi dữ liệu vào history (syncHealthData)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp đủ **4** quyền đọc (Steps, Sleep, Heart rate, Active calories).
- **Bước:** 1. Run `periodic_health_sync_worker` bằng Background Task Inspector → 2. Kiểm tra log `HealthDataRepository`.
- **Kết quả mong đợi:** Log dạng **"Background sync completed and saved to Room: 8000 steps today"**; bảng `health_data_history` có thêm 1 hàng với đủ 4 chỉ số + `recordedAt`.

### TC-HC-10 — Sync bị bỏ qua khi thiếu quyền
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thu hồi 1 trong 4 quyền (ví dụ quyền Sleep).
- **Bước:** 1. Run `periodic_health_sync_worker` → 2. Kiểm tra `health_data_history`.
- **Kết quả mong đợi:** Không có hàng mới nào được ghi; worker vẫn trả `Result.success()` (thoát sớm, không lỗi).

### TC-HC-11 — Dọn dữ liệu cũ hơn 30 ngày
- **Ưu tiên:** P2
- **Bước:** 1. Chèn thủ công 1 hàng `health_data_history` với `recordedAt` = 40 ngày trước (qua Database Inspector chỉ áp dụng khi DB chưa mã hóa, hoặc qua script) → 2. Run worker sync.
- **Kết quả mong đợi:** Hàng cũ bị xóa (`deleteOldData(now - 30 ngày)`); các hàng mới còn nguyên.

### TC-HC-12 — Quyền Health Connect bị thiếu → app không crash
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thu hồi toàn bộ quyền Health Connect.
- **Bước:** 1. Mở lần lượt cả 4 tab, đặc biệt Home và Phân tích.
- **Kết quả mong đợi:** Không crash; giá trị hiển thị `0`/rỗng; không có `SecurityException` chưa bắt trong logcat. Màn Home hiện thêm thẻ **"Cấp quyền Health Connect"**.

### TC-HC-13 — Xin quyền ngay trong app (thẻ ở màn Home)
- **Ưu tiên:** P0
- **Tiền điều kiện:** Có Health Connect, app **chưa** được cấp quyền đọc.
- **Bước:**
  1. Mở app → màn Trang chủ → quan sát thẻ ngay dưới "Tóm tắt sức khỏe".
  2. Bấm **"Cấp quyền Health Connect"**.
  3. Bật đủ 4 quyền (Bước chân, Giấc ngủ, Nhịp tim, Calories) trong hộp thoại của Health Connect → quay lại app.
- **Kết quả mong đợi:**
  - Thẻ có tiêu đề **"Cấp quyền Health Connect"**, mô tả nêu rõ 4 loại dữ liệu cần đọc, nút xanh cùng tên.
  - Bấm nút → mở **hộp thoại quyền của Health Connect** (`PermissionController.createRequestPermissionResultContract()`); các quyền hiển thị đúng 4 loại (Steps, Sleep, Heart rate, Active calories burned).
  - Sau khi cấp đủ → App quay lại Home, **thẻ biến mất ngay** và số liệu sức khỏe được nạp lại (không cần khởi động lại app).
  - Không có crash, không có banner lỗi đỏ.

### TC-HC-14 — Từ chối / chỉ cấp một phần quyền
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chưa cấp quyền (như TC-HC-13).
- **Bước:** 1. Bấm **"Cấp quyền Health Connect"** → 2. Chỉ bật **Steps** rồi xác nhận.
- **Kết quả mong đợi:** Thẻ **vẫn hiển thị** (chưa đủ 4 quyền) để người dùng cấp tiếp; app không crash; chỉ số của quyền đã cấp có thể hiển thị, các chỉ số còn lại vẫn `0`.

### TC-HC-15 — Cấp quyền trực tiếp trong app Health Connect rồi quay lại
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chưa cấp quyền cho HealthyAI; app đang mở ở màn Home.
- **Bước:** 1. Mở app **Health Connect** → Apps → HealthyAI → bật đủ 4 quyền → 2. Quay lại HealthyAI (không đóng app).
- **Kết quả mong đợi:** Nhờ `LifecycleEventEffect(ON_RESUME)`, khi Home hiện lại app tự kiểm tra quyền → thẻ nhắc biến mất và số liệu được nạp lại.

### TC-HC-16 — Thẻ nhắc không chặn thao tác
- **Ưu tiên:** P2
- **Tiền điều kiện:** Chưa cấp quyền Health Connect.
- **Bước:** 1. Ở màn Home, cuộn xuống, tạo/ghi nhận thói quen như bình thường.
- **Kết quả mong đợi:** Thẻ chỉ mang tính hướng dẫn (không phải dialog, không chiếm toàn màn hình); danh sách thói quen, ghi nhận và tab khác hoạt động bình thường.

### TC-HC-17 — Unit test JVM cho luồng quyền
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HealthConnect*" --tests "*HomeViewModelTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm: map `getSdkStatus` (3 → AVAILABLE, 2 → UPDATE_REQUIRED, 1/khác → NOT_INSTALLED), use case `GetHealthConnectPermissionStatusUseCase` (đủ/thiếu quyền, lỗi đọc → NOT_INSTALLED), và `HomeViewModel` (thiếu quyền → hiện thẻ không có banner đỏ; chưa cài → không đọc dữ liệu; cấp đủ quyền → ẩn thẻ + nạp lại số liệu).

---

## 13. TC-DB — Room + SQLCipher

> Từ bản sửa lỗi mục 3 **#17**, passphrase mở DB **không còn hard-code**: `di/DatabaseModule.kt`
> lấy khoá từ `data/local/security/DatabasePassphraseProvider.kt` — sinh ngẫu nhiên 32 byte ở lần
> chạy đầu, bọc bằng khoá AES-256/GCM trong **Android Keystore** (`AndroidPassphraseStorage.kt`) rồi
> lưu bản mã ở prefs `healthy_ai_secure_store`. Máy đã cài bản cũ được
> `LegacyDatabaseKeyMigration` rekey một lần sang khoá mới (xem **TC-DB-12**).

### TC-DB-01 — Tạo DB mã hóa ở lần chạy đầu
- **Ưu tiên:** P0
- **Tiền điều kiện:** `pm clear`.
- **Bước:** 1. Mở app → 2. `adb shell run-as com.healthyai.app ls -l databases`.
- **Kết quả mong đợi:** Tồn tại **`healthy_ai_encrypted.db`** (kèm `-wal`/`-shm` khi có giao dịch); không lỗi `UnsatisfiedLinkError` khi `System.loadLibrary("sqlcipher")`. Đồng thời app sinh prefs `shared_prefs/healthy_ai_secure_store.xml` chứa khoá `sqlcipher_passphrase_v1` là **bản mã Base64** (không phải passphrase dạng thô) — kiểm tra ở **TC-DB-11**.

### TC-DB-02 — Xác nhận file DB đã mã hóa
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã tạo ít nhất 1 thói quen tên `Uống nước`.
- **Bước:**
  1. `adb exec-out run-as com.healthyai.app cat databases/healthy_ai_encrypted.db > "$env:TEMP\db.bin"`
  2. Tìm chuỗi header SQLite trong file nhị phân.
- **Kết quả mong đợi:** **Không** tìm thấy `SQLite format 3` (file không phải SQLite thuần); **không** tìm thấy chuỗi `Uống nước` / `habits` dạng plain text (SQLCipher AES-256).

### TC-DB-03 — Dữ liệu thói quen được giữ sau khi khởi động lại app
- **Ưu tiên:** P0
- **Bước:** 1. Tạo 3 thói quen → 2. Thoát hẳn app → 3. Mở lại.
- **Kết quả mong đợi:** Cả 3 thói quen vẫn còn, đúng tên/mục tiêu/đơn vị/danh mục/thời gian/tần suất.

### TC-DB-04 — Dữ liệu giữ sau khi reboot thiết bị
- **Ưu tiên:** P1
- **Bước:** 1. Tạo thói quen + log → 2. `adb reboot` → 3. Mở app.
- **Kết quả mong đợi:** Dữ liệu còn nguyên.

### TC-DB-05 — Ghi log lưu đúng trường `targetValueAtTime`
- **Ưu tiên:** P1
- **Bước:** 1. Thói quen target 2000 → ghi log 500 → 2. Sửa target thành 3000 → 3. Ghi log 500 thứ hai.
- **Kết quả mong đợi:** Log thứ nhất giữ `targetValueAtTime = 2000`, log thứ hai có `targetValueAtTime = 3000` (phục vụ phân tích lịch sử).

### TC-DB-06 — Cờ `isDirty` mặc định
- **Ưu tiên:** P1
- **Bước:** 1. Ghi 1 log → 2. Kiểm tra trường `isDirty` của hàng `habit_logs`.
- **Kết quả mong đợi:** `isDirty = 1` (chưa đồng bộ) — điều kiện cho `SyncWorker`.

### TC-DB-07 — Xóa cứng (delete cascade) khi xóa hàng `habits` trực tiếp
- **Ưu tiên:** P2
- **Bước:** 1. Xóa hàng thói quen trực tiếp trong DB (không qua UI) → 2. Kiểm tra `habit_logs`.
- **Kết quả mong đợi:** Log tương ứng bị xóa theo (`onDelete = CASCADE`). Điều này **khác** với xóa qua UI (soft delete, xem TC-ADD-11).

### TC-DB-08 — Nâng version schema có `Migration` → **không** mất dữ liệu (phụ lục C #14)
- **Ưu tiên:** P0
- **Bước:** 1. Cài bản có `AppDatabase version = 3` và tạo dữ liệu → 2. Cài bản `version = 4` **có** `MIGRATION_3_4` trong `DatabaseMigrations.ALL`.
- **Kết quả mong đợi:** Room chạy `MIGRATION_3_4`; thói quen/nhật ký **giữ nguyên**; app không crash. Nếu ai đó tăng version mà **quên** thêm migration → `DatabaseMigrationsTest` fail ngay trên CI/máy dev (chặn trước khi phát hành).
- **Ghi chú:** `fallbackToDestructiveMigration()` đã bị **bỏ** khỏi `di/DatabaseModule.kt`; thiếu migration giờ là lỗi **ồn ào** (crash) chứ không âm thầm xoá dữ liệu. Chi tiết nâng cấp từ v1/v2 ở **TC-DB-15**.

### TC-DB-09 — Mở app khi DB bị hỏng
- **Ưu tiên:** P2
- **Bước:** 1. Ghi đè nội dung rác vào `databases/healthy_ai_encrypted.db` (build debug) → 2. Mở app.
- **Kết quả mong đợi:** Ghi nhận hành vi thực tế (crash `SQLiteException` hay fallback). Nếu crash → cần bọc xử lý.

### TC-DB-10 — Đọc/ghi từ nhiều luồng không lỗi
- **Ưu tiên:** P2
- **Bước:** 1. Bấm ghi log liên tục 10 lần ở 5 thói quen khác nhau thật nhanh.
- **Kết quả mong đợi:** Không `IllegalStateException`/`SQLiteDatabaseLockedException`; tổng log đúng bằng số lần bấm.

### TC-DB-11 — Passphrase lấy từ Android Keystore, không còn hard-code (mục 3 #17)
- **Ưu tiên:** P0
- **Tiền điều kiện:** `pm clear com.healthyai.app`.
- **Bước:**
  1. Mở app → **Bỏ qua** ở màn đăng nhập → tạo 1 thói quen → thoát hẳn app.
  2. `adb shell run-as com.healthyai.app cat shared_prefs/healthy_ai_secure_store.xml`
  3. Mở lại app → kiểm tra thói quen vẫn còn.
- **Kết quả mong đợi:**
  - Prefs `healthy_ai_secure_store.xml` tồn tại, chứa `<string name="sqlcipher_passphrase_v1">` với giá trị **Base64** của **60 byte** (`IV 12 byte + ciphertext 32 byte + tag 16 byte`) → **~80 ký tự** Base64 — **không** đọc ra được passphrase, không có khoá dạng thô trong bất kỳ file nào của app.
  - Khoá dùng để giải mã bản mã là khoá AES-256 alias `healthy_ai_sqlcipher_key_v1` nằm trong **Android Keystore** (`AndroidKeystorePassphraseCipher.KEY_ALIAS`) — Android Studio → **App Inspection**/Device Explorer không có file nào chứa khoá này.
  - Bước 3: app đọc **đúng** passphrase đã lưu (dữ liệu còn nguyên) ⇒ không sinh khoá mới mỗi lần mở ⇒ hằng số trong APK không còn là khoá mở DB ⇒ file DB bị lấy trộm (root/backup) **không** mở được bằng bất kỳ giá trị nào trích từ APK.
  - Ghi nhận: hằng số `LegacyDatabaseKeyMigration.LEGACY_PASSPHRASE` **vẫn** nằm trong APK, nhưng **chỉ** dùng để rekey DB của bản cũ đúng một lần (TC-DB-12) — có thể xoá hẳn khi không còn máy nào chạy bản cũ.

### TC-DB-12 — Nâng cấp từ bản cũ: rekey một lần, giữ nguyên dữ liệu
- **Ưu tiên:** P0
- **Tiền điều kiện:** Có bản build **trước** bản sửa lỗi #17 (passphrase hard-code), ví dụ commit `91c1b1b`.
- **Bước:**
  1. `adb install -r` bản APK **cũ** → `pm clear com.healthyai.app` → mở app → **Bỏ qua** → tạo **3 thói quen** và ghi vài log trong hôm nay.
  2. `adb shell run-as com.healthyai.app ls shared_prefs` → xác nhận **chưa** có `healthy_ai_secure_store.xml` (bản cũ không sinh khoá).
  3. Cài **đè** bản APK mới, **không** `pm clear`: `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
  4. Mở app (chạy song song `adb logcat -s DatabaseKeyMigration`) → mở tab **Thói quen** và **Trang chủ**.
- **Kết quả mong đợi:**
  - App vào bình thường, **không** crash `SQLiteException: file is not a database`, không bị đẩy về màn đăng nhập kèm dữ liệu rỗng.
  - **Toàn bộ** 3 thói quen và log đã ghi ở bước 1 **còn nguyên** (đúng tên/mục tiêu/đơn vị/danh mục; tiến độ hôm nay vẫn tính đủ log cũ).
  - logcat có dòng `Đã đổi khoá 'healthy_ai_encrypted.db' từ passphrase hard-code sang khoá Android Keystore` (tag `DatabaseKeyMigration`); prefs `healthy_ai_secure_store.xml` xuất hiện sau bước 4.
  - Thoát hẳn rồi mở lại: dữ liệu vẫn nguyên và **không** chạy rekey lần hai (không có dòng log trên).

### TC-DB-13 — Không mở được DB cũ (khoá Keystore mất / file hỏng) → tạo DB mới, không crash
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã có dữ liệu (vài thói quen + log) từ bản đang chạy hiện tại.
- **Bước (chọn 1 trong 2 cách, đều là build debug nên `run-as` dùng được):**
  1. **Cách A — mất khoá Keystore:** Android Studio → **View → Tool Windows → Device Explorer** → `data/data/com.healthyai.app/shared_prefs/healthy_ai_secure_store.xml` → **Save As** về máy → sửa giá trị `sqlcipher_passphrase_v1` thành bản mã rác `QUJD` → **Upload** đè lại file.
  2. **Cách B — file DB hỏng:** ghi đè nội dung rác vào `databases/healthy_ai_encrypted.db` (giống **TC-DB-09**).
  3. Xoá app khỏi recents rồi mở lại app, chạy `adb logcat -s DatabaseKeyMigration AndroidRuntime:E`.
- **Kết quả mong đợi:**
  - Cả hai cách: app **không crash**, không có vòng lặp khởi động lại; màn Thói quen/Trang chủ hiển thị **rỗng** và tạo/ghi nhận thói quen mới hoạt động bình thường; mở lại app lần nữa vẫn đọc được dữ liệu vừa tạo.
  - Cách A: tầng sinh passphrase phát hiện không giải mã được bản mã → sinh khoá mới (`newlyCreated = true`) → không rekey được nên xoá DB không mở được.
  - Cách B: logcat có `Không rekey được DB cũ → xoá để tạo DB mới (mất dữ liệu cục bộ)`.
  - Ghi nhận đây là **mất dữ liệu cục bộ có kiểm soát** khi khoá đã mất thật (không thể khôi phục về mặt kỹ thuật) — nhưng app vẫn dùng được.

### TC-DB-14 — Unit test JVM cho nguồn passphrase mới
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*DatabasePassphraseProviderTest"` (repo chưa commit wrapper — dùng Gradle đã cài, xem mục 2.2; JDK 21).
- **Kết quả mong đợi:** **6 test** trong `app/src/test/.../data/local/security/DatabasePassphraseProviderTest.kt` pass (đã chạy lại trên Gradle 8.13 + JDK 21: `tests="6" skipped="0" failures="0" errors="0"`), chốt các tính chất:
  - máy mới → sinh passphrase **ngẫu nhiên đúng 32 byte** và lưu lại **đúng 1 lần**;
  - mở lại app → đọc đúng passphrase đã lưu, `newlyCreated = false`, **không** ghi đè (DB cũ không bị vô hiệu);
  - hai máy khác nhau → hai passphrase khác nhau (không còn hằng số dùng chung như bản hard-code);
  - vault có bản mã nhưng khoá Keystore mất → sinh khoá mới + `newlyCreated = true` **thay vì crash**, và bản mã cũ bị ghi đè;
  - passphrase **không** lộ qua `toString()` (an toàn cho log/crash report);
  - `DatabasePassphrase` so sánh/băm theo **nội dung byte** chứ không theo tham chiếu (khoá đọc lại luôn `equals` khoá đã lưu).
- **Ghi chú:** JVM không có Android Keystore nên test dùng `PassphraseVault`/`PassphraseCipher` giả; luồng mã hoá thật (AES-256/GCM) phải kiểm tra trên thiết bị/emulator theo **TC-DB-11**.

### TC-DB-15 — Nâng cấp từ bản cũ (schema **v1/v2**) giữ nguyên thói quen + nhật ký (phụ lục C #14)
- **Ưu tiên:** P0
- **Tiền điều kiện:** Emulator/thiết bị **đã cài bản cũ** có `AppDatabase version = 1` hoặc `2` và đang có dữ liệu thật (≥ 2 thói quen, ≥ 1 log mỗi thói quen).
- **Bước (chọn 1 cách để tạo DB v1/v2 trước):**
  1. **Cách A — APK của commit cũ:** build & cài APK debug từ commit `fa97245` (`version = 2`) hoặc `e6f25b4` (`version = 1`), rồi tạo thói quen + ghi log.
  2. **Cách B — hạ version tạm thời:** sửa `AppDatabase` xuống `version = 2` (hoặc `1`) → `assembleDebug` + `installDebug` → tạo dữ liệu → trả `version` về `3` → `adb install -r app\build\outputs\apk\debug\app-debug.apk` (cài **đè**, **không** `pm clear`).
  3. Mở app → tab **Trang chủ** và **Thói quen**; chạy `adb logcat -s AndroidRuntime:E RoomDatabase`.
  4. Kiểm tra version thực tế của DB:
     `adb exec-out run-as com.healthyai.app cat databases/healthy_ai_encrypted.db > "$env:TEMP\db.bin"` (DB đã mã hoá nên **không** đọc trực tiếp bằng `sqlite3`; thay vào đó xem `user_version` qua Room/Gradle ở bước 5).
  5. `.\gradlew test` — `DatabaseMigrationsTest` chốt dãy migration khớp schema đang dùng.
- **Kết quả mong đợi:**
  - **Toàn bộ** thói quen + nhật ký của bản cũ **vẫn còn**, tiến độ hôm nay tính đúng; app **không** crash, logcat không có `IllegalStateException: Migration didn't properly handle` hay `file is not a database`.
  - Thói quen cũ hiển thị buổi **"Cả ngày"** và tần suất **"Hàng ngày"** (giá trị mặc định của `MIGRATION_1_2`).
  - Log cũ có `targetValueAtTime` **bằng mục tiêu của chính thói quen đó** (backfill bằng `UPDATE … SELECT`), nên **Chỉ số sinh lực** không bị tụt về 0 vì dữ liệu cũ.
  - Màn Phân tích/Home đọc được `health_data_history` (bảng mới của `MIGRATION_2_3`).
- **Ghi chú:** Cả 2 migration chỉ dùng `ALTER TABLE … ADD COLUMN … DEFAULT` / `CREATE TABLE` — **không** DROP/RENAME bảng, nên không kích hoạt `ON DELETE CASCADE` của `habit_logs` (không mất nhật ký). Đã kiểm chứng offline: dựng DB v1 bằng SQLite thật → chạy đúng 2 migration → so `PRAGMA table_info`/`index_list`/`foreign_key_list` với `app/schemas/…/3.json` → **khớp hoàn toàn** (cột, kiểu, NOT NULL, PK, index `index_habit_logs_habitId`/`loggedAt`, FK `habits ON DELETE CASCADE`).

### TC-DB-16 — Passphrase lưu **đồng bộ** trước khi tạo DB (phụ lục C #29)
- **Ưu tiên:** P2
- **Tiền điều kiện:** `pm clear com.healthyai.app`.
- **Bước:**
  1. Mở app → **Bỏ qua** ở màn đăng nhập → tạo 1 thói quen.
  2. Thoát app **ngay** bằng `adb shell am force-stop com.healthyai.app` (mô phỏng tiến trình bị kill sát thời điểm DB được tạo).
  3. `adb shell run-as com.healthyai.app cat shared_prefs/healthy_ai_secure_store.xml`.
  4. Mở lại app → kiểm tra thói quen vừa tạo.
- **Kết quả mong đợi:**
  - Prefs **có** giá trị `sqlcipher_passphrase_v1` hợp lệ (Base64 ~80 ký tự) **ngay sau** lần chạy đầu — `SharedPreferencesPassphraseVault.write()` dùng `commit()` (ghi đồng bộ) thay vì `apply()`.
  - Mở lại app: thói quen vừa tạo **vẫn còn**, **không** có log `DatabaseKeyMigration`/mất dữ liệu. Đây là kịch bản trước đây có thể mất dữ liệu: passphrase mới sinh còn nằm trong hàng đợi `apply()` → tiến trình chết → lần sau không tìm thấy passphrase trong prefs → sinh khoá khác → DB cũ không mở được → bị xoá.
- **Ghi chú:** Khe thời gian này rất hẹp và khó tái hiện có chủ đích; test này chủ yếu xác nhận **hành vi ghi đồng bộ** và không có hồi quy (dữ liệu vẫn mở được sau khi force-stop sớm). Chi tiết phát hiện ở **phụ lục C #29**.

---

## 14. TC-WM — WorkManager & thông báo

### TC-WM-01 — `HealthSyncWorker` được lên lịch 6 giờ
- **Ưu tiên:** P1
- **Bước:** 1. Mở app → 2. App Inspection → Background Task Inspector.
- **Kết quả mong đợi:** Worker `periodic_health_sync_worker`, tag `health_sync`, interval **6 giờ**, constraint: không cần mạng, **pin không thấp**, không cần idle, backoff EXPONENTIAL 30 phút.

### TC-WM-02 — `SyncWorker` được lên lịch 6 giờ, cần mạng
- **Ưu tiên:** P1
- **Bước:** 1. App Inspection → chọn `periodic_sync_worker` → xem chi tiết.
- **Kết quả mong đợi:** Interval **6 giờ**, constraint **NetworkType.CONNECTED**, pin không thấp, backoff EXPONENTIAL 30 phút. **Không** có constraint `DeviceIdle` (nếu có, app đã crash ngay khi mở — xem TC-INIT-01).

### TC-WM-03 — `ReminderWorker` được lên lịch 24 giờ
- **Ưu tiên:** P1
- **Bước:** 1. App Inspection → chọn `daily_reminder_worker`.
- **Kết quả mong đợi:** Interval **24 giờ**, constraint pin không thấp.

### TC-WM-04 — Chạy tay worker nhắc nhở → có thông báo
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thiết bị **Android 12 trở xuống** (không cần `POST_NOTIFICATIONS`), hoặc đã cấp quyền thông báo (qua thẻ trên Home — **TC-WM-13** — hoặc trong Settings).
- **Bước:** 1. Run `daily_reminder_worker` → 2. Kéo thanh thông báo xuống.
- **Kết quả mong đợi:**
  - Thông báo tiêu đề **"HealthyAI 💪 Nhắc nhở thói quen"**, nội dung **"Bạn đã cập nhật thói quen hôm nay chưa? Chỉ cần vài giây thôi!"**.
  - Kênh thông báo **"Nhắc nhở thói quen sức khỏe"** được tạo trong Settings.
  - Bấm thông báo → thông báo tự biến mất (autoCancel).
- **Ghi chú:** Notification ID = `1001`; icon dùng `android.R.drawable.ic_dialog_info`.

### TC-WM-05 — Thông báo trên Android 13+ khi chưa cấp quyền
- **Ưu tiên:** P1
- **Tiền điều kiện:** Android 13+ (API 33/34), app **chưa** được cấp `POST_NOTIFICATIONS`.
- **Bước:** 1. Run `daily_reminder_worker` → 2. Quan sát thanh thông báo.
- **Kết quả mong đợi:** **Không có thông báo nào hiện** (`SecurityException` được bắt và bỏ qua); app **không crash**; worker trả `success`.
- **Ghi chú:** Màn Home hiện thẻ **"Bật nhắc nhở hàng ngày"** để người dùng tự cấp quyền (**TC-WM-13**) — không còn phải vào Settings thủ công như trước (mục 3, #8).

### TC-WM-06 — Thông báo sau khi cấp quyền
- **Ưu tiên:** P1
- **Bước:** 1. Cấp quyền thông báo (bấm **"Bật thông báo"** ở thẻ trên Home — **TC-WM-13** — hoặc Settings → Apps → HealthyAI → Notifications → bật) → 2. Run `daily_reminder_worker`.
- **Kết quả mong đợi:** Thông báo hiển thị như TC-WM-04; quay lại màn Home thì thẻ nhắc **đã biến mất**.

### TC-WM-07 — `HealthSyncWorker` ghi dữ liệu vào Room
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đủ 4 quyền Health Connect + có dữ liệu giả.
- **Bước:** 1. Run `periodic_health_sync_worker` → 2. Xem log `HealthSyncWorker`.
- **Kết quả mong đợi:** Log **"Periodic sync successful"**; `health_data_history` có hàng mới; trên tab Phân tích có thể xuất hiện gợi ý từ dữ liệu cứng.

### TC-WM-08 — `SyncWorker` khi chưa đăng nhập
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chưa đăng nhập (đang ở **chế độ khách**).
- **Bước:** 1. Run `periodic_sync_worker` → 2. Xem log `SyncWorker` và trạng thái worker.
- **Kết quả mong đợi:** Worker trả **`success` ngay lập tức** (không retry, không `failure`) vì `AuthRepository.currentUserId == null`. Log có dòng **"Sync skipped: chưa đăng nhập (chế độ khách)"**, không có "Sync completed".
- **Ghi chú:** Trước đây worker trả `failure` ngay khi thiếu phiên đăng nhập → job fail vĩnh viễn và không bao giờ đồng bộ lại; nay chỉ bỏ qua để job chu kỳ 6 giờ tiếp theo tự chạy lại sau khi người dùng đăng nhập (đã sửa ở mục 3, #6 và phụ lục C #24).

### TC-WM-09 — `SyncWorker` không chạy khi offline
- **Ưu tiên:** P1
- **Bước:** 1. Bật Airplane mode → 2. Bấm **Run** `periodic_sync_worker` trong Background Task Inspector.
- **Kết quả mong đợi:** Worker không thực thi `doWork` (bị hoãn do constraint `NetworkType.CONNECTED`), hoặc chạy nhưng thất bại do mạng; app không crash.

### TC-WM-10 — Không retry vô hạn khi lỗi
- **Ưu tiên:** P1
- **Bước:** 1. Gây lỗi liên tục cho worker (ví dụ thiếu quyền/mạng) → 2. Quan sát `runAttemptCount`.
- **Kết quả mong đợi:** `HealthSyncWorker` retry tối đa **3 lần** rồi trả `failure`; `SyncWorker` retry tối đa **3 lần** rồi `failure`; không lặp vô hạn gây tốn pin.

### TC-WM-11 — Job không nhân bản sau nhiều lần mở app
- **Ưu tiên:** P2
- **Bước:** 1. Mở app 5 lần liên tiếp → 2. Kiểm tra Background Task Inspector.
- **Kết quả mong đợi:** Mỗi worker vẫn đúng 1 instance (`ExistingPeriodicWorkPolicy.KEEP`).

### TC-WM-12 — Job tồn tại sau reboot
- **Ưu tiên:** P2
- **Bước:** 1. `adb reboot` → 2. Kiểm tra `dumpsys jobscheduler`.
- **Kết quả mong đợi:** 3 job vẫn tồn tại (WorkManager dùng `RECEIVE_BOOT_COMPLETED`).

### TC-WM-13 — Xin quyền thông báo ngay trong app (thẻ ở màn Home)
- **Ưu tiên:** P0
- **Tiền điều kiện:** Android 13+ (API 33/34), app **chưa** được cấp `POST_NOTIFICATIONS`.
- **Bước:**
  1. Mở app → màn Trang chủ → quan sát thẻ **"Bật nhắc nhở hàng ngày"**.
  2. Bấm **"Bật thông báo"**.
  3. Trong hộp thoại hệ thống bấm **Cho phép**.
  4. Run `daily_reminder_worker` (Background Task Inspector).
- **Kết quả mong đợi:**
  - Thẻ có icon chuông, tiêu đề **"Bật nhắc nhở hàng ngày"**, mô tả nêu lý do cần quyền, nút xanh **"Bật thông báo"**.
  - Bấm nút → hiện hộp thoại xin quyền `POST_NOTIFICATIONS` của hệ thống.
  - Sau khi cho phép → quay lại Home, **thẻ biến mất ngay** (không cần khởi động lại app).
  - `daily_reminder_worker` sinh thông báo như TC-WM-04.

### TC-WM-14 — Từ chối quyền thông báo
- **Ưu tiên:** P1
- **Tiền điều kiện:** Như TC-WM-13.
- **Bước:** 1. Bấm **"Bật thông báo"** → 2. Trong hộp thoại chọn **Không cho phép** → 3. Quan sát Home.
- **Kết quả mong đợi:** App không crash; thẻ **vẫn hiển thị** (bản này không có nút "Để sau" — người dùng chủ động muốn bật lại thì bấm tiếp); worker vẫn trả `success` nhưng không có thông báo (TC-WM-05).

### TC-WM-15 — Người dùng đã tắt thông báo trong Settings
- **Ưu tiên:** P1
- **Tiền điều kiện:** Quyền `POST_NOTIFICATIONS` **đã cấp**, nhưng tắt công tắc **Allow notifications** của app trong Settings (Android 13+), hoặc tắt thông báo của app trên Android ≤ 12.
- **Bước:** 1. Mở màn Trang chủ → 2. Bấm **"Mở cài đặt thông báo"** → 3. Bật lại thông báo → 4. Quay lại app.
- **Kết quả mong đợi:**
  - Thẻ hiển thị tiêu đề **"Thông báo đang bị tắt"** + nút **"Mở cài đặt thông báo"** (không dùng hộp thoại xin quyền, vì quyền đã được cấp).
  - Nút mở đúng màn **Notification settings của HealthyAI**.
  - Bật lại xong quay về Home → thẻ biến mất (nhờ kiểm tra lại khi màn hình resume).

### TC-WM-16 — Unit test JVM cho luồng quyền thông báo
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HomeViewModelTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm: thông báo bị chặn → hiện thẻ; bị tắt ở cấp app → thẻ hiện nhưng không xin lại quyền; đã bật → ẩn thẻ; `onNotificationPermissionResult()` và `refreshSystemPermissions()` → ẩn thẻ khi trạng thái đổi.
- **Ghi chú:** `NotificationPermissionRepositoryImpl` đọc `NotificationManagerCompat.areNotificationsEnabled()` nên chỉ test được trên thiết bị/emulator (không có trong unit test JVM).

---

## 15. TC-WG — Widget màn hình chính (Glance)

### TC-WG-01 — Thêm widget khi đã có thói quen
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã có ít nhất 1 thói quen đang hoạt động.
- **Bước:** 1. Nhấn giữ màn hình chính → **Widgets** → tìm **"Tiến độ thói quen"** → 2. Kéo ra màn hình chính.
- **Kết quả mong đợi:** Widget hiển thị:
  - Tiêu đề **"Tiến độ hôm nay"** + dòng **"x/y thói quen đạt mục tiêu"** (khớp dữ liệu đã ghi hôm nay).
  - Thanh tiến độ **thật**: tỉ lệ = số thói quen **đạt mục tiêu** / tổng số thói quen đang hoạt động (trước đây là mức **tĩnh ~40%** vẽ bằng `Row` chia cứng weight).
  - Danh sách tối đa **3** thói quen: `✓` (đã đạt mục tiêu) hoặc `○` (chưa đạt) + tên thói quen + `đã ghi / mục tiêu` (ví dụ `1200 / 2000 ml`, đọc từ `habit_logs` hôm nay).
  - Nếu có **nhiều hơn 3** thói quen thì dòng cuối là **"+N thói quen khác"**.
  - Thứ tự danh sách xem **TC-WG-03**.

### TC-WG-02 — Thêm widget khi chưa có thói quen
- **Ưu tiên:** P1
- **Tiền điều kiện:** `pm clear` (không có thói quen).
- **Bước:** 1. Thêm widget ra màn hình chính.
- **Kết quả mong đợi:** Hiển thị **"Healthy AI"** + **"Chưa có thói quen"**, không crash.

### TC-WG-03 — Thứ tự thói quen trên widget (buổi hiện tại lên đầu)
- **Ưu tiên:** P1
- **Bước:** 1. Tạo 3 thói quen, mỗi thói quen chọn mục **"Thời gian thực hiện"** khác nhau ở màn Thêm thói quen: A "Cả ngày" (tạo trước), B "Buổi sáng" (tạo sau A), C "Buổi tối" (tạo mới nhất) → 2. Thêm widget ra màn hình chính → 3. Kiểm tra lần lượt ở từng khung giờ (đổi giờ máy/emulator nếu cần): 08:00, 14:00, 21:00.
- **Kết quả mong đợi (thứ tự do `TodayHabitProgress.orderForWidget()`):**
  1. thói quen đúng **buổi hiện tại** — `Buổi sáng` 05:00–11:59, `Buổi chiều` 12:00–17:59, `Buổi tối` 18:00–04:59;
  2. rồi thói quen **Cả ngày**;
  3. rồi các thói quen còn lại.
  Trong mỗi nhóm: thói quen **mới tạo nhất** trước (`createdAt` giảm dần).
- **Ví dụ với dữ liệu ở bước 1:** 08:00 → **B, A, C**; 14:00 → **A, C, B** (không có thói quen "Buổi chiều" nên "Cả ngày" lên đầu, phần còn lại theo `createdAt`); 21:00 → **C, A, B**.
- **Ghi chú:** Widget **không còn chọn 1 thói quen** — nó hiển thị **cả danh sách hôm nay** (tối đa 3 dòng, TC-WG-01), nên buổi chỉ quyết định **thứ tự**; thứ tự này vẫn quan trọng vì các dòng ngoài 3 vị trí đầu bị gộp thành "+N thói quen khác". Yêu cầu "quan trọng nhất" vẫn không triển khai được vì `Habit` không có trường độ ưu tiên.

### TC-WG-04 — Thay đổi dữ liệu → widget tự cập nhật
- **Ưu tiên:** P1
- **Bước:** 1. Ghi nhận nội dung widget (dòng `x/y` + danh sách) → 2. **Ghi nhận** một thói quen (nút **Ghi**) → 3. Tạo thêm 1 thói quen mới → 4. Quay về màn hình chính, quan sát widget (không gỡ/thêm lại widget).
- **Kết quả mong đợi:** Widget **tự vẽ lại ngay** sau mỗi thao tác (không cần gỡ/thêm lại widget hay khởi động lại launcher):
  - sau khi **ghi nhận** → dòng đó tăng `đã ghi / mục tiêu`; khi đạt mục tiêu thì `○` → `✓` và `x` trong `x/y` tăng, thanh tiến độ dài ra;
  - sau khi **tạo** thói quen → `y` trong `x/y` tăng; thói quen mới xuất hiện trong danh sách nếu lọt vào 3 dòng đầu (theo thứ tự TC-WG-03), nếu không thì chỉ thấy `+N thói quen khác` tăng;
  - sau khi **sửa** → tên/mục tiêu trên dòng đó đổi ngay;
  - sau khi **xóa** → dòng đó biến mất và `y` giảm (TC-WG-06).
- **Cơ chế (đã sửa — mục 3 #15):** `HabitRepositoryImpl` gọi `habitWidgetRefresher.refresh()` sau mỗi lần ghi dữ liệu (`insertHabit` dùng chung cho **tạo mới** và **cập nhật**, `logHabit`, `deleteHabit`) → `HabitWidgetRefresherImpl` chạy `HabitWidget().updateAll(context)` → `provideGlance()` đọc lại `getAllActiveHabits()` + `getHabitLogsForPeriod(0)` từ Room rồi dựng lại `TodayHabitProgress`. Lỗi khi vẽ widget được nuốt trong `HabitWidgetRefresherImpl` (log `w` tag `HabitWidgetRefresher`) nên **không** làm hỏng luồng lưu dữ liệu.
- **Ghi chú:** `habit_widget_info.xml` đặt `updatePeriodMillis="1800000"` (30 phút — mức nhỏ nhất Android chấp nhận) để **qua mốc buổi** widget tự vẽ lại theo thứ tự mới dù không mở app (TC-WG-11); ngoài ra vẫn cập nhật **ngay theo sự kiện** khi dữ liệu thói quen đổi.

### TC-WG-05 — Bấm widget mở app
- **Ưu tiên:** P1
- **Bước:** 1. Bấm vào widget.
- **Kết quả mong đợi:** Mở `MainActivity` (màn Trang chủ); nếu app đã ở background thì đưa lên foreground, không tạo instance mới trùng lặp.

### TC-WG-06 — Xóa thói quen đang hiển thị trên widget
- **Ưu tiên:** P2
- **Tiền điều kiện:** Widget đang hiển thị thói quen C.
- **Bước:** 1. Xóa thói quen C trong app (xác nhận ở dialog) → 2. Quan sát widget **ngay**, không gỡ/thêm lại widget.
- **Kết quả mong đợi:** Widget **tự cập nhật ngay**: dòng của thói quen C biến mất, `y` trong `x/y` giảm, thanh tiến độ tính lại theo danh sách mới; nếu xóa hết thói quen đang hoạt động → trạng thái **"Healthy AI / Chưa có thói quen"**.

### TC-WG-07 — Kích thước & resize widget
- **Ưu tiên:** P2
- **Bước:** 1. Thêm widget ra màn hình chính → xác nhận **khung mặc định** hiển thị đủ nội dung → 2. Nhấn giữ widget → resize nhỏ nhất và lớn nhất.
- **Kết quả mong đợi:** Khung mặc định khai báo `minWidth="150dp"` / `minHeight="140dp"` — đủ cho tiêu đề + dòng `x/y` + thanh tiến độ + 3 dòng thói quen — và cho phép resize ngang & dọc (`resizeMode="horizontal|vertical"`), nội dung ăn theo khung.
- **Ghi chú:** Widget màn hình chính **không cuộn được**, nên khi resize **nhỏ hơn** khung mặc định thì vài dòng cuối có thể bị cắt — chấp nhận được vì danh sách đã giới hạn **3 dòng** + dòng `+N thói quen khác` (TC-WG-01).

### TC-WG-08 — Widget sau khi reboot
- **Ưu tiên:** P2
- **Bước:** 1. `adb reboot`.
- **Kết quả mong đợi:** Widget vẫn hiển thị (không bị mất/trắng), dữ liệu đọc lại từ Room.

### TC-WG-09 — Unit test JVM: repository vẽ lại widget khi dữ liệu đổi
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitRepositoryImplTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm 3 test mới chốt hợp đồng "dữ liệu đổi → vẽ lại widget": `insertHabit refreshes the home screen widget`, `logHabit refreshes the home screen widget`, và `deleteHabit soft deletes through the dao` (đã bổ sung `coVerify { widgetRefresher.refresh() }`).
- **Ghi chú:** Test dùng mockk cho `HabitWidgetRefresher` nên chỉ khẳng định repository **có gọi** `refresh()`; không kiểm chứng việc vẽ Glance thật (`updateAll()` cần `AppWidgetManager` + launcher) — phần đó chỉ test được trên thiết bị/emulator (TC-WG-01…08, TC-WG-11, TC-WG-12).

### TC-WG-10 — Unit test JVM: tiến độ ngày + thứ tự danh sách
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*TodayHabitProgressTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass (7 test), gồm:
  - bảng giờ → buổi và các mốc biên `04:59`/`05:00`, `11:59`/`12:00`, `17:59`/`18:00`, `23:59`/`00:00`;
  - **thứ tự**: thói quen của buổi hiện tại lên đầu (kể cả khi thói quen khác mới tạo hơn), rồi "Cả ngày", phần còn lại; trong mỗi nhóm **mới tạo nhất trước**;
  - **tiến độ**: cộng nhiều log trong ngày của cùng một thói quen (`1200 + 800 = 2000/2000` → đạt mục tiêu); thói quen chưa ghi → `0f`;
  - `completedCount` / `totalCount` / `ratio` (1/3 ≈ 0.333; tất cả đạt → `1f`);
  - log của thói quen **đã xóa mềm** (không còn hoạt động) bị bỏ qua;
  - danh sách rỗng → không dòng nào, `ratio = 0f` (widget hiện "Chưa có thói quen").
- **Ghi chú:** `TodayHabitProgress` là hàm **thuần** (`now: LocalTime` truyền vào) nên test được trên JVM; phần vẽ Glance của `HabitWidget.provideGlance()` chỉ kiểm chứng được trên thiết bị/emulator (TC-WG-01…08, TC-WG-11, TC-WG-12).

### TC-WG-11 — Thứ tự widget đổi theo buổi, không cần mở app
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có 2 thói quen: B "Buổi sáng" và C "Buổi tối" (**không** có thói quen "Cả ngày"); widget đã được thêm ra màn hình chính.
- **Bước:** 1. Đặt giờ máy **08:00** → xác nhận **dòng đầu tiên** trong danh sách là **B** → 2. **Không mở app**, đổi giờ máy sang **21:00** → 3. Ép widget vẽ lại để kiểm tra chu kỳ định kỳ: `adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n com.healthyai.app/.presentation.widget.HabitWidgetReceiver` (hoặc chờ launcher vẽ lại theo `updatePeriodMillis`).
- **Kết quả mong đợi:** Danh sách **đổi thứ tự** (21:00 → **C** lên đầu) mà **không** cần mở app / không cần thao tác dữ liệu nào — do `habit_widget_info.xml` đặt `updatePeriodMillis="1800000"` (30 phút, mức nhỏ nhất Android chấp nhận).
- **Ghi chú:** Android chỉ đảm bảo "không sớm hơn 30 phút" và có thể gộp/hoãn khi máy ngủ sâu (Doze), nên sai lệch vài chục phút là bình thường; bước 3 dùng broadcast để kiểm tra nhanh thay vì chờ thật.

### TC-WG-12 — Thanh tiến độ & danh sách khớp dữ liệu đã ghi
- **Ưu tiên:** P1
- **Tiền điều kiện:** 2 thói quen: "Uống nước / mục tiêu 2000 ml" (danh mục Uống nước) và "Chạy bộ / mục tiêu 30 phút" (danh mục Vận động); **chưa ghi log nào hôm nay**.
- **Bước:** 1. Xem widget → 2. Ghi **1200 ml** cho "Uống nước" → xem widget → 3. Ghi thêm **800 ml** → xem widget → 4. Ghi **10 phút** cho "Chạy bộ" → xem widget.
- **Kết quả mong đợi:**
  1. `0/2 thói quen đạt mục tiêu`, thanh tiến độ **rỗng**; 2 dòng đều `○`: "Uống nước" **`0.0L / 2.0L`**, "Chạy bộ" **`0 / 30 phút`**.
  2. `0/2` (chưa đạt); dòng "Uống nước" đổi thành **`1.2L / 2.0L`**, vẫn `○`.
  3. `1/2`; dòng "Uống nước" chuyển **`✓`**; thanh tiến độ dài đúng **50%**.
  4. `1/2`; dòng "Chạy bộ" hiện **`10 / 30 phút`** (vẫn `○`) → thanh tiến độ **không đổi** (vì chưa thói quen nào mới đạt mục tiêu).
- **Ghi chú:** Số hiển thị dùng chung `formatHabitProgress` với màn Thói quen (danh mục Uống nước quy đổi sang lít: `1.2L / 2.0L`), nên **đối chiếu trực tiếp** được với thẻ thói quen trong app; quy tắc "đạt mục tiêu" giống hệt `HabitsViewModel` (tổng log trong ngày ≥ `targetValue`).

---

## 16. TC-FB — Firebase Firestore sync (tùy chọn)

Chỉ thực hiện khi đã cấu hình `google-services.json` và (nếu cần) tạo user Firebase Auth.

### TC-FB-01 — App khởi động khi có `google-services.json`
- **Ưu tiên:** P1
- **Tiền điều kiện:** `app/google-services.json` đúng package `com.healthyai.app`.
- **Bước:** 1. `pm clear` → 2. Mở app → 3. Xem logcat.
- **Kết quả mong đợi:** Không có lỗi `FirebaseApp initialization unsuccessful` / `Default FirebaseApp is not initialized`.

### TC-FB-02 — Trạng thái chưa đăng nhập
- **Ưu tiên:** P1
- **Bước:** 1. Run `periodic_sync_worker` → 2. Kiểm tra Firestore.
- **Kết quả mong đợi:** Không có document nào được ghi; worker `success` nhưng **bỏ qua** bước upload ("Sync skipped: chưa đăng nhập") — xem TC-WM-08.

### TC-FB-03 — Upload log khi đã đăng nhập
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có user Firebase Auth; đã ghi 2 log `isDirty = 1`.
- **Bước:**
  1. Gán `uid` cho user (đăng nhập qua Firebase console/Test hoặc tạm set `FirebaseAuth` trong debug).
  2. Run `periodic_sync_worker`.
  3. Kiểm tra Firestore.
- **Kết quả mong đợi:**
  - Firestore có `users/{uid}/habit_logs/{logId}` với các trường: `habitId`, `habitName`, `category`, `value`, `unit`, `targetValueAtTime`, `loggedAt`, `note`.
  - Trong Room, các log đã sync có `isDirty = 0`.
  - Log `SyncWorker`: **"Sync completed successfully: 2 logs"**.

### TC-FB-04 — Không upload lại log đã sync
- **Ưu tiên:** P2
- **Bước:** 1. Run `periodic_sync_worker` lần thứ hai liên tiếp.
- **Kết quả mong đợi:** Không có log nào được gửi lại (danh sách `isDirty = 1` rỗng).

### TC-FB-05 — Lỗi mạng khi upload
- **Ưu tiên:** P2
- **Bước:** 1. Ngắt mạng giữa chừng → 2. Run worker.
- **Kết quả mong đợi:** Log `isDirty` **vẫn là 1** (không đánh dấu nhầm là đã sync); worker retry (tối đa 3 lần).

### TC-FB-06 — Quy tắc bảo mật Firestore
- **Ưu tiên:** P2
- **Bước:** 1. Kiểm tra Security Rules của project Firebase.
- **Kết quả mong đợi:** Chỉ user đã xác thực mới đọc/ghi được `users/{uid}/**`; không có rule `allow read, write: if true;` (rủi ro rò rỉ dữ liệu sức khỏe).

### TC-FB-07 — App chạy khi KHÔNG có Firebase
- **Ưu tiên:** P1
- **Tiền điều kiện:** Xóa/đổi tên `google-services.json` khỏi `app/` (hoặc build không có plugin google-services).
- **Bước:** 1. Build & chạy → 2. Sử dụng toàn bộ chức năng cục bộ.
- **Kết quả mong đợi:** App vẫn hoạt động đầy đủ (thói quen, log, Insights, Health Connect); chỉ phần sync cloud không có. Không crash khi vào bất kỳ màn hình nào.

---

## 17. TC-EDGE — Biên, lỗi, trạng thái đặc biệt

### TC-EDGE-01 — Dark mode
- **Ưu tiên:** P2
- **Bước:** 1. Settings → Display → Dark theme → Bật → 2. Mở lần lượt: **Trang chủ**, **Thói quen**, **Thêm/Sửa thói quen**, **Ghi chép**, **Phân tích**, màn **Đăng nhập**.
- **Kết quả mong đợi (sau bản sửa lỗi mục 3 #18):** Nền màn hình là `background` của dark scheme (`#1A1C19`), nền thẻ/panel là `surfaceContainerLow` (`#23261F`) → thẻ **vẫn nổi hơn nền** và chữ `onSurface` (`#E2E3DC`) đọc rõ trên cả hai (tương phản ≈ **11.9:1**, đạt WCAG AA). **Không** còn thẻ trắng hay nền `#F8F9FA` nào ở bất kỳ màn hình nào (kể cả màn Đăng nhập và màn chờ xác thực).
- **Ghi chú:** Các màu **cố ý giữ nguyên** vì không phải nền/thẻ: nút & FAB xanh thương hiệu (`#2E7D32`), chip/thẻ lỗi (`#FFEBEE` + `#C62828`), bộ màu danh mục (nước/chánh niệm/…), nền scrim đen mờ trên preview camera (`Color.Black.copy(alpha = 0.6f)`), badge "Tự động" (`#F1F8E9`) và vệt nền xám của thanh tiến độ (`Color(0xFFEEEEEE)`) — kiểm tra tự động ở **TC-EDGE-16**.

### TC-EDGE-02 — Dynamic color (Android 12+)
- **Ưu tiên:** P2
- **Bước:** 1. Đổi hình nền hệ thống → 2. Mở app.
- **Kết quả mong đợi:** App **không** đổi theo màu hệ thống vì `HealthyAITheme(dynamicColor = false)`; màu xanh HealthyAI giữ nguyên.

### TC-EDGE-03 — Xoay màn hình ở cả 4 tab
- **Ưu tiên:** P1
- **Bước:** 1. Ở mỗi tab, xoay dọc ↔ ngang.
- **Kết quả mong đợi:** Không crash; danh sách/điểm số giữ nguyên (ViewModel sống sót qua config change).
- **Ghi chú:** Dialog ghi nhận dùng `remember` (không phải `rememberSaveable`) → **sẽ tự đóng** khi xoay; nếu yêu cầu giữ dialog, ghi nhận thiếu sót.

### TC-EDGE-04 — Process death & khôi phục
- **Ưu tiên:** P1
- **Bước:** 1. Bật **Developer options → Don't keep activities** → 2. Lần lượt vào từng tab rồi thoát ra/vào lại.
- **Kết quả mong đợi:** App khôi phục về đúng route; không crash; dữ liệu đọc lại từ Room.

### TC-EDGE-05 — Chữ hệ thống rất lớn (font scale 2.0)
- **Ưu tiên:** P2
- **Bước:** 1. Settings → Display → Font size → Largest → 2. Mở cả 4 tab + màn Thêm thói quen.
- **Kết quả mong đợi:** Chữ không bị cắt mất nội dung quan trọng; nút bấm vẫn chạm được.
- **Ghi chú:** Một số ô có chiều cao cố định (`height(48.dp)`, `width(100.dp)`) có thể bị tràn chữ — ghi lại nếu xảy ra.

### TC-EDGE-06 — Thay đổi ngôn ngữ hệ thống
- **Ưu tiên:** P2
- **Bước:** 1. Đổi ngôn ngữ máy sang English → 2. Mở app.
- **Kết quả mong đợi:** Chuỗi trong app **vẫn tiếng Việt** (hard-code trong Kotlin, `strings.xml` chỉ có `app_name`). Định dạng ngày có thể thay đổi theo `Locale("vi")` được set cứng. Ghi nhận là chưa hỗ trợ đa ngôn ngữ.

### TC-EDGE-07 — Bộ nhớ đầy
- **Ưu tiên:** P2
- **Bước:** 1. Làm đầy dung lượng (hoặc chặn ghi) → 2. Ghi log cho một thói quen.
- **Kết quả mong đợi:**
  - Ghi log thất bại → màn Thói quen hiện **thẻ lỗi đỏ** ngay dưới tiêu đề (nội dung `"Lỗi ghi nhận: …"`), có nút **X** để đóng.
  - **Không** có Toast "Đã ghi nhận" (chỉ hiện khi lưu thành công), tiến độ không đổi, app không crash.
  - Bấm **X** → thẻ lỗi biến mất; ghi nhận lại sau khi giải phóng dung lượng → thành công bình thường.
- **Ghi chú:** Đây là bản sửa lỗi **phụ lục C #10** (trước đây `HabitsUiState.error` được set nhưng màn Thói quen không hiển thị ở đâu cả). Thẻ dùng chung component `presentation/ui/component/ErrorStateCard.kt` với màn Trang chủ.

### TC-EDGE-08 — Ảnh hưởng pin / Doze
- **Ưu tiên:** P2
- **Bước:** 1. `adb shell dumpsys deviceidle force-idle` → 2. Chờ job. 
- **Kết quả mong đợi:** Job bị hệ thống hoãn (không chạy) — đúng thiết kế; thoát Doze (`adb shell dumpsys deviceidle unforce`) thì job chạy lại.

### TC-EDGE-09 — Force stop rồi mở lại
- **Ưu tiên:** P1
- **Bước:** 1. Settings → Apps → HealthyAI → **Force stop** → 2. Mở lại app.
- **Kết quả mong đợi:** App mở bình thường, dữ liệu nguyên vẹn, job được đăng ký lại ở `MainActivity.onCreate`.

### TC-EDGE-10 — Người dùng thu hồi quyền khi app đang chạy
- **Ưu tiên:** P2
- **Bước:** 1. Đang ở tab Ghi chép (đã cấp quyền Camera) → 2. Thu hồi quyền qua Settings → 3. Quay lại app.
- **Kết quả mong đợi:** App không crash; màn Camera trở về trạng thái xin quyền (sau khi Activity được tạo lại).

### TC-EDGE-11 — Thời điểm nửa đêm (chuyển ngày)
- **Ưu tiên:** P2
- **Bước:** 1. Ghi log lúc 23:59 → 2. Đổi giờ hệ thống sang 00:01 ngày kế tiếp → 3. Mở lại app.
- **Kết quả mong đợi:** Log cũ rơi ra ngoài khoảng "hôm nay" → tiến độ về 0 (đúng theo nghiệp vụ), danh sách thói quen không đổi.

### TC-EDGE-12 — Dữ liệu cực lớn
- **Ưu tiên:** P2
- **Bước:** 1. Ghi 500 log cho 20 thói quen → 2. Mở tab Thói quen và Phân tích.
- **Kết quả mong đợi:** Không crash, không ANR; cuộn mượt; điểm sinh lực và gợi ý vẫn tính đúng theo 7 ngày gần nhất.

### TC-EDGE-13 — Nhiều thói quen (100) trên danh sách
- **Ưu tiên:** P2
- **Bước:** 1. Tạo 100 thói quen → 2. Cuộn danh sách.
- **Kết quả mong đợi:** `LazyColumn` render mượt (chỉ compose item hiển thị); không crash.

### TC-EDGE-14 — Mục tiêu rất lớn
- **Ưu tiên:** P2
- **Bước:** 1. Tạo thói quen mục tiêu `999999999` → 2. Xem dòng tiến độ.
- **Kết quả mong đợi:** Lưu được; hiển thị đúng, không tràn dòng, không hiển thị ký hiệu khoa học.

### TC-EDGE-15 — Nhập nhanh nhiều thao tác
- **Ưu tiên:** P2
- **Bước:** 1. Bấm đồng thời nhiều nút/ chip/chip lọc thật nhanh 30 giây.
- **Kết quả mong đợi:** Không crash, không điều hướng trùng màn hình (nhờ `launchSingleTop`).

### TC-EDGE-16 — Không còn nền/thẻ hard-code (unit test JVM)
- **Ưu tiên:** P2
- **Bước:**
  1. `.\gradlew test --tests "*ThemeSurfaceColorsTest"` (repo chưa commit wrapper — dùng Gradle đã cài, xem mục 2.2; JDK 21).
  2. Kiểm tra tĩnh: `Get-ChildItem -Recurse app\src\main\java\com\healthyai\app\presentation -Filter *.kt | Select-String -Pattern 'containerColor = Color.White|Color\(0xFFF8F9FA\)'` → **không** còn kết quả nào (đã chạy, không ra dòng nào).
- **Kết quả mong đợi:** **4 test** trong `app/src/test/.../presentation/ui/theme/ThemeSurfaceColorsTest.kt` pass (đã chạy thật trên Gradle 8.13 + JDK 21: toàn bộ `app/src/test` = **19 test class / 121 test, 0 fail**), chốt các tính chất của bản sửa lỗi:
  - nền thẻ (`surfaceContainerLow`) **khác** nền màn hình (`background`) ở **cả** chế độ sáng và tối;
  - nền thẻ **sáng hơn** nền màn hình ở cả hai chế độ → thẻ vẫn nổi lên (chống "thẻ tàng hình" khi đổi token);
  - nền thẻ ở chế độ tối **không** phải `Color.White` và có độ sáng tương đối **< 0.2** → chống tái phát đúng lỗi #18 (thẻ trắng ở dark mode);
  - chữ `onSurface` trên nền thẻ đạt tương phản **WCAG AA (≥ 4.5:1)** ở cả hai chế độ.
- **Ghi chú:** Test chạy trên **JVM** (không cần emulator/thiết bị) vì chỉ đọc giá trị màu của theme (`androidx.compose.ui.graphics.Color`), không compose UI. Kiểm tra bằng mắt (đổ bóng, độ tương phản thực tế trên màn hình) vẫn cần thiết bị/emulator theo **TC-EDGE-01**.

---

## 18. TC-REG — Smoke / Regression checklist (chạy trước mỗi bản phát hành)

| ID | Nội dung kiểm tra | Ưu tiên | KQ |
|:--|:--|:--|:--|
| TC-REG-01 | Cài đặt & mở app không crash (cold start sau `pm clear`) | P0 | |
| TC-REG-02 | Điều hướng được cả 4 tab, bottom bar đúng | P0 | |
| TC-REG-03 | Tạo thói quen mới thành công, thấy trong danh sách (tên trùng bị chặn — TC-ADD-25) | P0 | |
| TC-REG-04 | Ghi nhận thói quen thành công, tiến độ cập nhật + Toast **"Đã ghi nhận"** (TC-HAB-24) | P0 | |
| TC-REG-05 | Đạt mục tiêu → hiển thị "Hoàn thành" | P0 | |
| TC-REG-06 | Sửa thói quen lưu đúng dữ liệu | P0 | |
| TC-REG-07 | Xóa thói quen biến mất khỏi danh sách | P0 | |
| TC-REG-08 | Validate form Thêm thói quen (tên rỗng, mục tiêu ≤ 0) | P0 | |
| TC-REG-09 | Màn Phân tích hiển thị gợi ý + điểm sinh lực | P1 | |
| TC-REG-10 | Camera: xin quyền & hiển thị preview | P1 | |
| TC-REG-11 | Home: đọc đúng số bước/ngủ/nhịp tim từ Health Connect | P1 | |
| TC-REG-12 | Banner lỗi Health Connect hoạt động & đóng được | P1 | |
| TC-REG-13 | DB mã hóa: không đọc được nội dung plain text | P0 | |
| TC-REG-14 | 3 job nền được đăng ký, không nhân bản, không crash khi mở app | P0 | |
| TC-REG-15 | Thông báo nhắc nhở hiển thị khi đã cấp quyền | P1 | |
| TC-REG-16 | Widget thêm được, hiển thị dữ liệu, tự cập nhật khi dữ liệu đổi, bấm mở app | P1 | |
| TC-REG-17 | `.\gradlew test` pass 100% | P0 | |
| TC-REG-18 | Dark mode không làm chữ không đọc được | P2 | |
| TC-REG-19 | Xoay màn hình & process death không crash | P1 | |
| TC-REG-20 | Không có `FATAL EXCEPTION` trong logcat suốt phiên test | P0 | |
| TC-REG-21 | Đăng xuất từ Home → quay về màn đăng nhập, dữ liệu thói quen còn nguyên (TC-AUTH-15) | P1 | |
| TC-REG-22 | Nâng cấp app từ bản cũ (schema v1/v2) → thói quen + nhật ký **giữ nguyên**, không crash (TC-DB-08, TC-DB-15) | P0 | |
| TC-REG-23 | Màn Ghi chép: nút **"Thêm vào nhật ký"** mở hộp thoại ghi nhận thói quen Dinh dưỡng và ghi được log (TC-CAM-15) | P1 | |
| TC-REG-24 | Gợi ý vận động đúng theo **đơn vị** thói quen: "30 phút" không bị đọc thành "30 bước" (TC-INS-29) | P1 | |

---

## 19. Phụ lục A — Ma trận truy vết (UI ↔ ViewModel ↔ UseCase ↔ Data)

| Màn hình | ViewModel | UseCase | Nguồn dữ liệu | Test case |
|:--|:--|:--|:--|:--|
| Home | `HomeViewModel` | `GetTodayHabitsUseCase`, `GetHealthDataUseCase`, `GetHealthConnectPermissionStatusUseCase`, `GetNotificationPermissionStatusUseCase` | Room `habits`, Health Connect, NotificationManager | TC-HOME-01…11, TC-HC-01…17, TC-WM-13…16 |
| Đăng nhập | `AuthGateViewModel` + `LoginViewModel` | `AuthRepository` (signIn/signUp/authState) | Firebase Auth | TC-AUTH-01…14 |
| Habits | `HabitsViewModel` | `GetTodayHabitsUseCase`, `LogHabitUseCase` | Room `habits` + `habit_logs` | TC-HAB-01…30 |
| Add/Edit Habit | `AddEditHabitViewModel` | `AddHabitUseCase`, `UpdateHabitUseCase`, `DeleteHabitUseCase` | Room `habits` (soft delete: `isActive = 0`) | TC-ADD-01…27 |
| Camera | `CameraViewModel` | ML Kit `ImageLabeling`; `CameraScreen` → `AppNavigation.openHabitLogFor(DINH DƯỠNG)` → `HabitsViewModel` | On-device ML Kit; Room `habits`/`habit_logs` | TC-CAM-01…15 |
| Insights | `InsightsViewModel` | `GetHealthInsightsUseCase`, `GetVitalityScoreUseCase` | Room `habit_logs` + `health_data_history` → `HabitRuleEngine` | TC-INS-01…30 |
| Insights → Habits (nút gợi ý) | `InsightsScreen` → `AppNavigation` → `HabitsViewModel` | `SavedStateHandle` (`KEY_LOG_CATEGORY`) + `LogRequest` | Room `habits` | TC-INS-11, TC-INS-27, TC-INS-28 |
| (chưa có UI) | `CoreTrackingViewModel` | `GetHealthDataUseCase` | Health Connect | TC-ENV-03 (unit test) |
| Widget | — (Glance `HabitWidget`) | `TodayHabitProgress` (tiến độ ngày + thứ tự theo buổi), `HabitWidgetRefresher` (vẽ lại) | Room `habits` + `habit_logs` hôm nay qua `HabitRepository` | TC-WG-01…12 |
| Background | — | `HealthSyncWorker`, `SyncWorker`, `ReminderWorker` | Health Connect → Room; Room → Firestore | TC-WM-01…12, TC-FB-* |

---

## 20. Phụ lục B — Bảng tổng hợp kết quả

> Điền `Pass`/`Fail`/`Blocked`/`N/A` vào cột **KQ** cùng ngày test và ghi chú.

### Nhóm môi trường & khởi động

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-ENV-01 | Build debug | P0 | | |
| TC-ENV-02 | Cài đặt lần đầu | P0 | | |
| TC-ENV-03 | Unit test JVM | P1 | | |
| TC-ENV-04 | Instrumented test DAO | P1 | | |
| TC-ENV-05 | Smoke 4 màn hình | P0 | | |
| TC-ENV-06 | Không ANR khi điều hướng nhanh | P2 | | |
| TC-INIT-01 | Cold start lần đầu | P0 | | |
| TC-INIT-02 | Đăng ký 3 job nền | P1 | | |
| TC-INIT-03 | Không tạo job trùng | P1 | | |
| TC-INIT-04 | Khởi động khi offline | P1 | | |
| TC-INIT-05 | Sau reboot | P2 | | |
| TC-AUTH-01 | Lần mở đầu tiên → màn đăng nhập | P0 | | |
| TC-AUTH-02 | Bỏ qua → chế độ khách | P0 | | |
| TC-AUTH-03 | Đăng nhập thành công | P0 | | |
| TC-AUTH-04 | Sai mật khẩu | P0 | | |
| TC-AUTH-05 | Validate phía client | P1 | | |
| TC-AUTH-06 | Chuyển Đăng nhập ⇄ Đăng ký | P1 | | |
| TC-AUTH-07 | Đăng ký email đã tồn tại | P1 | | |
| TC-AUTH-08 | Provider Email/Password chưa bật | P1 | | |
| TC-AUTH-09 | Mất mạng khi đăng nhập | P1 | | |
| TC-AUTH-10 | Hiện/ẩn mật khẩu | P2 | | |
| TC-AUTH-11 | Chống bấm gửi 2 lần | P2 | | |
| TC-AUTH-12 | Phiên đã lưu còn hiệu lực | P0 | | |
| TC-AUTH-13 | Unit test JVM `LoginViewModelTest` | P1 | | |
| TC-AUTH-14 | Đăng nhập không mất dữ liệu cục bộ | P1 | | |
| TC-AUTH-15 | Nút Đăng xuất ở Home (mục 3 #19) | P0 | | |
| TC-AUTH-16 | Unit test JVM luồng đăng xuất | P1 | | |
| TC-NAV-01 | Bottom bar trên 4 màn | P0 | | |
| TC-NAV-02 | Tab đang chọn được đánh dấu | P2 | | |
| TC-NAV-03 | Ẩn bottom bar ở Add/Edit | P1 | | |
| TC-NAV-04 | Back từ Add/Edit | P0 | | |
| TC-NAV-05 | Back ở tab gốc thoát app | P1 | | |
| TC-NAV-06 | Bảo toàn filter khi đổi tab | P1 | | |
| TC-NAV-07 | Hiệu ứng chuyển màn | P2 | | |

### Home

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-HOME-01 | Lời chào theo thời điểm | P2 | | |
| TC-HOME-02 | Ngày tháng tiếng Việt | P2 | | |
| TC-HOME-03 | Danh sách thói quen | P1 | | |
| TC-HOME-04 | Trạng thái hoàn thành trên Home | P1 | | |
| TC-HOME-05 | Tóm tắt sức khỏe có dữ liệu | P1 | | |
| TC-HOME-06 | Thiếu quyền → hiển thị 0 | P1 | | |
| TC-HOME-07 | Health Connect không khả dụng → thẻ mời cài | P1 | | |
| TC-HOME-07b | Health Connect cần cập nhật | P1 | | |
| TC-HOME-08 | Đóng banner lỗi | P2 | | |
| TC-HOME-09 | Trạng thái rỗng | P1 | | |
| TC-HOME-10 | Loading | P2 | | |
| TC-HOME-11 | Cuộn danh sách dài | P2 | | |

### Thói quen

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-HAB-01 | Header & ngày | P2 | | |
| TC-HAB-02 | Danh sách theo danh mục | P1 | | |
| TC-HAB-03 | Định dạng tiến độ theo danh mục | P1 | | |
| TC-HAB-04 | Mở hộp thoại ghi nhận | P0 | | |
| TC-HAB-05 | Ghi nhận hợp lệ | P0 | | |
| TC-HAB-06 | Ghi nhiều lần cộng dồn | P0 | | |
| TC-HAB-07 | Đạt mục tiêu → hoàn thành | P0 | | |
| TC-HAB-08 | Vượt mục tiêu | P1 | | |
| TC-HAB-09 | Hủy hộp thoại | P1 | | |
| TC-HAB-10 | Nhập rỗng | P1 | | |
| TC-HAB-11 | Số thập phân | P2 | | |
| TC-HAB-12 | Giá trị âm/0 bị chặn | P2 | | |
| TC-HAB-13 | Lọc "Sáng" | P1 | | |
| TC-HAB-14 | Lọc "Chiều"/"Tối" | P1 | | |
| TC-HAB-15 | Lọc "Tất cả" | P1 | | |
| TC-HAB-16 | Lọc ra danh sách rỗng | P2 | | |
| TC-HAB-17 | Chỉ hiện thói quen đang hoạt động | P1 | | |
| TC-HAB-18 | Tiến độ sau khi mở lại app | P1 | | |
| TC-HAB-19 | Tiến độ cập nhật realtime | P2 | | |
| TC-HAB-20 | FAB thêm thói quen | P1 | | |
| TC-HAB-21 | Bấm thẻ → màn sửa | P1 | | |
| TC-HAB-22 | Bấm "Ghi" không mở màn sửa | P2 | | |
| TC-HAB-23 | Nhiều thói quen cùng danh mục | P2 | | |
| TC-HAB-24 | Phản hồi "Đã ghi nhận" sau khi lưu | P1 | | |
| TC-HAB-25 | Unit test JVM `HabitsViewModelTest` | P1 | | |
| TC-HAB-26 | Số mặc định hộp thoại ghi nhận là số sạch | P2 | | |
| TC-HAB-27 | Unit test JVM định dạng số (`NumberDisplayTest`) | P1 | | |
| TC-HAB-28 | Nhập sai trong hộp thoại ghi nhận → giữ hộp thoại + báo lỗi | P1 | | |
| TC-HAB-29 | Unit test JVM đọc giá trị ô "Số lượng" (`HabitLogInputTest`) | P1 | | |
| TC-HAB-30 | Unit test JVM validate số lượng & hiển thị lỗi | P1 | | |

### Thêm/Sửa/Xóa thói quen

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-ADD-01 | Mở chế độ thêm | P1 | | |
| TC-ADD-02 | Đổi đơn vị theo danh mục | P0 | | |
| TC-ADD-03 | Tạo thói quen hợp lệ | P0 | | |
| TC-ADD-04 | Validate tên rỗng | P0 | | |
| TC-ADD-05 | Validate tên khoảng trắng | P1 | | |
| TC-ADD-06 | Validate mục tiêu rỗng/0 | P0 | | |
| TC-ADD-07 | Validate mục tiêu không phải số | P1 | | |
| TC-ADD-08 | Thứ tự kiểm tra lỗi | P2 | | |
| TC-ADD-09 | Sửa thói quen | P0 | | |
| TC-ADD-10 | Sửa danh mục đổi đơn vị | P1 | | |
| TC-ADD-11 | Xóa thói quen (soft delete) | P0 | | |
| TC-ADD-12 | Log của thói quen đã xóa | P2 | | |
| TC-ADD-13 | Trùng tên thói quen | P2 | | |
| TC-ADD-14 | Double tap nút Lưu (chặn bằng `isSaving`) | P1 | | |
| TC-ADD-15 | Tên rất dài | P2 | | |
| TC-ADD-16 | Tên emoji/ký tự đặc biệt | P2 | | |
| TC-ADD-17 | Xoay màn hình giữ dữ liệu | P2 | | |
| TC-ADD-18 | Process death | P2 | | |
| TC-ADD-19 | Nút đáy luôn hiển thị | P2 | | |
| TC-ADD-20 | Tổ hợp tần suất | P2 | | |
| TC-ADD-21 | Hủy dialog xác nhận xóa | P1 | | |
| TC-ADD-22 | Unit test JVM luồng xóa mềm | P1 | | |
| TC-ADD-23 | Ô "Mục tiêu" hiển thị số sạch (bỏ đuôi `.0`) | P2 | | |
| TC-ADD-24 | Unit test JVM định dạng mục tiêu | P1 | | |
| TC-ADD-25 | Chống trùng tên khi tạo/đổi tên | P1 | | |
| TC-ADD-26 | Unit test JVM chống trùng tên | P1 | | |
| TC-ADD-27 | Unit test JVM chống double-submit khi lưu | P1 | | |

### Camera, Insights

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-CAM-01 | Chưa cấp quyền Camera | P0 | | |
| TC-CAM-02 | Cấp quyền Camera | P0 | | |
| TC-CAM-03 | Từ chối quyền Camera | P0 | | |
| TC-CAM-04 | Preview (mô phỏng) | P1 | | |
| TC-CAM-05 | Nút "Phân tích món ăn" (known issue #4) | P1 | | |
| TC-CAM-06 | Trạng thái đang phân tích | P2 | | |
| TC-CAM-07 | Kết quả nhận diện | P1 | | |
| TC-CAM-08 | Ảnh không có món ăn | P2 | | |
| TC-CAM-09 | Lỗi phân tích | P2 | | |
| TC-CAM-10 | Thu hồi quyền khi đang chạy | P2 | | |
| TC-CAM-11 | Nút phân tích disable khi camera chưa sẵn sàng | P1 | | |
| TC-CAM-12 | Không mở được camera → thẻ lỗi đỏ | P1 | | |
| TC-CAM-13 | Chụp & phân tích liên tiếp nhiều lần | P2 | | |
| TC-CAM-14 | Preview hoạt động lại khi quay lại màn hình | P2 | | |
| TC-CAM-15 | Nút "Thêm vào nhật ký" → mở hộp thoại ghi nhận thói quen Dinh dưỡng (phụ lục C #22) | P1 | | |
| TC-INS-01 | Trạng thái rỗng | P1 | | |
| TC-INS-02 | Nước CRITICAL | P0 | | |
| TC-INS-03 | Nước MEDIUM | P0 | | |
| TC-INS-04 | Nước đủ → không gợi ý | P1 | | |
| TC-INS-05 | Ngủ HIGH | P0 | | |
| TC-INS-06 | Ngủ MEDIUM | P0 | | |
| TC-INS-07 | Ngủ đủ → không gợi ý | P1 | | |
| TC-INS-08 | Vận động HIGH | P0 | | |
| TC-INS-09 | Vận động LOW | P1 | | |
| TC-INS-10 | Sắp xếp theo ưu tiên | P1 | | |
| TC-INS-11 | Nút hành động trong thẻ gợi ý AI | P2 | | |
| TC-INS-12 | Điểm sinh lực 100 | P0 | | |
| TC-INS-13 | Điểm sinh lực 50 | P0 | | |
| TC-INS-14 | Thiếu danh mục → mặc định 70 | P1 | | |
| TC-INS-15 | Điểm cập nhật realtime | P1 | | |
| TC-INS-16 | Biên trên điểm | P2 | | |
| TC-INS-17 | Biên dưới điểm | P2 | | |
| TC-INS-18 | Gợi ý thiếu ngủ từ Health Connect | P1 | | |
| TC-INS-19 | Gợi ý nhịp tim cao | P1 | | |
| TC-INS-20 | Kết hợp 2 nguồn gợi ý | P2 | | |
| TC-INS-21 | Biểu đồ nhất quán hàng tuần (dữ liệu thật) | P1 | | |
| TC-INS-22 | Loading | P2 | | |
| TC-INS-23 | Thẻ giấc ngủ: chưa có dữ liệu pha | P1 | | |
| TC-INS-24 | Thẻ giấc ngủ: dữ liệu pha thật từ Health Connect | P1 | | |
| TC-INS-25 | Delta điểm: ẩn khi thiếu dữ liệu tuần trước | P1 | | |
| TC-INS-26 | Delta điểm: tăng / giảm / không đổi | P1 | | |
| TC-INS-27 | Nút hành động khi chưa có thói quen tương ứng | P2 | | |
| TC-INS-28 | Unit test JVM yêu cầu ghi nhận từ Insights | P1 | | |
| TC-INS-29 | Gợi ý vận động theo mục tiêu cho thói quen đo bằng phút (phụ lục C #17) | P1 | | |
| TC-INS-30 | Unit test JVM rule engine: gom theo ngày + theo đơn vị (phụ lục C #16, #17) | P1 | | |

### Dữ liệu, nền, widget, Firebase

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-HC-01 | Health Connect khả dụng | P0 | | |
| TC-HC-02 | Health Connect không khả dụng | P1 | | |
| TC-HC-03 | Số bước chân | P0 | | |
| TC-HC-04 | Thời gian ngủ | P0 | | |
| TC-HC-05 | Ngủ ngoài khung giờ | P2 | | |
| TC-HC-06 | Nhịp tim | P1 | | |
| TC-HC-07 | Không có dữ liệu nhịp tim | P2 | | |
| TC-HC-08 | Calories | P2 | | |
| TC-HC-09 | Ghi vào `health_data_history` | P1 | | |
| TC-HC-10 | Bỏ qua khi thiếu quyền | P1 | | |
| TC-HC-11 | Dọn dữ liệu > 30 ngày | P2 | | |
| TC-HC-12 | Thiếu quyền → không crash | P0 | | |
| TC-HC-13 | Xin quyền ngay trong app (thẻ Home) | P0 | | |
| TC-HC-14 | Từ chối / cấp một phần quyền | P1 | | |
| TC-HC-15 | Cấp quyền trong app Health Connect rồi quay lại | P1 | | |
| TC-HC-16 | Thẻ nhắc không chặn thao tác | P2 | | |
| TC-HC-17 | Unit test JVM luồng quyền | P1 | | |
| TC-DB-01 | Tạo DB mã hóa | P0 | | |
| TC-DB-02 | Xác nhận DB mã hóa | P0 | | |
| TC-DB-03 | Giữ dữ liệu sau khi mở lại | P0 | | |
| TC-DB-04 | Giữ dữ liệu sau reboot | P1 | | |
| TC-DB-05 | `targetValueAtTime` đúng | P1 | | |
| TC-DB-06 | Cờ `isDirty` | P1 | | |
| TC-DB-07 | Cascade khi xóa cứng | P2 | | |
| TC-DB-08 | Destructive migration (known issue — phụ lục C #14) | P2 | | |
| TC-DB-09 | DB hỏng | P2 | | |
| TC-DB-10 | Ghi đồng thời | P2 | | |
| TC-DB-11 | Passphrase từ Android Keystore, không hard-code | P0 | | |
| TC-DB-12 | Nâng cấp từ bản cũ → rekey giữ dữ liệu | P0 | | |
| TC-DB-13 | Mất khoá Keystore / DB hỏng → tạo DB mới, không crash | P1 | | |
| TC-DB-14 | Unit test JVM nguồn passphrase | P1 | | |
| TC-DB-15 | Nâng cấp từ schema v1/v2 → giữ nguyên thói quen + nhật ký (phụ lục C #14) | P0 | | |
| TC-DB-16 | Passphrase lưu đồng bộ trước khi tạo DB (phụ lục C #29) | P2 | | |
| TC-WM-01 | Lịch HealthSyncWorker | P1 | | |
| TC-WM-02 | Lịch SyncWorker | P1 | | |
| TC-WM-03 | Lịch ReminderWorker | P1 | | |
| TC-WM-04 | Thông báo nhắc nhở | P1 | | |
| TC-WM-05 | Android 13+ thiếu quyền (known issue #8) | P1 | | |
| TC-WM-06 | Sau khi cấp quyền thông báo | P1 | | |
| TC-WM-07 | HealthSyncWorker ghi Room | P1 | | |
| TC-WM-08 | SyncWorker khi chưa đăng nhập (known issue #6) | P1 | | |
| TC-WM-09 | SyncWorker khi offline | P1 | | |
| TC-WM-10 | Giới hạn retry | P1 | | |
| TC-WM-11 | Không nhân bản job | P2 | | |
| TC-WM-12 | Job sau reboot | P2 | | |
| TC-WM-13 | Xin quyền thông báo trên Home | P0 | | |
| TC-WM-14 | Từ chối quyền thông báo | P1 | | |
| TC-WM-15 | Thông báo bị tắt trong Settings | P1 | | |
| TC-WM-16 | Unit test JVM luồng quyền thông báo | P1 | | |
| TC-WG-01 | Widget có dữ liệu | P1 | | |
| TC-WG-02 | Widget không có dữ liệu | P1 | | |
| TC-WG-03 | Thứ tự thói quen trên widget theo buổi (đã sửa #16) | P1 | | |
| TC-WG-04 | Widget tự cập nhật khi dữ liệu đổi (đã sửa #15) | P1 | | |
| TC-WG-05 | Bấm widget mở app | P1 | | |
| TC-WG-06 | Xóa thói quen đang hiển thị | P2 | | |
| TC-WG-07 | Resize | P2 | | |
| TC-WG-08 | Sau reboot | P2 | | |
| TC-WG-09 | Unit test JVM vẽ lại widget | P1 | | |
| TC-WG-10 | Unit test JVM tiến độ ngày + thứ tự danh sách | P1 | | |
| TC-WG-11 | Thứ tự widget đổi theo buổi (định kỳ 30 phút) | P2 | | |
| TC-WG-12 | Thanh tiến độ & danh sách khớp dữ liệu đã ghi | P1 | | |
| TC-FB-01 | Khởi động có Firebase | P1 | | |
| TC-FB-02 | Chưa đăng nhập | P1 | | |
| TC-FB-03 | Upload log | P1 | | |
| TC-FB-04 | Không upload lại | P2 | | |
| TC-FB-05 | Lỗi mạng | P2 | | |
| TC-FB-06 | Security rules | P2 | | |
| TC-FB-07 | App không có Firebase | P1 | | |
| TC-EDGE-01 | Dark mode | P2 | | |
| TC-EDGE-02 | Dynamic color | P2 | | |
| TC-EDGE-03 | Xoay màn hình | P1 | | |
| TC-EDGE-04 | Process death | P1 | | |
| TC-EDGE-05 | Font scale 2.0 | P2 | | |
| TC-EDGE-06 | Đổi ngôn ngữ hệ thống | P2 | | |
| TC-EDGE-07 | Ghi log thất bại → thẻ lỗi đỏ ở màn Thói quen | P2 | | |
| TC-EDGE-08 | Doze | P2 | | |
| TC-EDGE-09 | Force stop → mở lại | P1 | | |
| TC-EDGE-10 | Thu hồi quyền khi đang chạy | P2 | | |
| TC-EDGE-11 | Qua nửa đêm | P2 | | |
| TC-EDGE-12 | Dữ liệu cực lớn | P2 | | |
| TC-EDGE-13 | 100 thói quen | P2 | | |
| TC-EDGE-14 | Mục tiêu rất lớn | P2 | | |
| TC-EDGE-15 | Thao tác nhanh liên tục | P2 | | |
| TC-EDGE-16 | Nền/thẻ không hard-code (unit test màu, mục 3 #18) | P2 | | |

---

## 21. Phụ lục C — Tổng hợp lỗi phát hiện khi đọc code (cần xác nhận với team)

| # | Mô tả | File/Class liên quan | Mức độ | Test case | Trạng thái |
|:--|:--|:--|:--|:--|:--|
| 1 | ~~Home luôn `isCompleted = false`~~ | `presentation/screen/home/{HomeScreen,HomeViewModel}.kt` | Trung bình | TC-HOME-04 | ✅ Đã sửa |
| 2 | ~~Tiến độ tính sai mốc ngày (`getHabitLogsForPeriod(0)`)~~ | `data/repository/HabitRepositoryImpl.kt` | **Cao** | TC-HAB-18 | ✅ Đã sửa |
| 3 | ~~Không có CameraX preview thật~~ | `CameraScreen.CameraPreviewSection` | **Cao** | TC-CAM-04 | ✅ Đã sửa |
| 4 | ~~Nút "Phân tích món ăn" chưa nối ML Kit~~ | `CameraScreen`, `CameraViewModel` | **Cao** | TC-CAM-05 | ✅ Đã sửa |
| 5 | ~~Insights dùng dữ liệu hard-code~~ | `InsightsScreen`, `InsightsViewModel`, `GetWeeklyConsistencyUseCase`, `GetSleepStageBreakdownUseCase` | Trung bình | TC-INS-21, TC-INS-23…26 | ✅ Đã sửa |
| 6 | ~~Không có màn đăng nhập → `SyncWorker` luôn fail~~ | `presentation/navigation/{AppNavigation,AuthGateViewModel}.kt`, `presentation/screen/auth/{LoginScreen,LoginViewModel}.kt`, `domain/repository/AuthRepository.kt`, `data/repository/AuthRepositoryImpl.kt`, `di/AppModule.kt`, `worker/SyncWorker.kt` | Trung bình | TC-AUTH-01…14, TC-WM-08 | ✅ Đã sửa |
| 7 | ~~Không có UI xin quyền Health Connect~~ | `HomeScreen`, `HomeViewModel`, `HealthConnectPermissionCard`, `GetHealthConnectPermissionStatusUseCase`, `HealthConnectDataSourceImpl` | Trung bình | TC-HC-12…17, TC-HOME-07 | ✅ Đã sửa |
| 8 | ~~Không xin `POST_NOTIFICATIONS`~~ | `HomeScreen`, `HomeViewModel`, `ReminderNotificationCard`, `NotificationPermissionRepositoryImpl`, `GetNotificationPermissionStatusUseCase` | Trung bình | TC-WM-05, TC-WM-13…16 | ✅ Đã sửa |
| 9 | ~~Không hiển thị `showLogSuccessFor`~~ | `HabitsScreen`, `HabitsViewModel`, `app/src/test/.../habits/HabitsViewModelTest.kt` | Thấp | TC-HAB-05, TC-HAB-24, TC-HAB-25 | ✅ Đã sửa |
| 10 | ~~Không hiển thị `error` ở màn Habits (màn Camera đã có thẻ lỗi)~~ | `HabitsScreen`, `HabitsViewModel` — nay dùng chung `presentation/ui/component/ErrorStateCard.kt` (thẻ lỗi đỏ + nút đóng) | Thấp | TC-EDGE-07, TC-HAB-30 | ✅ Đã sửa |
| 11 | ~~Không validate giá trị âm khi ghi nhận~~ | `HabitsViewModel.logHabit` (chặn `value <= 0` / `NaN` / `Infinity`) + `parseLogValue` trong `HabitLogInput.kt` ("Số lượng phải lớn hơn 0") | Thấp | TC-HAB-12, TC-HAB-30 | ✅ Đã sửa |
| 12 | ~~Không chống double-submit khi lưu/tạo~~ | `AddEditHabitViewModel.saveHabit`, `AddEditHabitScreen` (nút đáy) | Thấp | TC-ADD-14, TC-ADD-27 | ✅ Đã sửa |
| 13 | ~~Passphrase SQLCipher hard-code (chưa dùng Keystore)~~ **→ Đã sửa** | `data/local/security/{DatabasePassphraseProvider,AndroidPassphraseStorage,LegacyDatabaseKeyMigration}.kt`, `di/DatabaseModule.kt`, `app/src/test/.../data/local/security/DatabasePassphraseProviderTest.kt` | **Cao (bảo mật)** | TC-DB-11…14 | ✅ Đã sửa |
| 14 | ~~`fallbackToDestructiveMigration()` → mất dữ liệu khi đổi schema~~ **→ Đã sửa** | `data/local/migration/DatabaseMigrations.kt` (mới: `MIGRATION_1_2`, `MIGRATION_2_3`, `ALL`, `DATABASE_VERSION`), `di/DatabaseModule.kt` (bỏ fallback, thêm `addMigrations`), `data/local/AppDatabase.kt`, `app/src/test/.../data/local/migration/DatabaseMigrationsTest.kt` (mới) | **Cao (dữ liệu)** | TC-DB-08, TC-DB-15, TC-ENV-03 | ✅ Đã sửa |
| 15 | ~~Widget không tự refresh~~ | `presentation/widget/HabitWidget.kt`, `data/repository/HabitRepositoryImpl.kt`, `domain/widget/HabitWidgetRefresher.kt`, `presentation/widget/HabitWidgetRefresherImpl.kt`, `di/AppModule.kt` | Trung bình | TC-WG-04, TC-WG-09 | ✅ Đã sửa |
| 16 | ~~`getRecentHealthHistoryFlow(limit)` bỏ qua tham số `limit`~~ **→ Đã sửa** (lỗi thật rộng hơn: `health_data_history` ghi **1 hàng mỗi lần sync 6 giờ**, nên `history.take(3)` là "3 lần sync" (~18 giờ) chứ không phải 3 ngày, và "nhịp tim 7 ngày" thực chất chỉ ~1,75 ngày) | `data/local/dao/health/HealthDataDao.kt` (bỏ `LIMIT 30` hard-code, nhận `:limit`), `data/repository/HealthDataRepositoryImpl.kt` (truyền `limit`), `domain/usecase/HabitRuleEngine.kt` (`latestRowPerDay()` + `areConsecutiveDays()` — gom theo **ngày**, cảnh báo cần **3 ngày liên tiếp**), `app/src/test/.../{domain/usecase/HabitRuleEnginePersonalizedTest,data/repository/HealthDataRepositoryImplTest}.kt` | Trung bình | TC-INS-18, TC-INS-19, TC-INS-30 | ✅ Đã sửa |
| 17 | ~~Nhãn danh mục "Vận động" so sánh với 5000/8000 bất kể đơn vị~~ **→ Đã sửa** | `domain/usecase/HabitRuleEngine.kt` (`analyzeExerciseLogs` tách `analyzeStepLogs` cho log đơn vị **bước** + `analyzeExerciseTargetCompletion` so **tỉ lệ `value / targetValueAtTime`** cho đơn vị khác: < 50% → HIGH, < 80% → LOW), `app/src/test/.../domain/usecase/HabitRuleEngineTest.kt` (4 test mới) | Trung bình | TC-INS-08, TC-INS-09, TC-INS-29, TC-INS-30 | ✅ Đã sửa |
| 18 | ~~Nền màn hình hard-code sáng → dark mode tương phản kém~~ **→ Đã sửa** (lỗi thật nặng hơn: `Card` để `Color.White` nên dark mode thành **thẻ trắng + chữ `onSurface` sáng ⇒ mất chữ**) | `presentation/ui/theme/{Color,Theme}.kt`, `presentation/screen/home/HomeScreen.kt`, `habits/HabitsScreen.kt`, `habits/addedit/AddEditHabitScreen.kt`, `insights/InsightsScreen.kt`, `camera/CameraScreen.kt`, `auth/LoginScreen.kt`, `presentation/navigation/AppNavigation.kt`, `presentation/ui/component/InfoActionCard.kt`, `app/src/test/.../presentation/ui/theme/ThemeSurfaceColorsTest.kt` | Thấp | TC-EDGE-01, TC-EDGE-16 | ✅ Đã sửa |
| 19 | Chưa export `release` signing / chưa cấu hình ProGuard cho Room-Glance | `app/build.gradle.kts`, `proguard-rules.pro` | Trung bình | — | |
| 20 | ~~`HabitRuleEngineTest.kt` không compile (3 helper thiếu `targetValueAtTime`) → `.\gradlew test` fail ngay ở bước compile, toàn bộ unit test không chạy được~~ | `app/src/test/.../HabitRuleEngineTest.kt` | **Cao (chặn test)** | TC-ENV-03 | ✅ Đã sửa |
| 21 | ML Kit `Image Labeling` trả **nhãn ảnh chung** ("Food", "Fruit", "Table"…) chứ không phải món ăn/dinh dưỡng → chip "kết quả nhận diện" có thể hiển thị nhãn không đúng ngữ cảnh bữa ăn | `CameraViewModel`, `image-labeling` | Trung bình | TC-CAM-07 | |
| 22 | ~~Nút **"Thêm vào nhật ký"** trong thẻ kết quả camera vẫn là TODO (chưa nối vào `habit_logs`)~~ **→ Đã sửa** | `presentation/screen/camera/CameraScreen.kt` (`CameraScreen(onAddToJournal)` → `DetectionResultCard.onClick`), `presentation/navigation/AppNavigation.kt` (`NavHostController.openHabitLogFor(category)` dùng chung với màn Phân tích), `presentation/screen/habits/HabitsScreen.kt` (Toast khi chưa có thói quen phù hợp) | Trung bình | TC-CAM-07, TC-CAM-15, TC-INS-27 | ✅ Đã sửa |
| 23 | Chưa có unit/UI test cho luồng camera (ML Kit + CameraX chỉ test được trên thiết bị) | `app/src/test`, `app/src/androidTest` | Thấp | TC-CAM-* | |
| 24 | ~~`SyncWorker` trả `Result.failure()` ngay khi `auth.currentUser == null` → không có cơ chế chờ đăng nhập, job fail vĩnh viễn~~ | `worker/SyncWorker.kt` | Thấp | TC-WM-08 | ✅ Đã sửa |
| 25 | Thẻ giấc ngủ phụ thuộc dữ liệu pha của thiết bị: nếu Health Connect chỉ trả `STAGE_TYPE_SLEEPING` (không chia pha) thì thẻ hiển thị trạng thái rỗng dù có dữ liệu ngủ | `HealthConnectDataSourceImpl.getSleepStageBreakdown` | Thấp | TC-INS-23 | Ghi nhận |
| 26 | Thẻ giấc ngủ đọc Health Connect mỗi lần mở màn hình (không cache/Room) → phải mở lại màn Phân tích mới thấy số liệu mới, không tự cập nhật realtime như 2 thẻ kia | `InsightsViewModel.loadSleepStages` | Thấp | TC-INS-24 | Ghi nhận |
| 27 | Biểu đồ nhất quán tuần lấy mẫu số là **số thói quen đang hoạt động hiện tại** → xoá/tạo thói quen sẽ làm thay đổi tỉ lệ của cả các ngày trong quá khứ (không phải snapshot tại thời điểm đó) | `GetWeeklyConsistencyUseCase` | Thấp | TC-INS-21 | Ghi nhận |
| 28 | ~~Chưa có UI **Đăng xuất** → sau khi đăng nhập (hoặc bấm "Bỏ qua") không có đường quay lại màn đăng nhập nếu không xoá dữ liệu app~~ **→ Đã sửa** | `domain/repository/AuthRepository.kt`, `data/repository/AuthRepositoryImpl.kt`, `presentation/navigation/{AuthGateViewModel,AppNavigation}.kt`, `presentation/screen/home/HomeScreen.kt`, `app/src/test/.../{presentation/navigation/AuthGateViewModelTest,data/repository/AuthRepositoryImplTest}.kt` | Thấp | TC-AUTH-15, TC-AUTH-16 | ✅ Đã sửa |
| 29 | Passphrase SQLCipher được ghi bằng `SharedPreferences.apply()` (**bất đồng bộ**) ngay trước khi Room tạo DB → nếu tiến trình bị kill trong khe thời gian đó, bản mã chưa xuống đĩa → lần sau sinh khoá khác → DB cũ không mở được nữa → `LegacyDatabaseKeyMigration` **xoá DB** ⇒ mất dữ liệu (khe rất hẹp, khó tái hiện nhưng hậu quả nặng) | `data/local/security/AndroidPassphraseStorage.kt` (`write()` nay dùng `commit()`), `data/local/security/DatabasePassphraseProvider.kt` (ghi **trước** khi trả khoá) | Thấp | TC-DB-16, TC-DB-11 | ✅ Đã sửa |

---

## 22. TC-AUTH — Màn hình đăng nhập / Đăng ký (email + mật khẩu)

> Thêm ở bản sửa lỗi mục 3 #6: app trước đây **không có màn hình đăng nhập**, nên `SyncWorker`
> luôn thiếu `uid` và trả `failure` vĩnh viễn.
>
> **Kiến trúc:** `AppNavigation` dựng một "cổng xác thực" bằng `AuthGateViewModel`
> (`SessionState = Loading | SignedOut | Guest | SignedIn`). Màn đăng nhập **không** nằm trong
> `NavHost` — khi có phiên (đăng nhập hoặc khách) app mới dựng `NavHost` với `startDestination = "home"`.
> Provider cần bật: **Firebase Console → Authentication → Sign-in method → Email/Password = Enable**
> (project hiện dùng `google-services.json` của `healthy-ai-a11cf`).

### TC-AUTH-01 — Lần mở đầu tiên khi chưa có phiên
- **Ưu tiên:** P0
- **Tiền điều kiện:** `pm clear com.healthyai.app` (không có phiên FirebaseAuth nào được lưu).
- **Bước:** 1. Mở app.
- **Kết quả mong đợi:** Màn hình **"Đăng nhập"** hiển thị: logo tròn + **HealthyAI**, 2 ô nhập **Email**, **Mật khẩu**, nút **Đăng nhập** (xanh `#2E7D32`), liên kết **"Chưa có tài khoản? Đăng ký"** và nút **"Bỏ qua, dùng ở chế độ khách"**. Không có bottom bar.

### TC-AUTH-02 — Bỏ qua để dùng ở chế độ khách
- **Ưu tiên:** P0
- **Bước:** 1. Ở màn đăng nhập → bấm **"Bỏ qua, dùng ở chế độ khách"** → 2. Kiểm tra màn Home + bottom bar → 3. Xoay màn hình → 4. Thoát hẳn app rồi mở lại.
- **Kết quả mong đợi:**
  - Vào thẳng màn **Home** với đầy đủ bottom bar; **không** quay lại màn đăng nhập khi xoay màn hình (cờ khách lưu trong `SavedStateHandle`).
  - Mở lại app sau khi thoát: hiện lại màn đăng nhập (cờ khách không được lưu vĩnh viễn) — ghi nhận là hành vi hiện tại.
  - Mọi chức năng offline (thói quen, Health Connect, widget) hoạt động bình thường.
  - Trong lúc app đang chạy ở chế độ khách, có thể quay lại màn đăng nhập bất cứ lúc nào bằng nút **Đăng xuất** ở màn Trang chủ (**TC-AUTH-15**, mục 3 #19) — **không** cần xoá dữ liệu app.

### TC-AUTH-03 — Đăng nhập thành công
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã có user trong Firebase Console (Authentication → Users); Email/Password đã bật.
- **Bước:** 1. Nhập email + mật khẩu đúng → 2. Bấm **Đăng nhập**.
- **Kết quả mong đợi:** Nút hiện spinner khi đang gửi; khi Firebase trả về user → màn đăng nhập **tự động** thay bằng màn Home (không cần bấm thêm, nhờ `AuthGateViewModel` quan sát `AuthRepository.authState`).

### TC-AUTH-04 — Sai mật khẩu
- **Ưu tiên:** P0
- **Bước:** 1. Nhập email đúng + mật khẩu sai (≥ 6 ký tự) → 2. Bấm **Đăng nhập**.
- **Kết quả mong đợi:** Thẻ lỗi đỏ ngay dưới ô mật khẩu: **"Email hoặc mật khẩu không đúng"**; viền 2 ô nhập chuyển đỏ; vẫn ở màn đăng nhập; không có crash. Bấm icon **X** ở thẻ lỗi để đóng (hoặc gõ lại vào ô nhập, lỗi cũng tự xoá).
- **Ghi chú:** Firebase trả `ERROR_INVALID_CREDENTIALS`; message gốc của Firebase **không** được hiển thị thẳng cho người dùng.

### TC-AUTH-05 — Validate phía client (không gọi Firebase)
- **Ưu tiên:** P1
- **Bước:** Lần lượt nhập rồi bấm **Đăng nhập**:
  1. Email trống → 2. Email `alex.healthyai.app` (thiếu `@`) → 3. Mật khẩu 5 ký tự → 4. Bấm khi cả 2 ô trống.
- **Kết quả mong đợi:** Lần lượt **"Vui lòng nhập email"**, **"Email không hợp lệ"**, **"Mật khẩu phải có ít nhất 6 ký tự"**, và nút **Đăng nhập bị disable** khi còn ô trống. Không có request nào được gửi lên Firebase.

### TC-AUTH-06 — Chuyển chế độ Đăng nhập ⇄ Đăng ký
- **Ưu tiên:** P1
- **Bước:** 1. Nhập email + mật khẩu → 2. Bấm **"Chưa có tài khoản? Đăng ký"**.
- **Kết quả mong đợi:** Tiêu đề đổi thành **"Tạo tài khoản"**, nút đổi thành **Đăng ký**, liên kết đổi thành **"Đã có tài khoản? Đăng nhập"**; **email được giữ nguyên**, **mật khẩu bị xoá** (tránh lưu sai mật khẩu giữa 2 chế độ).

### TC-AUTH-07 — Đăng ký email đã tồn tại
- **Ưu tiên:** P1
- **Tiền điều kiện:** Email đã có user trong Firebase.
- **Bước:** 1. Chuyển sang **Đăng ký** → 2. Nhập email đã tồn tại + mật khẩu ≥ 6 ký tự → 3. Bấm **Đăng ký**.
- **Kết quả mong đợi:** Thẻ lỗi **"Email này đã được đăng ký, hãy đăng nhập"**.

### TC-AUTH-08 — Provider Email/Password chưa bật trên Firebase Console
- **Ưu tiên:** P1
- **Tiền điều kiện:** Authentication → Sign-in method → **Email/Password = Disabled**.
- **Bước:** 1. Nhập email + mật khẩu hợp lệ → 2. Bấm **Đăng nhập**.
- **Kết quả mong đợi:** Thẻ lỗi **"Phương thức Email/Mật khẩu chưa được bật trên Firebase Console"** (`ERROR_OPERATION_NOT_ALLOWED`); app không crash.

### TC-AUTH-09 — Mất mạng khi đăng nhập
- **Ưu tiên:** P1
- **Bước:** 1. Bật Airplane mode → 2. Nhập email + mật khẩu → 3. Bấm **Đăng nhập**.
- **Kết quả mong đợi:** Thẻ lỗi **"Không có kết nối mạng, vui lòng thử lại"**; nút trở lại trạng thái bấm được; không crash, không treo spinner.

### TC-AUTH-10 — Hiện / ẩn mật khẩu
- **Ưu tiên:** P2
- **Bước:** 1. Nhập mật khẩu → 2. Bấm icon con mắt ở ô mật khẩu → 3. Bấm lại lần nữa.
- **Kết quả mong đợi:** Lần 1 hiện mật khẩu dạng chữ thường (icon `VisibilityOff`), lần 2 che lại bằng dấu chấm (icon `Visibility`).

### TC-AUTH-11 — Chống bấm gửi 2 lần
- **Ưu tiên:** P2
- **Bước:** 1. Với mạng chậm (hoặc mạng bị chặn), bấm **Đăng nhập** liên tiếp thật nhanh nhiều lần.
- **Kết quả mong đợi:** Chỉ **1** request được gửi (`LoginViewModel.submit()` return sớm khi `isSubmitting = true`); nút hiện spinner và bị disable; ô nhập bị disable trong lúc gửi.

### TC-AUTH-12 — Phiên đã lưu còn hiệu lực
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã đăng nhập thành công ở TC-AUTH-03.
- **Bước:** 1. Thoát hẳn app (swipe khỏi recents) → 2. Mở lại app.
- **Kết quả mong đợi:** Vào thẳng **Home** (FirebaseAuth tự lưu phiên); màn đăng nhập **không** xuất hiện. Trong lúc chờ FirebaseAuth đọc phiên, app hiện splash HealthyAI + vòng xoay xanh rất ngắn.

### TC-AUTH-13 — Unit test JVM cho màn đăng nhập
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*LoginViewModelTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass: validate email/mật khẩu, gọi đúng `signIn`/`signUp` với email đã `trim()`, chế độ Đăng ký, xoá lỗi khi gõ lại, chống double-submit, và dịch 5 nhóm lỗi Firebase sang tiếng Việt.
- **Ghi chú:** Exception của Firebase được tạo bằng MockK trong test vì constructor của `FirebaseException` gọi `android.text.TextUtils` (không có trên JVM).

### TC-AUTH-14 — Đăng nhập không làm mất dữ liệu cục bộ
- **Ưu tiên:** P1
- **Bước:** 1. Ở chế độ khách, tạo 2 thói quen + ghi log → 2. Thoát app → 3. Xoá dữ liệu app (`pm clear`) → mở lại → 4. Đăng nhập ở màn đầu tiên → 5. Kiểm tra tab Thói quen.
- **Kết quả mong đợi:** Room/SQLCipher làm việc độc lập với phiên đăng nhập: sau `pm clear` (bước 3) DB rỗng như thiết kế; ở lần mở khác, đăng nhập **không** xoá hay ghi đè dữ liệu thói quen đang có trên máy.

### TC-AUTH-15 — Nút **Đăng xuất** ở màn Trang chủ (mục 3 #19)
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã đăng nhập (TC-AUTH-03) **hoặc** đang ở chế độ khách (TC-AUTH-02), và đã tạo ít nhất 1 thói quen + ghi 1 log.
- **Bước:**
  1. Mở tab **Trang chủ** → bấm icon **Đăng xuất** (góc phải phần lời chào, cạnh avatar) → bấm **Huỷ**.
  2. Bấm lại icon → bấm **Đăng xuất**.
  3. Kiểm tra app quay về màn đăng nhập → bấm **"Bỏ qua, dùng ở chế độ khách"** → mở tab **Thói quen**.
- **Kết quả mong đợi:**
  - Icon **Logout** nằm ở **góc phải trên** của lời chào (ngay trên avatar), `contentDescription = "Đăng xuất"`, màu `colorScheme.primary`.
  - Bấm icon → hiện **hộp thoại xác nhận**: tiêu đề **"Đăng xuất"**, nội dung *"Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên."*, 2 nút **Huỷ** và **Đăng xuất**. Bấm **Huỷ** → vẫn ở Home, phiên **không** đổi (không quay về màn đăng nhập).
  - Bấm **Đăng xuất** → app chuyển ngay về **màn đăng nhập** (không còn bottom bar). Áp dụng cho **cả hai** trường hợp: đã đăng nhập Firebase **và** chế độ khách (cờ khách bị xoá nên màn đăng nhập mới hiện lại được).
  - **Dữ liệu cục bộ còn nguyên**: vào lại app (khách hoặc đăng nhập lại) → tab Thói quen hiển thị đúng các thói quen/log đã tạo ở tiền điều kiện; không có Toast/dialog lỗi nào xuất hiện.
  - Đăng nhập lại bằng tài khoản cũ vẫn vào được app bình thường (cổng xác thực không bị "kẹt" ở trạng thái đã đăng xuất).
- **Ghi chú:** Trước bản sửa lỗi #19 **không** có icon này — muốn đổi phiên phải xoá dữ liệu app (`pm clear`). Đăng xuất **không** xoá file DB mã hoá (`healthy_ai_encrypted.db`).

### TC-AUTH-16 — Unit test JVM cho luồng đăng xuất
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*AuthGateViewModelTest" --tests "*AuthRepositoryImplTest"` (repo chưa commit wrapper — xem mục 2.2; JDK 21).
- **Kết quả mong đợi:** **7 test** pass (**6** trong `AuthGateViewModelTest` + **1** trong `AuthRepositoryImplTest`), chốt các tính chất của bản sửa lỗi:
  - chưa có phiên → `SessionState.SignedOut`; có uid → `SignedIn`; bấm "Bỏ qua" → `Guest`;
  - **đăng xuất khi đang ở chế độ khách** → xoá cờ khách → `SignedOut` (nếu không xoá cờ thì `combine` vẫn trả `Guest` và màn đăng nhập không bao giờ hiện lại);
  - **đăng xuất khi đã đăng nhập** → gọi `AuthRepository.signOut()` **đúng 1 lần** → `SignedOut`;
  - **đăng nhập lại sau khi đăng xuất** → `SignedIn` trở lại (cổng xác thực không bị kẹt);
  - `AuthRepositoryImpl.signOut()` uỷ quyền cho `FirebaseAuth.signOut()` và trả `Result.success`.
- **Ghi chú:** `AuthGateViewModelTest` dùng `AuthRepository` giả trong bộ nhớ (`MutableStateFlow` cho `authState`) + `SavedStateHandle()` thật; `AuthRepositoryImplTest` mock `FirebaseAuth` bằng MockK vì JVM không có Firebase/Android. Kiểm tra bằng mắt (icon + hộp thoại) vẫn theo **TC-AUTH-15**.

---

## 23. Hướng dẫn nhanh: 30 phút test "happy path"

1. `pm clear com.healthyai.app` → mở app (**TC-INIT-01**) → ở màn đăng nhập bấm **"Bỏ qua, dùng ở chế độ khách"** (**TC-AUTH-01**, **TC-AUTH-02**).
2. Bấm lần lượt 4 tab, xác nhận không crash (**TC-ENV-05**, **TC-NAV-01**). Ở Trang chủ, nếu thấy thẻ **Cấp quyền Health Connect** → bấm và bật đủ 4 quyền, xác nhận thẻ biến mất và số liệu được nạp lại (**TC-HC-13**).
3. Tab Thói quen → FAB `+` → tạo "Uống nước / 2000 ml / Cả ngày / Hàng ngày" (**TC-ADD-03**).
4. Tạo thêm "Ngủ đủ giấc / 8 giờ / Tối", "Chạy bộ / 30 phút / Sáng" (**TC-ADD-02**, **TC-ADD-03**).
5. Ghi 1000 ml cho Uống nước → kiểm tra tiến độ "1.0L / 2.0L", Toast **"Đã ghi nhận: Uống nước"** và ô nhập mặc định là **`2000`** (không phải `2000.0`) (**TC-HAB-05**, **TC-HAB-24**, **TC-HAB-26**).
6. Ghi tiếp 1000 ml → kiểm tra trạng thái "Hoàn thành" (**TC-HAB-07**).
7. Vào sửa "Chạy bộ", kiểm tra ô mục tiêu điền sẵn **`30`** (không còn đuôi `.0` — **TC-ADD-23**), đổi mục tiêu thành 45, lưu (**TC-ADD-09**).
8. Mở tab Phân tích: kiểm tra điểm sinh lực, thẻ gợi ý nước, biểu đồ nhất quán tuần (cột hôm nay có dữ liệu) (**TC-INS-13**, **TC-INS-02**, **TC-INS-21**). Bấm nút trong thẻ gợi ý **"Ghi nhận uống nước"** → xác nhận app nhảy sang tab Thói quen với hộp thoại ghi nhận mở sẵn (**TC-INS-11**). Ghi 5 log `10` phút cho **"Chạy bộ / 30 phút"** → quay lại tab Phân tích, xác nhận gợi ý vận động nói **theo phút** (không phải "10 bước") (**TC-INS-29**).
9. Mở tab Ghi chép: cấp quyền Camera, quan sát preview (**TC-CAM-01**, **TC-CAM-04**) → chụp một món ăn → chờ thẻ "Kết quả nhận diện" rồi bấm **"Thêm vào nhật ký"** → xác nhận hộp thoại ghi nhận của thói quen **Dinh dưỡng** mở sẵn, lưu được log + Toast "Đã ghi nhận" (**TC-CAM-07**, **TC-CAM-15**).
10. Đóng app hoàn toàn → mở lại → kiểm tra dữ liệu còn nguyên (**TC-DB-03**) và ghi nhận hành vi tiến độ (**TC-HAB-18**).
11. Thêm widget ra màn hình chính (**TC-WG-01**) → xác nhận dòng **`x/y thói quen đạt mục tiêu`** + danh sách tối đa 3 dòng khớp dữ liệu vừa ghi, thói quen **của buổi hiện tại** nằm đầu danh sách (**TC-WG-03**, **TC-WG-12**) → tạo thêm 1 thói quen mới → quay về màn hình chính, xác nhận widget **tự cập nhật** (số `y` trong `x/y` tăng) mà **không** cần gỡ/thêm lại (**TC-WG-04**, **TC-WG-09**, **TC-WG-10**).
12. Run `daily_reminder_worker` bằng Background Task Inspector (**TC-WM-04**) — nếu máy Android 13+ và chưa cấp quyền thông báo, bấm **"Bật thông báo"** ở thẻ trên Home trước (**TC-WM-13**).
13. Ở **Trang chủ** bấm icon **Đăng xuất** (góc phải lời chào) → **Đăng xuất** → xác nhận app quay về màn đăng nhập (**TC-AUTH-15**) → bấm **"Bỏ qua, dùng ở chế độ khách"** → mở tab Thói quen, xác nhận dữ liệu vẫn còn nguyên.
14. **Bước thêm cho bản này — nâng cấp không mất dữ liệu:** cài **đè** APK mới lên bản cũ (không `pm clear`) rồi mở app → thói quen/nhật ký và tiến độ hôm nay phải còn nguyên (**TC-DB-15**, **TC-REG-22**). Trước khi phát hành, chạy `.\gradlew test` và xác nhận `DatabaseMigrationsTest` + TC-ENV-03 (**22 class / 138 test**) pass.

---

**Hết tài liệu.**
