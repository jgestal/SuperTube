package com.liskovsoft.smartyoutubetv2.mobile.ui.favorites

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LocalContentColor
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoriteFolder
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoritesStore
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.EmptyState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedView
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.YouTubeRed

/**
 * Plays the videos one after another through the shared playback queue (the player continues with the next one
 * until the end of the list). [shuffle] randomizes the order.
 */
fun playContinuously(context: Context, videos: List<Video>, startIndex: Int = 0, shuffle: Boolean = false) {
    if (videos.isEmpty()) return
    val ordered = if (shuffle) videos.shuffled() else videos.drop(startIndex.coerceIn(0, videos.lastIndex))
    val queue = Playlist.instance()
    queue.clear()
    queue.addAll(ordered)
    val first = ordered.first()
    queue.setCurrent(first)
    first.fromQueue = true
    PlaybackPresenter.instance(context).openVideo(first)
}

/** Heart drawn over a thumbnail. Filled when the video is in any folder. */
@Composable
fun FavoriteBadgeButton(video: Video, modifier: Modifier = Modifier) {
    if (video.videoId == null) return
    val store = FavoritesStore.instance(LocalContext.current)
    var showSheet by remember { mutableStateOf(false) }
    val isFavorite = store.isFavorite(video.videoId)

    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = modifier.padding(6.dp).size(36.dp),
        onClick = { showSheet = true },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.mobile_save_to_favorites),
                tint = if (isFavorite) YouTubeRed else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (showSheet) FavoriteFoldersSheet(video) { showSheet = false }
}

/** Chip used in the player actions row */
@Composable
fun FavoriteChipState(video: Video?): Pair<Boolean, () -> Unit> {
    val store = FavoritesStore.instance(LocalContext.current)
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet && video != null) FavoriteFoldersSheet(video) { showSheet = false }
    return store.isFavorite(video?.videoId) to { showSheet = true }
}

/**
 * "Save to…" sheet: the video title, one checkbox per folder (the general "Favorites" folder first),
 * a new folder button and, when the video is already saved, "Remove from favorites".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteFoldersSheet(video: Video, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = FavoritesStore.instance(context)
    var showNewFolder by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.mobile_save_in),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                video.getTitleFull() ?: video.getTitle() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
            )
            store.folders.forEachIndexed { index, folder ->
                val checked = store.isInFolder(video.videoId, folder.id)
                ListItem(
                    modifier = Modifier.clickable { store.toggle(video, folder.id) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Icon(
                            if (index == 0) Icons.Filled.Favorite else Icons.Outlined.Folder, null,
                            tint = if (index == 0) YouTubeRed else LocalContentColor.current,
                        )
                    },
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text(context.resources.getQuantityString(R.plurals.mobile_videos_count, folder.videos.size, folder.videos.size)) },
                    trailingContent = { Checkbox(checked = checked, onCheckedChange = { store.toggle(video, folder.id) }) },
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showNewFolder = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mobile_new_folder))
                }
                Spacer(Modifier.weight(1f))
                if (store.isFavorite(video.videoId)) {
                    TextButton(
                        onClick = { video.videoId?.let(store::removeEverywhere); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.mobile_remove_favorite)) }
                }
                Button(onClick = onDismiss) { Text(stringResource(R.string.mobile_done)) }
            }
        }
    }

    if (showNewFolder) {
        FolderNameDialog(stringResource(R.string.mobile_new_folder), "", onDismiss = { showNewFolder = false }) { name ->
            val folder = store.createFolder(name)
            store.toggle(video, folder.id)
            MessageHelpers.showMessage(context, context.getString(R.string.mobile_saved_in, folder.name))
            showNewFolder = false
        }
    }
}

@Composable
private fun FolderNameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.mobile_folder_name)) })
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(stringResource(R.string.mobile_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.mobile_cancel)) } },
    )
}

/** "Favorites" section: folders as chips, play all / shuffle, and the videos in the chosen layout */
@Composable
fun FavoritesScreen(onLongClick: (Video) -> Unit) {
    val context = LocalContext.current
    val store = FavoritesStore.instance(context)
    var selectedId by rememberSaveable { mutableStateOf(store.folders.first().id) }
    val folder = store.folders.firstOrNull { it.id == selectedId } ?: store.folders.first()
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    // Search looks into every folder (title, channel and info line)
    val source = if (searching) {
        store.folders.flatMap { it.videos }.distinctBy { it.videoId }.filter { fav ->
            listOfNotNull(fav.title, fav.author, fav.secondTitle).any { it.contains(query.trim(), ignoreCase = true) }
        }
    } else {
        folder.videos
    }
    val videos = remember(source) { source.map { it.toVideo() } }
    var confirmDelete by remember { mutableStateOf(false) }
    val feed = remember(videos) { FeedState().apply { if (videos.isNotEmpty()) updateGrid(VideoGroup.from(videos)) } }
    var showNewFolder by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val actions = VideoActions(
        // Tapping a video plays the folder from that video on
        onClick = { video -> playContinuously(context, videos, videos.indexOfFirst { it.videoId == video.videoId }) },
        onLongClick = onLongClick,
        onScrollEnd = {},
    )

    val header: @Composable () -> Unit = {
        FolderHeader(
            query = query,
            onQueryChange = { query = it },
            resultsCount = if (searching) videos.size else null,
            folders = store.folders,
            selected = folder,
            onSelect = { selectedId = it.id },
            onNewFolder = { showNewFolder = true },
            onPlay = { playContinuously(context, videos) },
            onShuffle = { playContinuously(context, videos, shuffle = true) },
            onRename = { showRename = true },
            onDelete = { confirmDelete = true },
        )
    }

    if (videos.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            // Same insets as the header inside the feed grid (FeedView content padding)
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) { header() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(text = stringResource(if (searching) R.string.mobile_no_results else R.string.mobile_favorites_empty))
            }
        }
    } else {
        FeedView(feed, actions, header = header)
    }

    if (showNewFolder) {
        FolderNameDialog(stringResource(R.string.mobile_new_folder), "", onDismiss = { showNewFolder = false }) { name ->
            selectedId = store.createFolder(name).id
            showNewFolder = false
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.mobile_delete_folder)) },
            text = { Text(stringResource(R.string.mobile_delete_folder_confirm, folder.name)) },
            confirmButton = {
                TextButton(onClick = { store.deleteFolder(folder.id); selectedId = store.folders.first().id; confirmDelete = false }) {
                    Text(stringResource(R.string.mobile_delete_folder), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.mobile_cancel)) } },
        )
    }
    if (showRename) {
        FolderNameDialog(stringResource(R.string.mobile_rename), folder.name, onDismiss = { showRename = false }) { name ->
            store.renameFolder(folder.id, name)
            showRename = false
        }
    }
}

@Composable
private fun FolderHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    resultsCount: Int?,
    folders: List<FavoriteFolder>,
    selected: FavoriteFolder,
    onSelect: (FavoriteFolder) -> Unit,
    onNewFolder: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val count = resultsCount ?: selected.videos.size

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text(stringResource(R.string.mobile_search_favorites)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.mobile_clear)) }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        if (resultsCount == null) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(folders, key = { it.id }) { folder ->
                FilterChip(
                    selected = folder.id == selected.id,
                    onClick = { onSelect(folder) },
                    label = { Text("${folder.name} · ${folder.videos.size}") },
                    leadingIcon = { Icon(Icons.Outlined.Folder, null, Modifier.size(18.dp)) },
                )
            }
            item {
                FilterChip(selected = false, onClick = onNewFolder, label = { Text(stringResource(R.string.mobile_new_folder)) }, leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
            }
        }
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (resultsCount != null) stringResource(R.string.mobile_results) else selected.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    context.resources.getQuantityString(R.plurals.mobile_videos_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (resultsCount == null) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.mobile_more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_rename)) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_delete_folder)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        if (count > 0) {
            // Compact pill buttons (Material 3 small size) instead of half-screen wide ones
            Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay, contentPadding = ButtonDefaults.ButtonWithIconContentPadding, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.mobile_play_all), maxLines = 1)
                }
                FilledTonalButton(onClick = onShuffle, contentPadding = ButtonDefaults.ButtonWithIconContentPadding, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Default.Shuffle, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.mobile_shuffle), maxLines = 1)
                }
            }
        }
    }
}
