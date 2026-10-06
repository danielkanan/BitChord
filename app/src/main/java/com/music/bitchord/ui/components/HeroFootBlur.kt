package com.music.bitchord.ui.components

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Share of the photograph's height, from its top, that the blur leaves untouched. */
internal const val FOOT_START = 0.64f

/**
 * Where the blur has reached full strength, as a share of the photograph's
 * height. The far edge of the blur box. Held from here on.
 */
internal const val FOOT_BLUR_FULL = 1.15f

/**
 * Where the picture starts fading out. The blur is already on above this, so
 * the fade sits under it.
 */
internal const val FOOT_FADE_START = 0.80f

/**
 * Where that fade finishes, as a share of the photograph's height.
 */
internal const val FOOT_FADE_END = 1.35f

/** How far the foot runs on past the photograph, as a share of its height. Long enough to hold the fade and the blur past it. */
internal const val FOOT_RUN = 1.05f

/** The blur at its widest: a Gaussian sigma, as a share of the photograph's width. */
internal const val FOOT_SIGMA = 0.09f

/** Bottom share of the photograph whose mean colour the foot settles into. */
internal const val FOOT_SOLID_BAND = 0.12f

/**
 * How far along the blur [t] is — 0 on the sharp edge, 1 where the blur is
 * fully on. Straight, so the blur is exactly as long as the box between
 * those two edges. The video uses the same line.
 */
internal fun footEase(t: Float): Float = t.coerceIn(0f, 1f)

/**
 * How much of the picture is still showing [t] of the way down the fade.
 * 1 on the colour edge, 0 on the clear edge. The slope is flat at both
 * edges, so it leaves the solid picture without a line and is still clear
 * only at the bottom of the box.
 */
internal fun footFadeCoverage(t: Float): Float {
    val s = t.coerceIn(0f, 1f)
    val faded = s * s * (3f - 2f * s)
    return 1f - faded
}

/**
 * [progressiveFoot]'s result: [width] x [height] ARGB pixels — the photograph,
 * then its blurred run. Opaque through the blur, then fading out below it,
 * the same way the video's foot does. [solid] is the page colour behind that fade.
 */
internal class FootPixels(val pixels: IntArray, val width: Int, val height: Int, val solid: Int)

/** Rows under the photograph. Enough to hold the fade through [FOOT_FADE_END]. */
internal fun footRunRows(width: Int, photoHeight: Int): Int {
    val fadeRows = (photoHeight * (FOOT_FADE_END - 1f)).roundToInt() + 1
    return maxOf((photoHeight * FOOT_RUN).roundToInt(), fadeRows, footReach(width * FOOT_SIGMA) + 2)
}

/**
 * A real progressive blur of the photograph's foot, onto its own bottom colour.
 *
 * [photo] is `width * photoHeight` ARGB, already cropped to the hero box. Rows
 * of the page colour are appended under it: [solid] when the photograph came
 * with one, and otherwise [pageColour] of the linear-light mean of its bottom
 * [bandRows]. From [startRow] down every row is blurred at its own radius,
 * growing to [maxSigma] at [FOOT_BLUR_FULL] and held there. Below
 * [FOOT_FADE_START] the same pixels fade out, over the run to [FOOT_FADE_END],
 * so the blur stays a band above the fade and the fade has room to happen.
 * The page behind the hero is [solid], which is what shows through once the
 * fade has finished.
 *
 * Three fractional box passes each way stand in for the Gaussian, every pass a
 * prefix-sum lookup, so the cost per pixel does not grow with the radius.
 * Worked in linear light, as light actually spreads, and dithered on the way
 * back to eight bits so the long, nearly flat ramp into the solid cannot band.
 */
internal fun progressiveFoot(
    photo: IntArray,
    width: Int,
    photoHeight: Int,
    runRows: Int = footRunRows(width, photoHeight),
    startRow: Int = (photoHeight * FOOT_START).roundToInt(),
    maxSigma: Float = width * FOOT_SIGMA,
    bandRows: Int = (photoHeight * FOOT_SOLID_BAND).roundToInt(),
    solid: Int? = null,
): FootPixels {
    require(width > 0 && photoHeight > 0 && runRows >= 0 && photo.size >= width * photoHeight)
    val height = photoHeight + runRows
    val out = photo.copyOf(width * height)
    // Apple publishes a colour for the photograph that is a step brighter than
    // the page their app actually paints. Deepen that, and a colour read off
    // the picture, into the same band — not so far that a bright sleeve goes
    // black.
    val page = pageColour(
        solid?.let { (0xFF shl 24) or (it and 0xFFFFFF) }
            ?: meanColour(photo, width, photoHeight, bandRows.coerceIn(1, photoHeight)),
    )
    // The run under the photograph stays the picture's own bottom colour.
    // Painting the page colour there made the fade invisible: by the top of
    // it the pixels were already that colour, so only a sliver looked like
    // it was going clear. The page behind the hero is what shows through.
    val bottom = (photoHeight - 1) * width
    for (y in photoHeight until height) {
        out.copyInto(out, y * width, bottom, bottom + width)
    }

    val radiusMax = boxRadius(maxSigma)
    if (radiusMax <= 0f || startRow >= height) {
        fadeFoot(out, width, height, photoHeight)
        return FootPixels(out, width, height, page)
    }

    val reach = footReach(maxSigma)
    // Rows above the start never change, but the first blurred rows reach up
    // into them, so the working strip begins that far above it.
    val top = (startRow - reach).coerceAtLeast(0)
    val rows = height - top
    // Nothing on the sharp edge, full strength on the far edge of the blur
    // box, then held from there on.
    val fullRow = (photoHeight * FOOT_BLUR_FULL).roundToInt().coerceIn(startRow + 1, height - 1)
    val ramp = (fullRow - startRow).coerceAtLeast(1).toFloat()
    val radius = FloatArray(rows) { i ->
        val y = top + i
        if (y <= startRow) 0f else boxRadius(maxSigma * footEase(((y - startRow) / ramp).coerceAtMost(1f)))
    }

    val pad = radiusMax.toInt() + 2
    val plane = FloatArray(width * rows)
    val rowPadded = FloatArray(width + 2 * pad)
    val rowSums = DoubleArray(width + 2 * pad + 1)
    val columnA = FloatArray(rows)
    val columnB = FloatArray(rows)
    val columnSums = DoubleArray(rows + 1)

    for (shift in CHANNEL_SHIFTS) {
        for (i in 0 until rows) {
            val from = (top + i) * width
            val to = i * width
            for (x in 0 until width) plane[to + x] = TO_LINEAR[(out[from + x] ushr shift) and 0xFF]
        }

        // Across. The run is the photograph's bottom edge carried on, so it
        // blurs the same way the picture does.
        for (i in 0 until rows) {
            val r = radius[i]
            if (r > 0f) repeat(PASSES) { blurRow(plane, i * width, width, r, rowPadded, rowSums) }
        }

        // Down, each row gathering at its own radius.
        for (x in 0 until width) {
            for (i in 0 until rows) columnA[i] = plane[i * width + x]
            var src = columnA
            var dst = columnB
            repeat(PASSES) {
                blurColumn(src, dst, rows, radius, columnSums)
                val swap = src
                src = dst
                dst = swap
            }
            for (i in 0 until rows) plane[i * width + x] = src[i]
        }

        val keep = (0xFF shl shift).inv()
        for (i in 0 until rows) {
            val y = top + i
            if (radius[i] <= 0f) continue
            val row = y * width
            val source = i * width
            for (x in 0 until width) {
                val value = (toSrgb255(plane[source + x]) + dither(x, y) + 0.5f).toInt().coerceIn(0, 255)
                out[row + x] = (out[row + x] and keep) or (value shl shift)
            }
        }
    }
    fadeFoot(out, width, height, photoHeight)
    return FootPixels(out, width, height, page)
}

/**
 * The transparency under the blur. Opaque through [FOOT_FADE_START], clear
 * by [FOOT_FADE_END], and clear after that. The same line the video's foot
 * fades on.
 */
private fun fadeFoot(pixels: IntArray, width: Int, height: Int, photoHeight: Int) {
    val fadeStart = (photoHeight * FOOT_FADE_START).roundToInt().coerceIn(0, height - 1)
    val fadeEnd = (photoHeight * FOOT_FADE_END).roundToInt().coerceIn(fadeStart + 1, height)
    val span = (fadeEnd - fadeStart).toFloat()
    for (y in fadeStart until height) {
        val t = if (y >= fadeEnd) 1f else (y - fadeStart) / span
        val alpha = (footFadeCoverage(t) * 255f).toInt().coerceIn(0, 255)
        if (alpha == 255) continue
        val row = y * width
        for (x in 0 until width) pixels[row + x] = scaleAlpha(pixels[row + x], alpha)
    }
}

/** [pixel] at [alpha], premultiplied the way a bitmap stores it. */
private fun scaleAlpha(pixel: Int, alpha: Int): Int {
    if (alpha <= 0) return 0
    val red = ((pixel ushr 16) and 0xFF) * alpha / 255
    val green = ((pixel ushr 8) and 0xFF) * alpha / 255
    val blue = (pixel and 0xFF) * alpha / 255
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

/**
 * The linear-light mean of the bottom [FOOT_SOLID_BAND] of [photo], the colour
 * [pageColour] deepens into the page.
 */
internal fun footMean(
    photo: IntArray,
    width: Int,
    photoHeight: Int,
    bandRows: Int = (photoHeight * FOOT_SOLID_BAND).roundToInt(),
): Int = meanColour(photo, width, photoHeight, bandRows.coerceIn(1, photoHeight))

/**
 * [mean], brought into the band Apple actually paints behind a hero. A colour
 * already in that band is returned unchanged. One above it — the bright teal
 * a sky publishes, which their app then paints a step darker — comes down to
 * the top of the band, and only a little of its saturation goes with it, so
 * it stays that colour rather than turning grey or black.
 */
internal fun pageColour(mean: Int): Int {
    val red = ((mean ushr 16) and 0xFF) / 255f
    val green = ((mean ushr 8) and 0xFF) / 255f
    val blue = (mean and 0xFF) / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val light = (max + min) / 2f
    val target = light.coerceIn(PAGE_LIGHT_MIN, PAGE_LIGHT_MAX)
    if (target == light) return (0xFF shl 24) or (mean and 0xFFFFFF)
    val delta = max - min
    val saturation = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * light - 1f))
    val hue = if (delta == 0f) {
        0f
    } else {
        val sector = when (max) {
            red -> ((green - blue) / delta).mod(6f)
            green -> (blue - red) / delta + 2f
            else -> (red - green) / delta + 4f
        }
        (sector / 6f).mod(1f)
    }
    val muted = (target / light.coerceAtLeast(0.05f)).coerceIn(0.82f, 1f)
    return fromHsl(hue, saturation * muted, target)
}

/**
 * The band measured off Apple's own pages: deep enough that a bright hero is
 * not pasted on as-is, and no deeper than the teal they paint under a sky.
 */
private const val PAGE_LIGHT_MIN = 0.20f
private const val PAGE_LIGHT_MAX = 0.36f

private fun fromHsl(hue: Float, saturation: Float, light: Float): Int {
    val q = if (light < 0.5f) light * (1f + saturation) else light + saturation - light * saturation
    val p = 2f * light - q
    fun channel(offset: Float): Int {
        var t = hue + offset
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        val value = when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 0.5f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
        return (value * 255f + 0.5f).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(1f / 3f) shl 16) or (channel(0f) shl 8) or channel(-1f / 3f)
}

/** Three box passes per axis: close enough to a Gaussian that the eye cannot tell. */
private const val PASSES = 3

private val CHANNEL_SHIFTS = intArrayOf(16, 8, 0)

/**
 * The fractional box radius whose [PASSES] passes have the variance of a
 * Gaussian at [sigma]. One pass of radius r has variance r(r + 1) / 3.
 */
private fun boxRadius(sigma: Float): Float =
    if (sigma <= 0f) 0f else (sqrt(1f + 4f * sigma * sigma) - 1f) / 2f

/** How many rows away a pixel still feels the blur at [sigma]: each pass reaches one past its radius. */
private fun footReach(sigma: Float): Int = PASSES * (boxRadius(sigma).toInt() + 1)

/**
 * One fractional box pass along a row, in place. The ends are mirrored rather
 * than clamped, so the frame edge does not smear into streaks.
 */
private fun blurRow(
    plane: FloatArray,
    offset: Int,
    width: Int,
    radius: Float,
    padded: FloatArray,
    sums: DoubleArray,
) {
    val r = radius.toInt()
    val f = radius - r
    val pad = r + 1
    val span = width + 2 * pad
    for (k in 0 until span) padded[k] = plane[offset + mirror(k - pad, width)]
    sums[0] = 0.0
    for (k in 0 until span) sums[k + 1] = sums[k] + padded[k]
    val norm = 1.0 / (2.0 * radius + 1.0)
    for (x in 0 until width) {
        val c = x + pad
        val inner = sums[c + r + 1] - sums[c - r]
        val edge = padded[c + r + 1] + padded[c - r - 1]
        plane[offset + x] = ((inner + f * edge) * norm).toFloat()
    }
}

/**
 * One fractional box pass down a column, each output row at its own radius.
 * Rows past either end read the end row; the top is never reached in practice,
 * and the bottom is the solid.
 */
private fun blurColumn(src: FloatArray, dst: FloatArray, rows: Int, radius: FloatArray, sums: DoubleArray) {
    sums[0] = 0.0
    for (i in 0 until rows) sums[i + 1] = sums[i] + src[i]
    val first = src[0].toDouble()
    val last = src[rows - 1].toDouble()
    for (i in 0 until rows) {
        val rad = radius[i]
        if (rad <= 0f) {
            dst[i] = src[i]
            continue
        }
        val r = rad.toInt()
        val f = rad - r
        var lo = i - r
        var hi = i + r
        var inner = 0.0
        if (lo < 0) {
            inner += -lo * first
            lo = 0
        }
        if (hi > rows - 1) {
            inner += (hi - rows + 1) * last
            hi = rows - 1
        }
        if (lo <= hi) inner += sums[hi + 1] - sums[lo]
        val above = i - r - 1
        val below = i + r + 1
        val edge = (if (above < 0) first else src[above].toDouble()) +
            (if (below >= rows) last else src[below].toDouble())
        dst[i] = ((inner + f * edge) / (2.0 * rad + 1.0)).toFloat()
    }
}

private fun mirror(i: Int, n: Int): Int = when {
    i < 0 -> (-i - 1).coerceAtMost(n - 1)
    i >= n -> (2 * n - i - 1).coerceAtLeast(0)
    else -> i
}

/** The linear-light mean of the bottom [bandRows] of [photo], back in eight-bit sRGB. */
private fun meanColour(photo: IntArray, width: Int, photoHeight: Int, bandRows: Int): Int {
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    val from = (photoHeight - bandRows) * width
    val to = photoHeight * width
    for (k in from until to) {
        val pixel = photo[k]
        red += TO_LINEAR[(pixel ushr 16) and 0xFF]
        green += TO_LINEAR[(pixel ushr 8) and 0xFF]
        blue += TO_LINEAR[pixel and 0xFF]
    }
    val count = (to - from).toDouble()
    return (0xFF shl 24) or
        (srgbByte(red / count) shl 16) or
        (srgbByte(green / count) shl 8) or
        srgbByte(blue / count)
}

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun linearToSrgb(l: Float): Float =
    if (l <= 0.0031308f) l * 12.92f else (1.055 * l.toDouble().pow(1.0 / 2.4) - 0.055).toFloat()

private fun srgbByte(linear: Double): Int =
    (linearToSrgb(linear.toFloat().coerceIn(0f, 1f)) * 255f + 0.5f).toInt().coerceIn(0, 255)

private val TO_LINEAR = FloatArray(256) { srgbToLinear(it / 255f) }

private const val SRGB_STEPS = 4096

/** Linear light to sRGB, scaled to 0..255 and interpolated between steps. */
private val TO_SRGB = FloatArray(SRGB_STEPS + 1) { linearToSrgb(it / SRGB_STEPS.toFloat()) * 255f }

private fun toSrgb255(linear: Float): Float {
    val p = linear.coerceIn(0f, 1f) * SRGB_STEPS
    val i = p.toInt().coerceAtMost(SRGB_STEPS - 1)
    return TO_SRGB[i] + (TO_SRGB[i + 1] - TO_SRGB[i]) * (p - i)
}

/** Interleaved gradient noise in [-0.5, 0.5): half a step either way, never a pattern. */
private fun dither(x: Int, y: Int): Float {
    val v = 52.9829189f * fract(0.06711056f * x + 0.00583715f * y)
    return fract(v) - 0.5f
}

private fun fract(v: Float): Float = v - floor(v)
