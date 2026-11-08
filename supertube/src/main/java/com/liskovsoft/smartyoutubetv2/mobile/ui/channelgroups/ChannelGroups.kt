package com.liskovsoft.smartyoutubetv2.mobile.ui.channelgroups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.channelgroups.ChannelGroupData
import com.liskovsoft.smartyoutubetv2.mobile.channelgroups.ChannelGroupFormats
import com.liskovsoft.smartyoutubetv2.mobile.channelgroups.ChannelGroupsStore
import com.liskovsoft.smartyoutubetv2.mobile.channelgroups.GroupChannel
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.BrowseActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.favorites.playContinuously
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.EmptyState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedView
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * "Channel groups" section: group chips, the channels of the selected group as an avatar strip and the latest
 * videos of those channels (RSS, like the TV groups). Groups are shared with SuperTube TV and SmartTube.
 */
@Composable
fun ChannelGroupsScreen(onLongClick: (Video) -> Unit) {
    val context = LocalContext.current
    val store = ChannelGroupsStore.instance(context)
    // The TV-style menus and profile changes also edit the groups
    LifecycleResumeEffect(Unit) {
        store.reload()
        onPauseOrDispose { }
    }

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewGroup by remember { mutableStateOf(false) }
    val groups = store.groups
    val group = groups.firstOrNull { it.id == selectedId } ?: groups.firstOrNull()

    if (group == null) {
        NoGroups(onNewGroup = { showNewGroup = true })
    } else {
        GroupBody(
            group = group,
            groups = groups,
            onSelect = { selectedId = it.id },
            onNewGroup = { showNewGroup = true },
            onDeleted = { selectedId = null },
            onLongClick = onLongClick,
        )
    }

    if (showNewGroup) {
        GroupNameDialog(stringResource(R.string.mobile_groups_new), "", exceptId = null, onDismiss = { showNewGroup = false }) { name ->
            selectedId = store.create(name)
            showNewGroup = false
        }
    }
}

@Composable
private fun NoGroups(onNewGroup: () -> Unit) {
    val activity = LocalContext.current as? BrowseActivity
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.FolderShared, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.mobile_groups_empty_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.mobile_groups_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNewGroup) {
            Icon(Icons.Outlined.CreateNewFolder, null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.mobile_groups_new))
        }
        if (activity != null) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.importChannelGroups() }) {
                Icon(Icons.Outlined.FileDownload, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.mobile_groups_import))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.mobile_groups_import_formats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )
        }
    }
}

@Composable
private fun GroupBody(
    group: ChannelGroupData,
    groups: List<ChannelGroupData>,
    onSelect: (ChannelGroupData) -> Unit,
    onNewGroup: () -> Unit,
    onDeleted: () -> Unit,
    onLongClick: (Video) -> Unit,
) {
    val context = LocalContext.current
    val store = ChannelGroupsStore.instance(context)
    val groupId = group.id ?: return
    val channelIds = group.channels.map { it.channelId }
    val feed = remember(groupId) { FeedState() }
    var videos by remember(groupId) { mutableStateOf<List<Video>>(emptyList()) }
    var showAddChannels by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<GroupChannel?>(null) }

    // Latest videos of every channel of the group, reloaded when its channels change
    DisposableEffect(groupId, channelIds) {
        feed.clear()
        videos = emptyList()
        val disposable = if (channelIds.isEmpty()) null else {
            feed.isLoading = true
            YouTubeServiceManager.instance().contentService.getRssFeedObserve(*channelIds.toTypedArray())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mediaGroup: MediaGroup ->
                    val videoGroup = VideoGroup.from(mediaGroup)
                    feed.clear()
                    feed.updateGrid(videoGroup)
                    videos = videoGroup.videos.orEmpty()
                    store.learnTitles(groupId, videos)
                }, { feed.isLoading = false }, { feed.isLoading = false })
        }
        onDispose { disposable?.dispose() }
    }

    val actions = VideoActions(
        onClick = { video -> PlaybackPresenter.instance(context).openVideo(video) },
        onLongClick = onLongClick,
        onScrollEnd = {},
    )

    val header: @Composable () -> Unit = {
        GroupHeader(
            group = group,
            groups = groups,
            hasVideos = videos.isNotEmpty(),
            onSelect = onSelect,
            onNewGroup = onNewGroup,
            onAddChannels = { showAddChannels = true },
            onRename = { showRename = true },
            onDelete = { confirmDelete = true },
            onOpenChannel = { ChannelPresenter.instance(context).openChannel(it.channelId) },
            onRemoveChannel = { removing = it },
            onPlay = { playContinuously(context, videos) },
            onShuffle = { playContinuously(context, videos, shuffle = true) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (group.channels.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) { header() }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(text = stringResource(R.string.mobile_groups_group_empty))
                        Button(onClick = { showAddChannels = true }) {
                            Icon(Icons.Outlined.PersonAdd, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.mobile_groups_add_channels))
                        }
                    }
                }
            }
        } else {
            FeedView(feed, actions, header = header)
            if (feed.isLoading) {
                if (feed.isEmpty) CircularProgressIndicator(Modifier.align(Alignment.Center))
                else LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    if (showAddChannels) AddChannelsSheet(group) { showAddChannels = false }
    if (showRename) {
        GroupNameDialog(stringResource(R.string.mobile_rename), group.title, exceptId = groupId, onDismiss = { showRename = false }) { name ->
            store.rename(groupId, name)
            showRename = false
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.mobile_groups_delete)) },
            text = { Text(stringResource(R.string.mobile_groups_delete_confirm, group.title)) },
            confirmButton = {
                TextButton(onClick = { store.delete(groupId); confirmDelete = false; onDeleted() }) {
                    Text(stringResource(R.string.mobile_groups_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.mobile_cancel)) } },
        )
    }
    removing?.let { channel ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.mobile_groups_remove_channel)) },
            text = { Text(stringResource(R.string.mobile_groups_remove_channel_confirm, channel.title ?: channel.channelId, group.title)) },
            confirmButton = {
                TextButton(onClick = { store.removeChannel(groupId, channel.channelId); removing = null }) {
                    Text(stringResource(R.string.mobile_groups_remove_channel), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.mobile_cancel)) } },
        )
    }
}

@Composable
private fun GroupHeader(
    group: ChannelGroupData,
    groups: List<ChannelGroupData>,
    hasVideos: Boolean,
    onSelect: (ChannelGroupData) -> Unit,
    onNewGroup: () -> Unit,
    onAddChannels: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenChannel: (GroupChannel) -> Unit,
    onRemoveChannel: (GroupChannel) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? BrowseActivity
    var menu by remember { mutableStateOf(false) }
    val count = group.channels.size

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(groups, key = { it.id ?: it.title }) { item ->
                FilterChip(
                    selected = item.id == group.id,
                    onClick = { onSelect(item) },
                    label = { Text("${item.title} · ${item.channels.size}") },
                    leadingIcon = { Icon(Icons.Outlined.FolderShared, null, Modifier.size(18.dp)) },
                )
            }
            item {
                FilterChip(selected = false, onClick = onNewGroup, label = { Text(stringResource(R.string.mobile_groups_new)) }, leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
            }
        }
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    context.resources.getQuantityString(R.plurals.mobile_groups_channels_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.mobile_more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_groups_add_channels)) }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) }, onClick = { menu = false; onAddChannels() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_rename)) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mobile_groups_delete)) }, onClick = { menu = false; onDelete() })
                    if (activity != null) {
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.mobile_groups_import)) }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) }, onClick = { menu = false; activity.importChannelGroups() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.mobile_groups_export)) }, leadingIcon = { Icon(Icons.Outlined.FileUpload, null) }, onClick = { menu = false; activity.exportChannelGroups() })
                    }
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
            item(key = "add") { AddBubble(onAddChannels) }
            items(group.channels, key = { it.channelId }) { channel ->
                ChannelBubble(channel, onClick = { onOpenChannel(channel) }, onLongClick = { onRemoveChannel(channel) })
            }
        }
        if (hasVideos) {
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

private val BUBBLE_WIDTH = 76.dp

@Composable
private fun AddBubble(onClick: () -> Unit) {
    Column(
        Modifier.width(BUBBLE_WIDTH).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.mobile_groups_add), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelBubble(channel: GroupChannel, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.width(BUBBLE_WIDTH).clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChannelAvatar(channel, 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            channel.title ?: "…",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/**
 * Round tonal button next to "Subscribe" (same 40 dp height): "add to collection", with a check when the channel is
 * already in a group.
 */
@Composable
fun ChannelGroupButton(inGroup: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = if (inGroup) IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) else IconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        Icon(
            if (inGroup) Icons.Outlined.LibraryAddCheck else Icons.Outlined.LibraryAdd,
            contentDescription = stringResource(R.string.mobile_groups_add_to_group),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Avatar with the initial underneath (imports from PocketTube/GrayJay don't carry the picture) */
@Composable
fun ChannelAvatar(channel: GroupChannel, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Text(
            (channel.title?.trim()?.firstOrNull() ?: '?').uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (channel.iconUrl != null) RemoteImage(channel.iconUrl, Modifier.fillMaxSize(), width = 160, height = 160)
    }
}

@Composable
private fun GroupNameDialog(title: String, initial: String, exceptId: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val store = ChannelGroupsStore.instance(LocalContext.current)
    var name by rememberSaveable { mutableStateOf(initial) }
    val taken = name.isNotBlank() && store.isNameTaken(name, exceptId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                isError = taken,
                label = { Text(stringResource(R.string.mobile_groups_name)) },
                supportingText = if (taken) ({ Text(stringResource(R.string.mobile_groups_name_taken)) }) else null,
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !taken, onClick = { onConfirm(name) }) { Text(stringResource(R.string.mobile_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.mobile_cancel)) } },
    )
}

/**
 * Picks channels for [group] from the subscriptions (search box filters them). A pasted channel link or id
 * adds that channel directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChannelsSheet(group: ChannelGroupData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = ChannelGroupsStore.instance(context)
    val groupId = group.id ?: return
    val current = store.group(groupId) ?: group
    var query by rememberSaveable { mutableStateOf("") }
    var subscriptions by remember { mutableStateOf<List<GroupChannel>?>(null) }
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        val disposable = YouTubeServiceManager.instance().contentService.subscribedChannelsByNameObserve
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mediaGroup: MediaGroup ->
                subscriptions = mediaGroup.mediaItems.orEmpty()
                    .mapNotNull { item -> item.channelId?.let { GroupChannel(it, item.title ?: item.author, item.cardImageUrl) } }
                    .distinctBy { it.channelId }
            }, { subscriptions = emptyList() }, { if (subscriptions == null) subscriptions = emptyList() })
        onDispose { disposable.dispose() }
    }

    val pastedId = ChannelGroupFormats.channelIdFromUrl(query)
    val inGroup = current.channels.map { it.channelId }.toSet()
    // Channels of the group that aren't subscriptions are listed too, so they can be unchecked here
    val all = (subscriptions.orEmpty() + current.channels.filter { c -> subscriptions.orEmpty().none { it.channelId == c.channelId } })
    val filtered = if (query.isBlank() || pastedId != null) all else all.filter { it.title?.contains(query.trim(), ignoreCase = true) == true }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Text(
                stringResource(R.string.mobile_groups_add_channels),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                current.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text(stringResource(R.string.mobile_groups_search_channels), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, stringResource(R.string.mobile_clear)) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (pastedId != null && pastedId !in inGroup) {
                    item(key = "pasted") {
                        ListItem(
                            modifier = Modifier.clickable {
                                store.addChannel(groupId, all.firstOrNull { it.channelId == pastedId } ?: GroupChannel(pastedId))
                                query = ""
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Icon(Icons.Outlined.Link, null) },
                            headlineContent = { Text(stringResource(R.string.mobile_groups_add_link)) },
                            supportingContent = { Text(pastedId, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
                when {
                    subscriptions == null -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    filtered.isEmpty() && pastedId == null -> item(key = "empty") {
                        Text(
                            stringResource(if (all.isEmpty()) R.string.mobile_groups_no_subscriptions else R.string.mobile_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                    else -> items(filtered, key = { it.channelId }) { channel ->
                        val checked = channel.channelId in inGroup
                        ListItem(
                            modifier = Modifier.clickable { store.toggle(groupId, channel) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { ChannelAvatar(channel, 40.dp) },
                            headlineContent = { Text(channel.title ?: channel.channelId, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = { Checkbox(checked = checked, onCheckedChange = { store.toggle(groupId, channel) }) },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss) { Text(stringResource(R.string.mobile_done)) }
            }
        }
    }
}

/**
 * "Add to group" sheet for one channel (channel page): a checkbox per group and a new group button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToGroupSheet(channel: GroupChannel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = ChannelGroupsStore.instance(context)
    var showNewGroup by remember { mutableStateOf(false) }
    val member = store.groupsWith(channel.channelId)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.mobile_groups_add_to_group),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChannelAvatar(channel, 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(channel.title ?: channel.channelId, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(store.groups, key = { it.id ?: it.title }) { group ->
                    val id = group.id ?: return@items
                    ListItem(
                        modifier = Modifier.clickable { store.toggle(id, channel) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Outlined.FolderShared, null) },
                        headlineContent = { Text(group.title) },
                        supportingContent = { Text(context.resources.getQuantityString(R.plurals.mobile_groups_channels_count, group.channels.size, group.channels.size)) },
                        trailingContent = { Checkbox(checked = id in member, onCheckedChange = { store.toggle(id, channel) }) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showNewGroup = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mobile_groups_new))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onDismiss) { Text(stringResource(R.string.mobile_done)) }
            }
        }
    }

    if (showNewGroup) {
        GroupNameDialog(stringResource(R.string.mobile_groups_new), "", exceptId = null, onDismiss = { showNewGroup = false }) { name ->
            store.create(name, listOf(channel))
            MessageHelpers.showMessage(context, context.getString(R.string.mobile_groups_added_to, name.trim()))
            showNewGroup = false
        }
    }
}
