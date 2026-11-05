package com.dots.app.domain.model

import kotlinx.serialization.Serializable

/**
 * The complete, serializable description of a DOTS wallpaper.
 *
 * Every size here is expressed in "units": 1 unit = 1/1000 of the wallpaper width.
 * Because nothing is stored in pixels, the live preview (a few hundred pixels wide) and
 * the generated wallpaper (1440+ pixels wide) are produced from the exact same numbers
 * by the exact same renderer, so they always look identical.
 *
 * Colors are stored as ARGB ints. Opacities are 0f..1f.
 */
@Serializable
data class WallpaperConfig(
    val countdown: CountdownSettings = CountdownSettings(),
    val background: BackgroundSettings = BackgroundSettings(),
    val dots: DotSettings = DotSettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val label: LabelSettings = LabelSettings(),
)

@Serializable
data class CountdownSettings(
    val mode: CountdownMode = CountdownMode.YEAR,
    /** ISO-8601 date (yyyy-MM-dd), used by LIFE mode. */
    val birthDate: String? = null,
    val lifeExpectancyYears: Int = 80,
    /** ISO-8601 dates for CUSTOM mode. */
    val customStart: String? = null,
    val customEnd: String? = null,
    val customTitle: String = "Goal",
    val customUnit: TimeUnitKind = TimeUnitKind.DAY,
    /** 1 = Monday … 7 = Sunday (java.time.DayOfWeek value). */
    val weekStartsOn: Int = 1,
)

@Serializable
enum class CountdownMode(val title: String, val unitLabel: String) {
    YEAR("Year", "days"),
    MONTH("Month", "days"),
    WEEK("Week", "days"),
    LIFE("Life", "weeks"),
    CUSTOM("Goal", "days"),
}

@Serializable
enum class TimeUnitKind { DAY, WEEK }

@Serializable
data class BackgroundSettings(
    val color: Int = 0xFF000000.toInt(),
    /** Absolute path of the imported image inside app storage, or null for a solid background. */
    val imagePath: String? = null,
    val imageOpacity: Float = 1f,
    val fit: ImageFit = ImageFit.FILL,
    /** Extra zoom on top of the fit, 1f..5f. */
    val zoom: Float = 1f,
    /** Pan as a fraction of the available overflow, -1f..1f on each axis. */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** -1f..1f, 0 = unchanged. */
    val brightness: Float = 0f,
    /** 0f..2f, 1 = unchanged. */
    val contrast: Float = 1f,
    /** 0f..2f, 1 = unchanged, 0 = grayscale. */
    val saturation: Float = 1f,
    /** Blur radius in units (0..50). */
    val blur: Float = 0f,
    /** Black overlay opacity, 0 = off. */
    val darkOverlay: Float = 0f,
    val colorOverlay: Int = 0xFF3B82F6.toInt(),
    /** Color overlay opacity, 0 = off. */
    val colorOverlayOpacity: Float = 0f,
)

@Serializable
enum class ImageFit(val title: String) {
    /** Cover the whole wallpaper, cropping overflow (no distortion). */
    FILL("Fill"),
    /** Show the whole image, background colour fills the rest. */
    FIT("Fit"),
    /** Fit to wallpaper width. */
    FIT_WIDTH("Width"),
    /** Fit to wallpaper height. */
    FIT_HEIGHT("Height"),
}

@Serializable
data class DotSettings(
    val filledColor: Int = 0xFFFFFFFF.toInt(),
    val emptyColor: Int = 0xFFFFFFFF.toInt(),
    val highlightColor: Int = 0xFFFF5733.toInt(),
    val accentColor: Int = 0xFFFFFFFF.toInt(),
    val filledOpacity: Float = 1f,
    val emptyOpacity: Float = 0.18f,
    val globalOpacity: Float = 1f,
    /** Whether today's dot uses the highlight colour. */
    val highlightToday: Boolean = true,
    val shape: DotShape = DotShape.CIRCLE,
    val emptyStyle: EmptyDotStyle = EmptyDotStyle.SOLID,
    /** Dot diameter in units. */
    val size: Float = 26f,
)

@Serializable
enum class DotShape(val title: String) { CIRCLE("Circle"), ROUNDED_SQUARE("Rounded"), SQUARE("Square") }

@Serializable
enum class EmptyDotStyle(val title: String) { SOLID("Solid"), OUTLINE("Outline") }

@Serializable
data class LayoutSettings(
    /** 0 = automatic (mode-dependent). */
    val columns: Int = 0,
    val horizontalSpacing: Float = 14f,
    val verticalSpacing: Float = 14f,
    /** Horizontal padding inside the wallpaper, in units. */
    val paddingHorizontal: Float = 110f,
    /** Vertical padding (top and bottom safe area), in units. */
    val paddingVertical: Float = 240f,
    /** 0 = top/left, 0.5 = centre, 1 = bottom/right of the free space. */
    val verticalPosition: Float = 0.5f,
    val horizontalPosition: Float = 0.5f,
    /** How an incomplete last row is aligned. */
    val alignment: GridAlignment = GridAlignment.START,
)

@Serializable
enum class GridAlignment(val title: String) { START("Left"), CENTER("Centre"), END("Right") }

@Serializable
data class LabelSettings(
    val show: Boolean = true,
    /** Text size in units. */
    val size: Float = 30f,
    val opacity: Float = 0.7f,
)

/** Which wallpaper slot(s) to write. */
@Serializable
enum class WallpaperTarget(val title: String) { HOME("Home"), LOCK("Lock"), BOTH("Both") }
