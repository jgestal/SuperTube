package com.liskovsoft.smartyoutubetv2.mobile.prefs

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * Export / import of the SuperTube data (favorites and mobile settings) as a JSON file chosen by the user.
 * SmartTube's own settings keep their backup in Settings > Backup/restore.
 */
object BackupManager {
    private const val FORMAT = "supertube-backup"

    fun export(context: Context, uri: Uri) {
        val json = JSONObject().apply {
            put("format", FORMAT)
            put("version", 1)
            put("createdAt", System.currentTimeMillis())
            put("favorites", FavoritesStore.instance(context).exportJson())
            put("settings", MobilePrefs.instance(context).exportJson())
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toString(2).toByteArray()) }
    }

    /** Returns the number of favorites added, or -1 when the file isn't a SuperTube backup */
    fun import(context: Context, uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return -1
        val json = try { JSONObject(text) } catch (e: Exception) { return -1 }
        if (json.optString("format") != FORMAT) return -1
        json.optJSONObject("settings")?.let { MobilePrefs.instance(context).importJson(it) }
        return json.optJSONArray("favorites")?.let { FavoritesStore.instance(context).importJson(it) } ?: 0
    }
}
