package com.dots.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dots.app.domain.color.ColorMath
import com.dots.app.ui.theme.CaptionStyle

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Caption(title, Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        content()
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, style = CaptionStyle, color = color)
}

/** Slider with a label on the left and the formatted value on the right. */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    format: (Float) -> String = { "%.0f".format(it) },
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                format(value),
                style = CaptionStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.height(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

fun percent(v: Float) = "${(v * 100).toInt()}%"

/** Pill-shaped segmented selector. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSel = option == selected
            val bg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, label = "seg")
            val fg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segText",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = fg, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/** Round colour swatch; checkerboard-free because opacity is always controlled separately. */
@Composable
fun Swatch(color: Int, size: Int = 28, selected: Boolean = false, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    var m = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
        .padding(if (selected) 3.dp else 0.dp)
        .clip(CircleShape)
        .background(Color(color or 0xFF000000.toInt()))
    if (onClick != null || onLongClick != null) {
        m = m.then(
            Modifier.combinedClickableCompat(onClick = { onClick?.invoke() }, onLongClick = onLongClick),
        )
    }
    Box(m)
}

/** A row showing a named colour with its HEX code; tapping it opens the picker. */
@Composable
fun ColorRow(title: String, color: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Swatch(color, size = 30)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(ColorMath.toHex(color), style = CaptionStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "chip",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
