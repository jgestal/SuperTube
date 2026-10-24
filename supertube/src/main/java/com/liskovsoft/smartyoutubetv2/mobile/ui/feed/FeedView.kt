package com.liskovsoft.smartyoutubetv2.mobile.ui.feed

import com.liskovsoft.smartyoutubetv2.mobile.ui.favorites.FavoriteBadgeButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FeedLayout
import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.LimitFontScale
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed

/**
 * Any video feed (sections, search results, channels) in the layout chosen by the user.
 * All non carousel layouts share one adaptive grid with a small base cell, and every item type
 * takes the number of cells that fits it: shorts and grid cards 1, list/compact cards wider.
 */
@Composable
fun FeedView(
    feed: FeedState,
    actions: VideoActions,
    modifier: Modifier = Modifier,
    layout: FeedLayout = MobilePrefs.instance(LocalContext.current).feedLayout,
    header: (@Composable () -> Unit)? = null,
    scrollToTop: ScrollToTopRequest? = null,
) {
    if (layout == FeedLayout.CAROUSEL && feed.rows.size > 1) {
        VideoShelves(feed, actions, modifier, header = header, scrollToTop = scrollToTop)
        return
    }

    val state = rememberLazyGridState()
    ScrollToTopEffect(scrollToTop, isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }) { state.animateScrollToItem(0) }
    // A single list (grid sections) can't be a carousel: show it as a list
    val effective = if (layout == FeedLayout.CAROUSEL) FeedLayout.LIST else layout
    val showTitles = feed.rows.size > 1

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(if (effective == FeedLayout.COMPACT) 0.dp else 4.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") { header() }
        }
        val lastRow = feed.rows.lastOrNull()
        feed.rows.forEach { row ->
            val revision = row.revision
            if (showTitles && !row.title.isNullOrBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "title") {
                    Text(
                        row.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
                    )
                }
            }
            items(
                count = row.items.size,
                span = { index -> spanOf(row.items[index], row.isShorts, effective) },
                contentType = { index -> contentTypeOf(row.items[index], effective) },
            ) { index ->
                val video = row.items[index]
                if (revision >= 0) FeedItem(video, row.isShorts, effective, actions)
                // Only the last group grows while scrolling down (vertical layouts show the other ones as they are)
                if (row === lastRow && index == row.items.size - 4) {
                    LaunchedEffect(row.items.size) { row.lastVideo?.let(actions.onScrollEnd) }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { NavigationBarSpacer() }
    }
}

private fun LazyGridItemSpanScope.spanOf(video: Video, rowIsShorts: Boolean, layout: FeedLayout): GridItemSpan {
    val max = maxLineSpan
    return when {
        video.isShorts || rowIsShorts || video.isChannel -> GridItemSpan(1)
        layout == FeedLayout.GRID -> GridItemSpan(1)
        // Cards per row so that rows are always full: LIST cards need ~2 base cells, COMPACT ones ~3
        layout == FeedLayout.COMPACT -> GridItemSpan(max / maxOf(1, max / 3))
        else -> GridItemSpan(max / maxOf(1, max / 2))
    }
}

private fun contentTypeOf(video: Video, layout: FeedLayout): Int = when {
    video.isShorts -> 1
    video.isChannel -> 2
    else -> 10 + layout.ordinal
}

@Composable
private fun FeedItem(video: Video, rowIsShorts: Boolean, layout: FeedLayout, actions: VideoActions) {
    when {
        video.isChannel -> ChannelCard(video, actions)
        video.isShorts || rowIsShorts -> ShortsCard(video, actions)
        layout == FeedLayout.COMPACT -> CompactVideoCard(video, actions)
        layout == FeedLayout.GRID -> VideoCard(video, actions, compact = true)
        else -> VideoCard(video, actions)
    }
}

/** Thumbnail on the left, title and info on the right (dense list) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactVideoCard(video: Video, actions: VideoActions, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = { actions.onClick(video) }, onLongClick = { actions.onLongClick(video) })
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(168.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
            RemoteImage(video.getCardImageUrl(), Modifier.fillMaxSize(), width = 336, height = 189)
            FavoriteBadgeButton(video, Modifier.align(Alignment.TopEnd))
            val badge = if (video.isLive) "LIVE" else video.badge
            if (!badge.isNullOrBlank()) {
                LimitFontScale(1.1f) {
                    Text(
                        badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(if (video.isLive) YouTubeRed else Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            if (video.percentWatched > 0) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.35f))) {
                    Box(Modifier.fillMaxWidth((video.percentWatched / 100f).coerceIn(0f, 1f)).fillMaxSize().background(YouTubeRed))
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(video.getTitle() ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                video.getSecondTitle()?.toString() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(onClick = { actions.onLongClick(video) }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.mobile_more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private class LayoutOption(val layout: FeedLayout, val labelRes: Int, val icon: ImageVector)

private val LAYOUT_OPTIONS = listOf(
    LayoutOption(FeedLayout.LIST, R.string.mobile_layout_list, Icons.Outlined.ViewAgenda),
    LayoutOption(FeedLayout.COMPACT, R.string.mobile_layout_compact, Icons.AutoMirrored.Outlined.ViewList),
    LayoutOption(FeedLayout.GRID, R.string.mobile_layout_grid, Icons.Outlined.GridView),
    LayoutOption(FeedLayout.CAROUSEL, R.string.mobile_layout_carousel, Icons.Outlined.ViewCarousel),
)

/** Top bar action to switch the feed layout (remembered between sessions) */
@Composable
fun FeedLayoutButton() {
    val prefs = MobilePrefs.instance(LocalContext.current)
    var expanded by remember { mutableStateOf(false) }
    val current = LAYOUT_OPTIONS.first { it.layout == prefs.feedLayout }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(current.icon, contentDescription = stringResource(R.string.mobile_layout))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LAYOUT_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    leadingIcon = { Icon(option.icon, null) },
                    trailingIcon = { if (option.layout == prefs.feedLayout) Icon(Icons.Default.Check, null) },
                    onClick = {
                        prefs.updateFeedLayout(option.layout)
                        expanded = false
                    },
                )
            }
        }
    }
}
