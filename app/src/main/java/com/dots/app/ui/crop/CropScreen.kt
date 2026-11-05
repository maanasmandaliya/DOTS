package com.dots.app.ui.crop

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dots.app.domain.model.ImageFit
import com.dots.app.ui.DotsViewModel
import com.dots.app.ui.UiState
import com.dots.app.ui.components.Caption
import com.dots.app.ui.components.LabeledSlider
import com.dots.app.ui.components.Segmented
import com.dots.app.render.WallpaperRenderer
import com.dots.app.ui.preview.WallpaperCanvas
import kotlin.math.abs

/**
 * Pinch to zoom and drag to reposition the photo inside the exact screen shape. The crop
 * is stored as zoom + normalized offset, so it maps to any wallpaper resolution.
 */
@Composable
fun CropScreen(state: UiState, vm: DotsViewModel, onDone: () -> Unit) {
    val bg = state.config.background
    val image = state.previewImage
    var frame by remember { mutableStateOf(IntSize.Zero) }
    var showGrid by remember { mutableStateOf(true) }
    val latest by rememberUpdatedState(bg)
    val original = remember { bg }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                vm.edit { it.copy(background = it.background.copy(zoom = original.zoom, offsetX = original.offsetX, offsetY = original.offsetY, fit = original.fit)) }
                onDone()
            }) { Text("Cancel", color = Color.White) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Caption("Crop & position", color = Color.White.copy(alpha = 0.7f)) }
            Button(onClick = onDone) { Text("Done") }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .aspectRatio(state.screenAspect, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(18.dp))
                    .onSizeChanged { frame = it }
                    .pointerInput(image) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            if (image == null || frame.width == 0) return@detectTransformGestures
                            val b = latest
                            val zoom = (b.zoom * zoomChange).coerceIn(1f, 5f)
                            val r = RectF()
                            WallpaperRenderer.imageRect(image.width, image.height, frame.width.toFloat(), frame.height.toFloat(), b.copy(zoom = zoom), r)
                            val slackX = abs(frame.width - r.width()) / 2f
                            val slackY = abs(frame.height - r.height()) / 2f
                            val ox = if (slackX > 0.5f) (b.offsetX + pan.x / slackX).coerceIn(-1f, 1f) else 0f
                            val oy = if (slackY > 0.5f) (b.offsetY + pan.y / slackY).coerceIn(-1f, 1f) else 0f
                            vm.edit { it.copy(background = it.background.copy(zoom = zoom, offsetX = ox, offsetY = oy)) }
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (showGrid) {
                            val c = Color.White.copy(alpha = 0.35f)
                            for (i in 1..2) {
                                drawLine(c, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height), 1.dp.toPx())
                                drawLine(c, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), 1.dp.toPx())
                            }
                        }
                    },
            ) {
                WallpaperCanvas(state.config, state.progress, image, Modifier.fillMaxSize())
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Segmented(ImageFit.entries, bg.fit, { f -> vm.edit { it.copy(background = it.background.copy(fit = f, offsetX = 0f, offsetY = 0f)) } }, { it.title })
            LabeledSlider("Zoom", bg.zoom, { z -> vm.edit { it.copy(background = it.background.copy(zoom = z)) } }, 1f..5f, format = { "%.1f×".format(it) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.edit { it.copy(background = it.background.copy(zoom = 1f, offsetX = 0f, offsetY = 0f)) } }) { Text("Reset") }
                TextButton(onClick = { showGrid = !showGrid }) { Text(if (showGrid) "Hide grid" else "Show grid") }
            }
            Caption("Pinch to zoom · drag to move")
        }
    }
}
