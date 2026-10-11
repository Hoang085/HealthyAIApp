package com.healthyai.app.data.vision

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Nén `Bitmap` thành **JPEG** để gửi cho AI (mục 3 #42).
 *
 * Ảnh chụp từ CameraX là `Bitmap` đầy đủ (vài MP), còn:
 * - gửi base64 nên **mỗi byte đều tốn thời gian + tiền**;
 * - Gemini có giới hạn kích thước request;
 * - ảnh quá lớn cũng chẳng thêm thông tin — AI chỉ cần thấy rõ món ăn.
 *
 * Nên ảnh được thu nhỏ về cạnh dài ≤ [MAX_DIMENSION] và nén chất lượng [QUALITY]. Ảnh chọn từ thư viện
 * đã được `GalleryImageLoader` thu nhỏ sẵn (cùng ngưỡng 1280 px) nên bước này gần như không đổi kích
 * thước — nó chỉ đảm bảo **mọi** đường vào đều qua cùng một chuẩn.
 *
 * Nén là việc CPU (vài chục ms với ảnh 1,3 MP) nên chạy trên [Dispatchers.Default] để không chặn UI.
 */
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

    /**
     * Thu nhỏ [bitmap] nếu cạnh dài vượt [maxDimension].
     *
     * Bản gốc **không** bị `recycle()`: nó thuộc về chỗ gọi (ảnh chụp/ảnh thư viện), và màn hình có thể
     * còn dùng lại.
     */
    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension || longestSide == 0) return bitmap

        val ratio = maxDimension.toFloat() / longestSide
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, /* filter = */ true)
    }
}
