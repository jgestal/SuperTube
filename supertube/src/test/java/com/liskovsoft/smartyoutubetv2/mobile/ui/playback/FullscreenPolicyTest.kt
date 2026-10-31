package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import com.liskovsoft.smartyoutubetv2.mobile.ui.playback.FullscreenPolicy.Companion.SETTLE_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenPolicyTest {
    private var now = 0L
    private var autoRotate = true
    private val policy = FullscreenPolicy(isAutoRotateEnabled = { autoRotate }, clock = { now })

    /** Holds the phone at [degrees] for [ms] (accelerometer updates every 100 ms) */
    private fun hold(degrees: Int, ms: Long = SETTLE_MS + 100) {
        val end = now + ms
        while (now <= end) {
            policy.onDeviceRotated(degrees)
            now += 100
        }
    }

    // Entering fullscreen

    @Test
    fun button_inPortrait_forcesLandscape() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    @Test
    fun forcedLandscape_isKeptWhileThePhoneStaysPortrait() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(0)
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    @Test
    fun forcedLandscape_followsTheSensorOnceThePhoneIsTurned() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(90)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun stillPhone_singleSensorEvent_isReleasedByTheSettleCheck() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        // Some sensors only report changes: one event, then the activity's delayed check with the same reading
        policy.onDeviceRotated(90)
        now += SETTLE_MS + 50
        policy.onDeviceRotated(90)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun forcedLandscape_isNotReleasedBeforeTheRotationSettles() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(90, ms = SETTLE_MS / 2)
        // Releasing now made the system rotate back to portrait for a moment (its rotation has its own delay)
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    @Test
    fun briefLandscapeWhileMovingThePhone_isIgnored() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(90, ms = 300)
        hold(0, ms = 300)
        hold(90, ms = 300)
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    @Test
    fun reverseLandscape_alsoReleases() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(270)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun autoRotateOff_neverReleasesTheFullscreen() {
        autoRotate = false
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(90, ms = 5_000)
        // Following the sensor would bring the system rotation lock back: portrait
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    // Leaving fullscreen

    @Test
    fun button_inLandscape_forcesPortrait() {
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        assertEquals(OrientationRequest.PORTRAIT, policy.request)
    }

    @Test
    fun exitWhileHoldingLandscape_staysPortraitUntilThePhoneIsTurned() {
        hold(90)
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        hold(90, ms = 3_000)
        assertEquals(OrientationRequest.PORTRAIT, policy.request)
        hold(0)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun exitOnATable_thenPickingThePhoneInLandscape_entersFullscreenAgain() {
        hold(-1) // lying flat: unknown
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        hold(-1, ms = 3_000)
        assertEquals(OrientationRequest.PORTRAIT, policy.request)
        hold(90)
        // Before: locked in portrait forever, turning the phone did nothing
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun exitHoldingLandscape_leaveOnATable_thenPickItUpInLandscape_entersFullscreenAgain() {
        hold(90)
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        hold(-1, ms = 2_000)
        hold(90)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun exitWhileHoldingPortrait_releasesRightAway() {
        hold(0)
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        hold(0)
        assertEquals(OrientationRequest.SENSOR, policy.request)
    }

    @Test
    fun diagonalPhone_changesNothing() {
        policy.toggle(isLandscape = false, isVerticalVideo = false)
        hold(45, ms = 3_000)
        assertEquals(OrientationRequest.LANDSCAPE, policy.request)
    }

    // Vertical videos

    @Test
    fun verticalVideo_goesFullscreenInPortrait() {
        policy.toggle(isLandscape = false, isVerticalVideo = true)
        assertTrue(policy.isPortraitFullscreen)
        assertEquals(OrientationRequest.SENSOR, policy.request)
        assertTrue(policy.hidesSystemBars(isLandscape = false))
    }

    @Test
    fun verticalVideo_toggleAgain_leavesThePortraitFullscreen() {
        policy.toggle(isLandscape = false, isVerticalVideo = true)
        policy.toggle(isLandscape = false, isVerticalVideo = true)
        assertFalse(policy.isPortraitFullscreen)
        assertFalse(policy.hidesSystemBars(isLandscape = false))
    }

    @Test
    fun horizontalVideoAfterAVerticalOne_leavesThePortraitFullscreen() {
        policy.toggle(isLandscape = false, isVerticalVideo = true)
        policy.onVideoChanged(isVerticalVideo = false)
        assertFalse(policy.isPortraitFullscreen)
    }

    // Lifecycle and system bars

    @Test
    fun reset_followsTheSensorForTheNextVideo() {
        policy.toggle(isLandscape = true, isVerticalVideo = false)
        policy.reset()
        assertEquals(OrientationRequest.SENSOR, policy.request)
        assertFalse(policy.isPortraitFullscreen)
    }

    @Test
    fun systemBars_hiddenOnlyInFullscreen() {
        assertTrue(policy.hidesSystemBars(isLandscape = true))
        assertFalse(policy.hidesSystemBars(isLandscape = false))
    }

    @Test
    fun postures() {
        assertEquals(DevicePosture.PORTRAIT, FullscreenPolicy.postureOf(0))
        assertEquals(DevicePosture.PORTRAIT, FullscreenPolicy.postureOf(350))
        assertEquals(DevicePosture.LANDSCAPE, FullscreenPolicy.postureOf(90))
        assertEquals(DevicePosture.LANDSCAPE, FullscreenPolicy.postureOf(270))
        assertEquals(DevicePosture.UNKNOWN, FullscreenPolicy.postureOf(45))
        assertEquals(DevicePosture.UNKNOWN, FullscreenPolicy.postureOf(-1))
    }
}
