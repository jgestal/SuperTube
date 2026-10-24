package com.liskovsoft.smartyoutubetv2.mobile.ui.browse

import com.liskovsoft.smartyoutubetv2.mobile.ui.playback.MiniPlayerBar
import com.liskovsoft.smartyoutubetv2.mobile.BuildConfig
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.liskovsoft.smartyoutubetv2.mobile.ui.downloads.DownloadIndicator
import com.liskovsoft.smartyoutubetv2.mobile.ui.downloads.DownloadsScreen
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStore
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.ScrollToTopRequest
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.liskovsoft.smartyoutubetv2.mobile.ui.favorites.FavoritesScreen
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoritesStore
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedLayoutButton
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedView
import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.filled.Check
import com.liskovsoft.smartyoutubetv2.common.R as CommonR
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Language
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.FitText
import androidx.compose.ui.unit.sp
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.LimitFontScale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.ChannelCard
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedContainer
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoGrid
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoShelves

private const val MORE_DESTINATION = Int.MIN_VALUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    state: BrowseUiState,
    presenter: BrowsePresenter,
    onSelectSection: (Int) -> Unit,
    onShowMore: () -> Unit,
) {
    val context = LocalContext.current
    val selected = state.selectedSection
    val primaries = state.primarySections
    val currentDestination = when {
        state.isMoreListShown -> MORE_DESTINATION
        selected != null && state.isPrimary(selected.id) -> selected.id
        selected != null -> MORE_DESTINATION
        else -> primaries.firstOrNull()?.id ?: MORE_DESTINATION
    }
    val inSecondarySection = !state.isMoreListShown && selected != null && !state.isPrimary(selected.id)
    val prefs = MobilePrefs.instance(context)
    var isReordering by rememberSaveable { mutableStateOf(false) }
    // Re-tap on the current tab: scroll to top (or refresh when already at the top)
    var retaps by remember { mutableIntStateOf(0) }
    val scrollToTop = ScrollToTopRequest(retaps) { presenter.refresh() }
    if (!state.isMoreListShown && isReordering) isReordering = false
    val showsVideos = !state.isMoreListShown && selected != null && selected.type != BrowseSection.TYPE_SETTINGS_GRID && selected.id != DownloadStore.SECTION_ID

    // Back: reorder mode -> secondary section -> "More" list -> first destination -> exit
    BackHandler(enabled = isReordering) { isReordering = false }
    BackHandler(enabled = inSecondarySection) { onShowMore() }
    BackHandler(enabled = !inSecondarySection && primaries.isNotEmpty() && currentDestination != primaries.first().id) {
        onSelectSection(primaries.first().id)
    }

    val actions = VideoActions(
        onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
        onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
        onScrollEnd = { presenter.onScrollEnd(it) },
    )

    // Rail when the height is compact (phone in landscape) or the width isn't (tablets, foldables). Bar otherwise.
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val navigationType = if (sizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT || sizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    NavigationSuiteScaffold(
        layoutType = navigationType,
        navigationSuiteItems = {
            primaries.forEach { section ->
                val isSelected = currentDestination == section.id
                item(
                    selected = isSelected,
                    onClick = { if (isSelected) retaps++ else onSelectSection(section.id) },
                    icon = { SectionIconView(section, isSelected) },
                    label = { NavLabel(section.title ?: "") },
                )
            }
            item(
                selected = currentDestination == MORE_DESTINATION,
                onClick = onShowMore,
                icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                label = { NavLabel(stringResource(R.string.mobile_more)) },
            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        if (inSecondarySection) {
                            IconButton(onClick = onShowMore) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mobile_back))
                            }
                        }
                    },
                    title = {
                        val title = when {
                            state.isMoreListShown -> stringResource(R.string.mobile_more)
                            inSecondarySection -> selected?.title ?: ""
                            else -> stringResource(R.string.mobile_app_name)
                        }
                        Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
                    },
                    actions = {
                        if (state.isMoreListShown) {
                            IconButton(onClick = { isReordering = !isReordering }) {
                                if (isReordering) {
                                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.mobile_done))
                                } else {
                                    Icon(Icons.Outlined.SwapVert, contentDescription = stringResource(R.string.mobile_reorder))
                                }
                            }
                        }
                        if (showsVideos) FeedLayoutButton()
                        IconButton(onClick = { SearchPresenter.instance(context).startSearch(null) }) {
                            Icon(Icons.Outlined.Search, contentDescription = stringResource(com.liskovsoft.smartyoutubetv2.common.R.string.action_search))
                        }
                        IconButton(onClick = { AccountSettingsPresenter.instance(context).show() }) {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                        }
                    },
                )
            },
        ) { padding ->
            // Landscape: keep the content away from the system navigation buttons / camera cutout
            // (the rail already handles the start side)
            val sides = if (navigationType == NavigationSuiteType.NavigationRail) WindowInsetsSides.End else WindowInsetsSides.Horizontal
            Box(Modifier.padding(padding).windowInsetsPadding(WindowInsets.safeDrawing.only(sides)).fillMaxSize()) {
                if (state.isMoreListShown || selected == null) {
                    MoreList(
                        sections = orderSections(state.secondarySections, prefs.sectionOrder),
                        isReordering = isReordering,
                        onClick = { onSelectSection(it.id) },
                        onLongClick = { presenter.onSectionLongPressed(it.id) },
                        // Keep the position of the sections that aren't listed right now (disabled, signed out...)
                        onOrderChanged = { ids -> prefs.setSectionOrder(ids + prefs.sectionOrder.filter { it !in ids }) },
                    )
                } else {
                    SectionBody(state.contentOf(selected), state.isLoading, actions, presenter, scrollToTop)
                }
                // Bottom overlays: downloads progress pill and the mini player
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    if (selected?.id != DownloadStore.SECTION_ID) {
                        DownloadIndicator(onClick = { onSelectSection(DownloadStore.SECTION_ID) }, modifier = Modifier.padding(16.dp))
                    }
                    MiniPlayerBar()
                }
            }
        }
    }
}

/** Bottom bar labels: one line, limited growth with the system font size (Material spec: 12sp label) */
@Composable
private fun NavLabel(text: String) {
    // Long translations ("Suscripciones") shrink a little instead of being cut
    LimitFontScale(1.1f) {
        // Rail items are narrow: cap the width so the text shrinks instead of overflowing
        Box(Modifier.widthIn(max = 76.dp)) {
            FitText(text, style = MaterialTheme.typography.labelMedium, maxFontSize = MaterialTheme.typography.labelMedium.fontSize, minFontSize = 9.sp)
        }
    }
}

@Composable
internal fun SectionIconView(section: BrowseSection, selected: Boolean) {
    if (section.iconUrl != null) {
        // Pinned channels/playlists
        RemoteImage(section.iconUrl, Modifier.size(24.dp).clip(CircleShape), width = 96, height = 96)
    } else {
        Icon(sectionIcon(section.id, selected), contentDescription = null)
    }
}

@Composable
private fun SectionBody(content: SectionContent, isLoading: Boolean, actions: VideoActions, presenter: BrowsePresenter, scrollToTop: ScrollToTopRequest) {
    if (content.section.id == FavoritesStore.SECTION_ID) {
        FavoritesScreen(onLongClick = actions.onLongClick)
        return
    }
    if (content.section.id == DownloadStore.SECTION_ID) {
        DownloadsScreen()
        return
    }

    val error = content.error
    if (error != null) {
        ErrorView(error.message, error.actionText) { error.onAction() }
        return
    }

    val section = content.section
    FeedContainer(
        isLoading = isLoading,
        isEmpty = content.isEmpty,
        onRefresh = if (section.type == BrowseSection.TYPE_SETTINGS_GRID) null else ({ presenter.refresh() }),
    ) {
        when (section.type) {
            BrowseSection.TYPE_SETTINGS_GRID -> SettingsList(content.settings)
            BrowseSection.TYPE_MULTI_GRID -> FeedView(content.feed, actions, header = { ChannelsStrip(content, presenter) }, scrollToTop = scrollToTop)
            else -> FeedView(content.feed, actions, scrollToTop = scrollToTop)
        }
    }
}

/** Subscribed channels as a horizontal avatar strip. Tap loads uploads below, long press opens the menu. */
@Composable
private fun ChannelsStrip(content: SectionContent, presenter: BrowsePresenter) {
    val channels = content.channels.rows.firstOrNull()?.items ?: return
    val channelActions = VideoActions(
        onClick = { channel: Video ->
            content.selectedChannel = channel
            content.feed.clear()
            presenter.onVideoItemSelected(channel)
        },
        onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
        onScrollEnd = { presenter.onScrollEnd(it) },
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(channels.size) { index ->
            val channel = channels[index]
            ChannelCard(channel, channelActions, Modifier.width(96.dp), selected = channel == content.selectedChannel)
        }
    }
}

@Composable
private fun SettingsList(items: List<SettingsItem>) {
    val activity = LocalContext.current as? BrowseActivity
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        // SuperTube-only options: backup of favorites/settings and updates from the SuperTube releases
        if (activity != null) {
            item {
                Text(stringResource(R.string.mobile_supertube), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
            }
            item { ListItem(modifier = Modifier.clickable { activity.checkForUpdates() }, headlineContent = { Text(stringResource(R.string.mobile_check_updates)) }, supportingContent = { Text(BuildConfig.VERSION_NAME) }, leadingContent = { Icon(Icons.Outlined.SystemUpdate, null) }) }
            item { ListItem(modifier = Modifier.clickable { activity.exportData() }, headlineContent = { Text(stringResource(R.string.mobile_export)) }, leadingContent = { Icon(Icons.Outlined.FileUpload, null) }) }
            item { ListItem(modifier = Modifier.clickable { activity.importData() }, headlineContent = { Text(stringResource(R.string.mobile_import)) }, leadingContent = { Icon(Icons.Outlined.FileDownload, null) }) }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }
        items(items.size) { index ->
            val item = items[index]
            ListItem(
                modifier = Modifier.combinedClickableCompat { item.onClick?.run() },
                headlineContent = { Text(item.title ?: "") },
                leadingContent = {
                    val vector = settingsIcon(item.imageResId)
                    when {
                        vector != null -> Icon(vector, contentDescription = null)
                        item.imageResId > 0 -> Icon(painterResource(item.imageResId), contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier = this.then(Modifier.combinedClickable(onClick = onClick))

@Composable
fun ErrorView(message: String?, actionText: String?, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            message ?: "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (!actionText.isNullOrBlank()) {
            FilledTonalButton(onClick = onAction) { Text(actionText) }
        }
    }
}

/** Material Symbols instead of the TV settings artwork (consistent outlined style) */
private fun settingsIcon(resId: Int): androidx.compose.ui.graphics.vector.ImageVector? = when (resId) {
    CommonR.drawable.settings_account -> Icons.Outlined.AccountCircle
    CommonR.drawable.settings_language -> Icons.Outlined.Language
    CommonR.drawable.settings_app -> Icons.Outlined.Tune
    CommonR.drawable.settings_main_ui -> Icons.Outlined.Dashboard
    CommonR.drawable.settings_player -> Icons.Outlined.SmartDisplay
    CommonR.drawable.settings_subtitles -> Icons.Outlined.ClosedCaption
    CommonR.drawable.settings_search -> Icons.Outlined.Search
    CommonR.drawable.settings_block -> Icons.Outlined.Shield
    CommonR.drawable.settings_dearrow -> Icons.Outlined.TextFields
    CommonR.drawable.settings_backup -> Icons.Outlined.Backup
    CommonR.drawable.settings_about -> Icons.Outlined.Info
    else -> null
}
