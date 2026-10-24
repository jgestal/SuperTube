package com.liskovsoft.smartyoutubetv2.mobile

import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView
import com.liskovsoft.smartyoutubetv2.common.prefs.NetworkData
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.BrowseActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.channel.ChannelActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.channel.ChannelUploadsActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialog.AppDialogActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.playback.PlaybackActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.search.SearchActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.signin.AddDeviceActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.signin.SignInActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.splash.SplashActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.web.WebBrowserActivity
import org.conscrypt.Conscrypt
import java.security.Security

/**
 * Same business logic as the TV app (all presenters live in the 'common' module).
 * Only the views are different: every View interface is backed by a touch-first Compose activity.
 */
class MobileApplication : MultiDexApplication(), Configuration.Provider {
    companion object {
        init {
            // Same networking tweak as the TV app (less buffering on streams)
            System.setProperty("http.keepAlive", "false")
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the native Conscrypt provider BEFORE reading any configs (see TV MainApplication)
        val conscryptProvider = try { Conscrypt.newProvider() } catch (e: Throwable) { null }

        if (conscryptProvider != null && NetworkData.instance(this).isConscryptEnabled) {
            try {
                Security.insertProviderAt(conscryptProvider, 1)
            } catch (e: Throwable) {
                // UnsatisfiedLinkError
            }
        }

        setupGlobalExceptionHandler()
        setupViewManager()
    }

    // Lazy WorkManager init (auto-init is removed in the manifest)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    private fun setupViewManager() {
        val viewManager = ViewManager.instance(this)

        viewManager.setRoot(BrowseActivity::class.java)
        viewManager.register(SplashView::class.java, SplashActivity::class.java) // root activity
        viewManager.register(BrowseView::class.java, BrowseActivity::class.java) // root activity
        viewManager.register(PlaybackView::class.java, PlaybackActivity::class.java, BrowseActivity::class.java)
        viewManager.register(AppDialogView::class.java, AppDialogActivity::class.java, BrowseActivity::class.java)
        viewManager.register(SearchView::class.java, SearchActivity::class.java, BrowseActivity::class.java)
        viewManager.register(SignInView::class.java, SignInActivity::class.java, BrowseActivity::class.java)
        viewManager.register(AddDeviceView::class.java, AddDeviceActivity::class.java, BrowseActivity::class.java)
        viewManager.register(ChannelView::class.java, ChannelActivity::class.java, BrowseActivity::class.java)
        viewManager.register(ChannelUploadsView::class.java, ChannelUploadsActivity::class.java, BrowseActivity::class.java)
        viewManager.register(WebBrowserView::class.java, WebBrowserActivity::class.java, BrowseActivity::class.java)
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler() ?: return

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (shouldIgnore(e)) {
                MessageHelpers.showLongMessage(this, e.message)
            } else {
                defaultHandler.uncaughtException(t, e)
            }
        }
    }

    private fun shouldIgnore(e: Throwable): Boolean {
        return Helpers.containsAny(e.message, "ListenableFuture", "Missing android.support.FILE_PROVIDER_PATHS meta-data") ||
                e.javaClass.name.startsWith("org.chromium")
    }
}
