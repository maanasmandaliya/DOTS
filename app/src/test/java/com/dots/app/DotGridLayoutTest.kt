package com.dots.app

import com.dots.app.domain.model.CountdownMode
import com.dots.app.domain.model.GridAlignment
import com.dots.app.domain.model.LayoutSettings
import com.dots.app.render.DotGridLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DotGridLayoutTest {

    private fun grid(w: Float, h: Float, total: Int, layout: LayoutSettings = LayoutSettings(), size: Float = 26f, mode: CountdownMode = CountdownMode.YEAR) =
        DotGridLayout.compute(w, h, total, mode, layout, size, 30f, true)

    @Test fun `rows and columns`() {
        val g = grid(1080f, 2400f, 365)
        assertEquals(20, g.columns)
        assertEquals(19, g.rows)
    }

    @Test fun `layout is resolution independent`() {
        val small = grid(270f, 600f, 365)
        val large = grid(1080f, 2400f, 365)
        assertEquals(small.dotSize * 4, large.dotSize, 0.01f)
        assertEquals(small.centerX(200) * 4, large.centerX(200), 0.05f)
        assertEquals(small.centerY(200) * 4, large.centerY(200), 0.05f)
    }

    @Test fun `grid stays inside padding`() {
        val layout = LayoutSettings(paddingHorizontal = 100f, paddingVertical = 200f)
        val g = grid(1000f, 2000f, 4174, layout, size = 40f, mode = CountdownMode.LIFE)
        assertTrue(g.scale < 1f)
        val lastX = g.centerX(51) + g.dotSize / 2
        val lastY = g.centerY(4173) + g.dotSize / 2
        assertTrue(g.originX >= 100f - 0.01f)
        assertTrue(lastX <= 900f + 0.01f)
        assertTrue(lastY <= 1800f + 0.01f)
    }

    @Test fun `last row alignment`() {
        val start = grid(1000f, 2000f, 30, LayoutSettings(columns = 7, alignment = GridAlignment.START))
        val end = grid(1000f, 2000f, 30, LayoutSettings(columns = 7, alignment = GridAlignment.END))
        // 30 dots / 7 columns → 2 in the last row; right aligned moves them by 5 steps.
        assertEquals(start.centerX(28) + 5 * start.stepX, end.centerX(28), 0.01f)
        assertEquals(start.centerX(0), end.centerX(0), 0.01f)
    }

    @Test fun `vertical position moves grid`() {
        val top = grid(1000f, 2000f, 31, LayoutSettings(verticalPosition = 0f))
        val bottom = grid(1000f, 2000f, 31, LayoutSettings(verticalPosition = 1f))
        assertTrue(top.originY < bottom.originY)
    }
}
