package com.dots.app.render

import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.GridAlignment
import com.dots.app.domain.model.LayoutSettings
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pixel geometry of the dot grid for a specific canvas size. Pure math, no Android types,
 * so it is shared by the preview and the generator and covered by unit tests.
 */
data class GridGeometry(
    val columns: Int,
    val rows: Int,
    val total: Int,
    val dotSize: Float,
    val stepX: Float,
    val stepY: Float,
    val originX: Float,
    val originY: Float,
    val gridWidth: Float,
    val gridHeight: Float,
    val lastRowOffset: Float,
    val labelSize: Float,
    val labelBaseline: Float,
    /** < 1 when the grid had to shrink to fit inside the padding. */
    val scale: Float,
) {
    fun centerX(index: Int): Float {
        val row = index / columns
        val col = index % columns
        val offset = if (row == rows - 1) lastRowOffset else 0f
        return originX + offset + col * stepX + dotSize / 2f
    }

    fun centerY(index: Int): Float = originY + (index / columns) * stepY + dotSize / 2f
}

object DotGridLayout {

    /** 1 unit = 1/1000 of the canvas width. */
    fun unit(width: Float) = width / 1000f

    fun defaultColumns(mode: CountdownMode, total: Int): Int = when (mode) {
        CountdownMode.YEAR -> 20
        CountdownMode.MONTH, CountdownMode.WEEK -> 7
        CountdownMode.LIFE -> 52
        CountdownMode.CUSTOM -> when {
            total <= 7 -> total.coerceAtLeast(1)
            total <= 35 -> 7
            total <= 120 -> 12
            total <= 400 -> 20
            else -> ceil(sqrt(total * 0.6)).toInt()
        }
    }

    fun compute(
        width: Float,
        height: Float,
        total: Int,
        mode: CountdownMode,
        layout: LayoutSettings,
        dotSizeUnits: Float,
        labelSizeUnits: Float,
        showLabel: Boolean,
    ): GridGeometry {
        val u = unit(width)
        val safeTotal = max(total, 1)
        val requested = if (layout.columns > 0) layout.columns else defaultColumns(mode, safeTotal)
        val cols = min(requested, safeTotal).coerceAtLeast(1)
        val rows = ceil(safeTotal / cols.toDouble()).toInt()

        val dot = dotSizeUnits * u
        val hs = layout.horizontalSpacing * u
        val vs = layout.verticalSpacing * u
        val gridW = cols * dot + (cols - 1) * hs
        val gridH = rows * dot + (rows - 1) * vs
        val label = if (showLabel) labelSizeUnits * u else 0f
        val labelGap = if (showLabel) label * 1.4f else 0f

        val padX = layout.paddingHorizontal * u
        val padY = layout.paddingVertical * u
        val availW = max(width - 2 * padX, 1f)
        val availH = max(height - 2 * padY, 1f)
        // Shrink uniformly (never stretch) when the requested grid does not fit.
        val scale = min(1f, min(availW / gridW, availH / (gridH + labelGap + label)))

        val sDot = dot * scale
        val sHs = hs * scale
        val sVs = vs * scale
        val sGridW = gridW * scale
        val sGridH = gridH * scale
        val sLabel = label * scale
        val sGap = labelGap * scale
        val blockH = sGridH + sGap + sLabel

        val originX = padX + (availW - sGridW) * layout.horizontalPosition.coerceIn(0f, 1f)
        val originY = padY + (availH - blockH) * layout.verticalPosition.coerceIn(0f, 1f)

        val lastRowCount = safeTotal - (rows - 1) * cols
        val missing = (cols - lastRowCount) * (sDot + sHs)
        val lastRowOffset = when (layout.alignment) {
            GridAlignment.START -> 0f
            GridAlignment.CENTER -> missing / 2f
            GridAlignment.END -> missing
        }

        return GridGeometry(
            columns = cols,
            rows = rows,
            total = total,
            dotSize = sDot,
            stepX = sDot + sHs,
            stepY = sDot + sVs,
            originX = originX,
            originY = originY,
            gridWidth = sGridW,
            gridHeight = sGridH,
            lastRowOffset = lastRowOffset,
            labelSize = sLabel,
            labelBaseline = originY + sGridH + sGap + sLabel * 0.8f,
            scale = scale,
        )
    }
}
