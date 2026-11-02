package com.liskovsoft.smartyoutubetv2.mobile.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Opening YouTube links in SuperTube.
 *
 * The manifest already declares the youtube.com / youtu.be filters, but since Android 12 http(s) links only open
 * in an app that verified the domain (YouTube does, we can't) or that the user enabled by hand in
 * "Open by default > Add links". Until then the system sends them to the browser. If the YouTube app is installed
 * its verified links win over the user selection, so its "Open supported links" has to be turned off too.
 */
object YouTubeLinks {
    /** Same hosts as the SplashActivity intent filter */
    private val HOSTS = listOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")

    /** Below Android 12 the system asks which app to use, so there is nothing to set up */
    val needsSetup: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    enum class State { ENABLED, DISABLED, TAKEN_BY_YOUTUBE }

    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    fun state(context: Context): State = when {
        resolvesToUs(context) -> State.ENABLED
        // Our links are selected but the YouTube app has them verified and wins: it must stop opening them
        hostsSelected(context) && isYouTubeInstalled(context) -> State.TAKEN_BY_YOUTUBE
        else -> State.DISABLED
    }

    /** What the system does with a YouTube link right now */
    private fun resolvesToUs(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ")).addCategory(Intent.CATEGORY_BROWSABLE)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    private fun hostsSelected(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(DomainVerificationManager::class.java) ?: return false
        val state = try {
            manager.getDomainVerificationUserState(context.packageName)
        } catch (e: Exception) {
            null
        } ?: return false
        if (!state.isLinkHandlingAllowed) return false
        val hosts = state.hostToStateMap
        return HOSTS.all { hosts[it] == DomainVerificationUserState.DOMAIN_STATE_SELECTED || hosts[it] == DomainVerificationUserState.DOMAIN_STATE_VERIFIED }
    }

    private fun isYouTubeInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(YOUTUBE_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * System "Open by default" screen: ours to add the YouTube links, or the YouTube app's one to turn off
     * "Open supported links" when it keeps taking them.
     */
    fun openSettings(context: Context) {
        val pkg = if (state(context) == State.TAKEN_BY_YOUTUBE) YOUTUBE_PACKAGE else context.packageName
        val uri = Uri.parse("package:$pkg")
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri))
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)) // some OEMs don't implement the screen above
        }
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next one
            }
        }
    }
}
