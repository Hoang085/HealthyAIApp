package com.healthyai.app.data.vision

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Đọc ảnh người dùng **chọn từ thư viện** thành `Bitmap` đưa được vào luồng phân tích món ăn.
 *
 * Khác với ảnh chụp qua CameraX (đã là `Bitmap` đúng hướng), ảnh trong thư viện là **tệp bất kỳ** nên
 * phải xử lý hai việc:
 *
 * 1. **Thu nhỏ trước khi giải mã** — ảnh điện thoại thường 12–50 MP; giải mã thẳng vào bộ nhớ có thể
 *    ngốn hàng chục MB và làm app bị `OutOfMemoryError`. Ở đây đọc kích thước trước
 *    (`inJustDecodeBounds`), tính `inSampleSize` (luỹ thừa 2) để cạnh dài chỉ còn quanh
 *    [MAX_DIMENSION] điểm ảnh rồi mới giải mã — AI chỉ cần ảnh cỡ đó để nhận diện món ăn.
 * 2. **Xoay theo cờ EXIF** — ảnh chụp dọc hay được lưu ở dạng ngang kèm cờ `TAG_ORIENTATION`; thiếu
 *    bước này thì món ăn bị quay 90° khi phân tích.
 *
 * Toàn bộ việc đọc tệp chạy trên [Dispatchers.IO] và **không** bao giờ ném ra ngoài: uri hỏng, tệp bị
 * xoá, không có quyền đọc… đều trả `null` để màn hình báo *"Không đọc được ảnh đã chọn"* thay vì crash.
 */
object GalleryImageLoader {

    /**
     * Cạnh dài tối đa (điểm ảnh) của ảnh đưa vào phân tích.
     *
     * 1280 đủ để mô hình nhận ra món ăn mà vẫn nhẹ: ảnh 4:3 chỉ còn ~1,3 MP (~5 MB trong bộ nhớ) so với
     * ~48 MB của ảnh 12 MP nguyên bản. `ImageJpegEncoder` dùng cùng ngưỡng này khi nén để gửi cho AI.
     */
    const val MAX_DIMENSION = 1280

    /**
     * Đọc ảnh tại [uri] thành `Bitmap` đã thu nhỏ và xoay đúng hướng.
     *
     * @return `null` khi không đọc được ảnh (uri hỏng, tệp không phải ảnh, hết bộ nhớ…).
     */
    suspend fun load(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
            }
            val decoded = resolver.openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null

            rotateByExif(decoded, readExifRotation(resolver, uri))
        } catch (e: CancellationException) {
            // Coroutine bị huỷ (người dùng rời màn) → ném tiếp, không trả `null` như "ảnh hỏng"
            // (phụ lục C #39).
            throw e
        } catch (e: Exception) {
            // Ảnh hỏng/không đọc được không phải lỗi lập trình — màn hình đã có nhánh báo lỗi riêng.
            null
        }
    }

    /** Hướng xoay cần bù, tính bằng độ, đọc từ cờ EXIF của ảnh. */
    private fun readExifRotation(resolver: ContentResolver, uri: Uri): Int {
        val orientation = resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: return 0

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /** Xoay [bitmap] theo [degrees]; bản gốc được giải phóng khi đã tạo bản xoay. */
    private fun rotateByExif(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    /**
     * Hệ số thu nhỏ (luỹ thừa 2, đúng yêu cầu của `BitmapFactory`) để cạnh dài sau khi giải mã không
     * vượt quá [maxDimension].
     */
    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxDimension) sample *= 2
        return sample
    }
}
