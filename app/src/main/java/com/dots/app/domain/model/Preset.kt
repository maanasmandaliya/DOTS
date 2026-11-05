package com.dots.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val config: WallpaperConfig,
    val builtIn: Boolean = false,
    val createdAt: Long = 0L,
)

@Serializable
data class HistoryEntry(
    val id: String,
    val appliedAt: Long,
    val config: WallpaperConfig,
    val target: WallpaperTarget,
    /** Small JPEG thumbnail in app storage. */
    val thumbnailPath: String?,
    val summary: String,
)

object BuiltInPresets {

    private fun theme(
        id: String,
        name: String,
        bg: Long,
        filled: Long,
        empty: Long,
        highlight: Long,
        emptyOpacity: Float = 0.2f,
    ) = Preset(
        id = id,
        name = name,
        builtIn = true,
        config = WallpaperConfig(
            background = BackgroundSettings(color = bg.toInt()),
            dots = DotSettings(
                filledColor = filled.toInt(),
                emptyColor = empty.toInt(),
                highlightColor = highlight.toInt(),
                accentColor = filled.toInt(),
                emptyOpacity = emptyOpacity,
            ),
        ),
    )

    val MIDNIGHT = theme("builtin_midnight", "Midnight", 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF3B30)
    val SNOW = theme("builtin_snow", "Snow", 0xFFF5F5F2, 0xFF111111, 0xFF111111, 0xFFE11D48, 0.14f)
    val OCEAN = theme("builtin_ocean", "Ocean", 0xFF061A2E, 0xFF00D9FF, 0xFF7DD3FC, 0xFFFFFFFF)
    val SUNSET = theme("builtin_sunset", "Sunset", 0xFF140B08, 0xFFFF7A1A, 0xFFFFB27A, 0xFFFFE3B3)
    val FOREST = theme("builtin_forest", "Forest", 0xFF0B1F14, 0xFF86EFAC, 0xFFBBF7D0, 0xFFFDE68A)

    val all = listOf(MIDNIGHT, SNOW, OCEAN, SUNSET, FOREST)

    /** Theme-only part of a preset: keep the user's countdown and image, take the colours. */
    fun applyTheme(current: WallpaperConfig, preset: Preset): WallpaperConfig = current.copy(
        background = current.background.copy(color = preset.config.background.color),
        dots = current.dots.copy(
            filledColor = preset.config.dots.filledColor,
            emptyColor = preset.config.dots.emptyColor,
            highlightColor = preset.config.dots.highlightColor,
            accentColor = preset.config.dots.accentColor,
            emptyOpacity = preset.config.dots.emptyOpacity,
        ),
    )
}
