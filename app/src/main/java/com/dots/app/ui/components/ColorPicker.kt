package com.dots.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dots.app.domain.color.ColorMath

private enum class PickerTab(val title: String) { PALETTE("Palette"), SPECTRUM("Spectrum"), RGB("RGB") }

/**
 * Bottom-sheet colour picker: large palette, full HSV spectrum, RGB sliders, validated HEX
 * entry and the user's saved colours. Every change is reported immediately through
 * [onColorChange] so the wallpaper preview behind the sheet updates live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    title: String,
    color: Int,
    savedColors: List<Int>,
    onColorChange: (Int) -> Unit,
    onSaveColor: (Int) -> Unit,
    onRemoveSavedColor: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by rememberSaveable { mutableStateOf(PickerTab.PALETTE) }
    // HSV is kept locally so hue survives when saturation/value hit zero.
    val initial = remember { ColorMath.toHsv(color) }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var sat by remember { mutableFloatStateOf(initial[1]) }
    var value by remember { mutableFloatStateOf(initial[2]) }
    var current by remember { mutableIntStateOf(color or 0xFF000000.toInt()) }

    fun set(c: Int, syncHsv: Boolean = true) {
        current = c or 0xFF000000.toInt()
        if (syncHsv) {
            val hsv = ColorMath.toHsv(current)
            if (hsv[1] > 0f && hsv[2] > 0f) hue = hsv[0]
            sat = hsv[1]
            value = hsv[2]
        }
        onColorChange(current)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(current))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Caption(ColorMath.toHex(current))
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }

            HexField(current) { set(it) }

            Segmented(PickerTab.entries, tab, { tab = it }, { it.title })

            when (tab) {
                PickerTab.PALETTE -> PaletteGrid(current) { set(it) }
                PickerTab.SPECTRUM -> {
                    SaturationValuePanel(hue, sat, value) { s, v ->
                        sat = s; value = v
                        set(ColorMath.fromHsv(hue, s, v), syncHsv = false)
                    }
                    HueBar(hue) { h ->
                        hue = h
                        set(ColorMath.fromHsv(h, sat, value), syncHsv = false)
                    }
                }
                PickerTab.RGB -> {
                    val channels = listOf("Red" to ColorMath.red(current), "Green" to ColorMath.green(current), "Blue" to ColorMath.blue(current))
                    channels.forEachIndexed { i, (name, v) ->
                        LabeledSlider(name, v.toFloat(), { nv ->
                            val r = if (i == 0) nv.toInt() else ColorMath.red(current)
                            val g = if (i == 1) nv.toInt() else ColorMath.green(current)
                            val b = if (i == 2) nv.toInt() else ColorMath.blue(current)
                            set(ColorMath.argb(255, r, g, b))
                        }, 0f..255f)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption("Saved colours · long-press to remove")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onSaveColor(current) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Save colour", Modifier.size(18.dp))
                    }
                    savedColors.forEach { c ->
                        Swatch(c, size = 36, selected = c == current, onClick = { set(c) }, onLongClick = { onRemoveSavedColor(c) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HexField(color: Int, onValid: (Int) -> Unit) {
    var text by remember { mutableStateOf(ColorMath.toHex(color)) }
    var focusedEdit by remember { mutableStateOf(false) }
    // Keep the field in sync with other inputs unless the user is mid-edit.
    LaunchedEffect(color) {
        if (!focusedEdit || ColorMath.parseHex(text) == color) text = ColorMath.toHex(color)
    }
    val valid = ColorMath.isValidHex(text)
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.trim().take(9).let { if (it.startsWith("#")) it else "#$it" }.uppercase()
            text = cleaned
            focusedEdit = true
            ColorMath.parseHex(cleaned)?.let(onValid)
        },
        label = { Text("HEX") },
        singleLine = true,
        isError = !valid,
        supportingText = { if (!valid) Text("Use #RGB, #RRGGBB or #AARRGGBB") },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusedEdit = false }),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

/** 12 hues × 6 tones plus a neutral row — 84 colours. */
private val PALETTE: List<List<Int>> = buildList {
    add(listOf(0xFFFFFFFF, 0xFFE5E5E5, 0xFFBDBDBD, 0xFF8E8E8E, 0xFF5C5C5C, 0xFF2E2E2E, 0xFF000000).map { it.toInt() })
    val hues = listOf(0f, 15f, 30f, 45f, 60f, 90f, 140f, 175f, 195f, 220f, 265f, 300f, 330f)
    val tones = listOf(0.25f to 1f, 0.5f to 1f, 0.8f to 1f, 1f to 0.85f, 1f to 0.6f, 0.9f to 0.35f)
    tones.forEach { (s, v) -> add(hues.map { ColorMath.fromHsv(it, s, v) }) }
}

@Composable
private fun PaletteGrid(selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PALETTE.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                if (c == selected) 2.dp else 0.5.dp,
                                if (c == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            )
                            .clickable { onPick(c) },
                    )
                }
                // Neutral row has fewer entries; pad so circles keep the same size.
                repeat(13 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SaturationValuePanel(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit) {
    val hueColor = Color(ColorMath.fromHsv(hue, 1f, 1f))
    fun handle(o: Offset, w: Float, h: Float) = onChange((o.x / w).coerceIn(0f, 1f), 1f - (o.y / h).coerceIn(0f, 1f))
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                detectTapGestures { handle(it, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> handle(change.position, size.width.toFloat(), size.height.toFloat()) }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val p = Offset(sat * size.width, (1f - value) * size.height)
        drawCircle(Color.White, 11.dp.toPx(), p, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.3f), 13.dp.toPx(), p, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit) {
    val rainbow = remember { (0..12).map { Color(ColorMath.fromHsv(it * 30f, 1f, 1f)) } }
    fun handle(x: Float, w: Float) = onChange(((x / w).coerceIn(0f, 1f) * 359.9f))
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) { detectTapGestures { handle(it.x, size.width.toFloat()) } }
            .pointerInput(Unit) { detectDragGestures { c, _ -> handle(c.position.x, size.width.toFloat()) } },
    ) {
        drawRoundRect(Brush.horizontalGradient(rainbow), cornerRadius = CornerRadius(size.height / 2))
        val x = hue / 360f * size.width
        drawCircle(Color.White, size.height / 2 + 2.dp.toPx(), Offset(x.coerceIn(size.height / 2, size.width - size.height / 2), size.height / 2), style = Stroke(3.dp.toPx()))
    }
}
