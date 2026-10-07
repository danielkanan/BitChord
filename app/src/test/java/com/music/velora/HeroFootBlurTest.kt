package com.music.velora

import com.music.velora.ui.components.FOOT_FADE_END
import com.music.velora.ui.components.FOOT_FADE_START
import com.music.velora.ui.components.footFadeCoverage
import com.music.velora.ui.components.FOOT_SOLID_BAND
import com.music.velora.ui.components.FOOT_START
import com.music.velora.ui.components.FootPixels
import com.music.velora.ui.components.footMean
import com.music.velora.ui.components.pageColour
import com.music.velora.ui.components.progressiveFoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The foot of a detail page's hero: a blur that grows down the photograph,
 * fully on above the fade, then transparent over the long run under it.
 */
class HeroFootBlurTest {
    private val width = 200
    private val photoHeight = 150
    private val startRow = (photoHeight * FOOT_START).roundToInt()

    @Test
    fun `rows above the start are byte-identical to the source`() {
        val photo = noise(seed = 7)
        val foot = progressiveFoot(photo, width, photoHeight)
        for (k in 0 until startRow * width) {
            assertEquals("pixel ${k % width}, ${k / width}", photo[k], foot.pixels[k])
        }
    }

    @Test
    fun `the final rows are clear`() {
        val foot = progressiveFoot(noise(seed = 11), width, photoHeight)
        val fadeEnd = (photoHeight * FOOT_FADE_END).roundToInt().coerceAtMost(foot.height)
        for (y in fadeEnd until foot.height) {
            for (x in 0 until width) {
                assertEquals("pixel $x, $y", 0, foot.pixels[y * width + x] ushr 24)
            }
        }
    }

    @Test
    fun `the fade is still there halfway down its box`() {
        val foot = progressiveFoot(noise(seed = 17), width, photoHeight)
        val fadeStart = (photoHeight * FOOT_FADE_START).roundToInt()
        val fadeEnd = (photoHeight * FOOT_FADE_END).roundToInt().coerceAtMost(foot.height)
        val mid = (fadeStart + fadeEnd) / 2
        val alpha = foot.pixels[mid * width + width / 2] ushr 24
        val expected = (footFadeCoverage(0.5f) * 255f).roundToInt()
        assertTrue("halfway down: $alpha", alpha > expected - 8)
    }

    @Test
    fun `the blur stays opaque above the fade`() {
        val foot = progressiveFoot(noise(seed = 13), width, photoHeight)
        val fadeStart = (photoHeight * FOOT_FADE_START).roundToInt()
        for (k in 0 until fadeStart * width) {
            assertEquals("pixel ${k % width}, ${k / width}", 0xFF, foot.pixels[k] ushr 24)
        }
    }

    @Test
    fun `a flat bright band deepens into the page colour`() {
        // Pale grass, about the colour this page used to paint. Deepened, it
        // is the dark olive Apple paints for the same photograph, near
        // (54, 63, 59) — dark enough for white text, and still green.
        val band = opaque(0x797E5B)
        val bandStart = photoHeight - (photoHeight * FOOT_SOLID_BAND).roundToInt()
        val photo = noise(seed = 3)
        photo.fill(band, bandStart * width, photoHeight * width)
        val solid = progressiveFoot(photo, width, photoHeight).solid
        assertEquals(pageColour(band), solid)
        val red = (solid ushr 16) and 0xFF
        val green = (solid ushr 8) and 0xFF
        val blue = solid and 0xFF
        assertTrue("deepened to $red,$green,$blue", red < 110 && green < 120 && blue < 100)
        assertTrue(red > 40 && green > 40)
        assertTrue(green + 4 >= red && green + 4 >= blue)
    }

    @Test
    fun `the photograph is blurred, not painted over with the page colour`() {
        val photo = IntArray(width * photoHeight) { opaque(0xFFFFFF) }
        val foot = progressiveFoot(photo, width, photoHeight, solid = opaque(0x101810))
        fun green(y: Int) = (foot.pixels[y * width + width / 2] ushr 8) and 0xFF
        // Just under the start the picture is still the picture. The page
        // colour only arrives once the blur has run out below it.
        assertTrue("just under the start: ${green(startRow + 1)}", green(startRow + 1) > 240)
        assertEquals(0, foot.pixels[(foot.height - 1) * width + width / 2] ushr 24)
    }

    @Test
    fun `a colour the photograph came with is kept exactly`() {
        val apple = opaque(0x363F3B)
        val foot = progressiveFoot(noise(seed = 5), width, photoHeight, solid = apple)
        assertEquals(apple, foot.solid)
        val above = (photoHeight * FOOT_FADE_START).roundToInt() - 1
        for (x in 0 until width) {
            assertEquals(0xFF, foot.pixels[above * width + x] ushr 24)
        }
    }

    @Test
    fun `the solid is averaged in linear light before it is deepened`() {
        // Half black and half white is half the light, which sRGB writes as
        // 188 — not the 128 a plain average of the bytes would give.
        assertEquals(opaque(0xBCBCBC), footMean(stripes(), width, photoHeight))
    }

    @Test
    fun `the blur never weakens down the foot`() {
        val foot = progressiveFoot(stripes(), width, photoHeight)
        val contrast = IntArray(foot.height) { y -> rowContrast(foot, y) }
        // Gathering out of nothing: no line where it visibly starts.
        assertTrue("sharp just under the start: ${contrast[startRow + 1]}", contrast[startRow + 1] >= 250)
        for (y in startRow until foot.height - 1) {
            assertTrue(
                "row ${y + 1} is sharper than row $y: ${contrast[y + 1]} > ${contrast[y]}",
                contrast[y + 1] <= contrast[y] + DITHER_SLACK,
            )
        }
        // A real blur by the photograph's edge, not a nudge.
        assertTrue("still sharp at the edge: ${contrast[photoHeight - 1]}", contrast[photoHeight - 1] < 255 / 4)
        assertEquals(0, contrast[foot.height - 1])
    }

    @Test
    fun `pixels above the fade stay opaque and the bottom of it is clear`() {
        val foot = progressiveFoot(stripes(), width, photoHeight)
        val fadeStart = (photoHeight * FOOT_FADE_START).roundToInt()
        assertTrue(foot.pixels.take(fadeStart * width).all { it ushr 24 == 0xFF })
        assertTrue(foot.pixels.drop((foot.height - 1) * width).all { it ushr 24 == 0 })
    }

    private fun noise(seed: Int): IntArray {
        val random = Random(seed)
        return IntArray(width * photoHeight) { opaque(random.nextInt(0x1000000)) }
    }

    /** Black and white columns four pixels wide, the same all the way down. */
    private fun stripes() = IntArray(width * photoHeight) { k ->
        if ((k % width) / 4 % 2 == 0) opaque(0x000000) else opaque(0xFFFFFF)
    }

    /** How far apart the darkest and brightest pixels in row [y] are, in eight-bit green. */
    private fun rowContrast(foot: FootPixels, y: Int): Int {
        var min = 255
        var max = 0
        for (x in 0 until foot.width) {
            val green = (foot.pixels[y * foot.width + x] ushr 8) and 0xFF
            min = minOf(min, green)
            max = maxOf(max, green)
        }
        return max - min
    }

    private fun opaque(rgb: Int) = (0xFF shl 24) or rgb

    private companion object {
        /** The output dither moves a pixel at most one step; two rows, two steps. */
        const val DITHER_SLACK = 2
    }
}
