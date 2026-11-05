package com.dots.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dots.app.container
import com.dots.app.data.AppSettings
import com.dots.app.data.ImageImportException
import com.dots.app.data.ThemePreference
import com.dots.app.data.WallpaperException
import com.dots.app.domain.color.ColorMath
import com.dots.app.domain.color.DotPalette
import com.dots.app.domain.color.PaletteSuggester
import com.dots.app.domain.countdown.CountdownCalculator
import com.dots.app.domain.countdown.CountdownProgress
import com.dots.app.domain.model.BuiltInPresets
import com.dots.app.domain.model.HistoryEntry
import com.dots.app.domain.model.Preset
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.domain.model.WallpaperTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class UiState(
    val loaded: Boolean = false,
    val config: WallpaperConfig = WallpaperConfig(),
    val settings: AppSettings = AppSettings(),
    val today: LocalDate = LocalDate.now(),
    val previewImage: Bitmap? = null,
    val suggestions: List<DotPalette> = emptyList(),
    val busy: String? = null,
    val screenAspect: Float = 9f / 19.5f,
    val homeZoom: Float = 1f,
) {
    val progress: CountdownProgress get() = CountdownCalculator.calculate(config.countdown, today)
    val hasUnappliedChanges: Boolean get() = settings.appliedConfig != config
}

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Launch(val intent: Intent) : UiEvent
}

private data class Local(
    val config: WallpaperConfig? = null,
    val today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val image: Bitmap? = null,
    val suggestions: List<DotPalette> = emptyList(),
    val busy: String? = null,
)

class DotsViewModel(app: Application) : AndroidViewModel(app) {

    private val c = app.container
    private val local = MutableStateFlow(Local())
    private val events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    private val aspect = c.generator.screenSize().let { it.width.toFloat() / it.height }
    private val homeZoom = c.generator.homeScreenZoom()

    val state: StateFlow<UiState> = combine(c.settings.settings, local) { s, l ->
        UiState(
            loaded = true,
            config = l.config ?: s.config,
            settings = s,
            today = l.today,
            previewImage = l.image,
            suggestions = l.suggestions,
            busy = l.busy,
            screenAspect = aspect,
            homeZoom = homeZoom,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState(screenAspect = aspect, homeZoom = homeZoom))

    private var saveJob: Job? = null

    init {
        // Load a preview-sized copy of the background image whenever it changes.
        viewModelScope.launch {
            state.map { it.config.background.imagePath }.distinctUntilChanged().collect { path ->
                // Old bitmaps are left to the GC rather than recycled: the preview may still
                // be drawing the previous frame with it.
                val bmp = path?.let { c.images.load(it, PREVIEW_LONG_SIDE) }
                val suggestions = bmp?.let { suggestionsFor(it) } ?: emptyList()
                local.update { it.copy(image = bmp, suggestions = suggestions) }
            }
        }
    }

    // ------------------------------------------------------------------ editing

    val config: WallpaperConfig get() = state.value.config

    /** Updates the design immediately (live preview) and persists it shortly after. */
    fun edit(transform: (WallpaperConfig) -> WallpaperConfig) {
        val updated = transform(config)
        local.update { it.copy(config = updated) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            c.settings.saveConfig(updated)
        }
    }

    private suspend fun flush() {
        saveJob?.cancel()
        c.settings.saveConfig(config)
    }

    fun onResume() {
        local.update { it.copy(today = LocalDate.now(ZoneId.systemDefault())) }
        c.scheduler.refreshNow()
    }

    // ------------------------------------------------------------------ image

    fun importImage(uri: Uri) = launchBusy("Importing photo…") {
        try {
            val file = c.images.import(uri)
            val bmp = c.images.load(file.absolutePath, PREVIEW_LONG_SIDE)
            val suggestions = bmp?.let { suggestionsFor(it) }.orEmpty()
            edit { cfg ->
                val withImage = cfg.copy(
                    background = cfg.background.copy(imagePath = file.absolutePath, zoom = 1f, offsetX = 0f, offsetY = 0f),
                )
                suggestions.firstOrNull()?.let { applyPalette(withImage, it) } ?: withImage
            }
            if (suggestions.isNotEmpty()) message("Dot colours matched to your photo")
        } catch (e: ImageImportException) {
            message(e.message ?: "Couldn't import that image")
        }
    }

    fun removeImage() = edit { it.copy(background = it.background.copy(imagePath = null)) }

    private suspend fun suggestionsFor(bmp: Bitmap): List<DotPalette> {
        val colors = c.images.extractColors(bmp)
        // Dots sit on the image *after* the dark overlay, so suggest against that.
        val effective = ColorMath.mix(colors.dominant, 0xFF000000.toInt(), config.background.darkOverlay)
        return PaletteSuggester.suggest(effective, colors.swatches)
    }

    fun applySuggestion(p: DotPalette) = edit { applyPalette(it, p) }

    private fun applyPalette(cfg: WallpaperConfig, p: DotPalette) = cfg.copy(
        dots = cfg.dots.copy(
            filledColor = p.filled,
            emptyColor = p.empty,
            highlightColor = p.highlight,
            accentColor = p.accent,
            emptyOpacity = p.emptyOpacity,
        ),
    )

    // ------------------------------------------------------------------ apply / export

    fun applyWallpaper(config: WallpaperConfig = this.config) = launchBusy("Applying wallpaper…") {
        try {
            flush()
            val target = state.value.settings.target
            c.updater.applyNow(config, target)
            message("Wallpaper applied to ${target.title.lowercase()} screen${if (target == WallpaperTarget.BOTH) "s" else ""}")
        } catch (e: WallpaperException) {
            message(e.message ?: "Couldn't apply the wallpaper")
        }
    }

    fun saveToGallery() = launchBusy("Saving…") {
        try {
            val bmp = c.generator.render(config)
            try {
                c.exporter.saveToGallery(bmp)
            } finally {
                bmp.recycle()
            }
            message("Saved to Pictures/DOTS")
        } catch (e: WallpaperException) {
            message(e.message ?: "Couldn't save the wallpaper")
        }
    }

    fun share() = launchBusy("Preparing…") {
        try {
            val bmp = c.generator.render(config)
            val intent = try {
                c.exporter.shareIntent(bmp)
            } finally {
                bmp.recycle()
            }
            events.send(UiEvent.Launch(intent))
        } catch (e: Exception) {
            Log.w(TAG, "Share failed", e)
            message("Couldn't share the wallpaper")
        }
    }

    // ------------------------------------------------------------------ presets

    fun applyBuiltInTheme(preset: Preset) = edit { BuiltInPresets.applyTheme(it, preset) }

    fun savePreset(name: String) = viewModelScope.launch {
        val s = state.value.settings
        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Design ${s.presets.size + 1}" },
            config = config,
            createdAt = System.currentTimeMillis(),
        )
        c.settings.savePresets(listOf(preset) + s.presets)
        message("Saved “${preset.name}”")
    }

    fun deletePreset(preset: Preset) = viewModelScope.launch {
        c.settings.savePresets(state.value.settings.presets.filterNot { it.id == preset.id })
        flush()
        c.updater.cleanupImages()
    }

    fun loadPreset(preset: Preset) = edit { preset.config }

    /** One-tap: load a saved design and put it on the device. */
    fun applyPreset(preset: Preset) {
        edit { preset.config }
        applyWallpaper(preset.config)
    }

    fun restoreDefaults() = edit { WallpaperConfig(countdown = it.countdown) }

    fun restoreHistory(entry: HistoryEntry) {
        val cfg = if (entry.config.background.imagePath?.let { File(it).exists() } == false) {
            entry.config.copy(background = entry.config.background.copy(imagePath = null))
        } else {
            entry.config
        }
        edit { cfg }
        applyWallpaper(cfg)
    }

    fun clearHistory() = viewModelScope.launch {
        state.value.settings.history.forEach { h -> h.thumbnailPath?.let { File(it).delete() } }
        c.settings.saveHistory(emptyList())
        flush()
        c.updater.cleanupImages()
    }

    // ------------------------------------------------------------------ colours & settings

    fun saveCustomColor(color: Int) = viewModelScope.launch {
        val list = state.value.settings.customColors
        if (color !in list) c.settings.saveCustomColors((listOf(color) + list).take(24))
    }

    fun removeCustomColor(color: Int) = viewModelScope.launch {
        c.settings.saveCustomColors(state.value.settings.customColors - color)
    }

    fun setTarget(t: WallpaperTarget) = viewModelScope.launch { c.settings.setTarget(t) }

    fun setAutoUpdate(enabled: Boolean) = viewModelScope.launch {
        c.settings.setAutoUpdate(enabled)
        if (enabled) c.scheduler.refreshNow()
    }

    fun setTheme(theme: ThemePreference) = viewModelScope.launch { c.settings.setTheme(theme) }

    // ------------------------------------------------------------------ helpers

    private fun launchBusy(label: String, block: suspend () -> Unit) = viewModelScope.launch {
        if (local.value.busy != null) return@launch
        local.update { it.copy(busy = label) }
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Operation failed", e)
            message("Something went wrong. Please try again.")
        } finally {
            local.update { it.copy(busy = null) }
        }
    }

    private suspend fun message(text: String) = events.send(UiEvent.Message(text))

    private companion object {
        const val TAG = "DotsViewModel"
        const val PREVIEW_LONG_SIDE = 1600
    }
}
