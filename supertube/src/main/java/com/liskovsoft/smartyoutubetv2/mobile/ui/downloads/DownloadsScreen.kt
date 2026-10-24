package com.liskovsoft.smartyoutubetv2.mobile.ui.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadItem
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadKind
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStatus
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStore
import com.liskovsoft.smartyoutubetv2.mobile.download.Downloads
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.EmptyState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.NavigationBarSpacer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Filter { ALL, VIDEO, AUDIO }

/** Downloads management: filter by type, pause/resume/cancel the active ones, play/share/delete the finished ones */
@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val store = DownloadStore.instance(context)
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }

    val items = store.items.filter {
        when (filter) {
            Filter.ALL -> true
            Filter.VIDEO -> it.kind == DownloadKind.VIDEO
            Filter.AUDIO -> it.kind == DownloadKind.AUDIO
        }
    }
    val inProgress = items.filter { it.status != DownloadStatus.COMPLETED }
    val completed = items.filter { it.status == DownloadStatus.COMPLETED }

    // Files can be moved or deleted outside the app: check them (off the main thread)
    val exists = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(completed.map { it.id }) {
        val result = withContext(Dispatchers.IO) { completed.associate { it.id to store.fileExists(it) } }
        exists.putAll(result)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(filter == Filter.ALL, { filter = Filter.ALL }, label = { Text(stringResource(R.string.mobile_filter_all)) }) }
                item {
                    FilterChip(filter == Filter.VIDEO, { filter = Filter.VIDEO }, label = { Text(stringResource(R.string.mobile_filter_video)) },
                        leadingIcon = { Icon(Icons.Outlined.Movie, null, Modifier.size(18.dp)) })
                }
                item {
                    FilterChip(filter == Filter.AUDIO, { filter = Filter.AUDIO }, label = { Text(stringResource(R.string.mobile_filter_audio)) },
                        leadingIcon = { Icon(Icons.Outlined.MusicNote, null, Modifier.size(18.dp)) })
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    EmptyState(text = stringResource(R.string.mobile_downloads_empty))
                }
            }
        }

        if (inProgress.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.mobile_downloads_in_progress)) }
            items(inProgress, key = { it.id }) { DownloadRow(it, fileExists = true) }
        }
        if (completed.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.mobile_downloads_completed)) }
            items(completed, key = { it.id }) { DownloadRow(it, fileExists = exists[it.id] ?: true) }
        }
        item { NavigationBarSpacer() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(item: DownloadItem, fileExists: Boolean) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val playable = item.status == DownloadStatus.COMPLETED && fileExists

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = playable) { play(context, item) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
                RemoteImage(item.thumbnail ?: Downloads.thumbnailOf(item.videoId), Modifier.fillMaxSize(), width = 240, height = 135)
                Icon(
                    if (item.kind == DownloadKind.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie, null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(4.dp).size(14.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val missing = item.status == DownloadStatus.COMPLETED && !fileExists
                Text(
                    if (missing) stringResource(R.string.mobile_download_missing) else statusText(context, item),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (missing || item.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            when (item.status) {
                DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> IconButton(onClick = { Downloads.pause(context, item) }) {
                    Icon(Icons.Default.Pause, stringResource(R.string.mobile_pause))
                }
                DownloadStatus.PAUSED, DownloadStatus.FAILED -> IconButton(onClick = { Downloads.resume(context, item) }) {
                    Icon(if (item.status == DownloadStatus.FAILED) Icons.Default.Refresh else Icons.Default.Download, stringResource(R.string.mobile_resume))
                }
                DownloadStatus.PROCESSING -> CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                DownloadStatus.COMPLETED -> if (playable) IconButton(onClick = { play(context, item) }) { Icon(Icons.Default.PlayArrow, stringResource(R.string.mobile_play)) }
                else IconButton(onClick = { Downloads.retry(context, item) }) { Icon(Icons.Default.Refresh, stringResource(R.string.mobile_download_again)) }
            }
            Box {
                if (item.status == DownloadStatus.COMPLETED) {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.mobile_more_options)) }
                } else {
                    IconButton(onClick = { Downloads.remove(context, item, deleteFile = false) }) { Icon(Icons.Default.Close, stringResource(R.string.mobile_cancel)) }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (playable) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.mobile_share)) }, onClick = { menu = false; share(context, item) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.mobile_delete_file)) }, onClick = { menu = false; Downloads.remove(context, item, deleteFile = true) })
                    } else {
                        DropdownMenuItem(text = { Text(stringResource(R.string.mobile_download_again)) }, onClick = { menu = false; Downloads.retry(context, item) })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_remove_from_list)) }, onClick = { menu = false; Downloads.remove(context, item, deleteFile = false) })
                }
            }
        }
        if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.PAUSED) {
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = if (item.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun statusText(context: Context, item: DownloadItem): String {
    val size = { bytes: Long -> Formatter.formatShortFileSize(context, bytes) }
    val type = context.getString(if (item.kind == DownloadKind.AUDIO) R.string.mobile_filter_audio else R.string.mobile_filter_video)
    return when (item.status) {
        DownloadStatus.QUEUED -> context.getString(R.string.mobile_download_status_queued)
        DownloadStatus.DOWNLOADING -> "${(item.progress * 100).toInt()}% · ${size(item.downloadedBytes)}" + (if (item.totalBytes > 0) " / ${size(item.totalBytes)}" else "")
        DownloadStatus.PAUSED -> context.getString(R.string.mobile_download_status_paused) + " · ${(item.progress * 100).toInt()}%"
        DownloadStatus.PROCESSING -> context.getString(R.string.mobile_download_status_processing)
        DownloadStatus.FAILED -> context.getString(R.string.mobile_download_status_failed) + (item.error?.let { " · $it" } ?: "")
        DownloadStatus.COMPLETED -> listOfNotNull(type, item.quality, (item.outputBytes.takeIf { it > 0 } ?: item.totalBytes.takeIf { it > 0 })?.let(size)).joinToString(" · ")
    }
}

private fun play(context: Context, item: DownloadItem) {
    val uri = item.outputUri ?: return
    context.startActivity(LocalPlayerActivity.intent(context, Uri.parse(uri), item.title, item.kind == DownloadKind.AUDIO, item.thumbnail))
}

private fun share(context: Context, item: DownloadItem) {
    val uri = Uri.parse(item.outputUri ?: return)
    val intent = Intent(Intent.ACTION_SEND)
        .setType(if (item.kind == DownloadKind.AUDIO) "audio/*" else "video/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, item.title))
}

/** Floating pill shown while something is downloading. Tap opens the downloads management. */
@Composable
fun DownloadIndicator(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val store = DownloadStore.instance(LocalContext.current)
    val active = store.items.filter { it.isActive }
    if (active.isEmpty()) return
    val total = active.sumOf { it.totalBytes.coerceAtLeast(0) }
    val done = active.sumOf { it.downloadedBytes }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total else 0f },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
            }
            Text(
                "${active.size}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
