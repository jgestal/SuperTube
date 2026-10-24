package com.liskovsoft.smartyoutubetv2.mobile.ui.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import com.liskovsoft.smartyoutubetv2.common.app.presenters.WebBrowserPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.ScreenScaffold
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

class WebBrowserActivity : ScreenActivity(), WebBrowserView {
    private lateinit var presenter: WebBrowserPresenter
    private var webView: WebView? = null
    private var pendingUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = WebBrowserPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                ScreenScaffold(title = "", onBack = { if (webView?.canGoBack() == true) webView?.goBack() else finish() }) {
                    AndroidView(factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            webView = this
                            pendingUrl?.let { loadUrl(it) }
                            pendingUrl = null
                        }
                    })
                }
            }
        }

        presenter.onViewInitialized()
    }

    override fun loadUrl(url: String) {
        val view = webView
        if (view != null) view.loadUrl(url) else pendingUrl = url
    }
}
