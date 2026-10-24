package com.liskovsoft.smartyoutubetv2.mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * All the groups of a feed in one adaptive grid with a header per group (search results).
 * Shorts groups get their own narrower cells.
 */
@Composable
fun MultiRowGrid(
    feed: FeedState,
    actions: VideoActions,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val reached by remember {
        derivedStateOf {
            val total = state.layoutInfo.totalItemsCount
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 7
        }
    }
    LaunchedEffect(feed) {
        snapshotFlow { reached to state.layoutInfo.totalItemsCount }
            .distinctUntilChanged()
            .filter { it.first }
            .collect { feed.rows.lastOrNull()?.lastVideo?.let(actions.onScrollEnd) }
    }

    // Shorts are 2x narrower: use a small base cell and let regular videos span 2 cells
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val showHeaders = feed.rows.size > 1
        feed.rows.forEach { row ->
            val revision = row.revision
            if (showHeaders && !row.title.isNullOrBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
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
                span = { index ->
                    val video = row.items[index]
                    if (video.isShorts || row.isShorts || maxLineSpan < 2) GridItemSpan(1) else GridItemSpan(2)
                },
                contentType = { index -> if (row.items[index].isShorts) 1 else 0 },
            ) { index ->
                if (revision >= 0) VideoItem(row.items[index], actions)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { NavigationBarSpacer() }
    }
}
