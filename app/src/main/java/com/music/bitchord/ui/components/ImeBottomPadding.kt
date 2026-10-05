/*
 * Keyboard inset for frosted sheets hosted in a ModalBottomSheet Dialog.
 *
 * Compose's [androidx.compose.foundation.layout.WindowInsets.ime] alone is not
 * enough here: the sheet lives in its own dialog window, which often never
 * dispatches a full IME inset into the composition — so padding from
 * [imePadding] lifts the drawer only partway (or not at all).
 *
 * Height is taken from the dialog decor's visible display frame (screen height
 * minus the unobscured bottom), which tracks the real keyboard even when IME
 * WindowInsets under-report.
 */
package com.music.bitchord.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Bottom padding equal to the on-screen keyboard's height, measured from the
 * dialog (or activity) window that hosts this composition.
 */
@Composable
fun rememberImeBottomPadding(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val composeImePx = WindowInsets.ime.getBottom(density)
    var imeBottomPx by remember { mutableIntStateOf(composeImePx) }

    DisposableEffect(view) {
        val window = findHostWindow(view)
        val decor = window?.decorView ?: view.rootView
        val previousMode = window?.attributes?.softInputMode
        val visibleFrame = Rect()

        if (window != null) {
            // Edge-to-edge dialog: IME height is reported as insets rather than
            // by resizing the decor. Without ADJUST_RESIZE some OEM dialogs
            // never shrink the visible frame when the keyboard opens.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        fun readKeyboardHeight() {
            decor.getWindowVisibleDisplayFrame(visibleFrame)
            // Root height covers the full window; the visible frame's bottom is
            // where the keyboard begins. Difference = what is covering the foot.
            val covered = (decor.rootView.height - visibleFrame.bottom).coerceAtLeast(0)
            val rootInsets = ViewCompat.getRootWindowInsets(decor)
            val nav = rootInsets
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                ?.bottom
                ?: 0
            val imeInset = rootInsets
                ?.getInsets(WindowInsetsCompat.Type.ime())
                ?.bottom
                ?: 0
            // Closed: [covered] is often just the nav bar — ignore that so the
            // frost stays flush to the screen corners. Open: take the larger of
            // the inset and the visible-frame gap so under-reported IME insets
            // cannot leave the drawer sitting on the keys.
            imeBottomPx = when {
                imeInset > 0 -> maxOf(imeInset, covered)
                covered > nav -> covered
                else -> 0
            }
        }

        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { readKeyboardHeight() }
        decor.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        readKeyboardHeight()

        onDispose {
            decor.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            if (window != null && previousMode != null) {
                window.setSoftInputMode(previousMode)
            }
        }
    }

    // Prefer the live reading; keep Compose IME as a floor so a frame where
    // the listener has not fired yet still lifts something.
    val px = maxOf(imeBottomPx, composeImePx)
    return with(density) { px.toDp() }
}

/** Dialog window when inside a ModalBottomSheet; otherwise the Activity window. */
private fun findHostWindow(view: View): Window? {
    var parent = view.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) return context.window
        context = context.baseContext
    }
    return null
}
