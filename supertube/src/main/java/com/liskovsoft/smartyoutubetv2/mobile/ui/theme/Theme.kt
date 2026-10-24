package com.liskovsoft.smartyoutubetv2.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Red = Color(0xFFE53935)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF1C1B1B),
    surfaceContainerHigh = Color(0xFF272525),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC00011),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
)

/** Accent used for the "live" badge, watch progress and the player seek bar */
val YouTubeRed = Red

@Composable
fun SmartTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 && darkTheme -> dynamicDarkColorScheme(context).copy(background = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F))
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, content = content)
}
