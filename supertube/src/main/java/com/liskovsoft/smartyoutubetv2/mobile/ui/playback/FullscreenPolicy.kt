package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

/** Orientation the player asks the system for */
enum class OrientationRequest {
    /** Follow the device (and the system auto-rotate setting) */
    SENSOR,
    /** Landscape, either side */
    LANDSCAPE,
    PORTRAIT,
}

/** How the user is holding the phone, from the accelerometer */
enum class DevicePosture { PORTRAIT, LANDSCAPE, UNKNOWN }

/**
 * Fullscreen and rotation rules of the player. No Android dependencies: unit tested in FullscreenPolicyTest.
 *
 * - The fullscreen button forces an orientation. With the system auto-rotate on, the request is released back to the
 *   sensor once the user holds the phone that way for a moment ([SETTLE_MS]: the system rotation has its own delay,
 *   releasing earlier made the screen rotate back and forth). With auto-rotate off it's never released: the system
 *   lock would undo the fullscreen.
 * - Leaving fullscreen while the phone isn't held in landscape (e.g. lying on a table), or leaving the phone flat
 *   afterwards, also releases the portrait request when the phone is then held in landscape: turning the phone
 *   enters fullscreen again instead of staying locked in portrait.
 * - Vertical videos go fullscreen in portrait, without rotating.
 */
class FullscreenPolicy(
    private val isAutoRotateEnabled: () -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var request = OrientationRequest.SENSOR
        private set

    /** Fullscreen of a vertical video: portrait, without system bars */
    var isPortraitFullscreen = false
        private set

    private var postureAtToggle = DevicePosture.UNKNOWN
    private var lastPosture = DevicePosture.UNKNOWN
    private var releaseCandidateSinceMs = NONE

    /** Fullscreen button, swipe up/down or back in fullscreen */
    fun toggle(isLandscape: Boolean, isVerticalVideo: Boolean) {
        releaseCandidateSinceMs = NONE
        postureAtToggle = lastPosture
        when {
            isPortraitFullscreen -> isPortraitFullscreen = false
            isLandscape -> request = OrientationRequest.PORTRAIT
            isVerticalVideo -> isPortraitFullscreen = true
            else -> request = OrientationRequest.LANDSCAPE
        }
    }

    /** Video size changed: a horizontal video can't stay in the portrait fullscreen */
    fun onVideoChanged(isVerticalVideo: Boolean) {
        if (!isVerticalVideo) isPortraitFullscreen = false
    }

    /**
     * Accelerometer update (degrees from OrientationEventListener, negative = unknown, e.g. lying flat).
     * Returns true when [request] changed.
     */
    fun onDeviceRotated(degrees: Int): Boolean {
        val posture = postureOf(degrees)
        if (posture != DevicePosture.UNKNOWN) lastPosture = posture
        // Lying flat (e.g. left on a table): picking the phone up later is a new gesture
        if (degrees < 0) postureAtToggle = DevicePosture.UNKNOWN
        if (request == OrientationRequest.SENSOR || !isAutoRotateEnabled() || !canRelease(posture)) {
            releaseCandidateSinceMs = NONE
            return false
        }
        val now = clock()
        if (releaseCandidateSinceMs == NONE) {
            releaseCandidateSinceMs = now
            return false
        }
        if (now - releaseCandidateSinceMs < SETTLE_MS) return false
        request = OrientationRequest.SENSOR
        releaseCandidateSinceMs = NONE
        return true
    }

    /** The player was closed or collapsed: the next video starts following the device */
    fun reset() {
        request = OrientationRequest.SENSOR
        isPortraitFullscreen = false
        releaseCandidateSinceMs = NONE
    }

    /** System bars are hidden in landscape and in the portrait fullscreen */
    fun hidesSystemBars(isLandscape: Boolean): Boolean = isLandscape || isPortraitFullscreen

    private fun canRelease(posture: DevicePosture): Boolean = when (request) {
        OrientationRequest.LANDSCAPE -> posture == DevicePosture.LANDSCAPE
        OrientationRequest.PORTRAIT -> posture == DevicePosture.PORTRAIT ||
                (posture == DevicePosture.LANDSCAPE && postureAtToggle != DevicePosture.LANDSCAPE)
        OrientationRequest.SENSOR -> false
    }

    companion object {
        /** Time the phone must be held in the target orientation before following the sensor again */
        const val SETTLE_MS = 600L
        private const val NONE = -1L

        /** Angles between the two ranges (diagonal) count as unknown, so a half-turned phone changes nothing */
        fun postureOf(degrees: Int): DevicePosture = when {
            degrees < 0 -> DevicePosture.UNKNOWN
            degrees in 60..120 || degrees in 240..300 -> DevicePosture.LANDSCAPE
            degrees <= 30 || degrees >= 330 -> DevicePosture.PORTRAIT
            else -> DevicePosture.UNKNOWN
        }
    }
}
