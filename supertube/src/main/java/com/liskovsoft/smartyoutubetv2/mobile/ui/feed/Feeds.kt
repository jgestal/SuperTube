package com.liskovsoft.smartyoutubetv2.mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.mobile.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Load the next page a few items before reaching the end */
private const val PREFETCH_DISTANCE = 6

/**
 * Adaptive grid: 1 column on phones in portrait, 2-4 columns on landscape, foldables and tablets.
 * (Android adaptive guideline: LazyVerticalGrid + GridCells.Adaptive instead of LazyColumn)
 */
@Composable
fun VideoGrid(
    row: VideoRowState,
    actions: VideoActions,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: (@Composable () -> Unit)? = null,
) {
    val minCell: Dp = if (row.isShorts) 150.dp else 320.dp
    val revision = row.revision

    EndReachedEffect(state = { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }, total = { state.layoutInfo.totalItemsCount }) {
        row.lastVideo?.let(actions.onScrollEnd)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCell),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp + contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            end = 12.dp + contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            top = 8.dp + contentPadding.calculateTopPadding(),
            bottom = 16.dp + contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(if (row.isShorts) 12.dp else 4.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        itemsIndexed(row.items, contentType = { _, video -> if (video.isShorts) 1 else 0 }) { _, video ->
            // Read revision so in-place updates (watched progress, titles) re-render the card
            if (revision >= 0) VideoItem(video, actions)
        }
        item(span = { GridItemSpan(maxLineSpan) }) { NavigationBarSpacer() }
    }
}

/** Vertical list of horizontal shelves (YouTube style home, channel and playlists pages) */
@Composable
fun VideoShelves(
    feed: FeedState,
    actions: VideoActions,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLastRowReached: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    if (onLastRowReached != null) {
        EndReachedEffect(state = { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }, total = { state.layoutInfo.totalItemsCount }, distance = 2) {
            onLastRowReached()
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp + contentPadding.calculateTopPadding(), bottom = 16.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (header != null) {
            item { header() }
        }
        items(feed.rows, key = { it.id }) { row ->
            Shelf(row, actions)
        }
        item { NavigationBarSpacer() }
    }
}

@Composable
private fun Shelf(row: VideoRowState, actions: VideoActions) {
    val state = rememberLazyListState()
    val revision = row.revision

    EndReachedEffect(state = { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }, total = { state.layoutInfo.totalItemsCount }) {
        row.lastVideo?.let(actions.onScrollEnd)
    }

    Column {
        val title = row.title
        if (!title.isNullOrBlank()) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(row.items, contentType = { _, video -> if (video.isShorts) 1 else 0 }) { _, video ->
                val width = when {
                    video.isChannel -> 104.dp
                    video.isShorts || row.isShorts -> 150.dp
                    else -> 264.dp
                }
                if (revision >= 0) VideoItem(video, actions, Modifier.width(width), compact = true)
            }
        }
    }
}

/** Keeps the last items above the gesture bar (respects insets already consumed by a bottom navigation bar) */
@Composable
fun NavigationBarSpacer() {
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
private fun EndReachedEffect(state: () -> Int, total: () -> Int, distance: Int = PREFETCH_DISTANCE, onEndReached: () -> Unit) {
    val reached by remember {
        derivedStateOf {
            val count = total()
            count > 0 && state() >= count - 1 - distance
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { reached to total() }
            .distinctUntilChanged()
            .filter { it.first }
            .collect { onEndReached() }
    }
}

/** Pull to refresh + loading/empty states around any feed */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContainer(
    isLoading: Boolean,
    isEmpty: Boolean,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val body: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            content()
            if (isEmpty && isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (isEmpty) {
                EmptyState(Modifier.align(Alignment.Center))
            } else if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    if (onRefresh != null) {
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) { body() }
    } else {
        Box(modifier.fillMaxSize()) { body() }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, text: String = stringResource(R.string.mobile_nothing_here)) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.VideoLibrary, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
