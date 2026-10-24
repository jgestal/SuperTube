package com.liskovsoft.smartyoutubetv2.mobile.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Lets the content follow the system font size but only up to [maxScale].
 * For dense, fixed-width chrome (bottom bar labels, chips, thumbnail badges) where an unlimited
 * scale breaks the layout. Body text keeps the full accessibility scale.
 */
@Composable
fun LimitFontScale(maxScale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(maxScale)),
        content = content,
    )
}

/** Single line text that shrinks (from [maxFontSize] down to [minFontSize]) until it fits the available width */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxFontSize: TextUnit = 32.sp,
    minFontSize: TextUnit = 14.sp,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxWidthPx = constraints.maxWidth
        var size = maxFontSize.value
        while (size > minFontSize.value) {
            val width = measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width
            if (width <= maxWidthPx) break
            size -= 1f
        }
        Text(text, style = style.copy(fontSize = size.sp, lineHeight = (size * 1.2f).sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}
