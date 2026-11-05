package com.dots.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Saving to the gallery (MediaStore, no storage permission needed on Android 10+) and
 * sharing through a FileProvider.
 */
class WallpaperExporter(private val context: Context) {

    suspend fun saveToGallery(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DOTS_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DOTS")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw WallpaperException("Couldn't create the image in your gallery.")
        try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: throw WallpaperException("Couldn't write to your gallery.")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw if (e is WallpaperException) e else WallpaperException("Couldn't save the wallpaper.", e)
        }
    }

    suspend fun shareIntent(bitmap: Bitmap): Intent = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "DOTS_wallpaper.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(send, "Share wallpaper")
    }

    /** Small JPEG used for wallpaper history. */
    suspend fun saveThumbnail(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "history").apply { mkdirs() }
        val w = 270
        val h = (bitmap.height * w / bitmap.width.toFloat()).toInt().coerceAtLeast(1)
        val thumb = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val file = File(dir, "thumb_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (thumb !== bitmap) thumb.recycle()
        file.absolutePath
    }
}
