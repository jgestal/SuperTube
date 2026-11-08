package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.content.Context
import android.os.Build
import android.view.WindowManager

/**
 * Video quality that fits the screen: there's no point in downloading 4K for a phone screen (data, battery and
 * stalls on mobile networks). Fullscreen fills the short side of the screen, so that's the useful height.
 */
object ScreenQuality {
    /** Standard YouTube heights, biggest first */
    private val HEIGHTS = intArrayOf(2160, 1440, 1080, 720, 480, 360)

    /**
     * Biggest standard height that the screen can show. A screen slightly short of a step (e.g. 1080 px panels with
     * cutouts reporting 1072) still gets that step: up to 10% of upscaling isn't visible.
     */
    fun fitHeight(screenShortSide: Int): Int {
        if (screenShortSide <= 0) return 1080
        return HEIGHTS.firstOrNull { it <= screenShortSide * 1.1 } ?: HEIGHTS.last()
    }

    /** Short side of the whole display in pixels (the window may be smaller in split screen) */
    fun screenShortSide(context: Context): Int {
        val wm = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= 30 && wm != null) {
            val bounds = wm.maximumWindowMetrics.bounds
            minOf(bounds.width(), bounds.height())
        } else {
            val metrics = context.resources.displayMetrics
            minOf(metrics.widthPixels, metrics.heightPixels)
        }
    }
}
