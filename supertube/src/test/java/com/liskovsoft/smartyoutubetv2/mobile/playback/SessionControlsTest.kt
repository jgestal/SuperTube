package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.support.v4.media.session.PlaybackStateCompat.ACTION_FAST_FORWARD
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_REWIND
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In Android Auto the buttons next to play/pause showed "back 10 s / forward 10 s": the car must get previous/next
 * video instead, and those must reach the player.
 */
class SessionControlsTest {
    /** What the exoplayer connector computes for a seekable video with its default 5 s / 15 s steps */
    private val seekableVideo = ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or ACTION_STOP or
        ACTION_SEEK_TO or ACTION_FAST_FORWARD or ACTION_REWIND

    private val commands = RecordingCommands()
    private val navigator = SkipQueueNavigator(commands)

    @Test
    fun carGetsPreviousAndNextVideo() {
        val actions = SessionControls.publishedActions(seekableVideo, navigator)

        assertTrue("No next video button", actions has ACTION_SKIP_TO_NEXT)
        assertTrue("No previous video button", actions has ACTION_SKIP_TO_PREVIOUS)
    }

    @Test
    fun carDoesNotGetSeekBySteps() {
        val actions = SessionControls.publishedActions(seekableVideo, navigator)

        assertFalse("Forward 10 s would take the next button's place", actions has ACTION_FAST_FORWARD)
        assertFalse("Back 10 s would take the previous button's place", actions has ACTION_REWIND)
    }

    @Test
    fun playPauseAndProgressBarStay() {
        val actions = SessionControls.publishedActions(seekableVideo, navigator)

        assertTrue(actions has ACTION_PLAY_PAUSE)
        assertTrue(actions has ACTION_PLAY)
        assertTrue(actions has ACTION_PAUSE)
        assertTrue("Dragging the progress bar", actions has ACTION_SEEK_TO)
    }

    @Test
    fun skipButtonsAreOfferedForLiveStreamsToo() {
        // Not seekable: the connector offers only play/pause, the skip buttons must still be there
        val actions = SessionControls.publishedActions(ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or ACTION_STOP, navigator)

        assertTrue(actions has ACTION_SKIP_TO_NEXT)
        assertTrue(actions has ACTION_SKIP_TO_PREVIOUS)
    }

    @Test
    fun nextButtonSkipsTheVideo() {
        navigator.onSkipToNext(null, null)

        assertEquals(listOf("next"), commands.calls)
    }

    @Test
    fun previousButtonGoesToThePreviousVideo() {
        navigator.onSkipToPrevious(null, null)

        assertEquals(listOf("previous"), commands.calls)
    }

    @Test
    fun severalSkipsInARowAreAllForwarded() {
        repeat(3) { navigator.onSkipToNext(null, null) }
        navigator.onSkipToPrevious(null, null)

        assertEquals(listOf("next", "next", "next", "previous"), commands.calls)
    }

    @Test
    fun slotsNextToPlayAreReservedForSkipping() {
        val keys = SessionControls.SLOT_RESERVATION_KEYS

        assertTrue(keys.any { it.endsWith("ACTION_SKIP_TO_NEXT") })
        assertTrue(keys.any { it.endsWith("ACTION_SKIP_TO_PREVIOUS") })
        assertTrue("Current Android Auto key", "android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT" in keys)
    }

    private infix fun Long.has(action: Long) = this and action == action

    private class RecordingCommands : PlaybackCommands {
        val calls = mutableListOf<String>()
        override fun play() { calls += "play" }
        override fun pause() { calls += "pause" }
        override fun next() { calls += "next" }
        override fun previous() { calls += "previous" }
        override fun stop() { calls += "stop" }
    }
}
