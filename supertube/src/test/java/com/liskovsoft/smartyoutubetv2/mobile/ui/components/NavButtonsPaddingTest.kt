package com.liskovsoft.smartyoutubetv2.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Videos and lists were drawn under the three Android navigation buttons */
class NavButtonsPaddingTest {
    private val android10 = 29
    private val android16 = 36

    @Test
    fun threeButtons_bottom_keepsTheWholeBarFree() {
        val bar = EdgeInsets(bottom = 126)
        assertEquals(bar, navButtonsPadding(bar, tappable = EdgeInsets(bottom = 126), sdkInt = android16))
    }

    @Test
    fun gestureNavigation_staysEdgeToEdge() {
        // Gesture handle: the bar has a height but nothing is tappable
        assertEquals(EdgeInsets(), navButtonsPadding(EdgeInsets(bottom = 63), tappable = EdgeInsets(), sdkInt = android16))
    }

    @Test
    fun threeButtons_landscapeOnTheSide_onlyThatSide() {
        val bar = EdgeInsets(right = 126)
        assertEquals(bar, navButtonsPadding(bar, tappable = EdgeInsets(right = 126), sdkInt = android10))
        val left = EdgeInsets(left = 126)
        assertEquals(left, navButtonsPadding(left, tappable = EdgeInsets(left = 126), sdkInt = android10))
    }

    @Test
    fun hiddenBars_fullscreenPlayer_nothing() {
        assertEquals(EdgeInsets(), navButtonsPadding(EdgeInsets(), tappable = EdgeInsets(), sdkInt = android16))
    }

    @Test
    fun beforeAndroid10_alwaysButtons() {
        val bar = EdgeInsets(bottom = 126)
        assertEquals(bar, navButtonsPadding(bar, tappable = EdgeInsets(), sdkInt = 28))
    }
}
