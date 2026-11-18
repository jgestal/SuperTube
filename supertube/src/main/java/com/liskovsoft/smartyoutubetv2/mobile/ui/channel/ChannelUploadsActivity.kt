package com.liskovsoft.smartyoutubetv2.mobile.ui.channel

import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedLayoutButton
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.liskovsoft.smartyoutubetv2.common.R as CommonR
import com.liskovsoft.smartyoutubetv2.common.app.models.data.WatchLaterOrder
import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.ScreenScaffold
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedContainer
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoGrid
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SuperTubeTheme

/** Grid of videos of a channel or playlist */
class ChannelUploadsActivity : ScreenActivity(), ChannelUploadsView {
    private lateinit var presenter: ChannelUploadsPresenter
    private val feed = FeedState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChannelUploadsPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SuperTubeTheme {
                val actions = VideoActions(
                    onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
                    onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
                    onScrollEnd = { presenter.onScrollEnd(it) },
                )
                val channel = presenter.channel
                ScreenScaffold(title = channel?.getTitle() ?: channel?.getAuthor() ?: "", onBack = { finish() }, actions = {
                    if (WatchLaterOrder.isWatchLater(channel)) WatchLaterOrderButton { presenter.refresh() }
                    FeedLayoutButton()
                }) {
                    FeedContainer(isLoading = feed.isLoading, isEmpty = feed.isEmpty, onRefresh = { presenter.refresh() }) {
                        FeedView(feed, actions)
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

    override fun isWatchLaterNewestFirst(): Boolean? = MobilePrefs.instance(this).watchLaterNewestFirst

    override fun update(videoGroup: VideoGroup) {
        feed.updateGrid(videoGroup)
    }

    override fun showProgressBar(show: Boolean) {
        feed.isLoading = show
    }

    override fun clear() {
        feed.clear()
    }
}

/** Watch later: last saved first (default) or YouTube's order, oldest first */
@Composable
private fun WatchLaterOrderButton(onChanged: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = MobilePrefs.instance(context)
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(CommonR.string.playlist_order))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                true to CommonR.string.playlist_order_added_date_newer_first,
                false to CommonR.string.playlist_order_added_date_older_first,
            ).forEach { (newestFirst, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    trailingIcon = { if (prefs.watchLaterNewestFirst == newestFirst) Icon(Icons.Default.Check, null) },
                    onClick = {
                        expanded = false
                        if (prefs.watchLaterNewestFirst != newestFirst) {
                            prefs.updateWatchLaterNewestFirst(newestFirst)
                            onChanged()
                        }
                    },
                )
            }
        }
    }
}
