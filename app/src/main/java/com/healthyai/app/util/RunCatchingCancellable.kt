package com.healthyai.app.util

import kotlinx.coroutines.CancellationException

/**
 * Giống `runCatching` nhưng **không nuốt [CancellationException]** (phụ lục C #39).
 *
 * Vì sao cần: `kotlin.runCatching` bắt **mọi** `Throwable`, kể cả `CancellationException` — trong
 * hàm `suspend`, ngoại lệ huỷ coroutine bị biến thành `Result.failure` nên:
 * - chỗ gọi tưởng "lỗi nghiệp vụ" và hiện thông báo lỗi cho người dùng trong khi thao tác chỉ bị huỷ
 *   (đổi màn hình, `viewModelScope` bị huỷ);
 * - coroutine **không kết thúc đúng** (nuốt tín hiệu huỷ nên khối mã chạy tiếp sau điểm huỷ).
 *
 * Dùng thay `runCatching` ở **mọi chỗ bọc lời gọi `suspend`/I/O** trong code sản phẩm. Những chỗ
 * `runCatching` chỉ bọc việc thuần bộ nhớ (ví dụ `HabitCategory.valueOf(...)`,
 * `Context.startActivity(...)`) không cần hàm này vì chúng không thể sinh `CancellationException`.
 *
 * Hàm là `inline` nên khối [block] vẫn gọi được hàm `suspend` tại chỗ gọi (giống `runCatching`).
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
