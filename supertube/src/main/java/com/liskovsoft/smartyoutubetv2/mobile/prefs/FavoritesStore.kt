package com.liskovsoft.smartyoutubetv2.mobile.prefs

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import org.json.JSONArray
import org.json.JSONObject

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

data class FavoriteFolder(val id: Long, val name: String, val videos: List<FavoriteVideo>)

/**
 * User favorites organized in folders. Stored only on this device (JSON in SharedPreferences).
 * The folder list is Compose state: screens update as soon as something changes.
 */
class FavoritesStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("supertube_favorites", Context.MODE_PRIVATE)
    private val defaultName = context.getString(com.liskovsoft.smartyoutubetv2.mobile.R.string.mobile_favorites)

    val folders = mutableStateListOf<FavoriteFolder>().apply { addAll(load()) }

    init {
        if (folders.isEmpty()) {
            folders.add(FavoriteFolder(System.currentTimeMillis(), defaultName, emptyList()))
            save()
        }
    }

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
        val folder = FavoriteFolder(System.currentTimeMillis(), name.trim().ifEmpty { defaultName }, emptyList())
        folders.add(folder)
        save()
        return folder
    }

    fun renameFolder(folderId: Long, name: String) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1 || name.isBlank()) return
        folders[index] = folders[index].copy(name = name.trim())
        save()
    }

    fun deleteFolder(folderId: Long) {
        folders.removeAll { it.id == folderId }
        if (folders.isEmpty()) folders.add(FavoriteFolder(System.currentTimeMillis(), defaultName, emptyList()))
        save()
    }

    private fun save() {
        val json = JSONArray()
        folders.forEach { folder ->
            val videos = JSONArray()
            folder.videos.forEach { v ->
                videos.put(JSONObject().apply {
                    put("id", v.videoId); put("title", v.title); put("second", v.secondTitle); put("author", v.author)
                    put("thumb", v.thumbnail); put("badge", v.badge); put("channel", v.channelId); put("live", v.isLive)
                })
            }
            json.put(JSONObject().apply { put("id", folder.id); put("name", folder.name); put("videos", videos) })
        }
        prefs.edit().putString(KEY_FOLDERS, json.toString()).apply()
    }

    private fun load(): List<FavoriteFolder> = try {
        val json = JSONArray(prefs.getString(KEY_FOLDERS, "[]"))
        (0 until json.length()).map { i ->
            val folder = json.getJSONObject(i)
            val videos = folder.getJSONArray("videos")
            FavoriteFolder(
                folder.getLong("id"),
                folder.getString("name"),
                (0 until videos.length()).map { j ->
                    val v = videos.getJSONObject(j)
                    FavoriteVideo(
                        v.getString("id"), v.optStringOrNull("title"), v.optStringOrNull("second"), v.optStringOrNull("author"),
                        v.optStringOrNull("thumb"), v.optStringOrNull("badge"), v.optStringOrNull("channel"), v.optBoolean("live"),
                    )
                },
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

    companion object {
        private const val KEY_FOLDERS = "folders"

        /** Local section id for "Favorites" in the navigation (YouTube section ids are positive) */
        const val SECTION_ID = -1000

        @Volatile
        private var instance: FavoritesStore? = null

        fun instance(context: Context): FavoritesStore =
            instance ?: synchronized(this) { instance ?: FavoritesStore(context).also { instance = it } }
    }
}
