package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.download.Downloads

/**
 * Everything the system sees of the player: the media session (lock screen, Bluetooth and headset buttons,
 * Android 13+ media controls), the playback notification with its foreground service, and the
 * "audio becoming noisy" pause (headphones unplugged). Created with the engine, released with it.
 */
class PlaybackMediaSession(
    private val activity: Activity,
    private val player: SimpleExoPlayer,
    private val commands: PlaybackCommands,
    private val video: () -> Video?,
    private val durationMs: () -> Long,
    withSession: Boolean,
) {
    private val context: Context = activity.applicationContext
    private val session: MediaSessionCompat?
    private val connector: MediaSessionConnector?
    private var artwork: Bitmap? = null
    private var artworkUrl: String? = null
    private var artworkTarget: CustomTarget<Bitmap>? = null

    private val playerListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) = updateNotification()
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && player.playWhenReady) commands.pause()
        }
    }

    init {
        session = if (withSession) MediaSessionCompat(context, context.packageName).apply { isActive = true } else null
        connector = session?.let(::createConnector)
        player.addListener(playerListener)
        ContextCompat.registerReceiver(context, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)

        PlaybackService.commands = commands
        updateNotification()
        PlaybackService.start(activity)
    }

    /** The video (title, author, thumbnail) changed */
    fun onVideoChanged() {
        loadArtwork(artworkUrlOf(video()))
        connector?.invalidateMediaSessionMetadata()
        updateNotification()
    }

    fun release() {
        player.removeListener(playerListener)
        runCatching { context.unregisterReceiver(noisyReceiver) }
        artworkTarget?.let { Glide.with(context).clear(it) }
        connector?.setPlayer(null)
        session?.isActive = false
        session?.release()
        if (PlaybackService.commands === commands) PlaybackService.commands = null
        PlaybackService.stop(context)
    }

    private fun createConnector(session: MediaSessionCompat): MediaSessionConnector? {
        val connector = MediaSessionConnector(session)
        try {
            connector.setPlayer(player)
        } catch (e: NoSuchMethodError) {
            return null
        }
        connector.setMediaMetadataProvider { buildMetadata() }
        connector.setQueueNavigator(object : MediaSessionConnector.QueueNavigator {
            override fun getSupportedQueueNavigatorActions(player: Player?): Long =
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            override fun onTimelineChanged(player: Player?) {}
            override fun onCurrentWindowIndexChanged(player: Player?) {}
            override fun getActiveQueueItemId(player: Player?): Long = 0
            override fun onSkipToPrevious(player: Player?, controlDispatcher: ControlDispatcher?) = commands.previous()
            override fun onSkipToQueueItem(player: Player?, controlDispatcher: ControlDispatcher?, id: Long) {}
            override fun onSkipToNext(player: Player?, controlDispatcher: ControlDispatcher?) = commands.next()
            override fun onCommand(player: Player?, controlDispatcher: ControlDispatcher?, command: String?, extras: Bundle?, cb: ResultReceiver?): Boolean = false
        })
        return connector
    }

    private fun buildMetadata(): MediaMetadataCompat? {
        val video = video() ?: return null
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, video.videoId)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.getTitleFull())
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, video.getTitleFull())
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, video.getAuthor())
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, Helpers.toString(video.getSecondTitleFull()))
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUrlOf(video))
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs())
            .build()
    }

    /** Videos opened from a link may come without a card image: use the YouTube thumbnail of the id (like the mini player) */
    private fun artworkUrlOf(video: Video?): String? = video?.getCardImageUrl() ?: video?.videoId?.let(Downloads::thumbnailOf)

    private fun loadArtwork(url: String?) {
        if (url == artworkUrl) return
        artworkUrl = url
        artwork = null
        artworkTarget?.let { Glide.with(context).clear(it) }
        if (url == null) return
        // centerCrop: the 4:3 YouTube thumbnails have black bars around the 16:9 picture
        artworkTarget = Glide.with(context).asBitmap().load(url).centerCrop().into(object : CustomTarget<Bitmap>(ARTWORK_WIDTH, ARTWORK_HEIGHT) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (url != artworkUrl) return
                artwork = resource
                connector?.invalidateMediaSessionMetadata()
                updateNotification()
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun updateNotification() {
        val notification = buildNotification()
        PlaybackService.currentNotification = notification
        // The notification permission (Android 13+) may be denied: the service keeps working without it
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) manager.notify(PlaybackService.NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        createChannel()
        val video = video()
        val isPlaying = player.playWhenReady
        val playPause = if (isPlaying) {
            action(android.R.drawable.ic_media_pause, R.string.mobile_pause, PlaybackService.ACTION_PAUSE)
        } else {
            action(android.R.drawable.ic_media_play, R.string.mobile_play, PlaybackService.ACTION_PLAY)
        }
        val style = MediaStyle().setShowActionsInCompactView(0, 1, 2)
        session?.let { style.setMediaSession(it.sessionToken) }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(video?.getTitleFull() ?: video?.getTitle())
            .setContentText(video?.getAuthor())
            .setLargeIcon(artwork)
            .setContentIntent(openPlayerIntent())
            // Swiping away a paused notification (allowed for foreground services on Android 14+) closes the player
            .setDeleteIntent(commandIntent(PlaybackService.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .addAction(action(android.R.drawable.ic_media_previous, R.string.mobile_previous, PlaybackService.ACTION_PREVIOUS))
            .addAction(playPause)
            .addAction(action(android.R.drawable.ic_media_next, R.string.mobile_next, PlaybackService.ACTION_NEXT))
            .setStyle(style)
            .build()
    }

    private fun action(icon: Int, title: Int, command: String) =
        NotificationCompat.Action(icon, context.getString(title), commandIntent(command))

    private fun commandIntent(command: String): PendingIntent = PendingIntent.getService(
        context, command.hashCode(),
        Intent(context, PlaybackService::class.java).setAction(command),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openPlayerIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.mobile_playback_channel), NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "playback"
        const val ARTWORK_WIDTH = 512
        const val ARTWORK_HEIGHT = 288
    }
}
