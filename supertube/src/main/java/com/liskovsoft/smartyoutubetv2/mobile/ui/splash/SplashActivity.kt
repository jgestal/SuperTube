package com.liskovsoft.smartyoutubetv2.mobile.ui.splash

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

        presenter = SplashPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntent = intent
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
