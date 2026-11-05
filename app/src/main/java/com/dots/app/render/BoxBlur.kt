package com.dots.app.render

import android.graphics.Bitmap

/**
 * Fast separable box blur (3 passes ≈ Gaussian). Runs on a downscaled layer, so even large
 * radii cost only a few milliseconds, and it gives identical results on every API level
 * (unlike RenderEffect, which only exists on Android 12+ and can't be baked into a bitmap).
 */
internal object BoxBlur {

    fun blurInPlace(bitmap: Bitmap, radius: Int, passes: Int = 3) {
        if (radius < 1) return
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val tmp = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        repeat(passes) {
            pass(src, tmp, w, h, radius, horizontal = true)
            pass(tmp, src, w, h, radius, horizontal = false)
        }
        bitmap.setPixels(src, 0, w, 0, 0, w, h)
    }

    private fun pass(input: IntArray, output: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (line in 0 until lines) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            fun idx(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            for (i in -r..r) {
                val p = input[idx(i)]
                sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
            }
            for (i in 0 until len) {
                output[idx(i)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val pOut = input[idx(i - r)]
                val pIn = input[idx(i + r + 1)]
                sa += (pIn ushr 24) - (pOut ushr 24)
                sr += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                sg += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                sb += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }
}
