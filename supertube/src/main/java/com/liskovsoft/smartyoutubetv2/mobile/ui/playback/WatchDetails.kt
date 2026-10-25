package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.smartyoutubetv2.common.R as CommonR
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.download.Downloads
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.LimitFontScale
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialog.ChatContent
import com.liskovsoft.smartyoutubetv2.mobile.ui.downloads.DownloadOptionsSheet
import com.liskovsoft.smartyoutubetv2.mobile.ui.favorites.FavoriteChipState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.CompactVideoCard
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoItem
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoRowState
import kotlinx.coroutines.delay

// Portrait details under the video: title, channel, action chips and suggestions

/** Portrait: title, channel, actions, live chat and suggestions */
@Composable
internal fun WatchDetails(state: PlayerUiState, presenter: PlaybackPresenter, actions: VideoActions) {
    val rows = state.suggestions.rows
    // The biggest group (related videos) is a vertical list. Others (playlist, chapters) are shelves.
    val mainRow = rows.maxByOrNull { it.items.size }
    val revision = state.revision

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(contentType = "header") {
            if (revision >= 0) VideoHeader(state, presenter)
        }
        state.chatReceiver?.let { receiver ->
            item(contentType = "chat") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    ChatContent(receiver)
                }
            }
        }
        // Groups titled like the main one are continuations ("Suggestions" pages): one vertical list
        val listRows = rows.filter { mainRow != null && (it === mainRow || it.title == mainRow.title) }
        rows.filter { it !in listRows }.forEach { row ->
            item(key = "shelf${row.id}", contentType = "shelf") { SuggestionShelf(row, actions) }
        }
        if (mainRow != null) {
            item(contentType = "title") {
                Text(
                    mainRow.title ?: stringResource(R.string.mobile_up_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            listRows.forEach { row ->
                itemsIndexed(row.items, contentType = { _, _ -> "video" }) { index, video ->
                    if (row.revision >= 0) CompactVideoCard(video, actions, Modifier.padding(horizontal = 12.dp))
                    if (row === listRows.last() && index == row.items.size - 4) LaunchedEffect(row.items.size) { actions.onScrollEnd(row.items.last()) }
                }
            }
        }
        item(contentType = "spacer") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun VideoHeader(state: PlayerUiState, presenter: PlaybackPresenter) {
    val video = state.video
    val infoId = CommonR.id.action_info

    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(horizontal = 16.dp).clickable { presenter.onButtonClicked(infoId, state.buttonState(infoId)) }) {
            Text(state.title ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val subtitle = state.subtitle
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Channel row
        val channelId = CommonR.id.action_channel
        val subscribeId = CommonR.id.action_subscribe
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).clickable { presenter.onButtonClicked(channelId, state.buttonState(channelId)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(state.channelIconUrl ?: video?.getCardImageUrl()?.takeIf { video.isChannel }, Modifier.size(40.dp).clip(CircleShape), width = 128, height = 128)
                Spacer(Modifier.width(12.dp))
                Text(video?.getAuthor() ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val subscribed = state.buttonState(subscribeId) == PlayerUI.BUTTON_ON
            val label = stringResource(if (subscribed) R.string.mobile_subscribed else R.string.mobile_subscribe)
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
        PlayerAction(CommonR.id.action_chat, stringResource(CommonR.string.open_comments), Icons.AutoMirrored.Outlined.Comment),
        PlayerAction(CommonR.id.action_share, stringResource(CommonR.string.share_link), Icons.Outlined.Share),
        PlayerAction(CommonR.id.action_playlist_add, stringResource(CommonR.string.dialog_add_to_playlist), Icons.AutoMirrored.Outlined.PlaylistAdd),
        PlayerAction(CommonR.id.action_video_speed, stringResource(CommonR.string.video_speed), Icons.Outlined.Speed),
        PlayerAction(CommonR.id.action_repeat, stringResource(CommonR.string.action_repeat_mode), Icons.Outlined.Repeat),
        PlayerAction(CommonR.id.action_pip, stringResource(CommonR.string.action_pip), Icons.Outlined.PictureInPictureAlt),
        PlayerAction(CommonR.id.action_playback_queue, stringResource(CommonR.string.action_playback_queue), Icons.AutoMirrored.Outlined.QueueMusic),
    )

    val (isFavorite, onFavorite) = FavoriteChipState(state.video)

    // A dozen chips at most: a scrollable Row avoids the lazy layout overhead
    LimitFontScale(1.15f) { Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = isFavorite,
            onClick = onFavorite,
            label = { Text(stringResource(R.string.mobile_favorite), maxLines = 1) },
            leadingIcon = { Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, Modifier.size(FilterChipDefaults.IconSize)) },
            shape = RoundedCornerShape(18.dp),
        )
        DownloadChip(state.video)
        SleepTimerChip()
        actions.forEach { action ->
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
            itemsIndexed(row.items, contentType = { _, video -> video.isShorts }) { index, video ->
                VideoItem(video, actions, Modifier.width(if (video.isShorts) 140.dp else 240.dp), compact = true)
                if (index == row.items.size - 3) LaunchedEffect(row.items.size) { actions.onScrollEnd(row.items.last()) }
            }
        }
    }
}

@Composable
internal fun FullscreenSuggestions(state: PlayerUiState, actions: VideoActions, modifier: Modifier) {
    val row = state.suggestions.rows.maxByOrNull { it.items.size } ?: return
    LazyRow(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 72.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(row.items, contentType = { "video" }) { video ->
            Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.6f)) {
                VideoItem(video, actions, Modifier.width(220.dp).padding(4.dp), compact = true)
            }
        }
    }
}

/** "Download" chip: loads the available qualities and shows them in a sheet */
@Composable
private fun DownloadChip(video: Video?) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<Pair<MediaItemFormatInfo, List<Downloads.Option>>?>(null) }
    FilterChip(
        selected = false,
        enabled = video?.videoId != null && video.isLive != true && !loading,
        onClick = {
            val target = video ?: return@FilterChip
            loading = true
            Downloads.loadOptions(context, target) { info, list -> options = info to list; loading = false }
        },
        label = { Text(stringResource(R.string.mobile_download), maxLines = 1) },
        leadingIcon = {
            if (loading) CircularProgressIndicator(Modifier.size(FilterChipDefaults.IconSize), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.Download, null, Modifier.size(FilterChipDefaults.IconSize))
        },
        shape = RoundedCornerShape(18.dp),
    )
    // The request may fail silently (toast shown by Downloads): don't keep the spinner forever
    LaunchedEffect(loading) { if (loading) { kotlinx.coroutines.delay(15_000); loading = false } }

    val current = options
    if (current != null && video != null) {
        DownloadOptionsSheet(
            options = current.second,
            onPick = { option -> Downloads.download(context, video, current.first, option); options = null },
            onDismiss = { options = null },
        )
    }
}
