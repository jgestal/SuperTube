package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView as ExoPlayerView
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import kotlinx.coroutines.delay

@Composable
fun PlaybackScreen(
    state: PlayerUiState,
    presenter: PlaybackPresenter,
    engine: PlaybackActivity,
    onCreateExoView: (ExoPlayerView) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        while (true) {
            engine.pollEngine()
            // Smoother seek bar while the controls are visible
            delay(if (state.isOverlayShown) 250 else 500)
        }
    }

    val suggestionActions = remember(presenter) {
        VideoActions(
            onClick = { presenter.onSuggestionItemClicked(it) },
            onLongClick = { presenter.onSuggestionItemLongClicked(it) },
            onScrollEnd = { presenter.onScrollEnd(it) },
        )
    }

    // Moved (not recreated) between the portrait, fullscreen and PIP layouts: recreating the ExoPlayer view
    // swaps the video surface (black flash, stall) and loses subtitles visibility, zoom and resize mode.
    val playerArea = remember {
        movableContentOf { isFullscreen: Boolean, modifier: Modifier ->
            PlayerArea(state, presenter, engine, onCreateExoView, onToggleFullscreen, onBack, isFullscreen, modifier)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val isFullscreen = maxWidth > maxHeight || state.isInPip

        // Back in fullscreen returns to the portrait layout first
        BackHandler(enabled = isFullscreen && !state.isInPip) { onToggleFullscreen() }
        // Portrait: back collapses the player into the mini player (like YouTube)
        BackHandler(enabled = !isFullscreen && !state.isInPip) { onBack() }

        if (isFullscreen) {
            playerArea(true, Modifier.fillMaxSize())
            if (state.isSuggestionsShown && state.isOverlayShown && !state.isInPip) {
                FullscreenSuggestions(state, suggestionActions, Modifier.align(Alignment.BottomCenter))
            }
        } else {
            // Surface (not a plain background) so the text gets the right content color
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).background(Color.Black).fillMaxWidth())
                // Vertical videos get more room, but never more than a square
                val aspect = state.videoAspect.coerceIn(1f, 16f / 9f)
                playerArea(false, Modifier.fillMaxWidth().aspectRatio(aspect))
                WatchDetails(state, presenter, suggestionActions)
            } }
        }
    }
}

@Composable
private fun PlayerArea(
    state: PlayerUiState,
    presenter: PlaybackPresenter,
    engine: PlaybackActivity,
    onCreateExoView: (ExoPlayerView) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    isFullscreen: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val feedback = remember { GestureFeedback() }
    val haptic = rememberGestureHaptic()
    val gestures = Modifier.playerGestures(context, state, presenter, engine, feedback, isFullscreen, haptic, onToggleFullscreen, onClose = onBack)

    // Keep the screen on while a video is playing
    val view = LocalView.current
    DisposableEffect(state.isPlaying) {
        view.keepScreenOn = state.isPlaying
        onDispose { view.keepScreenOn = false }
    }

    // A single tap waits for a possible double tap before toggling the controls: keep that wait short
    val baseConfiguration = LocalViewConfiguration.current
    val viewConfiguration = remember(baseConfiguration) {
        object : ViewConfiguration by baseConfiguration {
            override val doubleTapTimeoutMillis: Long = DOUBLE_TAP_TIMEOUT_MS
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) { Box(modifier.background(Color.Black)) {
        state.backgroundUrl?.let { RemoteImage(it, Modifier.fillMaxSize(), width = 1280, height = 720) }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                ExoPlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    setKeepContentOnPlayerReset(true)
                    onCreateExoView(this)
                }
            },
        )

        // Tap, double tap, long press and vertical swipes (see PlayerGestures.kt)
        Box(Modifier.fillMaxSize().then(gestures))

        if (state.isBuffering || state.isProgressShown) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }

        AnimatedVisibility(visible = state.isOverlayShown && !state.isInPip, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            PlayerControls(state, presenter, engine, onToggleFullscreen, onBack, isFullscreen, gestures)
        }

        GestureFeedbackOverlay(feedback)
    } }
}

/** System default: 300 ms. Still comfortable for double taps, but the controls react sooner to single taps. */
private const val DOUBLE_TAP_TIMEOUT_MS = 200L
