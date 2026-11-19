package com.liskovsoft.smartyoutubetv2.mobile.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Insets in px of the left, right and bottom edges (the navigation bar is never at the top) */
data class EdgeInsets(val left: Int = 0, val right: Int = 0, val bottom: Int = 0)

/**
 * Space to keep free for the navigation bar.
 * Three-button navigation (the bar is tappable): the whole bar, so videos and lists aren't drawn under the buttons.
 * Gesture navigation (only a thin handle, nothing tappable): nothing, the content stays edge to edge.
 * Before Android 10 there was no gesture navigation and the tappable insets don't exist: always the whole bar.
 */
fun navButtonsPadding(navigationBars: EdgeInsets, tappable: EdgeInsets, sdkInt: Int): EdgeInsets {
    if (sdkInt < Build.VERSION_CODES.Q) return navigationBars
    return EdgeInsets(
        left = if (tappable.left > 0) navigationBars.left else 0,
        right = if (tappable.right > 0) navigationBars.right else 0,
        bottom = if (tappable.bottom > 0) navigationBars.bottom else 0,
    )
}

/**
 * Keeps the screen out from under the three Android navigation buttons, painting [color] behind them.
 * The space is consumed: navigation bar insets inside (mini player, list spacers, bottom bar) don't add it again.
 */
@Composable
fun NavButtonsSafeArea(
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val nav = WindowInsets.navigationBars
    val tappable = WindowInsets.tappableElement
    val pad = navButtonsPadding(
        navigationBars = EdgeInsets(nav.getLeft(density, direction), nav.getRight(density, direction), nav.getBottom(density)),
        tappable = EdgeInsets(tappable.getLeft(density, direction), tappable.getRight(density, direction), tappable.getBottom(density)),
        sdkInt = Build.VERSION.SDK_INT,
    )
    val view = LocalView.current
    val hasButtons = pad != EdgeInsets()
    SideEffect { if (hasButtons) paintNavButtonsBar(view, color) }

    val padding = with(density) { PaddingValues.Absolute(left = pad.left.toDp(), right = pad.right.toDp(), bottom = pad.bottom.toDp()) }

    Box(Modifier.fillMaxSize().background(color).padding(padding).consumeWindowInsets(padding)) {
        content()
    }
}

/** Solid bar behind the buttons, with icons that contrast with it (the system scrim left white icons on a light bar) */
private fun paintNavButtonsBar(view: View, color: Color) {
    var context = view.context
    while (context !is Activity && context is ContextWrapper) context = context.baseContext
    val window = (context as? Activity)?.window ?: return
    @Suppress("DEPRECATION")
    window.navigationBarColor = color.toArgb()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = color.luminance() > 0.5f
}
