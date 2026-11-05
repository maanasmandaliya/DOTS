package com.dots.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.dots.app.domain.color.ColorMath
import com.dots.app.domain.countdown.CountdownProgress
import com.dots.app.domain.model.BackgroundSettings
import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.DotShape
import com.dots.app.domain.model.EmptyDotStyle
import com.dots.app.domain.model.ImageFit
import com.dots.app.domain.model.WallpaperConfig
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The single rendering engine for DOTS. The live preview draws through it onto a Compose
 * canvas and the wallpaper generator draws through it onto a full-resolution bitmap, so
 * what the user sees is exactly what gets applied.
 *
 * Instances cache the blurred background layer, so keep one per surface (preview / worker)
 * and don't share an instance across threads.
 */
class WallpaperRenderer {

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        letterSpacing = 0.06f
    }
    private val rect = RectF()

    private var blurKey: Any? = null
    private var blurLayer: Bitmap? = null

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WallpaperConfig,
        progress: CountdownProgress,
        image: Bitmap?,
    ) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawBackground(canvas, w, h, config.background, image)
        drawGrid(canvas, w, h, config, progress)
    }

    // ---------------------------------------------------------------- background

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, bg: BackgroundSettings, image: Bitmap?) {
        canvas.drawColor(bg.color or 0xFF000000.toInt())
        if (image != null && !image.isRecycled && bg.imageOpacity > 0f) {
            imagePaint.colorFilter = colorFilterFor(bg)
            imagePaint.alpha = (bg.imageOpacity.coerceIn(0f, 1f) * 255).roundToInt()
            val radiusPx = bg.blur * DotGridLayout.unit(w)
            if (radiusPx >= 1f) {
                val layer = blurredLayer(image, w, h, bg, radiusPx)
                rect.set(0f, 0f, w, h)
                canvas.drawBitmap(layer, null, rect, imagePaint)
            } else {
                imageRect(image.width, image.height, w, h, bg, rect)
                canvas.drawBitmap(image, null, rect, imagePaint)
            }
            imagePaint.colorFilter = null
        }
        if (bg.darkOverlay > 0f) canvas.drawColor(ColorMath.withAlpha(0xFF000000.toInt(), bg.darkOverlay))
        if (bg.colorOverlayOpacity > 0f) canvas.drawColor(ColorMath.withAlpha(bg.colorOverlay, bg.colorOverlayOpacity))
    }

    /**
     * Blur is computed on a layer downscaled by k so the effective radius stays ~6px.
     * The layer depends only on the image transform + blur, so it is cached and colour,
     * dot or opacity changes never trigger a re-blur.
     */
    private fun blurredLayer(image: Bitmap, w: Float, h: Float, bg: BackgroundSettings, radiusPx: Float): Bitmap {
        val key = listOf(System.identityHashCode(image), w, h, bg.fit, bg.zoom, bg.offsetX, bg.offsetY, bg.blur)
        blurLayer?.let { if (key == blurKey && !it.isRecycled) return it }
        val k = max(1f, radiusPx / 6f)
        val lw = max(1, (w / k).roundToInt())
        val lh = max(1, (h / k).roundToInt())
        val layer = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
        val c = Canvas(layer)
        c.scale(lw / w, lh / h)
        val r = RectF()
        imageRect(image.width, image.height, w, h, bg, r)
        c.drawColor(bg.color or 0xFF000000.toInt())
        c.drawBitmap(image, null, r, Paint(Paint.FILTER_BITMAP_FLAG))
        BoxBlur.blurInPlace(layer, max(1, (radiusPx / k).roundToInt()))
        blurLayer?.recycle()
        blurLayer = layer
        blurKey = key
        return layer
    }

    fun release() {
        blurLayer?.recycle()
        blurLayer = null
        blurKey = null
    }

    // ---------------------------------------------------------------- dots

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, config: WallpaperConfig, progress: CountdownProgress) {
        val dots = config.dots
        val label = config.label
        if (!progress.isConfigured || progress.total <= 0) {
            textPaint.textSize = 34f * DotGridLayout.unit(w)
            textPaint.color = ColorMath.withAlpha(dots.accentColor, 0.8f)
            textPaint.textAlign = Paint.Align.CENTER
            val msg = if (progress.mode == CountdownMode.LIFE) "Set your birth date in DOTS" else "Set your goal dates in DOTS"
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            return
        }

        val g = DotGridLayout.compute(
            width = w,
            height = h,
            total = progress.total,
            mode = progress.mode,
            layout = config.layout,
            dotSizeUnits = dots.size,
            labelSizeUnits = label.size,
            showLabel = label.show,
        )
        val global = dots.globalOpacity.coerceIn(0f, 1f)
        val filled = ColorMath.withAlpha(dots.filledColor, dots.filledOpacity * global)
        val empty = ColorMath.withAlpha(dots.emptyColor, dots.emptyOpacity * global)
        val highlight = ColorMath.withAlpha(dots.highlightColor, global)
        val stroke = max(1f, g.dotSize * 0.12f)
        val half = g.dotSize / 2f
        val corner = g.dotSize * 0.3f

        for (i in 0 until progress.total) {
            val isPast = i < progress.elapsed
            val isToday = i == progress.currentIndex && dots.highlightToday
            val outlined = !isPast && !isToday && dots.emptyStyle == EmptyDotStyle.OUTLINE
            dotPaint.color = when {
                isPast -> filled
                isToday -> highlight
                else -> empty
            }
            if (outlined) {
                dotPaint.style = Paint.Style.STROKE
                dotPaint.strokeWidth = stroke
            } else {
                dotPaint.style = Paint.Style.FILL
            }
            val inset = if (outlined) stroke / 2f else 0f
            val cx = g.centerX(i)
            val cy = g.centerY(i)
            when (dots.shape) {
                DotShape.CIRCLE -> canvas.drawCircle(cx, cy, half - inset, dotPaint)
                DotShape.SQUARE -> canvas.drawRect(cx - half + inset, cy - half + inset, cx + half - inset, cy + half - inset, dotPaint)
                DotShape.ROUNDED_SQUARE -> {
                    rect.set(cx - half + inset, cy - half + inset, cx + half - inset, cy + half - inset)
                    canvas.drawRoundRect(rect, corner, corner, dotPaint)
                }
            }
        }

        if (label.show) {
            textPaint.textSize = g.labelSize
            textPaint.color = ColorMath.withAlpha(dots.accentColor, label.opacity * global)
            val alignOffset = g.gridWidth
            val (align, x) = when (config.layout.alignment) {
                com.dots.app.domain.model.GridAlignment.START -> Paint.Align.LEFT to g.originX
                com.dots.app.domain.model.GridAlignment.CENTER -> Paint.Align.CENTER to g.originX + alignOffset / 2f
                com.dots.app.domain.model.GridAlignment.END -> Paint.Align.RIGHT to g.originX + alignOffset
            }
            textPaint.textAlign = align
            canvas.drawText(labelText(progress), x, g.labelBaseline, textPaint)
        }
    }

    companion object {

        fun labelText(p: CountdownProgress): String {
            val nf = NumberFormat.getIntegerInstance()
            val pct = if (p.percent < 10) "%.1f".format(p.percent) else p.percent.toInt().toString()
            val unit = if (p.remaining == 1) p.unitLabel.removeSuffix("s") else p.unitLabel
            return "${nf.format(p.remaining)} $unit left  ·  $pct%"
        }

        /** Destination rect of the image for a given fit/zoom/pan (shared with the crop editor). */
        fun imageRect(iw: Int, ih: Int, w: Float, h: Float, bg: BackgroundSettings, out: RectF) {
            val sx = w / iw
            val sy = h / ih
            val base = when (bg.fit) {
                ImageFit.FILL -> max(sx, sy)
                ImageFit.FIT -> minOf(sx, sy)
                ImageFit.FIT_WIDTH -> sx
                ImageFit.FIT_HEIGHT -> sy
            }
            val s = base * bg.zoom.coerceIn(1f, 5f)
            val dw = iw * s
            val dh = ih * s
            val left = (w - dw) / 2f + bg.offsetX.coerceIn(-1f, 1f) * abs(w - dw) / 2f
            val top = (h - dh) / 2f + bg.offsetY.coerceIn(-1f, 1f) * abs(h - dh) / 2f
            out.set(left, top, left + dw, top + dh)
        }

        /** Brightness, contrast and saturation as one colour matrix, applied while drawing. */
        fun colorFilterFor(bg: BackgroundSettings): ColorMatrixColorFilter? {
            if (bg.brightness == 0f && bg.contrast == 1f && bg.saturation == 1f) return null
            val m = ColorMatrix().apply { setSaturation(bg.saturation.coerceIn(0f, 2f)) }
            val c = bg.contrast.coerceIn(0f, 2f)
            val t = (1f - c) * 128f + bg.brightness.coerceIn(-1f, 1f) * 128f
            m.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            return ColorMatrixColorFilter(m)
        }
    }
}
