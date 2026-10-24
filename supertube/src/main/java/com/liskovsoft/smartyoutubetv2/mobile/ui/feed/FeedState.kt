package com.liskovsoft.smartyoutubetv2.mobile.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup

/**
 * Compose counterpart of the TV VideoGroupObjectAdapter: one horizontal shelf (or the whole grid).
 * Mirrors its append/prepend/remove/sync semantics because presenters rely on them.
 */
class VideoRowState(val id: Int, title: String?, val isShorts: Boolean) {
    var title by mutableStateOf(title)
    val items = mutableStateListOf<Video>()
    private val groups = mutableListOf<VideoGroup>()

    /** Bumped when videos are mutated in place (sync), so cards re-read their fields */
    var revision by mutableIntStateOf(0)
        private set

    fun add(group: VideoGroup) {
        val videos = group.videos ?: return

        if (group.action == VideoGroup.ACTION_PREPEND) {
            val begin = items.size
            if (groups.contains(group)) {
                if (videos.size > begin) items.addAll(0, videos.subList(begin, videos.size))
            } else {
                items.addAll(0, videos)
                groups.add(0, group)
            }
        } else {
            val begin = items.size
            // Presenters may reuse the same group object and grow it (continuation)
            if (groups.contains(group)) {
                if (begin > videos.size) items.addAll(videos) else items.addAll(videos.subList(begin, videos.size))
            } else {
                items.addAll(videos)
                groups.add(group)
            }
        }
    }

    fun remove(group: VideoGroup) {
        val videos = group.videos ?: return
        for (video in videos) {
            while (items.remove(video)) {
                video.group?.remove(video)
            }
        }
    }

    fun removeAuthor(group: VideoGroup) {
        val author = group.videos?.firstOrNull()?.getAuthor() ?: return
        items.removeAll { Helpers.equals(author, it.getAuthor()) }
    }

    fun sync(group: VideoGroup) {
        val videos = group.videos ?: return
        var changed = false
        for (video in videos) {
            for (origin in items) {
                if (origin == video) {
                    origin.sync(video)
                    changed = true
                }
            }
        }
        if (changed) revision++
    }

    fun clear() {
        items.clear()
        groups.clear()
    }

    val lastVideo: Video? get() = items.lastOrNull()
}

/**
 * Holds the content of one screen: either a single grid (grid sections, search, uploads)
 * or a list of shelves (home, channel, playlists).
 */
class FeedState {
    val rows = mutableStateListOf<VideoRowState>()
    var isLoading by mutableStateOf(false)

    val isEmpty: Boolean get() = rows.all { it.items.isEmpty() }

    /** Single list semantics (TV VideoGridFragment) */
    fun updateGrid(group: VideoGroup) {
        val row = rows.firstOrNull() ?: VideoRowState(group.id, group.title, group.isShorts).also { rows.add(it) }

        when (group.action) {
            VideoGroup.ACTION_REPLACE -> row.clear()
            VideoGroup.ACTION_REMOVE -> { row.remove(group); return }
            VideoGroup.ACTION_REMOVE_AUTHOR -> { row.removeAuthor(group); return }
            VideoGroup.ACTION_SYNC -> { row.sync(group); return }
        }

        if (group.isEmpty) return

        row.add(group)
    }

    /** Multiple shelves semantics (TV MultipleRowsFragment) */
    fun updateRows(group: VideoGroup) {
        when (group.action) {
            VideoGroup.ACTION_REPLACE -> if (group.position == -1) rows.clear() else rows.removeAll { it.id == group.id }
            VideoGroup.ACTION_REMOVE -> { findRow(group.id)?.remove(group); return }
            VideoGroup.ACTION_REMOVE_AUTHOR -> { rows.forEach { it.removeAuthor(group) }; return }
            VideoGroup.ACTION_SYNC -> { findRow(group.id)?.sync(group); return }
        }

        if (group.isEmpty) return

        val existing = findRow(group.id)

        if (existing == null) {
            val row = VideoRowState(group.id, group.title, group.isShorts)
            row.add(group)
            if (group.position == -1 || group.position > rows.size) rows.add(row) else rows.add(group.position, row)
        } else {
            existing.add(group)
        }
    }

    fun clear() {
        rows.clear()
    }

    private fun findRow(id: Int): VideoRowState? = rows.firstOrNull { it.id == id }
}
