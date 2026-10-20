package com.healthyai.app.presentation.ui.format

/** Chuỗi mục tiêu gọn: bỏ đuôi thập phân khi là số nguyên; tránh String.format vì phụ thuộc locale. */
fun formatTargetValue(value: Float): String =
    if (value % 1f == 0f) value.toLong().toString() else value.toString()

/** Chuỗi số bước: dưới 1000 hiện số chính xác, từ 1000 rút gọn "x,yk"; làm tròn nửa lên tới 100. */
fun formatStepCount(steps: Long): String {
    val safe = steps.coerceAtLeast(0L)
    if (safe < 1_000L) return safe.toString()

    // Số "phần nghìn bước" đã làm tròn, tách thành phần nguyên/thập phân của "x,yk".
    val tenths = (safe + 50L) / 100L
    return "${tenths / 10L},${tenths % 10L}k"
}

/** Chuỗi số bước đầy đủ cho widget ("6250" → "6.250"), tự chèn dấu chấm để không phụ thuộc locale. */
fun formatStepsExact(steps: Long): String {
    val safe = steps.coerceAtLeast(0L)
    val digits = safe.toString()
    if (digits.length <= 3) return digits

    // Chèn dấu chấm vào mỗi 3 chữ số kể từ bên phải: "1234567" → "1.234.567".
    return digits.reversed()
        .chunked(3)
        .joinToString(separator = ".")
        .reversed()
}

/** Chuỗi số giờ ngủ: 7.5 → "7h 30m"; giá trị ≤ 0 vẫn ra "0h 0m", nơi gọi tự thay bằng "--" nếu cần. */
fun formatSleepDuration(hours: Double): String {
    val safe = hours.coerceAtLeast(0.0)
    val h = safe.toInt()
    val m = ((safe - h) * 60).toInt()
    return "${h}h ${m}m"
}

/** Chuỗi nhịp tim: 72.0 → "72 bpm"; ≤ 0 → "-- bpm" để không bị đọc thành "nhịp tim bằng 0". */
fun formatHeartRate(bpm: Double): String =
    if (bpm > 0.0) "${bpm.toInt()} bpm" else "-- bpm"
