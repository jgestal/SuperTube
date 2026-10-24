package com.liskovsoft.smartyoutubetv2.mobile.prefs

import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStore
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup

/** How video lists are drawn. The user picks one from the top bar. */
enum class FeedLayout {
    /** Full width cards in a vertical feed (YouTube app style) */
    LIST,
    /** Small thumbnail on the left, text on the right: more videos per screen */
    COMPACT,
    /** Two columns of small cards on phones */
    GRID,
    /** Horizontal shelves (original TV-like layout) */
    CAROUSEL,
}

/**
 * Preferences that only exist in the mobile app (the shared ones live in the 'common' module).
 * Values are Compose state, so screens update as soon as they change.
 */
class MobilePrefs private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("supertube_prefs", Context.MODE_PRIVATE)

    var feedLayout by mutableStateOf(readLayout())
        private set

    /** Order of the sections listed in "More" (section ids). Unknown sections go after these. */
    val sectionOrder = mutableStateListOf<Int>().apply {
        val saved = readIntList(KEY_SECTION_ORDER)?.toMutableList() ?: DEFAULT_SECTION_ORDER.toMutableList()
        // Favorites was added later: place it right after History in orders saved before
        if (FavoritesStore.SECTION_ID !in saved) {
            val history = saved.indexOf(MediaGroup.TYPE_HISTORY)
            saved.add(if (history == -1) saved.size else history + 1, FavoritesStore.SECTION_ID)
        }
        if (DownloadStore.SECTION_ID !in saved) {
            saved.add(saved.indexOf(FavoritesStore.SECTION_ID) + 1, DownloadStore.SECTION_ID)
        }
        addAll(saved)
    }

    /** Most recent first */
    val recentSearches = mutableStateListOf<String>().apply { addAll(readStringList(KEY_RECENT_SEARCHES)) }

    fun updateFeedLayout(layout: FeedLayout) {
        feedLayout = layout
        prefs.edit().putString(KEY_FEED_LAYOUT, layout.name).apply()
    }

    fun setSectionOrder(ids: List<Int>) {
        sectionOrder.clear()
        sectionOrder.addAll(ids)
        prefs.edit().putString(KEY_SECTION_ORDER, ids.joinToString(",")).apply()
    }

    fun addRecentSearch(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        recentSearches.removeAll { it.equals(text, ignoreCase = true) }
        recentSearches.add(0, text)
        while (recentSearches.size > MAX_RECENT_SEARCHES) recentSearches.removeAt(recentSearches.lastIndex)
        saveRecentSearches()
    }

    fun removeRecentSearch(query: String) {
        recentSearches.remove(query)
        saveRecentSearches()
    }

    fun clearRecentSearches() {
        recentSearches.clear()
        saveRecentSearches()
    }

    private fun saveRecentSearches() {
        // Unit separator: search queries never contain it
        prefs.edit().putString(KEY_RECENT_SEARCHES, recentSearches.joinToString(SEPARATOR)).apply()
    }

    private fun readLayout(): FeedLayout =
        prefs.getString(KEY_FEED_LAYOUT, null)?.let { runCatching { FeedLayout.valueOf(it) }.getOrNull() } ?: FeedLayout.LIST

    private fun readIntList(key: String): List<Int>? =
        prefs.getString(key, null)?.split(",")?.mapNotNull { it.toIntOrNull() }

    private fun readStringList(key: String): List<String> =
        prefs.getString(key, null)?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    companion object {
        private const val KEY_FEED_LAYOUT = "feed_layout"
        private const val KEY_SECTION_ORDER = "section_order"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val SEPARATOR = "\u001F"
        private const val MAX_RECENT_SEARCHES = 20

        /** Mobile defaults: news, music and channels first, kids content at the end */
        private val DEFAULT_SECTION_ORDER = listOf(
            MediaGroup.TYPE_NEWS,
            MediaGroup.TYPE_MUSIC,
            MediaGroup.TYPE_CHANNEL_UPLOADS,
            MediaGroup.TYPE_HISTORY,
            FavoritesStore.SECTION_ID,
            DownloadStore.SECTION_ID,
            MediaGroup.TYPE_USER_PLAYLISTS,
            MediaGroup.TYPE_TRENDING,
            MediaGroup.TYPE_LIVE,
            MediaGroup.TYPE_SPORTS,
            MediaGroup.TYPE_GAMING,
            MediaGroup.TYPE_MY_VIDEOS,
            MediaGroup.TYPE_NOTIFICATIONS,
            MediaGroup.TYPE_PLAYBACK_QUEUE,
            MediaGroup.TYPE_BLOCKED_CHANNELS,
            MediaGroup.TYPE_KIDS_HOME,
        )

        @Volatile
        private var instance: MobilePrefs? = null

        fun instance(context: Context): MobilePrefs =
            instance ?: synchronized(this) { instance ?: MobilePrefs(context).also { instance = it } }
    }
}
