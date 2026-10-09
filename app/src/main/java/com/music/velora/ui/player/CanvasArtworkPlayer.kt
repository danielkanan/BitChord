package com.music.velora.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.music.velora.ui.components.footFadeCoverage
import com.music.velora.ui.rememberIsForeground
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.music.velora.data.Http
import com.music.velora.data.canvas.CanvasArtwork
import com.music.velora.data.canvas.CanvasCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.roundToInt
import java.util.Locale

private const val TAG = "CanvasArtworkPlayer"

/**
 * How long a clip gets to paint itself onto a surface it was just handed back
 * before the still art is brought in behind it instead. Long enough to cover a
 * decoder being re-created from cold, short enough that a clip which is never
 * coming back does not sit there as a hole for the length of a glance.
 */
private const val REPAINT_TIMEOUT_MS = 700L

/** How a clip fills the bounds supplied by its caller. */
enum class CanvasContentMode {
    CROP,
    FIT_PORTRAIT,
}

/**
 * The looping video that plays over a track's cover art, sized to fill and
 * clipped by whatever laid it out.
 *
 * A second, deliberately unassuming ExoPlayer: silent, with its audio track
 * switched off entirely so a clip's soundtrack is never even fetched, and no
 * audio attributes — taking focus here would duck the music this is decorating.
 * It follows the transport, so pausing the track stops the sleeve moving too.
 *
 * Nothing is drawn until the first frame arrives, and the fade in from there
 * means a failed or slow clip simply leaves the still art showing rather than
 * flashing a black square over it. [CanvasArtwork.fallbackUrl] gets one try if
 * the first rendition won't decode.
 */
@OptIn(UnstableApi::class)
@Composable
fun CanvasArtworkPlayer(
    canvas: CanvasArtwork,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    contentMode: CanvasContentMode = CanvasContentMode.CROP,
    /** Align a contained portrait clip to the top; other callers retain centered FIT. */
    alignPortraitTop: Boolean = false,
    /** The decoded video's display aspect, or zero until Media3 knows it. */
    onAspectRatioChanged: (Float) -> Unit = {},
    /** The full player bounds required before a portrait hero may reveal its first frame. */
    portraitRevealBounds: IntSize = IntSize.Zero,
    /** Fades a full-player portrait clip away as the existing sleeve collapses. */
    presentationAlpha: () -> Float = { 1f },
    /** Fires once the clip has an actual frame on screen, and again if it drops back to none. */
    onRenderedChanged: (Boolean) -> Unit = {},
    /** A single frame off the playing clip, for callers that want to re-tint around it. */
    onFrameCaptured: (Bitmap) -> Unit = {},
    /**
     * Keep calling [onFrameCaptured] every so many milliseconds instead of
     * only once — for a caller re-tinting its backdrop off a playing clip,
     * which is worth following as it plays rather than settling on whatever
     * colours its opening frame happened to have. That holds for every
     * source, not just
     * [CanvasSource.SPOTIFY][com.music.velora.data.canvas.CanvasSource.SPOTIFY]:
     * a clip is a clip, and one that pans or cuts changes colour under its own
     * still sleeve exactly the same way regardless of who published it. Null
     * when a caller has nothing worth re-tinting off a moving colour at all —
     * re-reading a texture off the GPU costs a frame stall, so this stays
     * opt-in rather than always-on.
     */
    refreshFrameEveryMs: Long? = null,
    /**
     * The longest edge of the bitmap [onFrameCaptured] is handed.
     *
     * This is the whole cost of following a clip. `getBitmap()` with no
     * arguments hands back a copy at the view's own size — full-bleed, so most
     * of a phone screen, five or six megabytes read back off the GPU and
     * allocated afresh on every call. Nobody wants that resolution: the one
     * caller there is averages the frame down to a handful of colours. Asking
     * for a small copy instead makes the readback scale during the blit, which
     * is what turns a refresh from something worth doing every few seconds into
     * something affordable several times a second.
     */
    frameCapturePx: Int = FRAME_CAPTURE_PX,
    /**
     * How much of whatever is behind the clip it is currently hiding: 0 while
     * nothing is drawn, ramping to 1 as the first frame fades in, and back down
     * if it drops out again.
     *
     * A caller that stacks a still image under the clip needs this to take that
     * image back out from under it, and cannot get there from
     * [onRenderedChanged] alone — that fires when the fade *starts*. It matters
     * most with [bottomFade]: one gradient over each of two stacked layers
     * leaves the lower one showing through the upper one instead of the backdrop
     * showing through both, so the still art stays half-visible over the clip
     * for as long as it is left lit underneath.
     */
    onCoverChanged: (Float) -> Unit = {},
    /**
     * Share of the clip's height, measured up from its bottom edge, over which
     * it dissolves to nothing — 0 for a hard edge. See [setBottomFade] for why
     * this is a parameter here rather than a mask the caller could draw.
     */
    bottomFade: Float = 0f,
    /** Optional end of the fade in view pixels; defaults to the view's bottom edge. */
    bottomFadeEndPx: Float? = null,
    /**
     * A real blur across the clip's foot and on below it, into a solid colour,
     * instead of [bottomFade]. Android 12+ only, where a blur effect exists;
     * ignored below that, where a caller should not ask.
     */
    footBlur: FootBlurSpec? = null,
    /**
     * Halts decoding for the length of a caller-driven transition — the sleeve
     * collapsing into the queue or lyrics panel and back — rather than only at
     * the two ends of it. That collapse is driven by the same clock as this
     * clip's own fade, and a decoder left running through it competes with the
     * slide for the same frame budget; the stutter that produced this flag was
     * the decode, not the animation. The clip keeps its last frame on screen
     * while paused, so there is nothing to fade back in once it lifts.
     */
    pausedForTransition: Boolean = false,
) {
    val context = LocalContext.current

    var url by remember(canvas) { mutableStateOf(canvas.url) }
    var rendered by remember(canvas) { mutableStateOf(false) }
    // Aspect of the clip itself. Zero until the decoder reports it, which is
    // also the signal that there is nothing sensible to crop to yet.
    var clipAspect by remember(canvas) { mutableFloatStateOf(0f) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var textureView by remember(canvas) { mutableStateOf<TextureView?>(null) }
    // Frames are counted rather than flagged, because [rendered] cannot answer
    // the question the repaint below has to ask: "did a frame land on *this*
    // surface", not "has one ever landed".
    var frameTick by remember(canvas) { mutableIntStateOf(0) }
    // Bumped each time the view is handed a surface to replace one that was
    // taken away — which, in practice, means each time the app comes back from
    // off screen. Not bumped for the first surface of all, which arrives with
    // nothing needing doing to it. See the repaint effect below.
    var surfaceGeneration by remember(canvas) { mutableIntStateOf(0) }
    val currentContentMode by rememberUpdatedState(contentMode)
    val currentAlignPortraitTop by rememberUpdatedState(alignPortraitTop)
    val currentPortraitRevealBounds by rememberUpdatedState(portraitRevealBounds)
    val currentPresentationAlpha by rememberUpdatedState(presentationAlpha)
    val currentFootBlur by rememberUpdatedState(footBlur)
    val reportAspect by rememberUpdatedState(onAspectRatioChanged)
    val player = remember {
        ExoPlayer.Builder(context)
            // Shares the app's one OkHttp client, as everything that fetches
            // over the network here does — and wrapped in CanvasCache so a
            // loop past the first is read off disk rather than re-fetched;
            // see that object's doc for why this matters far more here than
            // it would for a clip played once.
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(CanvasCache.dataSourceFactory(OkHttpDataSource.Factory(Http.client))),
            )
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val width = videoSize.width * videoSize.pixelWidthHeightRatio
                val aspect = if (width.isFinite() && width > 0f && videoSize.height > 0) {
                    width / videoSize.height
                } else 0f
                if (aspect != clipAspect) rendered = false
                clipAspect = aspect
                reportAspect(aspect)
                textureView?.applyContentTransform(clipAspect, currentContentMode, currentAlignPortraitTop)
            }

            override fun onPlayerError(error: PlaybackException) {
                // One retry, at the other rendition. If that is the one that
                // just failed there is nowhere left to go: leave the still
                // art up rather than looping through a broken URL.
                val alternate = canvas.fallbackUrl
                if (alternate != null && alternate != url) {
                    url = alternate
                } else {
                    rendered = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(url) {
        rendered = false
        clipAspect = 0f
        reportAspect(0f)
        val item = MediaItem.Builder().setUri(url)
        mimeTypeOf(url)?.let { item.setMimeType(it) }
        player.setMediaItem(item.build())
        player.prepare()
    }

    // Gated on the app being on screen as well as on the caller's own state.
    //
    // This is a video decoder. Left to [isPlaying] alone it goes on decoding
    // frames into a surface nobody can see for as long as the composition is
    // alive — which, with the phone in a pocket and music playing, is the whole
    // album. Worse on a detail page, whose caller passes a constant `true`
    // because "the page is only up while it's being read": true of a page being
    // looked at, not of one left open behind a locked screen.
    //
    // Held inside this component rather than asked of each caller, so no call
    // site can forget it. [isPlaying] covers transport on the player and hero
    // visibility on a detail page — the last frame stays up while decode is off.
    val foreground = rememberIsForeground()
    LaunchedEffect(foreground, pausedForTransition, isPlaying) {
        player.playWhenReady = foreground && !pausedForTransition && isPlaying
    }

    LaunchedEffect(bounds.width, bounds.height) {
        val w = bounds.width
        val h = bounds.height
        if (w <= 0 || h <= 0) return@LaunchedEffect
        val maxEdge = maxOf(w, h)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(maxEdge, maxEdge)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
    }

    // Repaint onto a surface that has just been handed back. A TextureView's
    // SurfaceTexture does not survive every background/layout transition, and
    // ExoPlayer's old "first frame rendered" callback says nothing about the
    // replacement surface. Keep the still artwork visible while waiting, ask
    // the decoder to paint at its current position, and only hand back to the
    // clip when onSurfaceTextureUpdated confirms real pixels below.
    LaunchedEffect(surfaceGeneration) {
        if (surfaceGeneration == 0) return@LaunchedEffect
        rendered = false
        val before = frameTick
        if (player.playbackState != Player.STATE_IDLE) {
            player.seekTo(player.currentPosition)
        }
        delay(REPAINT_TIMEOUT_MS)
        if (frameTick == before) {
            rendered = false
        }
    }

    val reportRendered by rememberUpdatedState(onRenderedChanged)
    LaunchedEffect(rendered) {
        reportRendered(rendered)
        if (!rendered) return@LaunchedEffect
        // Let the surface actually paint the frame that just triggered this
        // before reading it back — grabbing it the instant the callback fires
        // can still catch the previous, empty buffer.
        withFrameMillis { }
        val view = textureView ?: return@LaunchedEffect
        val bitmap = view.captureAt(frameCapturePx, clipAspect, contentMode, alignPortraitTop)
        if (bitmap != null) {
            Log.d(TAG, "frame captured after rendered=true, size=${bitmap.width}x${bitmap.height}")
            onFrameCaptured(bitmap)
        } else {
            Log.w(TAG, "frame capture returned null after rendered=true")
        }
    }

    // The opt-in follow-up to the capture above, for a caller that asked for
    // one — see [refreshFrameEveryMs]. A separate effect rather than a loop
    // folded into the one above: that one is keyed on [rendered] so it fires
    // again on every fade-in, and this one only needs to start once a fade-in
    // has actually happened and then keep going for as long as it holds.
    LaunchedEffect(rendered, refreshFrameEveryMs, frameCapturePx, clipAspect, contentMode, alignPortraitTop) {
        val interval = refreshFrameEveryMs ?: return@LaunchedEffect
        Log.d(TAG, "periodic frame refresh started, interval=$interval")
        if (!rendered) return@LaunchedEffect
        while (isActive) {
            delay(interval)
            val view = textureView ?: continue
            val bitmap = view.captureAt(frameCapturePx, clipAspect, contentMode, alignPortraitTop)
            if (bitmap != null) {
                Log.d(TAG, "periodic frame captured, size=${bitmap.width}x${bitmap.height}")
                onFrameCaptured(bitmap)
            } else {
                Log.w(TAG, "periodic frame capture returned null")
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (rendered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "canvasAlpha",
    )

    // Published rather than left for the caller to mirror with a second
    // animation off [onRenderedChanged]: one fade, one account of how far along
    // it is. Zeroed on the way out, or a caller would be left holding something
    // hidden behind a clip that is no longer mounted.
    val reportCover by rememberUpdatedState(onCoverChanged)
    LaunchedEffect(Unit) {
        snapshotFlow { alpha * currentPresentationAlpha() }.collect { reportCover(it) }
    }
    DisposableEffect(Unit) {
        onDispose {
            // The parent owns the still/canvas handoff. Never leave it holding
            // a Success from a player or TextureView that no longer exists.
            reportRendered(false)
            reportCover(0f)
            reportAspect(0f)
        }
    }

    val viewUpdateCache = remember(canvas) { CanvasViewUpdateCache() }

    AndroidView(
        factory = { viewContext ->
            val texture = TextureView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isClickable = false
                isFocusable = false
                // Blend rather than punch a hole: the still sleeve stays
                // visible underneath for the length of the fade.
                isOpaque = false
                this.alpha = 0f
                // Under a foot blur the clip is laid out shorter than its
                // frame, after the update that asked for it; its crop has to
                // follow it there.
                addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    val resized = right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop
                    if (resized && currentFootBlur != null) {
                        (view as TextureView).applyContentTransform(
                            clipAspect, currentContentMode, currentAlignPortraitTop,
                        )
                    }
                }
                player.setVideoTextureView(this)
                // setVideoTextureView installs ExoPlayer's own listener, and
                // the player has to keep it — it is how the surface reaches
                // the video renderer at all. So wrap it rather than replace
                // it: everything is passed straight through, and the one
                // callback that matters here is noted on the way past.
                //
                // Asking the lifecycle instead would be simpler and wrong. The
                // surface comes back on the first traversal after the activity
                // is visible, which is *after* ON_RESUME — a repaint fired
                // there lands on the placeholder surface and the real one
                // arrives blank a moment later.
                val delegate = surfaceTextureListener
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    /** Whether the next surface is a replacement for one taken away. */
                    private var replacing = false

                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        delegate?.onSurfaceTextureAvailable(surface, width, height)
                        // The first surface needs nothing: prepare() paints it.
                        if (!replacing) return
                        replacing = false
                        Log.d(TAG, "surface recreated (gen $surfaceGeneration), waiting for frame")
                        surfaceGeneration++
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        delegate?.onSurfaceTextureSizeChanged(surface, width, height)
                        if (currentContentMode == CanvasContentMode.FIT_PORTRAIT && clipAspect in 0f..1f) {
                            rendered = false
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        replacing = true
                        Log.d(TAG, "surface destroyed, rendered=$rendered")
                        // The old buffer is gone now, not when/if ExoPlayer
                        // later reports another first frame. Restore the still
                        // artwork immediately so an empty replacement surface
                        // can never become the only visible artwork layer.
                        rendered = false
                        return delegate?.onSurfaceTextureDestroyed(surface) ?: true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        delegate?.onSurfaceTextureUpdated(surface)
                        // This callback is the proof that the current
                        // TextureView, rather than some previously destroyed
                        // surface, contains a drawable video buffer.
                        // In portrait-fit mode the frame is not handed to the
                        // still artwork until its aspect and transform are ready.
                        if (!rendered) {
                            val transformed = textureView?.applyContentTransform(
                                clipAspect, currentContentMode, currentAlignPortraitTop,
                            ) == true
                            val portrait = currentContentMode == CanvasContentMode.FIT_PORTRAIT &&
                                clipAspect > 0f && clipAspect < 1f
                            val expected = currentPortraitRevealBounds
                            val view = textureView
                            val layoutReady = !portrait ||
                                (expected != IntSize.Zero && view?.width == expected.width &&
                                    view?.height == expected.height)
                            if ((currentContentMode == CanvasContentMode.CROP || transformed) && layoutReady) {
                                rendered = true
                                frameTick++
                                Log.d(TAG, "first frame on surface (tick $frameTick, gen $surfaceGeneration)")
                            }
                        }
                    }
                }
            }
            textureView = texture
            // Wrapped on every API level so there is one view tree to reason
            // about: below API 31 the frame is what draws [bottomFade], and
            // above it the frame is just a box around the texture.
            FadingBottomFrame(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isClickable = false
                isFocusable = false
                addView(texture)
            }
        },
        update = { frame ->
            // Not child 0: the page colour sits behind the clip, at the front
            // of the list, once the foot blur has been asked for.
            val view = frame.clipView()
            // Set on the view itself. A Compose alpha layer over a TextureView
            // is not reliably composited, and this is the same fade either way.
            val cover = if (contentMode == CanvasContentMode.FIT_PORTRAIT && clipAspect <= 0f) {
                0f
            } else {
                // Called here, in the view's update, so a fade driven by the
                // player's collapse re-runs this block rather than
                // recomposing the player around it.
                alpha * presentationAlpha()
            }
            val foot = footBlur?.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
            val footPath = when {
                foot != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> 2
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> 1
                else -> 0
            }
            if (viewUpdateCache.matches(
                    cover = cover,
                    foot = foot,
                    footPath = footPath,
                    bounds = bounds,
                    clipAspect = clipAspect,
                    contentMode = contentMode,
                    alignPortraitTop = alignPortraitTop,
                    bottomFade = bottomFade,
                    bottomFadeEndPx = bottomFadeEndPx,
                )
            ) {
                return@AndroidView
            }
            view.layOutAtHeight(foot?.contentHeightPx)
            view.applyContentTransform(clipAspect, contentMode, alignPortraitTop)
            if (footPath == 2) {
                // The clip stays fully drawn. The frame's own alpha is what
                // fades it in, so the shader below reads the picture at full
                // strength and the whole foot — blur included — arrives with it.
                view.alpha = 1f
                frame.alpha = cover
                frame.fadeFraction = 0f
                view.setRenderEffect(null)
                frame.setFootBlur(foot!!, bounds.width, bounds.height)
            } else if (footPath == 1) {
                view.alpha = cover
                frame.alpha = 1f
                frame.clearFootBlur(view)
                view.setBottomFade(bottomFade, bounds, bottomFadeEndPx)
            } else {
                view.alpha = cover
                frame.alpha = 1f
                frame.fadeFraction = bottomFade
                frame.fadeEndPx = bottomFadeEndPx
            }
        },
        modifier = modifier
            .onSizeChanged { bounds = it }
            // Decorative on detail pages: the list scrolls above this view, but
            // some OEMs still route vertical drags into the TextureView.
            .pointerInteropFilter { false },
    )
}

/**
 * A frame off the clip, no bigger than [maxPx] on its longest edge — see
 * [CanvasArtworkPlayer]'s `frameCapturePx`.
 *
 * The aspect is kept rather than squared off. Nothing downstream draws this,
 * but everything downstream *averages* it, and squashing one axis would quietly
 * reweight which part of the frame each average is mostly made of.
 *
 * Null whenever the view has no frame to give — it is laid out but not yet
 * measured, or its surface has gone. A caller that gets null should keep what
 * it already had; the next tick will have one.
 */
private fun TextureView.captureAt(
    maxPx: Int,
    clipAspect: Float,
    contentMode: CanvasContentMode,
    alignPortraitTop: Boolean,
): Bitmap? {
    val viewWidth = width
    val viewHeight = height
    if (viewWidth <= 0 || viewHeight <= 0) return null
    val scale = maxPx.toFloat() / maxOf(viewWidth, viewHeight)
    return runCatching {
        // TextureView.getBitmap copies its texture layer, before the view's
        // RenderEffect; the pre-31 mask is on the parent frame instead.
        // Neither display-only fade is baked into the mesh's sampled frame.
        val frame = if (scale >= 1f) {
            getBitmap()
        } else {
            getBitmap(
                (viewWidth * scale).roundToInt().coerceAtLeast(1),
                (viewHeight * scale).roundToInt().coerceAtLeast(1),
            )
        } ?: return null
        if (contentMode != CanvasContentMode.FIT_PORTRAIT || clipAspect >= 1f || clipAspect <= 0f) {
            return frame
        }

        // getBitmap includes the view's transform. Read back only at the small
        // requested size, then exclude the transparent contain margins before
        // handing pixels to the mesh or legacy palette (both average RGB).
        val viewAspect = viewWidth.toFloat() / viewHeight
        val contentWidth = if (clipAspect < viewAspect) frame.height * clipAspect else frame.width.toFloat()
        val contentHeight = if (clipAspect < viewAspect) frame.height.toFloat() else frame.width / clipAspect
        val left = ceil((frame.width - contentWidth) / 2f).toInt().coerceIn(0, frame.width - 1)
        val top = if (alignPortraitTop) 0 else
            ceil((frame.height - contentHeight) / 2f).toInt().coerceIn(0, frame.height - 1)
        val right = (frame.width - left).coerceAtLeast(left + 1)
        val bottom = if (alignPortraitTop) contentHeight.toInt().coerceIn(1, frame.height) else
            (frame.height - top).coerceAtLeast(top + 1)
        Bitmap.createBitmap(frame, left, top, right - left, bottom - top)
    }.getOrNull()
}

/**
 * Big enough that averaging it is stable, small enough that reading it back off
 * the GPU is not an event. Every consumer reduces this to a handful of colours.
 */
private const val FRAME_CAPTURE_PX = 128

/**
 * A TextureView stretches its content to its own bounds. Compensate with a
 * transform: cover by default, or contain for a portrait clip when the hero
 * explicitly requests it. An unknown size clears the old transform.
 */
private fun TextureView.applyContentTransform(
    clipAspect: Float,
    contentMode: CanvasContentMode,
    alignPortraitTop: Boolean,
): Boolean {
    val bounds = IntSize(width, height)
    if (bounds.width <= 0 || bounds.height <= 0 || !clipAspect.isFinite() || clipAspect <= 0f) {
        setTransform(Matrix())
        return false
    }
    val viewAspect = bounds.width.toFloat() / bounds.height
    val pivotX = bounds.width / 2f
    val pivotY = if (alignPortraitTop && contentMode == CanvasContentMode.FIT_PORTRAIT && clipAspect < 1f) {
        0f
    } else bounds.height / 2f
    val matrix = Matrix().apply {
        val fit = contentMode == CanvasContentMode.FIT_PORTRAIT && clipAspect < 1f
        if (fit && clipAspect > viewAspect) {
            setScale(1f, viewAspect / clipAspect, pivotX, pivotY)
        } else if (fit) {
            setScale(clipAspect / viewAspect, 1f, pivotX, pivotY)
        } else if (clipAspect > viewAspect) {
            setScale(clipAspect / viewAspect, 1f, pivotX, pivotY)
        } else {
            setScale(1f, viewAspect / clipAspect, pivotX, pivotY)
        }
    }
    setTransform(matrix)
    return true
}

/**
 * Dissolves the clip's bottom edge into whatever is behind it.
 *
 * Done here, on the view's own RenderNode, rather than with a DstIn mask in the
 * caller's draw scope: a TextureView's frames are composited from its surface
 * and a Compose blend drawn over the node simply doesn't reach them — the mask
 * lands on the layer around the video and leaves the video's own hard edge
 * exactly where it was.
 *
 * [RenderEffect] is API 31+; below that [FadingBottomFrame] does the same job
 * the older way, with a saveLayer and a Porter-Duff mask.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun TextureView.setBottomFade(fraction: Float, bounds: IntSize, endPx: Float?) {
    val endY = endPx?.coerceIn(0f, bounds.height.toFloat()) ?: bounds.height.toFloat()
    if (fraction <= 0.001f || endY <= 0f) {
        setRenderEffect(null)
        return
    }
    val gradient = LinearGradient(
        0f,
        endY * (1f - fraction.coerceAtMost(1f)),
        0f,
        endY,
        android.graphics.Color.BLACK,
        android.graphics.Color.TRANSPARENT,
        Shader.TileMode.CLAMP,
    )
    // createOffsetEffect(0, 0) is the identity effect over the node's own
    // content, which is the only way to name "what this view drew" as the
    // destination of a blend.
    setRenderEffect(
        RenderEffect.createBlendModeEffect(
            RenderEffect.createOffsetEffect(0f, 0f),
            RenderEffect.createShaderEffect(gradient),
            BlendMode.DST_IN,
        ),
    )
}

/**
 * A progressive blur across a clip's foot: sharp down to [startPx], at full
 * strength by [fullPx], then held. From [fadeStartPx] to [fadeEndPx] that
 * picture fades out. The blur is already on above the fade, and the fade runs
 * on past the clip so the transition has room.
 *
 * The moving twin of the still hero's foot (see HeroFoot), on the same lines.
 */
@Immutable
data class FootBlurSpec(
    val contentHeightPx: Int,
    val startPx: Float,
    val fullPx: Float,
    val fadeStartPx: Float,
    val fadeEndPx: Float,
    val maxSigmaPx: Float,
    val solid: Color,
)

/** Pins the clip to [heightPx] at the top of its frame, or back to filling it when null. */
private fun TextureView.layOutAtHeight(heightPx: Int?) {
    val wanted = heightPx ?: ViewGroup.LayoutParams.MATCH_PARENT
    if (layoutParams?.height == wanted) return
    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, wanted, Gravity.TOP)
}

/**
 * The clip, read once so the blur has a picture to work on. Past the clip
 * the bottom edge is held, so the fade below it is still the picture going
 * clear, not the page colour already sitting there.
 */
private const val FOOT_FLAT_SKSL = """
uniform shader content;
uniform float width;
uniform float clipBottom;

half4 main(float2 coord) {
    float2 p = float2(
        clamp(coord.x, 0.5, width - 0.5),
        clamp(coord.y, 0.5, max(clipBottom - 0.5, 0.5))
    );
    return content.eval(p);
}
"""

/**
 * How wide each step of the foot is, as a share of [FootBlurSpec.maxSigmaPx],
 * and how far down the blur box (0 at the sharp edge, 1 at full strength)
 * that step takes over. Evenly spaced, the same straight line as the still.
 */
private val FOOT_BLUR_LEVELS = arrayOf(
    0.25f to 0.25f,
    0.50f to 0.50f,
    0.75f to 0.75f,
    1f to 1f,
)

/**
 * [FOOT_FLAT_SKSL] blurred in steps down the foot. The frame's alpha follows
 * the clip's fade, so none of this is rebuilt to do it.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class FootBlur {
    private val flatShader = RuntimeShader(FOOT_FLAT_SKSL)
    private var applied: Triple<FootBlurSpec, Int, Int>? = null

    /** The blur for a frame [width] x [height], or null when it is already applied. */
    fun effect(spec: FootBlurSpec, width: Int, height: Int): RenderEffect? {
        val inputs = Triple(spec, width, height)
        if (inputs == applied) return null
        applied = inputs
        flatShader.setFloatUniform("width", width.toFloat())
        flatShader.setFloatUniform("clipBottom", spec.contentHeightPx.toFloat())
        val picture = RenderEffect.createRuntimeShaderEffect(flatShader, "content")
        val widest = spec.maxSigmaPx.coerceAtLeast(1f)
        var image = picture
        val span = (spec.fullPx - spec.startPx).coerceAtLeast(1f)
        var fromT = 0f
        for ((share, toT) in FOOT_BLUR_LEVELS) {
            val radius = (widest * share).coerceAtLeast(1f)
            val blurred = RenderEffect.createChainEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                picture,
            )
            val masked = RenderEffect.createBlendModeEffect(
                blurred,
                RenderEffect.createShaderEffect(
                    reveal(spec.startPx + span * fromT, spec.startPx + span * toT, height),
                ),
                BlendMode.DST_IN,
            )
            image = RenderEffect.createBlendModeEffect(image, masked, BlendMode.SRC_OVER)
            fromT = toT
        }
        return RenderEffect.createBlendModeEffect(
            image,
            RenderEffect.createShaderEffect(fadeOut(spec.fadeStartPx, spec.fadeEndPx, height)),
            BlendMode.DST_IN,
        )
    }

    /** Opaque on the colour edge, clear on the far edge, on the same curve as the still. */
    private fun fadeOut(fromPx: Float, toPx: Float, height: Int): LinearGradient {
        val span = height.toFloat().coerceAtLeast(1f)
        var start = (fromPx / span).coerceIn(0f, 1f)
        var end = (toPx / span).coerceIn(0f, 1f)
        if (end <= start) end = (start + 0.001f).coerceAtMost(1f)
        if (end <= start) start = (end - 0.001f).coerceAtLeast(0f)
        val steps = 8
        val colors = ArrayList<Int>(steps + 2)
        val positions = ArrayList<Float>(steps + 2)
        if (start > 0.001f) {
            colors.add(android.graphics.Color.BLACK)
            positions.add(0f)
        }
        for (i in 0..steps) {
            val s = i / steps.toFloat()
            val alpha = (footFadeCoverage(s) * 255f).toInt().coerceIn(0, 255)
            val pos = start + (end - start) * s
            if (positions.isNotEmpty() && pos <= positions.last()) continue
            colors.add(android.graphics.Color.argb(alpha, 0, 0, 0))
            positions.add(pos)
        }
        return LinearGradient(
            0f,
            0f,
            0f,
            span,
            colors.toIntArray(),
            positions.toFloatArray(),
            Shader.TileMode.CLAMP,
        )
    }

    /** Transparent above [fromPx], opaque from [toPx] down, so a blur level can take over. */
    private fun reveal(fromPx: Float, toPx: Float, height: Int): LinearGradient {
        val span = height.toFloat().coerceAtLeast(1f)
        val start = (fromPx / span).coerceIn(0.002f, 0.96f)
        val end = (toPx / span).coerceIn(start + 0.012f, 0.994f)
        val clear = android.graphics.Color.TRANSPARENT
        val opaque = android.graphics.Color.BLACK
        return LinearGradient(
            0f,
            0f,
            0f,
            span,
            intArrayOf(clear, clear, opaque, opaque),
            floatArrayOf(0f, start, end, 1f),
            Shader.TileMode.CLAMP,
        )
    }
}

/**
 * The pre-[Build.VERSION_CODES.S] bottom fade: the same dissolve
 * [setBottomFade] gets from a [RenderEffect], done the way it was done before
 * there was one.
 *
 * Draw the child into an offscreen layer, paint a gradient over that layer with
 * [PorterDuff.Mode.DST_IN], then compose the result down. Because the layer is
 * this group's — not the TextureView's own node — the video frames are inside it
 * by the time the mask lands, which is exactly what a Compose blend over the
 * texture cannot achieve.
 *
 * It costs a full-screen offscreen buffer per frame, so it stays off entirely
 * while [fadeFraction] is zero: with no fade asked for this is a plain
 * FrameLayout and `dispatchDraw` takes the ordinary path.
 */
private class FadingBottomFrame(context: Context) : FrameLayout(context) {
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    /** The clip. A colour plate may sit in front of it in the child list. */
    fun clipView(): TextureView {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextureView) return child
        }
        error("canvas frame has no clip")
    }

    /** Share of the height, from the bottom, over which the child dissolves. */
    var fadeFraction: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped == field) return
            field = clamped
            // A software layer would defeat the point — the texture has to stay
            // hardware-composited — so this is only ever the invalidate.
            gradient = null
            invalidate()
        }
    /** Optional video bottom in this frame's pixels; null retains the historical view bottom. */
    var fadeEndPx: Float? = null
        set(value) {
            if (value == field) return
            field = value
            gradient = null
            invalidate()
        }

    private val maskPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private var gradient: LinearGradient? = null
    private var gradientHeight = 0
    private var footBlur: Any? = null

    /** The page colour filling the run under the clip, so the blur has it to settle into. */
    private var solidPlate: View? = null

    /**
     * Blurs this frame's foot per [spec]: the clip pinned to the top, and the
     * run under it. The picture is read off the frame first — a blur cannot
     * see the clip otherwise — and that picture is what gets blurred.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFootBlur(spec: FootBlurSpec, width: Int, height: Int) {
        solidPlate?.visibility = GONE
        val blurWidth = this.width.takeIf { it > 0 } ?: width
        val blurHeight = this.height.takeIf { it > 0 } ?: height
        if (blurWidth <= 0 || blurHeight <= 0 || spec.contentHeightPx <= 0) return
        val blur = footBlur as? FootBlur ?: FootBlur().also { footBlur = it }
        blur.effect(spec, blurWidth, blurHeight)?.let(::setRenderEffect)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun clearFootBlur(clip: TextureView) {
        clip.setRenderEffect(null)
        if (footBlur == null && solidPlate?.visibility != VISIBLE) return
        footBlur = null
        solidPlate?.visibility = GONE
        setRenderEffect(null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradient = null
    }

    override fun dispatchDraw(canvas: Canvas) {
        val fade = fadeFraction
        val endY = fadeEndPx?.coerceIn(0f, height.toFloat()) ?: height.toFloat()
        if (fade <= 0.001f || endY <= 0f) {
            super.dispatchDraw(canvas)
            return
        }
        val shader = gradient?.takeIf { gradientHeight == height } ?: LinearGradient(
            0f,
            endY * (1f - fade),
            0f,
            endY,
            android.graphics.Color.BLACK,
            android.graphics.Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        ).also {
            gradient = it
            gradientHeight = height
        }
        maskPaint.shader = shader
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.restoreToCount(layer)
    }
}

/**
 * Apple serves HLS, Tidal and the community index serve MP4. Naming the type
 * saves ExoPlayer a sniff, and an unrecognised URL is left for it to work out.
 */
private fun mimeTypeOf(url: String): String? {
    val path = url.substringBefore('?').lowercase(Locale.ROOT)
    return when {
        path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
        else -> null
    }
}
/** Skips redundant [AndroidView] updates when scroll recomposes the parent. */
private class CanvasViewUpdateCache {
    private var cover = Float.NaN
    private var foot: FootBlurSpec? = null
    private var footPath = -1
    private var bounds = IntSize.Zero
    private var clipAspect = Float.NaN
    private var contentMode: CanvasContentMode? = null
    private var alignPortraitTop = false
    private var bottomFade = Float.NaN
    private var bottomFadeEndPx: Float? = null

    fun matches(
        cover: Float,
        foot: FootBlurSpec?,
        footPath: Int,
        bounds: IntSize,
        clipAspect: Float,
        contentMode: CanvasContentMode,
        alignPortraitTop: Boolean,
        bottomFade: Float,
        bottomFadeEndPx: Float?,
    ): Boolean {
        if (cover == this.cover && foot == this.foot && footPath == this.footPath &&
            bounds == this.bounds && clipAspect == this.clipAspect &&
            contentMode == this.contentMode && alignPortraitTop == this.alignPortraitTop &&
            bottomFade == this.bottomFade && bottomFadeEndPx == this.bottomFadeEndPx
        ) {
            return true
        }
        this.cover = cover
        this.foot = foot
        this.footPath = footPath
        this.bounds = bounds
        this.clipAspect = clipAspect
        this.contentMode = contentMode
        this.alignPortraitTop = alignPortraitTop
        this.bottomFade = bottomFade
        this.bottomFadeEndPx = bottomFadeEndPx
        return false
    }
}

