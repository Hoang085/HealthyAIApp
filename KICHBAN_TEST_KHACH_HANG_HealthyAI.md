# KỊCH BẢN TEST TRẢI NGHIỆM KHÁCH HÀNG — HealthyAI

> Tài liệu này **không phải** danh sách test case kỹ thuật (xem `TESTCASE_HealthyAI.md` cho việc đó).
> Đây là **kịch bản đóng vai khách hàng**: bạn cầm điện thoại, làm những việc một người dùng thật sẽ làm,
> và ghi lại cảm nhận + bằng chứng khi có gì đó sai.

| Thông tin | Giá trị |
|:--|:--|
| Phiên bản tài liệu | 1.0 |
| Ngày tạo | 2026-09-24 |
| Nguồn đối chiếu | mã nguồn tại `E:\HealthyAIApp` — branch `feature/dev-ui`, commit `18c4198` |
| Ứng dụng | HealthyAI (`com.healthyai.app`), `versionName 1.0.0`, Android `minSdk 26` |
| Người thực hiện | .......................... |
| Thiết bị / API level | .......................... |
| Số kịch bản | **80** (chia 11 hành trình) |

---

## 1. Đọc gì trước khi bắt đầu

### 1.1. Ba quy tắc khi test theo vai khách hàng

1. **Chỉ ghi điều bạn thực sự thấy.** Không suy đoán nguyên nhân kỹ thuật — bạn là khách hàng, không phải lập trình viên.
2. **Mỗi kịch bản test trọn vẹn rồi mới sang kịch bản khác.** Nếu bị kẹt, ghi lại và đi tiếp, đừng cố sửa.
3. **Có gì lạ thì chụp ảnh màn hình ngay.** Một ảnh chụp lúc lỗi đáng giá hơn 10 câu mô tả.

### 1.2. Ký hiệu mức độ

| Ký hiệu | Nghĩa |
|:--|:--|
| 🔴 | **Bắt buộc** — nếu sai thì khách hàng không dùng được app (crash, mất dữ liệu, không vào được) |
| 🟠 | **Quan trọng** — luồng chính một người dùng thật sẽ đi qua |
| ⚪ | **Tùy chọn** — trường hợp hiếm, máy đặc biệt, hoặc kiểm tra thêm |

### 1.3. Cách ghi kết quả

Mỗi kịch bản ghi một trong bốn trạng thái: **Đạt** / **Không đạt** / **Chưa test được** / **Không áp dụng**.
Ghi vào **bảng theo dõi ở mục 8**, kèm ảnh chụp nếu không đạt.

---

## 2. Chuẩn bị (khoảng 15 phút)

### 2.1. Bạn cần có

| Hạng mục | Yêu cầu | Ghi chú |
|:--|:--|:--|
| Điện thoại / emulator | Pixel 7, **API 34, có Google Play** | Cần Google Play để cài Health Connect |
| Bản cài app | APK debug `app-debug.apk` (đã có sẵn: `app/build/outputs/apk/debug/app-debug.apk`) | Nếu chưa có, mở project bằng Android Studio → Run ▶️ |
| Mạng Internet | Cần cho **đăng nhập/đăng ký** và đồng bộ Firebase | Không cần cho thói quen, Health Connect, Camera, AI |
| Nhiệt độ pin | Trên 20%, tốt nhất đang sạc | Một số tác vụ nền chỉ chạy khi pin không yếu |

### 2.2. Cài app

1. Cài APK: kéo–thả file `app-debug.apk` vào emulator, hoặc `adb install -r app-debug.apk`.
2. Nếu máy đã có bản cũ, **cài đè** (không gỡ app) — đây chính là kịch bản **KB-67** (nâng cấp không mất dữ liệu).

### 2.3. Ba thứ nên chuẩn bị trước

1. **Một tài khoản email test** (ví dụ `healthyai.test01@gmail.com`) + mật khẩu ≥ 6 ký tự, để test đăng nhập/đăng ký.
   Nếu app báo *"Phương thức Email/Mật khẩu chưa được bật trên Firebase Console"* hoặc *"Không có kết nối mạng"*: ghi lại, rồi **bỏ qua** bằng chế độ khách và tiếp tục — mọi thứ khác vẫn test được.
2. **Health Connect Toolbox** (cài từ Google Play trên emulator) — đây là "máy bơm" dữ liệu giả để xem chỉ số sức khỏe (xem **phụ lục B**).
3. **Chỗ trống trên màn hình chính** để thêm widget "Tiến độ thói quen".

### 2.4. Trước mỗi vòng test, làm sạch để bắt đầu từ số 0

| Cách | Kết quả |
|:--|:--|
| Gỡ app rồi cài lại | Sạch hoàn toàn: về màn Đăng nhập, **mất** thói quen và nhật ký (dữ liệu nằm trên máy) |
| Bấm icon **Đăng xuất** ở Trang chủ | Chỉ xoá phiên đăng nhập — **giữ nguyên** thói quen/nhật ký |

> Nếu bạn muốn test lại từ đầu nhưng không muốn mất dữ liệu đã tạo, chỉ cần **đăng xuất** rồi tạo lại tài khoản mới — dữ liệu vẫn còn.

---

## 3. Nhân vật & hành trình

Bạn sẽ lần lượt "đóng vai" 4 khách hàng dưới đây. Mỗi nhân vật có tính cách khác nhau — và người khó tính nhất thường tìm ra lỗi nhiều nhất.

| Nhân vật | Là ai | Mong muốn | Hành trình |
|:--|:--|:--|:--|
| **Lan** | Nhân viên văn phòng, 30 tuổi, mới tải app | Không muốn tạo tài khoản, chỉ muốn theo dõi uống nước và giấc ngủ | A, B |
| **Hùng** | Chạy bộ 3 buổi/tuần, đã dùng Health Connect | Số bước chân/giấc ngủ phải tự động hiện, thói quen phải đúng theo buổi | B, D, H |
| **Mai** | Quan tâm "app có thông minh không" | Muốn app mách nước và ghi chép bữa ăn bằng camera | F, G |
| **Bảo** | Khó tính: từ chối mọi quyền, tắt thông báo, dùng chế độ tối, gõ sai tùm lum | App không được crash dù bị từ chối mọi thứ | C, E, I, J |

---

## 4. Kịch bản

### Hành trình A — "Ngày đầu tiên của Lan"

**KB-01 · Mở app lần đầu** 🔴
- **Bạn đang ở:** vừa cài app xong, chưa từng mở.
- **Bạn làm:** bấm icon **HealthyAI**.
- **Bạn sẽ thấy:** một khoảnh khắc rất ngắn có vòng xoay + chữ "HealthyAI", sau đó là màn **Đăng nhập**: logo trái tim xanh, chữ "HealthyAI", dòng "Theo dõi & cải thiện thói quen sức khỏe", 2 ô **Email** / **Mật khẩu**, nút **Đăng nhập** (đang mờ), dòng "Chưa có tài khoản? Đăng ký", và nút **"Bỏ qua, dùng ở chế độ khách"** kèm dòng giải thích nhỏ bên dưới.
- **Đáng ngờ nếu:** app đứng ở vòng xoay quá 5 giây, trắng màn hình, hoặc thoát ra ngay.

**KB-02 · Xem mật khẩu đã gõ** ⚪
- **Bạn làm:** gõ mật khẩu, bấm biểu tượng **con mắt** trong ô Mật khẩu.
- **Bạn sẽ thấy:** mật khẩu hiện thành chữ thường; bấm lần nữa thì trở lại dấu chấm.
- **Đáng ngờ nếu:** nút không phản ứng, hoặc hiện luôn mật khẩu khi chưa bấm.

**KB-03 · Gõ sai khi đăng nhập** 🟠
- **Bạn làm:** (1) gõ email `abc` → bấm Đăng nhập. (2) gõ email `abc@gmail.com` + mật khẩu `123` → bấm Đăng nhập. (3) gõ email đúng + mật khẩu sai → bấm Đăng nhập.
- **Bạn sẽ thấy:** thẻ lỗi đỏ ngay dưới ô mật khẩu, lần lượt: **"Email không hợp lệ"** → **"Mật khẩu phải có ít nhất 6 ký tự"** → **"Email hoặc mật khẩu không đúng"** (hoặc *"Không có kết nối mạng, vui lòng thử lại"* nếu máy đang offline). Thẻ lỗi có nút **X** để đóng, và khi bạn gõ lại thì lỗi tự mất.
- **Đáng ngờ nếu:** app đóng im lặng không phản hồi, hoặc báo lỗi kỹ thuật khó hiểu kiểu "Xác thực thất bại (ERROR_...)" khi lỗi là do bạn gõ sai.

**KB-04 · Tạo tài khoản mới** 🟠
- **Bạn làm:** bấm "Chưa có tài khoản? Đăng ký" → gõ email + mật khẩu → bấm **Đăng ký**.
- **Bạn sẽ thấy:** nút đổi thành vòng xoay trong lúc chờ, rồi app vào thẳng **Trang chủ**. Lời chào có **2 dòng**: "Chào buổi sáng," và dòng tên màu xanh = **phần trước dấu `@`** của email (ví dụ email `lan.nguyen@gmail.com` → hiện `lan.nguyen`).
- **Đáng ngờ nếu:** hiện tên người khác, hiện chữ "Alex", hoặc vẫn đứng ở màn Đăng nhập dù đã tạo được tài khoản.
- **Lưu ý:** nếu tài khoản đã tồn tại, app phải báo **"Email này đã được đăng ký, hãy đăng nhập"**.

**KB-05 · Vào app không cần tài khoản (như Lan)** 🔴
- **Bạn làm:** ở màn Đăng nhập, bấm **"Bỏ qua, dùng ở chế độ khách"**.
- **Bạn sẽ thấy:** vào thẳng Trang chủ, lời chào **chỉ có 1 dòng** ("Chào buổi sáng") và **không có dòng tên màu xanh** nào.
- **Đáng ngờ nếu:** vẫn hiện một dòng tên (nhất là "Alex"), hoặc hiện dấu phẩy treo lơ lửng ở cuối câu chào.

**KB-06 · Trang chủ khi chưa có gì** 🟠
- **Bạn đang ở:** Trang chủ, chưa tạo thói quen, chưa cấp quyền gì.
- **Bạn sẽ thấy, từ trên xuống:** ngày hôm nay viết bằng tiếng Việt → lời chào → icon **Đăng xuất** và avatar xám ở góc phải → thẻ **"Tóm tắt sức khỏe"** với 3 vòng tròn **Bước chân 0.0k / Ngủ 0h 0m / Tim 0 bpm** → thẻ xin quyền Health Connect → thẻ nhắc bật thông báo → tiêu đề **"Thói quen hôm nay"** → thẻ **"Thêm thói quen đầu tiên"**. Thanh dưới cùng có **4 tab: Trang chủ · Thói quen · Ghi chép · Phân tích**.
- **Đáng ngờ nếu:** app hiện **banner/thẻ lỗi đỏ** ngay khi vừa cài (thẻ xin quyền màu xanh là bình thường), hoặc có tab bấm vào là trắng màn hình.

**KB-07 · Tạo thói quen đầu tiên** 🔴
- **Bạn làm:** tab **Thói quen** → bấm nút tròn **+** (dưới phải) → màn hình mới mở với tiêu đề **"Thêm thói quen mới"** → gõ tên `Uống nước` → chọn ô **💧 Uống nước** → nhập mục tiêu `2000` → để nguyên **Cả ngày** và **Hàng ngày** → bấm **"Tạo thói quen"**.
- **Bạn sẽ thấy:** quay về danh sách, thẻ `Uống nước` xuất hiện với dòng **"0.0L / 2.0L"** và nút xanh **"Ghi"**. Đơn vị tự đổi thành **ml** khi bạn chọn danh mục Uống nước (đơn vị hiển thị ngay bên phải ô mục tiêu).
- **Đáng ngờ nếu:** app giữ nguyên màn thêm thói quen, hoặc báo lỗi dù đã nhập đủ, hoặc thói quen không xuất hiện trong danh sách.

**KB-08 · Tạo một thói quen cho mỗi danh mục** 🟠
- **Bạn làm:** tạo thêm 4 thói quen theo bảng dưới đây.

  | Tên bạn gõ | Danh mục chọn | Mục tiêu | Buổi |
  |:--|:--|:--|:--|
  | `Ngủ đủ giấc` | 😴 Giấc ngủ | `8` | Tối |
  | `Chạy bộ buổi sáng` | 🏃 Vận động | `30` | Sáng |
  | `Ăn salad` | 🥗 Dinh dưỡng | `500` | Chiều |
  | `Thiền 15 phút` | 🧘 Thiền định | `15` | Sáng |

- **Bạn sẽ thấy:** đơn vị tự đổi theo danh mục — Giấc ngủ → **giờ**, Vận động → **phút**, Dinh dưỡng → **calo**, Thiền định → **phút**. Danh sách có đủ 5 thói quen, mới tạo nằm trên cùng.
- **Đáng ngờ nếu:** đơn vị sai (ví dụ Vận động hiện "ml"), hoặc không chọn được buổi/tần suất.

---

### Hành trình B — "Một ngày dùng app của Lan"

**KB-09 · Ghi nhận một lần uống nước** 🔴
- **Bạn làm:** tab Thói quen → bấm nút **"Ghi"** trên thẻ `Uống nước` → hộp thoại **"Ghi nhận Uống nước"** hiện ra với ô số **điền sẵn `2000`** (không có đuôi `.0`) → xoá và gõ `1000` → bấm **Lưu**.
- **Bạn sẽ thấy:** hộp thoại đóng, một dòng chữ nhỏ nổi lên **"Đã ghi nhận: Uống nước"**, và thẻ đổi thành **"1.0L / 2.0L"**.
- **Đáng ngờ nếu:** không có thông báo nào, hoặc tiến độ không đổi, hoặc ô mặc định hiện `2000.0`.

**KB-10 · Ghi đủ và đạt mục tiêu** 🔴
- **Bạn làm:** bấm **"Ghi"** thêm một lần nữa, nhập `1000`, Lưu.
- **Bạn sẽ thấy:** nút "Ghi" biến thành ô xám chữ **"Xong"**, phía trên là dòng xanh **"Hoàn thành"**, và icon tròn bên trái đổi thành dấu ✓.
- **Đáng ngờ nếu:** vẫn cho bấm "Ghi" và tiến độ vượt quá mục tiêu mà không báo gì (ghi lại để đối chiếu, đây là điểm cần thống nhất với nhóm sản phẩm).

**KB-11 · Ghi nhận vận động theo từng lần** 🟠
- **Bạn làm:** ghi `10` phút cho `Chạy bộ buổi sáng` **ba lần**.
- **Bạn sẽ thấy:** sau lần 1 thẻ hiện `10 / 30 phút`, lần 2 hiện `20 / 30 phút`, lần 3 thẻ chuyển hẳn sang **"Hoàn thành"** + nút **"Xong"**.
- **Đáng ngờ nếu:** phải ghi 1 lần 30 phút mới đạt (tức là app không cộng dồn), hoặc lần ghi sau ghi đè lần trước.

**KB-12 · Lọc thói quen theo buổi** 🟠
- **Bạn làm:** ở tab Thói quen, bấm lần lượt các chip **Tất cả · Sáng · Chiều · Tối**.
- **Bạn sẽ thấy:** "Sáng" chỉ hiện thói quen đặt **Buổi sáng** và **Cả ngày** (ví dụ `Uống nước` luôn hiện vì để Cả ngày); "Chiều"/"Tối" tương tự; "Tất cả" hiện đủ 5.
- **Đáng ngờ nếu:** chip không đổi trạng thái khi bấm, hoặc lọc ra danh sách trống dù có thói quen đúng buổi.

**KB-13 · Sửa mục tiêu của một thói quen** 🟠
- **Bạn làm:** bấm vào thẻ `Chạy bộ buổi sáng` → màn **"Sửa thói quen"** → ô mục tiêu điền sẵn `30` → đổi thành `45` → bấm **"Cập nhật"**.
- **Bạn sẽ thấy:** quay về danh sách, thẻ hiện `30 / 45 phút` (không còn "Hoàn thành" vì mục tiêu cao hơn).
- **Đáng ngờ nếu:** ô mục tiêu hiện `30.0`, hoặc đổi mục tiêu xong mà danh sách vẫn số cũ.

**KB-14 · Huỷ giữa đường** ⚪
- **Bạn làm:** bấm **"Ghi"** trên một thói quen, gõ số khác, rồi bấm **Hủy**.
- **Bạn sẽ thấy:** hộp thoại đóng, tiến độ **không đổi**, không có thông báo "Đã ghi nhận".
- **Đáng ngờ nếu:** vẫn ghi nhận dù bạn đã bấm Hủy.

**KB-15 · Xoá một thói quen** 🟠
- **Bạn làm:** bấm vào thẻ `Ăn salad` → bấm icon **thùng rác đỏ** ở góc phải trên → đọc hộp thoại **"Xóa thói quen?"** → bấm **Xóa**.
- **Bạn sẽ thấy:** hộp thoại nói rõ *"Thói quen sẽ bị ẩn khỏi danh sách và không còn được tính vào tiến độ hôm nay. Nhật ký đã ghi vẫn được giữ lại trong máy."* → sau khi xoá: dòng chữ **"Đã xóa thói quen"**, quay về danh sách và thẻ đó biến mất khỏi tab Thói quen, khỏi Trang chủ và khỏi widget.
- **Đáng ngờ nếu:** thói quen vẫn còn, hoặc app báo lỗi, hoặc xoá luôn cả nhật ký cũ (kiểm tra bằng tab Phân tích ở **KB-53**).

**KB-16 · Đổi ý, không xoá nữa** ⚪
- **Bạn làm:** vào một thói quen → bấm thùng rác → bấm **Hủy**.
- **Bạn sẽ thấy:** hộp thoại đóng, thói quen còn nguyên, không có thông báo "Đã xóa".

**KB-17 · Dùng lại tên của thói quen đã xoá** ⚪
- **Bạn làm:** tạo thói quen mới tên `Ăn salad` (tên vừa xoá ở KB-15) → bấm Tạo.
- **Bạn sẽ thấy:** tạo được bình thường, không báo trùng tên.
- **Đáng ngờ nếu:** báo "Tên thói quen đã tồn tại" cho một thói quen bạn đã xoá.

**KB-18 · Chống trùng tên thói quen** 🟠
- **Bạn làm:** tạo thói quen mới tên `uống NƯỚC` (khác hoa/thường, có khoảng trắng thừa) rồi bấm Tạo.
- **Bạn sẽ thấy:** hộp thoại **"Lỗi"** với nội dung **"Tên thói quen đã tồn tại"**, thói quen **không** được tạo.
- **Đáng ngờ nếu:** tạo được và bạn có 2 thói quen cùng tên (rất dễ ghi nhầm nhật ký về sau).

---

### Hành trình C — "Bảo là người gõ sai và thao tác lộn xộn"

**KB-19 · Tạo thói quen khi bỏ trống tên** 🟠
- **Bạn làm:** màn Thêm thói quen → **không gõ tên** → nhập mục tiêu `100` → bấm **Tạo thói quen**.
- **Bạn sẽ thấy:** hộp thoại **"Lỗi"**: **"Tên thói quen không được để trống"**, vẫn ở màn thêm thói quen.

**KB-20 · Mục tiêu không hợp lệ** 🟠
- **Bạn làm:** nhập tên `Test mục tiêu`, rồi lần lượt thử: để trống mục tiêu → bấm Tạo; nhập `0` → bấm Tạo; (nếu gõ được) nhập `-5` → bấm Tạo.
- **Bạn sẽ thấy:** mỗi lần đều báo **"Mục tiêu phải là số dương"** và **không** tạo thói quen.
- **Đáng ngờ nếu:** app tạo ra thói quen mục tiêu 0 rồi hiển thị "Hoàn thành" ngay (dấu hiệu tính sai).

**KB-21 · Ghi nhận khi bỏ trống số lượng** 🟠
- **Bạn làm:** bấm **"Ghi"** trên một thói quen → **xoá trắng** ô số lượng → bấm **Lưu**.
- **Bạn sẽ thấy:** hộp thoại **vẫn mở**, dòng lỗi đỏ dưới ô nhập: **"Vui lòng nhập số lượng"**; tiến độ không đổi.
- **Đáng ngờ nếu:** hộp thoại đóng im lặng mà không ghi gì và cũng không báo gì (bạn sẽ tưởng đã ghi thành công).

**KB-22 · Ghi nhận bằng chữ** ⚪
- **Bạn làm:** dán/gõ `abc` vào ô số lượng → Lưu.
- **Bạn sẽ thấy:** lỗi **"Số lượng phải là số"**, hộp thoại vẫn mở.

**KB-23 · Ghi nhận số 0 hoặc số âm** 🟠
- **Bạn làm:** lần lượt nhập `0` → Lưu; rồi nhập `-5` → Lưu.
- **Bạn sẽ thấy:** lỗi **"Số lượng phải lớn hơn 0"**, hộp thoại vẫn mở, tiến độ hôm nay **không giảm**.
- **Đáng ngờ nếu:** nhập số âm làm tiến độ tụt xuống (lỗi nghiêm trọng về dữ liệu).

**KB-24 · Bấm Lưu liên tục như người sốt ruột** 🟠
- **Bạn làm:** mở hộp thoại ghi nhận → **bấm Lưu thật nhanh nhiều lần**; rồi vào màn Sửa thói quen → sửa mục tiêu → **bấm "Cập nhật" thật nhanh nhiều lần**; cuối cùng ở màn Thêm thói quen mới → **bấm "Tạo thói quen" thật nhanh nhiều lần**.
- **Bạn sẽ thấy:** hộp thoại ghi nhận **đóng ngay** sau lần bấm đầu nên không thể ghi trùng; ở hai màn còn lại nút **mờ đi và hiện vòng xoay** trong lúc lưu, và kết quả chỉ có **một** thói quen/cập nhật.
- **Đáng ngờ nếu:** tiến độ tăng gấp nhiều lần (ví dụ ghi 1000ml mà thành 5000ml), hoặc danh sách xuất hiện **2 thói quen trùng tên**.

**KB-25 · Xoay màn hình khi đang nhập** 🟠
- **Bạn làm:** (1) Ở màn **Sửa thói quen**, gõ tên mới nhưng **chưa lưu** → xoay ngang màn hình. (2) Mở hộp thoại **ghi nhận** rồi xoay màn hình.
- **Bạn sẽ thấy (1):** nội dung bạn vừa gõ **vẫn còn**. **(2):** hộp thoại ghi nhận **có thể đóng lại** và bạn phải bấm "Ghi" lần nữa — ghi lại nếu điều này làm bạn khó chịu (đây là hiện trạng đã biết, không phải lỗi crash).
- **Đáng ngờ nếu:** app crash, hoặc mất luôn tên/mục tiêu đã gõ ở màn Sửa.

**KB-26 · Nút Back của điện thoại** 🟠
- **Bạn làm:** ở từng tab bấm Back; ở màn Thêm/Sửa thói quen bấm Back; ở Trang chủ bấm Back.
- **Bạn sẽ thấy:** ở màn Thêm/Sửa → quay về tab Thói quen (và thanh 4 tab dưới cùng **ẩn** ở màn này); ở một trong 4 tab → về Trang chủ; ở Trang chủ → thoát app.
- **Đáng ngờ nếu:** bấm Back làm app thoát ngay từ tab Phân tích, hoặc quay về màn Đăng nhập dù bạn chưa đăng xuất.

**KB-27 · Dùng app khi mất mạng hoàn toàn** 🟠
- **Bạn làm:** bật **chế độ máy bay** → tạo thói quen, ghi nhận, mở Camera, mở Phân tích → sau đó thử đăng nhập/đăng ký.
- **Bạn sẽ thấy:** thói quen/ghi nhận/Phân tích vẫn hoạt động bình thường; chỉ phần đăng nhập báo **"Không có kết nối mạng, vui lòng thử lại"**.
- **Đáng ngờ nếu:** thao tác thói quen bị treo hoặc chậm bất thường khi không có mạng.

---

### Hành trình D — "Hùng muốn số liệu sức khỏe tự hiện"

**KB-28 · Máy chưa cài Health Connect** 🟠
- **Bạn đang ở:** Trang chủ, máy không có app Health Connect.
- **Bạn sẽ thấy:** thẻ **"Chưa cài Health Connect"** với nút **"Cài Health Connect"**; 3 chỉ số hiển thị **0**; **không** có banner lỗi đỏ.
- **Bạn làm:** bấm **"Cài Health Connect"** → app mở Google Play.
- **Đáng ngờ nếu:** app crash, hoặc hiện banner đỏ báo lỗi kỹ thuật.

**KB-29 · Health Connect cần cập nhật** ⚪
- **Bạn sẽ thấy (nếu máy ở tình huống này):** thẻ **"Health Connect cần được cập nhật"** + nút **"Cập nhật Health Connect"**.

**KB-30 · Cấp quyền Health Connect** 🔴
- **Bạn làm:** ở Trang chủ, thẻ **"Cấp quyền Health Connect"** → bấm nút → hộp thoại của Health Connect hiện ra → bật **cả 4 quyền** (Bước chân, Giấc ngủ, Nhịp tim, Calories) → quay lại app.
- **Bạn sẽ thấy:** thẻ **biến mất** và số liệu **được nạp lại ngay** — bạn **không cần** mở lại app.
- **Đáng ngờ nếu:** thẻ vẫn còn sau khi đã bật đủ 4 quyền (thử rời Trang chủ rồi quay lại; nếu vẫn còn thì ghi lại).

**KB-31 · Cố tình cấp thiếu quyền** 🟠
- **Bạn làm:** trong hộp thoại Health Connect, chỉ bật Bước chân và Giấc ngủ, **tắt** Nhịp tim và Calories → quay lại.
- **Bạn sẽ thấy:** thẻ xin quyền **vẫn còn** để nhắc bạn; app **không** crash; không có banner lỗi đỏ.
- **Đáng ngờ nếu:** app crash hoặc màn Trang chủ trắng.

**KB-32 · Bơm dữ liệu giả và xem chỉ số** 🟠
- **Bạn làm:** làm theo **phụ lục B** để bơm steps = 3 500, giấc ngủ = 7 giờ 30 phút, nhịp tim = 72 → sau đó **tắt hẳn app rồi mở lại** (Cài đặt máy → Ứng dụng → HealthyAI → **Buộc dừng**, rồi mở app lại).
- **Bạn sẽ thấy:** thẻ "Tóm tắt sức khỏe" hiện **Bước chân 3.5k**, **Ngủ 7h 30m**, **Tim 72 bpm**.
- **Đáng ngờ nếu:** số vẫn là 0 **sau khi** đã bơm dữ liệu, đã cấp đủ quyền **và** đã mở lại app (ghi lại giờ bơm dữ liệu + ảnh chụp).
- **Điểm cần biết trước:** màn Trang chủ chỉ đọc lại số liệu khi **app được mở lại** hoặc khi **trạng thái quyền Health Connect vừa thay đổi**. Chuyển sang tab khác rồi quay lại Trang chủ **không** làm số liệu tự đọc lại — nên bạn sẽ thấy số **đứng im** nếu bơm dữ liệu trong lúc app đang mở (xem mục 5 và mục 6 #11).


**KB-33 · Từ chối quyền Health Connect** ⚪
- **Bạn làm:** ở hộp thoại Health Connect, bấm **Từ chối** (thử 2 lần).
- **Bạn sẽ thấy:** thẻ xin quyền vẫn hiện, app dùng bình thường với số liệu 0.
- **Đáng ngờ nếu:** bấm Từ chối mà hộp thoại cứ hiện lại liên tục không cho dùng app.

**KB-34 · Số liệu có nhớ sau khi tắt app** 🟠
- **Bạn làm:** sau KB-32, tắt hẳn app rồi mở lại.
- **Bạn sẽ thấy:** các chỉ số sức khỏe vẫn hiện (không quay về 0).

**KB-35 · Gợi ý dựa trên dữ liệu Health Connect** ⚪
- **Điều cần biết trước:** loại gợi ý này cần dữ liệu **nhiều ngày liên tiếp** (app tự đồng bộ 6 giờ/lần), nên **rất khó** thấy trong một buổi test.
- **Bạn sẽ thấy:** nếu chưa đủ dữ liệu, tab Phân tích chỉ hiển thị gợi ý từ nhật ký thói quen của bạn (xem hành trình G) — đây là **hiện trạng bình thường**, không phải lỗi.

---

### Hành trình E — "Bảo không muốn bị làm phiền"

**KB-36 · Bật nhắc nhở hàng ngày** 🟠
- **Bạn làm:** ở Trang chủ, thẻ **"Bật nhắc nhở hàng ngày"** → bấm **"Bật thông báo"** → hộp thoại hệ thống → **Cho phép**.
- **Bạn sẽ thấy:** thẻ **biến mất** ngay.
- **Đáng ngờ nếu:** thẻ vẫn còn sau khi đã bấm Cho phép (thử rời rồi quay lại Trang chủ).

**KB-37 · Từ chối quyền thông báo** 🟠
- **Bạn làm:** thẻ "Bật nhắc nhở hàng ngày" → **"Bật thông báo"** → **Từ chối**.
- **Bạn sẽ thấy:** thẻ vẫn còn để nhắc lại; app không crash; mọi tính năng khác bình thường.

**KB-38 · Tắt thông báo ở cài đặt máy** 🟠
- **Bạn làm:** vào **Cài đặt máy → Ứng dụng → HealthyAI → Thông báo** → tắt → quay lại app.
- **Bạn sẽ thấy:** thẻ đổi thành **"Thông báo đang bị tắt"** với nút **"Mở cài đặt thông báo"**; bấm nút này phải mở **đúng** màn cài đặt thông báo của HealthyAI.
- **Đáng ngờ nếu:** vẫn hiện nút "Bật thông báo" như lúc chưa cấp quyền, hoặc nút mở sai màn hình.

**KB-39 · Bật lại thông báo** ⚪
- **Bạn làm:** ở màn cài đặt vừa mở, bật thông báo lại → quay về app.
- **Bạn sẽ thấy:** thẻ nhắc **biến mất**.

**KB-40 · Kiểm tra thông báo nhắc nhở có thật** 🟠
- **Bạn làm:** làm theo **phụ lục C** để chạy job `daily_reminder_worker` (hoặc chờ đủ 24 giờ sau lần đầu mở app).
- **Bạn sẽ thấy:** một thông báo: tiêu đề **"HealthyAI 💪 Nhắc nhở thói quen"**, nội dung **"Bạn đã cập nhật thói quen hôm nay chưa? Chỉ cần vài giây thôi!"**, thuộc kênh **"Nhắc nhở thói quen sức khỏe"**. Bấm vào thông báo thì thông báo tự biến mất.
- **Đáng ngờ nếu:** job chạy mà không có thông báo **và** bạn đã cấp quyền thông báo (ghi lại kèm ảnh chụp màn hình Cài đặt thông báo).

**KB-41 · Thông báo có bị chặn khi chưa cấp quyền?** ⚪
- **Bạn làm:** đảm bảo đã **tắt** quyền thông báo → chạy job như KB-40.
- **Bạn sẽ thấy:** không có thông báo nào và app **không crash**.

---

### Hành trình F — "Mai muốn chụp ảnh bữa ăn"

**KB-42 · Lần đầu mở tab Ghi chép** 🟠
- **Bạn làm:** tab **Ghi chép** → thẻ **"Cần quyền Camera"** → bấm **"Cấp quyền Camera"** → **Cho phép**.
- **Bạn sẽ thấy:** màn hình có tiêu đề **"Ghi chép bữa ăn"**, dòng phụ *"Hướng camera vào món ăn để phân tích."*, khung camera xem trước với **khung vuông trắng** ở giữa và nhãn **"Quét món ăn"**, cùng nút xanh **"Phân tích món ăn"**.
- **Đáng ngờ nếu:** màn hình đen, hoặc nút "Phân tích món ăn" mờ vĩnh viễn.

**KB-43 · Từ chối quyền Camera** 🟠
- **Bạn làm:** ở thẻ xin quyền Camera, bấm **Từ chối**.
- **Bạn sẽ thấy:** thẻ xin quyền vẫn còn, app không crash, các tab khác dùng bình thường.
- **Đáng ngờ nếu:** app crash hoặc trắng màn hình khi không có quyền camera.

**KB-44 · Chụp và phân tích một món ăn** 🟠
- **Bạn làm:** hướng camera vào đồ ăn (trên emulator dùng "camera ảo"/webcam) → bấm **"Phân tích món ăn"**.
- **Bạn sẽ thấy:** nút đổi thành **"Đang phân tích..."** trong giây lát → xuất hiện thẻ **"Kết quả nhận diện"** với badge **"Tự động"** và các chip tên món + **phần trăm độ tin cậy** (app chỉ hiện kết quả có độ tin cậy trên 65%).
- **Đáng ngờ nếu:** quay mãi ở "Đang phân tích...", hoặc hiện chữ đỏ báo lỗi phân tích.
- **Lưu ý cho người test:** emulator thường dùng ảnh webcam/văn phòng nên kết quả có thể vô nghĩa (ví dụ "Garden", "Table") — **không phải lỗi**, app chỉ nhận diện được ảnh thật của đồ ăn.

**KB-45 · Đưa kết quả vào nhật ký** 🟠
- **Bạn làm:** ở thẻ kết quả, bấm **"Thêm vào nhật ký"** — thử **hai tình huống**:
  1. Khi bạn **chưa** có thói quen nào thuộc danh mục Dinh dưỡng.
  2. Sau khi bạn đã tạo một thói quen Dinh dưỡng (ví dụ `Ăn salad`).
- **Bạn sẽ thấy:**
  1. Dòng chữ nổi: **"Chưa có thói quen phù hợp — bấm nút + để tạo mới."**
  2. App **tự nhảy sang tab Thói quen** và **mở sẵn hộp thoại ghi nhận** của thói quen Dinh dưỡng đó → bấm Lưu → **"Đã ghi nhận: Ăn salad"** và tiến độ tăng.
- **Đáng ngờ nếu:** nhảy sang tab Thói quen mà không mở hộp thoại nào, hoặc ghi nhận sai thói quen (ví dụ ghi vào `Uống nước`).
- **Điểm cần ghi lại (trải nghiệm):** kết quả nhận diện **không được điền sẵn** vào ô số lượng — bạn vẫn phải tự nhập calo. Đây là điều một khách hàng sẽ thấy hụt hẫng.

**KB-46 · Camera dùng bình thường, không bị "treo"** ⚪
- **Bạn làm:** rời khỏi tab Ghi chép sang tab khác rồi quay lại vài lần; tắt app rồi mở lại vào tab Ghi chép.
- **Bạn sẽ thấy:** khung camera luôn mở lại được, không bị "camera đang bận" (không có thông báo *"Không mở được camera"* lặp lại).

---

### Hành trình G — "Mai kiểm tra xem app có thông minh không"

> Công thức tạo nhanh từng loại gợi ý nằm ở **phụ lục A** — dùng khi bạn muốn "dàn dựng" một gợi ý cụ thể.

**KB-47 · Tab Phân tích khi còn ít dữ liệu** 🟠
- **Bạn làm:** vào tab **Phân tích** khi mới cài app và chưa ghi nhận gì.
- **Bạn sẽ thấy:** tiêu đề **"Phân tích AI"** + *"Xu hướng sức khỏe của bạn, được phân tích để phát triển."*, rồi: thẻ **"Mọi thứ đang rất tốt!"**, thẻ **"Chỉ số sinh lực"** hiện **50/100** (và **không** có dòng so sánh tuần trước), thẻ **"Sự nhất quán hàng tuần"** báo *"Chưa có thói quen nào để theo dõi. Hãy tạo thói quen đầu tiên ở tab Thói quen."*, thẻ **"Xu hướng chất lượng giấc ngủ"** báo chưa có dữ liệu chi tiết pha giấc ngủ.
- **Đáng ngờ nếu:** app crash, hoặc hiện gợi ý "dọa" (ví dụ cảnh báo thiếu nước) khi bạn chưa ghi gì.

**KB-48 · Gợi ý "uống nước nguy hiểm thấp" (mức khẩn cấp)** 🟠
- **Bạn làm:** với thói quen `Uống nước` 2000ml, ghi **ba lần `300`** → mở tab Phân tích.
- **Bạn sẽ thấy:** thẻ có nhãn **"CẢNH BÁO KHẨN CẤP"** (chữ đỏ, thẻ có viền đỏ): **"🚨 Mức nước nguy hiểm thấp"** với nội dung *"Bạn uống trung bình 300ml/ngày — chỉ bằng 50% nhu cầu tối thiểu! Ngay lúc này hãy uống 1 ly nước đầy."* và nút **"Ghi nhận uống nước"**.
- **Bạn làm tiếp:** bấm nút đó.
- **Bạn sẽ thấy:** app sang **tab Thói quen** với **hộp thoại ghi nhận `Uống nước` mở sẵn** (ô số điền `2000`).
- **Đáng ngờ nếu:** bấm nút mà không có gì xảy ra, hoặc mở hộp thoại sai thói quen.

**KB-49 · Gợi ý "uống thêm nước" (mức trung bình)** ⚪
- **Bạn làm:** với máy vừa được làm sạch, ghi **một lần `1200`** cho `Uống nước` 2000ml → mở Phân tích.
- **Bạn sẽ thấy:** thẻ **"💧 Uống thêm nước mỗi ngày"** với *"Lượng nước trung bình của bạn là 1200ml/ngày. Mục tiêu khuyến nghị: 2000ml..."* và **không có nút hành động**.
- **Đáng ngờ nếu:** trộn lẫn số liệu (ví dụ nói "1200ml" khi bạn đã ghi 2 lần 600).

**KB-50 · Gợi ý thiếu ngủ và nút hành động** 🟠
- **Bạn làm:** với thói quen `Ngủ đủ giấc` 8 giờ, ghi **ba lần `5`** → mở Phân tích.
- **Bạn sẽ thấy:** thẻ **"⚠️ Cảnh báo thiếu ngủ nghiêm trọng"** với *"Bạn đã ngủ trung bình 5.0 tiếng/đêm trong 3 ngày qua..."* và nút **"Đặt nhắc nhở đi ngủ"**; bấm nút → sang tab Thói quen với hộp thoại ghi nhận của thói quen **Giấc ngủ**.
- **Đáng ngờ nếu:** bấm nút mà mở hộp thoại của thói quen khác (ví dụ `Uống nước`).

**KB-51 · Gợi ý vận động phải nói đúng đơn vị** 🟠
- **Bạn làm:** với thói quen `Chạy bộ buổi sáng` 30 phút, ghi **ba lần `10`** → mở Phân tích.
- **Bạn sẽ thấy:** thẻ **"🏃 Tăng hoạt động thể chất ngay hôm nay"** nói về **phút**: *"Mức vận động trung bình của bạn là 10 phút trong 3 lần ghi gần nhất — mới đạt 33% mục tiêu 30 phút..."* và nút **"Ghi nhận vận động"**.
- **Đáng ngờ nếu:** câu chữ nói **"bước"** trong khi bạn ghi bằng phút (đây từng là lỗi đã sửa — nếu gặp lại, báo ngay).
- **Ghi chú cho người test:** app hiện tự gán đơn vị theo danh mục (Vận động → *phút*), nên bạn khó tạo được thói quen đo bằng **bước chân**; những gợi ý "5 000 / 8 000 bước" chỉ xuất hiện với dữ liệu đơn vị bước — coi là giới hạn đã biết, không cần cố tạo.

**KB-52 · Gợi ý phải "hạ nhiệt" khi bạn tiến bộ** 🟠
- **Bạn làm:** tiếp tục sau KB-51, ghi thêm **hai lần `25`** cho `Chạy bộ buổi sáng` → mở lại Phân tích.
- **Bạn sẽ thấy:** thẻ đổi từ "🏃 Tăng hoạt động thể chất ngay hôm nay" (mức cao) thành **"🚶 Tăng dần mục tiêu vận động"** (mức thấp hơn), nội dung nói bạn đạt hơn 50% mục tiêu.
- **Đáng ngờ nếu:** vẫn giữ nguyên cảnh báo cũ (nghĩa là gợi ý không cập nhật theo dữ liệu mới).

**KB-53 · Khi mọi thứ đều ổn thì gợi ý biến mất** 🟠
- **Bạn làm:** lần lượt ghi đủ mục tiêu cho mọi thói quen — ví dụ: nước `2000` ba lần; chạy bộ `25` năm lần; ngủ `8` sáu lần — rồi mở Phân tích.
- **Bạn sẽ thấy:** các thẻ cảnh báo **lần lượt biến mất**; khi không còn gợi ý nào, app hiện thẻ **"Mọi thứ đang rất tốt!"** với *"Bạn đang duy trì thói quen rất đều đặn. Hãy tiếp tục phát huy nhé!"*.
- **Đáng ngờ nếu:** cảnh báo cũ vẫn còn dù đã ghi vượt mục tiêu (bạn có thể phải rời tab rồi quay lại — nếu vẫn còn thì ghi lại).

**KB-54 · Chỉ số sinh lực phản ánh mức hoàn thành** 🟠
- **Bạn làm:** **trên máy vừa được làm sạch**, tạo 3 thói quen `Ngủ đủ giấc` 8 giờ, `Uống nước` 2000ml, `Chạy bộ buổi sáng` 30 phút — ghi đủ **100% mục tiêu cả ba** trong ngày hôm nay (không ghi các mức thấp hơn cho 3 thói quen này) → mở Phân tích.
- **Bạn sẽ thấy:** vòng tròn **"Chỉ số sinh lực"** hiện **100/100** (nếu máy vừa làm sạch, chưa có dữ liệu tuần trước thì **không** hiện dòng so sánh). Khi bạn ghi thêm hoặc xoá bớt, điểm phải thay đổi **ngay**, không cần mở lại app.
- **Đáng ngờ nếu:** điểm đứng yên, hoặc hiện số ngoài khoảng 0–100.

**KB-55 · Biểu đồ "Sự nhất quán hàng tuần"** 🟠
- **Bạn làm:** nhìn thẻ **"Sự nhất quán hàng tuần"** sau khi đã ghi được 2/3 thói quen đạt mục tiêu hôm nay.
- **Bạn sẽ thấy:** 7 cột (T2…CN), cột **hôm nay được in đậm màu xanh** và hiện **67%**; chú thích *"Tỉ lệ thói quen đạt mục tiêu theo từng ngày (7 ngày gần nhất)"*.
- **Đáng ngờ nếu:** cột hôm nay luôn 0% dù bạn vừa đạt mục tiêu, hoặc biểu đồ có 8 cột.

**KB-56 · Xu hướng chất lượng giấc ngủ** ⚪
- **Bạn làm:** xem thẻ cuối màn Phân tích, trong hai trường hợp: (1) chưa cấp quyền Giấc ngủ cho Health Connect; (2) đã cấp và đã bơm dữ liệu ngủ có chia pha.
- **Bạn sẽ thấy:** (1) câu hướng dẫn *"Chưa có dữ liệu chi tiết pha giấc ngủ..."*; (2) ba thanh **Sâu / Nông / REM** kèm số giờ–phút và dòng *"Tổng … trong 7 đêm gần nhất"*.
- **Đáng ngờ nếu:** hiện ba thanh toàn số 0 hoặc tổng lớn hơn 24 giờ.

---

### Hành trình H — "Hùng muốn thấy tiến độ ngay trên màn hình chính"

**KB-57 · Thêm widget ra màn hình chính** 🟠
- **Bạn làm:** chạm giữ vùng trống trên màn hình chính → **Widget** → tìm **HealthyAI → "Tiến độ thói quen"** → kéo ra.
- **Bạn sẽ thấy:** widget có dòng tiêu đề **"Tiến độ hôm nay"**, dòng **"x/y thói quen đạt mục tiêu"**, một thanh tiến độ, và tối đa 3 dòng kiểu **"✓ Uống nước  2.0L / 2.0L"** hoặc **"○ Ngủ đủ giấc  0.0 / 8.0 giờ"**.
- **Đáng ngờ nếu:** widget trắng trơn, hoặc hiện chữ "Loading" mãi không đổi.

**KB-58 · Widget xếp đúng thói quen của buổi hiện tại** 🟠
- **Bạn làm:** có 5 thói quen thuộc các buổi khác nhau → nhìn thứ tự 3 dòng trên widget.
- **Bạn sẽ thấy:** thói quen **đúng buổi hiện tại** nằm đầu, rồi tới thói quen **Cả ngày**, rồi mới tới các thói quen khác; vì chỉ hiện được 3 dòng, dòng cuối có thể là **"+N thói quen khác"**.
- **Đáng ngờ nếu:** danh sách hiện thói quen bạn đã xoá, hoặc hiện quá 3 dòng làm nội dung bị cắt.

**KB-59 · Widget tự cập nhật** 🔴
- **Bạn làm:** ghi nhận một thói quen trong app → về màn hình chính; sau đó tạo thêm một thói quen mới → về màn hình chính; sau đó xoá một thói quen → về màn hình chính.
- **Bạn sẽ thấy:** widget đổi ngay: tiến độ tăng, số `x/y` và số dòng thay đổi — bạn **không** phải gỡ widget rồi thêm lại.
- **Đáng ngờ nếu:** widget vẫn hiện số cũ (chờ 1 phút; nếu vẫn cũ thì ghi lại — đây là lỗi từng gặp).

**KB-60 · Widget khi chưa có thói quen** ⚪
- **Bạn làm:** xoá hết thói quen → xem widget.
- **Bạn sẽ thấy:** dòng **"Healthy AI"** và **"Chưa có thói quen"**; chạm vào widget thì **app mở ra**.
- **Đáng ngờ nếu:** chạm vào widget không mở app.

**KB-61 · Kéo giãn widget** ⚪
- **Bạn làm:** kéo cho widget rộng/cao hơn, rồi thu nhỏ lại.
- **Bạn sẽ thấy:** nội dung hiện đủ hơn khi rộng ra; khi thu nhỏ có thể bị cắt bớt — chấp nhận được, miễn không vỡ chữ hay chồng lên nhau.

---

### Hành trình I — "Bảo kiểm tra tài khoản và dữ liệu"

**KB-62 · Đăng xuất** 🟠
- **Bạn làm:** ở Trang chủ, bấm icon **Đăng xuất** (góc phải, phía trên avatar) → hộp thoại **"Đăng xuất"** với nội dung *"Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên."* → (1) bấm **Huỷ**, (2) bấm lại và chọn **Đăng xuất**.
- **Bạn sẽ thấy:** (1) vẫn ở Trang chủ; (2) quay về màn **Đăng nhập**.
- **Đáng ngờ nếu:** bấm một lần là thoát ngay (không hỏi lại), hoặc bấm Huỷ mà vẫn bị đăng xuất.

**KB-63 · Dữ liệu còn nguyên sau khi đăng xuất** 🔴
- **Bạn làm:** sau khi đăng xuất, bấm **"Bỏ qua, dùng ở chế độ khách"** → mở tab Thói quen và Trang chủ.
- **Bạn sẽ thấy:** đủ thói quen và tiến độ hôm nay **y như trước khi đăng xuất**; lời chào **không có dòng tên**.
- **Đáng ngờ nếu:** thói quen/nhật ký biến mất (lỗi nghiêm trọng về dữ liệu).

**KB-64 · Đăng nhập lại bằng tài khoản cũ** 🟠
- **Bạn làm:** đăng xuất → đăng nhập lại bằng email/mật khẩu đã dùng.
- **Bạn sẽ thấy:** vào Trang chủ, lời chào có **dòng tên màu xanh** (phần trước `@` của email) và dữ liệu vẫn như cũ.
- **Đáng ngờ nếu:** hiện tên của người khác (nếu vừa đăng nhập tài khoản khác), hoặc mất dữ liệu vừa tạo.

**KB-65 · Mở lại app sau khi đã đăng nhập** 🟠
- **Bạn làm:** tắt app hoàn toàn (vuốt khỏi danh sách ứng dụng gần đây) → mở lại.
- **Bạn sẽ thấy:** app vào **thẳng Trang chủ**, không hỏi đăng nhập lại (chỉ nháy màn hình chờ rất ngắn).
- **Đáng ngờ nếu:** mất phiên đăng nhập ngay sau khi vừa đăng nhập xong.

**KB-66 · Chế độ khách có được "nhớ" không?** 🟠
- **Bạn làm:** đang ở chế độ khách, **vuốt app khỏi danh sách ứng dụng gần đây** → mở lại app.
- **Bạn sẽ thấy (hiện trạng):** app **quay về màn Đăng nhập** (chế độ khách không được ghi nhớ sau khi tắt hẳn app), nhưng **dữ liệu vẫn còn** — bạn chỉ cần bấm "Bỏ qua, dùng ở chế độ khách" lại.
- **Cách ghi:** đây **không phải crash**, nhưng là trải nghiệm dễ gây khó chịu cho khách hàng. Hãy ghi lại như một **đề xuất cải tiến** ("cho phép nhớ chế độ khách đã chọn").

**KB-67 · Cài bản mới đè lên bản đang dùng** 🔴
- **Bạn làm:** khi trong máy đã có thói quen + nhật ký, **cài đè** APK mới (không gỡ app) → mở app.
- **Bạn sẽ thấy:** thói quen, nhật ký và tiến độ hôm nay **còn nguyên**.
- **Đáng ngờ nếu:** dữ liệu biến mất sau khi nâng cấp (lỗi nghiêm trọng nhất của tài liệu này).

**KB-68 · Gỡ app rồi cài lại** ⚪
- **Bạn làm:** gỡ app (giữ lại rồi cài lại) → mở app.
- **Bạn sẽ thấy:** về màn Đăng nhập, dữ liệu trống — **đây là đúng**, vì dữ liệu nằm trên máy. Dùng cách này khi bạn muốn test lại từ đầu.

**KB-69 · Bảo mật dữ liệu trên máy** ⚪
- **Chỉ dành cho người test biết kỹ thuật:** dữ liệu lưu trong một tệp **đã mã hoá**, nên công cụ xem dữ liệu thông thường sẽ không mở được và **không được** dùng làm bằng chứng lỗi. Các bước kiểm tra mã hoá nằm ở **TC-DB-11…14** trong `TESTCASE_HealthyAI.md`.

---

### Hành trình J — "Bảo soi từng chi tiết: màu, cỡ chữ, vòng đời"

**KB-70 · Chế độ tối (dark mode)** 🟠
- **Bạn làm:** Cài đặt máy → Hiển thị → bật **Chủ đề tối** → mở app, đi qua cả 4 tab + màn Thêm/Sửa thói quen.
- **Bạn sẽ thấy:** nền màn hình tối, các thẻ cũng tối hơn nền một chút, **chữ đọc rõ ở mọi màn**; các thẻ gợi ý AI vẫn phân biệt được mức độ (thẻ khẩn cấp có viền đỏ).
- **Đáng ngờ nếu:** có thẻ nền trắng chữ trắng (chữ biến mất), hoặc chữ xám trên nền xám khó đọc. **Chụp ảnh ngay** — đây từng là lỗi đã sửa.

**KB-71 · Đổi sáng/tối khi app đang mở** ⚪
- **Bạn làm:** để app đang mở, tắt/bật chủ đề tối từ cài đặt máy rồi quay lại app.
- **Bạn sẽ thấy:** app không crash, màu đổi theo (có thể phải mở lại app).

**KB-72 · Cỡ chữ lớn** ⚪
- **Bạn làm:** Cài đặt máy → Hiển thị → **Cỡ chữ: Lớn nhất** → mở app, xem nút **"Tạo thói quen"**, tiêu đề các màn, và thẻ thói quen.
- **Bạn sẽ thấy:** chữ vẫn đọc được, nút vẫn bấm được. Nếu chữ bị **cắt cụt** ở nút hoặc bị chồng lên nhau, hãy ghi lại kèm ảnh (đây là quan sát trải nghiệm, mức độ thấp).

**KB-73 · Xoay màn hình ở mọi màn** 🟠
- **Bạn làm:** xoay ngang rồi xoay dọc ở: cả 4 tab, màn Thêm/Sửa thói quen, hộp thoại ghi nhận, màn Đăng nhập.
- **Bạn sẽ thấy:** không màn nào bị trắng/crash; dữ liệu đã nhập ở màn Sửa còn nguyên; danh sách vẫn cuộn được.
- **Đáng ngờ nếu:** mất dữ liệu đã nhập, hoặc app đóng đột ngột.

**KB-74 · Qua nửa đêm (kiểm tra "ngày mới")** 🟠
- **Bạn làm:** đặt giờ máy tới **00:05** hôm sau (hoặc chờ thật) → mở lại app.
- **Bạn sẽ thấy:** tiến độ "hôm nay" **trở về 0** (dù hôm qua đã đạt mục tiêu), tiêu đề tab Thói quen đổi sang ngày mới, lời chào đổi theo giờ (**Chào buổi đêm** cho khung 22:00–04:59), biểu đồ tuần **dồn sang cột mới** còn cột hôm qua giữ nguyên % đã đạt; dữ liệu hôm qua **không** mất (vẫn thấy trong biểu đồ tuần và Phân tích).
- **Đáng ngờ nếu:** nhật ký hôm qua bị xoá, hoặc tiến độ hôm qua vẫn còn tính vào hôm nay.

**KB-75 · Tắt app rồi mở lại — dữ liệu có nhớ?** 🔴
- **Bạn làm:** sau khi tạo thói quen và ghi nhận, tắt app (vuốt khỏi danh sách gần đây) → mở lại.
- **Bạn sẽ thấy:** (ở chế độ khách bạn phải bấm "Bỏ qua" lại — xem KB-66) thói quen và **tiến độ hôm nay vẫn đúng như trước**.
- **Đáng ngờ nếu:** tiến độ về 0 dù vẫn trong cùng một ngày (đây từng là lỗi đã sửa — báo ngay nếu gặp lại).

**KB-76 · "Khách hàng khó tính": từ chối mọi thứ** 🟠
- **Bạn làm:** gỡ app → cài lại → **từ chối** quyền thông báo, quyền Health Connect, quyền Camera; không đăng nhập; đi qua cả 4 tab, tạo thói quen, ghi nhận, xoay màn hình, bật chế độ tối.
- **Bạn sẽ thấy:** app **không crash ở bất kỳ đâu**, chỉ hiện các thẻ nhắc cấp quyền.
- **Đáng ngờ nếu:** có bất kỳ màn hình trắng, đứng im, hay app tự thoát.

---

### Hành trình K — "Bảo muốn app tự đếm bước, không cần app nào khác"

> Áp dụng từ bản có **đếm bước bằng cảm biến của máy**. Trên **emulator** (không có cảm biến đếm bước) hành trình này bỏ qua — xem KB-80.

**KB-77 · Bật quyền đếm bước chân** 🟠
- **Bạn đang ở:** Trang chủ, máy là **điện thoại thật** (có cảm biến đếm bước), app chưa được cấp quyền.
- **Bạn sẽ thấy:** thẻ **"Bật đếm bước chân"** (nội dung nói app đọc số bước trực tiếp từ cảm biến của điện thoại), nút **"Bật đếm bước chân"** và nút phụ *"Không thấy hộp thoại? Mở cài đặt quyền"*.
- **Bạn làm:** bấm **"Bật đếm bước chân"** → hộp thoại hệ thống **"Cho phép HealthyAI nhận diện hoạt động thể chất?"** → **Cho phép**.
- **Bạn sẽ thấy:** thẻ **biến mất ngay** và chỉ số **Bước chân** chuyển từ `0.0k` sang số thật (có thể mất vài giây để cảm biến trả về giá trị đầu tiên).
- **Đáng ngờ nếu:** máy thật có cảm biến mà **không** thấy thẻ; hoặc cấp quyền xong vẫn `0.0k` sau khi đi bộ (xem KB-78).

**KB-78 · Đi bộ và thấy số bước tự tăng** 🟠
- **Bạn làm:** cầm điện thoại đi bộ khoảng **50–100 bước** (trong nhà cũng được), quay lại Trang chủ.
- **Bạn sẽ thấy:** số ở ô **Bước chân** tăng lên, kèm dòng chú thích nhỏ **"Số bước đọc trực tiếp từ cảm biến của điện thoại"** (khi Health Connect không có số). Bạn **không** cần cài Google Fit / Samsung Health / đồng hồ gì cả.
- **Đáng ngờ nếu:** đi 100 bước mà số đứng im (chờ thêm ~15 giây rồi xem lại; vẫn im thì ghi lỗi kèm máy/phiên bản Android).

**KB-79 · Tắt app, khởi động lại máy rồi đi tiếp** 🟠
- **Bạn làm:** (1) tắt hẳn app → đi tiếp ~50 bước → mở lại app. (2) **Khởi động lại điện thoại** → mở app → đi tiếp ~50 bước → xem lại số.
- **Bạn sẽ thấy:** (1) số bước **cộng dồn đúng** phần đi lúc app đóng. (2) sau khi khởi động lại máy, số bước trong ngày **không bị tụt về 0** mà tiếp tục tăng.
- **Đáng ngờ nếu:** số bước về 0 sau khi khởi động lại máy (đây là ca khó nhất đã được xử lý riêng — nếu gặp lại, báo ngay).

**KB-80 · Máy không có cảm biến đếm bước (emulator)** ⚪
- **Bạn làm:** mở app trên **emulator** (hoặc máy không có cảm biến đếm bước).
- **Bạn sẽ thấy:** **không** có thẻ "Bật đếm bước chân" (không có gì để người dùng bật), và số bước vẫn lấy từ Health Connect như trước — muốn có số thì phải bơm dữ liệu (phụ lục B).
- **Đáng ngờ nếu:** app vẫn hiện thẻ xin quyền trên máy không có cảm biến, hoặc crash vì thiếu cảm biến.

---

## 5. Điều KHÔNG phải lỗi (giới hạn đã biết — đừng ghi thành lỗi)

| Hiện tượng bạn có thể gặp | Vì sao | Ghi thế nào |
|:--|:--|:--|
| Chỉ số sức khỏe = **0** dù máy có Health Connect | Bạn chưa cấp đủ 4 quyền, hoặc máy chưa có dữ liệu | Ghi "chưa cấp quyền/chưa có dữ liệu" |
| Tab Phân tích gần như trống trong buổi test đầu | Gợi ý theo dữ liệu Health Connect cần **nhiều ngày liên tiếp** | Ghi "chưa đủ dữ liệu để đánh giá" |
| Kết quả camera vô nghĩa ("Garden", "Table"…) | Đang chụp ảnh webcam/văn phòng thay vì đồ ăn thật | Không ghi là lỗi |
| Thẻ "Xu hướng chất lượng giấc ngủ" báo chưa có dữ liệu | Máy/nhà sản xuất không ghi được **pha** Sâu/Nông/REM | Không ghi là lỗi |
| Thông báo nhắc nhở chưa đến ngay | Job nhắc nhở chạy theo chu kỳ **24 giờ**, hơn nữa chỉ chạy khi **pin không yếu** | Dùng cách ở **phụ lục C** để chạy ngay |
| Không xem được dữ liệu bằng công cụ xem database | Dữ liệu lưu trên máy đã được **mã hoá** | Không ghi là lỗi |
| Chế độ khách không được nhớ sau khi tắt hẳn app | Hiện trạng (KB-66) | Ghi thành **đề xuất cải tiến** |
| Gỡ app rồi cài lại thì mất dữ liệu | Dữ liệu nằm trên máy, không có tài khoản để khôi phục | Ghi "đúng như thiết kế" |
| **Số liệu sức khỏe không đổi** dù vừa có dữ liệu mới trong Health Connect | Trang chủ chỉ đọc lại số liệu khi **mở lại app** hoặc khi **quyền Health Connect vừa thay đổi** — chuyển tab qua lại không kích hoạt đọc lại | Mở lại app rồi đo lại; nếu vẫn sai thì ghi lỗi |
| **Số nhịp tim = 0** dù Health Connect có dữ liệu nhịp tim | App chỉ đọc nhịp tim trong **60 phút gần nhất** | Bơm dữ liệu nhịp tim mới rồi mở lại app |
| **Số giấc ngủ = 0** dù Health Connect có phiên ngủ | App chỉ đọc phiên ngủ nằm trong khoảng **18:00 hôm qua → bây giờ** | Bơm phiên ngủ kết thúc trong khoảng đó |
| **Số bước chân = 0** dù bạn đã đi bộ | Nếu máy **có cảm biến đếm bước**: cấp quyền "Bật đếm bước chân" (KB-77) là app tự đếm. Nếu máy **không có cảm biến** (emulator): điện thoại không tự ghi bước vào Health Connect — phải có **app nguồn** (Samsung Health / Fitbit / Mi Fitness / Toolbox) đồng bộ vào | Xem **phụ lục B.3** và **KB-77…KB-80** |

---

## 6. Điều nên ghi lại dù chưa chắc là lỗi (quan sát trải nghiệm)

Đây là những điểm tôi thấy khi đọc app — bạn hãy **kiểm chứng bằng mắt** và ghi lại cảm nhận như một khách hàng:

1. **Thẻ "Thêm thói quen đầu tiên" ở Trang chủ không bấm được** (KB-06). Khách hàng mới rất dễ bấm vào đó và… không có gì xảy ra. Có nên cho bấm để mở màn tạo thói quen?
2. **Thẻ "Tóm tắt sức khỏe" có mũi tên mà không bấm được** (KB-06) — mũi tên thường khiến người dùng nghĩ là "chạm để xem chi tiết".
3. **Khi đã đạt mục tiêu, nút "Ghi" biến thành "Xong"** (KB-10) → bạn không ghi thêm được trong ngày. Hợp lý hay gây bí?
4. **Kết quả camera không điền sẵn số calo** (KB-45) → vẫn phải tự nhập tay.
5. **Tên widget "Healthy AI" viết khác tên app "HealthyAI"** (KB-60) — chi tiết nhỏ nhưng lệch nhận diện thương hiệu.
6. **Chữ "Hủy"/"Huỷ" viết không thống nhất giữa các hộp thoại** (hộp thoại ghi nhận dùng "Hủy", hộp thoại đăng xuất dùng "Huỷ").
7. **Bấm nút hành động trong thẻ gợi ý AI khi chưa có thói quen đúng danh mục** → chỉ hiện dòng nhắc *"Chưa có thói quen phù hợp — bấm nút + để tạo mới."*; có nên đưa thẳng tới màn tạo thói quen với danh mục đã chọn sẵn?
8. **Thói quen đã xoá vẫn ảnh hưởng Phân tích**: nhật ký cũ vẫn được tính vào gợi ý AI và Chỉ số sinh lực (vì app giữ lại nhật ký theo thiết kế xoá mềm). Nếu bạn thấy cảnh báo về một thói quen bạn đã xoá, hãy ghi lại để nhóm sản phẩm quyết định.
9. **Tần suất Hàng tuần / Hàng tháng có vẻ chưa ảnh hưởng gì** đến danh sách thói quen hôm nay (mọi thói quen đang bật đều hiện mỗi ngày). Hãy thử tạo một thói quen "Hàng tuần" rồi xem nó có hiện mỗi ngày không và ghi lại.
10. **Ô "Mục tiêu hàng ngày" dùng bàn phím số nguyên** nên khó nhập số lẻ (ví dụ 2.5 giờ) — thử xem bạn có gõ được `7.5` không (KB-13) và ghi lại.
11. **Số liệu sức khỏe "đứng im" và không có lời giải thích** (KB-32): khi Health Connect có dữ liệu mới, Trang chủ vẫn hiện số cũ cho tới khi bạn tắt hẳn app rồi mở lại; và khi app **không đọc được** dữ liệu (thiếu quyền, máy chưa có dữ liệu), nó **lặng lẽ hiện số 0** thay vì nói "chưa có dữ liệu". Với một khách hàng, "0 bpm" trông giống như app bị lỗi. Hãy ghi lại cảm nhận này để nhóm sản phẩm cân nhắc: tự làm mới số liệu khi mở lại Trang chủ, và phân biệt rõ **"0 vì chưa có dữ liệu"** với **"không đọc được"**.

---

## 7. Khi bạn phát hiện vấn đề — mẫu báo lỗi

Ghi đúng 6 dòng dưới đây cho mỗi vấn đề (đừng cố giải thích nguyên nhân):

```
1. Tôi đang làm gì:      ..............................................
2. Tôi mong đợi:         ..............................................
3. Thực tế xảy ra:       ..............................................
4. Có lặp lại không:     luôn luôn / thỉnh thoảng / chỉ 1 lần
5. Ảnh & thời điểm:      <tên ảnh> — lúc ......... (ngày/giờ máy)
6. Mức độ khó chịu:      Chặn dùng / Rất khó chịu / Khó chịu nhẹ / Góp ý
```

**Cách lấy bằng chứng nhanh** (nếu bạn biết dùng máy tính):

| Bằng chứng | Cách lấy |
|:--|:--|
| Ảnh màn hình | Nút nguồn + giảm âm lượng, hoặc biểu tượng chụp ảnh của emulator |
| Video thao tác | Công cụ ghi màn hình của emulator |
| Nhật ký kỹ thuật (khi cần) | `adb logcat -s HealthSyncWorker SyncWorker HealthDataRepository DatabaseKeyMigration` |

> Nguyên tắc: **ảnh chụp + 6 dòng mô tả** là đủ. Không cần đọc code, không cần đoán bệnh.

---

## 8. Bảng theo dõi kết quả

Mỗi kịch bản ghi **Đạt / Không đạt / Chưa test được / Không áp dụng** và số hiệu ảnh nếu có.

| Mã | Kịch bản | KQ | Ảnh / Ghi chú |
|:--|:--|:--|:--|
| KB-01 | Mở app lần đầu | | |
| KB-02 | Xem mật khẩu đã gõ | | |
| KB-03 | Gõ sai khi đăng nhập | | |
| KB-04 | Tạo tài khoản mới | | |
| KB-05 | Vào app ở chế độ khách | | |
| KB-06 | Trang chủ khi chưa có gì | | |
| KB-07 | Tạo thói quen đầu tiên | | |
| KB-08 | Tạo một thói quen mỗi danh mục | | |
| KB-09 | Ghi nhận một lần uống nước | | |
| KB-10 | Ghi đủ và đạt mục tiêu | | |
| KB-11 | Ghi nhận vận động theo từng lần | | |
| KB-12 | Lọc thói quen theo buổi | | |
| KB-13 | Sửa mục tiêu của một thói quen | | |
| KB-14 | Huỷ giữa đường | | |
| KB-15 | Xoá một thói quen | | |
| KB-16 | Đổi ý, không xoá nữa | | |
| KB-17 | Dùng lại tên thói quen đã xoá | | |
| KB-18 | Chống trùng tên thói quen | | |
| KB-19 | Bỏ trống tên thói quen | | |
| KB-20 | Mục tiêu không hợp lệ | | |
| KB-21 | Ghi nhận khi bỏ trống số lượng | | |
| KB-22 | Ghi nhận bằng chữ | | |
| KB-23 | Ghi nhận số 0 / số âm | | |
| KB-24 | Bấm Lưu liên tục | | |
| KB-25 | Xoay màn hình khi đang nhập | | |
| KB-26 | Nút Back của điện thoại | | |
| KB-27 | Dùng app khi mất mạng | | |
| KB-28 | Máy chưa cài Health Connect | | |
| KB-29 | Health Connect cần cập nhật | | |
| KB-30 | Cấp quyền Health Connect | | |
| KB-31 | Cố tình cấp thiếu quyền | | |
| KB-32 | Bơm dữ liệu giả và xem chỉ số | | |
| KB-33 | Từ chối quyền Health Connect | | |
| KB-34 | Số liệu nhớ sau khi tắt app | | |
| KB-35 | Gợi ý từ dữ liệu Health Connect | | |
| KB-36 | Bật nhắc nhở hàng ngày | | |
| KB-37 | Từ chối quyền thông báo | | |
| KB-38 | Tắt thông báo ở cài đặt máy | | |
| KB-39 | Bật lại thông báo | | |
| KB-40 | Thông báo nhắc nhở có thật | | |
| KB-41 | Không có thông báo khi chưa cấp quyền | | |
| KB-42 | Lần đầu mở tab Ghi chép | | |
| KB-43 | Từ chối quyền Camera | | |
| KB-44 | Chụp và phân tích món ăn | | |
| KB-45 | Đưa kết quả vào nhật ký | | |
| KB-46 | Camera không bị treo | | |
| KB-47 | Tab Phân tích khi ít dữ liệu | | |
| KB-48 | Gợi ý nước mức khẩn cấp | | |
| KB-49 | Gợi ý nước mức trung bình | | |
| KB-50 | Gợi ý thiếu ngủ | | |
| KB-51 | Gợi ý vận động đúng đơn vị | | |
| KB-52 | Gợi ý hạ nhiệt khi tiến bộ | | |
| KB-53 | Gợi ý biến mất khi mọi thứ ổn | | |
| KB-54 | Chỉ số sinh lực 100/100 | | |
| KB-55 | Biểu đồ nhất quán tuần | | |
| KB-56 | Xu hướng chất lượng giấc ngủ | | |
| KB-57 | Thêm widget | | |
| KB-58 | Widget xếp theo buổi | | |
| KB-59 | Widget tự cập nhật | | |
| KB-60 | Widget khi chưa có thói quen | | |
| KB-61 | Kéo giãn widget | | |
| KB-62 | Đăng xuất | | |
| KB-63 | Dữ liệu còn nguyên sau đăng xuất | | |
| KB-64 | Đăng nhập lại tài khoản cũ | | |
| KB-65 | Mở lại app sau khi đăng nhập | | |
| KB-66 | Chế độ khách có được nhớ không | | |
| KB-67 | Cài bản mới đè lên bản cũ | | |
| KB-68 | Gỡ app rồi cài lại | | |
| KB-69 | Bảo mật dữ liệu trên máy | | |
| KB-70 | Chế độ tối | | |
| KB-71 | Đổi sáng/tối khi app đang mở | | |
| KB-72 | Cỡ chữ lớn | | |
| KB-73 | Xoay màn hình ở mọi màn | | |
| KB-74 | Qua nửa đêm | | |
| KB-75 | Tắt app rồi mở lại | | |
| KB-76 | Từ chối mọi quyền | | |
| KB-77 | Bật quyền đếm bước chân | | |
| KB-78 | Đi bộ và thấy số bước tự tăng | | |
| KB-79 | Tắt app, khởi động lại máy rồi đi tiếp | | |
| KB-80 | Máy không có cảm biến đếm bước | | |

---

## 9. Nếu bạn chỉ có 60 phút

Chạy 16 kịch bản theo đúng thứ tự này — đây là "một ngày của khách hàng" khép kín:

**KB-01 → KB-05 → KB-07 → KB-08 → KB-09 → KB-10 → KB-13 → KB-15 → KB-18 → KB-23 → KB-30 → KB-42 → KB-45 → KB-48 → KB-59 → KB-67 → KB-77 → KB-78**

Nếu tất cả đều Đạt, bạn có thể tự tin bản dựng này **dùng được cho người dùng thật**. Sau đó dành thời gian còn lại cho hành trình J (màu sắc/vòng đời) và hành trình E (nhắc nhở) — đây là nơi lỗi hay trốn nhất.

---

## Phụ lục A — Công thức tạo nhanh từng loại gợi ý AI

Gợi ý AI được tính từ **nhật ký ghi trong 7 ngày gần nhất**. Bạn không cần chờ nhiều ngày — chỉ cần ghi **nhiều lần trong cùng một ngày** theo công thức sau:

| Gợi ý bạn muốn thấy | Cách tạo | Nội dung sẽ hiện |
|:--|:--|:--|
| 🚨 Mức nước nguy hiểm thấp (**khẩn cấp**) | Thói quen Uống nước 2000ml → ghi `300` ba lần | *"Bạn uống trung bình 300ml/ngày…"* + nút "Ghi nhận uống nước" |
| 💧 Uống thêm nước mỗi ngày | Xoá dữ liệu → ghi `1200` **một lần** | *"Lượng nước trung bình của bạn là 1200ml/ngày…"*, không có nút |
| ⚠️ Cảnh báo thiếu ngủ nghiêm trọng | Thói quen Ngủ 8 giờ → ghi `5` ba lần | *"…ngủ trung bình 5.0 tiếng/đêm…"* + nút "Đặt nhắc nhở đi ngủ" |
| 💤 Cải thiện chất lượng giấc ngủ | Sau khi có 3 lần `5`, ghi thêm ba lần `8` | *"Chất lượng giấc ngủ của bạn có thể tốt hơn…"*, không có nút |
| 🏃 Tăng hoạt động thể chất ngay hôm nay | Thói quen Chạy bộ 30 phút → ghi `10` ba lần | Nói **phút** và **% mục tiêu** (không nói "bước") + nút "Ghi nhận vận động" |
| 🚶 Tăng dần mục tiêu vận động | Tiếp tục ghi thêm hai lần `25` | Chuyển sang mức nhẹ hơn |
| ✅ Mọi thứ đang rất tốt! | Ghi đủ/vượt mục tiêu đến khi hết cảnh báo (xem KB-53) | *"Bạn đang duy trì thói quen rất đều đặn…"* |

> Mẹo cho người test: sau mỗi lần ghi, chỉ cần chuyển sang tab Phân tích — gợi ý cập nhật **ngay**, không cần chờ.

---

## Phụ lục B — Bơm dữ liệu sức khỏe giả (để test hành trình D)

### B.0. Health Connect là gì — ai ghi dữ liệu vào đó

Health Connect là **kho dữ liệu sức khỏe + trạm phân quyền của Android** — nó **không phải cảm biến** và **không tự đo** gì cả. Nó đứng giữa hai loại ứng dụng:

- **Ứng dụng nguồn (ghi):** app sức khỏe của máy hoặc của đồng hồ — Samsung Health, Fitbit, Mi Fitness, Huawei Health, Zepp Life… Chúng đọc cảm biến/đồng hồ rồi **ghi** dữ liệu vào kho.
- **Ứng dụng đọc:** HealthyAI — chỉ **đọc ra** để hiển thị.

```
Cảm biến đếm bước / đồng hồ đeo tay
        │  (app nguồn đọc rồi ghi vào)
        ▼
Health Connect   ← kho dữ liệu + quyền riêng tư, chỉ nằm trên máy, không qua mạng
        │  (HealthyAI đọc ra, cần quyền)
        ▼
HealthyAI ──► Trang chủ: Bước chân · Ngủ · Tim
        └───► mỗi 6 giờ lưu bản sao vào Room → tab Phân tích (AI phân tích 7 ngày)
```

**Vì sao "kho trống thì app hiện 0":** HealthyAI không có cảm biến riêng cho giấc ngủ/nhịp tim/calo, nên khi chưa có app nguồn nào ghi dữ liệu thì app không có gì để đọc. Đây **không phải lỗi app**.

### B.0b. Riêng **bước chân**: app tự đếm bằng cảm biến (không cần Health Connect)

Từ bản có tính năng đếm bước bằng cảm biến, **số bước không còn phụ thuộc Health Connect**:

- Trên **điện thoại thật** (có cảm biến đếm bước phần cứng): bấm thẻ **"Bật đếm bước chân"** ở Trang chủ → cấp quyền **Nhận diện hoạt động thể chất** → app tự đếm, số bước hiện ngay và tự tăng khi bạn đi (xem **KB-77…KB-79**).
- Trên **emulator** (không có cảm biến): không có thẻ xin quyền, số bước vẫn lấy từ Health Connect → phải bơm dữ liệu như **B.2**.
- Thứ tự ưu tiên khi hiển thị: **Health Connect trước** (có thể gồm cả dữ liệu đồng hồ), nếu Health Connect không có số thì dùng **cảm biến của điện thoại** — khi đó thẻ có dòng *"Số bước đọc trực tiếp từ cảm biến của điện thoại"*.

### B.1. Mỗi chỉ số lấy dữ liệu từ đâu và trong khung thời gian nào

App **không** tự tạo số liệu — nó đọc từ **Health Connect** và chỉ đọc trong các khung sau:

| Chỉ số trên Trang chủ | Nguồn | Khung thời gian app đọc | Hiển thị |
|:--|:--|:--|:--|
| **Bước chân** | `StepsRecord` — **hoặc** cảm biến đếm bước của máy (xem B.0b) | từ **00:00 hôm nay** tới lúc mở app | `3.5k` (nghìn bước, 1 số thập phân) |
| **Ngủ** | `SleepSessionRecord` | từ **18:00 hôm qua** tới lúc mở app | `7h 30m` |
| **Tim** | `HeartRateRecord` | **60 phút gần nhất** ⚠️ | `72 bpm` (nhịp tim trung bình) |
| *(Calories)* | `ActiveCaloriesBurnedRecord` | từ **00:00 hôm nay** | không hiện trên thẻ, chỉ dùng cho đồng bộ nền |

> ⚠️ **Chỗ dễ sai nhất là Nhịp tim:** dữ liệu nhịp tim phải có **mẫu trong vòng 1 giờ trước khi bạn mở app**, nếu không sẽ hiện `0 bpm` dù Health Connect có dữ liệu nhịp tim cũ.

### B.2. Các bước bơm dữ liệu (emulator Pixel 7 API 34 có Google Play)

1. Bật emulator và cài app HealthyAI.
2. Đảm bảo máy có **Health Connect**: trên Android 14 nó là thành phần hệ thống; nếu Trang chủ hiện thẻ **"Chưa cài Health Connect"** thì bấm nút đó để mở Play Store và cài (gói `com.google.android.apps.healthdata`).
3. Trong app HealthyAI, bấm thẻ **"Cấp quyền Health Connect"** và bật **đủ 4 quyền**: Bước chân, Giấc ngủ, Nhịp tim, Calories.
4. Cài **Health Connect Toolbox** từ Play Store (trên emulator có Google Play).
5. Trong Toolbox, tạo dữ liệu — **nhớ đặt thời điểm theo bảng ở B.1**:
   - **Steps**: ~3 500 bước, khoảng thời gian **trong hôm nay** (ví dụ 08:00 → 09:00);
   - **Sleep**: một phiên **kết thúc trong khoảng 18:00 hôm qua → bây giờ** (ví dụ 23:00 hôm qua → 06:30 hôm nay). Muốn thẻ "Xu hướng chất lượng giấc ngủ" ở tab Phân tích có số thì thêm **các pha** Deep/Light/REM cho phiên đó;
   - **Heart rate**: một series có **mẫu mới nhất vài phút trước** (nằm trong 60 phút gần nhất), giá trị ~72;
   - *(tùy chọn)* **Active calories burned** trong hôm nay.
6. Quay lại HealthyAI → **tắt hẳn app rồi mở lại** (Cài đặt máy → Ứng dụng → HealthyAI → **Buộc dừng**).
7. Kỳ vọng ở thẻ "Tóm tắt sức khỏe": **3.5k** bước · **7h 30m** ngủ · **72 bpm**.

*Muốn test cảnh báo từ dữ liệu Health Connect (thiếu ngủ 3 ngày, nhịp tim > 85): cần dữ liệu **nhiều ngày liên tiếp** — nên dành việc này cho kỹ thuật, không tính vào buổi test của khách hàng.*

### B.3. Nếu bạn test trên **điện thoại thật**: đi bộ thôi là chưa đủ

> ✅ **Từ bản có "đếm bước bằng cảm biến của máy", số bước không còn phụ thuộc Health Connect nữa.**
> Trên **điện thoại thật**, bạn chỉ cần bấm **"Bật đếm bước chân"** ở Trang chủ (quyền *Nhận diện hoạt động thể chất*) là app **tự đếm** — xem **KB-77…KB-79**. Phần dưới đây dành cho trường hợp **máy không có cảm biến** (emulator) hoặc bạn muốn số bước/giấc ngủ/nhịp tim **có nguồn từ đồng hồ**.

⚠️ **Điện thoại đếm được bước chân nhưng KHÔNG tự đưa số bước vào Health Connect.** Health Connect chỉ là "kho" — phải có một **ứng dụng nguồn** ghi dữ liệu vào kho đó, rồi HealthyAI mới đọc được. Nếu bạn mới chỉ đi bộ quanh nhà mà trong Health Connect chưa có hàng dữ liệu nào thì app sẽ mãi hiện `0.0k` — **đây không phải lỗi app**.

**Kiểm tra theo thứ tự:**

1. **Trong Health Connect có dữ liệu bước chân chưa?**
   Mở app **Health Connect** → *Dữ liệu và quyền truy cập* → **Bước chân** → xem có hàng dữ liệu **hôm nay** và **ứng dụng nguồn** là app nào.
   - **Trống** → chưa có app nào ghi bước vào Health Connect (đây là nguyên nhân phổ biến nhất).
   - **Có dữ liệu** → nhảy xuống bước 4.
2. **Bật đồng bộ Health Connect ở app nguồn** (một trong các app sau, tùy máy bạn có gì):
   - **Samsung Health**: *Cài đặt* → **Health Connect** → bật đồng bộ (và đảm bảo Samsung Health đang đếm bước).
   - **Google Fit**: *Hồ sơ* → *Cài đặt* → **Đồng bộ với Health Connect**.
   - **Fitbit**: *Hồ sơ* → *Cài đặt Fitbit* → **Health Connect**.
   - **Mi Fitness / Zepp Life / Huawei Health**: trong phần cài đặt ứng dụng → **Health Connect**.
   Sau khi bật, **đi bộ vài trăm bước** rồi quay lại Health Connect xem số đã xuất hiện chưa (app nguồn thường đồng bộ theo chu kỳ, có thể chậm vài phút).
3. **Không có app nguồn nào?** Dùng **Health Connect Toolbox** để tự tay thêm một hàng **Steps** cho hôm nay (như B.2) — mục đích là để kiểm chứng phần đọc dữ liệu của HealthyAI, không phải để đo bước thật.
4. **Kiểm tra quyền của HealthyAI**: Health Connect → *Dữ liệu và quyền truy cập* → *Ứng dụng* → **HealthyAI** → phải thấy **Bước chân: cho phép đọc** (một số máy tự thu hồi quyền của app lâu không dùng → app trả 0 trong im lặng).
5. **Buộc dừng HealthyAI rồi mở lại** (xem KB-32) — vì app chỉ đọc lại số liệu khi được mở lại.
6. **Máy có cảm biến đếm bước không?** Mở ứng dụng sức khỏe sẵn có của máy (Samsung Health / app "Sức khỏe" của Google) — nếu chính app đó cũng hiện 0 bước thì cảm biến/ứng dụng đếm bước của máy chưa hoạt động, không liên quan tới HealthyAI.
7. **Nếu bạn đang chạy emulator trên laptop**: đi bộ quanh nhà **không giúp gì được** — emulator không có cảm biến bước chân. Bắt buộc dùng cách ở **B.2** (bơm dữ liệu).



---

## Phụ lục C — Chạy tác vụ nền để kiểm tra ngay (thay vì chờ 24 giờ)

| Việc cần làm | Cách nhanh |
|:--|:--|
| Chạy job nhắc nhở `daily_reminder_worker` | Android Studio → **View → Tool Windows → App Inspection → Background Task Inspector** → chọn app HealthyAI → chọn job → bấm **Run** |
| Xem các job đã lên lịch | Trong Background Task Inspector (hoặc `adb shell dumpsys jobscheduler | findstr healthyai`) |
| Chạy lại đồng bộ dữ liệu Health Connect | Background Task Inspector → job `periodic_health_sync_worker` → **Run** |
| Xem nhật ký khi có lỗi | `adb logcat -s HealthSyncWorker SyncWorker HealthDataRepository DatabaseKeyMigration` |

> Thông báo nhắc nhở và việc tự đồng bộ dữ liệu sức khỏe đều chạy theo chu kỳ (24 giờ và 6 giờ) và **chỉ khi pin không yếu** — đó là lý do bạn không thấy chúng ngay sau khi cài. Đây là bình thường, không phải lỗi.

---

## Phụ lục D — Từ điển chữ hiện trên màn hình (để đối chiếu nhanh)

| Nơi hiện | Chữ sẽ thấy |
|:--|:--|
| Màn Đăng nhập | "HealthyAI" · "Theo dõi & cải thiện thói quen sức khỏe" · "Đăng nhập để đồng bộ thói quen lên Firebase" · "Chưa có tài khoản? Đăng ký" · "Bỏ qua, dùng ở chế độ khách" |
| 4 tab dưới cùng | Trang chủ · Thói quen · Ghi chép · Phân tích |
| Trang chủ | "Tóm tắt sức khỏe" · "Bước chân" · "Ngủ" · "Tim" · "Thói quen hôm nay" · "Thêm thói quen đầu tiên" |
| Thẻ xin quyền (Trang chủ) | "Cấp quyền Health Connect" · "Chưa cài Health Connect" · "Health Connect cần được cập nhật" · "Bật nhắc nhở hàng ngày" · "Thông báo đang bị tắt" |
| Tab Thói quen | "Theo dõi thói quen" · chip "Tất cả / Sáng / Chiều / Tối" · nút "Ghi" · "Xong" · "Hoàn thành" · "Chưa có thói quen nào" |
| Hộp thoại ghi nhận | "Ghi nhận <tên thói quen>" · "Số lượng (<đơn vị>)" · "Lưu" · "Hủy" · "Đã ghi nhận: <tên>" |
| Màn Thêm/Sửa thói quen | "Thêm thói quen mới" / "Sửa thói quen" · "Danh mục" · "Mục tiêu hàng ngày" · "Thời gian thực hiện" · "Tần suất" · "Tạo thói quen" / "Cập nhật" · "Xóa thói quen?" |
| Tab Ghi chép | "Ghi chép bữa ăn" · "Hướng camera vào món ăn để phân tích." · "Cần quyền Camera" · "Cấp quyền Camera" · "Quét món ăn" · "Phân tích món ăn" · "Đang phân tích..." · "Kết quả nhận diện" · "Tự động" · "Thêm vào nhật ký" |
| Tab Phân tích | "Phân tích AI" · "BLOOM AI GỢI Ý" · "CẢNH BÁO KHẨN CẤP" · "Chỉ số sinh lực" · "Sự nhất quán hàng tuần" · "Xu hướng chất lượng giấc ngủ" · "Mọi thứ đang rất tốt!" |
| Widget | "Tiến độ thói quen" (tên widget) · "Tiến độ hôm nay" · "x/y thói quen đạt mục tiêu" · "+N thói quen khác" · "Healthy AI / Chưa có thói quen" |
| Hộp thoại đăng xuất | "Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên." |
| Thông báo nhắc nhở | "HealthyAI 💪 Nhắc nhở thói quen" · "Bạn đã cập nhật thói quen hôm nay chưa? Chỉ cần vài giây thôi!" · kênh "Nhắc nhở thói quen sức khỏe" |

---

**Hết tài liệu.** Khi test xong, gửi lại mục 8 (bảng theo dõi) + các vấn đề ở mục 7 cho nhóm phát triển.
