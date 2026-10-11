package com.healthyai.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ── Health-Inspired Green Palette ──────────────────────────
val GreenPrimary          = Color(0xFF2E7D32)
val GreenOnPrimary        = Color(0xFFFFFFFF)
val GreenPrimaryContainer = Color(0xFFB8F5B0)
val GreenOnPrimaryContainer = Color(0xFF002204)

// Dark variant
val GreenPrimaryDark      = Color(0xFF9CD898)
val GreenOnPrimaryDark    = Color(0xFF003909)
val GreenPrimaryContainerDark = Color(0xFF1B5E20)

// ── Teal Secondary ─────────────────────────────────────────
val TealSecondary         = Color(0xFF00695C)
val TealOnSecondary       = Color(0xFFFFFFFF)
val TealSecondaryContainer = Color(0xFFB2DFDB)
val TealOnSecondaryContainer = Color(0xFF002018)

// Dark variant
val TealSecondaryDark     = Color(0xFF80CBC4)
val TealSecondaryContainerDark = Color(0xFF004D40)

// ── Surface / Background ───────────────────────────────────
val BackgroundLight       = Color(0xFFF6FFF5)
val OnBackgroundLight     = Color(0xFF1A1C19)
val SurfaceLight          = Color(0xFFF6FFF5)
val OnSurfaceLight        = Color(0xFF1A1C19)
val SurfaceVariantLight   = Color(0xFFDEE5D8)

val BackgroundDark        = Color(0xFF1A1C19)
val OnBackgroundDark      = Color(0xFFE2E3DC)
val SurfaceDark           = Color(0xFF1A1C19)
val OnSurfaceDark         = Color(0xFFE2E3DC)
val SurfaceVariantDark    = Color(0xFF424940)

// ── Nền thẻ / panel (Card, Surface) ────────────────────────
// Trước đây Card hard-code `Color.White` → ở dark mode thành thẻ trắng trong khi chữ dùng
// `onSurface` sáng ⇒ mất chữ. Hai giá trị dưới đây nổi hơn `background` một chút ở CẢ hai chế độ
// (sáng: trắng trên nền xanh-nhạt; tối: xám-xanh nhạt trên nền #1A1C19).
val SurfaceContainerLowLight = Color(0xFFFFFFFF)
val SurfaceContainerLowDark  = Color(0xFF23261F)

// ── Semantic Colors ────────────────────────────────────────
val ErrorRed              = Color(0xFFB00020)
val WarningOrange         = Color(0xFFE65100)
val SuccessGreen          = Color(0xFF388E3C)
val InfoBlue              = Color(0xFF0288D1)
val CriticalRed           = Color(0xFFC62828)

// ── Widget màn hình chính "Sức khỏe hôm nay" (Glance) ───────
// Widget không dùng `GlanceTheme` (mặc định là **màu động** của hệ thống nên trên máy Android 12+ sẽ
// ra tông tím, lệch hẳn bản sắc xanh của app) mà tự khai báo từng cặp sáng/tối. Glance nhận
// `ColorProvider(day, night)` chứ không đọc được `MaterialTheme`, nên các giá trị dưới đây lấy đúng
// palette ở trên để widget trông như một phần của cùng ứng dụng.
//
// ⚠️ Khối này phải nằm **cuối file**: các `val` ở đây tham chiếu `val` khai báo phía trên (Kotlin khởi
// tạo `val` top-level theo thứ tự khai báo trong file).
/** Nền widget — dùng đúng token "nền thẻ" (`surfaceContainerLow`) của app. */
val WidgetSurfaceLight = SurfaceContainerLowLight
val WidgetSurfaceDark = SurfaceContainerLowDark

/** Chữ chính trên widget (số đo). */
val WidgetContentLight = OnSurfaceLight
val WidgetContentDark = OnSurfaceDark

/** Chữ phụ (nhãn "Hôm nay", tên chỉ số). */
val WidgetMutedLight = Color(0xFF5F6B5A)
val WidgetMutedDark = Color(0xFFA9B4A3)

/** Đường kẻ mảnh dưới tiêu đề. */
val WidgetDividerLight = SurfaceVariantLight
val WidgetDividerDark = SurfaceVariantDark

/** Màu nhấn riêng cho từng chỉ số (icon + nhãn), đủ tương phản trên nền ở cả hai chế độ. */
val WidgetStepsLight = GreenPrimary
val WidgetStepsDark = GreenPrimaryDark
val WidgetSleepLight = TealSecondary
val WidgetSleepDark = TealSecondaryDark
val WidgetHeartLight = CriticalRed
val WidgetHeartDark = Color(0xFFFF8A80)
