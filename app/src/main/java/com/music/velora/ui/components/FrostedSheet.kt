/*
 * Shared frosted shell for action / picker bottom sheets — same material as
 * the floating nav, with a drag handle and display-matched bottom corners.
 */
package com.music.velora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.music.velora.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState

/** Top corners stay a fixed sheet radius; the bottom follows the display. */
val FROSTED_SHEET_TOP_RADIUS = 28.dp

/**
 * A bottom sheet in the same frost family as the floating nav: identical
 * [frostedSurface] material, rounded on every corner so the foot matches the
 * phone's display corners, and a compact drag handle drawn here because the
 * host must pass a transparent [androidx.compose.material3.ModalBottomSheet]
 * container for the frost to show through.
 *
 * [palette] tints the frost from a page or sleeve when the host has one;
 * otherwise the sheet uses the default dark frost (or whatever
 * [LocalFrostChrome] already provides).
 *
 * [scrollable] wraps [content] in a vertical scroll for short action lists.
 * Turn it off when the caller hosts its own [androidx.compose.foundation.lazy.LazyColumn].
 */
@Composable
fun FrostedSheet(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    palette: ArtworkPalette? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chrome = palette?.toFrostChrome() ?: frostChromeColors()
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    val bottomRadius = rememberBottomDisplayCornerRadius(fallback = FROSTED_SHEET_TOP_RADIUS)
    val sheetShape = RoundedCornerShape(
        topStart = FROSTED_SHEET_TOP_RADIUS,
        topEnd = FROSTED_SHEET_TOP_RADIUS,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
    // Remap the theme colours so MaterialTheme-using sheet bodies (settings
    // pickers, listen-together forms, …) stay legible on the dark frost tint
    // without each one re-wiring every Text / Icon by hand.
    val sheetScheme = MaterialTheme.colorScheme.copy(
        background = chrome.tint,
        surface = chrome.tint,
        surfaceVariant = chrome.tint.copy(alpha = 0.55f),
        onBackground = chrome.content,
        onSurface = chrome.content,
        onSurfaceVariant = chrome.contentVariant,
        outline = chrome.edge.copy(alpha = 0.55f),
        primary = chrome.accent,
        onPrimary = chrome.content,
    )
    CompositionLocalProvider(LocalFrostChrome provides chrome) {
        MaterialTheme(colorScheme = sheetScheme) {
            Box(
                modifier
                    .fillMaxWidth()
                    // Flush to the screen foot so [bottomRadius] lands on the
                    // display's own corners rather than floating above them.
                    .frostedSurface(shape = sheetShape, hazeState = hazeState),
            ) {
                Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 34.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(chrome.content.copy(alpha = 0.35f)),
                        )
                    }
                    if (scrollable) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                            content = content,
                        )
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}
