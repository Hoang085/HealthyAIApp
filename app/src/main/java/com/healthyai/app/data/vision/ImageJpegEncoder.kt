package com.healthyai.app.data.vision

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Nén `Bitmap` thành JPEG để gửi AI; chạy trên [Dispatchers.Default] vì nén tốn CPU (chục ms/ảnh). */
object ImageJpegEncoder {

    /** Cạnh dài tối đa của ảnh gửi cho AI — khớp với `GalleryImageLoader.MAX_DIMENSION`. */
    const val MAX_DIMENSION = 1280

    /** Chất lượng JPEG: 80 là mức "mắt thường không phân biệt được" mà dung lượng giảm mạnh. */
    const val QUALITY = 80

    suspend fun encode(
        bitmap: Bitmap,
        maxDimension: Int = MAX_DIMENSION,
        quality: Int = QUALITY
    ): ByteArray = withContext(Dispatchers.Default) {
        val scaled = downscaleIfNeeded(bitmap, maxDimension)
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }

    /** Thu nhỏ [bitmap] nếu cạnh dài vượt [maxDimension]; không recycle bản gốc vì chỗ gọi còn dùng. */
    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension || longestSide == 0) return bitmap

        val ratio = maxDimension.toFloat() / longestSide
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, /* filter = */ true)
    }
}
