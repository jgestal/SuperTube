package com.liskovsoft.smartyoutubetv2.mobile.ui.browse

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

    // Back: secondary section -> "More" list -> first destination -> exit
    BackHandler(enabled = inSecondarySection) { onShowMore() }
    BackHandler(enabled = !inSecondarySection && primaries.isNotEmpty() && currentDestination != primaries.first().id) {
        onSelectSection(primaries.first().id)
    }

    val actions = VideoActions(
        onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
        onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
        onScrollEnd = { presenter.onScrollEnd(it) },
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            primaries.forEach { section ->
                val isSelected = currentDestination == section.id
                item(
                    selected = isSelected,
                    onClick = { if (isSelected) presenter.refresh() else onSelectSection(section.id) },
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
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (state.isMoreListShown || selected == null) {
                    MoreList(
                        sections = state.secondarySections,
                        onClick = { onSelectSection(it.id) },
                        onLongClick = { presenter.onSectionLongPressed(it.id) },
                    )
                } else {
                    SectionBody(state.contentOf(selected), state.isLoading, actions, presenter)
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
        FitText(text, style = MaterialTheme.typography.labelMedium, maxFontSize = MaterialTheme.typography.labelMedium.fontSize, minFontSize = 9.sp)
    }
}

@Composable
private fun SectionIconView(section: BrowseSection, selected: Boolean) {
    if (section.iconUrl != null) {
        // Pinned channels/playlists
        RemoteImage(section.iconUrl, Modifier.size(24.dp).clip(CircleShape), width = 96, height = 96)
    } else {
        Icon(sectionIcon(section.id, selected), contentDescription = null)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoreList(sections: List<BrowseSection>, onClick: (BrowseSection) -> Unit, onLongClick: (BrowseSection) -> Unit) {
    // Settings at the end, separated from the content sections
    val (settings, content) = sections.partition { it.id == MediaGroup.TYPE_SETTINGS }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(content, key = { it.id }) { section ->
            ListItem(
                modifier = Modifier.combinedClickable(onClick = { onClick(section) }, onLongClick = { onLongClick(section) }),
                headlineContent = { Text(section.title ?: "") },
                leadingContent = { SectionIconView(section, false) },
            )
        }
        if (settings.isNotEmpty()) {
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            items(settings, key = { it.id }) { section ->
                ListItem(
                    modifier = Modifier.combinedClickable(onClick = { onClick(section) }, onLongClick = { onLongClick(section) }),
                    headlineContent = { Text(section.title ?: "") },
                    leadingContent = { SectionIconView(section, false) },
                )
            }
        }
    }
}

@Composable
private fun SectionBody(content: SectionContent, isLoading: Boolean, actions: VideoActions, presenter: BrowsePresenter) {
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
            BrowseSection.TYPE_ROW -> VideoShelves(content.feed, actions)
            BrowseSection.TYPE_MULTI_GRID -> {
                val uploads = content.feed.rows.firstOrNull()
                val header: @Composable () -> Unit = {
                    ChannelsStrip(content, presenter)
                }
                if (uploads != null) {
                    VideoGrid(uploads, actions, header = header)
                } else {
                    Column { header() }
                }
            }
            else -> content.feed.rows.firstOrNull()?.let { VideoGrid(it, actions) }
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items.size) { index ->
            val item = items[index]
            ListItem(
                modifier = Modifier.combinedClickableCompat { item.onClick?.run() },
                headlineContent = { Text(item.title ?: "") },
                leadingContent = {
                    if (item.imageResId > 0) {
                        Icon(painterResource(item.imageResId), contentDescription = null, modifier = Modifier.size(24.dp))
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
