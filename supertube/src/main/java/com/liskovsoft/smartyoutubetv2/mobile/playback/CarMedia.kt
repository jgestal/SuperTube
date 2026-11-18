package com.liskovsoft.smartyoutubetv2.mobile.playback

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

/**
 * Android Auto gives the playback buttons only to the media apps it knows: the ones with a media browser service
 * and the "media" use in `automotive_app_desc.xml`. Without them SuperTube was a foreign app in the car and the
 * previous/next buttons were greyed out, whatever the session offered.
 */
object CarMedia {
    /** Root of the car's menu: empty, the car is a remote for what plays on the phone */
    const val ROOT_ID = "supertube_root"

    /** Android Auto (also the Desktop Head Unit) and the media center of Android Automotive cars */
    val CAR_CLIENTS = setOf(
        "com.google.android.projection.gearhead",
        "com.android.car.media",
    )

    /** Who may connect to the service: the cars and the app itself, not any app of the phone */
    fun isAllowedClient(clientPackage: String?, ownPackage: String): Boolean =
        clientPackage != null && (clientPackage == ownPackage || clientPackage in CAR_CLIENTS)
}

/**
 * Which player owns the shared session. When a player is replaced, the old one releasing late must not switch off
 * the session of the new one.
 */
class SessionOwnership {
    private var owner: Any? = null

    fun claim(newOwner: Any) {
        owner = newOwner
    }

    fun isOwner(candidate: Any): Boolean = owner === candidate

    /** True when [leaving] owned the session: then the session is left without player */
    fun release(leaving: Any): Boolean {
        if (owner !== leaving) return false
        owner = null
        return true
    }
}

/**
 * The media session of the app, alive as long as the process. Android Auto connects to it through [CarMediaService]
 * (its token can be given only once), and every player engine plugs into it while it plays.
 */
object SharedMediaSession {
    private var session: MediaSessionCompat? = null
    private val ownership = SessionOwnership()

    fun get(context: Context): MediaSessionCompat = session ?: MediaSessionCompat(context.applicationContext, context.packageName).also {
        // Keeps the slots next to play/pause for previous/next in Android Auto (see SessionControls)
        it.setExtras(Bundle().apply { SessionControls.SLOT_RESERVATION_KEYS.forEach { key -> putBoolean(key, true) } })
        session = it
    }

    fun claim(owner: Any) = ownership.claim(owner)

    fun release(owner: Any): Boolean = ownership.release(owner)
}

/** What Android Auto connects to: the app's media session and an empty menu */
class CarMediaService : MediaBrowserServiceCompat() {
    override fun onCreate() {
        super.onCreate()
        sessionToken = SharedMediaSession.get(this).sessionToken
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
        if (CarMedia.isAllowedClient(clientPackageName, packageName)) BrowserRoot(CarMedia.ROOT_ID, null) else null

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }
}
