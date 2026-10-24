package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import com.liskovsoft.smartyoutubetv2.mobile.ui.components.LimitFontScale
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ClosedCaptionOff
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView as ExoPlayerView
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialog.ChatContent
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoItem
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoRowState
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed
import kotlinx.coroutines.delay
import com.liskovsoft.smartyoutubetv2.common.R as CommonR

private const val SEEK_STEP_MS = 10_000L

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
            delay(500)
        }
    }

    val suggestionActions = VideoActions(
        onClick = { presenter.onSuggestionItemClicked(it) },
        onLongClick = { presenter.onSuggestionItemLongClicked(it) },
        onScrollEnd = { presenter.onScrollEnd(it) },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val isFullscreen = maxWidth > maxHeight || state.isInPip

        // Back in fullscreen returns to the portrait layout first
        BackHandler(enabled = isFullscreen && !state.isInPip) { onToggleFullscreen() }

        if (isFullscreen) {
            PlayerArea(state, presenter, engine, onCreateExoView, onToggleFullscreen, onBack, isFullscreen = true, Modifier.fillMaxSize())
            if (state.isSuggestionsShown && state.isOverlayShown && !state.isInPip) {
                FullscreenSuggestions(state, suggestionActions, Modifier.align(Alignment.BottomCenter))
            }
        } else {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).background(Color.Black).fillMaxWidth())
                // Vertical videos get more room, but never more than a square
                val aspect = state.videoAspect.coerceIn(1f, 16f / 9f)
                PlayerArea(state, presenter, engine, onCreateExoView, onToggleFullscreen, onBack, isFullscreen = false, Modifier.fillMaxWidth().aspectRatio(aspect))
                WatchDetails(state, presenter, suggestionActions)
            }
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
    Box(modifier.background(Color.Black)) {
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

        // Gestures: tap toggles the controls, double tap on the sides seeks
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            engine.showOverlay(!state.isOverlayShown)
                            presenter.onKeyDown(-1) // reset the auto hide timer
                        },
                        onDoubleTap = { offset ->
                            val forward = offset.x > size.width / 2
                            val target = (engine.positionMs + if (forward) SEEK_STEP_MS else -SEEK_STEP_MS).coerceAtLeast(0)
                            engine.positionMs = target
                            presenter.onSeekEnd()
                        },
                    )
                }
        )

        if (state.isBuffering || state.isProgressShown) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }

        AnimatedVisibility(visible = state.isOverlayShown && !state.isInPip, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            PlayerControls(state, presenter, engine, onToggleFullscreen, onBack, isFullscreen)
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    presenter: PlaybackPresenter,
    engine: PlaybackActivity,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    isFullscreen: Boolean,
) {
    val insets = if (isFullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
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

@Composable
private fun SeekBar(state: PlayerUiState, presenter: PlaybackPresenter, engine: PlaybackActivity) {
    val duration = state.durationMs.coerceAtLeast(1)
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val value = dragValue ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
        // SponsorBlock and other segments
        if (state.segments.isNotEmpty()) {
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(4.dp)) {
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
                presenter.onSeekEnd()
                dragValue = null
            },
            colors = SliderDefaults.colors(
                thumbColor = YouTubeRed,
                activeTrackColor = YouTubeRed,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
    }
}

/** Portrait: title, channel, actions, live chat and suggestions */
@Composable
private fun WatchDetails(state: PlayerUiState, presenter: PlaybackPresenter, actions: VideoActions) {
    val rows = state.suggestions.rows
    // The biggest group (related videos) is a vertical list. Others (playlist, chapters) are shelves.
    val mainRow = rows.maxByOrNull { it.items.size }
    val revision = state.revision

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            if (revision >= 0) VideoHeader(state, presenter)
        }
        state.chatReceiver?.let { receiver ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    ChatContent(receiver)
                }
            }
        }
        rows.filter { it !== mainRow }.forEach { row ->
            item(key = "shelf${row.id}") { SuggestionShelf(row, actions) }
        }
        if (mainRow != null) {
            item {
                Text(
                    mainRow.title ?: stringResource(R.string.mobile_up_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(mainRow.items) { index, video ->
                if (mainRow.revision >= 0) CompactSuggestion(video, actions)
                if (index == mainRow.items.size - 4) LaunchedEffect(mainRow.items.size) { actions.onScrollEnd(mainRow.items.last()) }
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun VideoHeader(state: PlayerUiState, presenter: PlaybackPresenter) {
    val video = state.video
    val infoId = CommonR.id.action_info

    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.clickable { presenter.onButtonClicked(infoId, state.buttonState(infoId)) }) {
            Text(state.title ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val subtitle = state.subtitle
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Channel row
        val channelId = CommonR.id.action_channel
        val subscribeId = CommonR.id.action_subscribe
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).clickable { presenter.onButtonClicked(channelId, state.buttonState(channelId)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(state.channelIconUrl ?: video?.getCardImageUrl()?.takeIf { video.isChannel }, Modifier.size(40.dp).clip(CircleShape), width = 128, height = 128)
                Spacer(Modifier.width(12.dp))
                Text(video?.getAuthor() ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val subscribed = state.buttonState(subscribeId) == PlayerUI.BUTTON_ON
            val label = stringResource(if (subscribed) CommonR.string.unsubscribe_from_channel else CommonR.string.subscribe_to_channel)
            if (subscribed) {
                FilledTonalButton(onClick = { presenter.onButtonClicked(subscribeId, state.buttonState(subscribeId)) }) { Text(label, maxLines = 1) }
            } else {
                Button(onClick = { presenter.onButtonClicked(subscribeId, state.buttonState(subscribeId)) }) { Text(label, maxLines = 1) }
            }
        }

        ActionChips(state, presenter)
    }
}

private class PlayerAction(val id: Int, val label: String, val icon: ImageVector, val activeIcon: ImageVector = icon)

@Composable
private fun ActionChips(state: PlayerUiState, presenter: PlaybackPresenter) {
    val actions = listOf(
        PlayerAction(CommonR.id.action_thumbs_up, stringResource(CommonR.string.action_like), Icons.Outlined.ThumbUp, Icons.Filled.ThumbUp),
        PlayerAction(CommonR.id.action_thumbs_down, stringResource(CommonR.string.action_dislike), Icons.Outlined.ThumbDown, Icons.Filled.ThumbDown),
        PlayerAction(CommonR.id.action_chat, stringResource(CommonR.string.open_comments), Icons.Outlined.Comment),
        PlayerAction(CommonR.id.action_share, stringResource(CommonR.string.share_link), Icons.Outlined.Share),
        PlayerAction(CommonR.id.action_playlist_add, stringResource(CommonR.string.dialog_add_to_playlist), Icons.AutoMirrored.Outlined.PlaylistAdd),
        PlayerAction(CommonR.id.action_video_speed, stringResource(CommonR.string.video_speed), Icons.Outlined.Speed),
        PlayerAction(CommonR.id.action_repeat, stringResource(CommonR.string.action_repeat_mode), Icons.Outlined.Repeat),
        PlayerAction(CommonR.id.action_pip, stringResource(CommonR.string.action_pip), Icons.Outlined.PictureInPictureAlt),
        PlayerAction(CommonR.id.action_playback_queue, stringResource(CommonR.string.action_playback_queue), Icons.AutoMirrored.Outlined.QueueMusic),
    )

    LimitFontScale(1.15f) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(actions) { action ->
            val buttonState = state.buttonState(action.id)
            val active = buttonState == PlayerUI.BUTTON_ON
            FilterChip(
                selected = active,
                onClick = { presenter.onButtonClicked(action.id, buttonState) },
                label = { Text(action.label, maxLines = 1) },
                leadingIcon = { Icon(if (active) action.activeIcon else action.icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
                shape = RoundedCornerShape(18.dp),
            )
        }
    } }
}

@Composable
private fun SuggestionShelf(row: VideoRowState, actions: VideoActions) {
    Column(Modifier.padding(top = 8.dp)) {
        if (!row.title.isNullOrBlank()) {
            Text(row.title ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(row.items) { index, video ->
                VideoItem(video, actions, Modifier.width(if (video.isShorts) 140.dp else 240.dp), compact = true)
                if (index == row.items.size - 3) LaunchedEffect(row.items.size) { actions.onScrollEnd(row.items.last()) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSuggestion(video: Video, actions: VideoActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { actions.onClick(video) }, onLongClick = { actions.onLongClick(video) })
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(168.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
            RemoteImage(video.getCardImageUrl(), Modifier.fillMaxSize(), width = 336, height = 189)
            val badge = if (video.isLive) "LIVE" else video.badge
            if (!badge.isNullOrBlank()) {
                Text(
                    badge,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(if (video.isLive) YouTubeRed else Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(video.getTitle() ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.getSecondTitle()?.toString() ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        IconButton(onClick = { actions.onLongClick(video) }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.mobile_more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FullscreenSuggestions(state: PlayerUiState, actions: VideoActions, modifier: Modifier) {
    val row = state.suggestions.rows.maxByOrNull { it.items.size } ?: return
    LazyRow(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 72.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(row.items) { video ->
            Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.6f)) {
                VideoItem(video, actions, Modifier.width(220.dp).padding(4.dp), compact = true)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
