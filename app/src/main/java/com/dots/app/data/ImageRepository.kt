package com.dots.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.sqrt

class ImageImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Dominant colour + prominent swatches of an image. */
data class ImageColors(val dominant: Int, val swatches: List<Int>)

/**
 * Imports photos picked by the user into private app storage.
 *
 * Photo picker URIs are only readable for a limited time, but the daily worker needs the
 * image forever, so the picked image is decoded once (EXIF rotation applied, downsampled to
 * a sane maximum) and saved as our own file.
 */
class ImageRepository(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    suspend fun import(uri: Uri): File = withContext(Dispatchers.IO) {
        val bitmap = try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val (w, h) = info.size.width to info.size.height
                val scale = importScale(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            }
        } catch (e: Exception) {
            throw ImageImportException("This image format isn't supported or the file is damaged.", e)
        } catch (e: OutOfMemoryError) {
            throw ImageImportException("This image is too large to import.", e)
        }
        try {
            val alpha = bitmap.hasAlpha()
            val file = File(dir, "bg_${System.currentTimeMillis()}.${if (alpha) "png" else "jpg"}")
            FileOutputStream(file).use { out ->
                val ok = if (alpha) bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                else bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                if (!ok) throw ImageImportException("Couldn't save the image.")
            }
            file
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Decodes a stored image. [maxLongSide] keeps the preview light (≈1600px) while the
     * generator asks for the full stored resolution.
     */
    suspend fun load(path: String, maxLongSide: Int = Int.MAX_VALUE): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return@withContext null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxLongSide) sample *= 2
        try {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: OutOfMemoryError) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample * 2 })
        }
    }

    suspend fun extractColors(bitmap: Bitmap): ImageColors = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val dominant = palette.dominantSwatch?.rgb ?: 0xFF000000.toInt()
        val swatches = listOfNotNull(
            palette.vibrantSwatch, palette.lightVibrantSwatch, palette.darkVibrantSwatch,
            palette.mutedSwatch, palette.lightMutedSwatch, palette.darkMutedSwatch,
        ).sortedByDescending { it.population }.map { it.rgb }
        ImageColors(dominant, swatches.ifEmpty { listOf(dominant) })
    }

    /** Deletes imported images that no config, preset or history entry refers to any more. */
    suspend fun cleanup(referenced: Set<String>) = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { if (it.absolutePath !in referenced) it.delete() }
    }

    private fun importScale(w: Int, h: Int): Float {
        val longSide = max(w, h).toFloat()
        val bySide = MAX_LONG_SIDE / longSide
        val byPixels = sqrt(MAX_PIXELS / (w.toFloat() * h))
        return minOf(1f, bySide, byPixels)
    }

    private companion object {
        /** Enough for 4K-class wallpapers with room to zoom, while staying ≈32MB decoded. */
        const val MAX_LONG_SIDE = 4096f
        const val MAX_PIXELS = 8_000_000f
    }
}
