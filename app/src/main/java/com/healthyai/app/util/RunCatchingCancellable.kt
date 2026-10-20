package com.healthyai.app.util

import kotlinx.coroutines.CancellationException

/** Như `runCatching` nhưng không nuốt [CancellationException] (ngoại lệ huỷ coroutine). */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
