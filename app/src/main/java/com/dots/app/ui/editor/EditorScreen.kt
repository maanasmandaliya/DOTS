package com.dots.app.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.Chip
import com.dots.app.ui.components.ColorPickerSheet
import com.dots.app.ui.components.Segmented
import com.dots.app.ui.preview.HomeLayout
import com.dots.app.ui.preview.PreviewMode
import com.dots.app.ui.preview.WallpaperPreview

val EDITOR_TABS = listOf("Countdown", "Background", "Dots", "Layout", "Presets")

/** Every colour the user can pick, with how to read and write it on the config. */
enum class ColorSlot(val title: String, val get: (WallpaperConfig) -> Int, val set: (WallpaperConfig, Int) -> WallpaperConfig) {
    FILLED("Filled dots", { it.dots.filledColor }, { c, v -> c.copy(dots = c.dots.copy(filledColor = v)) }),
    EMPTY("Empty dots", { it.dots.emptyColor }, { c, v -> c.copy(dots = c.dots.copy(emptyColor = v)) }),
    HIGHLIGHT("Today highlight", { it.dots.highlightColor }, { c, v -> c.copy(dots = c.dots.copy(highlightColor = v)) }),
    ACCENT("Accent / label", { it.dots.accentColor }, { c, v -> c.copy(dots = c.dots.copy(accentColor = v)) }),
    BACKGROUND("Background colour", { it.background.color }, { c, v -> c.copy(background = c.background.copy(color = v)) }),
    OVERLAY("Overlay colour", { it.background.colorOverlay }, { c, v -> c.copy(background = c.background.copy(colorOverlay = v)) }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: UiState,
    vm: DotsViewModel,
    snackbar: SnackbarHostState,
    initialTab: Int,
    onBack: () -> Unit,
    onCrop: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, EDITOR_TABS.lastIndex)) }
    var previewMode by rememberSaveable { mutableStateOf(PreviewMode.HOME) }
    var homeLayout by rememberSaveable { mutableStateOf(HomeLayout.CLASSIC) }
    var picker by rememberSaveable { mutableStateOf<ColorSlot?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Customize") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    Button(
                        onClick = { vm.applyWallpaper() },
                        enabled = state.busy == null,
                        modifier = Modifier.padding(end = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Apply") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Live preview: always visible while editing.
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WallpaperPreview(
                    config = state.config,
                    progress = state.progress,
                    image = state.previewImage,
                    aspect = state.screenAspect,
                    mode = previewMode,
                    homeLayout = homeLayout,
                    homeZoom = state.homeZoom,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Segmented(PreviewMode.entries, previewMode, { previewMode = it }, { it.title })
                if (previewMode == PreviewMode.HOME) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        HomeLayout.entries.forEach { l -> Chip(l.title, homeLayout == l, { homeLayout = l }) }
                    }
                }
            }

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EDITOR_TABS.forEachIndexed { i, t -> Chip(t, tab == i, { tab = i }) }
            }

            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(0.54f),
                label = "editorTab",
            ) { t ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (t) {
                        0 -> CountdownSection(state, vm)
                        1 -> BackgroundSection(state, vm, onCrop = onCrop, onPickColor = { picker = it })
                        2 -> DotsSection(state, vm, onPickColor = { picker = it })
                        3 -> LayoutSection(state, vm)
                        else -> PresetsSection(state, vm)
                    }
                    Box(Modifier.height(24.dp).width(1.dp))
                }
            }
        }
    }

    picker?.let { slot ->
        ColorPickerSheet(
            title = slot.title,
            color = slot.get(state.config),
            savedColors = state.settings.customColors,
            onColorChange = { c -> vm.edit { slot.set(it, c) } },
            onSaveColor = vm::saveCustomColor,
            onRemoveSavedColor = vm::removeCustomColor,
            onDismiss = { picker = null },
        )
    }
}
