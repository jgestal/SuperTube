package com.liskovsoft.smartyoutubetv2.mobile.prefs

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateListOf
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import org.json.JSONArray
import com.liskovsoft.smartyoutubetv2.mobile.R
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/** Minimal copy of a video, enough to show it in a list and to play it */
data class FavoriteVideo(
    val videoId: String,
    val title: String?,
    val secondTitle: String?,
    val author: String?,
    val thumbnail: String?,
    val badge: String?,
    val channelId: String?,
    val isLive: Boolean,
) {
    fun toVideo(): Video = Video().also {
        it.videoId = videoId
        it.title = title
        it.secondTitle = secondTitle
        it.author = author
        it.cardImageUrl = thumbnail
        it.badge = badge
        it.channelId = channelId
        it.isLive = isLive
    }

    companion object {
        fun from(video: Video): FavoriteVideo? {
            val id = video.videoId ?: return null
            val thumbnail = video.getCardImageUrl() ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            // Player videos keep the real title in the metadata (getTitleFull)
            return FavoriteVideo(id, video.getTitleFull() ?: video.getTitle(), (video.getSecondTitleFull() ?: video.getSecondTitle())?.toString(), video.getAuthor(), thumbnail, video.badge, video.channelId, video.isLive)
        }
    }
}

/**
 * @param hasDefaultName the automatic "Favorites" folder: its name is stored empty and shown in the current language
 */
data class FavoriteFolder(val id: Long, val name: String, val videos: List<FavoriteVideo>, val hasDefaultName: Boolean = false)

/**
 * User favorites organized in folders. Stored only on this device (JSON in SharedPreferences).
 * The folder list is Compose state: screens update as soon as something changes.
 */
class FavoritesStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("supertube_favorites", Context.MODE_PRIVATE)
    private val defaultName = context.getString(R.string.mobile_favorites)

    /** The default name in every language of the app: folders saved by version 1.2 and older stored it translated */
    private val translatedDefaultNames: Set<String> by lazy {
        appLocales().mapNotNull { locale ->
            val config = Configuration(appContext.resources.configuration).apply { setLocale(locale) }
            runCatching { appContext.createConfigurationContext(config).getString(R.string.mobile_favorites) }.getOrNull()
        }.toSet()
    }

    val folders = mutableStateListOf<FavoriteFolder>().apply { addAll(load()) }

    init {
        if (folders.isEmpty()) {
            folders.add(defaultFolder())
            save()
        }
    }

    private fun defaultFolder() = FavoriteFolder(System.currentTimeMillis(), defaultName, emptyList(), hasDefaultName = true)

    fun isFavorite(videoId: String?): Boolean = videoId != null && folders.any { folder -> folder.videos.any { it.videoId == videoId } }

    fun isInFolder(videoId: String?, folderId: Long): Boolean =
        videoId != null && folders.firstOrNull { it.id == folderId }?.videos?.any { it.videoId == videoId } == true

    /** Adds or removes the video from the folder. Returns true when the video is now in the folder. */
    fun toggle(video: Video, folderId: Long): Boolean {
        val favorite = FavoriteVideo.from(video) ?: return false
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1) return false
        val folder = folders[index]
        val contains = folder.videos.any { it.videoId == favorite.videoId }
        // Newest first
        val videos = if (contains) folder.videos.filter { it.videoId != favorite.videoId } else listOf(favorite) + folder.videos
        folders[index] = folder.copy(videos = videos)
        save()
        return !contains
    }

    fun remove(videoId: String, folderId: Long) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1) return
        folders[index] = folders[index].copy(videos = folders[index].videos.filter { it.videoId != videoId })
        save()
    }

    /** "Remove from favorites": takes the video out of every folder */
    fun removeEverywhere(videoId: String) {
        for (i in folders.indices) {
            val folder = folders[i]
            if (folder.videos.any { it.videoId == videoId }) folders[i] = folder.copy(videos = folder.videos.filter { it.videoId != videoId })
        }
        save()
    }

    fun createFolder(name: String): FavoriteFolder {
        val trimmed = name.trim()
        val folder = FavoriteFolder(System.currentTimeMillis(), trimmed.ifEmpty { defaultName }, emptyList(), hasDefaultName = trimmed.isEmpty())
        folders.add(folder)
        save()
        return folder
    }

    fun renameFolder(folderId: Long, name: String) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1 || name.isBlank()) return
        folders[index] = folders[index].copy(name = name.trim(), hasDefaultName = false)
        save()
    }

    fun deleteFolder(folderId: Long) {
        folders.removeAll { it.id == folderId }
        if (folders.isEmpty()) folders.add(defaultFolder())
        save()
    }

    private fun save() {
        prefs.edit().putString(KEY_FOLDERS, toJson(folders).toString()).apply()
        onChanged?.invoke()
    }

    /** All folders as JSON (also used by the export) */
    fun exportJson(): JSONArray = toJson(folders)

    /**
     * Merges an exported list: folders are matched by name, videos already present are skipped.
     * Returns the number of videos added.
     */
    fun importJson(json: JSONArray): Int {
        var added = 0
        for (incoming in parse(json)) {
            val index = folders.indexOfFirst {
                if (incoming.hasDefaultName) it.hasDefaultName else !it.hasDefaultName && it.name.equals(incoming.name, ignoreCase = true)
            }
            if (index == -1) {
                folders.add(incoming.copy(id = System.currentTimeMillis() + folders.size))
                added += incoming.videos.size
            } else {
                val existing = folders[index]
                val fresh = incoming.videos.filter { v -> existing.videos.none { it.videoId == v.videoId } }
                folders[index] = existing.copy(videos = existing.videos + fresh)
                added += fresh.size
            }
        }
        save()
        return added
    }

    private fun toJson(list: List<FavoriteFolder>): JSONArray {
        val json = JSONArray()
        list.forEach { folder ->
            val videos = JSONArray()
            folder.videos.forEach { v ->
                videos.put(JSONObject().apply {
                    put("id", v.videoId); put("title", v.title); put("second", v.secondTitle); put("author", v.author)
                    put("thumb", v.thumbnail); put("badge", v.badge); put("channel", v.channelId); put("live", v.isLive)
                })
            }
            json.put(JSONObject().apply { put("id", folder.id); put("name", if (folder.hasDefaultName) "" else folder.name); put("videos", videos) })
        }
        return json
    }

    private fun load(): List<FavoriteFolder> = try {
        parse(JSONArray(prefs.getString(KEY_FOLDERS, "[]")))
    } catch (e: Exception) {
        emptyList()
    }

    private fun parse(json: JSONArray): List<FavoriteFolder> =
        (0 until json.length()).map { i ->
            val folder = json.getJSONObject(i)
            val videos = folder.getJSONArray("videos")
            val name = folder.getString("name")
            val hasDefaultName = name.isEmpty() || name in translatedDefaultNames
            FavoriteFolder(
                folder.getLong("id"),
                if (hasDefaultName) defaultName else name,
                (0 until videos.length()).map { j ->
                    val v = videos.getJSONObject(j)
                    FavoriteVideo(
                        v.getString("id"), v.optStringOrNull("title"), v.optStringOrNull("second"), v.optStringOrNull("author"),
                        v.optStringOrNull("thumb"), v.optStringOrNull("badge"), v.optStringOrNull("channel"), v.optBoolean("live"),
                    )
                },
                hasDefaultName,
            )
        }

    /** Languages of the app, from the locale config (the same list the system language settings show) */
    private fun appLocales(): List<Locale> {
        val locales = mutableListOf<Locale>()
        appContext.resources.getXml(R.xml.locales_config).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue(ANDROID_NAMESPACE, "name")?.let { locales.add(Locale.forLanguageTag(it)) }
                }
            }
        }
        return locales
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

    companion object {
        private const val KEY_FOLDERS = "folders"
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

        /** Called after every change (home screen widget refresh) */
        var onChanged: (() -> Unit)? = null

        /** Local section id for "Favorites" in the navigation (YouTube section ids are positive) */
        const val SECTION_ID = -1000

        @Volatile
        private var instance: FavoritesStore? = null

        fun instance(context: Context): FavoritesStore =
            instance ?: synchronized(this) { instance ?: FavoritesStore(context).also { instance = it } }
    }
}
