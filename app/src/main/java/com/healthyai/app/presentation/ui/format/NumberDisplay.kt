package com.healthyai.app.presentation.ui.format

/**
 * Chuỗi hiển thị "sạch" cho **mục tiêu** của thói quen — dùng ở ô "Mục tiêu hàng ngày" của màn Sửa
 * và ở giá trị mặc định của hộp thoại ghi nhận.
 *
 * Bỏ đuôi thập phân khi giá trị là số nguyên (`2000f` → `"2000"` thay vì `"2000.0"`, `8f` → `"8"`)
 * nhưng giữ nguyên phần thập phân khi có (`7.5f` → `"7.5"`).
 *
 * Không dùng `String.format`/`DecimalFormat` vì chúng phụ thuộc locale — máy đặt `vi-VN` sẽ cho ra
 * `"7,5"`, và `toFloatOrNull()` lúc lưu sẽ trả `null` (mục tiêu bị coi là không hợp lệ).
 */
fun formatTargetValue(value: Float): String =
    if (value % 1f == 0f) value.toLong().toString() else value.toString()

/**
 * Chuỗi hiển thị **số bước** cho vòng tròn chỉ số "Bước chân" ở thẻ "Tóm tắt sức khỏe".
 *
 * - **Dưới 1 000 bước** → hiện **số chính xác** (`0` → `"0"`, `312` → `"312"`). Trước đây mọi giá
 *   trị đều bị quy về dạng `"x,yk"` nên đi 30–40 bước vẫn hiện `"0,0k"` — trông như app không đếm
 *   bước (mục 3 #23, phụ lục C #32).
 * - **Từ 1 000 bước** → giữ dạng rút gọn `"1,2k"` cho vừa vòng tròn.
 *
 * Quy tắc giống `formatTargetValue`: **không** dùng `String.format` (phụ thuộc locale) mà tự dựng
 * chuỗi, nên dấu thập phân luôn là **dấu phẩy** (giao diện tiếng Việt) và kết quả giống nhau trên
 * mọi máy. Làm tròn **nửa lên** tới 100 bước gần nhất (`1 950` → `"2,0k"`).
 */
fun formatStepCount(steps: Long): String {
    val safe = steps.coerceAtLeast(0L)
    if (safe < 1_000L) return safe.toString()

    // Tổng số "phần nghìn bước" đã làm tròn → tách thành phần nguyên/phần thập phân của "x,yk".
    val tenths = (safe + 50L) / 100L
    return "${tenths / 10L},${tenths % 10L}k"
}
