/*
 * The bar's structure is EchoMusicApp/Echo-Music's AppFloatingNavBar +
 * FloatingMiniPlayer (GPL-3.0), over the FloatingTabBar vendored in
 * [com.music.bitchord.ui.components.floatingtabbar]. BitChord's own tabs, song
 * model, transport and haptics are wired through it in place of Echo's Screens
 * routing and PlayerConnection. Scroll-collapse to the compact inline bar is
 * disabled — the expanded layout is permanent.
 */
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.bitchord.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBar
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarDefaults
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarInlineBehavior
import com.music.bitchord.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState

/**
 * The floating navigation bar: the iOS 26 shape where the now playing controls
 * and the tabs are one component rather than two stacked bars.
 *
 * Always expanded — a full-width now playing pill over the tab pill that holds
 * every destination, including Search. Surfaces use the same frosted Haze
 * material as the classic floating bars.
 *
 * [song] null means nothing is playing, and the accessory is simply absent: the
 * bar is then the tab pill and Search alone.
 */
@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    /** @see com.music.bitchord.data.listentogether.ListenTogether.State.controlsLocked */
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Held for the same reason [tabs] is. The glass factory below closes over
    // this shape, and a fresh RoundedCornerShape each pass means a fresh factory
    // lambda, which the tab bar sees as a changed argument and recomposes on.
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    // Album / playlist pages push their sleeve into [LocalFrostChrome] so the
    // pills and glyphs settle into the same colour as the page underneath.
    val chrome = frostChromeColors()
    // Selected tab keeps theme primary (same as the thumb). Only the idle
    // glyphs and the mini-player row take the sleeve colour.
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = chrome.contentVariant
    val contentColor = chrome.content
    val haptics = rememberHaptics()
    // Stay expanded; the library still requires a scroll connection.
    val scrollConnection = rememberFloatingTabBarScrollConnection(
        inlineBehavior = FloatingTabBarInlineBehavior.Never,
    )

    // The tab content lambdas below are captured once per contentKey and held
    // until it changes, so a click handler that reached back to this call's
    // `onTabSelected` would go stale the moment anything it closes over moved.
    // Held through a state that is always current instead, which also keeps the
    // handler out of the key.
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    // A factory, not a value — see the note in FloatingTabBar's header. Each of
    // the three surfaces gets its own frost modifier and so its own shape cache.
    val glassSurface: @Composable () -> Modifier = {
        Modifier.frostedSurface(
            shape = pillShape,
            hazeState = hazeState,
            // On a coloured page the pills take that colour, a step darker
            // than the wash. Elsewhere they stay the dark frost.
            tint = if (LocalFrostChrome.current != null) chrome.tint.navbarTint() else null,
        )
    }

    FloatingTabBar(
        selectedTabKey = selectedIndex,
        scrollConnection = scrollConnection,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth(),
        tabBarContentModifier = glassSurface,
        expandedAccessory = song?.let { current ->
            { accessoryModifier, _ ->
                GlassNowPlaying(
                    song = current,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onExpand = onExpand,
                    controlsLocked = controlsLocked,
                    onBlockedControl = onBlockedControl,
                    modifier = accessoryModifier.fillMaxWidth().then(glassSurface()),
                )
            }
        },
        // Transparent: the frosted surface underneath is the background, and a
        // colour over it would be the thing you saw instead of the blur.
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            accessoryBackgroundColor = Color.Transparent,
            // Theme primary, not the sleeve accent — the thumb stays the same
            // on every page so it doesn't pick up the album wash.
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        ),
        // Flat — the frosted pills carry their own hairline, and an elevation
        // shadow on top would be a second offscreen layer for each surface.
        elevations = FloatingTabBarDefaults.elevations(
            inlineElevation = 0.dp,
            expandedElevation = 0.dp,
        ),
        // Expanded, this is meant to be the plain [FloatingBottomBar] with a
        // different material — same outer width, same pill inset, same tab
        // padding, same 25dp glyph — so the two bars measure identically and the
        // toggle changes the surface rather than the layout. The horizontal tab
        // padding is gone with it: the tabs divide the pill by weight now, the
        // way the plain bar's do, so a per-tab horizontal padding would only
        // inset the ripple.
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(PILL_INSET),
            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),
        ),
        // Held too: this is declared `Any?`, so a fresh list every pass is a
        // changed argument by identity and defeats skipping on its own.
        contentKey = remember(selectedIndex, tabs, contentColor, selectedColor, unselectedColor) {
            listOf(selectedIndex, tabs, contentColor, selectedColor, unselectedColor)
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedColor else unselectedColor
            val onClick = {
                if (!isSelected) haptics.play(Haptic.Select)
                currentOnTabSelected(index)
            }
            tab(
                key = index,
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(25.dp),
                    )
                },
                title = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // The library's Tab stacks the glyph and the label
                        // with nothing between them; the plain bar spaces
                        // them, and this is where that gap goes.
                        modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),
                    )
                },
                onClick = onClick,
                indication = null,
            )
        }
    }
}

/**
 * The now playing controls docked into [GlassNavBar] as its accessory.
 *
 * The content is [MiniPlayer]'s — same artwork, same transport, same haptics —
 * and not Echo's, which reaches into a player connection this app doesn't have.
 * The press response is Echo's, and belongs to the glass rather than the row:
 * a surface you can push on is the whole point of the material.
 */
@Composable
private fun GlassNowPlaying(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    contentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean,
    onBlockedControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accessoryPressScale",
    )

    // Art matches the two tight text lines (16 + 14). Buttons keep a separate
    // larger touch size so they don't force the artwork taller.
    val artSize = 30.dp
    val artShape = RoundedCornerShape(6.dp)
    val artHighlight = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val glyphSlot = 36.dp
    val glyphSize = 32.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = pressSource,
                    indication = null,
                    onClick = onExpand,
                )
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
                )
                // Extra horizontal inset clears the pill's curved ends; keep
                // vertical padding modest so the bar stays a little shorter.
                .padding(horizontal = 16.dp, vertical = 5.dp),
        ) {
            // Tight line heights — default titleSmall/bodySmall padding is what
            // made the title and artist look far apart even with no Column gap.
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
            AsyncImage(
                model = rememberRemoteArtworkUrl(song)?.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(artSize)
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
                    color = contentColor,
                    compactBadge = true,
                )
                Text(
                    text = song.artist,
                    style = artistStyle,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isLoading) {
                Box(Modifier.size(glyphSlot), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(glyphSlot)
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
                        tint = contentColor,
                        modifier = Modifier.size(glyphSize),
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(glyphSlot)
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
                    tint = contentColor.copy(alpha = if (controlsLocked) 0.3f else 1f),
                    // Same optical flatten the full player applies so next doesn't
                    // read heavier than play at the same box size.
                    modifier = Modifier
                        .size(glyphSize)
                        .graphicsLayer { scaleY = 0.85f },
                )
            }
        }
    }
}
