package com.dots.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dots.app.domain.countdown.CountdownCalculator
import com.dots.app.domain.countdown.CountdownProgress
import com.dots.app.domain.model.BuiltInPresets
import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.Preset
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.domain.model.WallpaperTarget
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.Caption
import com.dots.app.ui.components.Chip
import com.dots.app.ui.components.DotMatrixText
import com.dots.app.ui.components.Segmented
import com.dots.app.ui.preview.PreviewMode
import com.dots.app.ui.preview.WallpaperCanvas
import com.dots.app.ui.preview.WallpaperPreview
import com.dots.app.ui.theme.DotsRed
import java.text.NumberFormat

@Composable
fun DashboardScreen(
    state: UiState,
    vm: DotsViewModel,
    snackbar: SnackbarHostState,
    onCustomize: (tab: Int) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val progress = state.progress
    var previewMode by rememberSaveable { mutableStateOf(PreviewMode.HOME) }
    val nf = NumberFormat.getIntegerInstance()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DotMatrixText("DOTS", dot = 4.dp, gap = 1.5.dp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHistory) { Icon(Icons.Default.DateRange, "Wallpaper history") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
            }

            // Mode chips
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountdownMode.entries.forEach { mode ->
                    Chip(mode.title, selected = state.config.countdown.mode == mode, onClick = {
                        vm.edit { it.copy(countdown = it.countdown.copy(mode = mode)) }
                    })
                }
            }

            // Hero stats
            AnimatedContent(
                targetState = progress,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hero",
            ) { p -> Hero(p, onSetup = { onCustomize(0) }) }

            Spacer(Modifier.height(20.dp))
            if (progress.isConfigured) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat("Passed", nf.format(progress.elapsed), progress.unitLabel, Modifier.weight(1f))
                    Stat("Remaining", nf.format(progress.remaining), progress.unitLabel, Modifier.weight(1f))
                    Stat("Total", nf.format(progress.total), progress.unitLabel, Modifier.weight(1f))
                }
            }

            // Preview
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WallpaperPreview(
                    config = state.config,
                    progress = progress,
                    image = state.previewImage,
                    aspect = state.screenAspect,
                    mode = previewMode,
                    homeZoom = state.homeZoom,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .clickable { onCustomize(1) },
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 64.dp)) {
                Segmented(PreviewMode.entries, previewMode, { previewMode = it }, { it.title })
            }

            // Actions
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Caption("Apply to")
                Segmented(WallpaperTarget.entries, state.settings.target, vm::setTarget, { it.title })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onCustomize(1) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("Customize") }
                    Button(
                        onClick = { vm.applyWallpaper() },
                        enabled = state.busy == null,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) { Text("Apply wallpaper") }
                }
                StatusLine(state)
            }

            // Presets
            Spacer(Modifier.height(32.dp))
            Caption("Themes", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BuiltInPresets.all.forEach { preset ->
                    val cfg = BuiltInPresets.applyTheme(state.config, preset).let {
                        // Theme cards show the colours on their own, not over the user's photo.
                        it.copy(background = it.background.copy(imagePath = null))
                    }
                    PresetCard(preset.name, cfg, progress, state, onClick = {
                        vm.edit { cfg }
                        vm.applyWallpaper(cfg)
                    })
                }
            }
            if (state.settings.presets.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Caption("Saved designs", Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.settings.presets.forEach { preset: Preset ->
                        PresetCard(
                            preset.name,
                            preset.config,
                            CountdownCalculator.calculate(preset.config.countdown, state.today),
                            state,
                            showImage = preset.config.background.imagePath == state.config.background.imagePath,
                            onClick = { vm.applyPreset(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(p: CountdownProgress, onSetup: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (!p.isConfigured) {
            Text(
                if (p.mode == CountdownMode.LIFE) "Add your birth date to see your life in weeks." else "Set a start and goal date for your countdown.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSetup) { Text("Set up") }
            return
        }
        Caption(p.title)
        Spacer(Modifier.height(14.dp))
        val pct = if (p.percent < 10) "%.1f%%".format(p.percent) else "${p.percent.toInt()}%"
        DotMatrixText(pct, dot = 7.dp, gap = 3.dp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(14.dp))
        Text(
            "of the ${modeNoun(p.mode)} complete",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        ProgressDots(p.fraction.toFloat())
    }
}

private fun modeNoun(mode: CountdownMode) = when (mode) {
    CountdownMode.YEAR -> "year"
    CountdownMode.MONTH -> "month"
    CountdownMode.WEEK -> "week"
    CountdownMode.LIFE -> "expected lifetime"
    CountdownMode.CUSTOM -> "goal"
}

/** A 40-dot progress bar in the brand language. */
@Composable
private fun ProgressDots(fraction: Float) {
    val count = 40
    val filled = (fraction * count).toInt()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < filled -> MaterialTheme.colorScheme.onBackground
                            i == filled -> DotsRed
                            else -> MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Caption(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusLine(state: UiState) {
    val text = when {
        state.settings.appliedConfig == null -> "Not applied yet"
        state.hasUnappliedChanges -> "You have changes that aren't applied yet"
        state.settings.autoUpdate -> "Applied · updates automatically every day"
        else -> "Applied · automatic updates are off"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val dotColor = if (state.settings.appliedConfig != null && !state.hasUnappliedChanges) Color(0xFF34C759) else DotsRed
        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PresetCard(
    name: String,
    config: WallpaperConfig,
    progress: CountdownProgress,
    state: UiState,
    showImage: Boolean = false,
    onClick: () -> Unit,
) {
    Column(Modifier.width(96.dp).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(state.screenAspect)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        ) {
            WallpaperCanvas(config, progress, if (showImage) state.previewImage else null, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
