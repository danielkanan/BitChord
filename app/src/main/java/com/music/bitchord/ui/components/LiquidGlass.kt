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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.ColorUtils
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
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
    // [FROST_TINT] is always dark, so glyph colours stay light even when the
    // app theme itself is light — otherwise labels vanish into the frost.
    return FrostChromeColors(
        tint = FROST_TINT,
        edge = GLASS_EDGE_COLOR,
        content = Color(0xFFF2F2F2),
        contentVariant = Color(0xFFB8B8B8),
        accent = scheme.primary,
    )
}

/**
 * Bottom corner radius of the current display, from the window's
 * [android.view.RoundedCorner] (API 31+).
 *
 * Edge-to-edge sheets that sit on the screen foot use this so their bottom
 * curve matches the phone's, not a fixed guess. Below API 31 — or on a square
 * panel — [fallback] is used instead.
 */
@Composable
fun rememberBottomDisplayCornerRadius(fallback: Dp = 28.dp): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var radiusPx by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        fun read() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                radiusPx = 0
                return
            }
            val insets = view.rootWindowInsets ?: return
            val left = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
            val right = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
            radiusPx = maxOf(left, right)
        }
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> read() }
        view.addOnLayoutChangeListener(listener)
        view.requestApplyInsets()
        read()
        onDispose { view.removeOnLayoutChangeListener(listener) }
    }

    return with(density) {
        (if (radiusPx > 0) radiusPx else fallback.roundToPx()).toDp()
    }
}

/**
 * The nav pills on a coloured page. [page] is the frost already taken from
 * that page, a step lighter than the wash. Darken it, holding the hue, so
 * the bar reads as a darker tint of the page behind it.
 */
fun Color.navbarTint(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = (hsl[2] - 0.10f).coerceIn(0.05f, 0.55f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Frosted Haze fill clipped to [shape], or a solid tint when blur is
 * reduced / no [hazeState] is available. Always finishes with the chrome edge.
 *
 * [tint] replaces the chrome fill. The nav bar passes the page's own colour
 * so the blur does not wash that hue back out to grey.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.frostedSurface(
    shape: CornerBasedShape,
    hazeState: HazeState?,
    tint: Color? = null,
): Modifier {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val chrome = frostChromeColors()
    val fill = tint ?: chrome.tint
    val haze = if (tint != null) {
        HazeStyle(
            backgroundColor = fill,
            tints = listOf(HazeTint(fill.copy(alpha = 0.94f))),
            blurRadius = 24.dp,
            noiseFactor = 0.08f,
            fallbackTint = HazeTint(fill),
        )
    } else {
        HazeMaterials.regular(fill)
    }
    return clip(shape)
        .then(
            when {
                hazeState != null && !reduceDynamicBlur -> Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = haze,
                )
                else -> Modifier.background(fill, shape)
            },
        )
        .border(GLASS_EDGE_WIDTH, chrome.edge, shape)
}
