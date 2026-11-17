package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

/**
 * Buttons the media session offers to Android Auto, the lock screen, Bluetooth (car, headset) and the system media
 * controls. A video is a track of the queue: the two buttons around play/pause go to the previous and the next video.
 *
 * Fast forward and rewind are not offered. With them in the session, Android Auto showed "back 10 s / forward 10 s"
 * in place of previous/next, and a car could only seek inside the video instead of skipping it.
 */
object SessionControls {
    /** Playback actions the connector may offer: all of them except seeking by steps */
    const val ENABLED_PLAYBACK_ACTIONS: Long = MediaSessionConnector.ALL_PLAYBACK_ACTIONS and
        (PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND).inv()

    /** Always offered: next falls back to the related video, previous restarts the video when nothing came before */
    const val QUEUE_ACTIONS: Long = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT

    /**
     * Session extras that keep the slots next to play/pause for previous/next in Android Auto (current and legacy
     * keys), so they are never given to other actions.
     */
    val SLOT_RESERVATION_KEYS = listOf(
        "android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS",
        "android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT",
        "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS",
        "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT",
    )

    /**
     * What the session publishes for a given set of player actions, the same way [MediaSessionConnector] combines them:
     * the playback actions it computes are filtered by the enabled ones and the queue navigator adds its own.
     */
    fun publishedActions(playerActions: Long, navigator: MediaSessionConnector.QueueNavigator): Long =
        (playerActions and ENABLED_PLAYBACK_ACTIONS) or
            (MediaSessionConnector.QueueNavigator.ACTIONS and navigator.getSupportedQueueNavigatorActions(null))
}

/** Previous/next of the session (car, headset, lock screen) go to the previous/next video of the player */
class SkipQueueNavigator(private val commands: PlaybackCommands) : MediaSessionConnector.QueueNavigator {
    override fun getSupportedQueueNavigatorActions(player: Player?): Long = SessionControls.QUEUE_ACTIONS
    override fun onTimelineChanged(player: Player?) {}
    override fun onCurrentWindowIndexChanged(player: Player?) {}
    override fun getActiveQueueItemId(player: Player?): Long = 0
    override fun onSkipToPrevious(player: Player?, controlDispatcher: ControlDispatcher?) = commands.previous()
    override fun onSkipToQueueItem(player: Player?, controlDispatcher: ControlDispatcher?, id: Long) {}
    override fun onSkipToNext(player: Player?, controlDispatcher: ControlDispatcher?) = commands.next()
    override fun onCommand(player: Player?, controlDispatcher: ControlDispatcher?, command: String?, extras: Bundle?, cb: ResultReceiver?): Boolean = false
}
