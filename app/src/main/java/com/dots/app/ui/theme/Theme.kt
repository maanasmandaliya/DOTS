package com.dots.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dots.app.data.ThemePreference

/** Signature accent — a single warm red, used sparingly (today's dot, primary emphasis). */
val DotsRed = Color(0xFFFF3B30)

private val Dark = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF000000),
    secondary = DotsRed,
    onSecondary = Color.White,
    tertiary = DotsRed,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF9A9A9A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF0A0A0A),
    outline = Color(0xFF2C2C2C),
    outlineVariant = Color(0xFF222222),
    error = Color(0xFFFF6B60),
)

private val Light = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color.White,
    secondary = DotsRed,
    onSecondary = Color.White,
    tertiary = DotsRed,
    background = Color(0xFFF3F3F0),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFEAEAE6),
    onSurfaceVariant = Color(0xFF6B6B6B),
    surfaceContainer = Color(0xFFFAFAF8),
    surfaceContainerHigh = Color(0xFFF0F0EC),
    surfaceContainerHighest = Color(0xFFE6E6E2),
    surfaceContainerLow = Color(0xFFFFFFFF),
    outline = Color(0xFFD4D4D0),
    outlineVariant = Color(0xFFE2E2DE),
)

private val base = Typography()

private val DotsTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium),
)

/** Small uppercase monospaced captions — the "technical" voice of the UI. */
val CaptionStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.sp,
    letterSpacing = 1.5.sp,
    fontWeight = FontWeight.Medium,
)

private val DotsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun DotsTheme(preference: ThemePreference = ThemePreference.DARK, content: @Composable () -> Unit) {
    val dark = when (preference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = DotsTypography,
        shapes = DotsShapes,
        content = content,
    )
}
