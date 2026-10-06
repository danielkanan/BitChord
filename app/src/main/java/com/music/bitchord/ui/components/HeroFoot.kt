package com.music.bitchord.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.music.bitchord.ui.player.FootBlurSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * A detail page's hero photograph with its foot run through a real
 * progressive blur — see [progressiveFoot] — and the colour that blur settles
 * into, which is the colour the page itself is painted.
 *
 * [image] is the photograph cropped to the hero box exactly as
 * `ContentScale.Crop` would show it, with [runFraction] of its height again
 * underneath, where the blur finishes arriving at [solid].
 */
@Immutable
class HeroFoot(val image: ImageBitmap, val solid: Color, val runFraction: Float)

/**
 * The same blur for a clip playing over this hero, settling into the same
 * colour, for a box [widthPx] wide over a photograph [photoHeightPx] tall.
 */
fun HeroFoot.videoBlur(widthPx: Int, photoHeightPx: Int): FootBlurSpec = FootBlurSpec(
    contentHeightPx = photoHeightPx,
    startPx = photoHeightPx * FOOT_START,
    fullPx = photoHeightPx * FOOT_BLUR_FULL,
    fadeStartPx = photoHeightPx * FOOT_FADE_START,
    fadeEndPx = photoHeightPx * FOOT_FADE_END,
    maxSigmaPx = widthPx * FOOT_SIGMA,
    solid = solid,
)

/**
 * [HeroFoot] for the artwork at [url], in a hero box [widthPx] wide over a
 * photograph [photoHeightPx] tall.
 *
 * Null until it has been read, and on a page with no artwork. Read once per
 * photograph and box, so a page opened again has it on its first frame. While
 * a different photograph is read into the same box, the previous one is held
 * rather than dropped — a playlist whose collage changes does not blink empty.
 *
 * [solid], when the photograph came with one — Apple's own colour for an
 * artist's hero — is what the blur settles into. Without it, the picture's
 * bottom colour is read and deepened.
 */
@Composable
fun rememberHeroFoot(url: String?, widthPx: Int, photoHeightPx: Int, solid: Int? = null): HeroFoot? {
    val context = LocalContext.current
    val key = remember(url, widthPx, photoHeightPx, solid, footTuning()) {
        url?.takeIf { widthPx > 0 && photoHeightPx > 0 }?.let {
            FootKey(it, widthPx, photoHeightPx, solid, footTuning())
        }
    }
    var foot by remember(key) { mutableStateOf(key?.let(footCache::get)) }
    val held = remember(widthPx, photoHeightPx) { mutableStateOf<HeroFoot?>(null) }
    LaunchedEffect(key) {
        if (key == null || foot != null) return@LaunchedEffect
        val read = renderHeroFoot(context, key)
        if (read == null) {
            held.value = null
        } else {
            footCache.put(key, read)
        }
        foot = read
    }
    SideEffect {
        if (foot != null || key == null) held.value = foot
    }
    return foot ?: held.value.takeIf { key != null }
}

private data class FootKey(
    val url: String,
    val widthPx: Int,
    val photoHeightPx: Int,
    val solid: Int?,
    val tuning: String,
)

/** So a change to where the fade sits is not served the previous picture. */
private fun footTuning(): String =
    listOf(FOOT_START, FOOT_BLUR_FULL, FOOT_FADE_START, FOOT_FADE_END, FOOT_SIGMA, FOOT_RUN)
        .joinToString(",")

private suspend fun renderHeroFoot(context: Context, key: FootKey): HeroFoot? {
    val request = ImageRequest.Builder(context)
        .data(key.url)
        // No smaller than the box, as AsyncImage would have asked for, and
        // cropped below rather than by the decoder.
        .size(key.widthPx, key.photoHeightPx)
        .scale(Scale.FILL)
        .precision(Precision.INEXACT)
        .allowHardware(false)
        .build()
    val source = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)
        ?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        val photo = source.croppedTo(key.widthPx, key.photoHeightPx)
        val pixels = IntArray(photo.width * photo.height)
        photo.getPixels(pixels, 0, photo.width, 0, 0, photo.width, photo.height)
        val foot = progressiveFoot(pixels, photo.width, photo.height, solid = key.solid)
        val bitmap = Bitmap.createBitmap(foot.pixels, foot.width, foot.height, Bitmap.Config.ARGB_8888)
        bitmap.prepareToDraw()
        HeroFoot(
            image = bitmap.asImageBitmap(),
            solid = Color(foot.solid),
            runFraction = (foot.height - photo.height) / photo.height.toFloat(),
        )
    }
}

/**
 * The part of this bitmap `ContentScale.Crop` shows in a [boxWidth] x
 * [boxHeight] box — centred, at the box's aspect — no larger than the box.
 */
private fun Bitmap.croppedTo(boxWidth: Int, boxHeight: Int): Bitmap {
    val boxAspect = boxWidth.toFloat() / boxHeight
    val cropWidth: Int
    val cropHeight: Int
    if (width.toFloat() / height > boxAspect) {
        cropHeight = height
        cropWidth = (height * boxAspect).roundToInt().coerceIn(1, width)
    } else {
        cropWidth = width
        cropHeight = (width / boxAspect).roundToInt().coerceIn(1, height)
    }
    val outWidth = cropWidth.coerceAtMost(boxWidth)
    val outHeight = (outWidth / boxAspect).roundToInt().coerceAtLeast(1)
    val scale = Matrix().apply {
        setScale(outWidth / cropWidth.toFloat(), outHeight / cropHeight.toFloat())
    }
    return Bitmap.createBitmap(
        this,
        (width - cropWidth) / 2,
        (height - cropHeight) / 2,
        cropWidth,
        cropHeight,
        scale,
        true,
    )
}

/**
 * Feet already read, by photograph and box. Counted in bytes: one is a few
 * megabytes, so this holds the handful of pages a back stack revisits.
 */
private val footCache = object : LruCache<FootKey, HeroFoot>(FOOT_CACHE_BYTES) {
    override fun sizeOf(key: FootKey, value: HeroFoot) = value.image.width * value.image.height * 4
}

private const val FOOT_CACHE_BYTES = 24 * 1024 * 1024
