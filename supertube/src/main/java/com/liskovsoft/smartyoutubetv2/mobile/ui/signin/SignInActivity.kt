package com.liskovsoft.smartyoutubetv2.mobile.ui.signin

import com.liskovsoft.smartyoutubetv2.mobile.ui.components.FitText
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AddDevicePresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.ScreenScaffold
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

/** Device code flow screen shared by "sign in" and "link device" */
abstract class CodeActivity : ScreenActivity() {
    protected var code by mutableStateOf<String?>(null)
    protected var url by mutableStateOf<String?>(null)
    protected var fullUrl by mutableStateOf<String?>(null)

    abstract val screenTitle: String
    open val description: String get() = getString(R.string.mobile_sign_in_steps)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartTubeTheme {
                ScreenScaffold(title = screenTitle, onBack = { finish() }) {
                    CodeScreen(code, url, fullUrl ?: url)
                }
            }
        }
    }

    @Composable
    private fun CodeScreen(code: String?, url: String?, openUrl: String?) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 480.dp))
            if (url != null) {
                SelectionContainer {
                    Text(url, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                if (code == null) {
                    CircularProgressIndicator(Modifier.padding(32.dp))
                } else {
                    SelectionContainer {
                        FitText(
                            code,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            maxFontSize = 28.sp,
                        )
                    }
                }
            }
            // Stacked full width buttons: long translations never wrap inside a button
            Column(Modifier.fillMaxWidth().widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = code != null, onClick = { copyCode() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ContentCopy, null)
                    Text(stringResource(R.string.mobile_copy_code), Modifier.padding(start = 8.dp))
                }
                Button(enabled = openUrl != null, onClick = { openBrowser(openUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.OpenInBrowser, null)
                    Text(stringResource(R.string.mobile_open_browser), Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    private fun copyCode() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
        MessageHelpers.showMessage(this, getString(R.string.mobile_code_copied))
    }

    private fun openBrowser(url: String?) {
        if (url == null) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            MessageHelpers.showMessage(this, e.message)
        }
    }
}

class SignInActivity : CodeActivity(), SignInView {
    private lateinit var presenter: SignInPresenter

    override val screenTitle: String get() = getString(com.liskovsoft.smartyoutubetv2.common.R.string.settings_accounts)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = SignInPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun showCode(userCode: String?, signInUrl: String?) = showCode(userCode, signInUrl, null)

    override fun showCode(userCode: String?, signInUrl: String?, fullSignInUrl: String?) {
        code = userCode
        url = signInUrl
        fullUrl = fullSignInUrl
    }

    override fun close() = finish()
}

class AddDeviceActivity : CodeActivity(), AddDeviceView {
    private lateinit var presenter: AddDevicePresenter

    override val screenTitle: String get() = getString(com.liskovsoft.smartyoutubetv2.common.R.string.dialog_add_device)
    override val description: String get() = getString(com.liskovsoft.smartyoutubetv2.common.R.string.add_device_view_description)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = AddDevicePresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun showCode(userCode: String?) {
        code = userCode
    }

    override fun close() = finish()
}
