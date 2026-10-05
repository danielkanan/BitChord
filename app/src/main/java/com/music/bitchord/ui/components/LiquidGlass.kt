/*
 * Shared frosted chrome for the floating nav / top controls / search field.
 * Material is Haze blur (same family as the classic FloatingBottomBar), not the
 * former backdrop-sampled liquid glass refraction path.
 */
package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Tint washed over every frosted Haze surface when no page palette is active. */
val FROST_TINT = Color(0xFF222222)

/**
 * The hairline along a floating bar's edge — same stroke every frosted
 * surface shares when no page palette is active.
 */
val GLASS_EDGE_WIDTH = 0.5.dp
val GLASS_EDGE_COLOR = Color(0xFF3E3E3E)

/**
 * Colours the floating frost chrome takes from an artwork-led page.
 *
 * Null in [LocalFrostChrome] means the fixed dark frost ([FROST_TINT] /
 * [GLASS_EDGE_COLOR]) and the theme's onSurface / primary for glyphs.
 */
@Immutable
data class FrostChromeColors(
    val tint: Color,
    val edge: Color,
    val content: Color,
    val contentVariant: Color,
    val accent: Color,
)

/**
 * When non-null, every [frostedSurface] and the floating bars paint with these
 * colours instead of the fixed dark frost — so album / playlist chrome tracks
 * the sleeve the page is washed in.
 */
val LocalFrostChrome = staticCompositionLocalOf<FrostChromeColors?> { null }

/** Frost chrome colours derived from an [ArtworkPalette]. */
fun ArtworkPalette.toFrostChrome(): FrostChromeColors = FrostChromeColors(
    // Same elevated fill the page's own glass chips use, so the bars belong
    // to the wash rather than sitting as a grey cutout on top of it.
    tint = elevated,
    edge = onBackground.copy(alpha = 0.18f),
    content = onBackground,
    contentVariant = onBackgroundVariant,
    accent = accent,
)

/** Resolved frost colours for the call site — page palette, or the defaults. */
@Composable
fun frostChromeColors(): FrostChromeColors {
    LocalFrostChrome.current?.let { return it }
    val scheme = MaterialTheme.colorScheme
    return FrostChromeColors(
        tint = FROST_TINT,
        edge = GLASS_EDGE_COLOR,
        content = scheme.onSurface,
        contentVariant = scheme.onSurfaceVariant,
        accent = scheme.primary,
    )
}

/**
 * Frosted Haze fill clipped to [shape], or a solid tint when blur is
 * reduced / no [hazeState] is available. Always finishes with the chrome edge.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.frostedSurface(
    shape: CornerBasedShape,
    hazeState: HazeState?,
): Modifier {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val chrome = frostChromeColors()
    return clip(shape)
        .then(
            when {
                hazeState != null && !reduceDynamicBlur -> Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(chrome.tint),
                )
                else -> Modifier.background(chrome.tint, shape)
            },
        )
        .border(GLASS_EDGE_WIDTH, chrome.edge, shape)
}
