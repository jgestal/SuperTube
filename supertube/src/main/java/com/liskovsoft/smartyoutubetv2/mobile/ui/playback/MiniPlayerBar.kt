package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.download.Downloads
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed
import kotlinx.coroutines.delay

private data class MiniState(val title: String, val author: String?, val thumbnail: String?, val playing: Boolean, val progress: Float)

/**
 * Collapsed player shown while browsing: the player keeps running in its own task (engine blocked),
 * this bar mirrors it. Tap expands it again, ✕ stops the playback.
 */
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mini by remember { mutableStateOf<MiniState?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val presenter = PlaybackPresenter.instance(context)
            val view = presenter.view
            val video = presenter.video
            mini = if (view != null && video != null && view.isEngineBlocked && view.isEngineInitialized && !view.isInPIPMode) {
                val duration = view.durationMs
                MiniState(
                    title = video.getTitleFull() ?: video.getTitle() ?: "",
                    author = video.getAuthor(),
                    thumbnail = video.getCardImageUrl() ?: video.videoId?.let(Downloads::thumbnailOf),
                    playing = view.isPlaying,
                    progress = if (duration > 0) (view.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                )
            } else {
                null
            }
            delay(500)
        }
    }

    AnimatedVisibility(visible = mini != null, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = modifier) {
        val state = mini ?: return@AnimatedVisibility
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.clickable { ViewManager.instance(context).startView(PlaybackView::class.java) }) {
                // Thin progress line on top (like YouTube)
                Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
                    Box(Modifier.fillMaxWidth(state.progress).fillMaxHeight().background(YouTubeRed))
                }
                Row(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(state.thumbnail, Modifier.width(96.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)), width = 192, height = 108)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(state.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!state.author.isNullOrBlank()) {
                            Text(state.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = {
                        val presenter = PlaybackPresenter.instance(context)
                        val view = presenter.view ?: return@IconButton
                        if (view.isPlaying) presenter.onPauseClicked() else presenter.onPlayClicked()
                        view.playWhenReady = !view.isPlaying
                        mini = state.copy(playing = !state.playing)
                    }) {
                        Icon(
                            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            stringResource(if (state.playing) R.string.mobile_pause else R.string.mobile_play),
                        )
                    }
                    IconButton(onClick = {
                        PlaybackPresenter.instance(context).forceFinish()
                        mini = null
                    }) {
                        Icon(Icons.Default.Close, stringResource(R.string.mobile_close_player))
                    }
                }
            }
        }
    }
}
