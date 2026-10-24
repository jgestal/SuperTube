package com.liskovsoft.smartyoutubetv2.mobile.ui.channel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.ScreenScaffold
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedContainer
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoShelves
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

/** Channel page: header (avatar, name, subscribers, in-channel search) + shelves */
class ChannelActivity : ScreenActivity(), ChannelView {
    private lateinit var presenter: ChannelPresenter
    private val feed = FeedState()
    private var revision by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChannelPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                val actions = VideoActions(
                    onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
                    onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
                    onScrollEnd = { presenter.onScrollEnd(it) },
                )
                ScreenScaffold(title = channelTitle(revision), onBack = { finish() }) {
                    FeedContainer(isLoading = feed.isLoading, isEmpty = feed.isEmpty, onRefresh = null) {
                        VideoShelves(feed, actions, header = { ChannelHeader(revision) })
                    }
                }
            }
        }

        presenter.onViewInitialized()
    }

    override fun onResume() {
        super.onResume()
        presenter.setView(this)
        presenter.onViewResumed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun finish() {
        presenter.onFinish()
        super.finish()
    }

    private fun channelTitle(@Suppress("UNUSED_PARAMETER") revision: Int): String {
        val channel = presenter.channel
        return channel?.getAuthor() ?: channel?.getTitle() ?: presenter.channelId?.takeIf { Helpers.startsWith(it, "@") } ?: ""
    }

    @Composable
    private fun ChannelHeader(revision: Int) {
        val channel = presenter.channel
        var query by rememberSaveable { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(channel?.getCardImageUrl(), Modifier.size(64.dp).clip(CircleShape), width = 256, height = 256)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(channelTitle(revision), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subs = channel?.subscriberCount
                    if (!subs.isNullOrBlank()) {
                        Text(subs, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text(stringResource(com.liskovsoft.smartyoutubetv2.common.R.string.action_search)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) presenter.onSearchSubmit(query)
                        focusManager.clearFocus()
                    }),
                )
                IconButton(onClick = { presenter.onSearchSettingsClicked() }) {
                    Icon(Icons.Outlined.Sort, contentDescription = stringResource(com.liskovsoft.smartyoutubetv2.common.R.string.search_sorting))
                }
            }
        }
    }

    // ChannelView

    override fun update(videoGroup: VideoGroup) {
        feed.updateRows(videoGroup)
        revision++ // channel info could be updated together with the content
    }

    override fun setPosition(index: Int) {
        // Touch UI: scrolling is up to the user
    }

    override fun showProgressBar(show: Boolean) {
        feed.isLoading = show
    }

    override fun clear() {
        feed.clear()
    }
}
