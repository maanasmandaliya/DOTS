package com.dots.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Text drawn as a 5×7 dot matrix — the DOTS brand voice. Supports digits, a few letters
 * and punctuation used across the app.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dot: Dp = 4.dp,
    gap: Dp = 2.dp,
    color: Color = Color.White,
    dimColor: Color = color.copy(alpha = 0.08f),
) {
    val glyphs = text.uppercase().map { GLYPHS[it] ?: GLYPHS[' ']!! }
    val cols = glyphs.sumOf { it[0].length } + (glyphs.size - 1).coerceAtLeast(0)
    val step = dot + gap
    val width = step * cols - gap
    val height = step * 7 - gap
    val density = LocalDensity.current
    Canvas(
        modifier
            .size(width, height)
            .semantics { contentDescription = text },
    ) {
        val s = with(density) { step.toPx() }
        val r = with(density) { dot.toPx() } / 2f
        var x0 = 0
        glyphs.forEach { g ->
            val w = g[0].length
            for (row in 0 until 7) for (col in 0 until w) {
                val on = g[row][col] == '#'
                drawCircle(if (on) color else dimColor, r, Offset((x0 + col) * s + r, row * s + r))
            }
            x0 += w + 1
        }
    }
}

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
    '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
    '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
    '3' to listOf("####.", "....#", "....#", ".###.", "....#", "....#", "####."),
    '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
    '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
    '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
    '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
    '%' to listOf("##...", "##..#", "...#.", "..#..", ".#...", "#..##", "...##"),
    '.' to listOf(".", ".", ".", ".", ".", ".", "#"),
    ' ' to listOf("..", "..", "..", "..", "..", "..", ".."),
    'D' to listOf("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
    'O' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    'T' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
    'S' to listOf(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
    'W' to listOf("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"),
    'K' to listOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
)
