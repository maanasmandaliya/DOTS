package com.dots.app.ui.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dots.app.domain.countdown.CountdownProgress
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.render.WallpaperRenderer
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class PreviewMode(val title: String) { WALLPAPER("Wallpaper"), HOME("Home"), LOCK("Lock") }

enum class HomeLayout(val title: String) { CLASSIC("Classic"), MINIMAL("Minimal"), WIDGETS("Widgets") }

/**
 * Draws the wallpaper with the production [WallpaperRenderer]. Only the draw phase reads
 * the config, so slider drags redraw without recomposition or bitmap allocation.
 */
@Composable
fun WallpaperCanvas(
    config: WallpaperConfig,
    progress: CountdownProgress,
    image: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { WallpaperRenderer() }
    DisposableEffect(renderer) { onDispose { renderer.release() } }
    Canvas(modifier.semantics { contentDescription = "Wallpaper preview" }) {
        drawIntoCanvas { c ->
            renderer.render(c.nativeCanvas, size.width.roundToInt(), size.height.roundToInt(), config, progress, image)
        }
    }
}

@Composable
fun WallpaperPreview(
    config: WallpaperConfig,
    progress: CountdownProgress,
    image: Bitmap?,
    aspect: Float,
    mode: PreviewMode,
    modifier: Modifier = Modifier,
    homeLayout: HomeLayout = HomeLayout.CLASSIC,
    homeZoom: Float = 1f,
) {
    when (mode) {
        PreviewMode.WALLPAPER -> Box(
            modifier
                .aspectRatio(aspect, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        ) {
            WallpaperCanvas(config, progress, image, Modifier.fillMaxSize())
        }
        PreviewMode.HOME -> PhoneFrame(aspect, modifier) {
            WallpaperCanvas(
                config,
                progress,
                image,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = homeZoom; scaleY = homeZoom },
            )
            HomeScreenOverlay(homeLayout)
        }
        PreviewMode.LOCK -> PhoneFrame(aspect, modifier) {
            WallpaperCanvas(config, progress, image, Modifier.fillMaxSize())
            LockScreenOverlay()
        }
    }
}

/** A generic modern phone: thin bezel, large corner radius, centred punch-hole camera. */
@Composable
fun PhoneFrame(aspect: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.aspectRatio(aspect, matchHeightConstraintsFirst = true)) {
        val w = maxWidth
        val bezel = w * 0.028f
        val outer = RoundedCornerShape(w * 0.13f)
        val inner = RoundedCornerShape(w * 0.105f)
        Box(
            Modifier
                .fillMaxSize()
                .clip(outer)
                .background(Color(0xFF050505))
                .border(1.dp, Color(0xFF3A3A3A), outer)
                .padding(bezel),
        ) {
            Box(Modifier.fillMaxSize().clip(inner)) {
                content()
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = w * 0.03f)
                        .size(w * 0.035f)
                        .clip(CircleShape)
                        .background(Color.Black),
                )
            }
        }
    }
}

@Composable
private fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    return now
}

@Composable
private fun Dp.sp(): TextUnit = with(LocalDensity.current) { this@sp.toSp() }

@Composable
private fun StatusBar(w: Dp, showTime: Boolean = true) {
    val now = rememberNow()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = w * 0.075f, vertical = w * 0.035f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (showTime) now.format(DateTimeFormatter.ofPattern("H:mm")) else "",
            color = Color.White,
            fontSize = (w * 0.036f).sp(),
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        // Signal bars
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(w * 0.006f)) {
            listOf(0.3f, 0.5f, 0.7f, 0.9f).forEach { f ->
                Box(Modifier.width(w * 0.008f).height(w * 0.03f * f).background(Color.White, RoundedCornerShape(1.dp)))
            }
        }
        Spacer(Modifier.width(w * 0.02f))
        // Battery
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(w * 0.05f)
                    .height(w * 0.026f)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(w * 0.008f))
                    .padding(w * 0.004f),
            ) {
                Box(Modifier.fillMaxSize().padding(end = w * 0.012f).background(Color.White, RoundedCornerShape(w * 0.004f)))
            }
            Box(Modifier.width(w * 0.004f).height(w * 0.01f).background(Color.White.copy(alpha = 0.8f)))
        }
    }
}

/** Simulated app icon: soft-tinted circle, original placeholder art (no real app branding). */
@Composable
private fun AppIcon(w: Dp, tint: Color, label: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(w * 0.14f)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(w * 0.05f).clip(RoundedCornerShape(w * 0.012f)).background(Color.White.copy(alpha = 0.85f)))
        }
        if (label) {
            Spacer(Modifier.height(w * 0.015f))
            Box(Modifier.width(w * 0.09f).height(w * 0.012f).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)))
        }
    }
}

private val ICON_TINTS = listOf(
    Color(0xFF3D5AFE), Color(0xFF00A86B), Color(0xFFFF7043), Color(0xFF8E24AA),
    Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFF5C6BC0), Color(0xFFFFB300),
)

@Composable
fun HomeScreenOverlay(layout: HomeLayout) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        Column(Modifier.fillMaxSize()) {
            StatusBar(w)
            if (layout == HomeLayout.WIDGETS) {
                val now = rememberNow()
                Column(Modifier.padding(horizontal = w * 0.08f, vertical = w * 0.04f)) {
                    Text(now.format(DateTimeFormatter.ofPattern("EEE d MMM")), color = Color.White, fontSize = (w * 0.06f).sp(), fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(w * 0.015f))
                    Box(Modifier.width(w * 0.3f).height(w * 0.06f).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
                }
            }
            Spacer(Modifier.weight(1f))
            if (layout != HomeLayout.MINIMAL) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = w * 0.06f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) { (0 until 4).forEach { AppIcon(w, ICON_TINTS[it + 4].copy(alpha = 0.9f), label = true) } }
                Spacer(Modifier.height(w * 0.06f))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = w * 0.06f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) { (0 until 4).forEach { AppIcon(w, ICON_TINTS[it].copy(alpha = 0.9f), label = false) } }
            Spacer(Modifier.height(w * 0.05f))
            if (layout == HomeLayout.CLASSIC) {
                Box(
                    Modifier
                        .padding(horizontal = w * 0.06f)
                        .fillMaxWidth()
                        .height(w * 0.12f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = w * 0.05f)
                            .size(w * 0.045f)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f)),
                    )
                }
                Spacer(Modifier.height(w * 0.04f))
            }
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = w * 0.03f)
                    .width(w * 0.3f)
                    .height(w * 0.012f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
fun LockScreenOverlay() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val now = rememberNow()
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            StatusBar(w, showTime = false)
            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(w * 0.05f))
            Spacer(Modifier.height(w * 0.04f))
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                color = Color.White,
                fontSize = (w * 0.22f).sp(),
                fontWeight = FontWeight.Light,
                lineHeight = (w * 0.22f).sp(),
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = (w * 0.04f).sp(),
            )
            Spacer(Modifier.weight(1f))
            repeat(2) { i ->
                Row(
                    Modifier
                        .padding(horizontal = w * 0.05f, vertical = w * 0.012f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(w * 0.05f))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(w * 0.035f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(w * 0.08f).clip(CircleShape).background(ICON_TINTS[i * 3]))
                    Spacer(Modifier.width(w * 0.03f))
                    Column(verticalArrangement = Arrangement.spacedBy(w * 0.015f)) {
                        Box(Modifier.width(w * 0.3f).height(w * 0.018f).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                        Box(Modifier.width(w * (0.5f - i * 0.1f)).height(w * 0.015f).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }
                }
            }
            Spacer(Modifier.height(w * 0.08f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = w * 0.1f, vertical = w * 0.06f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(2) {
                    Box(Modifier.size(w * 0.12f).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(w * 0.04f).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
            Box(
                Modifier
                    .offset(y = (-4).dp)
                    .padding(bottom = w * 0.03f)
                    .width(w * 0.3f)
                    .height(w * 0.012f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
            )
        }
    }
}
