package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L
private const val HOLD_SPEED = 2f

/** Transient feedback drawn over the video while gestures happen */
class GestureFeedback {
    /** Accumulated seconds of consecutive double taps (negative = rewind) */
    var seekSeconds by mutableIntStateOf(0)
    var seekEvents by mutableIntStateOf(0)
    var isSpeedHold by mutableStateOf(false)
    var levelIcon by mutableStateOf<ImageVector?>(null)
    var level by mutableFloatStateOf(0f)
}

/**
 * Touch gestures of the player surface:
 * - tap: show/hide controls
 * - double tap left/right: -/+ 10 s (consecutive taps accumulate)
 * - long press: 2x speed while held
 * - vertical swipe: fullscreen brightness (left) / volume (right), swipe down in the middle exits fullscreen;
 *   portrait swipe up enters fullscreen, swipe down closes the player
 */
fun Modifier.playerGestures(
    context: Context,
    state: PlayerUiState,
    presenter: PlaybackPresenter,
    engine: PlaybackActivity,
    feedback: GestureFeedback,
    isFullscreen: Boolean,
    onHaptic: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onClose: () -> Unit,
): Modifier = this
    .pointerInput(isFullscreen) {
        detectTapGestures(
            onTap = {
                engine.showOverlay(!state.isOverlayShown)
                presenter.onKeyDown(-1) // reset the auto hide timer
            },
            onDoubleTap = { offset ->
                val forward = offset.x > size.width / 2
                val duration = engine.durationMs
                var target = (engine.positionMs + if (forward) SEEK_STEP_MS else -SEEK_STEP_MS).coerceAtLeast(0)
                // Stay before the end: seeking past it ends the video (and may jump to the next one)
                if (duration > 1_000) target = target.coerceAtMost(duration - 1_000)
                engine.positionMs = target
                state.positionMs = target
                presenter.onSeekEnd()
                val delta = (SEEK_STEP_MS / 1000).toInt().let { if (forward) it else -it }
                // Same direction accumulates (+10, +20...), changing direction restarts
                val sameDirection = feedback.seekSeconds != 0 && (feedback.seekSeconds > 0) == forward
                feedback.seekSeconds = if (sameDirection) feedback.seekSeconds + delta else delta
                feedback.seekEvents++
            },
            onLongPress = {
                engine.startSpeedHold(HOLD_SPEED)
                feedback.isSpeedHold = true
                onHaptic()
            },
            onPress = {
                tryAwaitRelease()
                if (feedback.isSpeedHold) {
                    engine.endSpeedHold()
                    feedback.isSpeedHold = false
                }
            },
        )
    }
    .pointerInput(isFullscreen) {
        var zone = 0 // -1 left, 0 middle, 1 right
        var total = 0f
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        var volume = 0f
        var brightness = 0.5f

        detectVerticalDragGestures(
            onDragStart = { offset ->
                total = 0f
                zone = when {
                    !isFullscreen -> 0
                    offset.x < size.width / 3f -> -1
                    offset.x > size.width * 2 / 3f -> 1
                    else -> 0
                }
                volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
                val window = (context as? Activity)?.window
                val current = window?.attributes?.screenBrightness ?: -1f
                brightness = if (current < 0) 0.5f else current
            },
            onVerticalDrag = { change, dy ->
                change.consume()
                total += dy
                // Full height swipe = full range
                val delta = -dy / size.height
                when (zone) {
                    -1 -> {
                        brightness = (brightness + delta).coerceIn(0.01f, 1f)
                        (context as? Activity)?.window?.let { window ->
                            window.attributes = window.attributes.apply { screenBrightness = brightness }
                        }
                        feedback.levelIcon = Icons.Default.BrightnessMedium
                        feedback.level = brightness
                    }
                    1 -> {
                        volume = (volume + delta).coerceIn(0f, 1f)
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVolume).toInt(), 0)
                        feedback.levelIcon = Icons.AutoMirrored.Filled.VolumeUp
                        feedback.level = volume
                    }
                }
            },
            onDragEnd = {
                feedback.levelIcon = null
                val threshold = size.height / 5f
                if (zone == 0) {
                    when {
                        isFullscreen && total > threshold -> onToggleFullscreen()
                        !isFullscreen && total < -threshold -> onToggleFullscreen()
                        !isFullscreen && total > threshold -> onClose()
                    }
                }
            },
            onDragCancel = { feedback.levelIcon = null },
        )
    }

/** Seek bubbles, "2x" pill and brightness/volume level, drawn above the video */
@Composable
fun BoxScope.GestureFeedbackOverlay(feedback: GestureFeedback) {
    // Hide the seek bubble a moment after the last double tap
    var visibleSeek by remember { mutableIntStateOf(0) }
    var side by remember { mutableIntStateOf(1) }
    LaunchedEffect(feedback.seekEvents) {
        if (feedback.seekEvents == 0) return@LaunchedEffect
        visibleSeek = feedback.seekSeconds
        side = if (visibleSeek > 0) 1 else -1
        delay(700)
        visibleSeek = 0
        feedback.seekSeconds = 0
    }

    AnimatedVisibility(visible = visibleSeek != 0, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(if (side > 0) Alignment.CenterEnd else Alignment.CenterStart)) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(if (visibleSeek > 0) Icons.Default.FastForward else Icons.Default.FastRewind, null, tint = Color.White)
            Text("${if (visibleSeek > 0) "+" else ""}$visibleSeek s", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }

    AnimatedVisibility(visible = feedback.isSpeedHold, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
        Row(
            Modifier
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("2×", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }

    val icon = feedback.levelIcon
    if (icon != null) {
        Row(
            Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Color.White)
            Box(Modifier.padding(start = 12.dp).width(120.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)) {
                Box(Modifier.fillMaxWidth(feedback.level.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White, CircleShape))
            }
        }
    }
}

@Composable
fun rememberGestureHaptic(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } }
}
