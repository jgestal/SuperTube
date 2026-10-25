package com.liskovsoft.smartyoutubetv2.mobile.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.mobile.BuildConfig
import com.liskovsoft.smartyoutubetv2.mobile.R
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Updates from the SuperTube GitHub releases (the SmartTube updater is disabled in this fork).
 * Checks at most once a day on start (or on demand), shows the release notes, downloads the APK
 * and opens the Android package installer.
 */
object UpdateChecker {
    private const val LATEST_RELEASE = "https://api.github.com/repos/jgestal/SuperTube/releases/latest"
    private const val PREFS = "supertube_update"
    private const val KEY_LAST_CHECK = "last_check"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private val main = Handler(Looper.getMainLooper())

    private class Release(val version: String, val notes: String, val apkUrl: String)

    fun checkOnStart(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_CHECK, 0) < DAY_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        check(activity, manual = false)
    }

    fun check(activity: Activity, manual: Boolean) {
        if (manual) MessageHelpers.showMessage(activity, activity.getString(R.string.mobile_update_checking))
        Thread {
            val release = try { fetchLatest() } catch (e: Exception) { null }
            main.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                when {
                    release == null -> if (manual) MessageHelpers.showMessage(activity, activity.getString(R.string.mobile_update_error))
                    isNewer(release.version, BuildConfig.VERSION_NAME) -> showDialog(activity, release)
                    manual -> MessageHelpers.showMessage(activity, activity.getString(R.string.mobile_update_latest, BuildConfig.VERSION_NAME))
                }
            }
        }.start()
    }

    private fun fetchLatest(): Release? {
        val connection = (URL(LATEST_RELEASE).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (connection.responseCode != 200) return null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk") } ?: return null
            return Release(
                version = json.getString("tag_name").removePrefix("v"),
                notes = json.optString("body").replace(Regex("[#*`>]"), "").trim(),
                apkUrl = apk.getString("browser_download_url"),
            )
        } finally {
            connection.disconnect()
        }
    }

    /** "1.10" > "1.9", "1.0.1" > "1.0" */
    internal fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.split(".", "-").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun showDialog(activity: Activity, release: Release) {
        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(activity.getString(R.string.mobile_update_available, release.version))
            .setMessage(release.notes.take(1500))
            .setPositiveButton(R.string.mobile_update_install) { _, _ -> download(activity, release) }
            .setNegativeButton(R.string.mobile_update_later, null)
            .show()
    }

    private fun download(activity: Activity, release: Release) {
        MessageHelpers.showMessage(activity, activity.getString(R.string.mobile_update_downloading))
        val context = activity.applicationContext
        Thread {
            try {
                val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val file = File(dir, "SuperTube-${release.version}.apk")
                val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                connection.disconnect()
                main.post { install(activity, file) }
            } catch (e: Exception) {
                main.post { MessageHelpers.showMessage(context, context.getString(R.string.mobile_update_error)) }
            }
        }.start()
    }

    private fun install(activity: Activity, file: File) {
        // Android 8+: the user must allow SuperTube to install apps once
        if (!activity.packageManager.canRequestPackageInstalls()) {
            MessageHelpers.showLongMessage(activity, activity.getString(R.string.mobile_update_permission))
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
        activity.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
