package com.music.velora.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * Optional drawers the host wants drawn over Now Playing with the player's
 * own [HazeState] — so frost samples the player (and cover), not the tab under
 * the player sheet. Set from MainActivity; read in [NowPlayingScreen].
 */
val LocalPlayerExtraOverlays =
    staticCompositionLocalOf<(@Composable (HazeState) -> Unit)?> { null }
