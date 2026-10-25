package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ClosedCaptionOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.R as CommonR
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed

// Player overlay: top bar (speed, subtitles, quality), transport buttons and seek bar

@Composable
internal fun PlayerControls(
    state: PlayerUiState,
    presenter: PlaybackPresenter,
    engine: PlaybackActivity,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    isFullscreen: Boolean,
    gestures: Modifier,
) {
    val insets = if (isFullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier

    // The dimmed layer keeps the same gestures: tap hides the controls, double tap seeks...
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).then(gestures)) {
        // Top bar
        Row(insets.fillMaxWidth().align(Alignment.TopCenter).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(if (isFullscreen) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.KeyboardArrowDown, stringResource(R.string.mobile_back), tint = Color.White)
            }
            Text(
                if (isFullscreen) state.title ?: "" else "",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SpeedButton(state, presenter, engine)
            val ccId = CommonR.id.lb_control_closed_captioning
            IconButton(onClick = { presenter.onButtonClicked(ccId, state.buttonState(ccId)) }) {
                Icon(if (state.buttonState(ccId) == PlayerUI.BUTTON_ON) Icons.Default.ClosedCaption else Icons.Outlined.ClosedCaptionOff, null, tint = Color.White)
            }
            val hqId = CommonR.id.lb_control_high_quality
            IconButton(onClick = { presenter.onButtonClicked(hqId, state.buttonState(hqId)) }) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.mobile_quality), tint = Color.White)
            }
            if (isFullscreen && !state.suggestions.isEmpty) {
                IconButton(onClick = { engine.showSuggestions(!state.isSuggestionsShown) }) {
                    Icon(Icons.Outlined.VideoLibrary, stringResource(R.string.mobile_up_next), tint = Color.White)
                }
            }
        }

        // Transport
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { presenter.onPreviousClicked() }) {
                Icon(Icons.Default.SkipPrevious, stringResource(R.string.mobile_previous), tint = Color.White, modifier = Modifier.size(36.dp))
            }
            if (!state.isBuffering) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                    IconButton(
                        modifier = Modifier.size(64.dp),
                        onClick = {
                            if (state.isPlaying) {
                                presenter.onPauseClicked(); engine.playWhenReady = false
                            } else {
                                presenter.onPlayClicked(); engine.playWhenReady = true
                            }
                            presenter.onKeyDown(-1)
                        },
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            stringResource(if (state.isPlaying) R.string.mobile_pause else R.string.mobile_play),
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
            IconButton(onClick = { presenter.onNextClicked() }) {
                Icon(Icons.Default.SkipNext, stringResource(R.string.mobile_next), tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Seek bar
        Column(insets.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLive = state.video?.isLive == true
                Text(
                    if (isLive) "LIVE" else "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                    color = if (isLive) YouTubeRed else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp).weight(1f),
                )
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        stringResource(if (isFullscreen) R.string.mobile_exit_fullscreen else R.string.mobile_fullscreen),
                        tint = Color.White,
                    )
                }
            }
            SeekBar(state, presenter, engine)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(state: PlayerUiState, presenter: PlaybackPresenter, engine: PlaybackActivity) {
    val duration = state.durationMs.coerceAtLeast(1)
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val value = dragValue ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    val interaction = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = YouTubeRed,
        activeTrackColor = YouTubeRed,
        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
    )

    Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
        // SponsorBlock and other segments
        if (state.segments.isNotEmpty()) {
            // The slider track is inset by half the thumb width (14 dp thumb)
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 7.dp).height(4.dp)) {
                state.segments.forEach { segment ->
                    val start = segment.startProgress.coerceIn(0f, 1f) * size.width
                    val end = segment.endProgress.coerceIn(0f, 1f) * size.width
                    drawRect(Color(segment.color), topLeft = Offset(start, 0f), size = Size((end - start).coerceAtLeast(2f), size.height))
                }
            }
        }
        Slider(
            value = value,
            onValueChange = {
                dragValue = it
                presenter.onKeyDown(-1)
            },
            onValueChangeFinished = {
                val target = ((dragValue ?: value) * duration).toLong()
                presenter.onSeekPositionChanged(target)
                engine.positionMs = target
                // Don't wait for the next poll: the thumb would jump back to the old position for a moment
                state.positionMs = target
                presenter.onSeekEnd()
                dragValue = null
            },
            colors = colors,
            interactionSource = interaction,
            // Thin YouTube-like seek bar with a small round thumb
            thumb = {
                SliderDefaults.Thumb(interactionSource = interaction, colors = colors, thumbSize = DpSize(14.dp, 14.dp))
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = colors,
                    modifier = Modifier.height(3.dp),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                )
            },
        )
    }
}

private val QUICK_SPEEDS = listOf(1f, 1.5f, 2f)

/** Speed pill of the top bar: tap cycles 1× → 1.5× → 2×, long press opens the full speed list */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedButton(state: PlayerUiState, presenter: PlaybackPresenter, engine: PlaybackActivity) {
    val speedId = CommonR.id.action_video_speed
    val speed = state.speed
    val isNormal = speed == 1f
    val label = remember(speed) {
        java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(speed) + "×"
    }
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNormal) Color.Transparent else Color.White.copy(alpha = 0.2f))
            .combinedClickable(
                onClickLabel = stringResource(CommonR.string.video_speed),
                onClick = {
                    val next = QUICK_SPEEDS.getOrNull(QUICK_SPEEDS.indexOf(speed) + 1) ?: QUICK_SPEEDS.first()
                    engine.speed = next
                    presenter.onKeyDown(-1) // reset the auto hide timer
                },
                onLongClick = { presenter.onButtonLongClicked(speedId, state.buttonState(speedId)) },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
