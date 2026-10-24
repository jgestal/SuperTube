package com.liskovsoft.smartyoutubetv2.mobile.ui.base

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity
import com.liskovsoft.smartyoutubetv2.mobile.R

/**
 * Base for every phone/tablet screen.
 * Keeps [MotherActivity] (screensaver, permissions, results, locale) but disables the TV-only tweaks:
 * fake 1080p density, Leanback themes, fullscreen hacks and edge swipe.
 */
abstract class MobileActivity : MotherActivity() {
    override fun isTvUi(): Boolean = false

    override fun initTheme() {
        setTheme(themeResId)
    }

    protected open val themeResId: Int = R.style.Theme_SmartTube_Mobile

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars. Compose insets are applied by every screen.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
}
