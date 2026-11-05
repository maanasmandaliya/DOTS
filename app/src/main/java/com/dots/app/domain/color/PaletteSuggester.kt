package com.dots.app.domain.color

/**
 * A ready-to-apply set of dot colours derived from an image.
 */
data class DotPalette(
    val name: String,
    val background: Int,
    val filled: Int,
    val empty: Int,
    val highlight: Int,
    val accent: Int,
    val emptyOpacity: Float = 0.25f,
)

/**
 * Turns the dominant colours of an image into matching dot colour combinations.
 *
 * The input swatches come from AndroidX Palette (see ImagePaletteExtractor) but this class
 * only deals with plain ARGB ints, which keeps the colour logic testable.
 */
object PaletteSuggester {

    /**
     * @param dominant the most common colour of the image (what the dots sit on top of).
     * @param swatches other prominent colours (vibrant / muted), most important first.
     */
    fun suggest(dominant: Int, swatches: List<Int>): List<DotPalette> {
        val bg = dominant or 0xFF000000.toInt()
        val dark = ColorMath.isDark(bg)
        val neutral = if (dark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val vibrant = swatches.maxByOrNull { ColorMath.toHsv(it)[1] } ?: neutral
        val result = mutableListOf<DotPalette>()

        // 1. Maximum visibility — neutral dots, vibrant highlight.
        result += DotPalette(
            name = "Contrast",
            background = bg,
            filled = neutral,
            empty = neutral,
            highlight = ColorMath.ensureContrast(vibrant, bg, 3.0),
            accent = neutral,
            emptyOpacity = 0.22f,
        )

        // 2. Matching — the image's own vibrant colour, lifted for legibility.
        val matched = ColorMath.ensureContrast(vibrant, bg, 3.0)
        result += DotPalette(
            name = "Matching",
            background = bg,
            filled = matched,
            empty = ColorMath.ensureContrast(ColorMath.mix(matched, neutral, 0.5f), bg, 2.0),
            highlight = neutral,
            accent = matched,
            emptyOpacity = 0.3f,
        )

        // 3. Complementary — opposite hue of the dominant colour.
        val hsv = ColorMath.toHsv(bg)
        val comp = ColorMath.ensureContrast(
            ColorMath.fromHsv(hsv[0] + 180f, hsv[1].coerceAtLeast(0.55f), if (dark) 0.95f else 0.45f),
            bg,
            3.0,
        )
        result += DotPalette(
            name = "Complementary",
            background = bg,
            filled = comp,
            empty = comp,
            highlight = neutral,
            accent = comp,
        )

        // 4. Soft tone — a desaturated version of the image palette for subtle wallpapers.
        val muted = swatches.minByOrNull { ColorMath.toHsv(it)[1] } ?: neutral
        val soft = ColorMath.ensureContrast(ColorMath.mix(muted, neutral, 0.6f), bg, 2.5)
        result += DotPalette(
            name = "Soft",
            background = bg,
            filled = soft,
            empty = soft,
            highlight = ColorMath.ensureContrast(vibrant, bg, 2.5),
            accent = soft,
            emptyOpacity = 0.18f,
        )

        // 5. Analogous — neighbouring hue of the vibrant colour.
        val vh = ColorMath.toHsv(vibrant)
        val analog = ColorMath.ensureContrast(ColorMath.fromHsv(vh[0] + 35f, vh[1].coerceAtLeast(0.5f), 0.95f), bg, 3.0)
        result += DotPalette(
            name = "Analogous",
            background = bg,
            filled = analog,
            empty = matched,
            highlight = neutral,
            accent = analog,
        )
        return result
    }
}
