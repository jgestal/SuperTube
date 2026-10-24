package com.liskovsoft.smartyoutubetv2.mobile.ui.dialog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

/** One screen of the dialog back stack: either a list of categories or a single opened category */
class DialogPage(
    val categories: List<OptionCategory>,
    val title: CharSequence?,
    /** Category opened from a parent page (radio list, checkboxes, comments...) */
    val opened: OptionCategory? = null,
)

class DialogUiState {
    val pages = mutableStateListOf<DialogPage>()
    var isTransparent by mutableStateOf(false)
    var isOverlay by mutableStateOf(false)
    /** Bumped on every option change so summaries/check marks re-read the option state */
    var revision by mutableStateOf(0)

    val current: DialogPage? get() = pages.lastOrNull()
}

/**
 * Every menu of the app (video context menu, player settings, quality, comments, app settings...) goes through
 * [AppDialogPresenter]. On TV it's a Leanback side panel; here it's a bottom sheet (phones) or a centered dialog
 * (large screens) drawn by a translucent activity on top of the current screen.
 */
class AppDialogActivity : MobileActivity(), AppDialogView {
    private lateinit var presenter: AppDialogPresenter
    private val state = DialogUiState()
    private var isPausedState = false
    private var viewId = -1

    override val themeResId: Int = R.style.Theme_SmartTube_Mobile_Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = AppDialogPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                AppDialogScreen(
                    state = state,
                    onOpenCategory = { page, category -> state.pages.add(DialogPage(page.categories, category.title, category)) },
                    onChanged = { state.revision++ },
                    onBack = { onBackPressed() },
                    onDismiss = { closeAll() },
                )
            }
        }

        presenter.onViewInitialized()
    }

    override fun onResume() {
        super.onResume()
        isPausedState = false
    }

    override fun onPause() {
        super.onPause()
        isPausedState = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        closeAll()
    }

    override fun finish() {
        // Back pressed inside nested pages: return to the previous page instead of closing
        if (isBackPressed && canGoBack()) {
            goBack()
            resetBackState()
            return
        }

        closeAll()
    }

    private fun closeAll() {
        presenter.onFinish()
        // Destroy the dialog for real (the presenter may reopen it with new content)
        finishReally()
    }

    // AppDialogView

    override fun show(categories: List<OptionCategory>, title: CharSequence?, isExpandable: Boolean, isTransparent: Boolean, isOverlay: Boolean, id: Int) {
        // Only the root page decides transparency (same as TV)
        if (state.pages.isEmpty()) state.isTransparent = isTransparent
        state.isOverlay = isOverlay
        viewId = id

        val opened = if (isExpandable && categories.size == 1) categories[0] else null

        if (opened != null && opened.type == OptionCategory.TYPE_SINGLE_BUTTON) {
            // Single button item: imitate the click
            opened.options.firstOrNull()?.onSelect(true)
            return
        }

        state.pages.add(DialogPage(categories, title ?: opened?.title, opened))
    }

    override fun goBack() {
        if (canGoBack()) state.pages.removeAt(state.pages.lastIndex) else closeAll()
    }

    override fun clearBackstack() {
        val current = state.current
        state.pages.clear()
        if (current != null) state.pages.add(current)
    }

    override fun canGoBack(): Boolean = state.pages.size > 1

    override fun isShown(): Boolean = !isFinishing && !isPausedState

    override fun isTransparent(): Boolean = state.isTransparent

    override fun isOverlay(): Boolean = state.isOverlay

    override fun isPaused(): Boolean = isPausedState

    override fun getViewId(): Int = viewId
}
