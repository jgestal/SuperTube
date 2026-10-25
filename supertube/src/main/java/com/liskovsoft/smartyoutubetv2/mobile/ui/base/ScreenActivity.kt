package com.liskovsoft.smartyoutubetv2.mobile.ui.base

import com.liskovsoft.smartyoutubetv2.mobile.ui.playback.PlaybackActivity
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.utils.Utils

/**
 * Full screen destination registered in the ViewManager (mobile counterpart of the TV LeanbackActivity).
 * Navigation between screens is driven by the shared ViewManager stack, so back behaves like on TV:
 * finishing a screen re-launches its parent.
 */
abstract class ScreenActivity : MobileActivity() {
    private val onFinishApp = Runnable { Utils.forceFinishTheApp(this) }

    override fun onResume() {
        super.onResume()
        if (this !is PlaybackActivity) {
            // A browsing screen is visible: the user is using the whole app, not only the player opened from a link.
            // Reset the TV "player only" flags, otherwise ViewManager sends the app to the background on the next resume
            // (e.g. when coming back from the system file picker).
            viewManager.enablePlayerOnlyMode(false)
            Helpers.setField(viewManager, "mIsMoveToBackEnabled", false)
        }
        viewManager.addTop(this)
        if (this !is PlaybackActivity) {
            (PlaybackPresenter.instance(this).view as? PlaybackActivity)?.returnFromPip()
        }
    }

    override fun onSearchRequested(): Boolean {
        SearchPresenter.instance(this).startSearch(null)
        return true
    }

    override fun finish() {
        // User pressed back (single back press is the touch convention, no "press again to exit")
        if (!viewManager.hasParentView(this)) {
            viewManager.addOnFinish(onFinishApp)
            Utils.properlyFinishTheApp(this)
        } else {
            finishReally()
        }
    }

    /** Finishes without re-launching the parent screen */
    protected fun superFinishReally() = super.finishReally()

    override fun finishReally() {
        // Mandatory: fixes the view order (especially for the playback view)
        viewManager.startParentView(this)
        super.finishReally()
    }
}
