package com.music.bitchord.ui.components

import com.music.bitchord.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Shared touch target for play and next — independent of artwork height. */
private val GLYPH_SLOT = 36.dp

/** Play / pause / next glyph size inside [GLYPH_SLOT]. */
private val GLYPH_SIZE = 32.dp

/** The spinner that stands in for the play glyph, kept in proportion to it. */
private val SPINNER_SIZE = 22.dp

/** Gap between the two transport controls. */
private val TRANSPORT_GAP = 2.dp

/** Vertical padding — modest so the bar stays compact. */
private val ROW_PADDING_VERTICAL = 5.dp

/** Horizontal padding so artwork clears the pill's curved ends. */
private val ROW_PADDING_HORIZONTAL = 16.dp

/** The artwork's corner radius. */
private val ART_CORNER = 6.dp

/** Matches the tight title (16sp) + artist (14sp) line stack. */
private val ART_SIZE = 30.dp

/** Distance that makes a horizontal drag an intentional track change. */
private val TRACK_SWIPE_THRESHOLD = 72.dp

/**
 * Shared gesture for both mini-player materials. A left swipe advances through
 * the queue; a right swipe goes back, matching the full player's artwork
 * gesture. Waiting until drag end prevents one long gesture from skipping more
 * than one item.
 */
@Composable
internal fun Modifier.miniPlayerTrackSwipe(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    /** Listening in a party whose host holds the controls. Swipes say so instead of skipping. */
    locked: Boolean = false,
    onBlocked: () -> Unit = {},
): Modifier {
    // Playback state updates can recompose the bar while a finger is down.
    // Keep the gesture coroutine alive through those updates while still
    // dispatching to the latest controller callbacks when the drag finishes.
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentLocked by rememberUpdatedState(locked)
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    return pointerInput(Unit) {
        val threshold = TRACK_SWIPE_THRESHOLD.toPx()
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragCancel = { totalDrag = 0f },
            onDragEnd = {
                val crossed = totalDrag <= -threshold || totalDrag >= threshold
                when {
                    currentLocked -> if (crossed) currentOnBlocked()
                    totalDrag <= -threshold -> currentOnNext()
                    totalDrag >= threshold -> currentOnPrevious()
                }
                totalDrag = 0f
            },
            onHorizontalDrag = { change, amount ->
                change.consume()
                totalDrag += amount
            },
        )
    }
}

/** Frosted mini player that rides just above the floating tab bar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    /** @see com.music.bitchord.data.listentogether.ListenTogether.State.controlsLocked */
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val chrome = frostChromeColors()
    val haptics = rememberHaptics()
    // percent rather than a dp figure, so the corner stays exactly half the
    // height if the row's contents ever change it — which is what keeps a pill
    // a pill instead of a rounded rectangle. Same idiom as [FloatingBottomBar]
    // directly below it, so the two shapes are the same family.
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .padding(horizontal = PAGE_GUTTER)
            .clip(shape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(chrome.tint)
                } else {
                    Modifier.optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin(chrome.tint),
                    )
                },
            )
            .border(GLASS_EDGE_WIDTH, chrome.edge, shape)
            // Deliberately silent: the whole bar is the target, so it catches
            // stray taps meant for the page behind it, and the sheet rising is
            // its own confirmation. The glyphs on it still buzz.
            .clickable(onClick = onExpand)
            .miniPlayerTrackSwipe(
                onNext = {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                onPrevious = {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
                locked = controlsLocked,
                onBlocked = onBlockedControl,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_PADDING_HORIZONTAL,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tightLines = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            )
            val titleStyle = MaterialTheme.typography.titleSmall.copy(
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = tightLines,
            )
            val artistStyle = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 14.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = tightLines,
            )
            val artShape = RoundedCornerShape(ART_CORNER)
            val artHighlight = if (isSystemInDarkTheme()) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Black.copy(alpha = 0.08f)
            }
            AsyncImage(
                model = rememberRemoteArtworkUrl(song)?.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(ART_SIZE)
                    .clip(artShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, artHighlight, artShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                ExplicitSongTitle(
                    song = song,
                    style = titleStyle,
                    color = chrome.content,
                    compactBadge = true,
                )
                Text(
                    text = song.artist,
                    style = artistStyle,
                    color = chrome.contentVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isLoading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = chrome.content,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(SPINNER_SIZE),
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(GLYPH_SLOT)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                                onPlayPause()
                            },
                        ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play,
                        ),
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = chrome.content,
                        modifier = Modifier.size(GLYPH_SIZE),
                    )
                }
            }
            Spacer(Modifier.width(TRANSPORT_GAP))
            // Faded and inert rather than removed while the host holds the
            // controls, so the bar keeps its shape — see [controlsLocked].
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(GLYPH_SLOT)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !controlsLocked,
                        onClick = {
                            haptics.play(Haptic.SkipNext)
                            onNext()
                        },
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_player_next),
                    contentDescription = stringResource(R.string.widget_next),
                    tint = chrome.content.copy(alpha = if (controlsLocked) 0.3f else 1f),
                    modifier = Modifier
                        .size(GLYPH_SIZE)
                        .graphicsLayer { scaleY = 0.85f },
                )
            }
        }
    }
}
