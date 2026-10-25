package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.liskovsoft.sharedutils.mylogger.Log

/** Actions sent by the playback notification (and handled by the player that owns the session) */
interface PlaybackCommands {
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun stop()
}

/**
 * Foreground service (type mediaPlayback) that keeps the process alive while a video plays with the player in the
 * background, the screen off or in the mini player. Without it Android freezes or kills the app and, since Android 17,
 * background audio fails silently. It owns no player: it shows the notification built by [PlaybackMediaSession] and
 * forwards the notification actions to the current [PlaybackCommands].
 */
class PlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Mandatory after startForegroundService(), also for the action intents
        val notification = currentNotification
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(notification)

        val commands = commands
        when (intent?.action) {
            ACTION_PLAY -> commands?.play()
            ACTION_PAUSE -> commands?.pause()
            ACTION_NEXT -> commands?.next()
            ACTION_PREVIOUS -> commands?.previous()
            ACTION_STOP -> commands?.stop()
        }
        // Don't restart without a player behind it
        return START_NOT_STICKY
    }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private val TAG = PlaybackService::class.java.simpleName
        const val NOTIFICATION_ID = 4343
        const val ACTION_PLAY = "app.supertube.playback.PLAY"
        const val ACTION_PAUSE = "app.supertube.playback.PAUSE"
        const val ACTION_NEXT = "app.supertube.playback.NEXT"
        const val ACTION_PREVIOUS = "app.supertube.playback.PREVIOUS"
        const val ACTION_STOP = "app.supertube.playback.STOP"

        private val mainHandler = Handler(Looper.getMainLooper())
        private var pendingStop: Runnable? = null
        private var isStarted = false

        /** Set by the active [PlaybackMediaSession] */
        internal var commands: PlaybackCommands? = null
        internal var currentNotification: Notification? = null

        /**
         * Must be called while the player is visible: a foreground service started from the background is refused
         * (Android 12+) and doesn't get the while-in-use capabilities required for background audio (Android 17).
         */
        fun start(context: Context) {
            pendingStop?.let(mainHandler::removeCallbacks)
            pendingStop = null
            if (isStarted) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
                isStarted = true
            } catch (e: IllegalStateException) { // ForegroundServiceStartNotAllowedException
                Log.e(TAG, "Can't start the playback service: %s", e.message)
            }
        }

        /** Deferred, so an engine restart (release + init in a row) keeps the service and its foreground state */
        fun stop(context: Context) {
            if (pendingStop != null) return
            val appContext = context.applicationContext
            pendingStop = Runnable {
                pendingStop = null
                isStarted = false
                currentNotification = null
                appContext.stopService(Intent(appContext, PlaybackService::class.java))
                // Also when the service couldn't start and the notification was only posted
                NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
            }.also { mainHandler.post(it) }
        }
    }
}
