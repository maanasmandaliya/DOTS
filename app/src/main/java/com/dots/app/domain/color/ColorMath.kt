package com.dots.app.domain.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Framework-free colour helpers working on ARGB ints, so they are unit-testable on the JVM.
 */
object ColorMath {

    private val HEX_PATTERN = Regex("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    fun isValidHex(input: String): Boolean = HEX_PATTERN.matches(input.trim())

    /**
     * Parses #RGB, #RRGGBB or #AARRGGBB (the leading # is optional).
     * Returns null for anything else so callers can show a validation error.
     */
    fun parseHex(input: String): Int? {
        val match = HEX_PATTERN.matchEntire(input.trim()) ?: return null
        val hex = match.groupValues[1]
        return when (hex.length) {
            3 -> {
                val r = hex[0].digitToInt(16) * 17
                val g = hex[1].digitToInt(16) * 17
                val b = hex[2].digitToInt(16) * 17
                argb(255, r, g, b)
            }
            6 -> (0xFF000000 or hex.toLong(16)).toInt()
            else -> hex.toLong(16).toInt()
        }
    }

    /** Formats as #RRGGBB (alpha is controlled separately by opacity sliders). */
    fun toHex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun alpha(c: Int) = (c ushr 24) and 0xFF
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun withAlpha(color: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or (color and 0xFFFFFF)

    /** Returns [hue 0..360, saturation 0..1, value 0..1]. */
    fun toHsv(color: Int): FloatArray {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }.let { if (it < 0) it + 360f else it }
        val s = if (mx == 0f) 0f else d / mx
        return floatArrayOf(h, s, mx)
    }

    fun fromHsv(h: Float, s: Float, v: Float, alpha: Int = 255): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1 - abs((hh / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) = when {
            hh < 60 -> Triple(c, x, 0f)
            hh < 120 -> Triple(x, c, 0f)
            hh < 180 -> Triple(0f, c, x)
            hh < 240 -> Triple(0f, x, c)
            hh < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(alpha, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** WCAG relative luminance, 0..1. */
    fun luminance(color: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(red(color)) + 0.7152 * ch(green(color)) + 0.0722 * ch(blue(color))
    }

    /** WCAG contrast ratio, 1..21. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    fun isDark(color: Int) = luminance(color) < 0.4

    /** Linear blend; t = 0 → a, t = 1 → b. */
    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun lerp(x: Int, y: Int) = (x + (y - x) * k).roundToInt()
        return argb(255, lerp(red(a), red(b)), lerp(green(a), green(b)), lerp(blue(a), blue(b)))
    }

    /**
     * Moves [color] towards white or black (whichever increases contrast) until it reaches
     * [minRatio] against [background]. Keeps the hue so suggestions still "match" the image.
     */
    fun ensureContrast(color: Int, background: Int, minRatio: Double = 3.0): Int {
        if (contrast(color, background) >= minRatio) return color
        val target = if (isDark(background)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        var t = 0f
        var c = color
        while (t < 1f && contrast(c, background) < minRatio) {
            t += 0.05f
            c = mix(color, target, t)
        }
        return c
    }
}
