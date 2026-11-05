package com.dots.app.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowManager
import com.dots.app.domain.countdown.CountdownCalculator
import com.dots.app.domain.countdown.CountdownProgress
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.domain.model.WallpaperTarget
import com.dots.app.render.WallpaperRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

class WallpaperException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Renders full-resolution wallpapers and hands them to [WallpaperManager].
 */
class WallpaperGenerator(
    private val context: Context,
    private val images: ImageRepository,
) {

    fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    fun progress(config: WallpaperConfig, date: LocalDate = today()): CountdownProgress =
        CountdownCalculator.calculate(config.countdown, date)

    /** Physical portrait screen size, so the wallpaper maps 1:1 to pixels with no scaling. */
    fun screenSize(): Size {
        val wm = context.getSystemService(WindowManager::class.java)
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
        return Size(min(w, h), max(w, h))
    }

    /**
     * Launchers show the home wallpaper zoomed in by up to this factor (SystemUI
     * config_wallpaperMaxScale, 1.1 on Pixel) and zoom out when the shade or recents open.
     * The home preview applies it so the preview matches the real home screen.
     */
    fun homeScreenZoom(): Float {
        val res = android.content.res.Resources.getSystem()
        val id = res.getIdentifier("config_wallpaperMaxScale", "dimen", "android")
        if (id == 0) return 1f
        return try {
            res.getFloat(id).takeIf { it in 1f..1.5f } ?: 1f
        } catch (_: android.content.res.Resources.NotFoundException) {
            1f
        }
    }

    suspend fun render(config: WallpaperConfig, size: Size = screenSize(), date: LocalDate = today()): Bitmap =
        withContext(Dispatchers.Default) {
            val image = config.background.imagePath?.let { images.load(it) }
            val renderer = WallpaperRenderer()
            try {
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                renderer.render(Canvas(bitmap), size.width, size.height, config, progress(config, date), image)
                bitmap
            } catch (e: OutOfMemoryError) {
                throw WallpaperException("Not enough memory to render the wallpaper.", e)
            } finally {
                renderer.release()
                image?.recycle()
            }
        }

    suspend fun apply(bitmap: Bitmap, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        val wm = WallpaperManager.getInstance(context)
        if (!wm.isWallpaperSupported) throw WallpaperException("This device doesn't support wallpapers.")
        if (!wm.isSetWallpaperAllowed) throw WallpaperException("Changing the wallpaper is blocked on this device.")
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        try {
            wm.setBitmap(bitmap, null, true, flags)
        } catch (e: IOException) {
            throw WallpaperException("Couldn't set the wallpaper.", e)
        } catch (e: SecurityException) {
            throw WallpaperException("DOTS isn't allowed to change the wallpaper.", e)
        }
    }
}
