package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView as ExoPlayerView
import com.google.android.exoplayer2.video.VideoListener
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData
import com.liskovsoft.smartyoutubetv2.common.utils.Utils
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.playback.PlaybackCommands
import com.liskovsoft.smartyoutubetv2.mobile.playback.PlaybackMediaSession
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme
import java.io.InputStream

/**
 * Touch player. Implements the same [PlaybackView] contract as the TV PlaybackFragment,
 * delegating the engine to the shared [ExoPlayerController] and drawing a Compose UI.
 * Portrait: video on top, info + suggestions below. Landscape: immersive fullscreen.
 */
class PlaybackActivity : ScreenActivity(), PlaybackView {
    private lateinit var presenter: PlaybackPresenter
    private lateinit var controller: ExoPlayerController
    private lateinit var initializer: ExoPlayerInitializer
    private val state = PlayerUiState()
    private var player: SimpleExoPlayer? = null
    private var exoView: ExoPlayerView? = null
    private var mediaSession: PlaybackMediaSession? = null
    /** Speed to go back to when the "press and hold" 2x ends (null = not holding) */
    private var speedBeforeHold: Float? = null
    private var isEngineBlocked = false
    /** Collapsed into the mini player (still playing in its own task) */
    private var isMinimized = false
    private var userLeaveHintMs = 0L
    private var pipEnteredMs = 0L
    private var orientationListener: OrientationEventListener? = null
    /** Orientation requested by the fullscreen button. Released when the device physically matches it. */
    private var lockedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private val qualityView = object : PlayerView {
        override fun setQualityInfo(info: String?) { state.qualityInfo = info }
        override fun setVideo(video: Video?) {}
    }

    private val commands = object : PlaybackCommands {
        override fun play() { presenter.onPlayClicked(); playWhenReady = true }
        override fun pause() { presenter.onPauseClicked(); playWhenReady = false }
        override fun next() { presenter.onNextClicked() }
        override fun previous() { presenter.onPreviousClicked() }
        override fun stop() { presenter.forceFinish() }
    }

    private val videoListener = object : VideoListener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            if (width > 0 && height > 0) state.videoAspect = width * pixelWidthHeightRatio / height
        }
    }

    override val themeResId: Int = R.style.Theme_SmartTube_Mobile_Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializer = ExoPlayerInitializer(this)
        presenter = PlaybackPresenter.instance(this)
        // The controller must exist before setView(): the presenter may query the view right away
        controller = ExoPlayerController(this, presenter)
        presenter.setView(this)

        setContent {
            SmartTubeTheme(darkTheme = true) {
                PlaybackScreen(
                    state = state,
                    presenter = presenter,
                    engine = this,
                    onCreateExoView = { view -> exoView = view; view.player = player },
                    onToggleFullscreen = ::toggleFullscreen,
                    onBack = ::minimize,
                )
            }
        }

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN || lockedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) return
                val isPhysicalLandscape = degrees in 60..120 || degrees in 240..300
                val isPhysicalPortrait = degrees < 30 || degrees > 330
                val wantsLandscape = lockedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                if ((wantsLandscape && isPhysicalLandscape) || (!wantsLandscape && isPhysicalPortrait)) {
                    // The user holds the phone as requested: give control back to the sensor
                    lockedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        applySystemBars(resources.configuration)
        presenter.onViewInitialized()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        orientationListener?.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationListener?.disable()
        maybeReleasePlayer()
    }

    override fun onResume() {
        super.onResume()
        isMinimized = false
        if (player == null) initializePlayer()
        // NOTE: multiple components rely on it
        presenter.onViewResumed()
        blockEngine(false)
    }

    override fun onPause() {
        val isScreenOff = playerData.backgroundMode != PlayerData.BACKGROUND_MODE_DEFAULT && Utils.isHardScreenOff(this)
        if (isScreenOff) blockEngine(true)
        if (!isScreenOff) keepPlayingOnRecents()
        super.onPause()
        presenter.onViewPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBars(newConfig)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        state.isInPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            pipEnteredMs = System.currentTimeMillis()
            state.isOverlayShown = false
            // Also when the system entered PIP by itself (auto-enter on Recents): keep playing in the PIP window
            blockEngine(true)
            viewManager.blockTop(this)
        }
    }

    /**
     * An app screen came to the front while the video is in the PIP window (back to the app from Recents or a link):
     * the PIP window would keep floating over the app. Bring the video back to the player, where the user left it.
     * A PIP opened just before is the app's own navigation (e.g. finish() with PIP): leave it.
     */
    fun returnFromPip() {
        if (!isInPictureInPictureMode || System.currentTimeMillis() - pipEnteredMs < 1_500) return
        viewManager.startView(PlaybackView::class.java)
    }

    private fun applySystemBars(config: Configuration) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Collapses the player into the mini player: the engine keeps playing (this activity lives in its own task)
     * and the parent screen comes to the front.
     */
    fun minimize() {
        if (player == null || !controller.containsMedia()) {
            finish()
            return
        }
        isMinimized = true
        blockEngine(true)
        showOverlay(false)
        viewManager.startParentView(this)
    }

    private fun toggleFullscreen() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        lockedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        requestedOrientation = lockedOrientation
    }

    // Engine lifecycle (same order as the TV PlaybackFragment)

    private fun initializePlayer() {
        if (player != null) return

        val trackSelector: DefaultTrackSelector = RestoreTrackSelector(AdaptiveTrackSelection.Factory())
        controller.setTrackSelector(trackSelector)
        val renderersFactory: DefaultRenderersFactory = CustomOverridesRenderersFactory(this)
        val newPlayer = initializer.createPlayer(this, renderersFactory, trackSelector)
        player = newPlayer
        controller.setPlayer(newPlayer)
        controller.setPlayerView(qualityView)
        newPlayer.addVideoListener(videoListener)
        exoView?.player = newPlayer
        mediaSession = PlaybackMediaSession(
            activity = this,
            player = newPlayer,
            commands = commands,
            video = { video },
            durationMs = { durationMs },
            withSession = !playerTweaksData.isPlaybackNotificationsDisabled,
        ).also { it.onVideoChanged() }

        presenter.setView(this)
        presenter.onEngineInitialized()
    }

    fun maybeReleasePlayer() {
        if (isEngineBlocked) return
        releasePlayer()
    }

    private fun releasePlayer() {
        val current = player ?: return
        presenter.onEngineReleased()
        current.removeVideoListener(videoListener)
        mediaSession?.release()
        mediaSession = null
        speedBeforeHold = null
        exoView?.player = null
        initializer.release()
        controller.release()
        player = null
    }

    // Back, PIP and background playback (ported from the TV PlaybackActivity)

    override fun finish() {
        if (!skipPip()) enterPipMode()

        if (doNotDestroy() && !skipPip()) {
            blockEngine(true)
            viewManager.blockTop(this)
            viewManager.startParentView(this)
        } else {
            super.finish()
        }
    }

    override fun finishReally() {
        SleepTimer.cancel()
        if (isMinimized) {
            // Closed from the mini player: the parent screen is already in front, just stop and close
            isMinimized = false
            viewManager.removeTop(this)
            blockEngine(false)
            superFinishReally()
        } else {
            super.finishReally()
        }
        maybeReleasePlayer()
        presenter.onFinish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        userLeaveHintMs = System.currentTimeMillis()
        if (isBackPressed || isFinishing || viewManager.isNewViewPending || generalData.backgroundPlaybackShortcut == GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_BACK) {
            return
        }

        when (playerData.backgroundMode) {
            PlayerData.BACKGROUND_MODE_PIP -> {
                enterPipMode()
                if (doNotDestroy()) {
                    blockEngine(true)
                    viewManager.blockTop(this)
                }
            }
            PlayerData.BACKGROUND_MODE_SOUND -> if (doNotDestroy()) {
                blockEngine(true)
                viewManager.blockTop(this)
            }
        }
    }

    private fun enterPipMode() {
        if (!Helpers.isPictureInPictureSupported(this) || !wannaEnterToPip()) return
        try {
            val aspect = state.videoAspect.coerceIn(0.42f, 2.38f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational((aspect * 1000).toInt(), 1000))
                .build()
            enterPictureInPictureMode(params)
        } catch (e: Exception) {
            // Device doesn't support picture-in-picture mode
        }
    }

    /**
     * Recents button: some launchers (HyperOS) don't call onUserLeaveHint for it and their Recents transition refuses
     * PIP for every app, so the player was stopped and the video reloaded on return. Try PIP and, if it's refused, keep
     * playing in the background like the Home button does: the video goes on where it was (or in the mini player).
     * Not for the app's own navigation (mini player, channel...), configuration changes or when Home already did it.
     */
    private fun keepPlayingOnRecents() {
        val isLeavingApp = !isFinishing && !isChangingConfigurations && !isInPictureInPictureMode &&
                !viewManager.isNewViewPending && System.currentTimeMillis() - userLeaveHintMs > 1_000
        val isBackgroundPlayback = playerData.backgroundMode == PlayerData.BACKGROUND_MODE_PIP ||
                playerData.backgroundMode == PlayerData.BACKGROUND_MODE_SOUND
        if (!isLeavingApp || !isBackgroundPlayback || !controller.isPlaying ||
            generalData.backgroundPlaybackShortcut == GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_BACK) return

        if (playerData.backgroundMode == PlayerData.BACKGROUND_MODE_PIP) enterPipMode()
        blockEngine(true)
        viewManager.blockTop(this)
    }

    private fun skipPip(): Boolean = isBackPressed && generalData.backgroundPlaybackShortcut == GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_HOME

    private fun wannaEnterToPip(): Boolean {
        val isPip = playerData.backgroundMode == PlayerData.BACKGROUND_MODE_PIP || isEngineBlocked
        return isPip && !isInPictureInPictureMode
    }

    private fun doNotDestroy(): Boolean {
        sIsInPipMode = isInPIPMode()
        val isBackground = playerData.backgroundMode == PlayerEngine.BACKGROUND_MODE_SOUND || isEngineBlocked
        return sIsInPipMode || isBackground
    }

    // PlayerManager

    override fun setVideo(item: Video?) {
        controller.setVideo(item)
        state.video = item
        if (item != null) {
            state.title = item.getTitleFull() ?: "..."
            state.subtitle = item.getSecondTitleFull()?.toString()
        }
        state.revision++
        mediaSession?.onVideoChanged()
    }

    override fun getVideo(): Video? = controller.video

    override fun showBackground(url: String?) { state.backgroundUrl = url }

    override fun showBackgroundColor(colorResId: Int) { state.backgroundUrl = null }

    override fun resetPlayerState() {
        controller.resetPlayerState()
        state.backgroundUrl = null
        state.chatReceiver = null
        state.segments = emptyList()
    }

    override fun isEmbed(): Boolean = false

    // PlayerEngine

    override fun openSabr(formatInfo: MediaItemFormatInfo?) = controller.openSabr(formatInfo)
    override fun openDash(formatInfo: MediaItemFormatInfo?) = controller.openDash(formatInfo)
    override fun openDash(dashManifest: InputStream?) = controller.openDash(dashManifest)
    override fun openDashUrl(dashManifestUrl: String?) = controller.openDashUrl(dashManifestUrl)
    override fun openHlsUrl(hlsPlaylistUrl: String?) = controller.openHlsUrl(hlsPlaylistUrl)
    override fun openUrlList(urlList: MutableList<String>?) = controller.openUrlList(urlList)
    override fun openMerged(formatInfo: MediaItemFormatInfo?, hlsPlaylistUrl: String?) = controller.openMerged(formatInfo, hlsPlaylistUrl)
    override fun openMerged(dashManifest: InputStream?, hlsPlaylistUrl: String?) = controller.openMerged(dashManifest, hlsPlaylistUrl)
    override fun getPositionMs(): Long = controller.positionMs
    override fun setPositionMs(positionMs: Long) = controller.setPositionMs(positionMs)

    override fun getDurationMs(): Long {
        var duration = controller.durationMs
        val liveDuration = video?.liveDurationMs ?: 0
        if (duration > Video.MAX_LIVE_DURATION_MS && liveDuration != 0L) duration = liveDuration
        return duration
    }

    override fun setPlayWhenReady(play: Boolean) = controller.setPlayWhenReady(play)
    override fun getPlayWhenReady(): Boolean = controller.playWhenReady
    override fun isPlaying(): Boolean = controller.isPlaying
    override fun isLoading(): Boolean = controller.isLoading
    override fun getVideoFormats(): MutableList<FormatItem> = controller.videoFormats
    override fun getAudioFormats(): MutableList<FormatItem> = controller.audioFormats
    override fun getSubtitleFormats(): MutableList<FormatItem> = controller.subtitleFormats
    override fun setFormat(option: FormatItem?) = controller.selectFormat(option)
    override fun getVideoFormat(): FormatItem? = controller.videoFormat
    override fun getAudioFormat(): FormatItem? = controller.audioFormat
    override fun getSubtitleFormat(): FormatItem? = controller.subtitleFormat
    override fun isEngineInitialized(): Boolean = player != null

    override fun restartEngine() {
        if (isDestroyed) return
        releasePlayer()
        initializePlayer()
    }

    override fun reloadPlayback() {
        if (player != null) {
            presenter.onEngineReleased()
            presenter.onEngineInitialized()
        }
    }

    override fun blockEngine(block: Boolean) { isEngineBlocked = block }
    override fun isEngineBlocked(): Boolean = isEngineBlocked
    override fun isInPIPMode(): Boolean = isInPictureInPictureMode
    override fun containsMedia(): Boolean = controller.containsMedia()

    override fun setSpeed(speed: Float) {
        if (speedBeforeHold != null) {
            // Applied when the hold ends
            if (speed > 0) speedBeforeHold = speed
        } else {
            controller.speed = speed
            if (speed > 0) state.speed = speed
        }
        setButtonState(com.liskovsoft.smartyoutubetv2.common.R.id.action_video_speed, if (speed != 1.0f) PlayerUI.BUTTON_ON else PlayerUI.BUTTON_OFF)
    }

    /** While holding, the shared logic sees the real speed: the temporary 2x is never saved (channel, video state) */
    override fun getSpeed(): Float = speedBeforeHold ?: controller.speed

    /**
     * "Press and hold" speed. Applied straight to the player, without the engine speed events, so it isn't
     * remembered: if the app is closed while holding, the video reopens at the previous speed.
     */
    fun startSpeedHold(speed: Float) {
        val current = player ?: return
        if (speedBeforeHold == null) speedBeforeHold = controller.speed.takeIf { it > 0 } ?: 1f
        val pitch = if (playerTweaksData.isAudioTimeStretchingEnabled) current.playbackParameters.pitch else speed
        current.playbackParameters = PlaybackParameters(speed, pitch)
        state.speed = speed
    }

    fun endSpeedHold() {
        val speed = speedBeforeHold ?: return
        speedBeforeHold = null
        controller.speed = speed
        state.speed = speed
    }
    override fun setPitch(pitch: Float) { controller.pitch = pitch }
    override fun getPitch(): Float = controller.pitch
    override fun setVolume(volume: Float) { controller.volume = volume }
    override fun getVolume(): Float = controller.volume
    override fun setResizeMode(mode: Int) { exoView?.resizeMode = mode }
    override fun getResizeMode(): Int = exoView?.resizeMode ?: PlayerEngine.RESIZE_MODE_DEFAULT

    override fun setZoomPercents(percents: Int) {
        val scale = if (percents > 0) percents / 100f else 1f
        exoView?.videoSurfaceView?.let { it.scaleX = scale * if (it.scaleX < 0) -1 else 1; it.scaleY = scale }
    }

    override fun setAspectRatio(ratio: Float) {
        // The surface keeps the natural aspect ratio on phones
    }

    override fun setRotationAngle(angle: Int) { exoView?.videoSurfaceView?.rotation = angle.toFloat() }

    override fun setVideoFlipEnabled(enabled: Boolean) {
        exoView?.videoSurfaceView?.let { val abs = kotlin.math.abs(it.scaleX); it.scaleX = if (enabled) -abs else abs }
    }

    override fun setVideoGravity(gravity: Int) {
        // Not supported on the touch layout
    }

    // PlayerUI

    override fun updateSuggestions(group: VideoGroup) { state.suggestions.updateRows(group) }
    override fun removeSuggestions(group: VideoGroup) { state.suggestions.rows.removeAll { it.id == group.id } }
    override fun getSuggestionsIndex(group: VideoGroup): Int = state.suggestions.rows.indexOfFirst { it.id == group.id }

    override fun getSuggestionsByIndex(index: Int): VideoGroup? {
        val row = state.suggestions.rows.getOrNull(index) ?: return null
        return row.items.firstOrNull()?.group
    }

    override fun focusSuggestedItem(index: Int) {}
    override fun focusSuggestedItem(video: Video?) {}
    override fun resetSuggestedPosition() {}
    override fun isSuggestionsEmpty(): Boolean = state.suggestions.isEmpty
    override fun clearSuggestions() = state.suggestions.clear()

    override fun showOverlay(show: Boolean) {
        if (show && isInPIPMode()) return
        if (state.isOverlayShown != show) {
            state.isOverlayShown = show
            presenter.onControlsShown(show)
        }
        if (!show) state.isSuggestionsShown = false
    }

    override fun isOverlayShown(): Boolean = state.isOverlayShown

    override fun showSuggestions(show: Boolean) {
        showOverlay(show)
        state.isSuggestionsShown = show && !state.suggestions.isEmpty
    }

    override fun isSuggestionsShown(): Boolean = state.isOverlayShown && state.isSuggestionsShown

    override fun showControls(show: Boolean) {
        showOverlay(show)
        state.isSuggestionsShown = false
    }

    override fun isControlsShown(): Boolean = state.isOverlayShown && !state.isSuggestionsShown
    override fun getButtonState(buttonId: Int): Int = state.buttonState(buttonId)
    override fun setButtonState(buttonId: Int, buttonState: Int) { state.buttonStates[buttonId] = buttonState }
    override fun setChannelIcon(iconUrl: String?) { state.channelIconUrl = iconUrl }
    override fun setSeekPreviewTitle(title: String?) {}
    override fun setNextTitle(nextVideo: Video?) { state.nextTitle = nextVideo?.getTitle() }
    override fun showDebugInfo(show: Boolean) {}

    override fun showSubtitles(show: Boolean) {
        state.isSubtitlesShown = show
        exoView?.subtitleView?.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun loadStoryboard() {}
    override fun setTitle(title: String?) { state.title = title }

    override fun showProgressBar(show: Boolean) {
        // Hiding is always allowed. Showing is skipped while the engine reports its own buffering state.
        if (show && controller.isBuffering) return
        state.isProgressShown = show
    }

    override fun setSeekBarSegments(segments: MutableList<SeekBarSegment>?) { state.segments = segments?.toList() ?: emptyList() }
    override fun updateEndingTime() {}
    override fun setChatReceiver(chatReceiver: ChatReceiver?) { state.chatReceiver = chatReceiver }

    /** Polled by the UI (position, buffering) */
    fun pollEngine() {
        val p = player
        state.isPlaying = controller.isPlaying
        // NOTE: player.isLoading() is true while prefetching ahead during normal playback. Only real buffering stalls count.
        // Paused + buffering (e.g. after a seek while paused) keeps the play button available
        state.isBuffering = controller.isBuffering && controller.playWhenReady
        state.positionMs = controller.positionMs.coerceAtLeast(0)
        state.durationMs = durationMs.coerceAtLeast(0)
        state.bufferedMs = p?.bufferedPosition ?: 0
        p?.playbackParameters?.speed?.let { state.speed = it }

        if (SleepTimer.isActive && SleepTimer.shouldStop(state.positionMs, state.durationMs)) {
            presenter.onPauseClicked()
            playWhenReady = false
            MessageHelpers.showMessage(this, getString(R.string.mobile_sleep_stopped))
        }
    }
}
