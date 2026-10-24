package com.liskovsoft.smartyoutubetv2.mobile.ui.splash

import com.liskovsoft.smartyoutubetv2.mobile.widget.FavoritesWidgetProvider
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.BrowseActivity
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStore
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadService
import android.content.Intent
import android.os.Bundle
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity

/** Entry point: handles launcher and YouTube links/shares through the shared SplashPresenter */
class SplashActivity : MobileActivity(), SplashView {
    private var newIntent: Intent? = null
    private lateinit var presenter: SplashPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newIntent = intent
        if (intent?.getBooleanExtra(DownloadService.EXTRA_OPEN_DOWNLOADS, false) == true) BrowseActivity.requestSection(DownloadStore.SECTION_ID)
        intent?.getIntExtra(FavoritesWidgetProvider.EXTRA_OPEN_SECTION, 0)?.takeIf { it != 0 }?.let(BrowseActivity::requestSection)

        presenter = SplashPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntent = intent
        if (intent.getBooleanExtra(DownloadService.EXTRA_OPEN_DOWNLOADS, false)) BrowseActivity.requestSection(DownloadStore.SECTION_ID)
        intent.getIntExtra(FavoritesWidgetProvider.EXTRA_OPEN_SECTION, 0).takeIf { it != 0 }?.let(BrowseActivity::requestSection)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun getNewIntent(): Intent? = newIntent

    override fun finishView() {
        try {
            finish()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}
