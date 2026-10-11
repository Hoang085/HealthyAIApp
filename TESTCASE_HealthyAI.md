# TESTCASE — HealthyAI App (Android)

> Tài liệu test case chức năng / dữ liệu / nền (background) cho toàn bộ luồng của app HealthyAI.
> Nguồn đối chiếu: mã nguồn tại `E:\HealthyAIApp` — branch `feature/dev-ui`, commit `91c1b1b`
> (`dev logic setup app and connet heath, firebase`).

| Thông tin | Giá trị |
|:--|:--|
| Phiên bản tài liệu | 1.0 |
| Ngày tạo | 2026-09-17 |
| Ngày cập nhật gần nhất | 2026-09-30 — **(mới nhất)** **Widget màn hình chính đổi hẳn thành "Sức khỏe hôm nay": bước chân · giấc ngủ · nhịp tim** (mục 3 #15, #16, #20; **mục 15 viết lại**, TC-WG-01…TC-WG-13): widget cũ (thanh tiến độ + tối đa 3 dòng thói quen) bị **thay hẳn** vì khung **150×140dp** phải nhồi tiêu đề + dòng `x/y` + thanh tiến độ + danh sách nên rất chật, và màu lấy từ `GlanceTheme` — mặc định là **màu động** của hệ thống nên trên Android 12+ hiện **tông tím**, lệch bản sắc xanh của app. Widget mới: **3 cột** chỉ số của hôm nay, mỗi cột **icon vector riêng** (thêm `res/drawable/ic_widget_{steps,sleep,heart}.xml` + **bản `drawable-night/`** cho chế độ tối) + **số đo cỡ lớn đậm** + **nhãn nhỏ đúng màu nhấn** (xanh lá / xanh ngọc / đỏ), dưới dòng **"Healthy AI · Hôm nay"** có đường kẻ mảnh; màu lấy từ palette app ở `ui/theme/Color.kt` qua `ColorProvider(sáng, tối)` (**không** còn dùng `GlanceTheme`); khung mặc định nới lên **`minWidth 250dp` / `minHeight 130dp`**, bo góc 24dp, **`sizeMode = SizeMode.Exact`** nên kéo rộng là ba cột nở ra theo thay vì bị launcher phóng to; đọc dữ liệu **một lần** lúc vẽ với **thứ tự ưu tiên nguồn giống hệt** thẻ "Tóm tắt sức khỏe" ở Trang chủ — lớp mới `domain/usecase/health/TodayHealthSummary.kt` (`build()` hàm thuần, dùng chung nên widget và app **không thể lệch số**): bước chân ưu tiên **cảm biến của máy**, giấc ngủ/nhịp tim ưu tiên **Health Connect**; chưa có số nào thì hiện **`--` / `-- bpm`** thay vì `0`; bước chân đọc bằng `StepCounterRepository.readTodaySteps(NO_WAIT_MS)` (**không chờ mẫu cảm biến** — chờ tới 2 giây sẽ làm launcher hiện khung trắng). Vẽ lại widget nay gắn với **dữ liệu sức khỏe** thay vì thói quen: cổng `domain/widget/HabitWidgetRefresher` → **`HealthWidgetRefresher`**, `HealthDataRepositoryImpl` gọi `refresh()` sau `syncHealthData()` và `saveMeasuredHeartRate()` cho nên đo nhịp tim xong là widget đổi số **ngay** (TC-WG-04); `HabitRepositoryImpl` **bỏ** `refresh()` vì nội dung widget không còn phụ thuộc thói quen (TC-WG-13). Định dạng số dùng chung chuyển vào `presentation/ui/format/NumberDisplay.kt` (**`formatStepsExact`** — số bước đầy đủ, dấu **chấm** phân cách nghìn — cùng `formatSleepDuration` / `formatHeartRate` trước đây là hàm `private` trong `HomeScreen`), nhờ vậy widget và màn Trang chủ viết số giống nhau. Xoá **dead code**: `TodayHabitProgress.kt` (+ 7 test) cùng `HabitWidget`, `HabitWidgetRefresherImpl`, `habit_widget_info.xml`, 4 chuỗi `widget_*` cũ; thêm `health_widget_info.xml`, nhãn widget **"Sức khỏe hôm nay"** (`@string/widget_label`) và **7 test** cho `TodayHealthSummary` + **4 test** cho "ghi dữ liệu sức khỏe → vẽ lại widget" + **6 test** định dạng số. Kiểm chứng: **`testDebugUnitTest` — 40 class / 430 test pass** (bỏ 7 test `TodayHabitProgressTest`, thêm 17 test mới). Test case: **mục 15 TC-WG-01…TC-WG-13 viết lại** (thêm TC-WG-02 "hiện `--`", TC-WG-12 dark mode, TC-WG-13 "đổi thói quen không còn vẽ lại widget"), **TC-REG-16** mới; mục 3 (#15, #16, #20), TC-ENV-08, phụ lục A, phụ lục B, phụ lục C (#15), mục 23 (bước 11) cập nhật. Trước đó cùng ngày — **(mới nhất cũ)** **Đếm bước chân chỉ còn tính khi app đang chạy** (mục 3 #44): cảm biến `TYPE_STEP_COUNTER` trả **giá trị luỹ kế từ lúc máy khởi động** và **vẫn đếm** cả lúc app không mở, mà bản cũ lấy thẳng hiệu số (thậm chí **suy diễn ngược**: chưa có dữ liệu + máy khởi động trong hôm nay ⇒ `baseline = 0`; sang ngày mới ⇒ **nội suy** mốc 00:00) nên mở app là số bước **"tự nhảy"** thêm theo phần đã đi trong lúc app không chạy. Nay `domain/usecase/steps/StepCounterMath.kt` **chỉ tính phần quan sát được trong lúc app đang chạy**: `StepCounterStore` sinh **một `sessionId` cho mỗi tiến trình app** (prefs `healthy_ai_step_counter`, khoá mới `session_id` — **không** cần migration), `sample()` nhận `sessionId` thay cho `bootTimeMs` (tham số này cùng `deviceBootTimeMs()`/`SystemClock` đã **xoá** vì hết cần). **Phiên đổi** ⇒ `baseline = cumulative` (đếm lại từ mẫu hiện tại) nhưng **giữ `offset`** = số đã đếm trong ngày; **nội suy mốc 00:00 chỉ dùng khi hai mẫu thuộc cùng một phiên**. Hệ quả: app **đang mở** hoặc **còn nằm trong tác vụ** (chạy nền) → đếm tiếp; **đóng hẳn app / tiến trình bị thu hồi** → khoảng thời gian ở giữa **không** được cộng vào, nhưng số đã đếm trong ngày **vẫn giữ**; dữ liệu bản cũ (thiếu khoá `session_id`) bị coi là phiên khác nên lần mở đầu sau khi cập nhật chỉ bắt đầu đếm từ mốc đó. Mô tả thẻ xin quyền (`step_counter_description`) đã nói rõ luật này. Kiểm chứng: **`testDebugUnitTest` — 40 class / 420 test pass** (`StepCounterMathTest` 10 → **12 test**). Test case: **mục 28 TC-STEP-01…TC-STEP-10 viết mới** (trước đây `TC-STEP-xx` chỉ được tham chiếu mà **chưa có** test case nào), **TC-REG-32** mới; mục 3 (#44), TC-ENV-03, phụ lục A, phụ lục B, mục 23 (bước 22) cập nhật. Trước đó cùng ngày — **(mới nhất cũ)** **Màn "Ghi chép bữa ăn": AI trả lời NHANH HƠN, có hộp thoại "Đang phân tích món ăn", BỎ công tắc AI, và nút "Xem chính sách bảo mật" chuyển sang tab Phân tích AI** (mục 3 #43): (a) **nhanh hơn** — model mặc định đổi sang **`gemini-3.8-flash`** và request thêm `generationConfig.thinkingConfig.thinkingLevel = "low"`, vì Gemini 3 **mặc định suy luận mức `high`** (đo thật: một lượt đọc ảnh mất **~42 giây**); `"minimal"` **bị model này từ chối** (`400 INVALID_ARGUMENT` — *"Thinking level MINIMAL is not supported for this model"*) nên `"low"` là mức thấp nhất dùng được, và nếu model không hiểu `thinkingConfig` thì lớp gọi **tự gửi lại đúng request đó mà bỏ tham số** (nhận biết theo nội dung thông báo lỗi — `isThinkingConfigRejected`); (b) **model dự phòng** `gemini-flash-lite-latest` (`gemini.fallback.model` trong `local.properties` + `BuildConfig.GEMINI_FALLBACK_MODEL` + `AiModule`): Google trả **503 "high demand"** / **429 "exceeded your current quota"** rất thường xuyên ⇒ model chính chạy trước, hỏng vì quá tải thì **chuyển sang model dự phòng**, **mỗi model chỉ gọi 1 lần** (trước đây thử lại y nguyên model vừa hỏng — gần như luôn hỏng tiếp) và nghỉ giữa hai model **1,5 s → 0,6 s**; lỗi **không** tự khỏi (401/403) vẫn không đổi model; (c) **bỏ công tắc "Phân tích món ăn bằng AI"** cùng 2 dòng ghi chú *"Đang BẬT/TẮT…"* — gỡ luôn **cả cổng opt-in** ở tầng dữ liệu (`SettingsRepository.observeFoodAiEnabled`/`setFoodAiEnabled`, `KEY_FOOD_AI_ENABLED`, `CameraUiState.isFoodAiEnabled`, 4 test trong `SettingsRepositoryImplTest`) ⇒ bấm **"Phân tích món ăn"** / **"Chọn ảnh từ thư viện"** là ảnh **được gửi đi phân tích luôn**; hai nút chỉ còn bị khoá khi `isAnalyzing`; (d) **hộp thoại "Đang phân tích món ăn"** (`AnalyzingDialog`: vòng xoay + tiêu đề + dòng mô tả, `dismissOnBackPress = false` / `dismissOnClickOutside = false`) hiện ngay khi `isAnalyzing` và **tự đóng** khi có kết quả hoặc lỗi — trước đây người dùng chỉ thấy chữ "Đang phân tích..." trên một nút nằm giữa màn dài nên dễ tưởng app treo; (e) nút **"Xem chính sách bảo mật"** **chuyển từ màn Ghi chép sang tab Phân tích AI** — thẻ mới **"AI & quyền riêng tư"** ở cuối `InsightsScreen` (`onOpenPrivacyPolicy` nối trong `AppNavigation`), và `privacy_intro` / `privacy_camera_body` / `privacy_no_sale_body` viết lại cho khớp (ảnh rời thiết bị khi người dùng **chủ động bấm phân tích** — không còn "mặc định TẮT"). Kiểm chứng: **`gradle clean assembleDebug` + `testDebugUnitTest` đều chạy lại — 40 test class / 418 test pass, APK debug build OK** (trước là 40/416: `GeminiFoodInsightSourceTest 20 → 28`, `CameraViewModelTest 17 → 15` (bỏ 3 test cổng opt-in, thêm 1 test "bấm là gọi AI"), `SettingsRepositoryImplTest 16 → 12` (bỏ 4 test cổng AI)). Test case mới: **TC-CAM-25, TC-CAM-26, TC-INS-42**; TC-CAM-05, TC-CAM-06, TC-CAM-07, TC-CAM-09, TC-CAM-11, TC-CAM-17, TC-CAM-21 (đánh dấu bỏ), TC-CAM-22, TC-CAM-23, TC-CAM-24, TC-PRIV-01, TC-PRIV-04, TC-REG-30, TC-REG-31, TC-ENV-03, mục 3 (#43), phụ lục A, phụ lục B, mục 23 cập nhật. Trước đó, 2026-09-29 — **(mới nhất cũ)** **Màn "Ghi chép bữa ăn": bỏ ML Kit, dùng AI (Gemini) — biết đây là món gì + lợi ích/điều nên lưu ý cho cơ thể** (mục 3 #42, phụ lục C #49): **gốc lỗi** *"chụp đĩa cơm mà ra pizza"* là `ImageLabeling` mặc định của ML Kit chỉ là bộ gán **nhãn ảnh chung** (~400 nhãn tiếng Anh, **không** phải model món ăn) nên không phân biệt được cơm vs pizza, và kể cả khi đúng thì cũng chỉ có **tên món** — không trả lời được *"ăn vào lợi/hại gì"*. Nay: (1) **gỡ hẳn ML Kit** — xoá `data/vision/ImageLabelSource.kt`, `data/vision/MlKitImageLabelSource.kt`, `domain/usecase/food/FoodRecognition.kt` và `FoodRecognitionTest` (15 test), gỡ dependency `mlkit-image-labeling` + `camerax-mlkit-vision`; (2) thay bằng cổng `data/vision/FoodInsightSource.kt` → `GeminiFoodInsightSource` gọi **`POST …/v1beta/models/{model}:generateContent`** với header **`x-goog-api-key`**, ảnh **JPEG base64** trong `inline_data`, `responseMimeType=application/json` + **`responseSchema`** nên đọc được bằng máy thay vì đoán văn xuôi (HTTP tự viết trên `HttpURLConnection`, **không** thêm OkHttp; chỉ thêm **Gson 2.10.1** để parse vì là thư viện Java thuần ⇒ unit test JVM chạy được); (3) **prompt** `domain/usecase/food/FoodAnalysis.kt` (`buildPrompt()`) yêu cầu **tiếng Việt**, ưu tiên **món Việt/Á**, phủ cả **trái cây, đồ uống, đồ ăn vặt**, **bắt ghi "(có thể)"** khi không chắc (chống đúng lỗi cũ ở phía model), và **CẤM chẩn đoán/chữa bệnh**; `toInsight()` **làm sạch** dữ liệu AI trả về (≤ **3** gạch đầu dòng/mục, ≤ **120** ký tự/gạch, bỏ mục rỗng/trùng, emoji mặc định 🍽️); (4) **UI mới**: thẻ kết quả gồm **tên món** + mục **"Có lợi cho cơ thể"** + mục **"Nên lưu ý"** + dòng **miễn trừ trách nhiệm** ("thông tin tham khảo, KHÔNG phải tư vấn y tế"), thẻ lỗi có nút **"Thử lại"** (giữ lại ảnh của lượt trước nên không phải chụp/chọn lại), và mỗi loại lỗi ra một câu riêng: chưa cấu hình khoá / mất mạng / **AI quá tải (429, 503)** / khoá sai (401, 403) / máy chủ lỗi (5xx) / phản hồi không đọc được — **tự thử lại 1 lần** với 429/503/5xx/mất mạng; (5) **riêng tư**: vì ảnh **rời khỏi thiết bị**, thêm cổng **opt-in** `food_ai_enabled` trong `app_settings` (**mặc định TẮT**, cùng khuôn mẫu `CloudSyncPolicy` mục 3 #31) — chưa bật thì hai nút phân tích **bị khoá** và app **không gửi ảnh đi đâu**; `privacy_camera_body` viết lại cho đúng sự thật mới; (6) **khoá & tên model** đọc từ `local.properties` (đã bị `.gitignore`, **không** vào git) qua `BuildConfig.GEMINI_API_KEY`/`GEMINI_MODEL` (`buildConfig = true` đã bật) — **không hardcode model** vì đã gặp thật: `gemini-2.5-flash` bị Google trả **404 "no longer available to new users, use models/gemini-3.8-flash"**, còn model mới thì trả **503 "high demand"** lặp 5 lần liên tiếp. Kiểm chứng: **`testDebugUnitTest` + `assembleDebug` đều chạy lại — 40 test class / 416 test pass, APK debug build OK** (trước là 39/382: `−FoodRecognitionTest 15`, `+FoodAnalysisTest 17`, `+GeminiFoodInsightSourceTest 20`, `SettingsRepositoryImplTest +4 = 16`, `CameraViewModelTest 9 → 17`). Test case mới: **TC-CAM-21, TC-CAM-22, TC-CAM-23, TC-CAM-24, TC-PRIV-04**; mục 1.1, 2.1, 2.2, 3 (#42), TC-CAM-05…20, TC-PRIV-01, phụ lục A, phụ lục B, phụ lục C (#49), TC-REG-30, mục 23 cập nhật. Trước đó, 2026-09-29 — **(mới nhất cũ)** **Tab Phân tích: feed "BLOOM AI GỢI Ý" nay là BANNER TRƯỢT TỰ ĐỘNG** (mục 3 #41, phụ lục C #48): trước đây mỗi gợi ý là **một thẻ xếp dọc** (tối đa 4 thẻ liền nhau) nên màn Phân tích **quá dài**; nay toàn bộ feed nằm trong **một banner cao cố định 240dp** chỉ hiện **một gợi ý mỗi lúc**, **tự trượt** sang gợi ý kế tiếp sau **5 giây** — `InsightsScreen` thêm `AiTipBanner` dùng `HorizontalPager` + `rememberPagerState(pageCount = { Int.MAX_VALUE })` (lấy `page % suggestions.size` nên banner **luôn trượt tới trước**, không quay ngược về đầu khi hết danh sách), **tạm dừng tự cuộn lúc người dùng đang vuốt**, có **chấm chỉ báo** trang; `AiTipCard` đổi `fillMaxWidth()` → `fillMaxSize()` + `maxLines`/`TextOverflow.Ellipsis` + ghim nút hành động ở đáy (`Spacer(weight(1f))`) nên banner **không nhảy** chiều cao khi trượt giữa gợi ý có/không có nút, và nút **"Ghi nhận uống nước"**… vẫn bấm được ở gợi ý đang hiện. Bộ mẹo vẫn do `HabitRuleEngine` **xoay vòng theo ngày** (mục 3 #27) nên **cùng ngày ổn định, sang ngày mới đổi mẹo và không trùng nhau** — nay **khoá thêm bằng 2 test JVM** trong `HabitRuleEngineTest` (vòng xoay đi hết kho mẹo mới lặp lại; mỗi danh mục phủ hết kho `GENERAL`). Kiểm chứng: **`gradle testDebugUnitTest` + `assembleDebug` đều chạy lại — 39 test class / 382 test pass, APK debug build OK**. Test case mới: **TC-INS-40, TC-INS-41**; TC-ENV-03, mục 3 (#41), phụ lục A, phụ lục B, phụ lục C (#48), TC-REG-29, mục 23 cập nhật. Trước đó, 2026-09-29 — **(mới nhất cũ)** **Xử lý nốt 8 mục còn lại của lượt rà soát toàn app** (#25, #26, #27, #39, #41, #42, #43, #44) + mục #19/#38 **chốt là không sửa** vì app **không phát hành** (làm thử nghiệm) — xem **mục 3 #33…#40** và **mục 27. TC-FIX2**: (1) **#41 tầng i18n**: toàn bộ chuỗi UI tiếng Việt trong `presentation/**` + nhãn widget + kênh thông báo nay nằm ở **`res/values/strings.xml`** (**221 string**, trước là **2**) và được đọc bằng `stringResource(...)`/`context.getString(...)` (**~215 chỗ**, trước là **0**); nhãn 4 tab bottom bar chuyển sang `Screen.titleRes` (`@StringRes`) + `R.string.nav_*`; bộ lọc màn Thói quen đổi từ **chuỗi tiếng Việt viết cứng** sang enum `HabitFilter` (khoá = enum, nhãn = tài nguyên); `AndroidManifest` dùng `@string/widget_label`. Chữ trên màn hình **không đổi** — chỉ đổi nguồn lấy chuỗi; thêm `values-en` là có tiếng Anh ngay, không cần sửa code. (2) **#39 không nuốt `CancellationException`** (14 vị trí `catch (e: Exception)`) nay có `catch (e: CancellationException) { throw e }` đứng trước, và **~18 chỗ `runCatching` trong hàm `suspend`/coroutine** đổi sang helper `util/runCatchingCancellable` (giữ tín hiệu huỷ coroutine thay vì biến thành "lỗi nghiệp vụ"/`Result.failure`). (3) **#42 hợp đồng repository**: `HealthDataRepository.getHealthData(start, end)` — hàm **bỏ qua cả hai tham số** — nay là `getTodayHealthData()` (tên nói đúng việc nó làm), và `GetHealthInsightsUseCase` đọc lịch sử sức khỏe bằng `getHealthHistorySince(fromTs)` (**cắt theo mốc thời gian**, cùng cửa sổ 7 ngày với thẻ "Lịch sử sức khỏe 7 ngày") thay vì `getRecentHealthHistoryFlow(7)` (**cắt theo số hàng** — dữ liệu cũ nhiều hàng/ngày làm luật "3 ngày liên tiếp" **âm thầm không chạy**). (4) **#43 tab Phân tích không còn im lặng**: `InsightsUiState` có `error` + `sleepStagesError` (+ `visibleError`) và màn hình hiện `ErrorStateCard` có nút đóng; `HealthSyncWorker` **không** còn luôn trả `success` — đồng bộ lỗi thì `retry` (tối đa 3 lần) rồi `failure` nên backoff thật sự chạy; thêm **`InsightsViewModelTest` (6 test)** là ViewModel duy nhất trước đây **không có test**. (5) **#25 thẻ giấc ngủ**: máy chỉ báo `STAGE_TYPE_SLEEPING` (không chia pha) **không còn** ra thẻ rỗng — `SleepStageBreakdown` thêm `unstagedMinutes` + `hasStages`, UI hiện dòng *"Health Connect chỉ báo tổng thời gian ngủ …"*. (6) **#26 thẻ giấc ngủ tự đọc lại**: `LifecycleResumeEffect` gọi `InsightsViewModel.refresh()` mỗi lần **quay lại** màn Phân tích (trước đây chỉ đọc 1 lần lúc mở app). (7) **#27 biểu đồ nhất quán tuần**: mẫu số nay là **snapshot từng ngày** (`habit.createdAt < cuối ngày`) nên **tạo** thói quen hôm nay **không** làm tụt tỉ lệ 6 ngày trước và **xoá** thói quen **không** làm tỉ lệ quá khứ vọt lên. (8) **#44 manifest chỉ xin quyền ĐỌC Health Connect** (`WRITE_STEPS`/`WRITE_SLEEP` bị bỏ — code chỉ đọc, xem `HealthConnectDataSourceImpl.requiredPermissions`). Kiểm chứng: **`gradle testDebugUnitTest` + `assembleDebug` đều chạy lại — 39 test class / 380 test pass, APK debug build OK**. Test case mới: **TC-ENV-08, TC-INS-37, TC-INS-38, TC-INS-39, TC-WM-17** (`+ InsightsViewModelTest 6, SleepStageBreakdownTest 3, GetWeeklyConsistencyUseCaseTest +2`); TC-ENV-03, mục 3 (#33…#40), phụ lục C (#19, #23, #25, #26, #27, #38, #39, #41…#44) cập nhật. Trước đó, 2026-09-29 — **(mới nhất cũ)** **Sửa 2 lỗi ưu tiên cao của lượt rà soát: cổng đồng bộ đám mây (#37) + icon launcher/backup (#40)** (mục 3 #31, #32): (1) **Nhật ký thói quen trước đây bị đẩy lên Firestore không có sự đồng ý** — mọi log mặc định `isDirty = true` và `SyncWorker` đẩy hết ngay khi có phiên đăng nhập, nên ghi nhật ký ở **chế độ khách** rồi đăng nhập là **toàn bộ lịch sử** (kể cả `note`) bị đẩy lên; repo **không có** `firestore.rules` và chính sách bảo mật chỉ là placeholder `your-privacy-policy.com` chưa nối vào UI. Nay có `CloudSyncSettings` + khoá `cloud_sync_enabled`/`cloud_sync_consent_at` trong `app_settings` (**mặc định TẮT**), `CloudSyncPolicy` (hàm thuần — **một chỗ duy nhất** quyết định log nào được tải lên: tắt/thiếu mốc đồng ý → không tải gì; đã bật → **chỉ** log tạo **sau** mốc đồng ý), thẻ **"Đồng bộ đám mây"** ở Trang chủ (công tắc mặc định TẮT + dòng nhắc cho chế độ khách), **màn "Chính sách bảo mật"** trong app mở từ thẻ này **và** khi Health Connect gửi `ACTION_SHOW_PERMISSIONS_RATIONALE`/`ACTION_VIEW_PERMISSION_USAGE` (không qua cổng đăng nhập), cùng `firestore.rules` + `firebase.json` (mỗi người dùng chỉ đọc/ghi `users/{uid}/**`); (2) **icon launcher mặc định của Android** (`sym_def_app_icon`) nay thay bằng **adaptive icon vector** (`mipmap-anydpi-v26` + `drawable/ic_launcher_foreground.xml` + `colors.xml`, nền xanh + trái tim trắng), và **`allowBackup` từ `true` → `false`** vì DB SQLCipher + prefs bị auto-backup trong khi khoá nằm ở Keystore ⇒ restore sẽ xoá sạch DB (`LegacyDatabaseKeyMigration`) và rò rỉ dữ liệu sức khỏe. Test case mới: **TC-HOME-16, TC-PRIV-01, TC-PRIV-02, TC-PRIV-03, TC-ENV-07** (+ `CloudSyncPolicyTest` **5**, `SettingsRepositoryImplTest` +5 = **12**, `HomeViewModelTest` +5 = **45** → **37 class / 369 test**), và **`assembleDebug` đã chạy lại để kiểm tra resource icon**; TC-ENV-03, mục 1.1, 3 (#31, #32), phụ lục A, phụ lục B, phụ lục C (#37, #40) cập nhật. Trước đó, 2026-09-29 — **(mới nhất cũ)** **Màn Thêm/Sửa thói quen: bỏ lựa chọn "Hàng tuần"/"Hàng tháng" (chọn được nhưng không có tác dụng)** (mục 3 #30): enum `HabitFrequency` và cột `habits.frequency` vẫn tồn tại nhưng **không logic nào đọc** (`GetTodayHabitsUseCase`, `GetWeeklyConsistencyUseCase`, `HomeViewModel`, `HabitsViewModel`, `VitalityScoreMath`, `TodayHabitProgress` đều xét theo NGÀY) ⇒ chọn xong chỉ đổi nhãn. Nay `AddEditHabitScreen` **chỉ hiện "Hàng ngày"** (radio không bấm được + dòng chú thích), `AddEditHabitViewModel` **bỏ** `frequency` khỏi UI state + bỏ `onFrequencyChange` và **luôn ghi `HabitFrequency.DAILY`** ⇒ thói quen cũ lỡ lưu `WEEKLY`/`MONTHLY` được **chuẩn hoá** khi bấm "Cập nhật". **Không cần migration.** Test case: **TC-ADD-20 viết lại**, **TC-ADD-28** mới (+1 test ở `AddEditHabitViewModelTest` → **36 class / 354 test**); TC-ENV-03, mục 3 (#30), phụ lục B, phụ lục C (#36) cập nhật. Cùng ngày, kèm theo: **rà soát toàn app** và ghi lại danh sách việc cần xử lý trước khi phát hành ở **phụ lục C #37…#44** (trạng thái *Ghi nhận*, **chưa sửa**): upload nhật ký lên Firestore không có sự đồng ý + thiếu `firestore.rules` + privacy policy chỉ là placeholder (**#37**), release thiếu `signingConfig`/ProGuard keep cho Firebase-Glance (**#38**), `catch (Exception)` nuốt `CancellationException` (**#39**), thiếu icon launcher và `allowBackup` không rules ⇒ mất DB sau restore (**#40**), chưa có i18n (**#41**), `getHealthData(start,end)` bỏ qua tham số + insights đọc theo SỐ HÀNG thay vì mốc thời gian (**#42**), `InsightsViewModel` chưa có test + nuốt lỗi, `HealthSyncWorker` luôn trả success (**#43**), manifest xin thừa quyền Health Connect (**#44**). Trước đó, 2026-09-29 — **(mới nhất cũ)** **Tab Phân tích: feed gợi ý đa dạng & xoay vòng, chỉ số sinh lực tính theo ngày + ghép dữ liệu sức khỏe, thẻ giấc ngủ không còn phụ thuộc Health Connect** (mục 3 #27, #28, #29): (1) **BLOOM AI GỢI Ý** không còn một thẻ cảnh báo cố định lặp mãi — `domain/usecase/SuggestionCatalog.kt` giữ **kho mẹo theo danh mục** (nhóm **CONCERN** / **GENERAL**) và `HabitRuleEngine.generateSuggestions()` chọn **mẹo xoay vòng theo ngày** cho các danh mục người dùng có thói quen (cộng danh mục đang có vấn đề), **tối đa 4 thẻ**, `id` duy nhất, **bỏ mức CRITICAL**; (2) **"Chỉ số sinh lực"** nay tính **theo từng ngày** (`VitalityScoreMath`) = `0,4×tỉ lệ thói quen đạt + 0,3×giờ ngủ/8 + 0,3×bước chân/8000`, chuẩn hoá theo thành phần **thật sự có nguồn**, **ngày không ghi gì = 0** ⇒ bỏ thói quen là điểm **tụt**; thiếu nguồn → `hasData = false` ("Chưa đủ dữ liệu để đánh giá"); (3) **"Xu hướng chất lượng giấc ngủ"** thêm **biểu đồ 7 đêm giờ ngủ + xếp loại** (`SleepTrend`/`SleepTrendMath`) từ `health_data_history` (gồm cả số **app tự đo bằng cảm biến**) nên **không còn "đợi"** Health Connect; khối **Sâu/Nông/REM** chỉ hiện **thêm** khi có dữ liệu pha. Test case mới: **TC-INS-34, TC-INS-35, TC-INS-36** (+ `SuggestionCatalogTest` **6**, `VitalityScoreMathTest` **10**, `SleepTrendMathTest` **4**; viết lại `HabitRuleEngineTest`, `HabitRuleEngineExtendedTest`, `HabitRuleEnginePersonalizedTest`, `HabitRuleEngineStepTrendTest`, `GetVitalityScoreUseCaseTest` → **36 class / 353 test**); TC-INS-01…TC-INS-33 (trừ TC-INS-21, TC-INS-22), mục 3 (#27, #28, #29), phụ lục B, phụ lục C (#33, #34, #35) cập nhật. Trước đó, 2026-09-28 — **(mới nhất cũ)** **nhịp tim cũng đọc lại theo NGÀY** và **lịch sử tóm tắt sức khỏe 7 ngày vào tab Phân tích** (mục 3 #26): `HealthConnectDataSource.getTodayHeartRate()` nay lấy **trung bình 00:00 → hiện tại** (trước là *1 giờ gần nhất*) và `getLatestRecordedHeartRate()` chỉ đọc hàng **`recordedAt >= 00:00 hôm nay`** — nên sang ngày mới ô **"Tim"** ở thẻ Tóm tắt sức khỏe hiện **`-- bpm`** thay vì hiện lại số của hôm qua, đúng như bước chân/giờ ngủ đọc lại từ đầu ngày; thẻ **"Tóm tắt sức khỏe"** đồng thời **bỏ 3 dòng chú thích nguồn** ở dưới (không cần giải thích chi tiết). Tab **Phân tích** thêm thẻ **"Lịch sử sức khỏe 7 ngày"**: **biểu đồ cột bước chân** + **bảng 7 dòng** (ngày · bước chân · giờ ngủ · nhịp tim) đọc từ `health_data_history` bằng `GetRecentHealthSummaryUseCase` + `HealthSummaryHistory` (hàm thuần Kotlin: mỗi ngày lấy **hàng mới nhất**, luôn trả **đủ 7 ngày** cũ → mới, ngày chưa ghi được gì hiện **"Chưa có dữ liệu"** chứ không phải một loạt số 0) — **cùng nguồn số** mà Rule Engine dùng để sinh gợi ý AI. Test case mới: **TC-HOME-15, TC-INS-31, TC-INS-32, TC-INS-33** (+`HealthSummaryHistoryTest` **8 test**, `HealthDataRepositoryImplTest` +1, `HomeViewModelTest` đổi 1 test → **33 class / 330 test**); TC-HOME-05, TC-HOME-06, TC-HOME-14, TC-HR-07, TC-EDGE-11, mục 1.1, 3 (#26), phụ lục A, phụ lục B cập nhật. Trước đó, cùng ngày — thêm **chọn ảnh từ thư viện** vào màn **"Ghi chép bữa ăn"** (mục 3 #25): nút viền **"Chọn ảnh từ thư viện"** mở **photo picker của hệ thống** (`ActivityResultContracts.PickVisualMedia`, không cần quyền đọc thư viện) → `GalleryImageLoader` đọc ảnh **thu nhỏ** (`inJustDecodeBounds` + `inSampleSize` luỹ thừa 2, cạnh dài ~**1280 px**) và **xoay theo cờ EXIF** (thêm `androidx.exifinterface:1.3.2`) rồi đưa vào đúng luồng phân tích cũ (`CameraViewModel.analyzeBitmap` → ML Kit → `FoodRecognition`) nên kết quả vẫn là **tên món tiếng Việt**; nút này **nằm ngoài nhánh quyền Camera** nên từ chối quyền camera vẫn phân tích ảnh được, thẻ xin quyền thêm dòng *"Chưa muốn cấp quyền? Bạn vẫn có thể chọn ảnh có sẵn trong thư viện."*. Người dùng **bấm Huỷ** → không báo lỗi; ảnh **không đọc được** → thẻ lỗi *"Không đọc được ảnh đã chọn: …"* (`CameraViewModel.onImageLoadError`). Test case mới: **TC-CAM-18, TC-CAM-19, TC-CAM-20** (+ 1 test ở `CameraViewModelTest` → **32 class / 321 test**), TC-CAM-01, TC-CAM-17, TC-REG-28, mục 1.1, 3 (#25), phụ lục A, phụ lục B cập nhật. Trước đó, cùng ngày — đã sửa **mục 3 #24 / phụ lục C #21**: màn **"Ghi chép bữa ăn"** không còn "nhận diện" ảnh **không phải đồ ăn** và không còn hiện **nhãn ảnh tiếng Anh** chung chung. Nay có lớp thuần Kotlin `domain/usecase/food/FoodRecognition.kt`: **chỉ nhận nhãn thuộc danh mục món ăn/nguyên liệu** (~180 từ khoá, khớp **theo từ trọn vẹn** nên "Pineapple" không thành "Apple", "Steak" không thành "Tea", "Peanut" không thành "Pea"; từ khoá **cụ thể nhất** thắng nên "Fried chicken" → `Gà rán`), **dịch sang tiếng Việt kèm emoji** (`Pizza` → `Pizza 🍕`, `Chicken` → `Thịt gà 🍗`), gộp nhãn cùng món (giữ độ tin cậy cao nhất), chỉ nhận **≥ 0.5**, tối đa **5 món**. Ảnh không có món ăn nào → `CameraUiState.noFoodDetected` → thẻ **"Chưa nhận diện được món ăn"** + gợi ý chụp lại, **không** hiện kết quả giả; thẻ kết quả đổi nhãn góc phải "Tự động" → **"Trên máy"** và thêm dòng nói rõ **chỉ là tên món, không phải số liệu dinh dưỡng**. Nguồn nhãn ML Kit nằm sau cổng `data/vision/ImageLabelSource` (impl `MlKitImageLabelSource`, ngưỡng **ứng viên** 0.3) nên `CameraViewModel` test được trên JVM (`runAnalysis` nhận **hàm lấy nhãn**). Test case mới: **TC-CAM-16, TC-CAM-17** (+ `FoodRecognitionTest` **15 test**, `CameraViewModelTest` **8 test** → **32 class / 320 test**), **TC-CAM-07, TC-CAM-08, TC-CAM-05, TC-CAM-06** cập nhật; TC-REG-27, mục 1.1, 3 (#24), phụ lục A, phụ lục B, phụ lục C (#21, #23) cập nhật. Trước đó, 2026-09-25 — **(mới nhất cũ)** thêm **module 25 / TC-HR — đo nhịp tim bằng camera + đèn flash (PPG)**: đèn flash chiếu sáng xuyên mô ngón tay, **camera sau** ghi lại sự thay đổi rất nhỏ về **sắc độ đỏ** của từng khung hình theo từng đợt máu dồn về đầu ngón tay, thuật toán (`PpgAnalyzer`: lọc mượt AC → bỏ thành phần một chiều → tìm đỉnh mạch bằng ngưỡng thích ứng + chống dội + nội suy đỉnh → **kiểm tra tính chu kỳ bằng tự tương quan** → trung vị khoảng RR) quy ra nhịp tim. Ô **"Tim"** ở thẻ "Tóm tắt sức khỏe" có nút **"Đo ngay"** mở màn **"Đo nhịp tim"** (route riêng, **không** thêm tab) với **phần hướng dẫn đổi theo tình trạng đo** (chưa thấy ngón tay → tín hiệu yếu → đang đo → sắp xong), vòng tròn tiến độ 15–30 giây, số nhịp trực tiếp và **dạng sóng mạch đập**. Kết quả **tự lưu** vào `health_data_history` (dùng lại luật gộp của `HealthSyncWorker`, **không** cần migration vì cột `heartRate` đã có) nên ô "Tim" và Rule Engine ở tab Phân tích dùng được ngay; **Health Connect vẫn được ưu tiên** khi có số, chưa có mới dùng nhịp ghi trên máy. App **không** lưu và **không** hiện số khi tín hiệu nhiễu/nhịp không đều (số liệu sức khỏe). Sửa kèm lỗi build: `com.google.guava:listenablefuture` bị Gradle chọn bản `9999.0-empty-to-avoid-conflict-with-guava` (**jar rỗng**) nên Kotlin không biên dịch được `enableTorch`/`ProcessCameraProvider.getInstance` — `app/build.gradle.kts` nay **ép** về bản `1.0`. Test case mới: **TC-HR-01…TC-HR-13** + **TC-HOME-14** (+ `HeartRateMathTest` **20 test**, `PpgAnalyzerTest` **15 test**, `HeartRateViewModelTest` **21 test**, `HealthDataRepositoryImplTest` +6, `HomeViewModelTest` +4 → **30 class / 297 test**); TC-ENV-03, mục 1.1, 2.1, 3 (#23), phụ lục A, phụ lục B cập nhật. Trước đó, 2026-09-24 — **(mới nhất cũ)** **khung giờ ngủ do người dùng đặt** (mở rộng **module 24 / TC-SLEEP**): app **không** còn tự quyết định khung đêm 18:00–12:00 — người dùng chọn **giờ đi ngủ** + **giờ thức** (chưa đặt gì thì mặc định **23:00 → 07:00**) ở thẻ **"Khung giờ ngủ"** ngay dưới thẻ "Tóm tắt sức khỏe"; hộp thoại chọn giờ của hệ thống, chặn khung rỗng (hai mốc trùng nhau) ngay trong `HomeViewModel`. Khung được lưu ở bảng Room **`app_settings`** dạng **khoá – giá trị** (`SettingsRepository` + `MIGRATION_3_4`, tăng schema **3 → 4**) nên thêm thiết lập mới sau này **không cần** migration nữa; màn Home đọc bằng `Flow` nên đổi khung là cập nhật ngay, giá trị thiếu/hỏng đều rơi về mặc định. **Khoá đêm nay nhảy tại giờ đi ngủ** (trước là 18:00 cố định) và luật "số phút hiển thị" được tách theo *đang trong khung hay không* (ngoài khung thì hiện đúng số đêm vừa kết thúc — thức trắng phải hiện `0h 0m`, không lấy số đêm cũ ra thay). Test case: **TC-SLEEP-01…TC-SLEEP-13 viết lại** (+ `SleepScheduleTest` **6 test**, `SettingsRepositoryImplTest` **7 test**, `SleepTrackerMathTest` 26, `HomeViewModelTest` 36, `DatabaseMigrationsTest` 4 → **27 class / 231 test**) + **TC-DB-17** (migration 3→4, đã kiểm chứng offline bằng `sqlite3` so `PRAGMA` với `4.json`); TC-ENV-03, TC-HOME-05, TC-HOME-06 cập nhật. Trước đó, cùng ngày — thêm **module 24 / TC-SLEEP**: app **tự tính số giờ ngủ** bằng **cảm biến của chính điện thoại** (không cần Health Connect, **không cần quyền nào**) — `SleepTrackingWorker` lấy mẫu **15 phút/lần** (ánh sáng `TYPE_LIGHT` + gia tốc `TYPE_ACCELEROMETER` + `PowerManager.isInteractive`, chỉ nghe **3 giây** rồi gỡ listener nên không cần foreground service); `SleepTrackerMath` phân loại "đang ngủ" (**khung đêm** ≥ 18:00 / < 12:00, **màn hình tắt**, phòng tối < 5 lux, bất động < 0,05g) rồi **cộng dồn toàn bộ** khoảng giữa hai mẫu "đang ngủ" liên tiếp (không mất thời gian ngủ khi WorkManager bị hoãn trong Doze); một "đêm" gắn với **ngày thức dậy** nên sang đêm mới số tự về 0; lọc dưới **45 phút**, cắt trần **12 giờ**; trạng thái lưu ở prefs `healthy_ai_sleep_tracker`. Health Connect (đồng hồ đeo tay) vẫn được **ưu tiên**, không có số thì thẻ **Tóm tắt sức khỏe** hiển thị ước lượng kèm dòng chú thích nguồn, và `HealthSyncWorker` ghi số này vào `health_data_history` cho tab Phân tích. Test case mới: **TC-SLEEP-01…TC-SLEEP-10** (+ `SleepTrackerMathTest` **23 test**, `HealthDataRepositoryImplTest` **+4** test giờ ngủ, `HomeViewModelTest` **+4** test → **25 class / 207 test**); TC-ENV-03, TC-HOME-06 cập nhật. Trước đó, cùng ngày — thêm **module TC-STEP**: app **tự đếm bước** bằng **cảm biến của chính điện thoại** (`Sensor.TYPE_STEP_COUNTER`) nên **không còn phụ thuộc Health Connect** để có số bước — thẻ **"Bật đếm bước chân"** ở Trang chủ xin quyền `ACTIVITY_RECOGNITION`, quy đổi "luỹ kế từ lúc bật máy" thành "số bước hôm nay" ở `domain/usecase/steps/StepCounterMath.kt` (nội suy mốc 00:00 sang ngày mới, giữ số đã đi khi **khởi động lại máy**), baseline lưu ở prefs `healthy_ai_step_counter`; Health Connect vẫn được **ưu tiên** khi có số, không có thì hiển thị số từ cảm biến + dòng chú thích nguồn. Test case mới: **TC-STEP-01…TC-STEP-10** (+ `StepCounterMathTest` 10 test, `HomeViewModelTest` +6 test → **23 class / 159 test**); TC-HOME-01, TC-ENV-03 cập nhật. Trước đó, 2026-09-23 — đã sửa **mục 3 #21 / phụ lục C #30**: lời chào màn Trang chủ **không còn hard-code `Text("Alex")`** — nay lấy **tên tài khoản thật** (`AuthRepository.currentUserName`: `displayName` Firebase → nếu trống thì phần trước `@` của email); ở **chế độ khách** không hiện dòng tên và câu chào bỏ dấu phẩy đuôi. Test case mới: **TC-HOME-12, TC-HOME-13** (+ 5 unit test mới → **22 test class / 143 test**), TC-HOME-01 cập nhật. Cùng ngày, trước đó: đã sửa **phụ lục C #14** (thêm Room **`Migration`** 1→2 và 2→3 trong `data/local/migration/DatabaseMigrations.kt`, **bỏ** `fallbackToDestructiveMigration()` → nâng version schema không còn xoá sạch thói quen/nhật ký), **#16** (`getRecentHealthHistoryFlow(limit)` nay tôn trọng `limit`; `HabitRuleEngine` **gom dữ liệu Health Connect theo NGÀY** thay vì theo lần sync 6 giờ), **#17** (gợi ý vận động so theo **đơn vị + mục tiêu** của chính thói quen thay vì 5 000/8 000 bất kể đơn vị) và **#22** (nút **"Thêm vào nhật ký"** ở màn Ghi chép đã nối vào `habit_logs` qua hộp thoại ghi nhận thói quen Dinh dưỡng) + hàng mới **#29** (passphrase SQLCipher ghi bằng `commit()` để không mất khoá khi tiến trình bị kill). Test case mới: **TC-DB-15, TC-DB-16, TC-INS-29, TC-INS-30, TC-CAM-15**; unit test mới `DatabaseMigrationsTest` (+ 6 test mới ở `HabitRuleEngineTest`, `HabitRuleEnginePersonalizedTest`, `HealthDataRepositoryImplTest`). Cùng ngày, trước đó: đã sửa mục 3 **#19** (thêm **nút Đăng xuất** ở màn Trang chủ: `HomeScreen`/`GreetingHeader` → `AuthGateViewModel.signOut()` xoá **cả** phiên Firebase **và** cờ chế độ khách → quay lại màn đăng nhập, **không** mất dữ liệu cục bộ) — test case mới: TC-AUTH-15, TC-AUTH-16 + 2 class test mới; phụ lục C **#28**. Cùng ngày, trước đó: đã sửa mục 3 **#18** (màu nền hard-code: nền màn hình + nền `Card`/`Surface` nay lấy từ token theme `background`/`surfaceContainerLow` thay cho `#F8F9FA`/`Color.White` → dark mode không còn thẻ trắng làm mất chữ) — test case mới: TC-EDGE-16 + unit test `ThemeSurfaceColorsTest`; phụ lục C **#18**. Cùng ngày, trước đó: **2026-09-22** — đã sửa mục 3 **#17** (passphrase SQLCipher **không còn hard-code**: sinh ngẫu nhiên 32 byte ở lần chạy đầu, bọc bằng khoá **AES-256 trong Android Keystore**, bản mã lưu ở prefs `healthy_ai_secure_store`; kèm **migration rekey** cho máy đã cài bản cũ nên **không mất dữ liệu**) — test case mới: TC-DB-11…14. Cùng ngày, rà soát lại mục 3 **#17** và chỉnh số liệu trong TC-DB-11/TC-DB-14 cho khớp code đang chạy (bản mã lưu ở prefs là **~80 ký tự** Base64 = 60 byte; `DatabasePassphraseProviderTest` có **6 test**). Cùng ngày, trước đó: mục 3 **#20** (widget vẽ thanh tiến độ **thật** + **danh sách** thói quen thay cho thanh tĩnh ~40% và 1 thói quen; quy tắc buổi chỉ còn quyết định **thứ tự**) — test case mới: TC-WG-12. Cùng ngày, trước đó: mục 3 **#16** (widget hiển thị thói quen **theo buổi hiện tại** thay vì "mới tạo nhất") + bật cập nhật định kỳ 30 phút — TC-WG-10, TC-WG-11; mục 3 **#15** (widget không tự refresh) + phụ lục C **#15** — TC-WG-09; và mục 3 **#10** (phản hồi "Đã ghi nhận"), **#11** (số mục tiêu hiển thị gọn), **#12** (validate hộp thoại ghi nhận), **#13** (chống trùng tên), **#14** (nút hành động trong thẻ gợi ý AI) + phụ lục C **#10** (hiển thị lỗi ở màn Thói quen), **#11** (chặn số âm/0 khi ghi nhận), **#12** (chống double-submit) — test case: TC-HAB-24…30, TC-ADD-23…27, TC-INS-27, TC-INS-28 |
| Người thực hiện test | .......................... |
| Build/version test | `versionCode 1`, `versionName 1.0.0` |
| Ứng dụng (package) | `com.healthyai.app` |
| Nền tảng | Android, `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Kiến trúc | MVVM + Clean Architecture (Domain / Data / Presentation), Hilt DI |
| Số module test | 15 (+ phụ lục) |

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
| `TC-CAM-xx` | Màn hình Ghi chép: chụp ảnh hoặc **chọn ảnh từ thư viện** (Camera + **AI Gemini**) — cho biết **đây là món gì** + **lợi ích / điều nên lưu ý cho cơ thể**, phủ cả **trái cây, đồ uống, đồ ăn vặt**; ảnh **chỉ được gửi đi khi người dùng TỰ BẬT** công tắc (mục 3 #42) |
| `TC-INS-xx` | Màn hình Phân tích (Rule Engine AI) |
| `TC-STEP-xx` | Đếm bước bằng cảm biến của máy (quyền `ACTIVITY_RECOGNITION`) |
| `TC-SLEEP-xx` | Số giờ ngủ: **khung giờ do người dùng đặt** (bảng `app_settings`) + đo bằng cảm biến của máy (ánh sáng + gia tốc + màn hình; không cần quyền) |
| `TC-HR-xx` | **Đo nhịp tim bằng camera + đèn flash** (PPG): đèn flash chiếu xuyên mô ngón tay, camera ghi sắc độ đỏ từng khung hình, app phân tích chu kỳ để ra nhịp |
| `TC-HC-xx` | Health Connect |
| `TC-DB-xx` | Room + SQLCipher (lưu trữ, mã hóa, migration) |
| `TC-WM-xx` | WorkManager (job nền, thông báo) |
| `TC-WG-xx` | Widget màn hình chính (Glance) |
| `TC-FB-xx` | Firebase Firestore sync (tùy chọn) |
| `TC-PRIV-xx` | **Chính sách bảo mật** trong app + rationale Health Connect (mục 3 #31) |
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
| Thiết bị thật | Khuyến nghị test thêm 1 máy Android 13+ để kiểm tra `POST_NOTIFICATIONS`. **Bắt buộc có 1 máy thật cho màn "Đo nhịp tim" (TC-HR)**: cần **camera sau + đèn flash** — emulator không có đèn flash nên màn đo chỉ hiện cảnh báo và kết quả không đáng tin |
| Dữ liệu mạng | Không bắt buộc với Health Connect/Room; **bắt buộc với Firebase sync** và với **AI phân tích món ăn (Gemini)** — không có mạng thì màn Ghi chép báo rõ và cho **Thử lại** (mục 3 #42) |

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

**Cấu hình khoá AI (phân tích món ăn) — cần cho tính năng ở mục 3 #42:** thêm **2 dòng** vào `local.properties` (file này **đã bị `.gitignore`** nên khoá **không** bao giờ vào git):

```properties
gemini.api.key=<khoá Google AI Studio — dạng AQ.… (Authentication Key mới) hoặc AIza… (traffic key cũ)>
gemini.model=gemini-flash-latest
```

Hai giá trị này được đưa vào `BuildConfig.GEMINI_API_KEY` / `BuildConfig.GEMINI_MODEL` lúc build (`buildConfig = true`). **Thiếu khoá ⇒ app vẫn build và chạy bình thường**, chỉ tính năng AI bị tắt và màn Ghi chép báo *"chưa có khoá AI"* (không crash, không gọi mạng).

⚠️ **Khoá nhúng trong APK là trích xuất được** — chỉ hợp với app chạy **thử nghiệm cá nhân**; muốn phát hành thì phải gọi Gemini qua một **proxy backend**. Khi Google bỏ tên model cũ (đã gặp: `gemini-2.5-flash` → **404 "no longer available"**), chỉ cần sửa `gemini.model` rồi build lại — **không** phải sửa code.

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
| 15 | ~~Widget không tự refresh~~ **→ Đã sửa (và widget đã đổi nội dung)** | Cổng `domain/widget/HealthWidgetRefresher` + impl `presentation/widget/HealthWidgetRefresherImpl` (Glance `updateAll`), bind trong `di/AppModule`; **nay** `HealthDataRepositoryImpl` gọi `refresh()` sau `syncHealthData()` và `saveMeasuredHeartRate()` — tức mỗi lần **dữ liệu sức khỏe** đổi (đồng bộ xong / vừa đo nhịp tim) → widget đọc lại và vẽ lại (TC-WG-04, TC-WG-10). Kèm `updatePeriodMillis="1800000"` (30 phút) làm lưới an toàn khi app không chạy. ⚠️ **Thay đổi hành vi:** `HabitRepositoryImpl` **không** còn gọi `refresh()` (thói quen không làm nội dung widget khác đi — xem TC-WG-13) | ✅ Đã sửa |
| 16 | ~~Widget hiển thị thói quen **mới nhất**~~ **→ Không còn áp dụng** | Widget **không còn hiển thị thói quen**: từ bản sửa "Sức khỏe hôm nay" nó hiện **bước chân / giấc ngủ / nhịp tim** (mục 15). Cả `getAllActiveHabits().first()` **và** `TodayHabitProgress.orderForWidget()` (thứ tự theo buổi) đã bị **xoá** cùng `domain/usecase/habit/TodayHabitProgress.kt` — thứ tự thói quen theo buổi **không còn nơi dùng**, không phải bị bỏ quên | ✅ Đã xử lý |
| 17 | ~~Passphrase SQLCipher hard-code~~ **→ Đã sửa** | `data/local/security/DatabasePassphraseProvider.kt` sinh passphrase **ngẫu nhiên 32 byte** ở lần chạy đầu, `AndroidPassphraseStorage.kt` bọc bằng khoá **AES-256/GCM trong Android Keystore** (khoá không rời thiết bị) và lưu bản mã `Base64(IV‖ciphertext)` ở prefs `healthy_ai_secure_store`. APK **không** còn chứa khoá nào mở được DB; các lần mở sau đọc lại đúng khoá đã lưu. Máy đã cài bản cũ (DB mã hoá bằng khoá hard-code) được `LegacyDatabaseKeyMigration` **rekey một lần** sang khoá Keystore → giữ nguyên thói quen/nhật ký thay vì crash *"file is not a database"*. `fallbackToDestructiveMigration()` vẫn giữ (phụ lục C #14) | ✅ Đã sửa |
| 18 | ~~Màu nền hard-code~~ **→ Đã sửa** | ~~Các màn hình đặt `containerColor = Color(0xFFF8F9FA)` (sáng) trong khi theme có Dark scheme → dark mode tương phản kém~~. Lỗi thực tế **nặng hơn**: `Card` còn để `containerColor = Color.White` nên ở dark mode thành **thẻ trắng** trong khi chữ dùng `onSurface` sáng ⇒ **mất chữ**, không chỉ "kém tương phản". Nay **nền màn hình** và **nền thẻ/panel** đều lấy từ theme: `Scaffold(containerColor = MaterialTheme.colorScheme.background)` ở 7 chỗ (`HomeScreen`, `HabitsScreen`, `AddEditHabitScreen`, `InsightsScreen`, `CameraScreen`, `LoginScreen`, và `Surface` của `AuthLoadingScreen` trong `AppNavigation`), **12** chỗ `Card`/`InfoActionCard` đổi `Color.White` → `MaterialTheme.colorScheme.surfaceContainerLow`, thẻ gợi ý AI (`AiTipCard` trong `InsightsScreen`) bỏ 3 nền cứng `#FFFFFF`/`#FFEBEE`/`#FFF3E0` → dùng `surfaceContainerLow` + **lớp màu ưu tiên mờ 12%** (giữ tín hiệu CRITICAL/HIGH), badge "Xong" ở `HabitsScreen` dùng `surfaceVariant`. Thêm token `surfaceContainerLow` vào `Color.kt`/`Theme.kt` (sáng **`#FFFFFF`**, tối **`#23261F`**) để thẻ vẫn nổi hơn nền ở **cả hai** chế độ. Chữ trên nền thẻ đạt **WCAG AA** (sáng 17.2:1, tối 11.9:1). **Không** đụng màu chữ/icon, màu xanh thương hiệu, màu error và vệt nền thanh tiến độ (phụ lục C #18) | ✅ Đã sửa |
| 19 | ~~Chưa có nút **Đăng xuất**~~ **→ Đã sửa** | ~~Có màn đăng nhập (mục #6) nhưng không có UI đăng xuất: đã đăng nhập hoặc đã bấm "Bỏ qua" thì chỉ còn cách xoá dữ liệu app để quay lại màn đăng nhập. Nằm ngoài phạm vi đã chốt của bản sửa lỗi #6~~. Nay `AuthRepository` có `signOut()` (uỷ quyền `FirebaseAuth.signOut()`), `AuthGateViewModel.signOut()` xoá phiên **và** cờ chế độ khách → `SessionState.SignedOut`. UI: `IconButton` icon **Logout** ở góc phải lời chào màn Trang chủ (`HomeScreen.GreetingHeader`) + **hộp thoại xác nhận** ("Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên."). Áp dụng cho **cả** người đã đăng nhập lẫn chế độ khách; dữ liệu Room/SQLCipher **không** bị xoá. Chi tiết: TC-AUTH-15, TC-AUTH-16 | ✅ Đã sửa |
| 20 | ~~Widget vẽ thanh tiến độ **tĩnh ~40%** và chỉ hiển thị **1 thói quen**~~ **→ Không còn áp dụng** | Bản sửa cũ (`TodayHabitProgress.build()` cộng `habit_logs` hôm nay → dòng `x/y thói quen đạt mục tiêu` + `LinearProgressIndicator` thật + tối đa 3 dòng `✓/○ tên — đã ghi/mục tiêu` + `+N thói quen khác`) **đã bị thay cùng widget**: khung 150×140dp phải nhồi quá nhiều thứ nên chật, và `HabitProgress`/`HabitProgressLine` cùng `TodayHabitProgress` đã **xoá** (màn Thói quen vẫn dùng `formatHabitProgress`). Widget nay hiện **bước chân / giấc ngủ / nhịp tim** với khung `minWidth 250dp`/`minHeight 130dp` (mục 15) | ✅ Đã thay |
| 21 | ~~Lời chào trên màn Trang chủ **hard-code `Text("Alex")`** cho mọi phiên — đăng nhập khách hay đăng nhập thật cũng đều chào "Alex"~~ **→ Đã sửa** | Nay lấy tên thật: `AuthRepository.currentUserName` (`displayName` Firebase → nếu trống lấy **phần trước `@` của email**) → `HomeViewModel.loadUserName()` → `HomeUiState.userName` → `GreetingHeader(userName, …)`. Chế độ khách (`null`) **không** vẽ dòng tên và câu chào bỏ dấu phẩy đuôi ("Chào buổi sáng") | ✅ Đã sửa |
| 22 | ~~Chuỗi tiếng Việt trong `HomeScreen.kt` bị lỗi font (double-encode)~~ **→ Đã sửa** | ~~**26** string literal / **22** chuỗi (lời chào, dòng ngày, "Tóm tắt sức khỏe", "Bước chân", "Ngủ", "Đăng xuất", "Hoàn thành", "Mục tiêu: …", cả nhãn dữ liệu mẫu trong `@Preview`) bị double-encode (byte UTF-8 bị đọc như **cp1252**) nên hiển thị `ChÃ o buá»•i Ä‘Ãªm`, `TÃ³m táº¯t sá»©c khá»e`~~. Nay đã đưa về đúng dấu tiếng Việt; giữ nguyên CRLF và số dòng của file. `strings.xml` và các màn hình khác **không** bị lỗi này. Chi tiết: TC-EDGE-17 | ✅ Đã sửa |
| 23 | **Nhịp tim đo được không có nguồn nào khác ngoài Health Connect** **→ Đã bổ sung** | Trước đây ô **"Tim"** ở thẻ "Tóm tắt sức khỏe" **chỉ** lấy `healthConnect.getRecentHeartRate()`; máy không có ứng dụng sức khỏe/đồng hồ đeo tay ⇒ ô luôn `0 bpm` và app **không có cách nào tự đo**. Nay có màn **"Đo nhịp tim"** (`presentation/screen/heartrate/`): đèn flash chiếu sáng xuyên mô ngón tay, camera sau đọc **giá trị đỏ trung bình** từng khung hình (`data/heartrate/CameraPpgAnalyzer.kt`), thuật toán PPG (`domain/usecase/heartrate/`) bỏ thành phần một chiều, tìm đỉnh mạch và **kiểm tra tính chu kỳ bằng tự tương quan** để ra nhịp. Ô "Tim" (nút **"Đo ngay"**) mở màn đo; kết quả **tự lưu** vào `health_data_history` nên ô "Tim" và Rule Engine ở tab Phân tích dùng được ngay. **Thứ tự ưu tiên:** Health Connect vẫn thắng khi có số; chưa có số mới dùng nhịp ghi gần nhất trên máy (kèm dòng chú thích nguồn). ⚠️ **Giới hạn:** đây là **ước lượng** từ camera điện thoại (không phải thiết bị y tế), cần **đèn flash**; tay rung hoặc ấn ngón tay quá mạnh sẽ làm sai kết quả — app **không** kết luận khi nhịp không đều/tín hiệu không có chu kỳ. Chi tiết: TC-HR-01…TC-HR-13 | ✅ Đã bổ sung |
| 24 | ~~Màn **"Ghi chép bữa ăn"** luôn hiện thẻ "Kết quả nhận diện" dù ảnh **không phải đồ ăn**, và nhãn hiển thị là **nhãn ảnh tiếng Anh** chung chung ("Food", "Cuisine", "Table"…) nên kết quả vô nghĩa với bữa ăn~~ **→ Đã sửa** (phụ lục C #21) | Nay có lớp `domain/usecase/food/FoodRecognition.kt`: **chỉ nhận nhãn thuộc danh mục món ăn/nguyên liệu** (~180 từ khoá, khớp theo **từ trọn vẹn** nên "Pineapple" không thành "Apple", "Steak" không thành "Tea", "Peanut" không thành "Pea"), **dịch sang tiếng Việt kèm emoji** (`Pizza` → `Pizza 🍕`, `Chicken` → `Thịt gà 🍗`), gộp nhãn cùng món (giữ độ tin cậy cao nhất), chỉ nhận **≥ 0.5**, tối đa **5 món** xếp giảm dần. Ảnh **không có món ăn** nào → `CameraUiState.noFoodDetected = true` → thẻ **"Chưa nhận diện được món ăn"** + gợi ý chụp lại, **không** hiện kết quả giả. Nguồn nhãn ML Kit nằm sau cổng `data/vision/ImageLabelSource` (impl `MlKitImageLabelSource`, ngưỡng **ứng viên** 0.3) nên `CameraViewModel` test được trên JVM; thẻ kết quả đổi nhãn góc phải "Tự động" → **"Trên máy"** và thêm dòng nói rõ **chỉ là tên món, không phải số liệu dinh dưỡng**. Chi tiết: TC-CAM-07, TC-CAM-08, TC-CAM-16, TC-CAM-17 | ✅ Đã sửa |
| 25 | **Chỉ phân tích được ảnh vừa chụp — không có cách đưa ảnh có sẵn (ảnh cũ trong thư viện) vào phân tích** **→ Đã bổ sung** | Nút viền **"Chọn ảnh từ thư viện"** ở màn Ghi chép mở **photo picker của hệ thống** (`ActivityResultContracts.PickVisualMedia(ImageOnly)` — **không** cần quyền đọc thư viện, Android 13+ dùng picker riêng, máy cũ rơi về `ACTION_OPEN_DOCUMENT`). Ảnh chọn được đọc bằng `data/vision/GalleryImageLoader.kt`: đọc kích thước trước (`inJustDecodeBounds`) → thu nhỏ bằng `inSampleSize` (luỹ thừa 2, cạnh dài ~**1280 px**, tránh `OutOfMemoryError` với ảnh 12–50 MP) → **xoay theo cờ EXIF** (`androidx.exifinterface:1.3.2`, ảnh chụp dọc lưu ngang) → `CameraViewModel.analyzeBitmap()` — **dùng đúng luồng phân tích cũ** nên kết quả vẫn là **tên món tiếng Việt** và vẫn có nhánh "chưa nhận diện được". Bấm **Huỷ** → im lặng, không báo lỗi; ảnh **không đọc được** (uri hỏng/tệp bị xoá) → thẻ lỗi *"Không đọc được ảnh đã chọn: …"* (`onImageLoadError`), app **không** crash. Nút này **không** nằm trong nhánh quyền Camera nên **từ chối quyền camera vẫn dùng được**, thẻ xin quyền thêm dòng *"Chưa muốn cấp quyền? Bạn vẫn có thể chọn ảnh có sẵn trong thư viện."* Chi tiết: TC-CAM-18, TC-CAM-19, TC-CAM-20 | ✅ Đã bổ sung |
| 26 | **Nhịp tim không được "đọc lại từ đầu ngày"; chưa có chỗ xem lại lịch sử tóm tắt sức khỏe 7 ngày** **→ Đã bổ sung** (phụ lục C #32) | Bước chân và giờ ngủ về số mới khi sang ngày mới, nhưng nhịp tim thì **không**: `getLatestRecordedHeartRate()` đọc hàng **mới nhất trong cả bảng** (không lọc ngày) nên sang ngày mới ô "Tim" vẫn hiện số của **hôm qua**, còn `getRecentHeartRate()` chỉ lấy **1 giờ gần nhất** nên 00:05 vẫn dính số của tối hôm trước. Nay nhịp tim cũng theo **ngày**: `HealthConnectDataSource.getTodayHeartRate()` lấy **trung bình 00:00 → hiện tại** (cùng mốc với bước chân/calo), `HealthDataDao.getLatestHeartRateSince(fromTs)` + repository truyền **đầu ngày hôm nay**, nên sang ngày mới mà chưa đo lại thì `displayHeartRate = 0` → UI hiện **`-- bpm`** (không hiện `0 bpm` vì 0 bị đọc là "nhịp tim bằng 0"). Kèm theo, thẻ **"Tóm tắt sức khỏe"** **bỏ 3 dòng chú thích nguồn** (bước chân từ cảm biến / giờ ngủ ước lượng / nhịp tim ghi trên máy) — chỉ giữ 3 vòng tròn chỉ số + nút "Đo ngay". Tab **Phân tích** thêm thẻ **"Lịch sử sức khỏe 7 ngày"** (biểu đồ cột bước chân + bảng 7 dòng: ngày · bước chân · giờ ngủ · nhịp tim) đọc từ `health_data_history` qua `GetRecentHealthSummaryUseCase` → `HealthSummaryHistory.summarize()` (**hàm thuần Kotlin**, mỗi ngày lấy hàng mới nhất, luôn đủ 7 ngày cũ → mới, ngày không có hàng hiện "Chưa có dữ liệu"), dùng `getHealthHistorySince(fromTs)` — cắt theo **mốc thời gian** chứ không theo số hàng nên không thiếu ngày khi dữ liệu cũ có nhiều hàng/ngày. Chi tiết: TC-HOME-15, TC-INS-31, TC-INS-32, TC-INS-33 | ✅ Đã bổ sung |

| 33 | **Thẻ giấc ngủ trống dù Health Connect có dữ liệu ngủ** — máy chỉ báo `STAGE_TYPE_SLEEPING` (không chia Sâu/Nông/REM) **→ Đã sửa** (phụ lục C #25) | `getSleepStageBreakdown` chỉ cộng `stage.stage` theo 3 hằng số DEEP/LIGHT/REM nên phiên ngủ **không chia pha** bị **bỏ qua hoàn toàn** ⇒ thẻ "Xu hướng chất lượng giấc ngủ" hiện "Chưa có dữ liệu giấc ngủ" dù máy có dữ liệu. Nay `SleepStageBreakdown` thêm **`unstagedMinutes`** (tổng thời lượng các phiên không chia pha) + **`hasStages`** (chỉ `true` khi có pha), `HealthConnectDataSourceImpl.getSleepStageBreakdown` cộng dồn thời lượng phiên khi phiên đó không có pha nào; UI vẽ thanh pha khi `hasStages`, còn khi chỉ có phiên không chia pha thì hiện dòng **"Health Connect chỉ báo tổng thời gian ngủ (máy không chia pha Sâu/Nông/REM): … trong 7 đêm gần nhất."** (chuỗi `insights_sleep_unstaged_total`). `hasData` vẫn đúng nghĩa "có dữ liệu ngủ nào không". Chi tiết: TC-INS-37; unit test `SleepStageBreakdownTest` (3 test) | ✅ Đã sửa |
| 34 | **Thẻ giấc ngủ chỉ đọc Health Connect một lần lúc mở app** **→ Đã sửa** (phụ lục C #26) | Chi tiết pha giấc ngủ đọc trực tiếp từ Health Connect (không phải Flow) nên trước đây phải **thoát app** rồi mở lại mới thấy số mới; hai thẻ khác cùng màn tự cập nhật qua Flow. Nay `InsightsScreen` gọi `InsightsViewModel.refresh()` bằng **`LifecycleResumeEffect`** mỗi lần màn Phân tích được **quay lại** (đổi tab rồi quay về, mở lại app), nên số liệu pha luôn mới mà **không** cần đọc lại Health Connect liên tục. Chi tiết: TC-INS-38 | ✅ Đã sửa |
| 35 | **Biểu đồ "Sự nhất quán hàng tuần" lấy mẫu số là số thói quen đang hoạt động HIỆN TẠI** **→ Đã sửa** (phụ lục C #27) | Tạo thói quen mới hôm nay làm **tụt** tỉ lệ của cả 6 ngày trước (mẫu số tăng cho ngày cũ), còn xoá thói quen làm tỉ lệ quá khứ **vọt** lên — biểu đồ không còn phản ánh đúng lịch sử. Nay `GetWeeklyConsistencyUseCase` tính **snapshot từng ngày**: chỉ những thói quen đã tồn tại từ ngày đó (`habit.createdAt < mốc đầu ngày kế tiếp`) mới nằm trong mẫu số. Ghi chú: mục tiêu vẫn so với `Habit.targetValue` hiện tại (giữ nguyên nghiệp vụ cũ, chỉ sửa mẫu số). Chi tiết: TC-INS-21; `GetWeeklyConsistencyUseCaseTest` **+2 test** | ✅ Đã sửa |
| 36 | **`catch (e: Exception)` trong hàm `suspend` nuốt `CancellationException`** (**14 vị trí**, cộng ~18 chỗ `runCatching` trong coroutine) **→ Đã sửa** (phụ lục C #39) | Huỷ coroutine (rời màn, `viewModelScope` bị huỷ, WorkManager dừng worker) bị biến thành **"0 / không có dữ liệu / thất bại"** và coroutine **không kết thúc đúng**. Nay mọi chỗ bắt lỗi trong hàm `suspend` đều `catch (e: CancellationException) { throw e }` **trước** `catch (e: Exception)` (`HealthConnectDataSourceImpl` 6 chỗ, `GalleryImageLoader`, `CameraViewModel`, `CameraScreen`, `SyncWorker`, `HealthSyncWorker`, `SleepTrackingWorker`, `LegacyDatabaseKeyMigration`; `HeartRateScreen` đã đúng sẵn), và thêm helper **`util/RunCatchingCancellable.kt`** (`runCatchingCancellable`) dùng thay `runCatching` ở các chỗ bọc lời gọi `suspend`/I/O (`AuthRepositoryImpl`, `FirestoreSyncDataSource`, `HealthDataRepositoryImpl`, `HabitWidgetRefresherImpl`, `GetHealthDataUseCase`, `GetHealthConnectPermissionStatusUseCase`, `GetLatestRecordedHeartRateUseCase`, `InsightsViewModel`, `HabitsViewModel`, `AddEditHabitViewModel`, `HomeViewModel`). `runCatching` chỉ bọc việc thuần bộ nhớ (`HabitCategory.valueOf`, `Context.startActivity`) **giữ nguyên** vì không thể sinh `CancellationException`. Chi tiết: TC-WM-17 | ✅ Đã sửa |
| 37 | **Không có tầng i18n** — `strings.xml` chỉ có **2** chuỗi, `stringResource` = **0**, ~**150 chuỗi `Text("…")`/`contentDescription` tiếng Việt viết cứng** trong 15 file UI + nhãn widget + kênh thông báo **→ Đã sửa** (phụ lục C #41) | Nay chuỗi hiển thị nằm ở **`res/values/strings.xml`** (**221 `<string>`**) và được đọc bằng `stringResource(...)` (Compose) / `context.getString(...)` (Glance widget + `ReminderWorker` + `Toast` trong `LaunchedEffect`) — **~215 chỗ**; **chữ hiển thị không đổi**. Nhãn 4 tab bottom bar: `Screen.title` (chuỗi) → **`Screen.titleRes` (`@StringRes`)** + `R.string.nav_*`; bộ lọc màn Thói quen: `listOf("Tất cả", "Sáng", "Chiều", "Tối")` + so sánh chuỗi trong `HabitsViewModel` → **enum `HabitFilter`** (`labelRes` + `timeOfDay`); `AndroidManifest` dùng `@string/widget_label`. Ba hàm phụ trong `HeartRateScreen` (`subtitleFor`, `strengthLabel`, `guidanceVisuals`) đổi thành `@Composable` để đọc tài nguyên. Cách dùng chuỗi có giá trị động: `%1$s`/`%1$d` (ví dụ `home_habit_target`, `habits_log_dialog_title`, `insights_sleep_nights_with_data`, `heart_rate_quality` với `%%`). **Còn lại (ngoài phạm vi mục này):** thông báo lỗi sinh trong **ViewModel** (`LoginViewModel`, `HabitLogInput`, `CameraViewModel`, `HeartRateViewModel`…), nội dung gợi ý AI (`SuggestionCatalog`), tên danh mục/buổi/xếp loại trong **domain model** (`HabitCategory.displayName`, `HabitTimeOfDay.displayName`, `SleepQuality.label`), nhãn tuần `T2…CN`, chuỗi định dạng số (`TimeDisplay`, `HabitProgressText`) và dữ liệu mẫu trong `@Preview` — muốn dịch hết thì bước tiếp theo là chuyển các chỗ đó sang `@StringRes` trong UI state. Chưa thêm `values-<locale>` nào (khi cần tiếng Anh chỉ phải thêm `values-en/strings.xml`). Chi tiết: **TC-ENV-08** | ✅ Đã sửa |
| 38 | **Hợp đồng repository sai: `getHealthData(start, end)` bỏ qua cả `start` và `end`; tab Phân tích cắt lịch sử sức khỏe theo SỐ HÀNG** **→ Đã sửa** (phụ lục C #42) | (a) `HealthDataRepositoryImpl.getHealthData(start, end)` **không dùng** tham số nào (luôn trả số của hôm nay) trong khi `GetHealthDataUseCase` truyền `Instant.now()` hai lần ⇒ nay hàm tên là **`getTodayHealthData()`** (không tham số), use case gọi thẳng nó — names nói đúng việc hàm làm; dữ liệu ngày đã qua vẫn đọc qua `health_data_history`. (b) `GetHealthInsightsUseCase` đọc `getRecentHealthHistoryFlow(7)` (**7 hàng** gần nhất) trong khi thẻ "Lịch sử sức khỏe 7 ngày" dùng `getHealthHistorySince` (**mốc thời gian**) ⇒ dữ liệu cũ có nhiều hàng/ngày làm Rule Engine thiếu ngày và luật "3 ngày liên tiếp" **âm thầm không chạy**; nay đọc bằng `getHealthHistorySince(đầu ngày cách đây 6 ngày)` — **cùng cửa sổ 7 ngày** với thẻ lịch sử. Chi tiết: TC-INS-18, TC-INS-19, TC-INS-30, TC-INS-31 | ✅ Đã sửa |
| 39 | **Tab Phân tích im lặng khi lỗi + `HealthSyncWorker` luôn trả `success`** **→ Đã sửa** (phụ lục C #43) | `.catch { // Log error }` nuốt ngoại lệ trong khi `InsightsUiState` **không có** trường `error`, nên flow lỗi thì màn hình đứng ở trạng thái rỗng (người dùng không biết gì); `HealthSyncWorker` log lỗi rồi **vẫn** `Result.success()` nên backoff 3 lần là code chết. Nay: `InsightsUiState` có **`error`** (lỗi luồng dữ liệu chính) + **`sleepStagesError`** (lỗi đọc chi tiết pha giấc ngủ) + **`visibleError`**, `InsightsScreen` hiện `ErrorStateCard` (thẻ lỗi đỏ + nút đóng → `dismissError()`), `catch` **ném tiếp `CancellationException`**; `HealthSyncWorker.doWork()` trả **`retry`** khi đồng bộ thất bại (tối đa 3 lần) rồi **`failure`**, và không biến việc **dừng worker** thành "thử lại/thất bại". Thêm **`InsightsViewModelTest` (6 test)** — trước đây `InsightsViewModel` là ViewModel **duy nhất không có unit test**. Chi tiết: TC-INS-39, TC-WM-17 | ✅ Đã sửa |
| 40 | **Manifest xin thừa quyền GHI của Health Connect** **→ Đã sửa** (phụ lục C #44) | Manifest khai `READ_STEPS`+`WRITE_STEPS`, `READ_SLEEP`+`WRITE_SLEEP` nhưng app **chỉ đọc** (`HealthConnectDataSourceImpl.requiredPermissions` chỉ có 4 quyền đọc) ⇒ hộp thoại xin quyền hỏi nhiều hơn mức cần, dễ bị hiểu là app ghi dữ liệu sức khỏe. Nay manifest **chỉ còn 4 quyền ĐỌC** (`READ_STEPS`, `READ_SLEEP`, `READ_HEART_RATE`, `READ_ACTIVE_CALORIES_BURNED`) kèm chú thích lý do. Không cần đổi code quyền trong app. Chi tiết: TC-HC-13 | ✅ Đã sửa |
| 41 | ~~Feed **"BLOOM AI GỢI Ý"** ở tab Phân tích trình bày dạng **danh sách thẻ xếp dọc** nên màn hình **quá dài**~~ **→ Đã sửa** (phụ lục C #48) | Nay `InsightsScreen` có **`AiTipBanner`**: cả feed nằm trong **một banner cao cố định** (`AI_BANNER_HEIGHT = 240.dp`) dùng `HorizontalPager` + `rememberPagerState(pageCount = { Int.MAX_VALUE })` (lấy `page % suggestions.size` ⇒ **luôn trượt tới trước**, không quay ngược) và **tự cuộn mỗi 5 giây** (`AI_BANNER_AUTO_SCROLL_MS`; đứng ngoài khi người dùng đang vuốt) ⇒ chỉ hiện **một gợi ý mỗi lúc**, kèm **chấm chỉ báo** trang. `AiTipCard` ép `fillMaxSize()` + `maxLines`/`TextOverflow.Ellipsis` và ghim nút hành động ở đáy nên banner **không nhảy** chiều cao giữa các trang. Bộ mẹo vẫn do `HabitRuleEngine` **xoay vòng theo ngày** (mục 3 #27) nên **mỗi ngày một bộ khác, không trùng** — nay **khoá bằng test JVM** (`HabitRuleEngineTest` **+2 test**). Chi tiết: TC-INS-40, TC-INS-41 | ✅ Đã sửa |
| 42 | ~~Màn **"Ghi chép bữa ăn"** chỉ liệt kê **nhãn ảnh chung** của ML Kit (tiếng Anh, ~400 nhãn, **không** phải model món ăn) nên hay ra sai món — chụp **đĩa cơm** mà ra **Pizza** — và **không** cho biết món đó lợi/hại gì cho cơ thể~~ **→ Đã sửa** (phụ lục C #49) | Nay **bỏ hẳn ML Kit** (`ImageLabelSource`, `MlKitImageLabelSource`, `FoodRecognition` + dependency `mlkit-image-labeling`/`camerax-mlkit-vision` đã xoá) và dùng **AI (Gemini)**: `data/vision/FoodInsightSource` → `GeminiFoodInsightSource` gửi ảnh **JPEG base64** (`inline_data`) kèm **prompt tiếng Việt** (`FoodAnalysis.buildPrompt()` — ưu tiên món **Việt/Á**, phủ **trái cây/đồ uống/đồ ăn vặt**, bắt ghi **"(có thể)"** khi không chắc, **cấm** chẩn đoán/chữa bệnh) và ép đầu ra bằng **`responseSchema`**; `FoodAnalysis.toInsight()` làm sạch (≤3 gạch/mục, ≤120 ký tự/gạch, bỏ mục rỗng/trùng, emoji mặc định 🍽️). UI hiện **tên món** + mục **"Có lợi cho cơ thể"** + mục **"Nên lưu ý"** + **dòng miễn trừ** ("tham khảo, KHÔNG phải tư vấn y tế"); lỗi được phân loại rõ (chưa có khoá · mất mạng · **quá tải 429/503** · khoá sai 401/403 · máy chủ lỗi 5xx · phản hồi hỏng) và có nút **"Thử lại"** dùng lại ảnh của lượt trước. Vì ảnh **rời khỏi thiết bị**, thêm **cổng opt-in** `food_ai_enabled` (**mặc định TẮT**) — chưa bật thì nút phân tích **bị khoá** và app **không gửi ảnh đi**; `privacy_camera_body` đã viết lại cho đúng sự thật mới. Khoá/model đọc từ `local.properties` (đã gitignore) qua `BuildConfig` — **không** hardcode tên model (đã gặp thật: `gemini-2.5-flash` bị Google bỏ, trả **404**). **Cổng opt-in ở mục này đã bị bỏ ở #43.** Chi tiết: TC-CAM-05…TC-CAM-24, TC-PRIV-04 | ✅ Đã sửa |
| 43 | **AI phân tích món ăn trả lời quá lâu** (một lượt đọc ảnh mất **~42 giây** khi đo thật), **công tắc "Phân tích món ăn bằng AI" là một bước thừa**, và người dùng **không biết app đang làm gì** trong lúc chờ **→ Đã sửa** | (a) **Nhanh hơn**: model mặc định đổi sang **`gemini-3.8-flash`** và request thêm `generationConfig.thinkingConfig.thinkingLevel = "low"` — Gemini 3 **mặc định suy luận ở mức `high`** nên chờ rất lâu; `"minimal"` **bị model này từ chối** (`400 INVALID_ARGUMENT` *"Thinking level MINIMAL is not supported for this model"*) nên `"low"` là mức thấp nhất dùng được. Nếu model không hiểu `thinkingConfig` (model đời cũ) thì lớp gọi **tự gửi lại đúng request đó mà bỏ tham số** (nhận biết theo nội dung thông báo lỗi), thay vì để người dùng nhận "phản hồi không đọc được". (b) **Model dự phòng**: Google trả **503** *"high demand"* và **429** *"exceeded your current quota"* rất thường xuyên ⇒ thêm `gemini.fallback.model` (mặc định **`gemini-flash-lite-latest`**): model chính chạy trước, hỏng vì quá tải thì **chuyển sang model dự phòng**, mỗi model chỉ gọi **1 lần** (trước đây thử lại y nguyên model vừa hỏng — gần như luôn hỏng tiếp), nghỉ giữa hai model **1,5 s → 0,6 s**; lỗi không tự khỏi (401/403) vẫn **không** đổi model cho đỡ tốn thời gian. (c) **Bỏ công tắc** "Phân tích món ăn bằng AI" và **2 dòng ghi chú** "Đang BẬT/TẮT…" ở màn Ghi chép; gỡ luôn cổng `food_ai_enabled` (`SettingsRepository.observeFoodAiEnabled`/`setFoodAiEnabled`, `CameraUiState.isFoodAiEnabled`, màn hình chỉ còn khoá 2 nút khi `isAnalyzing`) ⇒ bấm "Phân tích món ăn" / "Chọn ảnh từ thư viện" là **gửi ảnh đi luôn**. (d) **Hộp thoại "Đang phân tích món ăn"** (vòng xoay + mô tả, **không** tắt được bằng Back/chạm ra ngoài) hiện suốt lúc `isAnalyzing` để người dùng biết AI đang đọc ảnh. (e) Nút **"Xem chính sách bảo mật"** chuyển từ màn Ghi chép sang **tab Phân tích AI** (thẻ **"AI & quyền riêng tư"** ở cuối màn), kèm viết lại `privacy_intro` / `privacy_camera_body` / `privacy_no_sale_body` cho đúng sự thật mới (ảnh rời thiết bị khi **bấm phân tích** — không còn "mặc định TẮT"). Chi tiết: TC-CAM-05, TC-CAM-06, TC-CAM-07, TC-CAM-09, TC-CAM-11, TC-CAM-17, TC-CAM-21, TC-CAM-23, TC-CAM-24, TC-CAM-25, TC-CAM-26, TC-INS-42, TC-PRIV-01, TC-PRIV-04 | ✅ Đã sửa |
| 44 | **Số bước chân vẫn tăng dù app đã bị đóng hẳn** **→ Đã sửa** | `TYPE_STEP_COUNTER` trả **giá trị luỹ kế từ lúc máy khởi động** và **vẫn đếm** cả khi app không mở; bản cũ lấy thẳng hiệu số — thậm chí **suy diễn ngược** (chưa có dữ liệu + máy khởi động trong hôm nay ⇒ `baseline = 0`; sang ngày mới ⇒ **nội suy** mốc 00:00) — nên mở app là số bước "tự nhảy" thêm theo phần đã đi trong lúc app không chạy. Nay `StepCounterMath` **chỉ tính phần quan sát được trong lúc app đang chạy**: `StepCounterStore` sinh **một `sessionId` cho mỗi tiến trình app** (lưu ở prefs `healthy_ai_step_counter`, khoá mới `session_id` — **không** cần migration, thiếu khoá thì đọc là chuỗi rỗng = phiên khác), và `StepCounterMath.sample()` nhận `sessionId` thay cho `bootTimeMs` (tham số này cùng helper `deviceBootTimeMs()`/`SystemClock` đã bị xoá vì hết cần). **Phiên đổi** ⇒ `baseline = cumulative` (đếm lại từ mẫu hiện tại) nhưng **giữ `offset`** = số đã đếm được trong ngày; còn **nội suy mốc 00:00 chỉ dùng khi hai mẫu thuộc cùng một phiên** (app thật sự còn chạy qua nửa đêm). Hệ quả đúng như yêu cầu: app **đang mở** hoặc **còn nằm trong tác vụ** (chạy nền) → đếm tiếp; **đóng hẳn app / tiến trình bị thu hồi** → khoảng thời gian ở giữa **không** được cộng vào, nhưng số đã đếm trong ngày **vẫn giữ**. Mô tả thẻ xin quyền `ACTIVITY_RECOGNITION` (`step_counter_description`) đã nói rõ luật này cho người dùng. Chi tiết: TC-STEP-01…TC-STEP-10 | ✅ Đã sửa |

> **Ghi chú mục 3:** các mục **#27…#32** (feed gợi ý xoay vòng, chỉ số sinh lực theo ngày, xu hướng giấc ngủ, `HabitFrequency`, cổng đồng bộ đám mây, icon launcher/backup) nằm trong **phụ lục C** — cùng lượt rà soát trước. **#33…#40** là **lượt rà soát kế tiếp** (8 mục còn lại của phụ lục C). **#41…#44** là các yêu cầu **người dùng báo trực tiếp** sau đó (banner BLOOM AI, bỏ ML Kit → Gemini, tốc độ/model/UI màn Ghi chép, **đếm bước chỉ khi app đang chạy**).

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
- **Kết quả mong đợi:** Toàn bộ test trong `app/src/test` pass — **40 test class / 420 test** (cập nhật 2026-09-30 — **đếm bước chỉ khi app đang chạy**, mục 3 #44. Trước đó cùng ngày — tốc độ/model/UI màn Ghi chép, mục 3 #43: **40 class / 418 test**. Trước đó 2026-09-29 — bỏ ML Kit, thay bằng AI Gemini ở màn Ghi chép, mục 3 #42: **40 class / 416 test**):
  - **Đo nhịp tim (PPG)**: `HeartRateMathTest` (**20 test**: trung vị/MAD, độ đều của nhịp, độ mạnh tín hiệu AC/DC là **tỉ lệ** nên không đổi khi ảnh sáng/tối, nội suy đỉnh parabol, đổi YUV→kênh đỏ, ngưỡng 40–200 bpm và phân loại nhịp nghỉ 60–100 — TC-HR-11), `PpgAnalyzerTest` (**15 test**: nhịp 72/130/48 bpm đo đúng trong ±4 bpm, đếm nhịp, dạng sóng chuẩn hoá `-1..1`, **chưa đủ 15 giây thì không chốt**, nhiễu thuần **không** thành nhịp, tín hiệu lẫn nhiễu mạnh không cho số sai lệch hẳn, khung tối = chưa thấy ngón tay, nhấc ngón tay giữa chừng thì quên nhịp cũ và đo lại được, nhịp không đều bị từ chối, `reset()`, mốc thời gian `0` vẫn tính — TC-HR-12), `HeartRateViewModelTest` (**21 test**: chỉ phân tích khi đang đo, đủ dữ liệu thì chốt + **tự lưu**, dừng sớm ngay khi đủ điều kiện, phân loại "Cao", nhiễu hết 30 giây thì **không** lưu, "Dừng đo" không lưu gì, đo lại từ 0, câu hướng dẫn theo tình trạng (chưa thấy ngón tay / tín hiệu yếu / đang đo / sắp xong), lỗi camera, cảnh báo máy không có đèn flash, lỗi lưu vẫn giữ kết quả — TC-HR-13)
  - **Rule engine / AI**: `SuggestionCatalogTest` (**6 test**: mỗi danh mục có cả mẹo **CONCERN** và **GENERAL**, tiêu đề/nội dung không rỗng, mẹo CONCERN có mức ưu tiên ≥ MEDIUM, tiêu đề không trùng, mỗi danh mục có ≥ 1 mẹo kèm nút hành động, kho phủ đủ 5 danh mục — mục 3 #27, TC-INS-34), `HabitRuleEngineTest` (**16 test** (mục 3 #41 thêm 2): feed bám danh mục người dùng có, thiếu nước → mẹo **CONCERN** / đủ nước → **GENERAL**, **không còn thẻ `CRITICAL`**, xoay `rotationSeed` → mẹo đổi, **tối đa 4 thẻ**, không lặp mẹo, sắp xếp ưu tiên, **id thẻ duy nhất**, 3 ngày liền ít bước → Vận động CONCERN, 7 ngày nhịp tim ≥ 90 → Thiền định, **xoay seed đi hết kho mẹo mới lặp lại**, **mỗi danh mục phủ hết kho `GENERAL` trong một vòng xoay** — mục 3 #27, TC-INS-41), `HabitRuleEngineExtendedTest` (**4 test**: đơn vị **phút** dưới mục tiêu → CONCERN, đạt mục tiêu → GENERAL, < 3 log → không kết luận, không có thẻ `CRITICAL` — phụ lục C #17), `HabitRuleEnginePersonalizedTest` (**6 test**: health trends gom **theo ngày**: thiếu ngủ 3 ngày liên tiếp → Giấc ngủ CONCERN, **3 hàng cùng ngày → không**, **3 ngày cách quãng → không**, nhịp tim cao 7 ngày → Thiền định, hàng thiếu dữ liệu không đánh dấu sai — phụ lục C #16), `HabitRuleEngineStepTrendTest` (**6 test**: 3 ngày liền ít bước → Vận động CONCERN; trên ngưỡng / cách quãng / thiếu dữ liệu / < 3 ngày → không; mẹo CONCERN có mức ưu tiên ≥ MEDIUM)
  - **Domain / use case / model**: `GetWeeklyConsistencyUseCaseTest`, `GetVitalityScoreUseCaseTest`, `GetHealthConnectPermissionStatusUseCaseTest`, `TodayHabitProgressTest`, `StepCounterMathTest` (**đếm bước bằng cảm biến, mục 3 #44 — luật "chỉ đếm khi app đang chạy": mẫu đầu tiên không cộng bước đi trước khi app chạy, cộng dồn trong cùng một phiên, **đóng app rồi mở lại trong cùng ngày vẫn giữ số đã đếm**, **bước đi lúc app đã đóng không bao giờ được cộng**, dữ liệu bản cũ (không có `sessionId`) là phiên khác, mở lại sang ngày mới bắt đầu từ 0, nội suy mốc 00:00 khi sang ngày mới **trong cùng một phiên**, bộ đếm reset khi máy khởi động lại không làm số tụt, giá trị bất thường không sinh số âm, giá trị đã lưu chỉ hiện trong đúng ngày — **12 test**; xem **TC-STEP-10**), `SleepScheduleTest` (**khung giờ ngủ người dùng đặt: khung vắt/không vắt qua nửa đêm, `contains` nửa mở, độ dài, khung rỗng không hợp lệ, mặc định 23:00–07:00** — TC-SLEEP-01), `SleepTrackerMathTest` (**số giờ ngủ từ cảm biến, theo khung người dùng đặt: khoá đêm nhảy tại giờ đi ngủ, phân loại "đang ngủ" (màn hình/tối/bất động), cộng dồn giữa hai mẫu "đang ngủ", mẫu "thức" cắt cộng dồn, sang đêm mới về 0 + chốt đêm cũ, cắt trần 12 giờ, lọc dưới 45 phút, làm tròn 1 chữ số, số phút hiển thị** — xem **TC-SLEEP-01…TC-SLEEP-05**), `HealthSummaryHistoryTest` (**8 test** cho phần gom lịch sử sức khỏe theo ngày của thẻ ở tab Phân tích: luôn đủ 7 ngày **cũ → mới**, ngày không có hàng → `hasData = false` (không phải "0 bước"), hàng nằm đúng ngày ghi, **hàng mới nhất trong ngày thắng** (hàng đã gộp là hàng đầy đủ nhất), dữ liệu ngoài cửa sổ bị bỏ, gom theo **múi giờ thiết bị** (23:30 giờ VN vẫn thuộc hôm đó), cửa sổ ngắn hơn tuần vẫn tôn trọng, ngày chỉ có **một** chỉ số vẫn tính là có dữ liệu — TC-INS-33), `VitalityScoreMathTest` (**10 test** cho chỉ số sinh lực tính **theo từng ngày**: không có nguồn → `null`, hoàn thành đủ mọi ngày → **100**, **bỏ 3/7 ngày → 43** (điểm **tụt**), mỗi ngày 1/2 thói quen → **50**, chỉ có thành phần thói quen → chuẩn hoá lại trọng số, chỉ có bước chân/giờ ngủ → **100**, ngủ 4/8h + bước 4000/8000 → **50**, **ngày thiếu dữ liệu đóng góp 0**, `hasObservations` đúng — mục 3 #28, TC-INS-35), `SleepTrendMathTest` (**4 test** cho xu hướng giấc ngủ: 1 đêm/ngày theo thứ tự **cũ → mới**, trung bình **bỏ qua** đêm thiếu số, cửa sổ không có giờ ngủ → `hasData = false`, xếp loại đúng biên `< 6h`/`6–7h`/`7–9h`/`> 9h` — mục 3 #29, TC-INS-36)
  - **Data**: `HabitRepositoryImplTest` (log đã đồng bộ, xóa mềm), `HealthDataRepositoryImplTest` (**truyền đúng `limit` xuống DAO** — phụ lục C #16; **bước chân và giờ ngủ lấy từ nguồn "tự lực" của máy khi Health Connect trống, và Health Connect luôn thắng khi có số** — TC-SLEEP-11; **nhịp tim đo bằng camera: ghi vào hàng hôm nay khi chưa có hàng, chỉ thay cột `heartRate` của hàng đã có (giữ bước chân/giờ ngủ/calo), từ chối nhịp ≤ 0; đọc lại nhịp ghi **trong ngày hôm nay** (hỏi DAO đúng từ mốc 00:00), `0` hoặc hôm nay chưa có thì trả `null`; lịch sử sức khỏe cắt theo mốc thời gian qua `getHealthDataSince`** — TC-HR-06, TC-HOME-15, TC-INS-31), `SettingsRepositoryImplTest` (**khung giờ ngủ lưu theo phút trong ngày; thiếu khoá / không phải số / ngoài `0..1439` / hai mốc trùng nhau → mặc định; khung không hợp lệ không ghi DB** — TC-SLEEP-12; **12 test: khung giờ ngủ + đồng bộ đám mây; 4 test của cổng AI đã bị xoá cùng công tắc ở mục 3 #43** — TC-PRIV-03), `HealthConnectStatusMappingTest` (map 3 → AVAILABLE, 2 → UPDATE_REQUIRED, 1/khác → NOT_INSTALLED)
  - **Bảo mật DB**: `DatabasePassphraseProviderTest` — passphrase SQLCipher sinh ngẫu nhiên 32 byte + lưu lại một lần, đọc lại đúng khoá cũ, mất khoá Keystore thì sinh khoá mới thay vì crash (mục 3 #17); `DatabaseMigrationsTest` — dãy migration phủ **liên tục** từ 1 tới `DatabaseMigrations.DATABASE_VERSION` (bỏ sót bước nào là fail), migration 1→2 chỉ **ALTER** bảng không drop (phụ lục C #14), và **`MIGRATION_3_4` chỉ tạo bảng `app_settings`**, không DROP/ALTER bảng khác (TC-DB-17)
  - **Riêng tư / đồng bộ đám mây**: `CloudSyncPolicyTest` (**5 test**: chưa bật → không tải gì; bật nhưng **thiếu mốc đồng ý** → không tải gì; chỉ tải log tạo **sau** mốc đồng ý; **lịch sử ghi ở chế độ khách KHÔNG bị đẩy lên**; log đúng bằng mốc đồng ý vẫn được tải — mục 3 #31, TC-PRIV-03)
  - **ViewModel**: `HomeViewModelTest` (**tên hiển thị trên lời chào: có tên khi đã đăng nhập, `null` ở chế độ khách** — phụ lục C #30; **giờ ngủ: dùng ước lượng từ cảm biến khi Health Connect trống, Health Connect thắng khi có số, refresh đọc lại được** — TC-SLEEP-11; **nhịp tim: dùng số ghi trong ngày khi Health Connect trống, Health Connect thắng khi có số, sang ngày mới chưa đo lại thì `0` (ô "Tim" hiện `-- bpm`), quay về từ màn đo thì đọc lại được** — TC-HOME-15, TC-HR-07; **khung giờ ngủ: mặc định 23:00–07:00, đọc giá trị từ DB, mở hộp thoại với khung đang dùng, lưu, chặn khung rỗng, xoá lỗi khi sửa, huỷ không lưu** — TC-SLEEP-07, TC-SLEEP-08; **đồng bộ đám mây: mặc định TẮT, phản chiếu thiết lập trong DB, gạt công tắc ghi qua repository, cờ đăng nhập đúng cho người dùng/khách** — TC-HOME-16, TC-PRIV-03), `HabitsViewModelTest` (phản hồi "Đã ghi nhận", **yêu cầu ghi nhận từ màn Phân tích**, **chặn số âm/0**, đóng thẻ lỗi), `AddEditHabitViewModelTest` (xóa mềm, định dạng mục tiêu, **chống trùng tên**, **chống double-submit**, **tần suất cũ WEEKLY/MONTHLY được chuẩn hoá về DAILY khi lưu** — mục 3 #30), `LoginViewModelTest`, `CoreTrackingViewModelTest`, `CameraViewModelTest` (**15 test** (17 trước mục 3 #43 — bỏ 3 test của cổng opt-in, thêm 1 test "bấm là gọi AI"): AI trả món → `insight` là **tên món tiếng Việt** + `benefits`/`cautions`, `isAnalyzing` đặt **đồng bộ** ngay khi bắt đầu (màn hình dựa vào cờ này để hiện hộp thoại "Đang phân tích món ăn"), **bấm phân tích là gọi AI ngay** (`calls == 1` — không còn công tắc để chặn), AI nói ảnh không có đồ ăn (hoặc không nêu tên món) → `noFoodDetected` và **không** có kết quả giả, **mỗi loại lỗi một câu riêng** + `canRetry`, nút **"Thử lại"** gửi lại **đúng ảnh cũ**, lượt mới **xoá kết quả + lỗi** của lượt trước, lỗi chụp ảnh / **lỗi đọc ảnh từ thư viện** → thẻ lỗi, `clearResult()` về **trạng thái đầu hoàn toàn** — TC-CAM-17)
  - **AI phân tích món ăn (màn Ghi chép, mục 3 #42, #43)**: `FoodAnalysisTest` (**17 test**: prompt đòi **tiếng Việt** + ưu tiên món **Việt/Á** + phủ **trái cây/đồ uống/đồ ăn vặt** + giới hạn **120 ký tự** + **cấm** chẩn đoán bệnh/thuốc + **bắt ghi "(có thể)"** khi không chắc; `toInsight` giữ dữ liệu sạch, **không phải đồ ăn → rỗng**, **có đồ ăn mà không có tên → coi như chưa nhận diện được**, bỏ gạch rỗng, **≤3 gạch/mục**, bỏ gạch trùng, cắt gạch/tên món quá dài, **emoji thiếu hoặc là văn bản → 🍽️** — TC-CAM-24), `GeminiFoodInsightSourceTest` (**28 test** (20 trước mục 3 #43): đọc đúng vỏ `candidates[0].content.parts[0].text`; body hỏng/thiếu `candidates`/text không phải JSON → `BAD_RESPONSE`; ánh xạ mã HTTP 200/401/403/429/503/500/404/400; request có `inline_data` + `image/jpeg` + prompt + `application/json` + `maxItems` và **không** có tiền tố `data:image`; **khoá API không nằm trong body**; URL theo **model cấu hình**; **thiếu khoá → không gọi mạng**; **mất mạng → `NETWORK`**; **request yêu cầu `thinkingLevel = "low"`** và **bỏ được `thinkingConfig`**; **model chính quá tải (503/429) → tự chuyển sang model dự phòng**; **cả hai quá tải → `BUSY` mà không gọi lại cùng model** (đúng 2 request); **model dự phòng rỗng/trùng tên → 1 request**; **401 → không đổi model**; **400 "Thinking level … not supported" → gửi lại đúng model đó, bỏ `thinkingConfig`**; **400 vì lý do khác → không gửi lại**; schema giới hạn gạch khớp `FoodAnalysis.MAX_BULLETS` — TC-CAM-24, TC-CAM-26)
  - ~~**Nhận diện món ăn bằng ML Kit**: `FoodRecognitionTest` (**15 test**)~~ **→ Đã xoá cùng ML Kit (mục 3 #42)** — bộ gán nhãn ảnh chung chính là nguồn của lỗi *"đĩa cơm → pizza"*; phần kiểm thử thay thế là 2 class ngay trên.
  - **Hiển thị & nhập liệu**: `NumberDisplayTest` (số mục tiêu hiển thị kiểu "số sạch"), `HabitLogInputTest` (đọc/validate ô "Số lượng" của hộp thoại ghi nhận, **kể cả số âm/0**)
  - **Theme / màu nền**: `ThemeSurfaceColorsTest` (nền thẻ `surfaceContainerLow` **khác** và **sáng hơn** `background` ở cả hai chế độ, dark mode không còn thẻ trắng, chữ trên nền thẻ đạt WCAG AA — mục 3 #18, TC-EDGE-16)
  - **Phiên đăng nhập / đăng xuất**: `AuthGateViewModelTest` (chưa có phiên → `SignedOut`, có uid → `SignedIn`, "Bỏ qua" → `Guest`, **đăng xuất ở chế độ khách và khi đã đăng nhập** → `SignedOut`, đăng nhập lại được — mục 3 #19, TC-AUTH-16), `AuthRepositoryImplTest` (`signOut()` uỷ quyền `FirebaseAuth.signOut()`; **`currentUserName`: `displayName` → phần trước `@` của email → `null` khi chưa đăng nhập** — phụ lục C #30)
  - **Tab Phân tích (mục 3 #39, phụ lục C #43)**: `InsightsViewModelTest` (**6 test** — trước đây là ViewModel **duy nhất không có test**): mọi nguồn (gợi ý AI / chỉ số sinh lực / nhất quán tuần / lịch sử sức khỏe) vào đúng UI state và `sleepTrend` dựng từ chính lịch sử; `isLoading` chỉ tắt khi có snapshot đầu; luồng dữ liệu **lỗi → `error`** (kèm thông báo) thay vì im lặng; `dismissError()` xoá lỗi; chi tiết pha giấc ngủ đọc **lúc mở màn và khi `refresh()`** (`coVerify(exactly = 2)`); lỗi đọc chi tiết giấc ngủ → `sleepStagesError` **không** lẫn với lỗi luồng chính — TC-INS-39
  - **Giấc ngủ (mục 3 #33, phụ lục C #25)**: `SleepStageBreakdownTest` (**3 test**): máy chia pha → `hasStages` + `hasData`; máy **chỉ báo tổng thời lượng** (`unstagedMinutes`) → `hasStages = false` nhưng `hasData = true`; bản rỗng → không có dữ liệu — TC-INS-37
  - **Tab Phân tích — mẫu số biểu đồ nhất quán (phụ lục C #27)**: `GetWeeklyConsistencyUseCaseTest` (**+2 test**): ngày **trước khi** thói quen được tạo không tính nó vào mẫu số; tạo thói quen **hôm nay** không làm tụt tỉ lệ 6 ngày trước; đổi `HealthDataRepositoryImplTest` sang hợp đồng mới `getTodayHealthData()` (phụ lục C #42) — TC-INS-21
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

### TC-ENV-07 — Icon launcher riêng & tắt backup cục bộ (mục 3 #32)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Cài bản debug (`adb install -r`).
- **Bước:** 1. Xem icon app trên launcher / trong Cài đặt ứng dụng → 2. Kiểm tra `AndroidManifest.xml` (hoặc APK bằng `aapt2 dump badging`).
- **Kết quả mong đợi:**
  - Icon là **adaptive icon của app** (`@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`) — **nền xanh `#2E7D32` + trái tim trắng**, **không** còn là icon mặc định của Android (`@android:drawable/sym_def_app_icon`).
  - Manifest có **`android:allowBackup="false"`** — không có `adb backup`/auto-backup nào chứa DB SQLCipher hay prefs.
- **Ghi chú:** Trước bản sửa #32 app dùng **icon mặc định của hệ thống** và `allowBackup="true"` **không kèm** `dataExtractionRules`/`fullBackupContent` ⇒ DB + prefs bị auto-backup lên Google Drive, trong khi **khoá SQLCipher nằm ở Android Keystore (không backup được)** ⇒ máy mới restore mở DB thất bại và rơi vào nhánh **xoá DB** (`LegacyDatabaseKeyMigration`) = mất sạch dữ liệu. Icon chỉ dùng **vector + XML** (`res/mipmap-anydpi-v26/*`, `res/drawable/ic_launcher_foreground.xml`, `res/values/colors.xml`) nên không cần thêm tệp ảnh nhị phân; `minSdk = 26` nên mọi máy đều hỗ trợ adaptive icon.

---

## 5. TC-INIT — Khởi động app & đăng ký job nền

### TC-INIT-01 — Cold start lần đầu
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã `pm clear com.healthyai.app`.
- **Bước:** 1. Mở app.
- **Kết quả mong đợi:**
  - `HiltApplication` khởi tạo, DB Room + SQLCipher tạo mới thành công (không lỗi `System.loadLibrary("sqlcipher")`).
  - `MainActivity.onCreate` gọi `HealthSyncWorker.schedule()`, `SyncWorker.schedule()`, `ReminderWorker.schedule()`, `SleepTrackingWorker.schedule()` **không ném ngoại lệ** (đặc biệt không có `IllegalArgumentException: Cannot set backoff criteria on an idle mode job`).
  - Hiện **màn hình đăng nhập** (sau một nhịp splash rất ngắn khi `FirebaseAuth` đọc lại phiên đã lưu) — xem **TC-AUTH-01**.
  - Bấm **"Bỏ qua, dùng ở chế độ khách"** → vào thẳng màn Home, không có splash/login nữa.

### TC-INIT-02 — Job nền được đăng ký sau lần mở đầu tiên
- **Ưu tiên:** P1
- **Bước:** 1. Mở app → 2. `adb shell dumpsys jobscheduler | Select-String "healthyai"`.
- **Kết quả mong đợi:** Thấy 4 job periodic gắn với `com.healthyai.app/androidx.work.impl.background.systemjob.SystemJobService`.
- **Ghi chú:** Kiểm tra rõ hơn bằng **App Inspection → Background Task Inspector**: thấy `periodic_health_sync_worker`, `periodic_sync_worker`, `daily_reminder_worker`, `periodic_sleep_tracking_worker`.

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

Thành phần: `GreetingHeader` (ngày + lời chào + **tên tài khoản đang đăng nhập** — phụ lục C #30 + avatar xám), `HealthSummaryCard` ("Tóm tắt sức khỏe" + 3 vòng tròn: **Bước chân** (**hôm nay**), **Ngủ** (đêm vừa qua), **Tim** (**hôm nay**, nút **"Đo ngay"**; hiện **`-- bpm`** khi chưa có số — mục 3 #26) — **không** còn dòng chú thích nguồn dưới các vòng tròn), thẻ **"Khung giờ ngủ"**, thẻ **"Đồng bộ đám mây"** (mặc định **TẮT**, có nút **"Xem chính sách bảo mật"** — mục 3 #31), tiêu đề **"Thói quen hôm nay"**, danh sách thẻ thói quen, khối lỗi đỏ (nếu có).

### TC-HOME-01 — Lời chào theo thời điểm
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đã đăng nhập (xem **TC-HOME-12** để kiểm tra dòng tên).
- **Bước:** 1. Đổi giờ hệ thống emulator vào các khoảng: 05:00–11:59, 12:00–17:59, 18:00–21:59, 22:00–04:59 (`adb shell date`) → 2. Mở tab Trang chủ.
- **Kết quả mong đợi:** Lần lượt: **"Chào buổi sáng,"**, **"Chào buổi chiều,"**, **"Chào buổi tối,"**, **"Chào buổi đêm,"** (có dấu phẩy vì bên dưới là dòng tên); dòng dưới là **tên tài khoản đang đăng nhập**, **không** còn luôn là "Alex" (phụ lục C #30).
- **Ghi chú:** Ở **chế độ khách** không có dòng tên → câu chào **không** kèm dấu phẩy ("Chào buổi sáng") — xem **TC-HOME-12**.

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
  - **Bước chân:** `formatStepCount` — dưới **1 000** bước hiện **số chính xác** (`312` → **"312"**, `0` → **"0"**), từ 1 000 bước hiện dạng rút gọn **một chữ số thập phân + `k`** dùng **dấu phẩy** (ví dụ 8 000 bước → **"8,0k"**, 1 950 → **"2,0k"**).
  - **Ngủ:** định dạng `Xh Ym` (ví dụ 7.5 h → **"7h 30m"**).
  - **Tim:** `<bpm> bpm`, số là **nhịp trung bình của Health Connect từ 00:00 hôm nay tới hiện tại** (ví dụ 90.0 → **"90 bpm"**). Hôm nay Health Connect chưa có mẫu nào → hiện **`-- bpm`**, **không** hiện `0 bpm` (xem TC-HOME-15).
  - **Dưới 3 vòng tròn chỉ số KHÔNG còn dòng chú thích nguồn nào** (mục 3 #26): thẻ chỉ còn tiêu đề **"Tóm tắt sức khỏe"**, 3 vòng tròn (Bước chân / Ngủ / Tim) và nút **"Đo ngay"** ở ô Tim.
  - **Ngay dưới thẻ** là thẻ **"Khung giờ ngủ"** (mặc định **23:00 – 07:00**) — xem TC-SLEEP-07.
  - Không có khối lỗi đỏ nào.

### TC-HOME-06 — Thẻ Tóm tắt sức khỏe: chưa cấp quyền Health Connect
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thu hồi hết quyền Health Connect của HealthyAI (hoặc cài trên thiết bị không có Health Connect).
- **Bước:** 1. Mở tab Trang chủ.
- **Kết quả mong đợi:** Nếu Health Connect khả dụng nhưng thiếu quyền: các chỉ số từ Health Connect hiển thị **0** (bước → **"0"**, ô Tim → **`-- bpm`**), app **không crash** (mọi lời gọi đều `try/catch → 0`), **và** màn Home hiện thêm thẻ **"Cấp quyền Health Connect"** ngay dưới thẻ Tóm tắt sức khỏe để người dùng cấp quyền (xem TC-HC-13).
- **Ghi chú (module TC-SLEEP / TC-HR):** ô **Ngủ** **không còn chắc chắn là `0h 0m`** — khi Health Connect không có số giờ ngủ, app dùng **ước lượng từ cảm biến của điện thoại** (TC-SLEEP-11). Ô **Tim** có thể hiện **số đo trong ngày** nếu người dùng vừa đo bằng camera (TC-HOME-15), còn lại là **`-- bpm`**. Muốn về đúng `0`/`--` thì phải xoá dữ liệu app (`pm clear`) để prefs `healthy_ai_sleep_tracker` và bảng `health_data_history` rỗng. **Không** còn dòng chú thích nguồn nào dưới 3 vòng tròn (mục 3 #26) — nguồn số liệu nằm trong ghi chú của test case này.

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

### TC-HOME-12 — Lời chào dùng **tên tài khoản thật**, chế độ khách không hiện tên (phụ lục C #30)
- **Ưu tiên:** P1
- **Bước:**
  1. `pm clear com.healthyai.app` → mở app → bấm **"Bỏ qua, dùng ở chế độ khách"** → xem dòng dưới câu chào ở tab Trang chủ.
  2. Bấm icon **Đăng xuất** (mục 3 #19) → quay về màn đăng nhập → **đăng nhập** bằng tài khoản email/mật khẩu (TC-AUTH-03) → xem lại dòng dưới câu chào.
- **Kết quả mong đợi:**
  - **Chế độ khách:** **không có dòng tên nào**; câu chào gọn **"Chào buổi sáng"** / **"Chào buổi chiều"** / **"Chào buổi tối"** / **"Chào buổi đêm"** (không có dấu phẩy treo). **Không** còn hiện "Alex".
  - **Đã đăng nhập:** dòng dưới câu chào là **tên tài khoản thật**, màu xanh brand `#2E7D32`. Cụ thể theo thứ tự ưu tiên:
    - nếu tài khoản Firebase có `displayName` → hiện `displayName`;
    - nếu `displayName` trống (tài khoản tạo trong app luôn như vậy) → hiện **phần trước `@` của email** (ví dụ `alex.healthyai@gmail.com` → `alex.healthyai`).
  - Đăng xuất rồi vào lại bằng **chế độ khách** → dòng tên biến mất ngay khi màn Trang chủ được dựng lại.
- **Ghi chú:** Bản sửa phụ lục C #30: trước đây `HomeScreen.GreetingHeader` hard-code `Text("Alex")` cho **mọi** phiên (khách lẫn đăng nhập thật). Tên nay lấy từ `AuthRepository.currentUserName` → `HomeViewModel` → `HomeUiState.userName`; `null` (khách) thì UI **không** vẽ dòng tên.

### TC-HOME-13 — Unit test JVM cho tên hiển thị trên lời chào (phụ lục C #30)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HomeViewModelTest" --tests "*AuthRepositoryImplTest"`.
- **Kết quả mong đợi:** Pass cả hai class, trong đó:
  - `HomeViewModelTest` (18 test, 2 test mới): `AuthRepository.currentUserName = "alex.healthyai"` → `uiState.userName` giữ nguyên chuỗi đó; `currentUserName = null` (chế độ khách) → `uiState.userName == null` (UI không có dòng tên).
  - `AuthRepositoryImplTest` (4 test, 3 test mới): có `displayName` → dùng `displayName`; `displayName = null` **hoặc** chỉ khoảng trắng → lấy phần trước `@` của email; chưa đăng nhập (`currentUser == null`) → `currentUserName == null`.
- **Ghi chú:** JVM không có Firebase nên test mock `FirebaseAuth`/`FirebaseUser` bằng MockK; kiểm tra bằng mắt (dòng chào trên màn Trang chủ) vẫn theo **TC-HOME-12**.

### TC-HOME-14 — Ô "Tim" trên thẻ "Tóm tắt sức khỏe" mở màn đo nhịp tim
- **Ưu tiên:** P1
- **Tiền điều kiện:** Health Connect **chưa có** nhịp tim (máy không có đồng hồ đeo tay / chưa bơm dữ liệu giả) — hoặc mục 2.5 bước 3 **chưa** làm.
- **Bước:**
  1. Mở app → tab **Trang chủ** → xem ô thứ ba của thẻ "Tóm tắt sức khỏe" (nhãn **"Tim"**).
  2. Bấm vào **ô "Tim"** (chỗ có dòng **"Đo ngay"** màu xanh).
  3. Đo xong (theo **TC-HR-04**) → bấm **"Xong"** để quay về Trang chủ.
  4. Bơm dữ liệu nhịp tim giả vào Health Connect (mục 2.5 bước 3) → rời Trang chủ rồi vào lại.
- **Kết quả mong đợi:**
  - Bước 1: ô "Tim" hiện **`-- bpm`** (hôm nay chưa có số nào — xem TC-HOME-15) và luôn có dòng **"Đo ngay"** — đây là lối vào màn đo (không thêm tab mới ở bottom bar, vẫn **4 tab**).
  - Bước 2: mở màn **"Đo nhịp tim"**, đèn flash bật, thấy thẻ hướng dẫn **"Cách đo nhịp tim"** (3 bước) và vùng xem trước camera — xem **TC-HR-01/TC-HR-03**.
  - Bước 3: ô "Tim" hiện **số vừa đo** (ví dụ `74 bpm`). **Không** có dòng chú thích nguồn nào dưới thẻ (mục 3 #26).
  - Bước 4: ô "Tim" **chuyển sang số của Health Connect** (trung bình từ 00:00 hôm nay) — Health Connect luôn được ưu tiên khi có số.
- **Ghi chú:** Nguồn dữ liệu: `HomeViewModel.getLatestRecordedHeartRateUseCase` (đọc `health_data_history` **của hôm nay**) → `HomeUiState.recordedHeartRate` → `displayHeartRate`; số đo được ghi vào `health_data_history` ngay khi chốt (TC-HR-06), nên **cả** tab Phân tích (Rule Engine đọc lịch sử sức khỏe) cũng dùng được. Đơn vị test JVM: 4 test nhịp tim trong `HomeViewModelTest` (TC-HR-07, TC-HOME-15).

### TC-HOME-15 — Ô "Tim" **đọc lại từ đầu ngày** và **không còn dòng chú thích nguồn** (mục 3 #26)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã đo nhịp tim bằng camera **trong ngày hôm qua** (hoặc bơm `HeartRateRecord` giả vào Health Connect rồi **chờ qua ngày**, mục 2.5 bước 3) để có số của **hôm qua**; hôm nay **chưa** đo lần nào.
- **Bước:**
  1. Mở app → tab **Trang chủ** → xem ô **"Tim"** và phần **dưới** 3 vòng tròn chỉ số.
  2. Bấm **"Đo ngay"** → đo xong (TC-HR-04) → quay về Trang chủ → xem lại ô "Tim".
  3. Đổi giờ hệ thống sang **ngày kế tiếp (00:01)** → rời Trang chủ rồi vào lại → xem ô "Tim".
  4. Mở tab **Phân tích** → kéo xuống thẻ **"Lịch sử sức khỏe 7 ngày"** → tìm dòng của **hôm qua**.
- **Kết quả mong đợi:**
  - Bước 1: ô "Tim" hiện **`-- bpm`** — số của **hôm qua không được hiện lại** (đọc lại từ đầu ngày giống bước chân/giờ ngủ), và **không** hiện `0 bpm` (số 0 bị hiểu là "nhịp tim bằng 0").
  - Bước 1: **dưới** 3 vòng tròn **không có dòng chữ nhỏ nào** (trước đây là các dòng *"Số bước đọc trực tiếp từ cảm biến của điện thoại"*, *"Giờ ngủ ước lượng từ cảm biến…"*, *"Nhịp tim ghi gần nhất trên máy…"*).
  - Bước 2: ô "Tim" hiện **số vừa đo** (ví dụ `74 bpm`).
  - Bước 3: ô "Tim" về lại **`-- bpm`** (chưa đo trong ngày mới) — nhưng **không** mất dữ liệu.
  - Bước 4: dòng của **hôm qua** vẫn có **nhịp tim đã đo** (ví dụ `74`) — số cũ **nằm nguyên trong lịch sử** để AI phân tích, chỉ **không** được hiện lại ở ô "Tim" của ngày mới.
- **Ghi chú:** `HealthConnectDataSource.getTodayHeartRate()` (trung bình **00:00 → hiện tại**), `HealthDataDao.getLatestHeartRateSince(fromTs)` (repository truyền **đầu ngày hôm nay**), `HomeUiState.displayHeartRate` (`0.0` → `formatHeartRate()` trả `-- bpm`). Đơn vị test JVM: `HealthDataRepositoryImplTest` (hỏi DAO đúng mốc 00:00) + `HomeViewModelTest` (`a new day with no measurement yet keeps the heart circle empty`).

### TC-HOME-16 — Thẻ **"Đồng bộ đám mây"**: mặc định TẮT, người dùng tự bật (mục 3 #31)
- **Ưu tiên:** P0 (riêng tư)
- **Tiền điều kiện:** `pm clear com.healthyai.app` (lần cài đầu) → vào app (đăng nhập **hoặc** chế độ khách đều được).
- **Bước:**
  1. Mở tab **Trang chủ**, tìm thẻ **"Đồng bộ đám mây"** (ngay dưới thẻ **"Khung giờ ngủ"**).
  2. Gạt công tắc **bật** rồi **tắt**, mỗi lần quan sát mô tả dưới công tắc.
  3. Kiểm tra bảng `app_settings` (mục 2.3) sau mỗi lần gạt.
  4. Thử ở **chế độ khách**: gạt bật → xem có dòng nhắc nào xuất hiện không.
- **Kết quả mong đợi:**
  - Bước 1: công tắc **đang TẮT**, mô tả là *"Đang TẮT: nhật ký chỉ nằm trên máy bạn và được mã hoá. Bật nếu bạn muốn sao lưu nhật ký mới lên đám mây."*, icon `CloudOff`, và có nút **"Xem chính sách bảo mật"**.
  - Bước 2 (bật): mô tả đổi sang *"Nhật ký thói quen tạo từ lúc bật sẽ được sao lưu lên đám mây (Firestore)…"*, icon `CloudUpload`.
  - Bước 3: gạt **bật** ghi **2 khoá** `cloud_sync_enabled = true` **và** `cloud_sync_consent_at` = **thời điểm bật** (epoch millis); gạt **tắt** chỉ ghi `cloud_sync_enabled = false` (**không** ghi mốc mới, **không** xoá gì trên cloud).
  - Bước 4 (khách): khi công tắc **bật** nhưng chưa đăng nhập → hiện thêm dòng cam *"Bạn đang ở chế độ khách — cần đăng nhập thì nhật ký mới được sao lưu."*
  - Trạng thái công tắc **được giữ** sau khi rời Trang chủ rồi quay lại (đọc từ DB qua Flow).
- **Ghi chú:** Đây là **cổng riêng tư** (phụ lục C #37). `CloudSyncPolicy.logsToUpload()` là **một chỗ duy nhất** quyết định log nào được tải lên: TẮT → không tải gì; thiếu mốc đồng ý → không tải gì; đã bật → **chỉ** log có `loggedAt >= consentAtMillis`. Đơn vị test JVM: `CloudSyncPolicyTest` (5 test), `SettingsRepositoryImplTest` (khoá/mặc định/giá trị hỏng), `HomeViewModelTest` (mặc định TẮT, phản chiếu thiết lập, ghi qua repository, cờ đăng nhập).

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

Màn hình: top bar **"Thêm thói quen mới"** (hoặc **"Sửa thói quen"**), nút back, nút xóa (chỉ ở chế độ sửa), ô tên, 5 ô danh mục, ô mục tiêu + đơn vị, 4 chip thời gian, **tần suất chỉ còn "Hàng ngày"** (radio không bấm được + dòng chú thích — mục 3 #30), nút đáy **"Tạo thói quen"** / **"Cập nhật"** (khi đang lưu: nút mờ đi + hiện vòng xoay — chống double-submit, xem **TC-ADD-14**). Ở chế độ Sửa, ô mục tiêu điền sẵn **số sạch** — `2000` thay vì `2000.0` (xem **TC-ADD-23**). Lỗi validate (tên trống, mục tiêu ≤ 0, **tên trùng**) hiện trong hộp thoại **"Lỗi"** ngay trên màn này.

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

### TC-ADD-20 — Tần suất: chỉ còn "Hàng ngày" (mục 3 #30)
- **Ưu tiên:** P2
- **Bước:** 1. Mở màn Thêm (và màn Sửa của một thói quen đã có) → 2. Cuộn xuống phần **"Tần suất"**.
- **Kết quả mong đợi:**
  - Chỉ hiện **một** lựa chọn **"Hàng ngày"** (radio đã chọn, **không bấm được** — chỉ còn một lựa chọn nên không có gì để đổi) kèm dòng chú thích *"App đang tính tiến độ theo từng ngày, nên tần suất chỉ có \"Hàng ngày\"."*.
  - **Không** còn hai lựa chọn **"Hàng tuần"** / **"Hàng tháng"**.
  - Tạo/Cập nhật thói quen → DB lưu `frequency = 'DAILY'`. Thói quen **cũ** lỡ lưu `WEEKLY`/`MONTHLY` (chọn được ở bản trước nhưng **không** có tác dụng gì) được **chuẩn hoá về `DAILY`** ngay khi người dùng bấm "Cập nhật".
- **Ghi chú:** Trước bản sửa #30, `HabitFrequency.WEEKLY`/`MONTHLY` **chọn được** ở màn Thêm/Sửa nhưng **không logic nào đọc** (`GetTodayHabitsUseCase`, `GetWeeklyConsistencyUseCase`, `HomeViewModel`, `HabitsViewModel`, `VitalityScoreMath`, `TodayHabitProgress` đều xét theo NGÀY) ⇒ chọn xong chỉ đổi nhãn, gây hiểu nhầm. Enum `HabitFrequency` và cột `habits.frequency` **vẫn giữ nguyên** (không cần migration); chỉ rút lựa chọn ở UI cho tới khi có logic theo chu kỳ.

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

### TC-ADD-28 — Unit test JVM: tần suất luôn được lưu là "Hàng ngày" (mục 3 #30)
- **Ưu tiên:** P2
- **Bước:** 1. `.\gradlew test --tests "*AddEditHabitViewModelTest"`.
- **Kết quả mong đợi:** `AddEditHabitViewModelTest` pass (**16 test**), trong đó 1 test cho mục 3 #30: mở màn Sửa một thói quen **cũ** đang lưu `frequency = WEEKLY` → bấm **Cập nhật** → `updateHabitUseCase` nhận `Habit` có `frequency == HabitFrequency.DAILY`.
- **Ghi chú:** `AddEditHabitViewModel` không còn trường `frequency` trong UI state và luôn ghi `HabitFrequency.DAILY`; enum + cột DB giữ nguyên nên **không** cần migration.

---

## 10. TC-CAM — Màn hình Ghi chép (Camera + AI phân tích món ăn)

Thành phần: tiêu đề **"Ghi chép bữa ăn"**, phụ đề **"Hướng camera vào món ăn, hoặc chọn ảnh có sẵn trong thư viện."**, thẻ **"Cần quyền Camera"** khi chưa cấp quyền, preview camera thật 400dp (CameraX `PreviewView`) + khung quét 220dp + chip **"Quét món ăn"**, nút **"Phân tích món ăn"** (chụp ảnh → **AI Gemini**), nút viền **"Chọn ảnh từ thư viện"** (photo picker của hệ thống → AI), **hộp thoại "Đang phân tích món ăn"** trong lúc chờ AI (mục 3 #43), thẻ lỗi đỏ (kèm nút **"Thử lại"** khi còn ảnh của lượt trước), thẻ **"Kết quả phân tích món ăn"** (tên món + **Có lợi cho cơ thể** + **Nên lưu ý** + dòng miễn trừ) và thẻ **"Chưa nhận diện được món ăn"** khi ảnh không có gì ăn được (phụ lục C #21, #49).

> ⚠️ **Đổi nền tảng ở mục 3 #42:** màn này **không còn dùng ML Kit Image Labeling** nữa (bộ gán nhãn ảnh chung là nguồn của lỗi *"chụp đĩa cơm mà ra pizza"*). Nay ảnh được nén JPEG rồi gửi tới **Gemini** để lấy **tên món + lợi ích/điều nên lưu ý**.
>
> ⚠️ **Đổi UI ở mục 3 #43:** **bỏ** thẻ cổng đồng ý / công tắc **"Phân tích món ăn bằng AI"** (cùng thiết lập `food_ai_enabled`) — bấm "Phân tích món ăn" / "Chọn ảnh từ thư viện" là **ảnh được gửi đi phân tích luôn**; **hộp thoại "Đang phân tích món ăn"** cho biết app đang làm việc; nút **"Xem chính sách bảo mật"** **chuyển sang tab Phân tích AI** (thẻ "AI & quyền riêng tư" — **TC-INS-42**, **TC-PRIV-01**, **TC-PRIV-04**). Model mặc định nay là **`gemini-3.8-flash`** với `thinkingLevel: low` và có **model dự phòng** `gemini-flash-lite-latest` — **TC-CAM-25**, **TC-CAM-26**.

### TC-CAM-01 — Chưa cấp quyền Camera
- **Ưu tiên:** P0
- **Tiền điều kiện:** `pm clear` app (quyền Camera chưa cấp).
- **Bước:** 1. Mở tab Ghi chép.
- **Kết quả mong đợi:** Hiển thị thẻ trắng: icon camera xanh 64dp, tiêu đề **"Cần quyền Camera"**, mô tả **"Để chụp ảnh món ăn, ứng dụng cần truy cập camera của bạn."** + dòng nhỏ **"Chưa muốn cấp quyền? Bạn vẫn có thể chọn ảnh có sẵn trong thư viện."**, nút xanh **"Cấp quyền Camera"**. **Không** thấy preview và nút **"Phân tích món ăn"**; **có** thẻ cổng đồng ý AI và nút **"Chọn ảnh từ thư viện"** ngay dưới thẻ này (mục 3 #25 — không cần quyền Camera).

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
  - Chữ trên nút đổi ngay thành **"Đang phân tích..."** (nút bị **disable** trong lúc xử lý) và **hộp thoại "Đang phân tích món ăn"** hiện ra (mục 3 #43, xem **TC-CAM-25**).
  - Sau khi **AI trả kết quả**: hộp thoại biến mất, hiện thẻ **"Kết quả phân tích món ăn"** và nút trở lại **"Phân tích món ăn"**.
- **Ghi chú:** Nút chỉ bật khi camera đã bind xong **và** AI không đang xử lý lượt trước. **Mục 3 #43 đã bỏ công tắc "Phân tích món ăn bằng AI"** (và cổng `food_ai_enabled`) nên **không cần bật gì trước** — bấm là ảnh được gửi đi phân tích. Ảnh chụp được **nén JPEG** (`ImageJpegEncoder`, cạnh dài ≤1280 px, chất lượng 80) trước khi gửi.

### TC-CAM-06 — Trạng thái đang phân tích
- **Ưu tiên:** P2
- **Bước:** 1. Bấm **"Phân tích món ăn"** và quan sát ngay.
- **Kết quả mong đợi:**
  - **Hộp thoại "Đang phân tích món ăn"** hiện ngay: vòng xoay xanh + tiêu đề *"Đang phân tích món ăn"* + dòng *"AI đang đọc ảnh để nhận diện món và phân tích lợi ích, điều nên lưu ý cho cơ thể. Vui lòng chờ trong giây lát."*
  - Hộp thoại **không tắt được**: bấm **Back** và **chạm ra ngoài** đều không đóng (lượt gọi AI vẫn đang chạy; đóng đi sẽ gây hiểu nhầm là đã huỷ).
  - Chữ trên nút đổi thành **"Đang phân tích..."**; hai nút phân tích **không bấm được** cho tới khi có kết quả → không chồng nhiều lượt phân tích. `isAnalyzing` được đặt **đồng bộ** ngay khi bắt đầu lượt phân tích nên hộp thoại hiện tức thì (không chờ coroutine vào Main).
  - Khi có kết quả (hoặc lỗi): hộp thoại **tự đóng**, hai nút bật lại.
- **Ghi chú:** Mục 3 #43. Hộp thoại nằm **ngoài** `Scaffold` (dialog là cửa sổ riêng) nên không ảnh hưởng bố cục danh sách bên dưới.

### TC-CAM-07 — Kết quả phân tích món ăn (mục 3 #42; phụ lục C #21, #49)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Không cần thiết lập gì thêm (mục 3 #43: màn này **không còn** công tắc AI); đưa vào khung hình một món ăn rõ ràng.
- **Bước:** 1. Bấm **"Phân tích món ăn"** → 2. Chờ kết quả.
- **Kết quả mong đợi:** Thẻ **"Kết quả phân tích món ăn"** với nhãn góc phải **"Gemini AI"**, gồm:
  - **tên món tiếng Việt** (ưu tiên gọi đúng tên món Việt/Á — ví dụ `Cơm tấm sườn`, `Phở bò`, `Bún chả`) + **1 emoji** minh hoạ;
  - mục **"Có lợi cho cơ thể"** (icon ✓ xanh) với **1–3** gạch đầu dòng, mỗi gạch **≤ 120 ký tự**;
  - mục **"Nên lưu ý"** (icon ⚠ cam) với **1–3** gạch đầu dòng cùng giới hạn — **không** chẩn đoán bệnh, **không** nói về thuốc/chữa bệnh;
  - dòng **miễn trừ trách nhiệm** *"Thông tin tham khảo do AI phân tích từ ảnh, KHÔNG phải tư vấn y tế. Hãy hỏi bác sĩ hoặc chuyên gia dinh dưỡng nếu bạn có bệnh nền."* + dòng nói rõ AI **không** cân đo được khối lượng/calo (muốn ghi số liệu thì bấm "Thêm vào nhật ký" để tự nhập);
  - nút viền **"Thêm vào nhật ký"** — mở hộp thoại ghi nhận thói quen **Dinh dưỡng** (xem **TC-CAM-15**).
  - Món **trái cây / đồ uống / đồ ăn vặt** cũng phải nhận ra được (ví dụ `Thanh long`, `Trà sữa`, `Bắp rang bơ`) — không chỉ món mặn.
- **Ghi chú:** ~~ML Kit Image Labeling trả nhãn ảnh chung bằng tiếng Anh ⇒ `FoodRecognition` phải tự lọc theo danh mục ~180 từ khoá rồi tự dịch sang tiếng Việt, và **không** nói được gì về tác dụng cho cơ thể~~ **→ Đã sửa (mục 3 #42)**: nay **tên món và nội dung lợi ích/lưu ý đều do Gemini sinh ra**, ràng buộc bằng `FoodAnalysis.buildPrompt()` (tiếng Việt, ưu tiên món Việt/Á, phủ trái cây/đồ uống/đồ ăn vặt, **bắt ghi "(có thể)"** khi không chắc — chống đúng lỗi *"đĩa cơm → pizza"*, và **cấm** phát ngôn y tế) rồi **làm sạch** ở `FoodAnalysis.toInsight()` (≤3 gạch/mục, ≤120 ký tự/gạch, bỏ mục rỗng/trùng, emoji mặc định 🍽️).

### TC-CAM-08 — Ảnh không có món ăn (phụ lục C #21)
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đã cấp quyền Camera.
- **Bước:** 1. Hướng camera vào bàn trống / bầu trời / người (ảnh chụp "lung tung") → 2. Bấm **"Phân tích món ăn"**.
- **Kết quả mong đợi:**
  - AI trả `is_food = false` → `uiState.insight = null` và `noFoodDetected = true`.
  - Hiện thẻ **"Chưa nhận diện được món ăn"** (icon `NoFood` xanh + dòng *"AI không thấy món ăn nào trong ảnh này. Hãy chụp rõ hơn (đủ sáng, gần hơn, không bị che), hoặc chọn một ảnh khác trong thư viện."*), **không** hiện thẻ "Kết quả phân tích món ăn" và **không** có thẻ lỗi đỏ.
  - Trường hợp AI nói `is_food = true` nhưng **không gọi được tên món** cũng rơi vào nhánh này (một thẻ kết quả không có tên món thì không nói lên điều gì).
  - Nút trở lại trạng thái thường; app không crash.

### TC-CAM-09 — Lỗi khi phân tích
- **Ưu tiên:** P2
- **Tiền điều kiện:** Không cần bật gì (mục 3 #43: màn này không còn công tắc AI).
- **Bước:** 1. Gây lỗi khi phân tích (ví dụ ngắt mạng, hoặc để AI trả lỗi).
- **Kết quả mong đợi:** `uiState.error` là **câu tiếng Việt riêng theo loại lỗi** (mất mạng / AI quá tải / khoá AI sai / máy chủ AI lỗi / phản hồi không đọc được / chưa cấu hình khoá) và `isAnalyzing = false` ⇒ **hộp thoại "Đang phân tích món ăn" tự đóng**; UI hiển thị **thẻ lỗi đỏ** (`CameraErrorCard`) ngay dưới nút, kèm nút **"Thử lại"** khi còn ảnh của lượt trước; app không crash. Bắt đầu lượt phân tích mới → thẻ lỗi tự xoá. Chi tiết từng loại lỗi: **TC-CAM-23**.

### TC-CAM-10 — Thu hồi quyền Camera khi đang mở app
- **Ưu tiên:** P2
- **Bước:** 1. Đang ở tab Ghi chép (đã cấp quyền) → 2. Vào Settings thu hồi quyền Camera → 3. Quay lại app.
- **Kết quả mong đợi:** Trở về thẻ xin quyền sau khi màn hình được tạo lại; không crash.

### TC-CAM-11 — Nút phân tích bị disable khi camera chưa sẵn sàng
- **Ưu tiên:** P1
- **Bước:** 1. Vừa cấp quyền Camera → 2. Bấm **"Phân tích món ăn"** ngay lập tức (trước khi preview hiện hình).
- **Kết quả mong đợi:** Trong khoảng thời gian camera đang khởi tạo, nút ở trạng thái **mờ/không bấm được**; sau khi preview hiện hình thì nút bật và bấm được. Không crash, không xảy ra lỗi chụp ảnh.
- **Ghi chú:** Nút bật khi `captureController != null` (đã bind lifecycle xong) **và** AI không đang xử lý lượt trước. Mục 3 #43 đã **bỏ** điều kiện thứ hai là "công tắc AI đang BẬT" — nay không còn công tắc nào ở màn này.

### TC-CAM-12 — Không mở được camera
- **Ưu tiên:** P1
- **Tiền điều kiện:** Trường hợp camera bị chiếm bởi app khác, hoặc emulator không có camera ảo.
- **Bước:** 1. Mở tab Ghi chép (đã cấp quyền) khi camera không khả dụng.
- **Kết quả mong đợi:** Hiện **thẻ lỗi đỏ** (`CameraErrorCard`) với nội dung **"Không mở được camera: <message>"** thay cho khối preview; app **không crash**; nút phân tích ở trạng thái không bấm được.

### TC-CAM-13 — Chụp và phân tích liên tiếp nhiều lần
- **Ưu tiên:** P2
- **Bước:** 1. Bấm phân tích → 2. Chờ kết quả → 3. Đổi món ăn trước camera → 4. Bấm phân tích lại (lặp 3 lần).
- **Kết quả mong đợi:** Mỗi lượt: kết quả (và thẻ lỗi) của lượt trước bị xoá **ngay khi bắt đầu** lượt mới, kết quả mới thay thế kết quả cũ; mỗi lượt là **một** request tới AI; không rò rỉ `ImageProxy` (không có log `ImageProxy` chưa close); không giảm FPS/treo sau nhiều lượt.

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
- **Ghi chú:** Đây là bản sửa lỗi **phụ lục C #22** (trước đây `onClick` là `/* TODO */`). Kết quả của AI **không** tự điền vào log (chỉ có **tên món** tiếng Việt + lợi ích/điều nên lưu ý, **không** có calo/khối lượng — phụ lục C #21, #49) — người dùng nhập số lượng/nội dung ở hộp thoại ghi nhận.

### TC-CAM-16 — ~~Unit test JVM cho luật lọc & dịch nhãn ảnh thành món ăn (phụ lục C #21)~~ **→ Không còn (mục 3 #42)**
- **Trạng thái:** **Bỏ** — ML Kit và lớp `FoodRecognition` (cùng **15 test** của nó) đã **bị xoá** vì bộ gán **nhãn ảnh chung** chính là nguồn của lỗi *"đĩa cơm → pizza"*. Phần kiểm thử thay thế nằm ở **TC-CAM-24** (`FoodAnalysisTest` + `GeminiFoodInsightSourceTest`).
- **Ghi chú:** Không còn code nào chạy luật cũ; muốn xem lại thì đọc ở lịch sử git.

### TC-CAM-17 — Unit test JVM cho máy trạng thái màn Ghi chép (mục 3 #42, #43; phụ lục C #21, #23)
- **Ưu tiên:** P1
- **Bước:** 1. Chạy `testDebugUnitTest` (TC-ENV-03) → 2. Xem kết quả của `CameraViewModelTest`.
- **Kết quả mong đợi:** **15 test** chạy qua (17 trước #43 — bỏ 3 test của cổng opt-in, thêm 1 test chốt "bấm là gọi AI"), gồm:
  - AI trả món → `insight` là **tên món tiếng Việt** kèm `benefits`/`cautions`, `noFoodDetected = false`, không lỗi;
  - ngay khi bắt đầu lượt phân tích: `isAnalyzing = true` và **chưa có** kết quả — màn hình dựa vào đúng cờ này để hiện hộp thoại "Đang phân tích món ăn" (TC-CAM-25);
  - **bấm phân tích là gọi AI luôn** (`source.calls == 1`) — mục 3 #43 đã bỏ công tắc nên không còn gì chặn;
  - AI nói ảnh **không có đồ ăn** → `noFoodDetected = true`, `insight = null`, **không** lỗi; AI nói "có" nhưng **không nêu tên món** cũng rơi vào nhánh này;
  - mỗi loại lỗi ra **một câu riêng** (mất mạng / AI quá tải / khoá AI sai / chưa cấu hình khoá) và `canRetry = true` khi đã có ảnh;
  - nút **"Thử lại"** gửi lại **đúng ảnh của lượt trước** (`calls == 2`, cùng bytes) và thành công thì xoá lỗi;
  - lượt mới **xoá kết quả và lỗi của lượt trước**; lỗi ngoài dự kiến → `"Lỗi phân tích món ăn: <message>"`; lỗi chụp ảnh / lỗi đọc ảnh thư viện thành thẻ lỗi riêng;
  - `clearResult()` đưa `CameraUiState` về **trạng thái đầu hoàn toàn** (không còn thiết lập nào để giữ lại).
- **Ghi chú:** Chỉ còn `FoodInsightSource` là được thay bằng giả lập (trước đây cả `SettingsRepository`) nên test chạy trên JVM — **không** cần camera, **không** cần mạng, **không** cần khoá API; trả một phần nợ ở **phụ lục C #23**.

### TC-CAM-18 — Chọn ảnh từ thư viện để phân tích (mục 3 #25)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Trong máy có sẵn một ảnh món ăn rõ ràng (ảnh chụp trước đó, tải về…). Nên thử **cả** ảnh chụp dọc (có cờ EXIF) và ảnh chụp ngang.
- **Bước:** 1. Mở tab **Ghi chép** → 2. Bấm nút viền **"Chọn ảnh từ thư viện"** → 3. Trong photo picker của hệ thống chọn ảnh món ăn → 4. Chờ kết quả.
- **Kết quả mong đợi:**
  - Mở **photo picker của hệ thống** (không phải hộp thoại xin quyền); chọn ảnh xong thì app trở về màn Ghi chép và bắt đầu phân tích (nút phân tích + nút thư viện **mờ đi** trong lúc chạy).
  - Sau vài giây: thẻ **"Kết quả phân tích món ăn"** hiện **tên món tiếng Việt** + lợi ích/điều nên lưu ý như khi chụp bằng camera (TC-CAM-07) → bấm **"Thêm vào nhật ký"** vẫn mở hộp thoại ghi nhận **Dinh dưỡng** (TC-CAM-15).
  - **Ảnh chụp dọc** phải được nhận diện đúng (không bị quay 90°): `GalleryImageLoader` xoay theo cờ `TAG_ORIENTATION` của EXIF.
  - Không crash với ảnh lớn (ảnh 12–50 MP được thu nhỏ trước khi giải mã, cạnh dài còn ~1280 px).
- **Ghi chú:** Dùng `ActivityResultContracts.PickVisualMedia(ImageOnly)` nên **không** cần quyền đọc thư viện (`READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE` không được xin); ảnh được đọc ở luồng IO nên UI không bị "đứng".

### TC-CAM-19 — Chọn ảnh **không phải đồ ăn** từ thư viện
- **Ưu tiên:** P2
- **Tiền điều kiện:** Trong máy có ảnh không phải đồ ăn (ảnh bàn làm việc, phong cảnh, ảnh chụp màn hình…).
- **Bước:** 1. Tab Ghi chép → 2. **"Chọn ảnh từ thư viện"** → 3. Chọn ảnh đó.
- **Kết quả mong đợi:** Thẻ **"Chưa nhận diện được món ăn"** (icon `NoFood`, gợi ý chụp/chọn ảnh rõ hơn) — **không** có thẻ kết quả nào và **không** có thẻ lỗi đỏ (phụ lục C #21, TC-CAM-08).

### TC-CAM-20 — Huỷ chọn ảnh / ảnh đọc lỗi / không cấp quyền Camera (mục 3 #25)
- **Ưu tiên:** P2
- **Bước:**
  1. `pm clear` (quyền Camera chưa cấp) → tab Ghi chép → bấm **"Chọn ảnh từ thư viện"** → chọn ảnh món ăn.
  2. Bấm **"Chọn ảnh từ thư viện"** lần nữa → trong photo picker bấm **Huỷ** (back).
  3. Chọn một tệp **không phải ảnh** / tệp đã bị xoá (hoặc đổi tên đuôi thành ảnh rồi chọn).
- **Kết quả mong đợi:**
  - (1) Phân tích **vẫn chạy bình thường dù chưa cấp quyền Camera** — kết quả là thẻ "Kết quả phân tích món ăn" hoặc "Chưa nhận diện được món ăn"; **không** có thẻ xin quyền nào chặn đường.
  - (2) Bấm Huỷ → app **im lặng**: không thẻ lỗi, không thay đổi kết quả cũ, vẫn ở màn Ghi chép.
  - (3) Ảnh đọc lỗi → thẻ lỗi đỏ **"Không đọc được ảnh đã chọn: tệp không phải ảnh hoặc đã bị xoá"**, nút trở lại trạng thái thường, app **không crash**.
- **Ghi chú:** Nhánh (2)/(3) nằm ở `CameraScreen` (`uri == null` → bỏ qua) và `GalleryImageLoader.load()` trả `null` → `CameraViewModel.onImageLoadError()`.

### TC-CAM-21 — ~~Cổng đồng ý: công tắc AI **mặc định TẮT** & chặn gửi ảnh (mục 3 #42)~~ **→ Không còn (mục 3 #43)**
- **Trạng thái:** **Bỏ** — công tắc **"Phân tích món ăn bằng AI"** và cổng `food_ai_enabled` (cùng 4 test JVM của nó trong `SettingsRepositoryImplTest`) đã **bị xoá** theo yêu cầu người dùng: bấm "Phân tích món ăn" / "Chọn ảnh từ thư viện" là ảnh **được gửi đi phân tích luôn**.
- **Cần kiểm tra lại thay vào đó (không phải test case riêng, chỉ là đối chiếu):**
  1. Mở tab **Ghi chép** → **không** còn thẻ "Phân tích món ăn bằng AI", **không** còn công tắc, **không** còn 2 dòng ghi chú *"Đang BẬT/TẮT…"*, **không** còn nút "Xem chính sách bảo mật" ở màn này.
  2. `strings.xml` không còn khoá `camera_food_ai_*`; `SettingsRepository` không còn `observeFoodAiEnabled`/`setFoodAiEnabled`.
  3. Việc **ảnh rời khỏi thiết bị** vẫn được nói rõ — nhưng ở **Chính sách bảo mật**, mở từ thẻ **"AI & quyền riêng tư"** ở **tab Phân tích AI** (**TC-INS-42**, **TC-PRIV-01**, **TC-PRIV-04**).
- **Ghi chú:** Hành vi "gửi ảnh khi bấm phân tích" được **khoá bằng test JVM** trong `CameraViewModelTest` (`source.calls == 1` ngay sau khi gọi `runAnalysis`) — TC-CAM-17.

### TC-CAM-22 — Trái cây / đồ uống / đồ ăn vặt cũng phân tích được (mục 3 #42)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Không cần thiết lập gì (mục 3 #43).
- **Bước:** Lần lượt chụp (hoặc chọn ảnh) **4** loại: (1) **món Việt** (đĩa cơm / bát phở / bánh mì), (2) **trái cây** (đĩa trái cây, quả nguyên), (3) **đồ uống** (ly trà sữa / cà phê / nước ngọt), (4) **đồ ăn vặt / bánh kẹo**.
- **Kết quả mong đợi:**
  - Cả 4 loại đều ra thẻ kết quả với **tên tiếng Việt**; không trường hợp nào bị "đoán" thành một món Tây khác hẳn.
  - **Món Việt được gọi gần đúng tên Việt** (ví dụ `Cơm tấm`, `Phở bò`, `Bánh mì`, `Cháo`); khi AI không chắc thì tên có kèm **"(có thể)"** thay vì đoán bừa (yêu cầu trong prompt).
  - **Đĩa cơm KHÔNG còn ra "Pizza"** — đây chính là lỗi gốc của bản ML Kit cũ (mục 3 #42, phụ lục C #49).
  - Mục **"Nên lưu ý"** phản ánh đúng loại: đồ **chiên/nướng nhiều dầu mỡ**, **nhiều đường** (trà sữa, bánh kẹo), **nhiều tinh bột** (cơm, mì)…
  - Mỗi mục giữ **≤ 3** gạch đầu dòng, mỗi gạch **≤ 120 ký tự** (AI trả dài hơn thì bị cắt ở `FoodAnalysis.toInsight`).
- **Ghi chú:** Chất lượng phụ thuộc model. Từ mục 3 #43 model mặc định là **`gemini-3.8-flash`** (đổi ở `gemini.model` trong `local.properties` — **không** cần sửa code).

### TC-CAM-23 — Lỗi AI: mạng / quá tải / khoá sai / chưa cấu hình & nút **"Thử lại"** (mục 3 #42, #43)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Không cần bật gì (mục 3 #43).
- **Bước:**
  1. Bật **chế độ máy bay** → chụp ảnh món ăn → bấm **"Phân tích món ăn"**.
  2. Tắt chế độ máy bay → bấm **"Thử lại"** ở thẻ lỗi.
  3. (Tuỳ chọn) Xoá/đổi khoá `gemini.api.key` trong `local.properties` rồi build lại → bấm phân tích.
- **Kết quả mong đợi:**
  - (1) Thẻ lỗi đỏ *"Không kết nối được tới AI. Kiểm tra mạng rồi thử lại."* + nút **"Thử lại"**.
  - (2) Bấm **"Thử lại"** → app gửi lại **đúng ảnh vừa chụp** (không phải chụp lại) và ra thẻ kết quả.
  - (3) Build **không cấu hình khoá** → *"Bản cài này chưa có khoá AI (Gemini), nên chưa phân tích được món ăn."* và **không** có request nào ra mạng; khoá **sai / bị thu hồi** (401/403) → *"Khoá AI không hợp lệ hoặc đã bị thu hồi."*
  - AI **cao tải (429/503)** → app **tự chuyển sang model dự phòng** (`gemini-flash-lite-latest`, xem **TC-CAM-26**); **cả hai** model đều quá tải mới hiện *"AI đang quá tải. Vui lòng thử lại sau vài giây."*; lỗi **5xx** → *"Máy chủ AI đang gặp lỗi. Vui lòng thử lại."*; phản hồi không đọc được → *"AI trả về kết quả không đọc được. Vui lòng thử lại."*
  - App **không crash** ở bất kỳ nhánh nào; **hộp thoại "Đang phân tích món ăn" luôn đóng lại** dù lỗi.
- **Ghi chú:** **429/503 đã gặp thật** khi thử khoá (Google trả *"This model is currently experiencing high demand"* lặp nhiều lần liên tiếp, và *429 "You exceeded your current quota"*) nên thông báo cho người dùng phải nói rõ là **tạm thời**, không phải "app hỏng".

### TC-CAM-24 — Unit test JVM cho luật prompt/làm sạch & tầng Gemini (mục 3 #42, #43)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*FoodAnalysisTest" --tests "*GeminiFoodInsightSourceTest"`.
- **Kết quả mong đợi:** Toàn bộ pass:
  - `FoodAnalysisTest` (**17 test**): prompt đòi **tiếng Việt**, ưu tiên món **Việt Nam/châu Á**, phủ **trái cây / đồ uống / đồ ăn vặt**, nêu giới hạn **120 ký tự**, **cấm** chẩn đoán bệnh và nói về thuốc, **bắt ghi "(có thể)"** khi không chắc; `toInsight` **giữ nguyên** dữ liệu sạch, **ảnh không phải đồ ăn → `FoodInsight()` rỗng**, **có đồ ăn mà không có tên → coi như chưa nhận diện được**, **bỏ gạch rỗng/khoảng trắng**, giới hạn **3 gạch/mục**, **bỏ gạch trùng**, **cắt gạch và tên món quá dài**, **emoji thiếu hoặc là văn bản → 🍽️**.
  - `GeminiFoodInsightSourceTest` (**28 test**, 20 trước #43): đọc đúng vỏ `candidates[0].content.parts[0].text`; thiếu `candidates` / text không phải JSON / body rỗng → `BAD_RESPONSE`; ánh xạ mã HTTP (**200** ok, **401/403** → `AUTH`, **429/503** → `BUSY`, **500** → `SERVER`, **404/400** → `BAD_RESPONSE`); request có ảnh `inline_data` + MIME `image/jpeg` + prompt + `application/json` + `maxItems`, **không** kèm tiền tố `data:image`; **khoá API không nằm trong body**; URL dùng đúng **model cấu hình**; **thiếu khoá → không gọi mạng**; **mất mạng → `NETWORK`** sau khi thử **cả hai** model; schema **giới hạn số gạch khớp** `FoodAnalysis.MAX_BULLETS`; **request yêu cầu `thinkingLevel = "low"`** và **bỏ được `thinkingConfig`** khi cần; **model chính quá tải (503/429) → chuyển sang model dự phòng**; **cả hai quá tải → `BUSY` mà KHÔNG gọi lại cùng model** (đúng **2** request); **model dự phòng rỗng hoặc trùng tên model chính → chỉ 1 request**; **401 → không đổi model**; **model trả 400 *"Thinking level … not supported"* → gửi lại đúng model đó KHÔNG kèm `thinkingConfig`**; **400 vì lý do khác → không gửi lại**.
- **Ghi chú:** Cả hai lớp đều **thuần Kotlin/JVM**: HTTP nằm sau cổng `data/network/HttpPost` (test thay bằng `object : HttpPost`) và JSON đọc bằng **Gson** (thư viện Java thuần) — cố ý **không** dùng `org.json` của Android vì bản trong `android.jar` chỉ là stub nên không kiểm thử được.

### TC-CAM-25 — Hộp thoại **"Đang phân tích món ăn"** (mục 3 #43)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Camera, preview đã hiển thị.
- **Bước:**
  1. Bấm **"Phân tích món ăn"** (hoặc **"Chọn ảnh từ thư viện"** → chọn ảnh) và quan sát ngay.
  2. Trong lúc hộp thoại đang hiện: bấm **Back**, rồi **chạm ra ngoài** hộp thoại.
  3. Chờ AI trả kết quả (hoặc gây lỗi mạng để kết thúc sớm).
- **Kết quả mong đợi:**
  - Bước (1): hộp thoại hiện **ngay** — tiêu đề **"Đang phân tích món ăn"** + dòng *"AI đang đọc ảnh để nhận diện món và phân tích lợi ích, điều nên lưu ý cho cơ thể. Vui lòng chờ trong giây lát."* + **vòng xoay** xanh; màn hình phía sau **không bấm được** (dialog chặn tương tác).
  - Bước (2): **không** có cách nào đóng hộp thoại bằng Back hay chạm ra ngoài (`dismissOnBackPress = false`, `dismissOnClickOutside = false`) — lượt gọi AI vẫn đang chạy nên "đóng" đi chỉ gây hiểu nhầm là đã huỷ.
  - Bước (3): hộp thoại **tự đóng** khi có kết quả **hoặc** khi có lỗi; sau đó hiện thẻ kết quả / thẻ "Chưa nhận diện được món ăn" / thẻ lỗi như bình thường.
  - Xoay màn hình trong lúc chờ: hộp thoại dựng lại và **vẫn hiện** (cờ `isAnalyzing` nằm trong `CameraViewModel`).
- **Ghi chú:** Đây là phần bù cho việc **bỏ công tắc AI** (mục 3 #43): trước đây người dùng chỉ thấy chữ "Đang phân tích..." trên nút, mà nút nằm giữa một màn dài nên dễ tưởng app bị "treo". Kiểm tra nhanh bằng `@Preview CameraAnalyzingPreview` trong `CameraScreen.kt`.

### TC-CAM-26 — Tốc độ, model & model dự phòng của AI phân tích món ăn (mục 3 #43)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `local.properties` có `gemini.api.key` + `gemini.model=gemini-3.8-flash` + `gemini.fallback.model=gemini-flash-lite-latest`; đã build lại bản debug.
- **Bước:**
  1. Đọc `BuildConfig` trong APK/log hoặc kiểm tra `app/build.gradle.kts`: hai `buildConfigField` **`GEMINI_MODEL`** và **`GEMINI_FALLBACK_MODEL`**.
  2. Chụp một món ăn và **bấm đồng hồ**: từ lúc bấm "Phân tích món ăn" tới lúc thẻ kết quả hiện ra.
  3. Bấm **"Thử lại"** vài lần vào lúc Google cao tải (hoặc chờ tới khi gặp 503/429) → quan sát `adb logcat -s GeminiFoodInsight`.
- **Kết quả mong đợi:**
  - Request gửi tới `…/models/gemini-3.8-flash:generateContent` và **có** `generationConfig.thinkingConfig.thinkingLevel = "low"` (Gemini 3 mặc định `high` — đo thật một lượt đọc ảnh ở mức mặc định mất **~42 giây**).
  - Thời gian chờ **giảm rõ rệt** so với bản trước (mục tiêu: vài giây, không phải hàng chục giây) — hộp thoại "Đang phân tích món ăn" (TC-CAM-25) cho biết app vẫn đang chạy trong lúc chờ.
  - Khi model chính trả **503/429**: log có dòng chuyển model và app vẫn ra **thẻ kết quả** (không hiện thẻ lỗi "AI đang quá tải"); request thứ hai đi tới `…/models/gemini-flash-lite-latest:generateContent`.
  - Khi **cả hai** model đều quá tải: hiện đúng **2** request trong log (mỗi model 1 lần — **không** thử lại cùng model) rồi mới báo *"AI đang quá tải…"*.
  - Đổi tên model trong `local.properties` (ví dụ về một model đời cũ) → app **vẫn phân tích được**: log ghi *"Model … không nhận thinkingConfig"* và request được gửi lại **không** kèm `thinkingConfig`.
- **Ghi chú:** `"minimal"` (mức suy luận thấp hơn `"low"`) **không dùng được**: `gemini-3.8-flash` trả `400 INVALID_ARGUMENT` — *"Thinking level MINIMAL is not supported for this model."*. Model đời cũ dùng `thinkingBudget` (Gemini 2.5) thay cho `thinkingLevel` (Gemini 3) nên mới cần nhánh gửi lại – xem `GeminiFoodInsightSource.isThinkingConfigRejected`.

---

## 11. TC-INS — Màn hình Phân tích (Rule Engine AI)

Thành phần: tiêu đề **"Phân tích AI"** + phụ đề **"Xu hướng sức khỏe của bạn, được phân tích để phát triển."**; **feed gợi ý đa dạng & xoay vòng** (mục 3 #27) — mỗi thẻ là một **mẹo cải thiện** của một danh mục (`SuggestionCatalog`), có nút hành động **chuyển sang tab Thói quen và mở sẵn hộp thoại ghi nhận** (TC-INS-11), tối đa **4 thẻ**, sắp xếp theo mức ưu tiên; **"Chỉ số sinh lực"** — điểm ghép **thói quen đạt mục tiêu + giờ ngủ + bước chân theo từng ngày** kèm so sánh với tuần trước (mục 3 #28, TC-INS-12…TC-INS-17); **"Lịch sử sức khỏe 7 ngày"** (biểu đồ cột **bước chân** + **bảng 7 dòng** ngày · bước chân · giờ ngủ · nhịp tim, đọc từ `health_data_history` — TC-INS-31, TC-INS-32, mục 3 #26); **"Sự nhất quán hàng tuần"** (biểu đồ cột 7 ngày); **"Xu hướng chất lượng giấc ngủ"** — **biểu đồ 7 đêm giờ ngủ + xếp loại**, kèm chi tiết **Sâu/Nông/REM** khi Health Connect có dữ liệu pha (mục 3 #29, TC-INS-23, TC-INS-24); và ở **cuối màn** là thẻ **"AI & quyền riêng tư"** — lối vào **Chính sách bảo mật** của phần AI (chuyển từ màn Ghi chép về đây — mục 3 #43, TC-INS-42). Mọi thẻ đều lấy **dữ liệu thật**, không còn số liệu hard-code.

### TC-INS-01 — Trạng thái rỗng (chưa có dữ liệu)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `pm clear` (chưa có thói quen, chưa có log, chưa có hàng nào trong `health_data_history`).
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - **Feed gợi ý vẫn có thẻ** (mẹo khởi đầu lấy từ toàn bộ `SuggestionCatalog`, tối đa 4) — **không** còn màn "Mọi thứ đang rất tốt!" khi chưa có gì.
  - Thẻ **"Chỉ số sinh lực"** hiện **"Chưa đủ dữ liệu để đánh giá"** + lời nhắc tạo thói quen/ghi nhận (không còn hiện `50` mặc định).
  - Có đủ các thẻ: **Sự nhất quán hàng tuần**, **Lịch sử sức khỏe 7 ngày**, **Xu hướng chất lượng giấc ngủ** (thẻ giấc ngủ hiện hướng dẫn đặt **"Khung giờ ngủ"** ở Trang chủ hoặc cấp quyền Health Connect).

### TC-INS-02 — Nước uống thiếu → mẹo nhóm "cần chú ý"
- **Ưu tiên:** P0
- **Tiền điều kiện:** Thói quen **Uống nước** (mục tiêu 2000 ml); ghi **3 log 800 ml** trong các ngày gần nhất (trung bình < 1500).
- **Bước:** 1. Ghi 3 log `800` ml → 2. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - Thẻ danh mục **Uống nước** lấy **một mẹo nhóm CONCERN** (`SuggestionCatalog.concernTips(WATER)`): **"💧 Uống một ly nước ngay bây giờ"** (HIGH, nút **"Ghi nhận uống nước"**), **"🍶 Mang theo bình nước"** hoặc **"⏰ Uống nước theo mốc cố định"**.
  - Thẻ nền **theo theme** (`surfaceContainerLow` + lớp màu ưu tiên mờ 12%), nhãn **"BLOOM AI GỢI Ý"**.
  - **Không** còn thẻ cảnh báo cố định kiểu nhãn **"CẢNH BÁO KHẨN CẤP"** / tiêu đề **"🚨 Mức nước nguy hiểm thấp"** (mục 3 #27).
- **Ghi chú:** Mẹo cụ thể **đổi theo ngày** — xem TC-INS-06.

### TC-INS-03 — Nước uống đủ → mẹo duy trì
- **Ưu tiên:** P1
- **Tiền điều kiện:** Log nước trung bình ≥ 1500 ml.
- **Bước:** 1. Ghi 3 log nước `2000` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ danh mục **Uống nước** lấy mẹo nhóm **GENERAL** (`generalTips(WATER)`) — *"💧 Duy trì thói quen uống nước"* hoặc *"🍋 Thêm chút hương vị cho nước"*, mức **LOW**.

### TC-INS-04 — Feed chỉ gồm danh mục người dùng có thói quen
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chỉ có **2** thói quen đang hoạt động: **Uống nước** và **Giấc ngủ**; không có dữ liệu sức khỏe bất thường.
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:** Feed chỉ có thẻ thuộc **Uống nước** và **Giấc ngủ** (mỗi danh mục 1 mẹo) — **không** có Vận động/Dinh dưỡng/Thiền định.
- **Ghi chú:** Danh mục **đang có vấn đề** vẫn được đưa vào feed dù người dùng chưa có thói quen đó (ví dụ nhịp tim cao → Thiền định — TC-INS-10).

### TC-INS-05 — Ngủ thiếu (log thủ công) → mẹo Giấc ngủ nhóm CONCERN
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thói quen giấc ngủ (đơn vị giờ); 3 log gần nhất có trung bình **< 7 giờ**.
- **Bước:** 1. Ghi `5`, `5.5`, `4.5` cho thói quen giấc ngủ → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Giấc ngủ** lấy một mẹo `concernTips(SLEEP)` — *"🌙 Ngủ sớm hơn 30 phút tối nay"* (HIGH, nút **"Đặt nhắc nhở đi ngủ"**), *"🛏️ Làm dịu phòng ngủ"* hoặc *"☕ Ngừng cà phê sau 15:00"*.

### TC-INS-06 — Mẹo xoay vòng theo ngày (không lặp mãi một thẻ)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thói quen **Uống nước** đang thiếu nước (như TC-INS-02).
- **Bước:** 1. Mở tab Phân tích, ghi nhớ mẹo nước đang hiện → 2. Đổi **ngày hệ thống** sang hôm sau (hoặc chờ sang ngày mới) → 3. Mở lại tab Phân tích.
- **Kết quả mong đợi:** Mẹo của danh mục nước **đổi sang mẹo khác** trong cùng nhóm CONCERN — feed luôn **đổi mới** thay vì lặp lại **cùng một** lời nhắc cho tới khi người dùng xử lý xong.
- **Ghi chú:** Mốc xoay vòng là `LocalDate.now().dayOfYear` truyền vào `HabitRuleEngine.generateSuggestions(rotationSeed = …)`; trong **cùng một ngày** feed **ổn định** (không nhảy khi dữ liệu đổi).

### TC-INS-07 — Giới hạn 4 thẻ & sắp xếp theo mức ưu tiên
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có thói quen ở **cả 5** danh mục; nước uống thiếu (nhóm CONCERN/HIGH).
- **Bước:** 1. Mở tab Phân tích → 2. Đếm số thẻ và quan sát thứ tự.
- **Kết quả mong đợi:** Tối đa **4** thẻ; sắp xếp giảm dần theo mức ưu tiên (**HIGH → MEDIUM → LOW**); danh mục đang có vấn đề nằm trên. **Không** còn mức **CRITICAL** trên màn này.

### TC-INS-08 — Vận động đo bằng **bước** dưới ngưỡng → nhóm CONCERN (phụ lục C #17)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thói quen vận động có **đơn vị bước** (`bước`/`bước chân`); 5 log gần nhất đều **< 5000**.
- **Bước:** 1. Ghi `1000`, `2000`, `3000`, `4000`, `4000` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Vận động** lấy mẹo `concernTips(EXERCISE)` — *"🚶 Đi bộ 10 phút sau bữa tối"* (HIGH, nút **"Ghi nhận vận động"**), *"🪜 Tận dụng cầu thang"* hoặc *"🎯 Chia nhỏ mục tiêu vận động"*.
- **Ghi chú:** Ngưỡng 5000/8000 chỉ áp dụng cho log có **đơn vị bước** — thói quen đo bằng đơn vị khác đi theo rule **so với mục tiêu của chính thói quen** (TC-INS-29).

### TC-INS-09 — Vận động đạt mục tiêu → mẹo duy trì (phụ lục C #17)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Log vận động (bước hoặc phút) **đều đạt mục tiêu**.
- **Bước:** 1. Ghi 5 log đạt mục tiêu (ví dụ `6000` bước hoặc `30` phút) → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Vận động** lấy mẹo `generalTips(EXERCISE)` — *"🏃 Giữ đà vận động"* hoặc *"🤸 Thêm vài phút giãn cơ"*, mức **LOW**.

### TC-INS-10 — Nhịp tim nghỉ cao (dữ liệu Health Connect) → mẹo Thiền định
- **Ưu tiên:** P1
- **Tiền điều kiện:** `health_data_history` có **7 ngày** liên tiếp nhịp tim ≥ 90 bpm.
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Thiền định** xuất hiện (`concernTips(MINDFULNESS)`) — **kể cả khi** người dùng **chưa có** thói quen Thiền định — với *"🧘 Thử 5 phút thiền"* (nút **"Bắt đầu thiền"**) hoặc *"🌬️ Hít thở 4-7-8"*.
- **Ghi chú:** Nhánh "chưa có thói quen tương ứng" khi bấm nút ở TC-INS-27.

### TC-INS-11 — Thẻ gợi ý: nút hành động
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đang có mẹo nước nhóm CONCERN kèm nút (TC-INS-02) và có thói quen "Uống nước" đang hoạt động.
- **Bước:** 1. Bấm nút **"Ghi nhận uống nước"**.
- **Kết quả mong đợi:**
  - Ứng dụng chuyển sang tab **Thói quen**, tab đó được đánh dấu trên bottom bar (không mở màn hình mới ngoài luồng tab).
  - **Hộp thoại "Ghi nhận Uống nước" mở sẵn** (ô nhập điền sẵn mục tiêu dạng số sạch), không cần người dùng tự tìm thẻ và bấm "Ghi".
  - Bấm **Lưu** → ghi log bình thường + Toast **"Đã ghi nhận: Uống nước"** (TC-HAB-24); bấm **Hủy**/ra ngoài → chỉ đóng hộp thoại, vẫn ở tab Thói quen.
  - Quay lại tab Phân tích rồi bấm nút lần nữa **vẫn** mở lại được hộp thoại (yêu cầu là sự kiện một lần, không bị "dính").
- **Ghi chú:** Đây là bản sửa lỗi **mục 3 #14** (trước đây `onClick` là TODO, bấm không có gì xảy ra). Chi tiết nhánh "chưa có thói quen tương ứng" ở TC-INS-27.


### TC-INS-12 — Chỉ số sinh lực: hoàn thành đủ mọi thành phần
- **Ưu tiên:** P0
- **Tiền điều kiện:** Có thói quen Ngủ/Nước/Vận động; ghi đủ mục tiêu **mỗi ngày trong 7 ngày**; `health_data_history` có **8 giờ ngủ** và **8000 bước** mỗi ngày.
- **Bước:** 1. Ghi đủ log mỗi ngày → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** **Chỉ số sinh lực = 100**; vòng tròn tô đủ **8/8** cung; hiển thị `100` + `/100`; dòng chú thích cuối thẻ nói rõ điểm tính theo **từng ngày** từ **thói quen + giờ ngủ + bước chân**.
- **Công thức đối chiếu:** mỗi ngày = `0,4 × tỉ lệ thói quen đạt mục tiêu + 0,3 × giờ ngủ/8 + 0,3 × bước chân/8000` (chuẩn hoá theo các thành phần **thật sự có nguồn**), rồi lấy **trung bình 7 ngày**.

### TC-INS-13 — Chỉ số sinh lực **tụt** khi bỏ thói quen (mục 3 #28)
- **Ưu tiên:** P0
- **Tiền điều kiện:** 1 thói quen (mục tiêu 8 giờ); **không** có dữ liệu bước chân/giờ ngủ.
- **Bước:** 1. Ghi đủ **hôm nay** → 2. Mở tab Phân tích (ghi nhớ điểm) → 3. Để trống **3–4 ngày** (hoặc xoá log các ngày trước) → 4. Mở lại tab Phân tích.
- **Kết quả mong đợi:**
  - Sau khi ghi đủ **1/7** ngày → điểm ≈ **14**.
  - Sau khi bỏ hoàn toàn → điểm = **0**.
  - Trước bản sửa #28, điểm lấy trung bình cả tuần với mặc định 50/70 nên **giữ nguyên** dù bỏ thói quen vài ngày.

### TC-INS-14 — Chỉ số sinh lực dùng cả dữ liệu sức khỏe thật (mục 3 #28)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy **không có** thói quen/health nhưng `health_data_history` có **8000 bước** mỗi ngày (hoặc **8 giờ ngủ**).
- **Bước:** 1. Bơm dữ liệu 7 ngày → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Điểm = **100** (thành phần bước chân/giờ ngủ đạt mục tiêu; trọng số **chuẩn hoá lại** nên không bị kéo xuống vì thiếu thành phần thói quen). Bơm **4 giờ ngủ/đêm** thay vì 8 giờ → điểm ≈ **50**.

### TC-INS-15 — Điểm sinh lực cập nhật realtime
- **Ưu tiên:** P1
- **Bước:** 1. Mở tab Phân tích (ghi nhớ điểm hiện tại) → 2. Sang tab Thói quen ghi log đạt mục tiêu → 3. Quay lại tab Phân tích.
- **Kết quả mong đợi:** Điểm tăng tương ứng (Flow reactive, không cần refresh thủ công).

### TC-INS-16 — Biên trên của điểm
- **Ưu tiên:** P2
- **Bước:** 1. Ghi log gấp 5 lần mục tiêu ở mọi danh mục, đủ cả 7 ngày.
- **Kết quả mong đợi:** Điểm **không vượt 100** (`coerceIn(0, 100)`), vòng tròn không vẽ quá 8 cung, không crash.

### TC-INS-17 — Chưa đủ dữ liệu để chấm điểm
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chưa có thói quen nào, chưa có bước chân và chưa có giờ ngủ.
- **Bước:** 1. `pm clear` → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ "Chỉ số sinh lực" hiện **icon + "Chưa đủ dữ liệu để đánh giá"** + lời nhắc tạo thói quen/ghi nhận — **không** hiện `50`/`0` như một điểm số. Điểm luôn nằm trong [0, 100] khi có dữ liệu.

### TC-INS-18 — Gợi ý từ dữ liệu Health Connect (thiếu ngủ 3 ngày) — phụ lục C #16
- **Ưu tiên:** P1
- **Tiền điều kiện:** Emulator có Health Connect; đã cấp đủ 4 quyền; đã bơm `SleepSessionRecord` **< 6 giờ** trong khoảng "18:00 hôm qua → hiện tại".
- **Bước:**
  1. Mở app (đăng ký `periodic_health_sync_worker`).
  2. App Inspection → Background Task Inspector → **Run** `periodic_health_sync_worker` **3 lần**, nhưng **mỗi lần lùi lại 1 ngày** (đổi ngày hệ thống của emulator rồi run) — mỗi lần ghi 1 hàng vào `health_data_history` thuộc **một ngày khác nhau**.
  3. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Giấc ngủ** xuất hiện với mẹo `concernTips(SLEEP)` (*"🌙 Ngủ sớm hơn 30 phút tối nay"*, *"🛏️ Làm dịu phòng ngủ"*, *"☕ Ngừng cà phê sau 15:00"*).
- **Ghi chú:** `HabitRuleEngine` **gom theo ngày** (mỗi ngày lấy hàng mới nhất) rồi mới xét: cần **3 ngày gần nhất có dữ liệu và liên tiếp nhau**, mỗi ngày `sleepHours < 6`. Vì vậy **3 lần sync trong cùng một ngày KHÔNG** đánh dấu Giấc ngủ (test `HabitRuleEnginePersonalizedTest`, TC-INS-30).

### TC-INS-19 — Gợi ý nhịp tim cao → thẻ Thiền định — phụ lục C #16
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã bơm `HeartRateRecord` **~90 bpm**; đã cấp quyền.
- **Bước:** 1. Run `periodic_health_sync_worker` **7 lần ở 7 ngày khác nhau** (lùi ngày hệ thống như TC-INS-18) → 2. Mở tab Phân tích.
- **Kết quả mong đợi:** Thẻ **Thiền định** xuất hiện với *"🧘 Thử 5 phút thiền"* (nút **"Bắt đầu thiền"**) hoặc *"🌬️ Hít thở 4-7-8"*.
- **Ghi chú:** Nếu dữ liệu < 85 bpm → **không** có thẻ này. Con số trung bình đúng là "7 **ngày** gần nhất" (trước bản sửa #16 là 7 **lần sync** ≈ 1,75 ngày).

### TC-INS-20 — Kết hợp gợi ý log thủ công + dữ liệu Health Connect
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có log nước thấp (TC-INS-02) + dữ liệu ngủ < 6h trong 3 ngày liền (TC-INS-18).
- **Bước:** 1. Mở tab Phân tích.
- **Kết quả mong đợi:** Hiển thị thẻ thuộc **cả** hai danh mục (Nước và Giấc ngủ).

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

### TC-INS-23 — Thẻ giấc ngủ luôn có dữ liệu nhờ nguồn tự đo (mục 3 #29)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy **không có** Health Connect (hoặc thiếu quyền Giấc ngủ); đã đặt **"Khung giờ ngủ"** ở Trang chủ và có ít nhất vài đêm được app ước lượng (ghi vào `health_data_history`).
- **Bước:** 1. Mở tab Phân tích → 2. Xem thẻ "Xu hướng chất lượng giấc ngủ".
- **Kết quả mong đợi:**
  - Thẻ hiện **biểu đồ cột 7 đêm** (giờ ngủ), nhãn trên cột dạng **"7h30"** hoặc **`—`** khi đêm chưa có số; cột hôm nay tô xanh đậm + in đậm.
  - Dòng **"Trung bình Xh Ym/đêm"** + **chip xếp loại**: **"Tốt"** (xanh, 7–9h), **"Chưa đủ"** (cam, 6–7h), **"Thiếu ngủ"** (đỏ, < 6h) hoặc **"Hơi dài"** (xám, > 9h).
  - Dòng chú thích **"Giờ ngủ x/7 đêm gần nhất (gồm cả số app tự đo bằng cảm biến)."**
  - **Không** còn phụ thuộc dữ liệu pha Sâu/Nông/REM để hiển thị số liệu.

### TC-INS-24 — Thẻ giấc ngủ kèm chi tiết pha khi Health Connect có dữ liệu
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Giấc ngủ; bơm `SleepSessionRecord` trong **7 ngày gần nhất** với các pha, ví dụ Deep 2h15m, Light 4h30m, REM 1h10m.
- **Bước:** 1. Mở tab Phân tích (mở lại màn hình nếu đang mở) → 2. Xem thẻ "Xu hướng chất lượng giấc ngủ".
- **Kết quả mong đợi:**
  - Vẫn có **biểu đồ 7 đêm + xếp loại** (như TC-INS-23), **thêm** khối **"Chi tiết pha (Health Connect)"**.
  - 3 thanh **Sâu / Nông / REM**, thanh tiến độ = tỉ lệ phút pha đó trên tổng, màu lần lượt `#1B5E20`, `#2E7D32`, `#81C784`; số phút hiển thị dạng **"2h 15m"**.
  - Dòng cuối: **"Tổng 7h 55m trong 7 đêm gần nhất"**.
- **Ghi chú:** Khối pha đọc **trực tiếp từ Health Connect** mỗi lần mở màn hình; biểu đồ 7 đêm lấy từ `health_data_history` (Flow của Room nên tự cập nhật). Thiết bị chỉ báo `STAGE_TYPE_SLEEPING` (không chia pha) → chỉ hiện biểu đồ 7 đêm.

### TC-INS-25 — Delta điểm sinh lực so với tuần trước
- **Ưu tiên:** P1
- **Tiền điều kiện:** Chỉ có log/dữ liệu trong 7 ngày gần nhất (không có gì ở 7 ngày trước đó).
- **Bước:** 1. Mở tab Phân tích → 2. Xem thẻ "Chỉ số sinh lực".
- **Kết quả mong đợi:** Chỉ hiển thị điểm + `/100`, **không** có dòng so sánh.
- **Ghi chú:** `GetVitalityScoreUseCase` đặt `delta = null` khi `VitalityScoreMath.hasObservations(window tuần trước) == false` (không có log thói quen và không có hàng bước chân/giờ ngủ nào).

### TC-INS-26 — Delta tăng / giảm / không đổi
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có log/dữ liệu ở **cả** tuần này và 7 ngày trước đó (ví dụ: tuần này ghi đủ cả 7 ngày; tuần trước chỉ 1 ngày).
- **Bước:** 1. Mở tab Phân tích → 2. Xem dòng dưới điểm sinh lực.
- **Kết quả mong đợi:**
  - Tuần này tốt hơn tuần trước → **"+X so với tuần trước"** màu xanh `#2E7D32` (X = điểm tuần này − điểm tuần trước).
  - Kém hơn → **"-X so với tuần trước"** màu đỏ `#C62828`.
  - Bằng nhau → **"Không đổi so với tuần trước"** màu xám.
- **Đối chiếu công thức:** cùng công thức ở TC-INS-12, tính cho **cửa sổ 7 ngày liền trước**.

### TC-INS-27 — Nút hành động khi chưa có thói quen tương ứng (mục 3 #14)
- **Ưu tiên:** P2
- **Tiền điều kiện:** Có mẹo **Thiền định** (nhịp tim cao — TC-INS-10) nhưng **chưa** có thói quen nào thuộc danh mục Thiền định.
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

### TC-INS-29 — Vận động đo bằng đơn vị khác "bước" → xét theo **mục tiêu của thói quen** (phụ lục C #17)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Thói quen **"Chạy bộ"** đang hoạt động, đơn vị **phút**, mục tiêu **30** phút (Data set A).
- **Bước:**
  1. Ghi **5 log `10`** phút cho "Chạy bộ" → 2. Mở tab Phân tích.
  3. Xoá log, ghi **5 log `30`** phút → 4. Mở tab Phân tích.
  5. Xoá log, ghi **2 log `1`** phút → 6. Mở tab Phân tích.
- **Kết quả mong đợi:**
  - 5 log `10` phút (33% mục tiêu, < 80%) → thẻ **Vận động** lấy mẹo **CONCERN** (`concernTips(EXERCISE)` — ví dụ *"🚶 Đi bộ 10 phút sau bữa tối"* hoặc *"🎯 Chia nhỏ mục tiêu vận động"*). **Không** còn cảnh báo kiểu *"Số bước chân trung bình của bạn là 10 bước…"*.
  - 5 log `30` phút (100%) → thẻ **Vận động** lấy mẹo **GENERAL** (duy trì) thay vì cảnh báo.
  - Chỉ **2 log** (dù giá trị rất thấp) → **không** bị coi là xu hướng → mẹo **GENERAL**.
- **Ghi chú:** Rule chia theo **đơn vị của chính thói quen**: log có đơn vị bước giữ ngưỡng 5000/8000 bước (TC-INS-08/09), log đơn vị khác (phút, km, calo…) so **tỉ lệ `value / targetValueAtTime`** (dưới 80% → mẹo "cần chú ý"), cần tối thiểu **3** lần ghi.

### TC-INS-30 — Unit test JVM cho feed gợi ý sau khi gom theo NGÀY và theo ĐƠN VỊ (phụ lục C #16, #17)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitRuleEngine*" --tests "*SuggestionCatalog*" --tests "*HealthDataRepositoryImplTest"`.
- **Kết quả mong đợi:** Toàn bộ pass, trong đó:
  - `HabitRuleEngineTest` (**14 test**): feed chỉ gồm danh mục người dùng có; danh mục thiếu nước → mẹo **CONCERN**, đủ nước → **GENERAL**; **không** còn thẻ `CRITICAL`; xoay `rotationSeed` → mẹo đổi, cùng seed → ổn định; tối đa **4** thẻ; không lặp mẹo; sắp xếp ưu tiên giảm dần; **id các thẻ duy nhất** (không trùng key của `LazyColumn`); 3 ngày liền ít bước → thẻ Vận động **CONCERN**; 7 ngày nhịp tim ≥ 90 → thẻ Thiền định.
  - `HabitRuleEnginePersonalizedTest` (**6 test**): **3 hàng cùng một ngày** → **không** đánh dấu Giấc ngủ (chống hồi quy #16); 3 ngày **cách quãng** → không; 3 ngày **liên tiếp** < 6h → Giấc ngủ **CONCERN**; 7 **ngày** nhịp tim ≥ 90 → Thiền định **CONCERN**; hàng thiếu dữ liệu không đánh dấu sai.
  - `HabitRuleEngineStepTrendTest` (**6 test**): 3 ngày liền ít bước → Vận động **CONCERN**; trên ngưỡng / cách quãng / thiếu dữ liệu / < 3 ngày → không; mẹo CONCERN có mức ưu tiên ≥ MEDIUM.
  - `HabitRuleEngineExtendedTest` (**4 test**): đơn vị **phút** dưới mục tiêu → **CONCERN**; đạt mục tiêu → **GENERAL**; < 3 log → không kết luận; **không** có thẻ `CRITICAL`.
  - `SuggestionCatalogTest` (**6 test**): mỗi danh mục có cả mẹo CONCERN và GENERAL; tiêu đề/nội dung không rỗng; mẹo CONCERN có mức ưu tiên ≥ MEDIUM; tiêu đề không trùng; mỗi danh mục có ≥ 1 mẹo kèm nút hành động; kho phủ đủ **5** danh mục.
  - `HealthDataRepositoryImplTest`: `getRecentHealthHistoryFlow(7)` gọi DAO với đúng `limit = 7` (mockk `verify`) — phụ lục C #16.
- **Ghi chú:** Dữ liệu test dựng theo **ngày** (mốc 12:00 giờ thiết bị) thay vì "N hàng liên tiếp", đúng với cách `health_data_history` ghi 1 hàng mỗi lần sync.

### TC-INS-31 — Thẻ "Lịch sử sức khỏe 7 ngày": biểu đồ cột + bảng 7 dòng (mục 3 #26)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `health_data_history` có dữ liệu của **vài ngày gần đây** (chạy `HealthSyncWorker` vài lần qua "mục 2.4 — cách chạy tay worker", mỗi ngày bơm bước chân + giấc ngủ khác nhau; bơm `HeartRateRecord` cho vài ngày — mục 2.5 bước 3). Máy có Health Connect **hoặc** cảm biến đếm bước (đều ghi được vào lịch sử).
- **Bước:**
  1. Mở tab **Phân tích** → kéo xuống **ngay dưới thẻ "Chỉ số sinh lực"** → xem thẻ **"Lịch sử sức khỏe 7 ngày"**.
  2. So từng con số trên thẻ với bảng `health_data_history` (mục 2.3 — `adb shell` + SQLite, hoặc `Database Inspector`).
- **Kết quả mong đợi:**
  - Thẻ có tiêu đề **"Lịch sử sức khỏe 7 ngày"** + icon trái tim có màn hình; bên dưới là **biểu đồ cột 7 cột** — mỗi cột là **số bước của một ngày**, cột cao nhất = ngày đi nhiều nhất (mốc 100% là **max của tuần**, không phải 10 000 bước), trên mỗi cột là số bước rút gọn (`6,2k`) hoặc **`—`** khi ngày đó chưa có số, dưới cột là nhãn thứ (**T2…CN**), ngày **hôm nay** được tô **xanh đậm + in đậm**.
  - Dưới biểu đồ là **bảng 7 dòng** (cột **Ngày · Bước · Ngủ · Tim**), **cũ → mới**, dòng **hôm nay** cũng được tô xanh đậm; cột Ngày là **thứ + ngày/tháng** (ví dụ **"T3 22/09"**).
  - Số trong bảng **khớp đúng** hàng mới nhất của từng ngày trong `health_data_history`: **bước chân** (dạng rút gọn), **giờ ngủ** (`Xh Ym`), **nhịp tim** (số nguyên, ví dụ `74`). Ngày **chưa có hàng nào** hiện **"Chưa có dữ liệu"** trên cả dòng; ô **thiếu riêng một chỉ số** (ví dụ máy chỉ đo được bước chân) hiện **`—`** — **không** hiện số `0` (0 sẽ bị hiểu là "ngày đó đi bộ 0 bước / ngủ 0 giờ").
  - Luôn có **đúng 7 dòng** kể cả khi vài ngày không có dữ liệu; dữ liệu **cũ hơn 7 ngày không** xuất hiện.
  - Ghi dữ liệu mới (đo nhịp tim ở màn "Đo nhịp tim", hoặc bấm chạy tay `HealthSyncWorker`) → thẻ **tự cập nhật ngay** khi quay lại tab Phân tích (Flow của Room), không cần mở lại app.
  - Dòng ghi chú ngắn cuối thẻ: *"Dữ liệu app đã lưu theo từng ngày — cũng là nguồn AI dùng để phân tích."*
- **Ghi chú:** `GetRecentHealthSummaryUseCase` → `HealthDataRepository.getHealthHistorySince(fromTs)` → `HealthDataDao.getHealthDataSince` (cắt theo **mốc thời gian** = 00:00 của `hôm nay − 6 ngày`) → `HealthSummaryHistory.summarize()` (mỗi ngày lấy **hàng mới nhất**). Đây **cùng dữ liệu** Rule Engine dùng để sinh gợi ý ở đầu màn, nên con số trên thẻ và gợi ý của AI luôn khớp.

### TC-INS-32 — Thẻ "Lịch sử sức khỏe 7 ngày" khi chưa có dữ liệu nào (mục 3 #26)
- **Ưu tiên:** P2
- **Tiền điều kiện:** App **chưa từng ghi** hàng nào vào `health_data_history` (vừa `pm clear`, máy không có Health Connect **và** chưa cấp quyền `ACTIVITY_RECOGNITION`).
- **Bước:** 1. Mở tab **Phân tích** → cuộn tới thẻ **"Lịch sử sức khỏe 7 ngày"**.
- **Kết quả mong đợi:** Thẻ hiện tiêu đề **"Lịch sử sức khỏe 7 ngày"** + dòng **"Chưa có dữ liệu sức khỏe nào được ghi trong 7 ngày gần nhất."** — **không** vẽ biểu đồ, **không** vẽ bảng toàn số `0`, app không crash. Màn Phân tích vẫn hiện các thẻ còn lại bình thường.
- **Ghi chú:** Điều kiện rỗng là `days.none { it.hasData }`; ngày có hàng nhưng **tất cả** chỉ số đều 0 cũng không tồn tại vì `syncHealthData()` **không ghi** hàng toàn số 0.

### TC-INS-33 — Unit test JVM cho gom lịch sử sức khỏe theo ngày (mục 3 #26)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HealthSummaryHistoryTest"`.
- **Kết quả mong đợi:** **8 test** chạy qua:
  - cửa sổ luôn đủ **7 ngày**, thứ tự **cũ → mới** (ngày đầu = `hôm nay − 6`);
  - ngày **không có hàng** → `hasData = false` và mọi chỉ số = 0 (**không** phải "0 bước");
  - hàng nằm **đúng ngày ghi** và các ngày khác vẫn trống;
  - **hàng mới nhất trong ngày thắng** (chuỗi ghi thật: đồng bộ sáng → đồng bộ tối → đo nhịp tim ⇒ lấy đúng hàng đã gộp);
  - dữ liệu **ngoài cửa sổ** (7 ngày, 30 ngày trước) không lọt vào kết quả;
  - gom theo **múi giờ thiết bị** (hàng lúc **23:30** giờ VN vẫn thuộc **đúng ngày hôm đó**, không nhảy sang ngày kế);
  - cửa sổ **ngắn hơn tuần** (`days = 3`) vẫn tôn trọng;
  - ngày chỉ có **một** chỉ số (ví dụ chỉ có nhịp tim do camera đo) vẫn tính là **có dữ liệu**.
- **Ghi chú:** `HealthSummaryHistory` là **hàm thuần Kotlin** (nhận `history`, `days`, `today`, `zone`) nên test chạy trên JVM với ngày/múi giờ **cố định**, không phụ thuộc giờ máy.

### TC-INS-34 — Unit test JVM cho kho mẹo gợi ý (mục 3 #27)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*SuggestionCatalogTest"`.
- **Kết quả mong đợi:** **6 test** chạy qua:
  - mỗi danh mục có **cả** mẹo nhóm **CONCERN** và **GENERAL** (điều kiện để feed luôn đa dạng);
  - mọi mẹo có tiêu đề/nội dung **không rỗng** và đúng danh mục;
  - mẹo CONCERN có mức ưu tiên **≥ MEDIUM** (nổi hơn mẹo duy trì);
  - tiêu đề **không trùng** trong toàn kho (mẹo xoay vòng không sinh thẻ trùng);
  - mỗi danh mục có **≥ 1** mẹo kèm nút hành động;
  - kho phủ đủ **5** danh mục của `HabitCategory`.
- **Ghi chú:** `SuggestionCatalog` là **object thuần Kotlin** nên test chạy trên JVM.

### TC-INS-35 — Unit test JVM cho chỉ số sinh lực (mục 3 #28)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*VitalityScoreMathTest" --tests "*GetVitalityScoreUseCaseTest"`.
- **Kết quả mong đợi:** Toàn bộ pass, trong đó:
  - `VitalityScoreMathTest` (**10 test**): không có nguồn nào → `null`; hoàn thành đủ mọi ngày → **100**; **bỏ 3/7 ngày → 43** (điểm **tụt** — chống hồi quy #28); mỗi ngày đạt 1/2 thói quen → **50**; chỉ có thành phần thói quen → **chuẩn hoá lại** trọng số (không bị kéo xuống); chỉ có **bước chân/giờ ngủ** đủ mục tiêu → **100**; ngủ 4/8h + bước 4000/8000 → **50**; **ngày thiếu dữ liệu đóng góp 0** cho thành phần đó; `hasObservations` đúng cho cửa sổ có/không có quan sát.
  - `GetVitalityScoreUseCaseTest` (**7 test**): không có dữ liệu → `hasData = false`, `score = 0`, `delta = null`; tuần hoàn hảo → **100**; **bỏ ngày → điểm tụt**; `delta = null` khi tuần trước **không có quan sát**; `delta` đúng khi tuần trước có quan sát; chỉ có dữ liệu sức khỏe vẫn chấm được; thói quen + dữ liệu sức khỏe ghép lại.
- **Ghi chú:** `VitalityScoreMath` nhận `today`/`zone` nên test chạy trên JVM với ngày **cố định**.

### TC-INS-36 — Unit test JVM cho xu hướng giấc ngủ (mục 3 #29)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*SleepTrendMathTest"`.
- **Kết quả mong đợi:** **4 test** chạy qua:
  - dựng đúng **1 đêm/ngày** theo thứ tự **cũ → mới**;
  - **trung bình bỏ qua** đêm chưa có số liệu;
  - cửa sổ **không có giờ ngủ nào** → `hasData = false`;
  - xếp loại đúng biên: `< 6h` → **Thiếu ngủ**, `6–7h` → **Chưa đủ**, `7–9h` → **Tốt**, `> 9h` → **Hơi dài**.
- **Ghi chú:** `SleepTrendMath` là **hàm thuần Kotlin** nên test chạy trên JVM.

### TC-INS-40 — Feed "BLOOM AI GỢI Ý" là **banner trượt tự động** (mục 3 #41)
- **Ưu tiên:** P1
- **Tiền điều kiện:** đã có **≥ 2 gợi ý** cho người dùng (tạo 2 thói quen ở 2 danh mục khác nhau, ví dụ "Uống nước" + "Ngủ đủ giấc").
- **Bước:**
  1. Mở tab **Phân tích**, quan sát khối **"BLOOM AI GỢI Ý"** ngay dưới tiêu đề.
  2. **Để yên** ~10 giây (không chạm màn hình).
  3. **Vuốt ngang** trong banner vài lần.
  4. Bấm nút hành động (ví dụ **"Ghi nhận uống nước"**) ở gợi ý **đang hiện**.
- **Kết quả mong đợi:**
  - Banner chiếm **một khối cao cố định** (~240dp) và chỉ hiện **MỘT** gợi ý mỗi lúc — màn Phân tích **không còn dài** như trước (4 thẻ gợi ý xếp dọc).
  - Sau ~5 giây banner **tự trượt** sang gợi ý kế tiếp; đi hết danh sách thì **quay lại gợi ý đầu** nhưng **không thấy cảnh quay ngược** về đầu (banner luôn trượt về phía trước).
  - **Chấm chỉ báo** dưới banner: chấm của gợi ý đang hiện **dài + xanh**, các chấm khác **xám**; số chấm = số gợi ý (tối đa 4).
  - **Vuốt tay** được: banner trượt theo ngón tay và **tạm dừng tự cuộn** trong lúc vuốt rồi tự chạy tiếp.
  - **Nút hành động** trên gợi ý đang hiện bấm được → app nhảy sang tab Thói quen mở sẵn hộp thoại ghi nhận (như **TC-INS-11**).
  - Chiều cao banner **không đổi** khi trượt giữa gợi ý **có** nút hành động và gợi ý **không** có nút (không nhảy layout); chữ dài bị **cắt bằng "…"** chứ không tràn ra ngoài thẻ.
- **Ghi chú:** danh sách mẹo vẫn do `HabitRuleEngine` sinh **xoay vòng theo ngày** nên **trong cùng một ngày** bộ gợi ý **ổn định** (mở lại màn không đổi), sang ngày mới thì **đổi mẹo** và **không trùng nhau** — xem **TC-INS-41** cho phần kiểm chứng tự động.

### TC-INS-41 — Unit test JVM: xoay vòng theo ngày phủ hết kho mẹo, không lặp (mục 3 #41)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HabitRuleEngineTest"`.
- **Kết quả mong đợi:** **16 test** chạy qua (trước là 14), trong đó **2 test mới**:
  - `rotating the seed walks the whole tip pool before repeating`: với một danh mục **ổn** (không có log ⇒ dùng kho mẹo `GENERAL`), `poolSize` ngày liên tiếp ra **`poolSize` mẹo khác nhau**, và ngày thứ `poolSize` **quay lại đúng mẹo đầu** ⇒ **không trùng lặp trước khi đi hết kho**.
  - `every tip of a category is reachable within one rotation cycle`: với **cả 5 danh mục**, tập mẹo `GENERAL` hiện ra trong `poolSize` ngày **bằng đúng kho mẹo** của danh mục đó ⇒ **không mẹo nào bị "bỏ quên"** vì xoay vòng.
- **Ghi chú:** `HabitRuleEngine` + `SuggestionCatalog` là **thuần Kotlin**, test truyền `rotationSeed` cố định nên **tất định**, không phụ thuộc ngày chạy máy.

### TC-INS-42 — Thẻ **"AI & quyền riêng tư"** trên tab Phân tích AI mở màn Chính sách bảo mật (mục 3 #43)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Bất kỳ (màn Phân tích **không** nằm sau bất kỳ cổng nào).
- **Bước:**
  1. Mở tab **Phân tích** → **cuộn xuống cuối màn**.
  2. Ở thẻ **"AI & quyền riêng tư"**, đọc nội dung rồi bấm **"Xem chính sách bảo mật"**.
  3. Bấm **Quay lại** (hoặc Back hệ thống).
- **Kết quả mong đợi:**
  - Thẻ có icon **ổ khoá**, tiêu đề **"AI & quyền riêng tư"**, đoạn mô tả *"Ảnh món ăn bạn chụp/chọn ở màn Ghi chép bữa ăn được gửi tới Google Gemini để nhận diện món và phân tích lợi ích, điều nên lưu ý cho cơ thể. Mọi phân tích khác trong app (gợi ý BLOOM AI, chỉ số sinh lực) chạy ngay trên máy bằng luật thuần Kotlin."* và nút **"Xem chính sách bảo mật"**.
  - Thẻ **luôn hiện**, kể cả khi tab đang **loading** hoặc có **thẻ lỗi đỏ** (nó nằm ngoài nhánh `isLoading`).
  - Bấm nút → mở màn **"Chính sách bảo mật"**; **Quay lại** → về đúng **tab Phân tích**, không crash.
  - Màn **Ghi chép** (tab "Ghi chép") **không** còn nút "Xem chính sách bảo mật" nào (nút này đã chuyển về đây).
- **Ghi chú:** Đây là **lối vào chính sách cho phần AI** sau khi bỏ công tắc ở màn Ghi chép — tab "Phân tích AI" là chỗ nói về AI nên đặt ở đó dễ tìm hơn. Thẻ không có trạng thái nào khác nên **không** cần test JVM (đây là UI tĩnh); kiểm tra nhanh bằng `@Preview InsightsScreenPreview` trong `InsightsScreen.kt` và **TC-PRIV-01**, **TC-PRIV-04**.

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
  - Hộp thoại **chỉ** hỏi **quyền ĐỌC**: manifest không còn `WRITE_STEPS`/`WRITE_SLEEP` (mục 3 #40, phụ lục C #44) — kiểm tra nhanh bằng `adb shell dumpsys package com.healthyai.app | findstr health.` (không được thấy `WRITE_*`).
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

### TC-DB-17 — Migration **3 → 4**: thêm bảng `app_settings`, giữ nguyên mọi bảng cũ
- **Ưu tiên:** P0
- **Tiền điều kiện:** Bản đang cài là **schema v3** (chưa có bảng `app_settings`) và đã có ít nhất 1 thói quen + vài log + 1 hàng `health_data_history`.
- **Bước:**
  1. Ghi lại số hàng của `habits`, `habit_logs`, `health_data_history` bằng **Database Inspector** (hoặc `adb shell` + `sqlite3` trên bản sao DB).
  2. Cài **đè** APK mới bằng `adb install -r` (**không** `pm clear`) → mở app → vào Trang chủ (thẻ "Khung giờ ngủ" phải hiện mặc định **23:00 – 07:00**).
  3. Đặt khung **22:00 → 06:30** → **Lưu** → kiểm tra bảng `app_settings`.
  4. Kiểm tra lại số hàng của 3 bảng ở bước 1 + tab Thói quen/nhật ký + tab Phân tích.
- **Kết quả mong đợi:**
  - KHÔNG có `IllegalStateException: A migration from 3 to 4 was required but not found` (nếu thiếu `MIGRATION_3_4`, mọi máy đã cài bản cũ sẽ crash ngay khi mở app).
  - `app_settings` được tạo mới với **2 cột** `settingKey` / `settingValue` và **khoá chính là `settingKey`**; sau bước 3 có đúng 2 hàng (`sleep_bedtime_minutes = 1320`, `sleep_wake_minutes = 390`).
  - `habits` / `habit_logs` / `health_data_history` **không đổi** (đúng số hàng, đúng dữ liệu) — migration chỉ **CREATE TABLE**, không DROP/ALTER bảng nào.
- **Ghi chú:** DDL trong `DatabaseMigrations.STATEMENTS_3_4` phải **khớp từng ký tự** với `createSql` của `app_settings` trong `app/schemas/com.healthyai.app.local.AppDatabase/4.json`; nếu lệch, Room báo `Migration didn't properly handle: app_settings`. Đã kiểm chứng **offline, không cần emulator**: dựng DB v3 bằng `sqlite3` từ `3.json` → chạy đúng câu lệnh của `MIGRATION_3_4` → so `PRAGMA table_info` / `index_list` / `foreign_key_list` với bảng do Room tạo trực tiếp từ `4.json` → **khớp hoàn toàn** (đã bỏ qua `sqlite_autoindex_*` mà SQLite tự tạo cho khoá chính TEXT, Room cũng bỏ qua khi so schema), và 3 bảng cũ **không bị đổi**. Unit test cấu trúc dãy migration: `DatabaseMigrationsTest` (4 test).

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

## 15. TC-WG — Widget màn hình chính "Sức khỏe hôm nay" (Glance)

### TC-WG-01 — Thêm widget "Sức khỏe hôm nay" ra màn hình chính
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy có ít nhất một nguồn số: đã cấp quyền đếm bước (`ACTIVITY_RECOGNITION`), hoặc đã cài/cấp quyền Health Connect, hoặc đã đo nhịp tim trong app.
- **Bước:** 1. Nhấn giữ màn hình chính → **Widgets** → tìm **"Sức khỏe hôm nay"** → 2. Kéo ra màn hình chính → 3. Mở app và đối chiếu với thẻ **"Tóm tắt sức khỏe"** ở màn Trang chủ.
- **Kết quả mong đợi:** Dòng trên cùng là **"Healthy AI"** (đậm, xanh lá) + **"Hôm nay"** (nhạt), có một đường kẻ mảnh phân cách; bên dưới là **3 cột** chia đều:
  1. icon **bước chân** (xanh lá) — số bước **đầy đủ, có dấu phân cách nghìn** (ví dụ `6.250`) — nhãn **"Bước chân"**;
  2. icon **giấc ngủ** (xanh ngọc) — `7h 30m` — nhãn **"Giấc ngủ"**;
  3. icon **nhịp tim** (đỏ) — `72 bpm` — nhãn **"Nhịp tim"**.
  Ba số **khớp** ba ô của thẻ "Tóm tắt sức khỏe" trong app (nguồn số xem **TC-WG-03**).

### TC-WG-02 — Chưa có số nào thì hiện `--`, không hiện `0`
- **Ưu tiên:** P1
- **Tiền điều kiện:** `adb shell pm clear com.healthyai.app` (chưa cấp quyền nào, chưa đo nhịp tim, không có Health Connect) hoặc emulator không có cảm biến đếm bước.
- **Bước:** 1. Thêm widget ra màn hình chính.
- **Kết quả mong đợi:** Widget **không trắng, không crash**, không giữ mãi chữ "Loading": bước chân **`0`**, giấc ngủ **`--`**, nhịp tim **`-- bpm`**.
- **Ghi chú:** `--` / `-- bpm` là **chủ ý**: số `0` sẽ bị đọc thành "ngủ 0 giờ" / "nhịp tim bằng 0", khác hẳn ý nghĩa "hôm nay chưa có số nào".

### TC-WG-03 — Nguồn số: bước chân theo cảm biến, giấc ngủ / nhịp tim theo Health Connect
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy **có** cảm biến đếm bước và app **đã** được cấp `ACTIVITY_RECOGNITION`; Health Connect **có** số (ví dụ đồng hồ đeo tay đã đồng bộ).
- **Bước:** 1. Xem widget → 2. So từng số với nguồn tương ứng (mở app Health Connect, hoặc xem thẻ **"Lịch sử sức khỏe 7 ngày"** ở tab Phân tích cho hôm nay).
- **Kết quả mong đợi:**
  - **Bước chân** lấy từ **cảm biến của chính điện thoại** nên có thể **nhỏ hơn** số của đồng hồ đeo tay trong Health Connect — chiều ưu tiên này là **chủ ý** và **giống hệt** thẻ "Tóm tắt sức khỏe" ở màn Trang chủ (mục 3 #24), không phải lỗi.
  - **Giấc ngủ** và **nhịp tim** ưu tiên **Health Connect**; Health Connect trống thì giấc ngủ lấy **ước lượng từ cảm biến của app**, nhịp tim lấy **lần đo bằng camera** gần nhất trong ngày.
- **Ghi chú:** Quy tắc nằm ở `TodayHealthSummary.build()` — dùng chung cho widget và màn Trang chủ, nên hai nơi không thể lệch nhau.

### TC-WG-04 — App ghi dữ liệu sức khỏe → widget tự vẽ lại
- **Ưu tiên:** P1
- **Bước:** 1. Ghi nhận số đang hiện trên widget → 2. Vào ô **"Tim"** ở Trang chủ → **"Đo ngay"** và đo một lần → 3. Quay về màn hình chính, xem widget (**không** gỡ/thêm lại widget).
- **Kết quả mong đợi:** Ô **"Nhịp tim"** trên widget đổi sang số vừa đo **ngay** ở bước 3; số bước và giờ ngủ cập nhật sau mỗi lần app đồng bộ sức khỏe (`HealthSyncWorker`) — không cần gỡ/thêm lại widget hay khởi động lại launcher.
- **Cơ chế:** `HealthDataRepositoryImpl.syncHealthData()` / `saveMeasuredHeartRate()` → `HealthWidgetRefresher.refresh()` → `HealthWidget().updateAll(context)` → `provideGlance()` đọc lại dữ liệu và dựng lại nội dung. Lỗi khi vẽ widget bị nuốt trong `HealthWidgetRefresherImpl` (log `w` tag `HealthWidgetRefresher`) nên **không** làm hỏng luồng ghi dữ liệu.
- **Ghi chú:** `health_widget_info.xml` đặt `updatePeriodMillis="1800000"` (30 phút — mức nhỏ nhất Android chấp nhận) làm **lưới an toàn** cho lúc app không chạy; lần vẽ theo sự kiện ở trên mới là đường cập nhật chính.

### TC-WG-05 — Bấm widget mở app
- **Ưu tiên:** P1
- **Bước:** 1. Bấm vào widget.
- **Kết quả mong đợi:** Mở `MainActivity` (màn Trang chủ); nếu app đã ở background thì đưa lên foreground, không tạo instance mới trùng lặp.

### TC-WG-06 — Số bước trên widget chỉ tính phần app đang chạy
- **Ưu tiên:** P2
- **Bước:** 1. Đi bộ ~50 bước **trong lúc app đang mở** → chờ widget vẽ lại (hoặc bấm widget rồi về màn hình chính) và xem số bước → 2. **Đóng hẳn app** (vuốt khỏi tác vụ gần đây) → đi bộ ~50 bước nữa → 3. Mở lại app và xem widget.
- **Kết quả mong đợi:** Số bước **không** nhảy thêm phần đã đi trong lúc app đã đóng — đây là luật đếm **đã chốt** ở mục 3 #44 (`StepCounterMath`, `StepCounterStore`), **không phải** lỗi của widget.
- **Ghi chú:** Widget đọc **giá trị đã lưu** (`StepCounterRepository.readTodaySteps(NO_WAIT_MS)`) — không chờ mẫu cảm biến mới khi vẽ. Chờ một mẫu có thể mất tới 2 giây và làm launcher hiện khung trắng, nên `NO_WAIT_MS` là chủ ý.

### TC-WG-07 — Kích thước & resize widget
- **Ưu tiên:** P2
- **Bước:** 1. Thêm widget ra màn hình chính → xác nhận khung mặc định đã đủ nội dung → 2. Nhấn giữ widget → resize rộng nhất rồi hẹp nhất.
- **Kết quả mong đợi:** Khung mặc định khai báo `minWidth="250dp"` / `minHeight="130dp"`, cho phép resize ngang & dọc (`resizeMode="horizontal|vertical"`). Kéo **rộng** ra thì ba cột nở ra theo và số không bị cắt; **không** kéo được hẹp hơn 250dp (`minWidth` chặn) nên ba cột luôn đủ chỗ.
- **Ghi chú:** Widget dùng `sizeMode = SizeMode.Exact` nên Glance **vẽ lại theo đúng kích thước hiện tại** thay vì vẽ ở khung mặc định rồi để launcher phóng to (chữ bị mờ nét). Bo góc 24dp do chính widget vẽ, không phụ thuộc `system_app_widget_background_radius` (chỉ có từ Android 12).

### TC-WG-08 — Widget sau khi reboot
- **Ưu tiên:** P2
- **Bước:** 1. `adb reboot`.
- **Kết quả mong đợi:** Widget vẫn hiển thị (không bị mất/trắng), số liệu được đọc lại; đèn flash/cảm biến bình thường.

### TC-WG-09 — Unit test JVM: ghép ba chỉ số theo đúng thứ tự ưu tiên nguồn
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*TodayHealthSummaryTest"` (hoặc `testDebugUnitTest`).
- **Kết quả mong đợi:** **7 test pass**, chốt lại hợp đồng chọn nguồn số:
  - bước chân lấy **cảm biến của máy** khi cảm biến đã sẵn sàng — **kể cả** khi Health Connect có số lớn hơn (`1 200` thắng `8 000`);
  - bước chân chỉ rơi về Health Connect khi cảm biến **chưa** sẵn sàng; thiếu cả hai nguồn ⇒ `0`;
  - giấc ngủ / nhịp tim ưu tiên Health Connect, chỉ rơi về nguồn tự đo khi Health Connect **không có số**;
  - thiếu hết nguồn ⇒ cả ba chỉ số bằng `0` (widget hiện `--` / `-- bpm` theo TC-WG-02).
- **Ghi chú:** `TodayHealthSummary.build()` là hàm **thuần** nên test được trên JVM; phần vẽ Glance của `HealthWidget.provideGlance()` chỉ kiểm chứng được trên thiết bị/emulator (TC-WG-01…08, TC-WG-12).

### TC-WG-10 — Unit test JVM: repository vẽ lại widget khi dữ liệu sức khỏe đổi
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*HealthDataRepositoryImplTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, gồm **4 test mới** chốt hợp đồng "ghi dữ liệu sức khỏe → vẽ lại widget": `a successful sync refreshes the home screen widget`, `a sync without any data does not refresh the home screen widget`, `a saved measurement refreshes the home screen widget`, `an invalid measurement does not refresh the home screen widget`.
- **Ghi chú:** Test dùng mockk cho `HealthWidgetRefresher` nên chỉ khẳng định repository **có gọi** `refresh()`; không kiểm chứng việc vẽ Glance thật (`updateAll()` cần `AppWidgetManager` + launcher).

### TC-WG-11 — Unit test JVM: định dạng số hiển thị trên widget
- **Ưu tiên:** P2
- **Bước:** 1. `.\gradlew test --tests "*NumberDisplayTest"`.
- **Kết quả mong đợi:** Toàn bộ test pass, trong đó các test mới chốt:
  - **số bước đầy đủ** với dấu **chấm** phân cách nghìn: `312`, `1.000`, `6.250`, `12.345`, `1.234.567`; số âm ⇒ `0` (khác `formatStepCount` dạng rút gọn `6,2k` dùng cho vòng tròn 64dp ở Trang chủ);
  - **giờ ngủ**: `7h 30m`, `8h 0m`, `6h 15m`, số âm ⇒ `0h 0m`;
  - **nhịp tim**: `72 bpm` khi có số, **`-- bpm`** khi `≤ 0` (không bao giờ hiện `0 bpm`).

### TC-WG-12 — Chế độ tối (dark mode)
- **Ưu tiên:** P2
- **Bước:** 1. Bật **Dark theme** của hệ thống → 2. Xem widget; nếu launcher chưa vẽ lại, ép bằng `adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n com.healthyai.app/.presentation.widget.HealthWidgetReceiver`.
- **Kết quả mong đợi:** Nền widget đổi sang xám-xanh đậm (không còn thẻ trắng), chữ sáng đọc được, ba icon đổi sang bản `res/drawable-night/` (xanh lá / xanh ngọc / đỏ **nhạt**) — không chữ hay icon nào chìm vào nền.
- **Ghi chú:** Widget **không** dùng `GlanceTheme` (mặc định là **màu động** của hệ thống ⇒ tông tím lệch bản sắc app) mà lấy màu từ palette app (`ui/theme/Color.kt`) qua `ColorProvider(sáng, tối)`; icon lấy bản `-night` theo qualifier tài nguyên.

### TC-WG-13 — Đổi dữ liệu **thói quen** không còn vẽ lại widget
- **Ưu tiên:** P3
- **Bước:** 1. Ghi nhận nội dung widget → 2. Tạo thêm một thói quen, sửa một thói quen, ghi một nhật ký rồi xoá một thói quen → 3. Xem widget.
- **Kết quả mong đợi:** Nội dung widget **không đổi** — **đúng như thiết kế**, không phải lỗi: widget đã chuyển từ "tiến độ thói quen" sang **ba chỉ số sức khỏe**, nên `HabitRepositoryImpl` **không** còn gọi `HealthWidgetRefresher.refresh()`.
- **Ghi chú:** Xem lại khối "hành vi hiện tại" ở mục 3 #15 trước khi ghi nhận đây là lỗi.
## 16. TC-FB — Firebase Firestore sync (tùy chọn, **phải được người dùng bật**)

> Từ bản sửa **#37 / mục 3 #31**, phần này có thêm **cổng đồng ý**: mặc định app **không** tải gì lên đám mây. Worker phải qua **hai** điều kiện — (1) người dùng đã **bật** "Đồng bộ đám mây" ở Trang chủ (TC-HOME-16) và (2) log đó được tạo **sau** mốc đồng ý (`cloud_sync_consent_at`) — xem `CloudSyncPolicy`. Nhật ký ghi **trước** khi bật (ví dụ ở chế độ khách) **không bao giờ** bị đẩy lên.

Chỉ thực hiện khi đã cấu hình `google-services.json` và (nếu cần) tạo user Firebase Auth. Nên chạy thêm `firebase deploy --only firestore:rules` để áp `firestore.rules` trong repo (TC-FB-06).

### TC-FB-01 — App khởi động khi có `google-services.json`
- **Ưu tiên:** P1
- **Tiền điều kiện:** `app/google-services.json` đúng package `com.healthyai.app`.
- **Bước:** 1. `pm clear` → 2. Mở app → 3. Xem logcat.
- **Kết quả mong đợi:** Không có lỗi `FirebaseApp initialization unsuccessful` / `Default FirebaseApp is not initialized`.

### TC-FB-02 — Trạng thái chưa đăng nhập
- **Ưu tiên:** P1
- **Bước:** 1. Run `periodic_sync_worker` → 2. Kiểm tra Firestore.
- **Kết quả mong đợi:** Không có document nào được ghi; worker `success` nhưng **bỏ qua** bước upload ("Sync skipped: chưa đăng nhập") — xem TC-WM-08.

### TC-FB-03 — Upload log khi đã đăng nhập **và đã bật đồng bộ đám mây**
- **Ưu tiên:** P1
- **Tiền điều kiện:** Có user Firebase Auth; đã ghi 2 log `isDirty = 1` **sau khi** bật công tắc **"Đồng bộ đám mây"** ở Trang chủ (TC-HOME-16).
- **Bước:**
  1. Gán `uid` cho user (đăng nhập qua Firebase console/Test hoặc tạm set `FirebaseAuth` trong debug).
  2. Run `periodic_sync_worker`.
  3. Kiểm tra Firestore.
- **Kết quả mong đợi:**
  - Firestore có `users/{uid}/habit_logs/{logId}` với các trường: `habitId`, `habitName`, `category`, `value`, `unit`, `targetValueAtTime`, `loggedAt`, `note`.
  - Trong Room, các log đã sync có `isDirty = 0`.
  - Log `SyncWorker`: **"Sync completed successfully: 2 logs"**.
- **Ghi chú:** Từ bản sửa **#37**, worker còn phải qua **cổng riêng tư** `CloudSyncPolicy`: chưa bật đồng bộ (mặc định) hoặc log được tạo **trước** mốc đồng ý thì **không** upload — xem **TC-FB-08**.

### TC-FB-04 — Không upload lại log đã sync
- **Ưu tiên:** P2
- **Bước:** 1. Run `periodic_sync_worker` lần thứ hai liên tiếp.
- **Kết quả mong đợi:** Không có log nào được gửi lại (danh sách `isDirty = 1` rỗng).

### TC-FB-05 — Lỗi mạng khi upload
- **Ưu tiên:** P2
- **Bước:** 1. Ngắt mạng giữa chừng → 2. Run worker.
- **Kết quả mong đợi:** Log `isDirty` **vẫn là 1** (không đánh dấu nhầm là đã sync); worker retry (tối đa 3 lần).

### TC-FB-06 — Quy tắc bảo mật Firestore (mục 3 #31)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã `firebase deploy --only firestore:rules` (file `firestore.rules` trong repo, đường dẫn khai trong `firebase.json`).
- **Bước:**
  1. Kiểm tra Security Rules của project Firebase (Console → Firestore → Rules) khớp với `firestore.rules` trong repo.
  2. Thử đọc/ghi từ một client khác (không đăng nhập, hoặc user khác) vào `users/{uid}/habit_logs/*`.
- **Kết quả mong đợi:**
  - Rule chỉ cho `request.auth.uid == userId` đọc/ghi trong `users/{userId}/**`; **mọi** đường khác bị **từ chối**.
  - **Không** còn `allow read, write: if true;` (rủi ro rò rỉ dữ liệu sức khỏe).
- **Ghi chú:** Trước bản sửa #37 repo **không có** file rules nào (`firestore.rules` + `firebase.json` mới được thêm).

### TC-FB-08 — Cổng đồng ý: không upload khi chưa bật / log cũ (mục 3 #31)
- **Ưu tiên:** P0 (riêng tư)
- **Tiền điều kiện:** Đã đăng nhập (có `uid`); có **log cũ** ghi từ trước (ví dụ ghi ở chế độ khách, `isDirty = 1`).
- **Bước:**
  1. Vào Trang chủ, **để công tắc "Đồng bộ đám mây" TẮT** → run `periodic_sync_worker` → kiểm tra Firestore + Room.
  2. Bật công tắc **"Đồng bộ đám mây"** → ghi thêm **1 log mới** → run `periodic_sync_worker` → kiểm tra Firestore.
- **Kết quả mong đợi:**
  - Bước 1: **không** có gì trên Firestore; log cũ vẫn `isDirty = 1`; log worker ghi *"Sync skipped: chưa bật đồng bộ đám mây hoặc không có log nào tạo sau khi đồng ý"*.
  - Bước 2: **chỉ log mới** xuất hiện trên Firestore (`isDirty = 0`); **log cũ KHÔNG bị đẩy lên** dù vẫn `isDirty = 1`.
- **Ghi chú:** Cổng nằm ở `domain/usecase/sync/CloudSyncPolicy.logsToUpload()` (log chỉ được tải khi `loggedAt >= consentAtMillis`) — đơn vị test JVM: TC-PRIV-03.

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
- **Tiền điều kiện:** Trong ngày có ghi log thói quen **và** có số liệu sức khỏe của hôm đó (bước chân từ cảm biến/Health Connect, **nhịp tim đo bằng camera** — TC-HR-04).
- **Bước:** 1. Ghi log lúc 23:59 → 2. Đổi giờ hệ thống sang 00:01 ngày kế tiếp → 3. Mở lại app → 4. Mở tab Phân tích.
- **Kết quả mong đợi:** Log cũ rơi ra ngoài khoảng "hôm nay" → tiến độ về 0 (đúng theo nghiệp vụ), danh sách thói quen không đổi. **Thẻ "Tóm tắt sức khỏe" cũng đọc lại từ đầu ngày** (mục 3 #26): bước chân về số của ngày mới, và **ô "Tim" về `-- bpm`** — số nhịp tim của hôm qua **không** hiện lại. Tab Phân tích: thẻ **"Lịch sử sức khỏe 7 ngày"** vẫn còn dòng của **hôm qua** với nhịp tim đã đo (dữ liệu **không** mất, chỉ không hiện ở ô "Tim" của ngày mới) — xem **TC-HOME-15**.

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

### TC-EDGE-17 — Chuỗi tiếng Việt trong code không bị lỗi font (double-encode)
- **Ưu tiên:** P1
- **Bước:**
  1. `.\gradlew test --tests "*HomeViewModelTest"` (repo chưa commit wrapper — dùng Gradle đã cài, xem mục 2.2; JDK 21).
  2. **Quét tĩnh** string literal trong `app/src/main/java/com/healthyai/app/presentation/screen/home/HomeScreen.kt`: đọc file bằng **UTF-8**, encode lại bằng **cp1252** rồi decode **UTF-8** — nếu round-trip này **thành công** thì chuỗi đó đang bị double-encode. Kết quả mong đợi: **0** literal mojibake.
  3. Cài đè APK lên thiết bị/emulator → mở tab **Trang chủ** → kiểm tra **bằng mắt** các chuỗi từng bị lỗi: lời chào **"Chào buổi sáng / chiều / tối / đêm"**, dòng ngày **"Thứ …, dd tháng MM"**, tiêu đề thẻ **"Tóm tắt sức khỏe"**, nhãn **"Bước chân"**, **"Ngủ"**, tiêu đề **"Thói quen hôm nay"**, **"Hoàn thành"** / **"Đã hoàn thành"**, dòng **"Mục tiêu: …"**, tiêu đề thẻ khi thiếu quyền **"Bật đếm bước chân"**, nút + hộp thoại **"Đăng xuất"** / **"Huỷ"**.
- **Kết quả mong đợi:** Tất cả chuỗi hiển thị **đúng dấu tiếng Việt** (không còn dạng `ChÃ o buá»•i Ä‘Ãªm`, `TÃ³m táº¯t sá»©c khá»e`, `Ngá»§`), không có ô vuông/ký tự lạ, bố cục và cỡ chữ không đổi.
- **Ghi chú:** Lỗi phát sinh từ bản viết lại `HomeScreen.kt` (mục 3 #22, phụ lục C #31): **26 vị trí / 22 chuỗi** bị double-encode **một lần** (byte UTF-8 bị đọc như cp1252) — gồm cả nhãn dữ liệu mẫu trong `@Preview`. `string resource` (`strings.xml`) và các màn hình khác **không** bị, nên đây **không** phải lỗi font của thiết bị: trên cùng một màn hình vẫn có chuỗi tiếng Việt hiển thị đúng. Bản sửa chỉ đổi **string literal** (giữ nguyên CRLF và số dòng của file); phần **comment** trong file vẫn còn chữ mojibake nhưng **không** ảnh hưởng hiển thị.

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
| TC-REG-16 | Widget "Sức khỏe hôm nay" thêm được, hiện đúng bước chân/giấc ngủ/nhịp tim, tự cập nhật khi dữ liệu sức khỏe đổi, bấm mở app | P1 | |
| TC-REG-17 | `.\gradlew test` pass 100% | P0 | |
| TC-REG-18 | Dark mode không làm chữ không đọc được | P2 | |
| TC-REG-19 | Xoay màn hình & process death không crash | P1 | |
| TC-REG-20 | Không có `FATAL EXCEPTION` trong logcat suốt phiên test | P0 | |
| TC-REG-21 | Đăng xuất từ Home → quay về màn đăng nhập, dữ liệu thói quen còn nguyên (TC-AUTH-15) | P1 | |
| TC-REG-22 | Nâng cấp app từ bản cũ (schema v1/v2) → thói quen + nhật ký **giữ nguyên**, không crash (TC-DB-08, TC-DB-15) | P0 | |
| TC-REG-23 | Màn Ghi chép: nút **"Thêm vào nhật ký"** mở hộp thoại ghi nhận thói quen Dinh dưỡng và ghi được log (TC-CAM-15) | P1 | |
| TC-REG-24 | Gợi ý vận động đúng theo **đơn vị** thói quen: "30 phút" không bị đọc thành "30 bước" (TC-INS-29) | P1 | |
| TC-REG-25 | Lời chào màn Trang chủ dùng **tên tài khoản thật**; chế độ khách **không** hiện tên (không còn "Alex" cứng) — TC-HOME-12 | P1 | |
| TC-REG-26 | Màn Trang chủ hiển thị **đúng dấu tiếng Việt** (không lỗi font kiểu `ChÃ o buá»•i Ä‘Ãªm`) — TC-EDGE-17 | P1 | |
| TC-REG-27 | Ghi chép bữa ăn: chụp **món ăn** → chip **tên tiếng Việt** + emoji; chụp **không phải đồ ăn** (bàn trống/bầu trời) → thẻ **"Chưa nhận diện được món ăn"**, **không** có kết quả giả (TC-CAM-07, TC-CAM-08) | P1 | |
| TC-REG-28 | Ghi chép bữa ăn: **"Chọn ảnh từ thư viện"** → phân tích ảnh có sẵn (kể cả ảnh chụp dọc, không cần quyền Camera) ra chip tiếng Việt; Huỷ chọn ảnh thì im lặng (TC-CAM-18, TC-CAM-20) | P1 | |
| TC-REG-29 | Tab Phân tích: feed "BLOOM AI GỢI Ý" là **banner trượt tự động** (một gợi ý mỗi lúc, cao cố định ~240dp, tự cuộn ~5 giây, vuốt được, có chấm chỉ báo) — màn hình **không còn** bị dài vì các thẻ xếp dọc (TC-INS-40) | P1 | |
| TC-REG-30 | Ghi chép bữa ăn: chụp **đĩa cơm** → ra **tên món Việt** + **Có lợi cho cơ thể** + **Nên lưu ý**, **không** còn ra "Pizza"; mất mạng → thẻ lỗi + **"Thử lại"** (TC-CAM-22, TC-CAM-23). **Mục 3 #43:** không còn công tắc AI (bấm là phân tích luôn) | P0 | |
| TC-REG-31 | Ghi chép bữa ăn **nhanh & rõ ràng** (mục 3 #43): bấm "Phân tích món ăn" → **hộp thoại "Đang phân tích món ăn"** hiện ngay, **không tắt được** bằng Back/chạm ra ngoài, **tự đóng** khi có kết quả; thời gian chờ **giảm rõ rệt** so với trước (model `gemini-3.8-flash` + `thinkingLevel: low`); tab **Phân tích** có thẻ **"AI & quyền riêng tư"** mở được màn Chính sách bảo mật (TC-CAM-25, TC-CAM-26, TC-INS-42) | P1 | |
| TC-REG-32 | Đếm bước **chỉ khi app đang chạy** (mục 3 #44): đi bộ ~300 bước **khi app đang mở** → số tăng; **vuốt app khỏi tác vụ** rồi đi thêm ~500 bước → mở lại app, số **KHÔNG** tăng thêm 500 và **vẫn giữ** phần đã đếm; để app **ở nền** (không vuốt) rồi đi tiếp → số **vẫn tăng** (TC-STEP-04, TC-STEP-05) | P0 | |

---

## 19. Phụ lục A — Ma trận truy vết (UI ↔ ViewModel ↔ UseCase ↔ Data)

| Màn hình | ViewModel | UseCase | Nguồn dữ liệu | Test case |
|:--|:--|:--|:--|:--|
| Home | `HomeViewModel` | `GetTodayHabitsUseCase`, `GetHealthDataUseCase`, `GetHealthConnectPermissionStatusUseCase`, `GetNotificationPermissionStatusUseCase`, `GetLatestRecordedHeartRateUseCase` (nhịp ghi **trong ngày**), `GetStepCounterStatusUseCase` (quyền + cảm biến đếm bước), `AuthRepository.currentUserName` (tên trên lời chào) | Room `habits`, Health Connect (**nhịp tim hôm nay**), NotificationManager, Firebase Auth, cảm biến đếm bước (`StepCounterRepository`) | TC-HOME-01…15, TC-HC-01…17, TC-WM-13…16, TC-STEP-01…03 |
| Đếm bước (cảm biến máy) | `HomeViewModel` (`stepCounter`, `displaySteps`) + `StepCounterPermissionCard` | `StepCounterRepository.observeTodaySteps()` / `readTodaySteps()` → `StepCounterStore` → `StepCounterMath` (**chỉ tính phần app đang chạy** — mục 3 #44) | `AndroidStepCounterSensor` (`Sensor.TYPE_STEP_COUNTER`, quyền `ACTIVITY_RECOGNITION`) → prefs `healthy_ai_step_counter` (**có khoá `session_id`** = phiên chạy của tiến trình app); Health Connect **ưu tiên** khi có số | TC-STEP-01…10, TC-HOME-05, TC-HOME-06 |
| Đăng nhập | `AuthGateViewModel` + `LoginViewModel` | `AuthRepository` (signIn/signUp/authState/currentUserName/signOut) | Firebase Auth | TC-AUTH-01…16 |
| Habits | `HabitsViewModel` | `GetTodayHabitsUseCase`, `LogHabitUseCase` | Room `habits` + `habit_logs` | TC-HAB-01…30 |
| Add/Edit Habit | `AddEditHabitViewModel` | `AddHabitUseCase`, `UpdateHabitUseCase`, `DeleteHabitUseCase` | Room `habits` (soft delete: `isActive = 0`) | TC-ADD-01…27 |
| Camera | `CameraViewModel` | `FoodAnalysis` (**prompt + làm sạch** kết quả AI) → `FoodInsightSource`/`GeminiFoodInsightSource` (**AI Gemini: tên món + lợi ích/điều nên lưu ý**, hạ `thinkingLevel` + **model dự phòng** — mục 3 #43), `ImageJpegEncoder` (nén JPEG trước khi gửi), `GalleryImageLoader` (thu nhỏ + xoay EXIF cho ảnh chọn từ thư viện); `CameraScreen` → hộp thoại **"Đang phân tích món ăn"** khi `isAnalyzing`, `AppNavigation.openHabitLogFor(DINH DƯỠNG)` → `HabitsViewModel` | **Gemini API** (`generativelanguage.googleapis.com` qua cổng `HttpPost` → `HttpUrlConnectionPost`) sau `FoodInsightSource`; photo picker của hệ thống; Room `habits`/`habit_logs`. **Không** còn cổng opt-in (`food_ai_enabled` đã xoá ở mục 3 #43) | TC-CAM-01…26, TC-PRIV-04 |
| Đo nhịp tim | `HeartRateViewModel` | `SaveMeasuredHeartRateUseCase` (lưu), `PpgAnalyzer` + `HeartRateMath` (tính nhịp); `CameraPpgAnalyzer` đọc kênh đỏ từng khung hình camera | Camera sau + đèn flash (CameraX `ImageAnalysis`, on-device) → Room `health_data_history` (`heartRate`) | TC-HR-01…13, TC-HOME-14, TC-HOME-15 |
| Insights | `InsightsViewModel` | `GetHealthInsightsUseCase` (**feed mẹo xoay vòng** + `SuggestionCatalog`), `GetVitalityScoreUseCase` (**chỉ số sinh lực theo ngày** + `VitalityScoreMath`), `GetWeeklyConsistencyUseCase`, `GetSleepStageBreakdownUseCase`, `GetRecentHealthSummaryUseCase` (**lịch sử tóm tắt 7 ngày**) → `HealthSummaryHistory`, `SleepTrendMath` (**xu hướng 7 đêm giấc ngủ**) | Room `habit_logs` + `health_data_history` → `HabitRuleEngine` (**UI: banner trượt tự động `AiTipBanner`** + thẻ **"AI & quyền riêng tư"** → Chính sách bảo mật, mục 3 #43) | TC-INS-01…42 |
| Insights → Habits (nút gợi ý) | `InsightsScreen` → `AppNavigation` → `HabitsViewModel` | `SavedStateHandle` (`KEY_LOG_CATEGORY`) + `LogRequest` | Room `habits` | TC-INS-11, TC-INS-27, TC-INS-28 |
| (chưa có UI) | `CoreTrackingViewModel` | `GetHealthDataUseCase` | Health Connect | TC-ENV-03 (unit test) |
| Chính sách bảo mật | `PrivacyPolicyScreen` (không có ViewModel) | — | Route `privacy_policy` trong `AppNavigation`; `MainActivity` mở thẳng khi Health Connect gửi `ACTION_SHOW_PERMISSIONS_RATIONALE`/`ACTION_VIEW_PERMISSION_USAGE`; chuỗi `health_permissions_rationale` (`strings.xml`) | TC-PRIV-01, TC-PRIV-02 |
| Đồng bộ đám mây (opt-in) | `HomeViewModel` (`cloudSync`, `isSignedIn`, `setCloudSyncEnabled`) → `CloudSyncCard` | `SettingsRepository` (`observeCloudSync`/`getCloudSync`/`setCloudSyncEnabled`) → bảng `app_settings`; `CloudSyncPolicy` (cổng quyết định log nào được tải lên) | `SyncWorker` → `FirestoreSyncDataSource` → Firestore `users/{uid}/habit_logs` (rules ở `firestore.rules`); `AuthRepository.currentUserId` | TC-HOME-16, TC-PRIV-03, TC-FB-* |
| Widget | — (Glance `HealthWidget`) | `TodayHealthSummary` (ghép 3 chỉ số theo thứ tự ưu tiên nguồn), `HealthWidgetRefresher` (vẽ lại) | `StepCounterRepository` (cảm biến máy), `SleepTrackerRepository` + `HealthDataRepository` qua `GetHealthDataUseCase` (Health Connect), `GetLatestRecordedHeartRateUseCase` (nhịp tim đã đo) | TC-WG-01…13 |
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
| TC-ENV-07 | Icon launcher riêng & `allowBackup=false` (mục 3 #32) | P1 | | |
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
| TC-HOME-12 | Tên trên lời chào là tên tài khoản thật; chế độ khách không hiện tên (phụ lục C #30) | P1 | | |
| TC-HOME-13 | Unit test JVM tên hiển thị trên lời chào | P1 | | |
| TC-HOME-14 | Ô "Tim" mở màn đo nhịp tim (phụ lục C #23) | P1 | | |
| TC-HOME-15 | Ô "Tim" đọc lại từ đầu ngày (`-- bpm`) + bỏ dòng chú thích nguồn (mục 3 #26) | P1 | | |
| TC-HOME-16 | Thẻ "Đồng bộ đám mây": mặc định TẮT, tự bật, chế độ khách (mục 3 #31) | P0 | | |

### Đếm bước bằng cảm biến của máy (mục 3 #44)

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-STEP-01 | Thẻ xin quyền `ACTIVITY_RECOGNITION` (máy có cảm biến) | P1 | | |
| TC-STEP-02 | Số bước tăng khi app đang mở | P1 | | |
| TC-STEP-03 | Máy không có cảm biến đếm bước → không có thẻ | P2 | | |
| TC-STEP-04 | **Đóng hẳn app thì KHÔNG đếm tiếp** (bước đi lúc app không chạy không được cộng) | P0 | | |
| TC-STEP-05 | App ở **nền** (còn trong tác vụ) thì vẫn đếm | P0 | | |
| TC-STEP-06 | Sang ngày mới khi app còn chạy → nội suy mốc 00:00 | P2 | | |
| TC-STEP-07 | Máy khởi động lại giữa ngày → số bước không tụt | P2 | | |
| TC-STEP-08 | Health Connect thắng khi có số bước | P1 | | |
| TC-STEP-09 | Worker ghi lịch sử vẫn lấy số của cảm biến | P2 | | |
| TC-STEP-10 | Unit test JVM luật đếm bước (12 test) | P1 | | |

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
| TC-ADD-20 | Tần suất: chỉ còn "Hàng ngày" (mục 3 #30) | P2 | | |
| TC-ADD-21 | Hủy dialog xác nhận xóa | P1 | | |
| TC-ADD-22 | Unit test JVM luồng xóa mềm | P1 | | |
| TC-ADD-23 | Ô "Mục tiêu" hiển thị số sạch (bỏ đuôi `.0`) | P2 | | |
| TC-ADD-24 | Unit test JVM định dạng mục tiêu | P1 | | |
| TC-ADD-25 | Chống trùng tên khi tạo/đổi tên | P1 | | |
| TC-ADD-26 | Unit test JVM chống trùng tên | P1 | | |
| TC-ADD-27 | Unit test JVM chống double-submit khi lưu | P1 | | |
| TC-ADD-28 | Unit test JVM: tần suất luôn lưu "Hàng ngày" (mục 3 #30) | P2 | | |

### Camera, Insights

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-CAM-01 | Chưa cấp quyền Camera (nút "Chọn ảnh từ thư viện" vẫn hiện) | P0 | | |
| TC-CAM-02 | Cấp quyền Camera | P0 | | |
| TC-CAM-03 | Từ chối quyền Camera | P0 | | |
| TC-CAM-04 | Preview (mô phỏng) | P1 | | |
| TC-CAM-05 | Nút "Phân tích món ăn" (kèm **hộp thoại "Đang phân tích món ăn"** — mục 3 #43) | P1 | | |
| TC-CAM-06 | Trạng thái đang phân tích (**hộp thoại không tắt được**, tự đóng khi xong) (mục 3 #43) | P2 | | |
| TC-CAM-07 | Kết quả nhận diện (món ăn **tiếng Việt**, phụ lục C #21) | P1 | | |
| TC-CAM-08 | Ảnh không có món ăn → thẻ "Chưa nhận diện được món ăn" (phụ lục C #21) | P2 | | |
| TC-CAM-09 | Lỗi phân tích | P2 | | |
| TC-CAM-10 | Thu hồi quyền khi đang chạy | P2 | | |
| TC-CAM-11 | Nút phân tích disable khi camera chưa sẵn sàng | P1 | | |
| TC-CAM-12 | Không mở được camera → thẻ lỗi đỏ | P1 | | |
| TC-CAM-13 | Chụp & phân tích liên tiếp nhiều lần | P2 | | |
| TC-CAM-14 | Preview hoạt động lại khi quay lại màn hình | P2 | | |
| TC-CAM-15 | Nút "Thêm vào nhật ký" → mở hộp thoại ghi nhận thói quen Dinh dưỡng (phụ lục C #22) | P1 | | |
| TC-CAM-16 | ~~Unit test JVM: lọc & dịch nhãn ảnh ML Kit~~ — **đã bỏ** cùng ML Kit (mục 3 #42); xem TC-CAM-24 | P1 | | |
| TC-CAM-17 | Unit test JVM: máy trạng thái màn Ghi chép — gồm **bấm phân tích là gọi AI ngay** (mục 3 #42, #43) | P1 | | |
| TC-CAM-18 | "Chọn ảnh từ thư viện" → phân tích ảnh có sẵn, kết quả tiếng Việt (mục 3 #25) | P1 | | |
| TC-CAM-19 | Ảnh chọn từ thư viện không phải đồ ăn → "Chưa nhận diện được món ăn" (mục 3 #25) | P2 | | |
| TC-CAM-20 | Huỷ chọn ảnh / ảnh đọc lỗi / vẫn dùng được khi chưa cấp quyền Camera (mục 3 #25) | P2 | | |
| TC-CAM-21 | ~~Cổng đồng ý: công tắc AI **mặc định TẮT** & chặn gửi ảnh đi~~ — **đã bỏ công tắc** (mục 3 #43) | P0 | | |
| TC-CAM-22 | Trái cây / đồ uống / đồ ăn vặt cũng phân tích được; đĩa cơm **không** còn ra "Pizza" (mục 3 #42) | P1 | | |
| TC-CAM-23 | Lỗi AI (mạng / quá tải / khoá sai / chưa cấu hình) + nút **"Thử lại"** (mục 3 #42, #43) | P1 | | |
| TC-CAM-24 | Unit test JVM: luật prompt/làm sạch + tầng Gemini (**28 test**) (mục 3 #42, #43) | P1 | | |
| TC-CAM-25 | Hộp thoại **"Đang phân tích món ăn"** (không tắt bằng Back/chạm ra ngoài, tự đóng khi xong) (mục 3 #43) | P1 | | |
| TC-CAM-26 | Tốc độ & model: `gemini-3.8-flash` + `thinkingLevel: low`, **model dự phòng** khi 503/429 (mục 3 #43) | P1 | | |
| TC-INS-01 | Trạng thái rỗng: feed mẹo khởi đầu + "Chưa đủ dữ liệu" (mục 3 #27, #28) | P1 | | |
| TC-INS-02 | Nước thiếu → mẹo nhóm CONCERN, không còn thẻ cố định (mục 3 #27) | P0 | | |
| TC-INS-03 | Nước đủ → mẹo duy trì (GENERAL) (mục 3 #27) | P1 | | |
| TC-INS-04 | Feed chỉ gồm danh mục người dùng có thói quen (mục 3 #27) | P1 | | |
| TC-INS-05 | Ngủ thiếu (log) → mẹo Giấc ngủ CONCERN (mục 3 #27) | P1 | | |
| TC-INS-06 | Mẹo xoay vòng theo ngày (mục 3 #27) | P1 | | |
| TC-INS-07 | Giới hạn 4 thẻ & sắp xếp theo ưu tiên (mục 3 #27) | P1 | | |
| TC-INS-08 | Vận động (bước) dưới ngưỡng → CONCERN (phụ lục C #17) | P1 | | |
| TC-INS-09 | Vận động đạt mục tiêu → mẹo duy trì (phụ lục C #17) | P1 | | |
| TC-INS-10 | Nhịp tim cao → thẻ Thiền định (phụ lục C #16) | P1 | | |
| TC-INS-11 | Nút hành động trong thẻ gợi ý AI | P2 | | |
| TC-INS-12 | Chỉ số sinh lực: hoàn thành đủ mọi thành phần (mục 3 #28) | P0 | | |
| TC-INS-13 | Chỉ số sinh lực **tụt** khi bỏ thói quen (mục 3 #28) | P0 | | |
| TC-INS-14 | Chỉ số sinh lực dùng cả dữ liệu sức khỏe thật (mục 3 #28) | P1 | | |
| TC-INS-15 | Điểm cập nhật realtime | P1 | | |
| TC-INS-16 | Biên trên điểm | P2 | | |
| TC-INS-17 | Chưa đủ dữ liệu → không hiện điểm ảo (mục 3 #28) | P1 | | |
| TC-INS-18 | Gợi ý thiếu ngủ từ Health Connect (phụ lục C #16) | P1 | | |
| TC-INS-19 | Gợi ý nhịp tim cao → Thiền định (phụ lục C #16) | P1 | | |
| TC-INS-20 | Kết hợp 2 nguồn gợi ý | P2 | | |
| TC-INS-21 | Biểu đồ nhất quán hàng tuần (dữ liệu thật) | P1 | | |
| TC-INS-22 | Loading | P2 | | |
| TC-INS-23 | Thẻ giấc ngủ có biểu đồ 7 đêm nhờ nguồn tự đo (mục 3 #29) | P1 | | |
| TC-INS-24 | Thẻ giấc ngủ kèm chi tiết pha Health Connect (mục 3 #29) | P1 | | |
| TC-INS-25 | Delta điểm: ẩn khi thiếu dữ liệu tuần trước | P1 | | |
| TC-INS-26 | Delta điểm: tăng / giảm / không đổi | P1 | | |
| TC-INS-27 | Nút hành động khi chưa có thói quen tương ứng | P2 | | |
| TC-INS-28 | Unit test JVM yêu cầu ghi nhận từ Insights | P1 | | |
| TC-INS-29 | Vận động theo mục tiêu cho thói quen đo bằng phút (phụ lục C #17) | P1 | | |
| TC-INS-30 | Unit test JVM rule engine + kho mẹo (phụ lục C #16, #17) | P1 | | |
| TC-INS-31 | Thẻ "Lịch sử sức khỏe 7 ngày": biểu đồ cột + bảng 7 dòng (mục 3 #26) | P1 | | |
| TC-INS-32 | Thẻ "Lịch sử sức khỏe 7 ngày" khi chưa có dữ liệu (mục 3 #26) | P2 | | |
| TC-INS-33 | Unit test JVM gom lịch sử sức khỏe theo ngày (mục 3 #26) | P1 | | |
| TC-INS-34 | Unit test JVM kho mẹo gợi ý (mục 3 #27) | P1 | | |
| TC-INS-35 | Unit test JVM chỉ số sinh lực (mục 3 #28) | P1 | | |
| TC-INS-36 | Unit test JVM xu hướng giấc ngủ (mục 3 #29) | P1 | | |
| TC-INS-40 | Feed "BLOOM AI GỢI Ý" là banner **trượt tự động**, một gợi ý mỗi lúc (mục 3 #41) | P1 | | |
| TC-INS-41 | Unit test JVM xoay vòng theo ngày phủ hết kho mẹo, không lặp (mục 3 #41) | P1 | | |
| TC-INS-42 | Thẻ **"AI & quyền riêng tư"** cuối tab Phân tích → mở màn Chính sách bảo mật (mục 3 #43) | P1 | | |

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
| TC-WG-01 | Widget hiện đủ 3 chỉ số của hôm nay | P1 | | |
| TC-WG-02 | Chưa có số nào → `--` / `-- bpm` (không hiện `0`) | P1 | | |
| TC-WG-03 | Nguồn số: bước chân theo cảm biến, giấc ngủ/nhịp tim theo Health Connect | P1 | | |
| TC-WG-04 | Widget tự vẽ lại khi app ghi dữ liệu sức khỏe (đã sửa #15) | P1 | | |
| TC-WG-05 | Bấm widget mở app | P1 | | |
| TC-WG-06 | Số bước chỉ tính phần app đang chạy (mục 3 #44) | P2 | | |
| TC-WG-07 | Resize | P2 | | |
| TC-WG-08 | Sau reboot | P2 | | |
| TC-WG-09 | Unit test JVM: ghép 3 chỉ số theo thứ tự ưu tiên nguồn | P1 | | |
| TC-WG-10 | Unit test JVM: repository vẽ lại widget khi dữ liệu sức khỏe đổi | P1 | | |
| TC-WG-11 | Unit test JVM: định dạng số trên widget | P2 | | |
| TC-WG-12 | Dark mode: nền + icon + chữ đều đọc được | P2 | | |
| TC-WG-13 | Đổi thói quen KHÔNG còn vẽ lại widget (chủ ý) | P3 | | |
| TC-FB-01 | Khởi động có Firebase | P1 | | |
| TC-FB-02 | Chưa đăng nhập | P1 | | |
| TC-FB-03 | Upload log | P1 | | |
| TC-FB-04 | Không upload lại | P2 | | |
| TC-FB-05 | Lỗi mạng | P2 | | |
| TC-FB-06 | Security rules | P2 | | |
| TC-FB-07 | App không có Firebase | P1 | | |
| TC-FB-08 | Cổng đồng ý: không upload khi chưa bật / log cũ (mục 3 #31) | P0 | | |
| TC-PRIV-01 | Mở màn "Chính sách bảo mật" từ Trang chủ (mục 3 #31) | P1 | | |
| TC-PRIV-02 | Health Connect rationale mở màn chính sách, không qua đăng nhập (mục 3 #31) | P1 | | |
| TC-PRIV-03 | Unit test JVM cổng đồng bộ đám mây (mục 3 #31) | P1 | | |
| TC-PRIV-04 | Chính sách bảo mật nói đúng về AI phân tích món ăn (mục 3 #42) | P1 | | |
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
| TC-EDGE-17 | Chuỗi tiếng Việt không bị lỗi font (double-encode) | P1 | | |

### Đo nhịp tim (camera + đèn flash)

| ID | Tên | Ưu tiên | KQ | Ghi chú |
|:--|:--|:--|:--|:--|
| TC-HR-01 | Ô "Tim" ở Trang chủ mở màn đo nhịp tim (không thêm tab) | P1 | | |
| TC-HR-02 | Hướng dẫn 3 bước trước khi đo + đèn flash bật ở chế độ chờ | P1 | | |
| TC-HR-03 | Chưa cấp quyền Camera → thẻ xin quyền, từ chối thì báo lỗi rõ | P1 | | |
| TC-HR-04 | Đo thành công: số nhịp + dạng sóng + tự chốt sau ≥ 15 giây | P1 | | |
| TC-HR-05 | Hướng dẫn đổi theo tình trạng: chưa thấy ngón tay / tín hiệu yếu | P1 | | |
| TC-HR-06 | Kết quả tự lưu vào `health_data_history` + hiện ở ô "Tim" | P1 | | |
| TC-HR-07 | Ưu tiên nguồn nhịp tim (Health Connect thắng số ghi trên máy) + 4 test `HomeViewModelTest` | P1 | | |
| TC-HR-08 | Tín hiệu nhiễu hết 30 giây → **không** hiện số, **không** lưu | P1 | | |
| TC-HR-09 | "Dừng đo" giữa chừng: không lưu gì, đo lại được | P1 | | |
| TC-HR-10 | Máy không có đèn flash → cảnh báo trước khi đo | P2 | | |
| TC-HR-11 | Rời màn / khoá máy / xoay màn hình khi đang đo: đèn flash tắt, không rò rỉ | P1 | | |
| TC-HR-12 | Chẩn đoán: thuật toán PPG (unit test JVM) | P1 | | |
| TC-HR-13 | Chẩn đoán: máy trạng thái của màn đo (unit test JVM) | P1 | | |

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
| 15 | ~~Widget không tự refresh~~ **→ Đã sửa (và widget đã đổi nội dung)** | `presentation/widget/HealthWidget.kt`, `data/repository/HealthDataRepositoryImpl.kt`, `domain/widget/HealthWidgetRefresher.kt`, `presentation/widget/HealthWidgetRefresherImpl.kt`, `di/AppModule.kt` | Trung bình | TC-WG-04, TC-WG-10, TC-WG-13 | ✅ Đã sửa |
| 16 | ~~`getRecentHealthHistoryFlow(limit)` bỏ qua tham số `limit`~~ **→ Đã sửa** (lỗi thật rộng hơn: `health_data_history` ghi **1 hàng mỗi lần sync 6 giờ**, nên `history.take(3)` là "3 lần sync" (~18 giờ) chứ không phải 3 ngày, và "nhịp tim 7 ngày" thực chất chỉ ~1,75 ngày) | `data/local/dao/health/HealthDataDao.kt` (bỏ `LIMIT 30` hard-code, nhận `:limit`), `data/repository/HealthDataRepositoryImpl.kt` (truyền `limit`), `domain/usecase/HabitRuleEngine.kt` (`latestRowPerDay()` + `areConsecutiveDays()` — gom theo **ngày**, cảnh báo cần **3 ngày liên tiếp**), `app/src/test/.../{domain/usecase/HabitRuleEnginePersonalizedTest,data/repository/HealthDataRepositoryImplTest}.kt` | Trung bình | TC-INS-18, TC-INS-19, TC-INS-30 | ✅ Đã sửa |
| 17 | ~~Nhãn danh mục "Vận động" so sánh với 5000/8000 bất kể đơn vị~~ **→ Đã sửa** | `domain/usecase/HabitRuleEngine.kt` (`analyzeExerciseLogs` tách `analyzeStepLogs` cho log đơn vị **bước** + `analyzeExerciseTargetCompletion` so **tỉ lệ `value / targetValueAtTime`** cho đơn vị khác: < 50% → HIGH, < 80% → LOW), `app/src/test/.../domain/usecase/HabitRuleEngineTest.kt` (4 test mới) | Trung bình | TC-INS-08, TC-INS-09, TC-INS-29, TC-INS-30 | ✅ Đã sửa |
| 18 | ~~Nền màn hình hard-code sáng → dark mode tương phản kém~~ **→ Đã sửa** (lỗi thật nặng hơn: `Card` để `Color.White` nên dark mode thành **thẻ trắng + chữ `onSurface` sáng ⇒ mất chữ**) | `presentation/ui/theme/{Color,Theme}.kt`, `presentation/screen/home/HomeScreen.kt`, `habits/HabitsScreen.kt`, `habits/addedit/AddEditHabitScreen.kt`, `insights/InsightsScreen.kt`, `camera/CameraScreen.kt`, `auth/LoginScreen.kt`, `presentation/navigation/AppNavigation.kt`, `presentation/ui/component/InfoActionCard.kt`, `app/src/test/.../presentation/ui/theme/ThemeSurfaceColorsTest.kt` | Thấp | TC-EDGE-01, TC-EDGE-16 | ✅ Đã sửa |
| 19 | **Chưa export `release` signing / chưa cấu hình ProGuard cho Room-Glance** **→ KHÔNG sửa (đã chốt với team)** | `app/build.gradle.kts`, `proguard-rules.pro` | Trung bình | — | ⏭️ Không sửa — app **làm thử nghiệm, không phát hành**, nên không cần keystore/ký release (xem chi tiết ở #38) |
| 20 | ~~`HabitRuleEngineTest.kt` không compile (3 helper thiếu `targetValueAtTime`) → `.\gradlew test` fail ngay ở bước compile, toàn bộ unit test không chạy được~~ | `app/src/test/.../HabitRuleEngineTest.kt` | **Cao (chặn test)** | TC-ENV-03 | ✅ Đã sửa |
| 21 | ~~ML Kit `Image Labeling` trả **nhãn ảnh chung** ("Food", "Fruit", "Table"…) chứ không phải món ăn/dinh dưỡng → chip "kết quả nhận diện" có thể hiển thị nhãn không đúng ngữ cảnh bữa ăn~~ **→ Đã sửa** (lỗi thật rộng hơn khi test máy thật: màn **luôn** ra "kết quả" kể cả khi chụp **không phải đồ ăn**, và nhãn hiện ra là **tiếng Anh**) | `domain/usecase/food/FoodRecognition.kt` (mới: danh mục ~180 món/nguyên liệu, khớp **theo từ**, dịch **tiếng Việt + emoji**, ngưỡng **0.5**, tối đa **5** món), `data/vision/ImageLabelSource.kt` + `data/vision/MlKitImageLabelSource.kt` (mới: cổng ra ML Kit, ngưỡng **ứng viên** 0.3), `di/AppModule.kt` (bind `ImageLabelSource`, **không** `@Singleton`), `presentation/screen/camera/CameraViewModel.kt` (`detectedFoods` + `noFoodDetected`, `runAnalysis` tách để test JVM), `presentation/screen/camera/CameraScreen.kt` (chip emoji + tên tiếng Việt, thẻ **"Chưa nhận diện được món ăn"**, dòng nói rõ *chỉ là tên món, không phải số liệu dinh dưỡng*, nhãn "Tự động" → **"Trên máy"**), `app/src/test/.../domain/usecase/food/FoodRecognitionTest.kt` (15 test), `app/src/test/.../presentation/screen/camera/CameraViewModelTest.kt` (8 test) | Trung bình | TC-CAM-07, TC-CAM-08, TC-CAM-16, TC-CAM-17 | ✅ Đã sửa |
| 22 | ~~Nút **"Thêm vào nhật ký"** trong thẻ kết quả camera vẫn là TODO (chưa nối vào `habit_logs`)~~ **→ Đã sửa** | `presentation/screen/camera/CameraScreen.kt` (`CameraScreen(onAddToJournal)` → `DetectionResultCard.onClick`), `presentation/navigation/AppNavigation.kt` (`NavHostController.openHabitLogFor(category)` dùng chung với màn Phân tích), `presentation/screen/habits/HabitsScreen.kt` (Toast khi chưa có thói quen phù hợp) | Trung bình | TC-CAM-07, TC-CAM-15, TC-INS-27 | ✅ Đã sửa |
| 23 | ~~Chưa có unit/UI test cho luồng camera (ML Kit + CameraX chỉ test được trên thiết bị)~~ **→ Đã trả một phần** | `presentation/screen/camera/CameraViewModel.kt` (`runAnalysis` nhận **hàm lấy nhãn** nên máy trạng thái test được trên JVM) + `app/src/test/.../presentation/screen/camera/CameraViewModelTest.kt` (8 test), `app/src/test/.../domain/usecase/food/FoodRecognitionTest.kt` (15 test). **Còn lại:** phần CameraX/`PreviewView` (chụp ảnh, bind lifecycle) vẫn **chỉ test được trên thiết bị** — không đổi ở lượt rà soát này (cần emulator/máy thật, xem TC-ENV-04) | Thấp | TC-CAM-16, TC-CAM-17 | 🟡 Đã trả một phần |
| 24 | ~~`SyncWorker` trả `Result.failure()` ngay khi `auth.currentUser == null` → không có cơ chế chờ đăng nhập, job fail vĩnh viễn~~ | `worker/SyncWorker.kt` | Thấp | TC-WM-08 | ✅ Đã sửa |
| 25 | ~~Thẻ giấc ngủ phụ thuộc dữ liệu pha của thiết bị: nếu Health Connect chỉ trả `STAGE_TYPE_SLEEPING` (không chia pha) thì thẻ hiển thị trạng thái rỗng dù có dữ liệu ngủ~~ **→ Đã sửa** | `HealthConnectDataSourceImpl.getSleepStageBreakdown` (cộng thêm thời lượng **phiên không chia pha** vào `unstagedMinutes`), `domain/model/SleepStageBreakdown.kt` (**`unstagedMinutes`** + **`hasStages`**; `hasData` = có pha **hoặc** có phiên không chia pha), `InsightsScreen.SleepQualityTrendCard` (vẽ thanh pha khi `hasStages`, còn lại hiện dòng *"Health Connect chỉ báo tổng thời gian ngủ (máy không chia pha Sâu/Nông/REM): … trong 7 đêm gần nhất."* — `insights_sleep_unstaged_total`), `app/src/test/.../domain/model/SleepStageBreakdownTest.kt` (3 test) | Thấp | TC-INS-37 | ✅ Đã sửa (mục 3 #33) |
| 26 | ~~Thẻ giấc ngủ đọc Health Connect mỗi lần mở màn hình (không cache/Room) → phải mở lại màn Phân tích mới thấy số liệu mới, không tự cập nhật realtime như 2 thẻ kia~~ **→ Đã sửa** (đọc lại khi **quay về** màn, không đọc liên tục khi đang hiển thị) | `presentation/screen/insights/InsightsScreen.kt` (**`LifecycleResumeEffect`** → `InsightsViewModel.refresh()`), `InsightsViewModelTest` (`coVerify(exactly = 2)` sau `refresh()` — trước là 1 lần lúc mở app) | Thấp | TC-INS-38 | ✅ Đã sửa (mục 3 #34) |
| 27 | ~~Biểu đồ nhất quán tuần lấy mẫu số là **số thói quen đang hoạt động hiện tại** → xoá/tạo thói quen sẽ làm thay đổi tỉ lệ của cả các ngày trong quá khứ (không phải snapshot tại thời điểm đó)~~ **→ Đã sửa** | `domain/usecase/habit/GetWeeklyConsistencyUseCase.kt` (mẫu số mỗi ngày = thói quen có `createdAt < mốc đầu ngày kế tiếp`), `app/src/test/.../GetWeeklyConsistencyUseCaseTest.kt` (**+2 test**: ngày trước khi thói quen được tạo không tính nó vào mẫu số; tạo thói quen **hôm nay** không làm tụt tỉ lệ các ngày trước). **Ghi chú:** mục tiêu vẫn so với `Habit.targetValue` hiện tại (giữ nguyên nghiệp vụ, chỉ sửa mẫu số) | Thấp | TC-INS-21 | ✅ Đã sửa (mục 3 #35) |
| 28 | ~~Chưa có UI **Đăng xuất** → sau khi đăng nhập (hoặc bấm "Bỏ qua") không có đường quay lại màn đăng nhập nếu không xoá dữ liệu app~~ **→ Đã sửa** | `domain/repository/AuthRepository.kt`, `data/repository/AuthRepositoryImpl.kt`, `presentation/navigation/{AuthGateViewModel,AppNavigation}.kt`, `presentation/screen/home/HomeScreen.kt`, `app/src/test/.../{presentation/navigation/AuthGateViewModelTest,data/repository/AuthRepositoryImplTest}.kt` | Thấp | TC-AUTH-15, TC-AUTH-16 | ✅ Đã sửa |
| 29 | Passphrase SQLCipher được ghi bằng `SharedPreferences.apply()` (**bất đồng bộ**) ngay trước khi Room tạo DB → nếu tiến trình bị kill trong khe thời gian đó, bản mã chưa xuống đĩa → lần sau sinh khoá khác → DB cũ không mở được nữa → `LegacyDatabaseKeyMigration` **xoá DB** ⇒ mất dữ liệu (khe rất hẹp, khó tái hiện nhưng hậu quả nặng) | `data/local/security/AndroidPassphraseStorage.kt` (`write()` nay dùng `commit()`), `data/local/security/DatabasePassphraseProvider.kt` (ghi **trước** khi trả khoá) | Thấp | TC-DB-16, TC-DB-11 | ✅ Đã sửa |
| 30 | ~~Lời chào màn Trang chủ **hard-code `Text("Alex")`** → đăng nhập khách hay đăng nhập thật đều chào "Alex"~~ **→ Đã sửa** | `domain/repository/AuthRepository.kt` (+`currentUserName`), `data/repository/AuthRepositoryImpl.kt` (`displayName` → phần trước `@` của email → `null` khi khách), `presentation/screen/home/{HomeViewModel,HomeScreen}.kt` (`HomeUiState.userName` + `GreetingHeader(userName, …)`: khách thì **không** vẽ dòng tên, câu chào bỏ dấu phẩy), `app/src/test/.../{presentation/screen/home/HomeViewModelTest,data/repository/AuthRepositoryImplTest}.kt` | Trung bình | TC-HOME-01, TC-HOME-12, TC-HOME-13 | ✅ Đã sửa |
| 31 | ~~String literal tiếng Việt trong `HomeScreen.kt` bị double-encode (UTF-8 bị đọc như cp1252) → màn Trang chủ hiển thị sai dấu: `ChÃ o buá»•i Ä‘Ãªm`, `TÃ³m táº¯t sá»©c khá»e`, `Ngá»§`, nút `ÄÄƒng xuáº¥t`…~~ **→ Đã sửa** (**26** vị trí / **22** chuỗi, gồm nhãn trong `@Preview`) | `presentation/screen/home/HomeScreen.kt` | Trung bình | TC-EDGE-17, TC-REG-26 | ✅ Đã sửa |
| 32 | **Bước chân và giờ ngủ "đọc lại từ đầu ngày" nhưng nhịp tim thì không** — sang ngày mới ô "Tim" vẫn hiện số của **hôm qua**, và **chưa có chỗ xem lại lịch sử tóm tắt sức khỏe** để AI phân tích | `HealthDataRepositoryImpl.getLatestRecordedHeartRate()` đọc hàng **mới nhất trong cả bảng** (không lọc `recordedAt`) và `HealthConnectDataSourceImpl.getRecentHeartRate()` chỉ lấy **1 giờ gần nhất** ⇒ 00:05 vẫn dính số của tối hôm trước; 3 dòng chú thích nguồn dưới thẻ "Tóm tắt sức khỏe" cũng dài dòng không cần thiết. Nay: `HealthDataDao.getLatestHeartRateSince(fromTs)` + `getLatestRecordedHeartRate()` truyền **đầu ngày hôm nay**; `getRecentHeartRate()` → **`getTodayHeartRate()`** (trung bình **00:00 → hiện tại**, cùng mốc với bước chân/calo); `HomeUiState.displayHeartRate = 0` ⇒ UI hiện **`-- bpm`** (`formatHeartRate()` trong `HomeScreen.kt`) và **bỏ 3 dòng chú thích nguồn**; tab Phân tích thêm **`domain/model/DailyHealthSummary.kt`** + **`domain/usecase/health/HealthSummaryHistory.kt`** (hàm thuần, gom **hàng mới nhất mỗi ngày**, luôn đủ 7 ngày) + **`GetRecentHealthSummaryUseCase`** đọc `HealthDataDao.getHealthDataSince(fromTs)` (cắt theo **mốc thời gian**) → thẻ **"Lịch sử sức khỏe 7 ngày"** (`InsightsScreen`: biểu đồ cột bước chân + bảng 7 dòng ngày · bước chân · giờ ngủ · nhịp tim). Số cũ **không** mất: vẫn nằm trong lịch sử 7 ngày cho AI phân tích | Cao | TC-HOME-15, TC-INS-31, TC-INS-32, TC-INS-33, TC-HR-07, TC-HOME-05, TC-HOME-06, TC-HOME-14, TC-EDGE-11 | ✅ Đã sửa |
| 33 | **Feed "BLOOM AI GỢI Ý" lặp lại một thẻ cảnh báo cố định cho tới khi người dùng xử lý xong** | `domain/usecase/SuggestionCatalog.kt` (mới: kho mẹo theo danh mục, nhóm **CONCERN**/**GENERAL**), `domain/usecase/HabitRuleEngine.kt` (**bỏ** các hàm dựng thẻ cảnh báo `analyzeSleepLogs`/`analyzeWaterLogs`/`analyzeStepLogs`/`analyzeExerciseTargetCompletion`/`analyzeHealthTrends` → `flaggedCategories()` chỉ để **chọn nhóm mẹo**, `generateSuggestions(activeHabits, recentLogs, healthHistory, rotationSeed)` **xoay vòng theo ngày**, tối đa **4** thẻ, `id` duy nhất), `domain/usecase/health/GetHealthInsightsUseCase.kt` (truyền thêm `getAllActiveHabits()`), `app/src/test/.../domain/usecase/{HabitRuleEngineTest,HabitRuleEngineExtendedTest,HabitRuleEnginePersonalizedTest,HabitRuleEngineStepTrendTest,SuggestionCatalogTest}.kt` | Trung bình | TC-INS-01…TC-INS-11, TC-INS-18…TC-INS-20, TC-INS-29, TC-INS-30, TC-INS-34 | ✅ Đã sửa |
| 34 | **"Chỉ số sinh lực" không phản ánh hoạt động thật**: điểm lấy trung bình cả tuần với mặc định **50/70** nên bỏ thói quen vài ngày điểm vẫn giữ nguyên | `domain/usecase/health/VitalityScoreMath.kt` (mới: điểm theo **từng ngày**, ghép **thói quen + giờ ngủ + bước chân**, chuẩn hoá theo thành phần có nguồn, ngày thiếu dữ liệu = 0; `hasObservations()` cho phần delta), `domain/usecase/health/GetVitalityScoreUseCase.kt` (đọc thêm `getHealthHistorySince`), `domain/model/VitalityScore.kt` (thêm `hasData`), `presentation/screen/insights/InsightsScreen.kt` (`VitalityScoreCard`: trạng thái "Chưa đủ dữ liệu", màu theo dải điểm, chú thích cách tính), `app/src/test/.../domain/usecase/health/{VitalityScoreMathTest,GetVitalityScoreUseCaseTest}.kt` | Trung bình | TC-INS-01, TC-INS-12…TC-INS-17, TC-INS-25, TC-INS-26, TC-INS-35 | ✅ Đã sửa |
| 35 | **"Xu hướng chất lượng giấc ngủ" chỉ đọc pha Sâu/Nông/REM từ Health Connect** → máy không có Health Connect / không chia pha thì thẻ trống, và số liệu **không tự cập nhật** (đọc lại mỗi lần mở màn) | `domain/model/SleepTrend.kt` (mới: `SleepNight`, `SleepTrend`, `SleepQuality`), `domain/usecase/health/SleepTrendMath.kt` (mới: dựng 7 đêm từ `health_data_history` + xếp loại theo giờ ngủ), `presentation/screen/insights/InsightsViewModel.kt` (`InsightsUiState.sleepTrend` từ Flow), `presentation/screen/insights/InsightsScreen.kt` (`SleepQualityTrendCard(trend, breakdown)`: biểu đồ 7 đêm + trung bình + chip xếp loại; khối pha chỉ hiện **thêm** khi có dữ liệu), `app/src/test/.../domain/usecase/health/SleepTrendMathTest.kt` | Trung bình | TC-INS-23, TC-INS-24, TC-INS-36 | ✅ Đã sửa |
| 36 | **`HabitFrequency.WEEKLY`/`MONTHLY` chọn được ở màn Thêm/Sửa nhưng không logic nào đọc** (chọn xong chỉ đổi nhãn — mọi thói quen vẫn được tính theo NGÀY) | `presentation/screen/habits/addedit/AddEditHabitScreen.kt` (bỏ vòng lặp `HabitFrequency.values()`, chỉ còn "Hàng ngày" dạng radio không bấm được + dòng chú thích), `presentation/screen/habits/addedit/AddEditHabitViewModel.kt` (bỏ `frequency` khỏi `AddEditHabitUiState` + bỏ `onFrequencyChange`; `saveHabit()` ghi thẳng `HabitFrequency.DAILY` để chuẩn hoá giá trị cũ), `app/src/test/.../presentation/screen/habits/addedit/AddEditHabitViewModelTest.kt` (1 test mới). Enum `HabitFrequency` + cột `habits.frequency` **giữ nguyên** → không cần migration | Thấp | TC-ADD-20, TC-ADD-28 | ✅ Đã sửa |
| 37 | **Nhật ký thói quen (gồm `note` tự do) được đẩy lên Firestore không có sự đồng ý, không có security rules, và chính sách bảo mật chỉ là placeholder chưa được nối vào UI** **→ Đã sửa** | Mọi log mặc định `isDirty = true` và `SyncWorker` đẩy **tất cả** log chưa đồng bộ lên đám mây ngay khi có phiên đăng nhập ⇒ ghi nhật ký ở **chế độ khách** rồi đăng nhập là **toàn bộ lịch sử** bị đẩy lên. Nay: `domain/model/CloudSyncSettings.kt` + khoá `cloud_sync_enabled`/`cloud_sync_consent_at` trong `app_settings` (**mặc định TẮT**), `domain/usecase/sync/CloudSyncPolicy.kt` (hàm thuần — **một chỗ duy nhất** quyết định log nào được tải lên; TẮT/thiếu mốc đồng ý → không tải gì; đã bật → chỉ `loggedAt >= consentAtMillis`), `worker/SyncWorker.kt` (gọi policy trước khi upload), `presentation/screen/home/{CloudSyncCard,HomeViewModel,HomeScreen}.kt` (công tắc mặc định TẮT + dòng nhắc cho chế độ khách + nút mở chính sách), `presentation/screen/privacy/PrivacyPolicyScreen.kt` + `MainActivity` (mở thẳng màn chính sách khi nhận `ACTION_SHOW_PERMISSIONS_RATIONALE`/`ACTION_VIEW_PERMISSION_USAGE`, **không** qua cổng đăng nhập), `presentation/navigation/{Screen,AppNavigation}.kt` (route `privacy_policy`), `firestore.rules` + `firebase.json` (mỗi người dùng chỉ đọc/ghi `users/{uid}/**`, mặc định đóng), `res/values/strings.xml` (bỏ URL placeholder; dùng `health_permissions_rationale`), `app/src/test/.../{domain/usecase/sync/CloudSyncPolicyTest,data/repository/SettingsRepositoryImplTest,presentation/screen/home/HomeViewModelTest}.kt` | **Cao (riêng tư + Play review)** | TC-HOME-16, TC-PRIV-01…TC-PRIV-03 | ✅ Đã sửa |
| 38 | **Chưa phát hành release được**: `release { isMinifyEnabled = true }` nhưng **không có `signingConfig`**, không `isShrinkResources`, `versionCode = 1`; `proguard-rules.pro` **thiếu keep cho Firebase & Glance** (chỉ có Room/Hilt/Health Connect/ML Kit/SQLCipher) và thiếu `-keepattributes SourceFile,LineNumberTable` **→ KHÔNG sửa (đã chốt với team)** | `app/build.gradle.kts:24-31`, `app/proguard-rules.pro` | Trung bình (chặn release) | — | ⏭️ Không sửa — app **làm thử nghiệm, không phát hành**: không cần keystore/ký release. Khi nào phát hành thật thì làm cùng lúc: `signingConfig` đọc từ file khoá **không commit**, `isShrinkResources`, và keep cho Firebase (`com.google.firebase.**`, `com.google.android.gms.**`) + Glance (`androidx.glance.**`) |
| 39 | ~~**`catch (e: Exception)` trong `suspend fun` nuốt `CancellationException`** (14 vị trí) ⇒ huỷ scope/stop worker bị biến thành "0/thành công", coroutine không kết thúc đúng; thêm 24 chỗ `runCatching`~~ **→ Đã sửa** | Nay **mọi** chỗ bắt lỗi trong hàm `suspend`/worker có `catch (e: CancellationException) { throw e }` **trước** `catch (e: Exception)`: `HealthConnectDataSourceImpl.kt` (6 chỗ: `hasAllPermissions`, `getTodaySteps`, `getTodaySleepHours`, `getTodayHeartRate`, `getTodayActiveCalories`, `getSleepStageBreakdown`), `HealthSyncWorker.kt`, `SleepTrackingWorker.kt`, `SyncWorker.kt`, `CameraViewModel.kt`, `GalleryImageLoader.kt`, `LegacyDatabaseKeyMigration.kt`, `CameraScreen.kt`; `HeartRateScreen.kt` **đã đúng sẵn** (đã có nhánh rethrow). Thêm **`util/RunCatchingCancellable.kt`** (`inline fun runCatchingCancellable`) — không nuốt `CancellationException` — và dùng thay `runCatching` ở **18 chỗ** bọc lời gọi `suspend`/I/O: `AuthRepositoryImpl` (3), `FirestoreSyncDataSource`, `HealthDataRepositoryImpl` (2), `HabitWidgetRefresherImpl`, `GetHealthDataUseCase`, `GetHealthConnectPermissionStatusUseCase` (3), `GetLatestRecordedHeartRateUseCase`, `InsightsViewModel`, `HabitsViewModel`, `AddEditHabitViewModel` (2), `HomeViewModel`. Chỗ `runCatching` chỉ bọc việc **thuần bộ nhớ** (`HabitCategory.valueOf`, `Context.startActivity`) **giữ nguyên**. (`HabitWidgetRefresherImpl` trong danh sách này nay là `HealthWidgetRefresherImpl` — widget đã đổi thành "Sức khỏe hôm nay".) Test: `GetHealthDataUseCase`/`HealthDataRepositoryImpl`… vẫn xanh (380 test pass) | Trung bình | TC-WM-17 | ✅ Đã sửa (mục 3 #36) |
| 40 | **Không có icon launcher** (`android:icon="@android:drawable/sym_def_app_icon"`, `res/` **không có** `drawable`/`mipmap`) và **`allowBackup="true"` nhưng không có `dataExtractionRules`/`fullBackupContent`**: DB SQLCipher + prefs bị auto-backup lên Google Drive trong khi **khoá SQLCipher nằm ở Android Keystore (không backup được)** ⇒ máy mới restore mở DB thất bại và rơi vào nhánh **xoá DB** (`LegacyDatabaseKeyMigration`) = mất sạch dữ liệu **→ Đã sửa** | Nay: icon adaptive **chỉ bằng vector + XML** — `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (nền `@color/ic_launcher_background`, hình `@drawable/ic_launcher_foreground`), `res/drawable/ic_launcher_foreground.xml` (trái tim trắng, vẽ trong khung 108x108, hình nằm gọn vùng an toàn 72x72), `res/values/colors.xml` (`ic_launcher_background = #2E7D32`); manifest đổi `android:icon`/`android:roundIcon` sang `@mipmap/ic_launcher(-round)`; và `android:allowBackup="false"` kèm chú thích lý do (không còn bản sao DB/prefs rò rỉ hay "restore" xoá dữ liệu). `minSdk = 26` nên mọi máy đều hỗ trợ adaptive icon, không cần PNG dự phòng | **Cao (mất dữ liệu)** | TC-ENV-07 | ✅ Đã sửa |
| 41 | ~~**Không có tầng i18n**: `res/values/strings.xml` chỉ có **3 string**, số lần dùng `stringResource` = **0**, ~**38** chuỗi `Text("...")` hardcode trong screens + nhãn widget (`AndroidManifest.xml:98`) + `contentDescription`; không có `values-<locale>`~~ **→ Đã sửa** (chuỗi tách ra tài nguyên, **chữ hiển thị không đổi**) | `res/values/strings.xml` nay có **221 `<string>`** (trước là 2 chuỗi dùng thật); **~215** chỗ đọc tài nguyên: `stringResource(...)` trong Compose, `context.getString(...)` ở Glance widget (`HealthWidget` + `androidx.glance.LocalContext`), `ReminderWorker` (tên/mô tả kênh + tiêu đề/nội dung thông báo) và **Toast trong `LaunchedEffect`** (`HabitsScreen`, `AddEditHabitScreen`); `AndroidManifest` dùng `@string/widget_label`. Nhãn bottom bar: `Screen.title` → **`Screen.titleRes: @StringRes`** + `R.string.nav_*` (đọc trong `AppNavigation`). Bộ lọc màn Thói quen: chuỗi `"Tất cả"/"Sáng"/"Chiều"/"Tối"` (vừa là khoá so sánh trong `HabitsViewModel` vừa là nhãn) → **enum `HabitFilter`** (`labelRes` + `timeOfDay`) — xem `presentation/screen/habits/HabitFilter.kt`. Chuỗi có giá trị động dùng tham số định dạng (`%1$s`, `%1$d`, `%%` trong `heart_rate_quality`). **Còn lại (chưa thuộc phạm vi mục này):** chuỗi **sinh trong ViewModel** (`LoginViewModel`, `HabitLogInput`, `CameraViewModel`, `HabitRateViewModel`, `InsightsViewModel`…), nội dung gợi ý AI (`SuggestionCatalog`), nhãn trong **domain model** (`HabitCategory.displayName`, `HabitTimeOfDay.displayName`, `Frequency`, `SleepQuality.label`, `HeartRateZone.displayName`), nhãn tuần `T2…CN`, hàm định dạng số (`TimeDisplay`, `HabitProgressText`) và dữ liệu mẫu `@Preview`; chưa có `values-<locale>` nào | Trung bình | TC-ENV-08 | ✅ Đã sửa (mục 3 #37) |
| 42 | ~~**Hợp đồng repository sai**: `HealthDataRepositoryImpl.getHealthData(start, end)` **bỏ qua cả `start` và `end`** (luôn trả số hôm nay) trong khi `GetHealthDataUseCase` truyền `Instant.now()` hai lần; **`GetHealthInsightsUseCase:31` dùng `getRecentHealthHistoryFlow(7)`** (cắt theo **số hàng**) trong khi thẻ lịch sử dùng `getHealthHistorySince` (cắt theo **mốc thời gian**) — dữ liệu cũ có nhiều hàng/ngày sẽ làm rule "3 ngày liên tiếp" **âm thầm không chạy**~~ **→ Đã sửa** | (a) `HealthDataRepository.getHealthData(start, end)` → **`getTodayHealthData()`** (không tham số): tên hàm nói đúng việc nó làm, `GetHealthDataUseCase` gọi thẳng; dữ liệu ngày đã qua đọc qua `health_data_history` (`getHealthHistorySince`). (b) `GetHealthInsightsUseCase` đọc lịch sử sức khỏe bằng **`getHealthHistorySince(đầu ngày cách đây 6 ngày)`** — **cùng cửa sổ 7 ngày** với thẻ "Lịch sử sức khỏe 7 ngày", nên Rule Engine luôn thấy đủ ngày cho luật "3 ngày liên tiếp". Cập nhật test: `HealthDataRepositoryImplTest` (`getTodayHealthData returns correct data from data source`) | Trung bình | TC-INS-18, TC-INS-19, TC-INS-30, TC-INS-31 | ✅ Đã sửa (mục 3 #38) |
| 43 | ~~**Thiếu test & nuốt lỗi ở tab Phân tích**: `InsightsViewModel` **không có unit test** (8/9 ViewModel còn lại đều có) và `.catch { // Log error }` nuốt ngoại lệ trong khi `InsightsUiState` **không có** trường `error` ⇒ flow lỗi thì màn im lặng ở trạng thái rỗng; `HealthSyncWorker` **luôn** trả `success` (`onFailure` chỉ log) nên backoff 3 lần là code chết~~ **→ Đã sửa** | (1) `InsightsUiState` có **`error`** (lỗi luồng dữ liệu chính) + **`sleepStagesError`** (lỗi đọc chi tiết pha giấc ngủ) + **`visibleError`**; `catch` ghi log, đặt `error` và **ném tiếp `CancellationException`**; `InsightsScreen` hiện **`ErrorStateCard`** có nút đóng (`dismissError()`). (2) `HealthSyncWorker.doWork()` trả **`retry`** khi `syncHealthData()` thất bại (tối đa **3** lần) rồi **`failure`** — backoff đã cấu hình (30 phút, exponential) **thật sự chạy**; việc worker bị dừng (`CancellationException`) **không** bị biến thành "thử lại/thất bại". (3) Thêm **`app/src/test/.../presentation/screen/insights/InsightsViewModelTest.kt`** (**6 test**: mọi nguồn vào UI state, `isLoading` tới snapshot đầu, luồng lỗi → `error` (không nuốt), `dismissError`, chi tiết giấc ngủ đọc lúc mở **và** khi `refresh` (**exactly 2**), lỗi đọc chi tiết giấc ngủ → `sleepStagesError` mà **không** lẫn với `error`) | Trung bình | TC-INS-39, TC-WM-17 | ✅ Đã sửa (mục 3 #39) |
| 44 | ~~**Xin thừa quyền Health Connect**: manifest khai `READ_STEPS`+`WRITE_STEPS`, `READ_SLEEP`+`WRITE_SLEEP` nhưng app **chỉ đọc** ⇒ hộp thoại quyền hỏi nhiều hơn mức cần~~ **→ Đã sửa** | `app/src/main/AndroidManifest.xml` nay **chỉ khai 4 quyền ĐỌC** (`READ_STEPS`, `READ_SLEEP`, `READ_HEART_RATE`, `READ_ACTIVE_CALORIES_BURNED`) kèm chú thích lý do; khớp đúng `HealthConnectDataSourceImpl.requiredPermissions` (4 quyền đọc) | Thấp | TC-HC-13 | ✅ Đã sửa (mục 3 #40) |
| 45 | **`SCHEDULE_EXACT_ALARM` khai trong manifest nhưng KHÔNG dùng** — không có chỗ nào gọi `AlarmManager`/`setExact*`/`canScheduleExactAlarms`; WorkManager 2.10 **không** khai quyền này (cùng loại với #44) | `app/src/main/AndroidManifest.xml:27` | Thấp | — | ⏭️ Ghi nhận — **chốt để nguyên** trước bản build hiện tại (hậu quả: hộp thoại "Alarms & reminders" của Android 13+ có thể xin thừa quyền; không ảnh hưởng build/hành vi). Ai dọn sau: xoá 1 dòng `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` |
| 46 | **`RECEIVE_BOOT_COMPLETED` trùng khai báo của WorkManager** (chính `work-runtime-2.10.0.aar` đã khai quyền này) ⇒ dòng trong manifest của app là **thừa** (vô hại, dùng cho `RescheduleReceiver` sau reboot — TC-WM-12) | `app/src/main/AndroidManifest.xml:26` | Thấp | TC-WM-12 | ⏭️ Ghi nhận — để nguyên (bỏ đi cũng không đổi hành vi, nhưng không cần cho bản build này) |
| 47 | **API chết sau khi sửa #42**: `HealthDataRepository.getRecentHealthHistory(limit)` và `getRecentHealthHistoryFlow(limit)` (cắt theo **số hàng**) nay **chỉ còn unit test dùng** — code sản phẩm đã chuyển hết sang `getHealthHistorySince` (cắt theo **mốc thời gian**); thêm `InsightsUiState.lastAnalyzedDays` không được đọc ở đâu | `domain/repository/HealthDataRepository.kt:58,86`, `data/repository/HealthDataRepositoryImpl.kt:150,175`, `presentation/screen/insights/InsightsViewModel.kt:42` | Thấp | — | ⏭️ Ghi nhận — không xoá vội (giữ làm API đọc lịch sử theo số hàng nếu sau này cần; `lastAnalyzedDays` giữ cho UI state ổn định) |
| 27 | **Feed "BLOOM AI GỢI Ý" quá tập trung vào 1 thứ — không xử lý xong thì thẻ đó lặp mãi** **→ Đã sửa** | Trước đây mỗi điều kiện (thiếu ngủ/uống ít nước/ít vận động/nhịp tim cao) sinh **một thẻ cảnh báo cố định**; điều kiện còn đúng thì thẻ còn nguyên ⇒ người dùng thấy **cùng một** gợi ý mãi, mang cảm giác bị ép buộc. Nay `domain/usecase/SuggestionCatalog.kt` (object thuần Kotlin) giữ **kho mẹo theo danh mục** — mỗi danh mục có nhóm **CONCERN** (khi dữ liệu cho thấy đang có vấn đề) và **GENERAL** (khi đang ổn) — và `HabitRuleEngine.generateSuggestions(activeHabits, recentLogs, healthHistory, rotationSeed)` chọn **một mẹo xoay vòng theo ngày** (`rotationSeed = LocalDate.now().dayOfYear`, ổn định trong cùng ngày) cho **từng danh mục người dùng có thói quen** (cộng danh mục đang có vấn đề): **tối đa 4 thẻ**, sắp xếp theo mức ưu tiên, `id` của thẻ **duy nhất** (không trùng key `LazyColumn`). **Không còn thẻ `CRITICAL`/nhãn "CẢNH BÁO KHẨN CẤP" cố định**. `GetHealthInsightsUseCase` truyền thêm `getAllActiveHabits()`. Chi tiết: TC-INS-01…TC-INS-11, TC-INS-34 | ✅ Đã sửa |
| 28 | **"Chỉ số sinh lực" không đánh giá được gì: vào app = 50, xong 1 thói quen = 73, rồi 3–4 ngày không làm gì vẫn 73** **→ Đã sửa** | Nguyên nhân: điểm cũ lấy **trung bình cả 7 ngày** với danh mục không có log tính **mặc định 70** và không có log nào = **50**, nên bỏ thói quen vài ngày điểm **không đổi** và còn bị "thổi" bởi hằng số. Nay `domain/usecase/health/VitalityScoreMath.kt` (hàm thuần Kotlin) tính **theo từng ngày** trong 7 ngày gần nhất rồi lấy trung bình: mỗi ngày = `0,4 × tỉ lệ thói quen đạt mục tiêu + 0,3 × giờ ngủ/8 + 0,3 × bước chân/8000`, trọng số **chuẩn hoá lại** theo thành phần **thật sự có nguồn** trên máy; **ngày không ghi gì đóng góp 0** nên bỏ thói quen là điểm **tụt**. Không có nguồn nào → `hasData = false` (UI hiện **"Chưa đủ dữ liệu để đánh giá"** thay vì `50`); vòng tròn nay đổi **màu theo dải điểm** (≥70 xanh, 40–69 cam, <40 đỏ). `GetVitalityScoreUseCase` nay đọc thêm `HealthDataRepository.getHealthHistorySince()`, `VitalityScore` có thêm `hasData`, `delta` chỉ hiện khi `VitalityScoreMath.hasObservations()` của tuần trước = true. Chi tiết: TC-INS-12…TC-INS-17, TC-INS-25, TC-INS-26, TC-INS-35 | ✅ Đã sửa |
| 29 | **"Xu hướng chất lượng giấc ngủ" luôn "đợi" dữ liệu pha Sâu/Nông/REM từ Health Connect nên máy thường không hiện gì** **→ Đã sửa** | Trước đây thẻ chỉ vẽ 3 thanh pha đọc từ `SleepSessionRecord.stages`; máy không có Health Connect (hoặc thiết bị không chia pha) ⇒ thẻ trống. Nay `domain/model/SleepTrend.kt` + `domain/usecase/health/SleepTrendMath.kt` dựng **biểu đồ 7 đêm giờ ngủ** từ `health_data_history` (**cùng nguồn** với thẻ "Lịch sử sức khỏe 7 ngày", **gồm cả giờ ngủ do app tự ước lượng từ cảm biến** — nên hoạt động trên mọi máy) kèm **trung bình mỗi đêm + xếp loại** (*Thiếu ngủ/Chưa đủ/Tốt/Hơi dài*). `InsightsViewModel` thêm `sleepTrend` vào UI state (Flow nên tự cập nhật, không cần mở lại màn); khối **Sâu/Nông/REM** chỉ hiện **thêm** khi Health Connect có dữ liệu pha. Chi tiết: TC-INS-23, TC-INS-24, TC-INS-36 | ✅ Đã sửa |
| 30 | **`HabitFrequency` "Hàng tuần"/"Hàng tháng" chọn được ở màn Thêm/Sửa nhưng không có tác dụng gì** **→ Đã sửa** | Enum + cột `habits.frequency` tồn tại và UI cho chọn đủ 3 giá trị (`AddEditHabitScreen`), nhưng **không logic nào đọc** `frequency`: `GetTodayHabitsUseCase`, `GetWeeklyConsistencyUseCase`, `HomeViewModel.observeHabits`, `HabitsViewModel`, `VitalityScoreMath.compute` và `TodayHabitProgress.build` đều xét mọi thói quen theo **NGÀY** ⇒ chọn "Hàng tuần" chỉ đổi nhãn, gây hiểu nhầm. Nay màn Thêm/Sửa **chỉ hiện "Hàng ngày"** (radio không bấm được + dòng chú thích), `AddEditHabitViewModel` **bỏ** trường `frequency` khỏi UI state và luôn lưu `HabitFrequency.DAILY` — nhờ vậy thói quen cũ lỡ lưu `WEEKLY`/`MONTHLY` được **chuẩn hoá** khi bấm "Cập nhật". Enum và cột DB **giữ nguyên** nên **không** cần migration; khi nào có logic theo chu kỳ thì mở lại cùng lúc với các use case. Chi tiết: TC-ADD-20, TC-ADD-28 | ✅ Đã sửa |
| 31 | **Nhật ký thói quen bị đẩy lên Firestore không có sự đồng ý; thiếu `firestore.rules`; chính sách bảo mật chỉ là placeholder chưa nối vào UI** **→ Đã sửa** (phụ lục C #37) | Mọi log mặc định `isDirty = true` và `SyncWorker` đẩy **tất cả** log chưa đồng bộ lên đám mây ngay khi có phiên đăng nhập ⇒ người dùng ghi nhật ký ở **chế độ khách** rồi đăng nhập bị đẩy **toàn bộ lịch sử** (gồm cả trường `note` tự do) mà không được hỏi; repo **không có** file rules nào; `health_privacy_policy_url` là `https://your-privacy-policy.com` và `health_permissions_rationale` **không được dùng ở đâu** dù manifest đã có hai `intent-filter` rationale của Health Connect. Nay: (1) `domain/model/CloudSyncSettings.kt` + khoá `cloud_sync_enabled`/`cloud_sync_consent_at` trong bảng `app_settings` — **mặc định TẮT**; (2) `domain/usecase/sync/CloudSyncPolicy.kt` (hàm thuần) là **một chỗ duy nhất** quyết định log nào được tải lên: TẮT → không tải gì, thiếu mốc đồng ý → không tải gì, đã bật → **chỉ** log có `loggedAt >= consentAtMillis`; (3) `SyncWorker` gọi policy trước khi upload; (4) thẻ **"Đồng bộ đám mây"** ở Trang chủ (`CloudSyncCard` + `HomeViewModel.setCloudSyncEnabled`) có công tắc mặc định TẮT, dòng nhắc riêng cho chế độ khách, và nút mở chính sách; (5) `presentation/screen/privacy/PrivacyPolicyScreen.kt` + `MainActivity` mở thẳng màn này khi nhận `ACTION_SHOW_PERMISSIONS_RATIONALE`/`ACTION_VIEW_PERMISSION_USAGE` (**không** qua cổng đăng nhập — điều kiện để Health Connect/Play duyệt app); (6) thêm `firestore.rules` + `firebase.json` (mỗi người dùng chỉ đọc/ghi `users/{uid}/**`, mặc định đóng). Tắt công tắc = **ngừng** tải lên, **không** xoá dữ liệu đã tải (đã chốt). Chi tiết: TC-HOME-16, TC-PRIV-01…TC-PRIV-03 | ✅ Đã sửa |
| 32 | **Không có icon launcher riêng và `allowBackup="true"` không kèm rules ⇒ restore làm mất sạch DB** **→ Đã sửa** (phụ lục C #40) | Manifest trỏ icon vào `@android:drawable/sym_def_app_icon` (**icon mặc định của Android**) và `res/` **không có** `mipmap`/`drawable` nào; đồng thời `android:allowBackup="true"` mà **không** có `dataExtractionRules`/`fullBackupContent` ⇒ DB SQLCipher + prefs bị auto-backup lên Google Drive, trong khi **khoá SQLCipher nằm ở Android Keystore (không backup được)** nên restore trên máy mới mở DB thất bại và rơi vào nhánh **xoá DB** (`LegacyDatabaseKeyMigration`) = mất sạch dữ liệu (và rò rỉ dữ liệu sức khỏe vào backup). Nay: adaptive icon **chỉ bằng vector + XML** (`res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/values/colors.xml` — nền xanh `#2E7D32` + trái tim trắng; `minSdk = 26` nên không cần PNG dự phòng) và `android:allowBackup="false"` (kèm chú thích lý do trong manifest). Chi tiết: TC-ENV-07 | ✅ Đã sửa |
| 48 | ~~Feed **"BLOOM AI GỢI Ý"** ở tab Phân tích trình bày bằng **danh sách thẻ xếp dọc** (`LazyColumn.items`) — 4 thẻ liền nhau (mỗi thẻ tiêu đề + nội dung + nút) làm màn hình **quá dài** và nặng~~ **→ Đã sửa** (mục 3 #41) | `presentation/screen/insights/InsightsScreen.kt`: thêm **`AiTipBanner`** — `HorizontalPager` + `rememberPagerState(pageCount = { Int.MAX_VALUE })` (lấy `page % suggestions.size` để **luôn trượt tới trước**), tự cuộn mỗi **5 giây** (`AI_BANNER_AUTO_SCROLL_MS`, **đứng ngoài** khi `pagerState.isScrollInProgress`), banner cao **cố định** `AI_BANNER_HEIGHT = 240.dp`, **chấm chỉ báo** trang; `AiTipCard` đổi `fillMaxWidth()` → `fillMaxSize()` + `maxLines`/`TextOverflow.Ellipsis` + ghim nút ở đáy (`Spacer(weight(1f))`); bỏ import `androidx.compose.foundation.lazy.items`; thêm `AI_BANNER_HEIGHT`/`AI_BANNER_AUTO_SCROLL_MS`. Bổ sung test JVM trong `app/src/test/.../domain/usecase/HabitRuleEngineTest.kt` (**+2 test**: vòng xoay hết kho mẹo mới lặp lại; mỗi danh mục phủ hết kho `GENERAL`) | Thấp | TC-INS-40, TC-INS-41 | ✅ Đã sửa (mục 3 #41) |
| 49 | ~~**Màn "Ghi chép bữa ăn" không nhận diện được *món gì***: dùng `ImageLabeling` **mặc định** của ML Kit — bộ gán **nhãn ảnh chung** (~400 nhãn tiếng Anh, **không** phải model món ăn) — nên chụp **đĩa cơm** có lúc ra **"Pizza"**; nâng/hạ ngưỡng tin cậy **không** sửa được vì lỗi nằm ở model. Và kể cả khi nhãn đúng thì kết quả cũng chỉ có **tên món**, **không** trả lời được *"ăn vào lợi/hại gì cho cơ thể"*~~ **→ Đã sửa** (mục 3 #42) | **Bỏ hẳn ML Kit**: xoá `data/vision/ImageLabelSource.kt`, `data/vision/MlKitImageLabelSource.kt`, `domain/usecase/food/FoodRecognition.kt` (+ `FoodRecognitionTest` 15 test) và gỡ dependency `mlkit-image-labeling`/`camerax-mlkit-vision`. Thay bằng **AI (Gemini)**: `domain/model/FoodInsight.kt` (`isFood`, `dishName`, `emoji`, `benefits`, `cautions`), `domain/usecase/food/FoodAnalysis.kt` (**`buildPrompt()`** — tiếng Việt, ưu tiên món Việt/Á, phủ trái cây/đồ uống/đồ ăn vặt, bắt ghi **"(có thể)"** khi không chắc, **cấm** chẩn đoán/chữa bệnh; **`toInsight()`** — làm sạch: ≤3 gạch/mục, ≤120 ký tự/gạch, bỏ mục rỗng/trùng, emoji mặc định 🍽️), `data/network/HttpPost.kt` + `HttpUrlConnectionPost.kt` (cổng HTTP, không thêm OkHttp), `data/vision/FoodInsightSource.kt` + `GeminiFoodInsightSource.kt` (`generateContent` + `x-goog-api-key` + `inline_data` + `responseMimeType`/`responseSchema`; ánh xạ **401/403→AUTH**, **429/503→BUSY**, **5xx→SERVER**, **404/400→BAD_RESPONSE**; **thử lại 1 lần** với 429/503/5xx/mất mạng), `data/vision/ImageJpegEncoder.kt` (nén JPEG ≤1280 px, chất lượng 80), `di/AiModule.kt` (`BuildConfig.GEMINI_API_KEY`/`GEMINI_MODEL` từ `local.properties` đã gitignore), `domain/repository/SettingsRepository.kt` + `SettingsRepositoryImpl` (**cổng opt-in `food_ai_enabled`, mặc định TẮT**), `presentation/screen/camera/{CameraViewModel,CameraScreen}.kt` (thẻ cổng đồng ý + thẻ kết quả **tên món · Có lợi cho cơ thể · Nên lưu ý · miễn trừ trách nhiệm** + nút **Thử lại**), `app/src/main/res/values/strings.xml` (`privacy_camera_body` viết lại: **ảnh chỉ được gửi khi người dùng tự bật**), `app/build.gradle.kts` (`buildConfig = true`, Gson `2.10.1`), `app/src/test/.../domain/usecase/food/FoodAnalysisTest.kt` (17 test), `app/src/test/.../data/vision/GeminiFoodInsightSourceTest.kt` (20 test), `app/src/test/.../presentation/screen/camera/CameraViewModelTest.kt` (9→17 test), `SettingsRepositoryImplTest` (12→16 test) | **Cao** | TC-CAM-05…TC-CAM-24, TC-PRIV-04, TC-REG-30 | ✅ Đã sửa (mục 3 #42) |

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
- **Kết quả mong đợi:** Nút hiện spinner khi đang gửi; khi Firebase trả về user → màn đăng nhập **tự động** thay bằng màn Home (không cần bấm thêm, nhờ `AuthGateViewModel` quan sát `AuthRepository.authState`). Lời chào ở màn Trang chủ hiện **tên tài khoản vừa đăng nhập** (`displayName`, nếu trống thì phần trước `@` của email) — **không** còn là "Alex" cứng (phụ lục C #30, TC-HOME-12).

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

1. `pm clear com.healthyai.app` → mở app (**TC-INIT-01**) → ở màn đăng nhập bấm **"Bỏ qua, dùng ở chế độ khách"** (**TC-AUTH-01**, **TC-AUTH-02**) → ở Trang chủ xác nhận lời chào **không kèm tên** (không còn "Alex" cứng — **TC-HOME-12**).
2. Bấm lần lượt 4 tab, xác nhận không crash (**TC-ENV-05**, **TC-NAV-01**). Ở Trang chủ, nếu thấy thẻ **Cấp quyền Health Connect** → bấm và bật đủ 4 quyền, xác nhận thẻ biến mất và số liệu được nạp lại (**TC-HC-13**).
3. Tab Thói quen → FAB `+` → tạo "Uống nước / 2000 ml / Cả ngày / Hàng ngày" (**TC-ADD-03**).
4. Tạo thêm "Ngủ đủ giấc / 8 giờ / Tối", "Chạy bộ / 30 phút / Sáng" (**TC-ADD-02**, **TC-ADD-03**).
5. Ghi 1000 ml cho Uống nước → kiểm tra tiến độ "1.0L / 2.0L", Toast **"Đã ghi nhận: Uống nước"** và ô nhập mặc định là **`2000`** (không phải `2000.0`) (**TC-HAB-05**, **TC-HAB-24**, **TC-HAB-26**).
6. Ghi tiếp 1000 ml → kiểm tra trạng thái "Hoàn thành" (**TC-HAB-07**).
7. Vào sửa "Chạy bộ", kiểm tra ô mục tiêu điền sẵn **`30`** (không còn đuôi `.0` — **TC-ADD-23**), đổi mục tiêu thành 45, lưu (**TC-ADD-09**).
8. Mở tab Phân tích: kiểm tra điểm sinh lực, thẻ gợi ý nước, biểu đồ nhất quán tuần (cột hôm nay có dữ liệu) (**TC-INS-13**, **TC-INS-02**, **TC-INS-21**). Bấm nút trong thẻ gợi ý **"Ghi nhận uống nước"** → xác nhận app nhảy sang tab Thói quen với hộp thoại ghi nhận mở sẵn (**TC-INS-11**). Ghi 5 log `10` phút cho **"Chạy bộ / 30 phút"** → quay lại tab Phân tích, xác nhận gợi ý vận động nói **theo phút** (không phải "10 bước") (**TC-INS-29**).
9. Mở tab Ghi chép: cấp quyền Camera, quan sát preview (**TC-CAM-01**, **TC-CAM-04**) → chụp một món ăn → chờ thẻ "Kết quả nhận diện" (**tên món hiện bằng tiếng Việt kèm emoji**) rồi bấm **"Thêm vào nhật ký"** → xác nhận hộp thoại ghi nhận của thói quen **Dinh dưỡng** mở sẵn, lưu được log + Toast "Đã ghi nhận" (**TC-CAM-07**, **TC-CAM-15**).
10. Đóng app hoàn toàn → mở lại → kiểm tra dữ liệu còn nguyên (**TC-DB-03**) và ghi nhận hành vi tiến độ (**TC-HAB-18**).
11. Thêm widget **"Sức khỏe hôm nay"** ra màn hình chính (**TC-WG-01**) → xác nhận đủ **3 cột** bước chân / giấc ngủ / nhịp tim và ba số **khớp** thẻ "Tóm tắt sức khỏe" ở Trang chủ (**TC-WG-03**) → vào ô **"Tim"** đo một lần rồi quay lại màn hình chính, xác nhận ô **"Nhịp tim"** đổi ngay mà **không** cần gỡ/thêm lại widget (**TC-WG-04**, **TC-WG-10**) → tạo thêm 1 thói quen mới và xác nhận widget **không đổi** — đúng thiết kế (**TC-WG-13**).
12. Run `daily_reminder_worker` bằng Background Task Inspector (**TC-WM-04**) — nếu máy Android 13+ và chưa cấp quyền thông báo, bấm **"Bật thông báo"** ở thẻ trên Home trước (**TC-WM-13**).
13. Ở **Trang chủ** bấm icon **Đăng xuất** (góc phải lời chào) → **Đăng xuất** → xác nhận app quay về màn đăng nhập (**TC-AUTH-15**) → bấm **"Bỏ qua, dùng ở chế độ khách"** → mở tab Thói quen, xác nhận dữ liệu vẫn còn nguyên, và **lời chào không kèm tên** vì đang ở chế độ khách (**TC-HOME-12**).
14. **Bước thêm cho bản này — nâng cấp không mất dữ liệu:** cài **đè** APK mới lên bản cũ (không `pm clear`) rồi mở app → thói quen/nhật ký và tiến độ hôm nay phải còn nguyên (**TC-DB-15**, **TC-REG-22**) và bảng `app_settings` được tạo mới, thẻ "Khung giờ ngủ" hiện mặc định 23:00 – 07:00 (**TC-DB-17**). Trước khi phát hành, chạy `.\gradlew test` và xác nhận toàn bộ test pass (TC-ENV-03: **40 class / 418 test**), trong đó có `DatabaseMigrationsTest`.
15. **Bước thêm cho bản này — chính sách bảo mật của phần AI:** mở tab **Phân tích** → cuộn xuống **cuối màn** → thẻ **"AI & quyền riêng tư"** → bấm **"Xem chính sách bảo mật"** → đọc mục *"3. Camera và ảnh món ăn"*: phải nói **ảnh được gửi tới Google Gemini khi bạn bấm "Phân tích món ăn"** và **không** còn câu về công tắc "mặc định TẮT" (**TC-INS-42**, **TC-PRIV-04**) → quay lại đúng tab Phân tích. Kiểm tra tab **Ghi chép**: **không** còn thẻ công tắc AI, **không** còn 2 dòng *"Đang BẬT/TẮT…"*, **không** còn nút "Xem chính sách bảo mật" (**TC-CAM-21** đã bỏ).
16. **Bước thêm cho bản này — phân tích món ăn bằng AI (nhanh, có hộp thoại):** chụp **một đĩa cơm/món Việt** → **bấm giờ**: **hộp thoại "Đang phân tích món ăn"** phải hiện **ngay** (vòng xoay + mô tả), **không** tắt được bằng **Back** hay **chạm ra ngoài**, rồi **tự đóng** khi có kết quả (**TC-CAM-25**) → thời gian chờ phải **ngắn hơn hẳn** bản cũ (model `gemini-3.8-flash` + `thinkingLevel: low` — **TC-CAM-26**) → thẻ **"Kết quả phân tích món ăn"** hiện **tên món tiếng Việt** + **"Có lợi cho cơ thể"** + **"Nên lưu ý"** + dòng miễn trừ *"… KHÔNG phải tư vấn y tế"* (**TC-CAM-07**) — **đặc biệt: đĩa cơm KHÔNG còn ra "Pizza"** như bản dùng ML Kit cũ (**TC-CAM-22**, **TC-REG-30**) → chụp thêm **trái cây**, **đồ uống**, **đồ ăn vặt** → đều ra tên tiếng Việt (**TC-CAM-22**) → chụp **một thứ không phải đồ ăn** (bàn trống, bầu trời) → ra thẻ **"Chưa nhận diện được món ăn"** (**TC-CAM-08**) → **bật chế độ máy bay** rồi chụp lại → thẻ lỗi *"Không kết nối được tới AI…"* + nút **"Thử lại"**; tắt máy bay và bấm **"Thử lại"** → ra kết quả mà **không** phải chụp lại (**TC-CAM-23**). Sau đó xem `adb logcat -s GeminiFoodInsight` để xác nhận request đi đúng **model chính** và khi Google quá tải thì **tự chuyển sang model dự phòng** (**TC-CAM-26**).
17. **Bước thêm cho bản này — ảnh có sẵn trong thư viện:** ở tab **Ghi chép** bấm **"Chọn ảnh từ thư viện"** → chọn **một ảnh món ăn đã chụp trước đó (nên chọn ảnh chụp dọc)** → app phân tích ngay và ra **tên món tiếng Việt** (ảnh dọc **không** bị quay 90°) (**TC-CAM-18**) → bấm lại nút này rồi **Huỷ** trong photo picker → không có thẻ lỗi nào xuất hiện (**TC-CAM-20**) → chọn một ảnh **không phải đồ ăn** (ảnh chụp màn hình) → ra thẻ **"Chưa nhận diện được món ăn"** (**TC-CAM-19**). Kiểm tra nhanh `pm clear` + **không** cấp quyền Camera: nút thư viện vẫn dùng được (**TC-CAM-01**, **TC-CAM-20**).
18. **Bước thêm cho bản này — lời chào đúng người dùng:** đăng nhập bằng 1 tài khoản thật (email/mật khẩu) → màn Trang chủ phải chào **tên tài khoản** (`displayName`, nếu trống thì phần trước `@` của email), không còn "Alex" cứng (**TC-HOME-12**).
19. **Bước thêm cho bản này — khung giờ ngủ:** ở Trang chủ, thẻ **"Khung giờ ngủ"** nằm ngay dưới thẻ "Tóm tắt sức khỏe" và hiện **23:00 – 07:00** (**TC-SLEEP-07**) → bấm **"Đổi khung giờ ngủ"** → đặt **22:30 / 06:15** → **Lưu** → thẻ cập nhật ngay, `app_settings` có `1350` / `375` (**TC-SLEEP-08**, **TC-SLEEP-12**) → mở lại hộp thoại, đặt **giờ thức trùng giờ đi ngủ** → **Lưu** → hộp thoại **vẫn mở** + lỗi *"Giờ đi ngủ và giờ thức không được trùng nhau"*, DB **không** đổi → bấm **Huỷ** → khung vẫn là 22:30 – 06:15.
20. **Bước thêm cho bản này — feed gợi ý là banner trượt:** tạo **2 thói quen ở 2 danh mục** rồi mở tab **Phân tích** → xác nhận khối **"BLOOM AI GỢI Ý"** chỉ hiện **một gợi ý** trong khối cao **cố định** (~240dp) và **tự trượt** sang gợi ý khác sau ~5 giây (**TC-INS-40**) → **vuốt ngang** trong banner để xem lại gợi ý trước, kiểm tra **chấm chỉ báo** đổi theo → bấm nút hành động ở gợi ý **đang hiện** → nhảy sang tab Thói quen mở sẵn hộp thoại ghi nhận (**TC-INS-11**).
21. **Bước thêm cho bản này — màn Ghi chép bữa ăn gọn hơn & trả lời nhanh hơn (mục 3 #43):** mở tab **Ghi chép** → xác nhận **không** còn thẻ công tắc **"Phân tích món ăn bằng AI"**, **không** còn 2 dòng *"Đang BẬT/TẮT…"* và **không** còn nút "Xem chính sách bảo mật" (**TC-CAM-21** đã bỏ) → bấm **"Phân tích món ăn"** với một món bất kỳ → **hộp thoại "Đang phân tích món ăn"** hiện **ngay**, **không** tắt được bằng **Back**/chạm ra ngoài, **tự đóng** khi có kết quả (**TC-CAM-05**, **TC-CAM-06**, **TC-CAM-25**) → so thời gian chờ với bản trước: phải **nhanh hơn rõ rệt** (**TC-CAM-26**) → vào tab **Phân tích**, cuộn xuống cuối, xác nhận thẻ **"AI & quyền riêng tư"** mở được màn Chính sách bảo mật (**TC-INS-42**, **TC-PRIV-01**).
22. **Bước thêm cho bản này — đếm bước chỉ khi app đang chạy (mục 3 #44):** ở **Trang chủ** bấm **"Bật đếm bước chân"** → **Cho phép** quyền **Nhận diện hoạt động thể chất** (**TC-STEP-01**) → đi bộ (hoặc lắc máy cho cảm biến đếm) ~**300** bước **trong lúc app đang mở** → số ở ô **"Bước chân"** tăng tương ứng (**TC-STEP-02**) → **vuốt app khỏi màn hình tác vụ gần đây** rồi đi thêm ~**500** bước → **mở lại app**: số bước **KHÔNG** tăng thêm 500 và **vẫn giữ** phần đã đếm trước đó (**TC-STEP-04**) → bấm **Home** để app **xuống nền** (đừng vuốt khỏi tác vụ) → đi thêm ~**200** bước → quay lại app: số **tăng** ~200, tức app còn trong tác vụ thì **vẫn đếm** (**TC-STEP-05**).

---

## 24. TC-SLEEP — Số giờ ngủ do app đo trong khung giờ người dùng đặt

> Nguồn "tự lực" thứ hai của app (song song **TC-STEP**): app **tự suy ra** giấc ngủ từ **cảm biến ánh
> sáng + gia tốc + trạng thái màn hình**, **không** cần Health Connect và **không** cần quyền nào.
> **Khung giờ ngủ do người dùng đặt** (mặc định 23:00 → 07:00) — app **không** tự quyết định giờ ngủ nữa.
>
> Nắm trước khi test:
> - `domain/model/SleepSchedule.kt` — khung giờ ngủ do người dùng đặt (nửa mở, có/không vắt qua nửa đêm);
> - `domain/usecase/sleep/SleepTrackerMath.kt` — toàn bộ phép tính (hàm thuần, test được trên JVM);
> - `data/sleep/AndroidSleepSensor.kt` + `data/local/sleep/SleepTrackerStore.kt` — đọc cảm biến và lưu prefs;
> - `data/repository/SettingsRepositoryImpl.kt` + bảng `app_settings` — nơi lưu khung giờ ngủ;
> - `worker/SleepTrackingWorker.kt` — job `periodic_sleep_tracking_worker`, **15 phút/lần**;
> - `presentation/screen/home/SleepScheduleCard.kt` — thẻ **"Khung giờ ngủ"** + hộp thoại đặt giờ.

### TC-SLEEP-01 — Khung giờ ngủ do người dùng đặt (và "đêm" gắn với ngày thức dậy)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew :app:testDebugUnitTest --tests "*SleepScheduleTest" --tests "*SleepTrackerMathTest"` (JDK 21 — xem mục 2.2).
- **Kết quả mong đợi:** Pass và chốt các tính chất của khung giờ:
  - Với khung **23:00 → 07:00** (khung vắt qua nửa đêm): `contains` = `false` cho **22:59**, `true` cho **23:00, 00:00, 06:59**, `false` cho **07:00** (nửa mở — có giờ đi ngủ, **không** có giờ thức) và **12:00**; `wrapsMidnight = true`; độ dài **480 phút**.
  - Với khung **01:00 → 08:00** (không vắt qua nửa đêm): `contains` = `false` cho **00:30** và **20:00**, `true` cho **01:00 … 07:59**; độ dài **420 phút**.
  - Hai mốc **trùng nhau** (ví dụ 07:00 → 07:00) → `isValid = false`, độ dài 24 giờ.
  - **Khoá đêm nhảy tại giờ đi ngủ**: mẫu lúc **22:59 ngày D** thuộc đêm **D**, mẫu lúc **23:00 ngày D** thuộc đêm **D+1**, mẫu lúc **06:00 và 15:00 ngày D+1** vẫn thuộc đêm **D+1**. Khung không vắt (01:00 → 08:00) giữ khoá đêm là **chính ngày đó** suốt ngày.
  - `SleepSchedule.DEFAULT` = **23:00 → 07:00**.
- **Ghi chú:** Nhờ khoá đêm nhảy tại **giờ đi ngủ** (không phải 12:00 như bản trước), số giờ ngủ của đêm vừa qua **không bị xoá** trong ngày — buổi chiều vẫn hiện đúng số của đêm đó.

### TC-SLEEP-02 — Phân loại "đang ngủ": phải đủ CẢ BA tín hiệu VÀ đang trong khung
- **Ưu tiên:** P1
- **Bước:** 1. Chạy `.\gradlew :app:testDebugUnitTest --tests "*SleepTrackerMathTest"`.
- **Kết quả mong đợi:** `isAsleep` = `true` **chỉ khi** (đang trong khung giờ ngủ) **và** (màn hình tắt) **và** (`lightLux < 5`) **và** (`maxMotionG < 0,05`). Từng ca lệch một điều kiện đều cho `false`:
  - màn hình đang dùng (`screenInteractive = true`) dù trời tối và điện thoại nằm yên;
  - phòng còn sáng (`lightLux = 40`) dù điện thoại nằm yên;
  - điện thoại còn rung lắc (`maxMotionG = 0,4`) dù phòng tối;
  - **chưa tới giờ đi ngủ** (22:30) hoặc **buổi chiều** (15:00) dù mọi tín hiệu khác đều đúng.
- **Ghi chú:** Đây là điểm **khác biệt cốt lõi** so với đếm bước: bước chân là số phần cứng đếm sẵn, còn giấc ngủ là **ước lượng** nên phải chặt tay ba lớp để giảm dương tính giả.

### TC-SLEEP-03 — Cộng dồn giữa hai mẫu "đang ngủ", mẫu "thức" cắt cộng dồn
- **Ưu tiên:** P1
- **Bước:** 1. Chạy `.\gradlew :app:testDebugUnitTest --tests "*SleepTrackerMathTest"`.
- **Kết quả mong đợi:**
  - Mẫu "đang ngủ" **đầu tiên** chỉ đánh dấu mốc bắt đầu → **0 phút**;
  - Hai mẫu "đang ngủ" liên tiếp (23:00 → 23:30) → **30 phút**; khoảng cách **dài** (23:00 → 06:00) → **420 phút** (7 giờ) — WorkManager bị hoãn trong Doze **không** làm mất thời gian ngủ;
  - Mẫu "đang thức" ở giữa (23:30) giữ nguyên số đã có và **cắt** cộng dồn; mẫu "ngủ lại" (23:45) về sau chỉ tính **10 phút** (23:45 → 23:55).
- **Ghi chú:** `sample()` chỉ cộng khi **cả** mẫu trước **và** mẫu hiện tại đều là "đang ngủ" — đó là lý do khoảng thức giữa đêm không bị tính.

### TC-SLEEP-04 — Sang đêm mới: về 0 + chốt lại đêm cũ; cắt trần; lọc nhiễu; làm tròn
- **Ưu tiên:** P1
- **Bước:** 1. Chạy `.\gradlew :app:testDebugUnitTest --tests "*SleepTrackerMathTest"`.
- **Kết quả mong đợi:**
  - Ngủ 23:00 → 06:00 cho **420 phút** ở đêm của **ngày thức dậy**; mẫu lúc **23:00 tối hôm sau** → về **0 phút** và đêm vừa xong được **chốt lại** (`archived` = đúng đêm đó, 420 phút);
  - Cảm biến bị kẹt ở trạng thái "đang ngủ" suốt khung → cắt còn đúng **`MAX_SLEEP_MINUTES` = 720 phút** (12 giờ), **không** sinh ra số vô lý;
  - Mẫu có mốc thời gian **cũ hơn** mẫu trước → không bao giờ sinh số âm;
  - **Đổi khung giữa chừng không làm mất số đã đo** (đổi 23:00–07:00 sang 01:00–08:00 lúc 09:00 vẫn giữ 420 phút);
  - `storedSleepMinutes(state, nightIso)` trả **0** khi `nightIso` khác đêm đang xét, và **0** khi chưa có gì trong prefs;
  - `toSleepHours`: **44 phút → `0.0`** (chưa đủ ngưỡng 45 phút), **45 phút → `0.8`**, **435 phút → `7.3`** (làm tròn 1 chữ số).
- **Ghi chú:** Lọc nhiễu để "ngồi yên 20 phút" không bị tính là một giấc ngủ; cắt trần để dữ liệu cảm biến bị kẹt không làm hỏng điểm sinh lực ở tab Phân tích.

### TC-SLEEP-05 — Số phút hiển thị: giữ số đêm vừa qua, không lấy số đêm cũ hơn
- **Ưu tiên:** P1
- **Bước:** 1. Chạy `.\gradlew :app:testDebugUnitTest --tests "*SleepTrackerMathTest"`.
- **Kết quả mong đợi:** `displaySleepMinutes` chốt các tính chất sau:
  - Mẫu lấy **ban ngày** (15:00) không làm mất số của đêm vừa qua → vẫn hiện **420 phút**;
  - **Ngoài khung giờ ngủ**: đêm đang xét chính là đêm vừa kết thúc nên hiển thị **thẳng** số của nó, **kể cả khi dưới ngưỡng** — đêm chỉ ngủ 20 phút thì hiện `20` → `toSleepHours` = `0.0` → ô "Ngủ" là **`0h 0m`** (thức trắng thì phải hiện 0, **không** lấy 8 giờ của đêm hôm kia ra thay);
  - **Trong khung giờ ngủ**, đêm mới mới được 15 phút (< 45) → vẫn hiện **420 phút** của đêm vừa kết thúc (số không nhảy về 0 vào buổi tối); khi đêm mới đã được **80 phút** → hiện **80** (đêm mới thay thế);
  - Đêm đã chốt **cũ hơn đêm vừa qua** → **không** bao giờ được hiện như đêm nay (trả `0`).
- **Ghi chú:** Đây là quy tắc chỉ ảnh hưởng phần **hiển thị**; số đã ghi vào `health_data_history` cũng lấy từ đây (TC-SLEEP-11).

### TC-SLEEP-06 — Job lấy mẫu được đăng ký và chạy khi app đóng
- **Ưu tiên:** P0
- **Tiền điều kiện:** Emulator/thiết bị đang chạy và app đã mở ít nhất một lần.
- **Bước:**
  1. Mở app → mở **App Inspection → Background Task Inspector**.
  2. Tìm `periodic_sleep_tracking_worker` → **Run now**.
  3. Lọc `logcat` theo tag `SleepTrackingWorker`.
- **Kết quả mong đợi:**
  - Thấy job `periodic_sleep_tracking_worker` với **interval 15 phút**, tag `sleep_tracking`, **không** có tiến trình foreground/notification thường trực nào xuất hiện;
  - Bấm **Run now** → log *"Lấy mẫu giấc ngủ xong: N phút cho đêm đang xét"* và job trả `success`;
  - Mỗi worker vẫn chỉ có **1 bản ghi** (`ExistingPeriodicWorkPolicy.KEEP` — xem thêm TC-INIT-02, TC-INIT-03).
- **Ghi chú:** Job chạy được **khi app đã đóng** (không cần mở app) — đây là lý do phải dùng WorkManager chứ không phải chỉ nghe cảm biến ở màn Trang chủ.

### TC-SLEEP-07 — Thẻ **"Khung giờ ngủ"** trên màn Trang chủ
- **Ưu tiên:** P1
- **Bước:**
  1. `pm clear com.healthyai.app` → mở app → vào tab **Trang chủ**.
  2. Cuộn xuống ngay dưới thẻ **"Tóm tắt sức khỏe"**.
- **Kết quả mong đợi:**
  - Ngay **dưới** thẻ "Tóm tắt sức khỏe" có thẻ **"Khung giờ ngủ"** (icon giường ngủ) với mô tả nêu rõ khung đang dùng: *"... hiện tại **23:00 – 07:00** (8 giờ) ... Thời gian điện thoại bất động ngoài khung này không được tính là đang ngủ."* và nút **"Đổi khung giờ ngủ"**;
  - **Chưa đặt gì** (DB `app_settings` rỗng) → khung hiển thị là **23:00 – 07:00** (mặc định);
  - Thẻ đứng **trước** các thẻ xin quyền (Health Connect / thông báo / đếm bước chân) và luôn hiện, không phụ thuộc quyền nào.
- **Ghi chú:** Xem nhanh giá trị đã lưu: **App Inspection → Database Inspector** → `healthy_ai_encrypted.db` → bảng `app_settings` (khoá `sleep_bedtime_minutes` / `sleep_wake_minutes`, giá trị là **phút trong ngày**).

### TC-SLEEP-08 — Hộp thoại đặt giờ: đổi giờ, chặn khung rỗng, lưu và huỷ
- **Ưu tiên:** P1
- **Bước:**
  1. Ở thẻ "Khung giờ ngủ" bấm **"Đổi khung giờ ngủ"** → hộp thoại **"Khung giờ ngủ"** hiện ra với 2 dòng **Giờ đi ngủ** / **Giờ thức** và dòng tóm tắt `Khung 23:00 – 07:00 · 8 giờ`.
  2. Bấm nút giờ ở dòng **Giờ đi ngủ** → chọn **22:30** → bấm **OK**; bấm dòng **Giờ thức** → chọn **06:15**.
  3. Bấm **Lưu** → xem lại thẻ trên Trang chủ và bảng `app_settings`.
  4. Mở lại hộp thoại, đặt **Giờ thức = Giờ đi ngủ** rồi bấm **Lưu**.
  5. Sửa lại thành 2 mốc khác nhau → bấm **Huỷ**.
- **Kết quả mong đợi:**
  - Bước 2: hộp thoại chọn giờ của hệ thống (đồng hồ **24 giờ**) hiện ra; chọn xong dòng tóm tắt cập nhật ngay (`Khung 22:30 – 06:15 · 7 giờ 45 phút`);
  - Bước 3: hộp thoại **đóng**, thẻ trên Trang chủ hiện **22:30 – 06:15**, `app_settings` có `sleep_bedtime_minutes = 1350` và `sleep_wake_minutes = 375` — **không cần mở lại app**;
  - Bước 4: hộp thoại **vẫn mở**, hiện lỗi **"Giờ đi ngủ và giờ thức không được trùng nhau"** ngay dưới dòng tóm tắt, và `app_settings` **không** bị ghi/đổi (thẻ trên Home vẫn 22:30 – 06:15);
  - Bước 5: bấm **Huỷ** → hộp thoại đóng, khung **không** đổi (vẫn là khung đã lưu ở bước 3).
- **Ghi chú:** Trạng thái hộp thoại, kiểm tra hợp lệ và việc lưu đều nằm trong `HomeViewModel` (`openSleepScheduleEditor`, `onSleepScheduleDraftChange`, `saveSleepSchedule`, `dismissSleepScheduleEditor`) nên test được bằng JVM — xem TC-SLEEP-13.

### TC-SLEEP-09 — Trong khung giờ ngủ, điều kiện "ngủ" làm tăng số phút trong prefs
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã đặt khung giờ ngủ **bao quanh thời điểm test** (ví dụ khung 07:00 → 12:00 rồi đổi giờ máy/emulator vào khoảng 09:00 — emulator cho phép đổi giờ trong Settings), hoặc test vào đúng đêm thật.
- **Bước:**
  1. Tắt màn hình điện thoại, **che cảm biến ánh sáng** (hoặc quay máy xuống mặt bàn trong phòng tối), **để máy hoàn toàn yên**.
  2. Chạy `periodic_sleep_tracking_worker` bằng **Run now** **hai lần**, cách nhau vài phút (lần thứ hai vẫn để máy yên trong cùng điều kiện).
  3. Kiểm tra prefs `healthy_ai_sleep_tracker` (Device Explorer → `data/data/com.healthyai.app/shared_prefs/`).
- **Kết quả mong đợi:** `last_was_asleep = true` sau lần chạy đầu; `sleep_millis` **tăng đúng bằng khoảng thời gian giữa hai lần chạy**; `night` = **ngày mai** nếu khung vắt qua nửa đêm và mẫu nằm **trước** nửa đêm, hoặc = **hôm nay** nếu khung không vắt — theo đúng quy tắc khoá đêm ở TC-SLEEP-01.
- **Ghi chú:** Trên emulator, cảm biến ánh sáng thường **không có** → `lightLux` được coi là `0` ("tối"), nên chỉ cần **màn hình tắt + máy bất động** là đủ; muốn kiểm tra nhánh "phòng sáng" thì dùng thiết bị thật hoặc phủ sáng mạnh vào cảm biến.

### TC-SLEEP-10 — Ngoài khung / màn hình đang bật / phòng sáng / máy rung lắc → KHÔNG cộng dồn
- **Ưu tiên:** P1
- **Tiền điều kiện:** Khung giờ ngủ **không** chứa thời điểm test (ví dụ đặt khung 23:00 → 07:00 rồi test lúc 09:00).
- **Bước:**
  1. Tắt màn hình, che cảm biến sáng, để máy yên → Run now (`periodic_sleep_tracking_worker`).
  2. Đặt lại khung giờ ngủ cho **chứa** thời điểm test → **mở màn hình** và thao tác trên app → Run now.
  3. Tắt màn hình nhưng **chiếu sáng mạnh** vào cảm biến ánh sáng (đèn pin) → Run now.
  4. Trong phòng tối, tắt màn hình nhưng **lắc/giữ điện thoại trên tay** rồi Run now.
  5. So `sleep_millis` trong prefs trước/sau mỗi bước.
- **Kết quả mong đợi:** Cả bốn bước đều **không** làm `sleep_millis` tăng; `last_was_asleep = false`; các mẫu "ngủ" sau đó **không** cộng ngược khoảng vừa thức (TC-SLEEP-03).
- **Ghi chú:** Bước 1 chính là điểm khác biệt so với bản trước: **ngoài khung người dùng đặt thì không đo gì cả**. Vì mẫu "thức" luôn **cắt** chuỗi cộng dồn, việc người dùng cầm điện thoại lúc nửa đêm không làm sai lệch số giờ ngủ đã ghi trước đó.

### TC-SLEEP-11 — Thẻ Tóm tắt sức khỏe + ghi vào `health_data_history` (ưu tiên Health Connect)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy/emulator chạy được worker (TC-SLEEP-09) **và** chưa bơm dữ liệu giấc ngủ nào vào Health Connect.
- **Bước:**
  1. Tạo trong prefs `healthy_ai_sleep_tracker` một trạng thái hợp lệ của **đêm đang xét** (ví dụ `night` = hôm nay, `sleep_millis` = `27000000` = 7,5 giờ, `last_was_asleep = false`) → mở tab **Trang chủ**.
  2. Run now `periodic_health_sync_worker` (xem mục 2.4) → mở **Database Inspector** → bảng `health_data_history` + `logcat` tag `HealthDataRepository` + tab **Phân tích**.
  3. Bơm một phiên ngủ giả vào **Health Connect** (mục 2.5) → mở lại tab Trang chủ.
  4. Xoá app khỏi recents → mở lại app → vào Trang chủ.
- **Kết quả mong đợi:**
  - Bước 1: ô **Ngủ** hiển thị **`7h 30m`** và có dòng *"Giờ ngủ ước lượng từ cảm biến của điện thoại (Health Connect chưa có dữ liệu)"* ngay dưới 3 vòng tròn chỉ số;
  - Bước 2: hàng của **hôm nay** trong `health_data_history` có `sleepHours` = **số giờ ngủ ước lượng** (không còn `0.0`) và vẫn **1 hàng/ngày** (gộp, không thêm hàng trùng); `logcat` có *"Đồng bộ xong và lưu vào Room: N bước, X giờ ngủ hôm nay"*; tab Phân tích dùng được số này;
  - Bước 3: ô **Ngủ** chuyển sang số của **Health Connect** (không phải số trong prefs) và dòng chú thích nguồn **biến mất** — đồng hồ đeo tay luôn thắng;
  - Bước 4: giá trị vẫn đúng như bước 3 (số giờ ngủ của đêm đã qua không đổi trong ngày), không có banner lỗi đỏ.
- **Ghi chú:** Đây là điểm **khác** với bước chân (bước chân ưu tiên **cảm biến** vì là nguồn live): giờ ngủ **không đổi trong ngày** nên số của Health Connect không bị "đóng băng". Nếu lần sync sau **không có** dữ liệu nào thì số đã ghi **không bị xoá về 0**. Đơn vị test JVM tương ứng: `HealthDataRepositoryImplTest` (4 test giờ ngủ) + `HomeViewModelTest` (4 test giờ ngủ).

### TC-SLEEP-12 — Khung giờ ngủ được lưu ở bảng `app_settings` (migration 3→4)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Bản cũ đang cài là **schema v3** (trước khi có bảng `app_settings`) và đã có thói quen + nhật ký.
- **Bước:**
  1. Cài **đè** APK mới (không `pm clear`) → mở app → vào Trang chủ (thẻ "Khung giờ ngủ" phải hiện **23:00 – 07:00**).
  2. Đặt khung **22:00 → 06:30** rồi **Lưu** → mở **Database Inspector** → bảng `app_settings`.
  3. Đóng app hoàn toàn → mở lại → kiểm tra thẻ "Khung giờ ngủ" và tab **Thói quen**.
- **Kết quả mong đợi:**
  - `app_settings` có đúng 2 hàng: `sleep_bedtime_minutes = 1320`, `sleep_wake_minutes = 390` — **thêm khung mới chỉ ghi đè theo khoá**, không sinh hàng trùng;
  - **Nâng cấp không mất dữ liệu**: `habits`, `habit_logs`, `health_data_history` còn nguyên sau khi cài đè (TC-DB-15, TC-DB-17);
  - Mở lại app: thẻ hiện **22:00 – 06:30** (giá trị đọc từ DB), không có banner lỗi, không có dialog;
  - Xoá hàng trong `app_settings` (hoặc `pm clear`) → thẻ quay về mặc định **23:00 – 07:00** thay vì trống.
- **Ghi chú:** Bảng dùng dạng **khoá – giá trị** nên thiết lập thêm sau này **không cần** migration nữa; giá trị thiếu/hỏng (thiếu 1 khoá, không phải số, ngoài `0..1439`, hai mốc trùng nhau) đều rơi về mặc định — chốt ở `SettingsRepositoryImplTest` (TC-SLEEP-13).

### TC-SLEEP-13 — Unit test JVM cho khung giờ ngủ, thuật toán và luồng tích hợp
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew :app:testDebugUnitTest` (JDK 21) hoặc chạy riêng `--tests "*SleepScheduleTest" --tests "*SleepTrackerMathTest" --tests "*SettingsRepositoryImplTest" --tests "*HealthDataRepositoryImplTest" --tests "*HomeViewModelTest" --tests "*DatabaseMigrationsTest"`.
- **Kết quả mong đợi:** Toàn bộ pass — **89 test** thuộc module này:
  - `SleepScheduleTest` — **6 test**: khung vắt/không vắt qua nửa đêm, `contains` nửa mở, độ dài, khung rỗng không hợp lệ, mặc định 23:00–07:00;
  - `SleepTrackerMathTest` — **26 test**: khung giờ + khoá đêm (nhảy tại giờ đi ngủ), phân loại "đang ngủ", cộng dồn, sang đêm mới + chốt đêm cũ, cắt trần, đổi khung giữa chừng, số phút hiển thị, ngưỡng 45 phút và làm tròn;
  - `SettingsRepositoryImplTest` — **7 test**: đọc/ghi theo phút trong ngày, giá trị thiếu/hỏng/trùng mốc → mặc định, khung không hợp lệ **không** ghi DB, `observe` phát đúng giá trị;
  - `DatabaseMigrationsTest` — **4 test** (thêm 1: `MIGRATION_3_4` chỉ tạo bảng `app_settings`, không DROP/ALTER bảng khác);
  - `HealthDataRepositoryImplTest` — **10 test** (4 test giờ ngủ: nguồn tự đo khi Health Connect trống, Health Connect thắng, giữ số đã ghi);
  - `HomeViewModelTest` — **36 test** (**7 test mới của bản này** cho khung giờ ngủ: mặc định 23:00–07:00, đọc giá trị từ DB, mở hộp thoại, lưu, chặn khung rỗng, xoá lỗi khi sửa giờ, huỷ không lưu; trước đó đã có **4 test** cho phần hiển thị/ưu tiên giờ ngủ).
- **Ghi chú:** Kiểm tra bằng mắt (prefs, Room, UI) vẫn theo **TC-SLEEP-06…TC-SLEEP-12**; báo cáo HTML ở `app\build\reports\tests\testDebugUnitTest\index.html`.

---

## 25. TC-HR — Đo nhịp tim bằng camera + đèn flash (PPG)

> Đây là **nguồn nhịp tim "tự lực" thứ ba** của app (song song **TC-STEP** và **TC-SLEEP**): máy không
> có ứng dụng sức khỏe / đồng hồ đeo tay vẫn **tự đo được nhịp tim**.
>
> **Nguyên lý (PPG — photoplethysmography):**
> 1. **Đèn flash chiếu sáng xuyên qua mô ngón tay**;
> 2. **camera** ghi lại **sự thay đổi rất nhỏ về sắc độ đỏ/độ sáng** của từng khung hình video theo
>    từng đợt máu dồn về đầu ngón tay;
> 3. **thuật toán** phân tích chu kỳ đổi màu liên tục đó để quy ra nhịp tim.
>
> **Nắm trước khi test:**
> - `domain/usecase/heartrate/PpgAnalyzer.kt` — toàn bộ thuật toán (hàm thuần, test được trên JVM):
>   lọc mượt AC → bỏ thành phần một chiều → tìm đỉnh mạch (ngưỡng thích ứng + chống dội + nội suy
>   đỉnh) → **kiểm tra tính chu kỳ bằng tự tương quan** → trung vị khoảng RR (loại ngoại lệ) ⇒ bpm;
> - `domain/usecase/heartrate/HeartRateMath.kt` — ngưỡng và phép tính dùng chung 40–200 bpm, độ đều
>   của nhịp, độ mạnh tín hiệu AC/DC, đổi YUV→kênh đỏ, phân loại nhịp nghỉ 60–100;
> - `data/heartrate/CameraPpgAnalyzer.kt` — `ImageAnalysis.Analyzer` đọc **giá trị đỏ trung bình vùng
>   giữa khung hình** (mặt phẳng `Y` + `V`) từ mỗi khung hình CameraX;
> - `presentation/screen/heartrate/` — `HeartRateScreen` (camera + đèn flash + **phần hướng dẫn**) và
>   `HeartRateViewModel` (máy trạng thái: chờ → đang đo → kết quả, tự lưu kết quả);
> - `AppNavigation` → `Screen.MeasureHeartRate` (`"measure_heart_rate"`, **không** nằm trong bottom bar);
> - Nguồn số liệu ở ô **"Tim"**: `HomeUiState.displayHeartRate` (Health Connect → nhịp ghi trên máy).
>
> ⚠️ **Không phải thiết bị y tế:** đây là **ước lượng** từ camera điện thoại. Tay rung, ấn ngón tay quá
> mạnh (chặn dòng máu), ánh sáng phòng nhấp nháy hoặc máy không có đèn flash đều làm sai kết quả — vì
> vậy app **chỉ kết luận khi nhịp đều và tín hiệu có chu kỳ rõ**, và **không lưu** khi tín hiệu nhiễu.

### TC-HR-01 — Lối vào: ô "Tim" ở Trang chủ → màn "Đo nhịp tim"
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã vào được app (đăng nhập hoặc chế độ khách). Nên dùng **máy thật có đèn flash**.
- **Bước:**
  1. Tab **Trang chủ** → thẻ **"Tóm tắt sức khỏe"** → ô thứ ba (nhãn **"Tim"**) có dòng **"Đo ngay"**.
  2. Bấm vào ô "Tim".
  3. Quan sát bottom bar.
- **Kết quả mong đợi:**
  - Mở màn **"Đo nhịp tim"**: tiêu đề **"Đo nhịp tim"**, phụ đề **"Đặt ngón tay lên camera sau để đo nhịp tim"**, nút **"Bắt đầu đo"** màu đỏ, và **không** có bottom bar (màn phụ, giống màn Thêm/Sửa thói quen).
  - **Đèn flash của máy bật ngay** khi màn mở (camera sau đã mở ở chế độ chờ) và **vùng xem trước** hiện khung camera.
  - Bottom bar vẫn **4 tab** (Trang chủ / Thói quen / Ghi chép / Phân tích) — tính năng này **không** thêm tab.
  - Nút **quay lại** (mũi tên) đưa về Trang chủ.
- **Ghi chú:** Nếu Health Connect **đã có** nhịp tim thì ô "Tim" hiển thị số của Health Connect (dòng "Đo ngay" vẫn còn) — xem **TC-HR-07**.

### TC-HR-02 — Hướng dẫn 3 bước trước khi đo (phần "hướng dẫn người dùng")
- **Ưu tiên:** P1
- **Bước:** 1. Mở màn đo (TC-HR-01) → 2. Đọc thẻ **"Cách đo nhịp tim"**.
- **Kết quả mong đợi:** Thẻ hướng dẫn hiện **3 bước có số thứ tự**:
  1. *"Đặt đầu ngón tay (ngón trỏ hoặc ngón giữa) che kín cả camera sau và đèn flash."*
  2. *"Giữ yên tay, ấn nhẹ vừa phải — ấn mạnh quá sẽ chặn dòng máu và làm sai kết quả."*
  3. *"Giữ nguyên 15–30 giây cho tới khi app chốt được kết quả."*
  kèm dòng lưu ý có icon tia chớp: *"Đèn flash sẽ sáng trong suốt quá trình đo. Nếu máy không có đèn flash, hãy đo ở nơi đủ sáng."*; cuối màn có dòng **miễn trừ**: *"Kết quả đo bằng camera điện thoại chỉ mang tính tham khảo, không phải thiết bị y tế…"*.
- **Ghi chú:** Đây là phần **hướng dẫn trước khi đo**; phần hướng dẫn **trong lúc đang đo** là **TC-HR-05**.

### TC-HR-03 — Quyền Camera (chưa cấp / từ chối)
- **Ưu tiên:** P1
- **Tiền điều kiện:** `adb shell pm clear com.healthyai.app` (hoặc thu hồi quyền Camera trong Settings).
- **Bước:**
  1. Vào Trang chủ → bấm ô "Tim" (**Đo ngay**).
  2. Quan sát màn hình khi **chưa** có quyền.
  3. Bấm **"Cấp quyền Camera"** → chọn **Deny / Từ chối**.
- **Kết quả mong đợi:**
  - Bước 2: hiện thẻ **"Cần quyền Camera"** (*"… Khung hình chỉ được xử lý ngay trên máy và không được lưu lại."*) + nút **"Cấp quyền Camera"** — **không** có khung xem trước, **không** bật đèn flash, nút "Bắt đầu đo" **không** hiện.
  - Bước 3: thẻ quyền vẫn hiện và có **thẻ lỗi đỏ** *"Không mở được camera: ứng dụng chưa được cấp quyền Camera"* (đóng được bằng nút X); app **không crash**.
  - Cấp quyền lại (**Allow**) → thẻ quyền biến mất, khung xem trước + đèn flash hoạt động như TC-HR-01.

### TC-HR-04 — Đo thành công (số nhịp hiện dần và tự chốt sau ≥ 15 giây)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy thật có đèn flash; người test **ngồi yên** 1 phút trước khi đo.
- **Bước:**
  1. Mở màn đo → bấm **"Bắt đầu đo"** → đặt **đầu ngón trỏ** che kín camera sau + đèn flash, giữ yên tay.
  2. Quan sát vòng tròn tiến độ, số nhịp và dạng sóng.
  3. Sau khi có kết quả, so **số bpm** với một nguồn tham chiếu (đồng hồ thông minh / tự đếm mạch 15 giây × 4).
- **Kết quả mong đợi:**
  - Phụ đề đổi thành **"Đang đo — hãy giữ yên"**; nút **"Dừng đo"** thay cho "Bắt đầu đo".
  - Trong ~5 giây đầu số nhịp (số lớn giữa vòng tròn) bắt đầu có giá trị; dòng dưới vòng tròn đếm ngược **"còn 15s" → "còn 1s"**, sau đó chuyển thành **"đã nhận N nhịp"**; vòng tròn chạy đầy dần và **dạng sóng mạch đập vẽ liên tục** ở khung dưới.
  - Thanh **"Tín hiệu"** hiện **"Mạnh"** hoặc **"Trung bình"** (màu xanh) khi ngón tay che đúng.
  - Sau **≥ 15 giây**, khi nhịp đã đều, app **tự dừng** và hiện **thẻ kết quả**: số bpm cỡ lớn + **"nhịp/phút"** + nhãn phân loại (**"Nhịp bình thường"** / **"Nhịp thấp"** / **"Nhịp cao"** — tham khảo 60–100 lúc nghỉ) + dòng **"Độ tin cậy của phép đo: N%"** + **"Đang lưu vào nhật ký sức khỏe…"** rồi **"Đã lưu vào nhật ký sức khỏe — số này sẽ hiện ở ô \"Tim\" trên Trang chủ."**; hai nút **"Đo lại"** và **"Xong"**.
  - **Đèn flash tắt** ngay khi có kết quả; số đo **lệch không quá ~5 bpm** so với nguồn tham chiếu (đo lại 2–3 lần vẫn ổn định).
- **Ghi chú:** Đo đúng cách là yếu tố quyết định: ngón tay che **kín** cả đèn flash lẫn ống kính, **không** ấn mạnh, tay **tì** lên bàn. Nếu số nhảy loạn hoặc không ra kết quả, xem **TC-HR-05** và **TC-HR-08**.

### TC-HR-05 — Hướng dẫn đổi theo tình trạng đo (phần hướng dẫn **trong lúc đo**)
- **Ưu tiên:** P1
- **Bước:**
  1. Bấm **"Bắt đầu đo"** nhưng **chưa** đặt ngón tay lên camera → quan sát thẻ hướng dẫn.
  2. Đặt ngón tay che camera nhưng **ấn thật mạnh** (hoặc che hờ) → quan sát lại.
  3. Che đúng và giữ yên → quan sát tiếp; chờ tới sau mốc 15 giây.
- **Kết quả mong đợi:** Thẻ hướng dẫn (ngay dưới bảng đo) **đổi nội dung theo tình trạng thật**:
  - Bước 1: **"Chưa thấy ngón tay"** (icon ngón tay, màu cam) — *"Hãy đặt đầu ngón tay che kín cả camera sau và đèn flash (ánh sáng sẽ hắt ngược màu đỏ qua da)."*
  - Bước 2: **"Tín hiệu yếu"** (icon cảnh báo, màu cam) — *"Ấn vừa phải (đừng ấn mạnh) và giữ yên tay…"*; thanh "Tín hiệu" ở mức **"Yếu"**.
  - Bước 3: **"Đang đo nhịp tim"** (icon tick, màu xanh) — *"Giữ yên tay và đừng nói chuyện; app đang phân tích từng đợt máu dồn về đầu ngón tay."*
  - Quá 15 giây mà tín hiệu chưa đủ rõ: **"Sắp xong"** — *"Tín hiệu chưa đủ đều để kết luận — cố giữ yên thêm vài giây."*
- **Ghi chú:** Bốn câu này ứng với `HeartRateGuidance.{PLACE_FINGER, WEAK_SIGNAL, MEASURING, ALMOST_DONE}` — chốt bằng unit test **TC-HR-13**.

### TC-HR-06 — Kết quả tự lưu vào `health_data_history` (và Rule Engine dùng được)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy đã có ít nhất 1 hàng của hôm nay trong `health_data_history` (hoặc chưa có, đều được).
- **Bước:**
  1. Đo thành công (**TC-HR-04**) → bấm **"Xong"**.
  2. Mở **Database Inspector** (hoặc `adb` + SQLCipher — xem mục 2.3) → bảng `health_data_history`.
  3. Mở tab **Phân tích**.
- **Kết quả mong đợi:**
  - Bảng `health_data_history` có **đúng 1 hàng cho hôm nay** (gộp, không sinh hàng trùng); cột `heartRate` = **số vừa đo**; các cột `steps` / `sleepHours` / `activeCalories` **giữ nguyên** giá trị đã có của ngày hôm đó; `recordedAt` được cập nhật.
  - Nếu hôm nay **chưa có hàng nào**: một hàng **mới** được thêm với `heartRate` = số vừa đo (các cột khác bằng `0`) — hàng này **không** bị coi là "dữ liệu rác" vì có chỉ số thật.
  - Tab **Phân tích** đọc được số này (Rule Engine đọc `health_data_history`): khi nhịp trung bình 7 ngày > 85 → có cảnh báo *"Nhịp tim trung bình 7 ngày qua của bạn là … bpm"*.
  - Đo **2 lần trong ngày** → vẫn **1 hàng**, `heartRate` là số của lần đo **sau**.
- **Ghi chú:** Hàm lưu là `SaveMeasuredHeartRateUseCase` → `HealthDataRepositoryImpl.saveMeasuredHeartRate()` (dùng lại đúng luật gộp `upsertToday` của `syncHealthData`). Nhịp ≤ 0 bị **từ chối** ngay, không ghi DB — 6 test trong `HealthDataRepositoryImplTest`.

### TC-HR-07 — Ưu tiên nguồn nhịp tim ở ô "Tim" (Health Connect thắng) — trong cùng một ngày
- **Ưu tiên:** P1
- **Bước:**
  1. Đo nhịp tim bằng camera (**TC-HR-04**) → về Trang chủ → ghi lại số ô "Tim".
  2. Bơm dữ liệu `HeartRateRecord` giả vào Health Connect (mục 2.5 bước 3) → rời Trang chủ rồi vào lại.
  3. Xoá dữ liệu Health Connect (hoặc thu hồi quyền đọc nhịp tim) → rời Trang chủ rồi vào lại.
- **Kết quả mong đợi:**
  - Bước 1: ô "Tim" = **số vừa đo** (ví dụ `74 bpm`). **Không** có dòng chú thích nguồn nào dưới thẻ (mục 3 #26).
  - Bước 2: ô "Tim" = **số của Health Connect** — đây là **nhịp trung bình của Health Connect từ 00:00 hôm nay tới hiện tại**, nên số bơm ở bước 2 phải **thuộc ngày hôm nay** mới thấy đổi (Health Connect được ưu tiên vì có thể là số của đồng hồ đeo tay).
  - Bước 3: ô "Tim" quay về **số đã ghi trong ngày** (số không bị mất).
  - Hôm nay **chưa đo** lần nào và Health Connect cũng trống → ô "Tim" hiện **`-- bpm`** (vẫn có nút "Đo ngay") — **không** hiện `0 bpm` và **không** hiện lại số của hôm qua; xem **TC-HOME-15**.
- **Ghi chú:** `HomeUiState.displayHeartRate` = `healthData.heartRate` (Health Connect, **hôm nay**) → dự phòng `recordedHeartRate` (nhịp ghi **trong ngày**, `HealthDataDao.getLatestHeartRateSince(00:00 hôm nay)`). 4 test trong `HomeViewModelTest` (đọc lại mỗi khi quay về Trang chủ nhờ `refreshSystemPermissions()`), riêng "sang ngày mới chưa đo lại thì `0`" là test `a new day with no measurement yet keeps the heart circle empty` (**TC-HOME-15**).

### TC-HR-08 — Tín hiệu nhiễu: **không** hiện số, **không** lưu
- **Ưu tiên:** P1
- **Bước:**
  1. Bấm **"Bắt đầu đo"** → **cố tình rung tay liên tục** (hoặc dí ngón tay lệch để chỉ che một nửa camera) trong suốt 30 giây.
  2. Chờ app tự dừng.
- **Kết quả mong đợi:**
  - Không bao giờ hiện một con số nhịp tim **chắc chắn** cho tín hiệu nhiễu (số có thể nhấp nháy nhưng chỉ chốt khi nhịp **đều** và tín hiệu **có chu kỳ**).
  - Quá **30 giây** (`HeartRateMath.MAX_MEASURE_MS`) app **tự dừng** và hiện thẻ **"Chưa đo được nhịp tim"** + hướng dẫn thử lại + nút **"Thử lại"** và **"Quay lại"**.
  - Thẻ lỗi đỏ *"Không đọc được nhịp tim rõ ràng. Hãy đặt đầu ngón tay che kín camera và đèn flash, giữ yên tay và ấn vừa phải rồi đo lại."* xuất hiện.
  - **`health_data_history` KHÔNG có hàng mới** và `heartRate` **không** bị đổi — app **không** lưu một con số sai (đây là yêu cầu bắt buộc vì đây là số liệu sức khỏe).
- **Ghi chú:** Tương ứng các test `noise is refused…`, `pure noise never passes as a heartbeat` (**TC-HR-12**, **TC-HR-13**).

### TC-HR-09 — "Dừng đo" giữa chừng
- **Ưu tiên:** P1
- **Bước:**
  1. Bấm "Bắt đầu đo", đặt ngón tay ~10 giây → bấm **"Dừng đo"**.
  2. Bấm **"Bắt đầu đo"** lại → đo bình thường tới khi có kết quả.
- **Kết quả mong đợi:**
  - Sau "Dừng đo": quay về màn hướng dẫn (nút "Bắt đầu đo"), vòng tròn/ số nhịp/ dạng sóng **xoá sạch** (không giữ số của lần đo bỏ dở), **không** có kết quả, **không** ghi gì vào DB.
  - Lần đo lại bắt đầu **từ 0 giây** và vẫn ra kết quả như TC-HR-04.
  - Bấm **"Xong"**/**quay lại** giữa chừng cũng **không** lưu gì.

### TC-HR-10 — Máy không có đèn flash
- **Ưu tiên:** P2
- **Tiền điều kiện:** Emulator (hoặc máy không có đèn flash).
- **Bước:** 1. Mở màn đo nhịp tim.
- **Kết quả mong đợi:** Hiện **thẻ lỗi đỏ** *"Máy này không có đèn flash nên phép đo chỉ dựa vào ánh sáng phòng — kết quả có thể kém chính xác. Nên đo ở nơi đủ sáng hoặc dùng thiết bị có đèn flash."*; app **không** crash; người dùng vẫn bấm "Bắt đầu đo" được (nhưng kết quả không đáng tin — dùng để kiểm tra đường đi của luồng, **không** đánh giá độ chính xác). Đóng thẻ lỗi bằng nút X; nếu thông báo này bị đóng rồi bấm đo thì **không** hiện lại (chỉ hiện 1 lần cho mỗi lần mở camera).

### TC-HR-11 — Rời màn / khoá máy / xoay màn hình khi đang đo
- **Ưu tiên:** P1
- **Bước:**
  1. Đang đo (đèn flash đang sáng) → bấm **Home** (thoát app ra nền) rồi quay lại app.
  2. Đang đo → bấm **quay lại** để rời màn đo → kiểm tra đèn flash và camera.
  3. Đang đo → **xoay màn hình** (nếu máy không khoá xoay) hoặc tắt/mở màn hình.
- **Kết quả mong đợi:**
  - Bước 1: đèn flash **tắt** khi app ra nền (camera gỡ khỏi vòng đời), quay lại thì đèn **bật lại** và phép đo **tiếp tục** (đồng hồ đo chạy theo **mốc thời gian của camera** nên không bị nhảy).
  - Bước 2: rời màn → **đèn flash tắt ngay**, camera được gỡ; quay lại Trang chủ **không** có đèn sáng, pin không bị hao thêm; mở lại màn đo thì bắt đầu từ trạng thái chờ ("Bắt đầu đo").
  - Bước 3: xoay màn hình **không crash**, UI vẫn đúng (đang đo thì tiếp tục đo; đã có kết quả thì kết quả vẫn còn vì nằm trong ViewModel).
  - `adb logcat -s AndroidRuntime:E` **không** có crash; `dumpsys media.camera` cho thấy app **không** còn giữ camera sau khi rời màn.
- **Ghi chú:** Đèn flash **phải** tắt trong mọi đường thoát: `HeartRateScreen.HeartRateCamera` tắt đèn và `unbindAll()` trong `finally` của `LaunchedEffect` (chạy cả khi coroutine bị huỷ).

### TC-HR-12 — Chẩn đoán: thuật toán PPG (unit test JVM)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew :app:testDebugUnitTest --tests "*HeartRateMathTest" --tests "*PpgAnalyzerTest"` (JDK 21 — mục 2.2).
- **Kết quả mong đợi:** Pass **35 test** (20 + 15), trong đó chốt các tính chất:
  - `HeartRateMathTest` — ngưỡng `MIN/MAX_BPM` 40–200 **khớp** với `MIN/MAX_INTERVAL_MS` (300–1500 ms), trung vị & MAD chịu được một khoảng ngoại lệ, độ đều = 1 khi nhịp đều tuyệt đối và = 0 khi lệch > 15%, **độ mạnh tín hiệu là tỉ lệ AC/DC** (cùng 1% ở ảnh tối và ảnh sáng cho **cùng** điểm), nội suy đỉnh parabol (đỉnh lệch phải → offset dương…), đổi YUV→kênh đỏ (V = 128 ⇒ đỏ = độ sáng; V ngả đỏ ⇒ đỏ cao hơn), phân loại 59/60/100/101 bpm.
  - `PpgAnalyzerTest` — tín hiệu mạch đập **giả lập đúng vật lý** (nền DC lớn + đỉnh tâm thu rộng ~55 ms + gợn dicrotic) ở **30 hình/giây**: **72 bpm → 72 ± 3 bpm**, **130 bpm → ± 4**, **48 bpm → ± 4**; đếm được số nhịp; dạng sóng nằm trong `-1..1`; **chưa đủ 15 giây thì `ready = false`**; **nhiễu thuần không bao giờ thành nhịp** (chất lượng < 0,5); tín hiệu lẫn nhiễu mạnh nếu có số thì không lệch quá ± 8 bpm; khung tối (không có ngón tay) ⇒ `fingerDetected = false`, `bpm = null`, số nhịp về 0; nhấc ngón tay giữa chừng ⇒ **quên** nhịp cũ rồi đo lại được; nhịp không đều ⇒ không tự chốt; `reset()` xoá sạch; mốc thời gian `0` vẫn tính đúng.
- **Ghi chú:** Tín hiệu giả lập nằm ở `app/src/test/.../heartrate/PpgTestSignal.kt` (dùng chung cho cả test ViewModel).

### TC-HR-13 — Chẩn đoán: máy trạng thái của màn đo (unit test JVM)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew :app:testDebugUnitTest --tests "*HeartRateViewModelTest" --tests "*HomeViewModelTest" --tests "*HealthDataRepositoryImplTest"`.
- **Kết quả mong đợi:** Pass, trong đó (module này **21 + 4 + 6 = 31 test**):
  - `HeartRateViewModelTest` — **21 test**: màn bắt đầu ở `IDLE` với hướng dẫn; bắt đầu đo thì xoá số cũ; **khung hình lúc chưa đo bị bỏ qua**; tín hiệu sạch ⇒ chốt kết quả + **tự lưu** (nhịp lưu quanh 72); **dừng ngay khi đủ điều kiện** (không đo hết 60 giây); nhịp nhanh 130 ⇒ phân loại "Cao"; nhiễu hết 30 giây ⇒ **không** lưu; "Dừng đo" không lưu gì và khung hình sau đó không khởi động lại phép đo; đo lại từ 0 và xoá kết quả cũ; hướng dẫn `PLACE_FINGER` / `WEAK_SIGNAL` / `MEASURING` / `ALMOST_DONE`; lỗi camera ⇒ về `IDLE` + thông báo; cảnh báo không có đèn flash **không** ghi đè lỗi thật; lỗi lưu ⇒ vẫn giữ kết quả nhưng báo chưa lưu được.
  - `HomeViewModelTest` — **4 test nhịp tim**: dùng số ghi trên máy khi Health Connect trống; Health Connect thắng khi có số; không có số nào ⇒ `0`; quay về từ màn đo đọc lại được số mới.
  - `HealthDataRepositoryImplTest` — **6 test nhịp tim**: ghi vào hàng hôm nay khi chưa có; **chỉ** thay cột `heartRate` của hàng đã có (giữ bước chân/giờ ngủ/calo, không thêm hàng); từ chối nhịp ≤ 0; đọc nhịp ghi gần nhất; chưa có ⇒ `null`; giá trị `0` ⇒ coi như chưa có.
- **Ghi chú:** ViewModel chỉ nhận **giá trị đỏ trung bình của từng khung hình** qua `onPpgSample(red, timestampMs)` nên toàn bộ máy trạng thái test được trên JVM không cần camera; báo cáo HTML ở `app\build\reports\tests\testDebugUnitTest\index.html`.

---

## 26. TC-PRIV — Chính sách bảo mật & rationale Health Connect

> Thêm ở bản sửa **phụ lục C #37 / mục 3 #31**. Trước đây repo có `health_privacy_policy_url = "https://your-privacy-policy.com"` (**placeholder**) và `health_permissions_rationale` nhưng **không chỗ nào dùng**; manifest đã có sẵn hai `intent-filter` cho Health Connect (`ACTION_SHOW_PERMISSIONS_RATIONALE` cho Android < 14, `ACTION_VIEW_PERMISSION_USAGE` cho Android 14+) mà app **không** hiển thị gì cả.
>
> Nay có `presentation/screen/privacy/PrivacyPolicyScreen.kt` và `MainActivity` mở thẳng màn này khi nhận hai action kể trên — **không** đi qua cổng đăng nhập (nếu không, Health Connect sẽ thấy màn Đăng nhập và từ chối duyệt app). Màn cũng mở được từ **hai** lối vào trong app: nút **"Xem chính sách bảo mật"** ở thẻ "Đồng bộ đám mây" trên **Trang chủ**, và thẻ **"AI & quyền riêng tư"** ở cuối **tab Phân tích AI** (lối vào thứ hai này được **chuyển từ màn Ghi chép bữa ăn** sang ở mục 3 #43).

### TC-PRIV-01 — Mở màn "Chính sách bảo mật" từ Trang chủ **và** từ tab Phân tích AI
- **Ưu tiên:** P1
- **Bước:**
  1. Mở tab **Trang chủ** → Ở thẻ **"Đồng bộ đám mây"**, bấm **"Xem chính sách bảo mật"**.
  2. Về Trang chủ → mở tab **Phân tích** → cuộn xuống **cuối** màn → ở thẻ **"AI & quyền riêng tư"**, bấm **"Xem chính sách bảo mật"**.
  3. Kiểm tra tab **Ghi chép**: **không** còn nút "Xem chính sách bảo mật" nào ở màn này (mục 3 #43).
- **Kết quả mong đợi:**
  - Cả hai lối vào đều mở màn **"Chính sách bảo mật"** (route riêng, **không** thêm tab ở bottom bar) với nút **Quay lại**; bấm Quay lại trở về đúng tab vừa đứng, không crash.
  - Nội dung có **6 mục**: *Dữ liệu lưu trên máy bạn*, *Health Connect*, *Camera và ảnh món ăn*, *Đồng bộ đám mây (tuỳ chọn — mặc định TẮT)*, *Chúng tôi không bán dữ liệu của bạn*, *Quyền của bạn*.
  - Mục **Health Connect** dùng đúng chuỗi `health_permissions_rationale` trong `strings.xml` (chuỗi này trước đây **không được dùng ở đâu**).
  - Cuộn được toàn bộ nội dung; bấm **Quay lại** (hoặc Back hệ thống) về tab trước đó, không crash.
- **Ghi chú:** Nội dung bám theo code: DB mã hoá SQLCipher + khoá trong Android Keystore, giữ lịch sử sức khỏe **30 ngày**, **ảnh món ăn được gửi tới Google Gemini khi người dùng bấm "Phân tích món ăn"** (mục 3 #42, #43), và mốc "chỉ tải log tạo sau khi bật đồng bộ". Thẻ "AI & quyền riêng tư" và nút ở Trang chủ là hai lối vào cho **cùng một màn**.

### TC-PRIV-02 — Health Connect mở rationale → màn chính sách (không cần đăng nhập)
- **Ưu tiên:** P1
- **Tiền điều kiện:** **Chưa** đăng nhập (đang ở màn Đăng nhập) — đây chính là tình huống Health Connect kiểm tra.
- **Bước:**
  1. Mô phỏng Health Connect gọi app (hoặc mở app Health Connect → mục quyền của HealthyAI → bấm liên kết **chính sách bảo mật/rationale**):
     `adb shell am start -a androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE -n com.healthyai.app/.MainActivity`
     và đường vào Android 14+: `adb shell am start -a android.intent.action.VIEW_PERMISSION_USAGE -c android.intent.category.HEALTH_PERMISSIONS -n com.healthyai.app/.MainActivity`
  2. Quan sát màn hình hiện ra.
- **Kết quả mong đợi:** App mở **thẳng màn "Chính sách bảo mật"** trong **cả hai** action — **không** hiện màn Đăng nhập và **không** hiện bottom bar. Bấm Back → app đóng (không kẹt, không crash).
- **Ghi chú:** `MainActivity.onCreate` kiểm tra `intent.action` rồi render `PrivacyPolicyScreen(onBack = { finish() })` thay cho `AppNavigation()`.

### TC-PRIV-03 — Unit test JVM cho cổng đồng bộ đám mây (mục 3 #31)
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*CloudSyncPolicyTest" --tests "*SettingsRepositoryImplTest"`.
- **Kết quả mong đợi:** Toàn bộ pass, trong đó:
  - `CloudSyncPolicyTest` (**5 test**): TẮT → không tải gì; **bật nhưng thiếu mốc đồng ý** → không tải gì; đã bật → **chỉ** log có `loggedAt >= consentAtMillis`; **lịch sử ghi ở chế độ khách (`isDirty = true`) không được tải lên**; log đúng **bằng** mốc đồng ý vẫn được tải.
  - `SettingsRepositoryImplTest` (**12 test**: chỉ còn khung giờ ngủ + đồng bộ đám mây — **4 test của cổng AI `food_ai_enabled` đã bị xoá cùng công tắc ở mục 3 #43**): chưa có gì → `CloudSyncSettings()` (TẮT); giá trị hỏng → TẮT; **bật** ghi **cả** cờ `true` **và** mốc `consent_at`; **tắt** chỉ ghi cờ `false`; `observeCloudSync()` phát đúng trạng thái đã lưu.
- **Ghi chú:** `CloudSyncPolicy` là **hàm thuần Kotlin** — một chỗ duy nhất quyết định log nào được rời khỏi thiết bị, nên đây là lưới chống hồi quy chính cho lỗi riêng tư #37.

### TC-PRIV-04 — Chính sách bảo mật nói đúng về AI phân tích món ăn (mục 3 #42, #43)
- **Ưu tiên:** P1
- **Bước:**
  1. Mở tab **Phân tích** → cuộn xuống cuối màn → thẻ **"AI & quyền riêng tư"** → bấm **"Xem chính sách bảo mật"**.
  2. Đọc mục **"3. Camera và ảnh món ăn"**, cùng dòng mở đầu và mục **"5. Chúng tôi không bán dữ liệu của bạn"**.
- **Kết quả mong đợi:**
  - Mở đúng màn **"Chính sách bảo mật"** (không qua màn Đăng nhập), bấm Quay lại về đúng **tab Phân tích**.
  - Mục 3 phải **nói thật** hành vi mới (mục 3 #43): khi người dùng **bấm "Phân tích món ăn"** (hoặc chọn ảnh từ thư viện) thì ảnh **được gửi tới Google Gemini** để nhận diện tên món và phân tích lợi ích/điều nên lưu ý; **không bấm phân tích thì app không gửi ảnh đi đâu cả**. Câu *"nếu bạn BẬT 'Phân tích món ăn bằng AI' (mặc định TẮT)"* **không** còn — công tắc đó đã bị bỏ.
  - Dòng mở đầu (`privacy_intro`) nói việc gửi dữ liệu xảy ra khi người dùng **chủ động yêu cầu** (bật đồng bộ đám mây **hoặc** bấm phân tích món ăn). Mục 5 (`privacy_no_sale_body`) nói *"mọi phân tích AI **KHÁC**… chạy trên thiết bị"* và nêu rõ ngoại lệ là **ảnh món ăn gửi tới Google Gemini** — không được để câu "mọi phân tích AI chạy trên máy" như trước.
  - **Không** còn câu cũ *"ảnh không được tải lên máy chủ nào"* (đã sai sau khi chuyển sang AI — đây chính là lý do phải sửa chính sách cùng lúc với code).
- **Ghi chú:** Chính sách mô tả **hành vi thật của bản đang cài**, nên mọi thay đổi về việc dữ liệu rời thiết bị đều phải kèm cập nhật màn này (cùng nguyên tắc đã áp cho đồng bộ đám mây — mục 3 #31, phụ lục C #37). **Mục 3 #43 bỏ công tắc opt-in** nhưng **không** bỏ việc nói rõ ảnh được gửi đi: cái bỏ là *cổng chặn*, không phải *thông tin*. Thẻ "AI & quyền riêng tư" trên tab Phân tích chính là để lối vào này không bị mất theo công tắc (TC-INS-42).

---

## 27. TC-FIX2 — 8 mục còn lại của lượt rà soát toàn app (mục 3 #33…#40)

> Lượt rà soát này xử lý nốt 8 mục "Ghi nhận" của **phụ lục C** (#25, #26, #27, #39, #41, #42, #43, #44).
> Hai mục **#19/#38** (ký release + ProGuard cho bản release) được **chốt là không sửa** vì app chỉ
> **làm thử nghiệm, không phát hành**.
>
> ⚠️ **Điều KHÔNG đổi sau lượt này** (đừng ghi thành lỗi mới):
> - Chữ hiển thị trên mọi màn hình **giống hệt trước** — chỉ đổi **nguồn** lấy chuỗi (`strings.xml`).
> - App vẫn **chưa có bản dịch tiếng Anh** (`values-en`): đổi ngôn ngữ hệ thống sang English thì app
>   vẫn hiển thị **tiếng Việt** (mặc định) và **không** crash — đây là hành vi mong đợi.
> - Một số chuỗi **không** nằm trong `strings.xml`: thông báo lỗi sinh trong ViewModel, nội dung gợi ý AI,
>   tên danh mục/buổi/xếp loại trong domain model, nhãn tuần `T2…CN`, hàm định dạng số và dữ liệu mẫu
>   `@Preview` (xem phụ lục C #41).
> - Thẻ "Xu hướng chất lượng giấc ngủ" chỉ đọc lại dữ liệu khi **quay về** màn Phân tích (không đọc liên
>   tục trong lúc màn đang mở) — Health Connect không cung cấp Flow nên không thể "realtime" như 2 thẻ kia.

### TC-ENV-08 — Tầng i18n: chuỗi UI lấy từ `res/values/strings.xml` (mục 3 #37)
- **Ưu tiên:** P1
- **Bước:**
  1. Kiểm tra nguồn chuỗi: mở `app/src/main/res/values/strings.xml` → phải có **≥ 200** `<string>`.
  2. Kiểm tra code không còn chuỗi hiển thị viết cứng trong màn hình:
     `Get-ChildItem app/src/main/java -Recurse -Filter *.kt | Select-String 'stringResource' | Measure-Object`
     → **> 150**; và không còn `Text("` chứa tiếng Việt trong `presentation/**` (trừ chuỗi sinh trong ViewModel/domain — xem ghi chú trên).
  3. `gradle assembleDebug` → build **phải xanh** (mọi `R.string.*` dùng trong code phải tồn tại — AAPT báo lỗi nếu sai tên).
  4. Mở app và đi qua **cả 6 màn**: Đăng nhập (và 2 chế độ Đăng nhập/Đăng ký), Trang chủ, Thói quen, Thêm/Sửa thói quen, Ghi chép, Đo nhịp tim, Phân tích, Chính sách bảo mật — đối chiếu **từng chữ** với bản trước lượt sửa.
- **Kết quả mong đợi:**
  - Chữ trên màn hình **không đổi** so với trước (không mất dấu, không lẫn tiếng Anh, không có chuỗi rỗng/`null`).
  - Nhãn bottom bar vẫn là **Trang chủ / Thói quen / Ghi chép / Phân tích** (nay đọc từ `R.string.nav_*`).
  - Bộ lọc màn Thói quen vẫn là **Tất cả / Sáng / Chiều / Tối** và lọc đúng như cũ (nay khoá là enum `HabitFilter`).
  - Tên widget trên màn hình chính nay là **"Sức khỏe hôm nay"** (`@string/widget_label`), nội dung là **"Healthy AI" + "Hôm nay" + 3 cột Bước chân / Giấc ngủ / Nhịp tim** (đổi ở lượt "widget Sức khỏe hôm nay" — mục 15) — **không** còn "Tiến độ hôm nay" / "x/y thói quen đạt mục tiêu" / "+N thói quen khác"; mọi chữ đều đọc từ `R.string.*`.
  - Thông báo nhắc nhở vẫn hiện tiêu đề **"HealthyAI 💪 Nhắc nhở thói quen"** + nội dung cũ, kênh thông báo vẫn là **"Nhắc nhở thói quen sức khỏe"**.
  - Đổi ngôn ngữ hệ thống sang **English** → app vẫn tiếng Việt, **không crash**, không có chữ nào biến mất.
- **Ghi chú:** để thêm tiếng Anh chỉ cần tạo `res/values-en/strings.xml` (không phải sửa code Kotlin).

### TC-INS-37 — Máy KHÔNG chia pha giấc ngủ: thẻ vẫn có số liệu (mục 3 #33)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Health Connect (hoặc app bơm dữ liệu) có **phiên ngủ không chia pha** — chỉ `STAGE_TYPE_SLEEPING` (Health Connect Toolbox → SleepSessionRecord **không** đặt `stages`).
- **Bước:** 1. Bơm 1 phiên ngủ ~7 giờ trong 1–2 tuần gần nhất → 2. Mở tab **Phân tích** → 3. Quan sát thẻ **"Xu hướng chất lượng giấc ngủ"**.
- **Kết quả mong đợi:**
  - Thẻ **không** còn rỗng: hiện dòng *"Health Connect chỉ báo tổng thời gian ngủ (máy không chia pha Sâu/Nông/REM): 7h 00m trong 7 đêm gần nhất."*
  - Bấm thẻ/`refresh` (đổi tab rồi quay lại) → số không đổi, không nhấp nháy về trạng thái "Chưa có dữ liệu".
  - Máy **có** chia pha thì hành vi cũ giữ nguyên: 3 thanh **Sâu / Nông / REM** + dòng "Tổng … trong 7 đêm gần nhất".
- **Ghi chú:** trước lượt sửa này phiên không chia pha bị **bỏ qua hoàn toàn** nên thẻ hiện trạng thái rỗng dù máy có dữ liệu ngủ.

### TC-INS-38 — Quay lại màn Phân tích là số liệu giấc ngủ được đọc lại (mục 3 #34)
- **Ưu tiên:** P2
- **Bước:**
  1. Mở tab **Phân tích** → ghi lại số ở thẻ "Xu hướng chất lượng giấc ngủ".
  2. Chuyển sang tab khác (Trang chủ).
  3. Trong lúc **app vẫn đang mở**, ghi thêm một phiên ngủ mới vào Health Connect (Toolbox) hoặc bơm qua máy khác.
  4. Quay lại tab **Phân tích**.
- **Kết quả mong đợi:** Số liệu pha/tổng giờ ngủ **cập nhật ngay khi quay lại** (không cần thoát app).
- **Ghi chú:** Health Connect không cung cấp Flow cho `SleepSessionRecord.stages`; app dùng `LifecycleResumeEffect` để gọi lại `getSleepStageBreakdown` **mỗi lần màn Phân tích được mở lại** — xem `InsightsViewModelTest` (`coVerify(exactly = 2)` sau `refresh()`). Không đọc liên tục trong lúc màn đang mở là **chủ ý** (tránh đọc Health Connect không cần thiết).

### TC-INS-39 — Lỗi đọc dữ liệu ở tab Phân tích không còn bị nuốt (mục 3 #39)
- **Ưu tiên:** P1
- **Tiền điều kiện:** Máy có cách gây lỗi đọc: ví dụ **thu hồi quyền Health Connect** rồi bơm dữ liệu hỏng, hoặc dùng bản debug + `adb shell` để chặn/đặt lại dữ liệu; cũng có thể kiểm chứng bằng unit test (nhanh hơn).
- **Bước (cách nhanh, không cần máy):** `gradle testDebugUnitTest --tests "*InsightsViewModelTest"` → xem 6 test pass.
- **Bước (trên máy):** 1. Gây lỗi đọc dữ liệu của tab Phân tích (ví dụ: xoá/hỏng bảng `health_data_history` nếu test trên bản debug). 2. Mở tab **Phân tích**.
- **Kết quả mong đợi:**
  - Màn Phân tích hiện **thẻ lỗi đỏ** (`ErrorStateCard`) với nội dung *"Không đọc được dữ liệu phân tích: …"* (hoặc *"… chi tiết giấc ngủ từ Health Connect: …"*) — **không** còn im lặng ở trạng thái rỗng.
  - Bấm **nút đóng** → thẻ lỗi biến mất; dữ liệu đọc được trở lại thì lỗi tự xoá và các thẻ hiện bình thường.
  - App **không** crash.
- **Ghi chú:** `InsightsUiState` tách **`error`** (luồng dữ liệu chính) và **`sleepStagesError`** (đọc chi tiết pha) để lỗi đọc giấc ngủ **không** bị luồng chính xoá mất.

### TC-WM-17 — Worker trả `retry`/`failure` đúng khi lỗi & không "hoá" việc dừng worker thành thất bại (mục 3 #36)
- **Ưu tiên:** P1
- **Bước:**
  1. `adb shell cmd jobscheduler run -f com.healthyai.app <jobId>` (lấy `jobId` theo mục 2.3) — chạy `HealthSyncWorker` bình thường → kiểm tra `adb shell dumpsys jobscheduler | findstr -i "periodic_health_sync"`.
  2. Gây lỗi đồng bộ (ví dụ tạm đổi quyền/khoá DB trên bản debug) rồi chạy lại worker **3 lần**.
  3. Kiểm tra log `adb logcat -s HealthSyncWorker`.
- **Kết quả mong đợi:**
  - Lần đồng bộ lỗi: log *"Periodic sync failed: …"* và WorkManager ghi nhận **retry** (state `ENQUEUED` với `runAttemptCount` tăng) — trước đây worker **luôn** trả `success` nên backoff 3 lần **không bao giờ** chạy.
  - Sau **3** lần vẫn lỗi → worker chuyển **`FAILED`** (không retry vô hạn).
  - **Dừng worker** (`adb shell cmd jobscheduler stop …` hoặc tắt app) → **không** sinh `Result.retry/failure`: coroutine bị huỷ được **ném tiếp** (`CancellationException`), không bị biến thành "đồng bộ thất bại".
- **Ghi chú:** cùng nguyên tắc đã áp cho `SyncWorker`, `SleepTrackingWorker`, `HealthConnectDataSourceImpl`, `GalleryImageLoader`, `CameraViewModel`, `CameraScreen`, `LegacyDatabaseKeyMigration` và helper `util/runCatchingCancellable` (phụ lục C #39).

---

## 28. TC-STEP — Đếm bước bằng cảm biến của máy (quyền `ACTIVITY_RECOGNITION`)

> Nguồn số bước **"tự lực"** của app (song song **TC-SLEEP** và **TC-HR**): app đọc trực tiếp
> `Sensor.TYPE_STEP_COUNTER` của điện thoại — **không** cần Health Connect, **không** cần cài thêm ứng
> dụng sức khỏe. Đây là các test case mà **mục 1.1** và **TC-ENV-03** đã tham chiếu từ trước.
>
> ⚠️ **Luật đếm đã siết lại ở mục 3 #44 — đọc trước khi test:**
> - Cảm biến trả **giá trị luỹ kế từ lúc máy khởi động**, và nó **vẫn đếm** cả khi app đã bị đóng. Bản cũ
>   lấy thẳng hiệu số, nên số bước **"tự nhảy" thêm** mỗi lần mở app — kể cả những bước đi lúc người
>   dùng đã đóng hẳn ứng dụng.
> - Nay `domain/usecase/steps/StepCounterMath.kt` (hàm thuần Kotlin, test trên JVM) **chỉ tính phần quan
>   sát được trong lúc app đang chạy**. Mỗi lần **tiến trình app được tạo** là một **phiên**
>   (`StepCounterStore.sessionId` — sinh một lần cho mỗi tiến trình, lưu ở prefs `healthy_ai_step_counter`
>   khoá `session_id`):
>   - app **đang mở**, hoặc **còn nằm trong tác vụ** (chạy nền) → **cùng phiên** → **đếm tiếp** bình thường;
>   - app bị **đóng hẳn** (vuốt khỏi tác vụ, tiến trình bị hệ thống thu hồi) → lần mở sau là **phiên mới**
>     ⇒ khoảng thời gian ở giữa **không** được cộng vào, nhưng **số đã đếm trong ngày vẫn được giữ**.
> - Dữ liệu do **bản cũ** để lại (chưa có khoá `session_id`) được coi là **phiên khác** ⇒ lần mở đầu tiên
>   sau khi cập nhật chỉ bắt đầu đếm từ mốc đó (một lần, theo hướng an toàn: không cộng thêm bước nào).
> - **Không** cần migration: prefs chỉ thêm một khoá mới, khoá thiếu được đọc là chuỗi rỗng.
>
> Thẻ xin quyền ở Trang chủ chỉ hiện khi máy **có** cảm biến mà **chưa** cấp quyền `ACTIVITY_RECOGNITION`
> (`HomeUiState.showStepCounterCard`); quyền này chỉ cần từ Android 10 (API 29).
>
> 💡 **Cách thử nhanh** (không cần đi bộ thật): dùng `adb emu` / cảm biến ảo, hoặc cầm máy lắc đều tay
> vài chục lần để cảm biến đếm bước — mỗi bước "giả" vẫn làm số tăng.

### TC-STEP-01 — Thẻ xin quyền `ACTIVITY_RECOGNITION` trên Trang chủ
- **Ưu tiên:** P1
- **Tiền điều kiện:** `pm clear com.healthyai.app`; máy/emulator **có** cảm biến đếm bước.
- **Bước:** 1. Mở app (qua cổng đăng nhập hoặc chế độ khách) → 2. Ở **Trang chủ**, tìm thẻ **"Bật đếm bước chân"** → 3. Bấm **"Bật đếm bước chân"** → 4. Chọn **Cho phép** trong hộp thoại của hệ thống.
- **Kết quả mong đợi:**
  - Thẻ có tiêu đề **"Bật đếm bước chân"**, mô tả nói rõ app đọc số bước từ cảm biến của điện thoại **và** luật đếm: *"Số bước chỉ được đếm trong lúc HealthyAI đang mở hoặc còn chạy nền; đóng hẳn ứng dụng thì app không đếm tiếp (số đã đếm trong ngày vẫn được giữ)."*
  - Sau khi **Cho phép**: thẻ **biến mất**, số bước trên thẻ **"Tóm tắt sức khỏe"** bắt đầu do cảm biến cung cấp (`stepCounter.isReady` = `true`).
  - Từ chối quyền → thẻ **vẫn hiện** (có dòng *"Không thấy hộp thoại? Mở cài đặt quyền"*).
- **Ghi chú:** `HomeScreen` phát danh mục qua `viewModel.onStepPermissionResult()` → `loadStepCounter()` đọc lại trạng thái **thật** (thẻ tự ẩn nếu quyền đã có ở nơi khác).

### TC-STEP-02 — Số bước tăng khi app đang mở
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền `ACTIVITY_RECOGNITION`.
- **Bước:** 1. Mở **Trang chủ** → ghi lại số ở ô **"Bước chân"** → 2. Đi bộ (hoặc lắc máy cho cảm biến đếm) khoảng 100–300 bước, **giữ app đang mở** → 3. Quay lại Trang chủ.
- **Kết quả mong đợi:** Số bước tăng lên tương ứng (chênh lệch xấp xỉ số bước vừa đi — cảm biến gộp theo lô nên có thể trễ vài giây). Số **không** nhảy vọt lên giá trị luỹ kế từ lúc bật máy.
- **Ghi chú:** Lần mở app đầu tiên **không** cộng bước đi trước đó: mẫu đầu tiên chỉ đặt mốc (`baseline = cumulative`) ⇒ số hiện `0` rồi mới tăng dần (`StepCounterMathTest`).

### TC-STEP-03 — Máy không có cảm biến đếm bước
- **Ưu tiên:** P2
- **Tiền điều kiện:** Emulator **không** có cảm biến `stepcounter` (hoặc máy thật không trang bị).
- **Bước:** 1. Mở **Trang chủ**.
- **Kết quả mong đợi:** **Không** có thẻ "Bật đếm bước chân" nào (không có gì để bật); ô "Bước chân" lấy theo Health Connect (nếu có) hoặc `0`/`--`; app **không** crash.

### TC-STEP-04 — **Đóng hẳn app thì KHÔNG đếm tiếp** (mục 3 #44) ⭐
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã cấp quyền `ACTIVITY_RECOGNITION`; bật sẵn **"Hiển thị tất cả ANR"** không cần.
- **Bước:**
  1. Mở app ở **Trang chủ** → đi bộ (hoặc lắc máy) khoảng **300** bước trong lúc app đang mở → xác nhận số bước tăng ~`300`.
  2. **Đóng hẳn app**: vuốt app khỏi **màn hình tác vụ gần đây** (recents) — hoặc `adb shell am force-stop com.healthyai.app`.
  3. Đi bộ thêm **500** bước khi app **không** chạy.
  4. Mở lại app → xem ô **"Bước chân"** ở Trang chủ.
- **Kết quả mong đợi:**
  - Bước (4): số bước **vẫn là ~300** như lúc đóng app — **KHÔNG** cộng thêm 500 bước đi lúc app không chạy.
  - Đi bộ thêm ~200 bước **trong lúc app đang mở** → số tăng lên ~`500` (đếm tiếp từ mốc lúc mở lại).
- **Ghi chú:** Đây chính là hành vi người dùng yêu cầu. Cơ chế: prefs `healthy_ai_step_counter` có khoá `session_id` sinh **một lần cho mỗi tiến trình app** (`StepCounterStore.sessionId`); mở lại app = phiên mới ⇒ `StepCounterMath` đặt lại `baseline = cumulative` (giá trị luỹ kế hiện tại) và **giữ** `offset` = số đã đếm trong ngày. Kiểm chứng tự động: `StepCounterMathTest` — `steps walked while the app was closed are never counted`, `reopening the app in the same day keeps the steps already counted`.

### TC-STEP-05 — App ở **nền** (còn trong tác vụ) thì vẫn đếm
- **Ưu tiên:** P0
- **Tiền điều kiện:** Đã cấp quyền `ACTIVITY_RECOGNITION`; đang ở **Trang chủ**.
- **Bước:**
  1. Ghi lại số bước → bấm **Home** (hoặc chuyển sang app khác) để app **xuống nền** (KHÔNG vuốt khỏi recents) → đi bộ ~200 bước → quay lại app.
- **Kết quả mong đợi:** Số bước **tăng** ~`200` — app còn trong tác vụ thì vẫn tính (đúng như yêu cầu: "app được chạy nền").
- **Ghi chú:** Nếu máy đã **ngủ sâu/đóng băng tiến trình** (Doze, cached-app freezer), cảm biến vẫn đếm ở phần cứng và mẫu luỹ kế được giao lại khi tiến trình chạy ⇒ vẫn **cùng phiên** nên khi quay lại app số bước sẽ cập nhật (có thể trễ vài giây). Bước chân đi trong lúc app ở nền mà **hệ thống đã thu hồi tiến trình** thì bị coi là phiên mới (không tính) — xem TC-STEP-04.

### TC-STEP-06 — Sang ngày mới khi app **vẫn đang chạy** → chỉ tính phần sau nửa đêm
- **Ưu tiên:** P2
- **Tiền điều kiện:** Đã cấp quyền; app **không** bị đóng qua nửa đêm (để ở nền hoặc mở).
- **Bước:** 1. Trước nửa đêm: đi bộ vài bước để có số trong ngày hôm trước → 2. Sau 00:00 (vẫn không đóng app): đi bộ ~150 bước → 3. Xem ô "Bước chân".
- **Kết quả mong đợi:** Số bước **về 0** rồi tăng theo phần đi **sau** nửa đêm (không cộng dồn số của hôm trước). Phần đi đúng lúc giao ngày được **nội suy** giữa hai mẫu cảm biến nên không bị "mất" cả ngày; số không âm.
- **Ghi chú:** Nội suy chỉ dùng **khi hai mẫu thuộc cùng một phiên** — tức app thật sự còn chạy. `StepCounterMathTest` — `day rollover inside a running session estimates the midnight baseline by interpolation`.

### TC-STEP-07 — Máy **khởi động lại** giữa ngày → số bước không tụt
- **Ưu tiên:** P2
- **Bước:** 1. Trong ngày, đi bộ để ô "Bước chân" có số (ví dụ `1 200`) → 2. **Khởi động lại máy** → 3. Mở lại app.
- **Kết quả mong đợi:** Số bước **không tụt** về 0 — phần đã đếm trước khi khởi động lại được giữ (`offset`), rồi đếm tiếp từ mẫu mới (bộ đếm cảm biến đã reset về 0). Số không âm.
- **Ghi chú:** Khởi động lại máy luôn kết thúc phiên cũ, nên đây cũng là ca "phiên mới"; khác biệt duy nhất là bộ đếm phần cứng nhỏ hơn mẫu trước (`StepCounterMathTest` — `a counter reset inside the same session keeps the steps already walked`).

### TC-STEP-08 — Health Connect vẫn **thắng** khi có số bước
- **Ưu tiên:** P1
- **Tiền điều kiện:** Đã cấp quyền Health Connect **và** có dữ liệu bước chân trong Health Connect (xem **mục 2.5** để tạo dữ liệu giả); cũng đã cấp `ACTIVITY_RECOGNITION`.
- **Bước:** 1. Mở **Trang chủ** → xem ô **"Bước chân"**.
- **Kết quả mong đợi:** Số hiển thị là số của **Health Connect** (có thể gồm cả đồng hồ đeo tay), **không** phải số của cảm biến máy. Cùng luật ở tab Phân tích / `health_data_history` (`(healthConnect.steps ?: 0).takeIf { it > 0 } ?: sensorSteps`).
- **Ghi chú:** `HomeUiState.displaySteps` = số cảm biến khi `stepCounter.isReady`, còn `HealthDataRepositoryImpl.syncHealthData()` ưu tiên Health Connect — hai chỗ cùng một thứ tự ưu tiên.

### TC-STEP-09 — Worker ghi lịch sử sức khỏe vẫn lấy được số của cảm biến
- **Ưu tiên:** P2
- **Bước:** 1. Đã cấp quyền và có vài bước trong ngày → 2. Chạy tay `HealthSyncWorker` (**mục 2.4**) → 3. Kiểm tra `health_data_history` của hôm nay (**mục 2.3**).
- **Kết quả mong đợi:** Hàng hôm nay có cột bước chân **> 0** (lấy từ `StepCounterRepository.readTodaySteps()` khi Health Connect trống) và xuất hiện ở thẻ **"Lịch sử sức khỏe 7 ngày"** (tab Phân tích).
- **Ghi chú:** Worker chạy trong **tiến trình của app**: nếu lúc đó app không chạy, tiến trình mới này **mở một phiên** và mốc đếm được đặt tại thời điểm đó — theo đúng luật "chỉ đếm khi app/app-nền đang chạy". Việc ghi lịch sử **không** làm số bước "tự nhảy" thêm cho khoảng thời gian app đã đóng trước đó.

### TC-STEP-10 — Unit test JVM cho luật đếm bước
- **Ưu tiên:** P1
- **Bước:** 1. `.\gradlew test --tests "*StepCounterMathTest"`.
- **Kết quả mong đợi:** **12 test** chạy qua (10 trước mục 3 #44), chốt đúng luật mới:
  - **mẫu đầu tiên không cộng bước đi trước khi app chạy** (`first sample starts counting from now …`);
  - bước cộng dồn trong cùng một phiên; **đóng app rồi mở lại trong cùng ngày → giữ số đã đếm**;
  - **bước đi lúc app đã đóng không bao giờ được cộng** (`steps walked while the app was closed are never counted`);
  - dữ liệu bản cũ (không có `sessionId`) được coi là **phiên khác**;
  - mở lại app sang **ngày mới** → bắt đầu lại từ 0;
  - **sang ngày mới trong lúc phiên đang chạy** → nội suy mốc 00:00 (chỉ tính phần sau nửa đêm);
  - bộ đếm bị reset (máy khởi động lại) → **giữ** phần đã đếm, số **không âm**;
  - `storedTodaySteps` chỉ hiện số **của đúng hôm nay**.
- **Ghi chú:** `StepCounterMath` **thuần Kotlin** (chỉ dùng `java.time`) nên test chạy trên JVM, không cần cảm biến. Phần Android (đăng ký `SensorEventListener`, `SharedPreferences`) **không** kiểm thử được trên JVM — theo dõi bằng tay ở TC-STEP-04/05.

---

**Hết tài liệu.**
