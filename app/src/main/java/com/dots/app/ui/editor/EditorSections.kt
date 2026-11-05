package com.dots.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dots.app.domain.color.ColorMath
import com.dots.app.domain.color.DotPalette
import com.dots.app.domain.countdown.CountdownCalculator
import com.dots.app.domain.model.BuiltInPresets
import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.DotShape
import com.dots.app.domain.model.EmptyDotStyle
import com.dots.app.domain.model.GridAlignment
import com.dots.app.domain.model.ImageFit
import com.dots.app.domain.model.TimeUnitKind
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.render.DotGridLayout
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.Caption
import com.dots.app.ui.components.Chip
import com.dots.app.ui.components.ColorRow
import com.dots.app.ui.components.LabeledSlider
import com.dots.app.ui.components.SectionCard
import com.dots.app.ui.components.Segmented
import com.dots.app.ui.components.ToggleRow
import com.dots.app.ui.components.percent
import com.dots.app.ui.preview.WallpaperCanvas
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ============================================================================ Countdown

@Composable
fun CountdownSection(state: UiState, vm: DotsViewModel) {
    val cd = state.config.countdown
    val p = state.progress
    SectionCard("Mode") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownMode.entries.forEach { m ->
                Chip(m.title, cd.mode == m, { vm.edit { it.copy(countdown = it.countdown.copy(mode = m)) } })
            }
        }
        Text(
            when (cd.mode) {
                CountdownMode.YEAR -> "One dot per day of ${p.title} (${p.total} days)."
                CountdownMode.MONTH -> "One dot per day of ${p.title} (${p.total} days)."
                CountdownMode.WEEK -> "Seven dots, one per day of the current week."
                CountdownMode.LIFE -> "Your life in weeks. Each dot is one week."
                CountdownMode.CUSTOM -> "Count down the days (or weeks) to a goal of your own."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (p.isConfigured) {
            val nf = NumberFormat.getIntegerInstance()
            Caption("${nf.format(p.elapsed)} passed · ${nf.format(p.remaining)} left · ${"%.1f".format(p.percent)}%")
        }
    }

    when (cd.mode) {
        CountdownMode.WEEK -> SectionCard("Week") {
            Text("Week starts on", style = MaterialTheme.typography.bodyMedium)
            Segmented(listOf(1, 7, 6), cd.weekStartsOn, { d -> vm.edit { it.copy(countdown = it.countdown.copy(weekStartsOn = d)) } }, {
                when (it) { 1 -> "Monday"; 7 -> "Sunday"; else -> "Saturday" }
            })
        }
        CountdownMode.LIFE -> SectionCard("Life calendar") {
            DateField("Date of birth", CountdownCalculator.parse(cd.birthDate), maxDate = state.today) { d ->
                vm.edit { it.copy(countdown = it.countdown.copy(birthDate = d.toString())) }
            }
            LabeledSlider("Life expectancy", cd.lifeExpectancyYears.toFloat(), { v ->
                vm.edit { it.copy(countdown = it.countdown.copy(lifeExpectancyYears = v.toInt())) }
            }, 30f..120f, format = { "${it.toInt()} years" })
            if (p.isConfigured) {
                val nf = NumberFormat.getIntegerInstance()
                Text("${nf.format(p.elapsed)} weeks lived · ${nf.format(p.remaining)} weeks remaining", style = MaterialTheme.typography.bodyMedium)
            }
        }
        CountdownMode.CUSTOM -> SectionCard("Goal") {
            OutlinedTextField(
                value = cd.customTitle,
                onValueChange = { t -> vm.edit { it.copy(countdown = it.countdown.copy(customTitle = t.take(40))) } },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Start date", CountdownCalculator.parse(cd.customStart)) { d ->
                vm.edit { it.copy(countdown = it.countdown.copy(customStart = d.toString())) }
            }
            DateField("Goal date", CountdownCalculator.parse(cd.customEnd)) { d ->
                vm.edit { it.copy(countdown = it.countdown.copy(customEnd = d.toString())) }
            }
            val start = CountdownCalculator.parse(cd.customStart)
            val end = CountdownCalculator.parse(cd.customEnd)
            if (start != null && end != null && !end.isAfter(start)) {
                Text("The goal date must be after the start date.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Text("One dot per", style = MaterialTheme.typography.bodyMedium)
            Segmented(TimeUnitKind.entries, cd.customUnit, { u -> vm.edit { it.copy(countdown = it.countdown.copy(customUnit = u)) } }, {
                if (it == TimeUnitKind.DAY) "Day" else "Week"
            })
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate?, maxDate: LocalDate? = null, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Choose",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) {
        // DatePicker works in UTC millis; convert through epoch days so no timezone shifts the date.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochDay()?.times(DAY_MS),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    maxDate == null || utcTimeMillis / DAY_MS <= maxDate.toEpochDay()
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(it / DAY_MS)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(pickerState) }
    }
}

private const val DAY_MS = 86_400_000L

// ============================================================================ Background

@Composable
fun BackgroundSection(state: UiState, vm: DotsViewModel, onCrop: () -> Unit, onPickColor: (ColorSlot) -> Unit) {
    val bg = state.config.background
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::importImage)
    }
    fun launchPicker() = pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun edit(f: (com.dots.app.domain.model.BackgroundSettings) -> com.dots.app.domain.model.BackgroundSettings) =
        vm.edit { it.copy(background = f(it.background)) }

    SectionCard("Photo") {
        val image = state.previewImage
        if (bg.imagePath != null && image != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    image.asImageBitmap(),
                    contentDescription = "Background photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your photo", style = MaterialTheme.typography.bodyLarge)
                    Text("${image.width} × ${image.height} preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCrop, modifier = Modifier.weight(1f)) { Text("Crop & zoom") }
                OutlinedButton(onClick = ::launchPicker, modifier = Modifier.weight(1f)) { Text("Replace") }
                OutlinedButton(onClick = vm::removeImage, modifier = Modifier.weight(1f)) { Text("Remove") }
            }
            Text("Fit", style = MaterialTheme.typography.bodyMedium)
            Segmented(ImageFit.entries, bg.fit, { f -> edit { it.copy(fit = f, offsetX = 0f, offsetY = 0f) } }, { it.title })
            LabeledSlider("Image opacity", bg.imageOpacity, { v -> edit { it.copy(imageOpacity = v) } }, 0f..1f, format = ::percent)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1f, 0.75f, 0.5f, 0.25f).forEach { o ->
                    Chip(percent(o), bg.imageOpacity == o, { edit { it.copy(imageOpacity = o) } })
                }
            }
        } else {
            Text(
                "Use one of your own photos behind the dots. JPG, PNG, WebP and HEIC are supported.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = ::launchPicker, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Choose photo") }
        }
    }

    SectionCard("Colour") {
        ColorRow("Background colour", bg.color) { onPickColor(ColorSlot.BACKGROUND) }
        if (bg.imagePath != null) {
            Text(
                "Shows through when the photo is transparent or doesn't fill the screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (bg.imagePath != null) {
        SectionCard("Adjust", trailing = {
            TextButton(onClick = { edit { it.copy(brightness = 0f, contrast = 1f, saturation = 1f, blur = 0f) } }) { Text("Reset") }
        }) {
            LabeledSlider("Brightness", bg.brightness, { v -> edit { it.copy(brightness = v) } }, -1f..1f, format = { "%+d".format((it * 100).toInt()) })
            LabeledSlider("Contrast", bg.contrast, { v -> edit { it.copy(contrast = v) } }, 0f..2f, format = ::percent)
            LabeledSlider("Saturation", bg.saturation, { v -> edit { it.copy(saturation = v) } }, 0f..2f, format = ::percent)
            LabeledSlider("Blur", bg.blur, { v -> edit { it.copy(blur = v) } }, 0f..50f)
        }
    }

    SectionCard("Overlay") {
        LabeledSlider("Dark overlay", bg.darkOverlay, { v -> edit { it.copy(darkOverlay = v) } }, 0f..0.9f, format = ::percent)
        ColorRow("Colour overlay", bg.colorOverlay) { onPickColor(ColorSlot.OVERLAY) }
        LabeledSlider("Colour overlay strength", bg.colorOverlayOpacity, { v -> edit { it.copy(colorOverlayOpacity = v) } }, 0f..0.9f, format = ::percent)
    }
}

// ============================================================================ Dots

private val COMBINATIONS = listOf(
    DotPalette("Mono", 0, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF3B30.toInt(), 0xFFFFFFFF.toInt(), 0.18f),
    DotPalette("Ink", 0, 0xFF111111.toInt(), 0xFF111111.toInt(), 0xFFE11D48.toInt(), 0xFF111111.toInt(), 0.14f),
    DotPalette("Cyan", 0, 0xFF00D9FF.toInt(), 0xFF7DD3FC.toInt(), 0xFFFFFFFF.toInt(), 0xFF00D9FF.toInt(), 0.22f),
    DotPalette("Ember", 0, 0xFFFF5733.toInt(), 0xFFFFB199.toInt(), 0xFFFFE3B3.toInt(), 0xFFFF5733.toInt(), 0.22f),
    DotPalette("Lilac", 0, 0xFFA855F7.toInt(), 0xFFD8B4FE.toInt(), 0xFFFFFFFF.toInt(), 0xFFA855F7.toInt(), 0.22f),
    DotPalette("Mint", 0, 0xFF86EFAC.toInt(), 0xFFBBF7D0.toInt(), 0xFFFDE68A.toInt(), 0xFF86EFAC.toInt(), 0.2f),
    DotPalette("Gold", 0, 0xFFFACC15.toInt(), 0xFFFEF08A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFACC15.toInt(), 0.2f),
)

@Composable
private fun PaletteChip(p: DotPalette, background: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp).clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color(background or 0xFF000000.toInt()))
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(p.filled, p.filled, p.highlight, ColorMath.withAlpha(p.empty, p.emptyOpacity.coerceAtLeast(0.3f))).forEach { c ->
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(c)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(p.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DotsSection(state: UiState, vm: DotsViewModel, onPickColor: (ColorSlot) -> Unit) {
    val d = state.config.dots
    fun edit(f: (com.dots.app.domain.model.DotSettings) -> com.dots.app.domain.model.DotSettings) =
        vm.edit { it.copy(dots = f(it.dots)) }

    if (state.suggestions.isNotEmpty()) {
        SectionCard("Suggested from your photo") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.suggestions.forEach { p -> PaletteChip(p, p.background) { vm.applySuggestion(p) } }
            }
        }
    }

    SectionCard("Combinations") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            COMBINATIONS.forEach { p -> PaletteChip(p, state.config.background.color) { vm.applySuggestion(p) } }
        }
    }

    SectionCard("Colours") {
        ColorRow(ColorSlot.FILLED.title, d.filledColor) { onPickColor(ColorSlot.FILLED) }
        ColorRow(ColorSlot.EMPTY.title, d.emptyColor) { onPickColor(ColorSlot.EMPTY) }
        ColorRow(ColorSlot.HIGHLIGHT.title, d.highlightColor) { onPickColor(ColorSlot.HIGHLIGHT) }
        ColorRow(ColorSlot.ACCENT.title, d.accentColor) { onPickColor(ColorSlot.ACCENT) }
        ToggleRow("Highlight today", d.highlightToday, { v -> edit { it.copy(highlightToday = v) } }, "Mark the current day or week in the highlight colour")
    }

    SectionCard("Transparency") {
        LabeledSlider("Filled dots", d.filledOpacity, { v -> edit { it.copy(filledOpacity = v) } }, 0f..1f, format = ::percent)
        LabeledSlider("Empty dots", d.emptyOpacity, { v -> edit { it.copy(emptyOpacity = v) } }, 0f..1f, format = ::percent)
        LabeledSlider("All dots", d.globalOpacity, { v -> edit { it.copy(globalOpacity = v) } }, 0f..1f, format = ::percent)
    }

    SectionCard("Shape & size") {
        Segmented(DotShape.entries, d.shape, { s -> edit { it.copy(shape = s) } }, { it.title })
        Text("Empty dots", style = MaterialTheme.typography.bodyMedium)
        Segmented(EmptyDotStyle.entries, d.emptyStyle, { s -> edit { it.copy(emptyStyle = s) } }, { it.title })
        LabeledSlider("Dot size", d.size, { v -> edit { it.copy(size = v) } }, 4f..80f)
        val layout = state.config.layout
        LabeledSlider("Spacing", (layout.horizontalSpacing + layout.verticalSpacing) / 2f, { v ->
            vm.edit { it.copy(layout = it.layout.copy(horizontalSpacing = v, verticalSpacing = v)) }
        }, 0f..60f)
    }
}

// ============================================================================ Layout

@Composable
fun LayoutSection(state: UiState, vm: DotsViewModel) {
    val l = state.config.layout
    val label = state.config.label
    fun edit(f: (com.dots.app.domain.model.LayoutSettings) -> com.dots.app.domain.model.LayoutSettings) =
        vm.edit { it.copy(layout = f(it.layout)) }
    val autoCols = DotGridLayout.defaultColumns(state.config.countdown.mode, state.progress.total.coerceAtLeast(1))

    SectionCard("Grid") {
        ToggleRow("Automatic columns", l.columns == 0, { auto -> edit { it.copy(columns = if (auto) 0 else autoCols) } }, "Best fit for ${state.config.countdown.mode.title.lowercase()} mode ($autoCols)")
        if (l.columns != 0) {
            LabeledSlider("Columns", l.columns.toFloat(), { v -> edit { it.copy(columns = v.toInt()) } }, 1f..60f)
        }
        Text("Last row alignment", style = MaterialTheme.typography.bodyMedium)
        Segmented(GridAlignment.entries, l.alignment, { a -> edit { it.copy(alignment = a) } }, { it.title })
        LabeledSlider("Horizontal spacing", l.horizontalSpacing, { v -> edit { it.copy(horizontalSpacing = v) } }, 0f..60f)
        LabeledSlider("Vertical spacing", l.verticalSpacing, { v -> edit { it.copy(verticalSpacing = v) } }, 0f..60f)
    }

    SectionCard("Position") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Top" to 0f, "Centre" to 0.5f, "Bottom" to 1f).forEach { (n, v) ->
                Chip(n, l.verticalPosition == v, { edit { it.copy(verticalPosition = v) } })
            }
        }
        LabeledSlider("Vertical", l.verticalPosition, { v -> edit { it.copy(verticalPosition = v) } }, 0f..1f, format = ::percent)
        LabeledSlider("Horizontal", l.horizontalPosition, { v -> edit { it.copy(horizontalPosition = v) } }, 0f..1f, format = ::percent)
    }

    SectionCard("Padding") {
        LabeledSlider("Sides", l.paddingHorizontal, { v -> edit { it.copy(paddingHorizontal = v) } }, 0f..350f)
        LabeledSlider("Top & bottom", l.paddingVertical, { v -> edit { it.copy(paddingVertical = v) } }, 0f..700f)
        Text(
            "Keep clear of the clock and the dock. The grid shrinks automatically if it doesn't fit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard("Label") {
        ToggleRow("Show progress label", label.show, { v -> vm.edit { it.copy(label = it.label.copy(show = v)) } }, "e.g. “55 days left · 85%”")
        if (label.show) {
            LabeledSlider("Text size", label.size, { v -> vm.edit { it.copy(label = it.label.copy(size = v)) } }, 12f..70f)
            LabeledSlider("Text opacity", label.opacity, { v -> vm.edit { it.copy(label = it.label.copy(opacity = v)) } }, 0.1f..1f, format = ::percent)
        }
    }
}

// ============================================================================ Presets

@Composable
private fun MiniWallpaper(state: UiState, config: WallpaperConfig, showImage: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(state.screenAspect)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
    ) {
        WallpaperCanvas(
            config,
            CountdownCalculator.calculate(config.countdown, state.today),
            if (showImage) state.previewImage else null,
            Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PresetsSection(state: UiState, vm: DotsViewModel) {
    var saveDialog by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    SectionCard("Themes") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BuiltInPresets.all.forEach { preset ->
                val cfg = BuiltInPresets.applyTheme(state.config, preset).let { it.copy(background = it.background.copy(imagePath = null)) }
                Column(Modifier.width(72.dp).clickable { vm.edit { cfg } }) {
                    MiniWallpaper(state, cfg, showImage = false, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(preset.name, style = MaterialTheme.typography.labelMedium)
                }
            }
            val suggestion = state.suggestions.firstOrNull()
            if (state.config.background.imagePath != null && suggestion != null) {
                Column(Modifier.width(72.dp).clickable { vm.applySuggestion(suggestion) }) {
                    val cfg = state.config.copy(
                        dots = state.config.dots.copy(
                            filledColor = suggestion.filled, emptyColor = suggestion.empty,
                            highlightColor = suggestion.highlight, accentColor = suggestion.accent,
                        ),
                    )
                    MiniWallpaper(state, cfg, showImage = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Custom image", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        Text(
            "Themes change colours only; your countdown and layout stay the same.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard("Saved designs", trailing = { TextButton(onClick = { saveDialog = true }) { Text("Save current") } }) {
        if (state.settings.presets.isEmpty()) {
            Text("Save a design to switch back to it with one tap.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.settings.presets.forEach { preset ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniWallpaper(
                    state,
                    preset.config,
                    showImage = preset.config.background.imagePath == state.config.background.imagePath,
                    modifier = Modifier.width(44.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(preset.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Caption(preset.config.countdown.mode.title)
                }
                TextButton(onClick = { vm.loadPreset(preset) }) { Text("Load") }
                TextButton(onClick = { vm.applyPreset(preset) }) { Text("Apply") }
                TextButton(onClick = { vm.deletePreset(preset) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    SectionCard("Export") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = vm::saveToGallery, enabled = state.busy == null, modifier = Modifier.weight(1f)) { Text("Save to gallery") }
            OutlinedButton(onClick = vm::share, enabled = state.busy == null, modifier = Modifier.weight(1f)) { Text("Share") }
        }
    }

    SectionCard("Reset") {
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Restore default design") }
    }

    if (saveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text("Save design") },
            text = {
                OutlinedTextField(name, { name = it.take(32) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = { TextButton(onClick = { vm.savePreset(name); saveDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Cancel") } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Restore defaults?") },
            text = { Text("Colours, photo and layout go back to the original DOTS look. Your countdown dates are kept.") },
            confirmButton = { TextButton(onClick = { vm.restoreDefaults(); confirmReset = false }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
    Spacer(Modifier.padding(4.dp))
}
