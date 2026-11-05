package com.dots.app

import com.dots.app.domain.color.ColorMath
import com.dots.app.domain.color.PaletteSuggester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test fun `parses valid hex forms`() {
        assertEquals(0xFFFFFFFF.toInt(), ColorMath.parseHex("#FFFFFF"))
        assertEquals(0xFF000000.toInt(), ColorMath.parseHex("#000000"))
        assertEquals(0xFFFF5733.toInt(), ColorMath.parseHex("#FF5733"))
        assertEquals(0xFF00D9FF.toInt(), ColorMath.parseHex("00d9ff"))
        assertEquals(0xFFA855F7.toInt(), ColorMath.parseHex(" #a855f7 "))
        assertEquals(0xFFFFAA00.toInt(), ColorMath.parseHex("#FA0"))
        assertEquals(0x80FF0000.toInt(), ColorMath.parseHex("#80FF0000"))
    }

    @Test fun `rejects invalid hex`() {
        listOf("", "#", "#12", "#12345", "#GGGGGG", "#1234567", "red", "#FF5733FF00").forEach {
            assertNull(it, ColorMath.parseHex(it))
            assertFalse(ColorMath.isValidHex(it))
        }
    }

    @Test fun `hex round trip`() {
        assertEquals("#FF5733", ColorMath.toHex(0xFFFF5733.toInt()))
        assertEquals("#00D9FF", ColorMath.toHex(ColorMath.parseHex("#00d9ff")!!))
    }

    @Test fun `hsv round trip`() {
        listOf(0xFFFF5733, 0xFF00D9FF, 0xFFA855F7, 0xFF808080, 0xFF000000).forEach { c ->
            val hsv = ColorMath.toHsv(c.toInt())
            assertEquals(c.toInt(), ColorMath.fromHsv(hsv[0], hsv[1], hsv[2]))
        }
    }

    @Test fun `contrast ratio extremes`() {
        assertEquals(21.0, ColorMath.contrast(0xFFFFFFFF.toInt(), 0xFF000000.toInt()), 0.01)
        assertEquals(1.0, ColorMath.contrast(0xFF336699.toInt(), 0xFF336699.toInt()), 0.001)
    }

    @Test fun `ensureContrast reaches target`() {
        val bg = 0xFF101820.toInt()
        val c = ColorMath.ensureContrast(0xFF202830.toInt(), bg, 3.0)
        assertTrue(ColorMath.contrast(c, bg) >= 3.0)
    }

    @Test fun `suggestions are legible on the image`() {
        val dominant = 0xFF1B2A3A.toInt()
        val palettes = PaletteSuggester.suggest(dominant, listOf(0xFFE07A5F.toInt(), 0xFF3D405B.toInt()))
        assertTrue(palettes.size >= 4)
        palettes.forEach { p ->
            assertTrue("${p.name} filled contrast", ColorMath.contrast(p.filled, dominant) >= 2.5)
        }
    }
}
