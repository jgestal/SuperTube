package com.liskovsoft.smartyoutubetv2.mobile.ui.feed

import com.liskovsoft.smartyoutubetv2.mobile.ui.components.LimitFontScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed

/** Card click callbacks shared by all feeds. Long press and the ⋮ button open the same context menu. */
class VideoActions(
    val onClick: (Video) -> Unit,
    val onLongClick: (Video) -> Unit,
    val onScrollEnd: (Video) -> Unit,
)

private val ThumbShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.videoClickable(video: Video, actions: VideoActions): Modifier {
    val haptics = LocalHapticFeedback.current
    return combinedClickable(
        onClick = { actions.onClick(video) },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.onLongClick(video)
        },
    )
}

@Composable
fun VideoItem(video: Video, actions: VideoActions, modifier: Modifier = Modifier, compact: Boolean = false) {
    when {
        video.isChannel -> ChannelCard(video, actions, modifier)
        video.isShorts -> ShortsCard(video, actions, modifier)
        else -> VideoCard(video, actions, modifier, compact)
    }
}

@Composable
fun VideoCard(video: Video, actions: VideoActions, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .videoClickable(video, actions)
            .padding(bottom = 8.dp)
    ) {
        Thumbnail(video, Modifier.fillMaxWidth())
        Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    text = video.getTitle() ?: "",
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = video.getSecondTitle()?.toString()
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Explicit affordance for the context menu (long press is not discoverable on touch screens)
            IconButton(onClick = { actions.onLongClick(video) }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.mobile_more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Thumbnail(video: Video, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(16f / 9f).clip(ThumbShape)) {
        RemoteImage(video.getCardImageUrl(), Modifier.fillMaxSize())

        val badge = when {
            video.isLive -> "LIVE"
            else -> video.badge
        }
        if (!badge.isNullOrBlank()) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (video.isLive) YouTubeRed else Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (video.hasPlaylist() && !video.hasVideo()) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                }
                LimitFontScale(1.1f) {
                    Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }

        if (video.percentWatched > 0) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.35f))) {
                Box(Modifier.fillMaxWidth((video.percentWatched / 100f).coerceIn(0f, 1f)).fillMaxSize().background(YouTubeRed))
            }
        }
    }
}

@Composable
fun ShortsCard(video: Video, actions: VideoActions, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(9f / 16f)
            .clip(ThumbShape)
            .videoClickable(video, actions)
    ) {
        RemoteImage(video.getCardImageUrl(), Modifier.fillMaxSize(), width = 360, height = 640)
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp)
        ) {
            Text(
                video.getTitle() ?: "",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ChannelCard(video: Video, actions: VideoActions, modifier: Modifier = Modifier, selected: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .videoClickable(video, actions)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            RemoteImage(
                video.getCardImageUrl(),
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
                width = 240, height = 240,
            )
            if (video.hasNewContent) {
                Box(Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
        Text(
            video.getTitle() ?: "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
