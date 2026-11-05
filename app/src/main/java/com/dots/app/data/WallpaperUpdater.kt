package com.dots.app.data

import com.dots.app.domain.model.HistoryEntry
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.domain.model.WallpaperTarget
import com.dots.app.render.WallpaperRenderer
import java.io.File
import java.util.UUID

/**
 * Use case that ties generation, application and bookkeeping together. Called both from
 * the UI ("Apply") and from the background worker (daily refresh).
 */
class WallpaperUpdater(
    private val settings: SettingsRepository,
    private val generator: WallpaperGenerator,
    private val exporter: WallpaperExporter,
    private val images: ImageRepository,
) {

    /** Renders and applies [config] now, and records it as the design to keep updated. */
    suspend fun applyNow(config: WallpaperConfig, target: WallpaperTarget) {
        val progress = generator.progress(config)
        val bitmap = generator.render(config)
        try {
            generator.apply(bitmap, target)
            settings.markApplied(config, target, progress.renderKey)
            addHistory(config, target, exporter.saveThumbnail(bitmap), WallpaperRenderer.labelText(progress))
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Re-renders the applied design if the countdown moved on since the last render.
     * Returns true if the wallpaper was changed. Cheap when nothing changed: no rendering
     * and no disk writes, which keeps the periodic work battery friendly.
     */
    suspend fun refreshIfNeeded(): Boolean {
        val s = settings.current()
        val config = s.appliedConfig ?: return false
        if (!s.autoUpdate) return false
        val progress = generator.progress(config)
        if (progress.renderKey == s.appliedRenderKey) return false
        val bitmap = generator.render(config)
        try {
            generator.apply(bitmap, s.appliedTarget)
            settings.markApplied(config, s.appliedTarget, progress.renderKey)
        } finally {
            bitmap.recycle()
        }
        return true
    }

    private suspend fun addHistory(config: WallpaperConfig, target: WallpaperTarget, thumb: String, summary: String) {
        val s = settings.current()
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            appliedAt = System.currentTimeMillis(),
            config = config,
            target = target,
            thumbnailPath = thumb,
            summary = "${config.countdown.mode.title} · $summary",
        )
        val updated = (listOf(entry) + s.history).take(MAX_HISTORY)
        (s.history - updated.toSet()).forEach { old -> old.thumbnailPath?.let { File(it).delete() } }
        settings.saveHistory(updated)
        cleanupImages()
    }

    suspend fun cleanupImages() {
        val s = settings.current()
        val referenced = buildSet {
            s.config.background.imagePath?.let(::add)
            s.appliedConfig?.background?.imagePath?.let(::add)
            s.presets.forEach { p -> p.config.background.imagePath?.let(::add) }
            s.history.forEach { h -> h.config.background.imagePath?.let(::add) }
        }
        images.cleanup(referenced)
    }

    private companion object {
        const val MAX_HISTORY = 30
    }
}
