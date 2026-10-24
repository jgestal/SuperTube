package com.liskovsoft.smartyoutubetv2.mobile.ui.channel

import android.os.Bundle
import androidx.activity.compose.setContent
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.ScreenScaffold
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedContainer
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoGrid
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

/** Grid of videos of a channel or playlist */
class ChannelUploadsActivity : ScreenActivity(), ChannelUploadsView {
    private lateinit var presenter: ChannelUploadsPresenter
    private val feed = FeedState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChannelUploadsPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                val actions = VideoActions(
                    onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
                    onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
                    onScrollEnd = { presenter.onScrollEnd(it) },
                )
                val channel = presenter.channel
                ScreenScaffold(title = channel?.getTitle() ?: channel?.getAuthor() ?: "", onBack = { finish() }) {
                    FeedContainer(isLoading = feed.isLoading, isEmpty = feed.isEmpty, onRefresh = { presenter.refresh() }) {
                        feed.rows.firstOrNull()?.let { VideoGrid(it, actions) }
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
