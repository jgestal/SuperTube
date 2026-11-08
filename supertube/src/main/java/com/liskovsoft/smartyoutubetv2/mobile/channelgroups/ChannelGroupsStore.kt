package com.liskovsoft.smartyoutubetv2.mobile.channelgroups

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.mediaserviceinterfaces.data.ItemGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.providers.channelgroup.ChannelGroupServiceWrapper
import com.liskovsoft.smartyoutubetv2.mobile.R
import java.io.BufferedInputStream

/** Result of an import: groups created and channels added (to new or existing groups) */
data class ImportResult(val groups: Int, val channels: Int)

/**
 * Channel groups of the phone app. They live in SmartTube's channel group service (the same storage the TV app and
 * SmartTube use, per signed-in profile), so backups and exports move between the apps unchanged.
 * [groups] is Compose state: screens update after every change.
 */
class ChannelGroupsStore private constructor(context: Context) {
    private val context = context.applicationContext
    private val service by lazy { ChannelGroupServiceWrapper.instance(this.context) }

    var groups by mutableStateOf<List<ChannelGroupData>>(emptyList())
        private set

    /** Re-reads the service (it changes with the signed-in profile and with the TV-style menus) */
    fun reload() {
        groups = service.channelGroups.map { it.toData() }
    }

    fun group(id: String?): ChannelGroupData? = groups.firstOrNull { it.id == id }

    fun groupsWith(channelId: String?): Set<String> =
        if (channelId == null) emptySet() else groups.filter { g -> g.channels.any { it.channelId == channelId } }.mapNotNull { it.id }.toSet()

    fun isNameTaken(title: String, exceptId: String? = null): Boolean =
        groups.any { it.id != exceptId && it.title.trim().equals(title.trim(), ignoreCase = true) }

    /** Creates a group (it goes first in the list). Returns its id. */
    fun create(title: String, channels: List<GroupChannel> = emptyList(), iconUrl: String? = null): String {
        val group = service.createChannelGroup(title.trim(), iconUrl, channels.map { service.createChannel(it.title, it.iconUrl, it.channelId) })
        service.addChannelGroup(group)
        reload()
        return group.id
    }

    fun rename(id: String, title: String) {
        val group = service.findChannelGroupById(id) ?: return
        service.renameChannelGroup(group, title.trim())
        reload()
    }

    fun delete(id: String) {
        val group = service.findChannelGroupById(id) ?: return
        service.removeChannelGroup(group)
        BrowsePresenter.instance(context).unpinItem(Video.from(group))
        reload()
    }

    fun addChannel(groupId: String, channel: GroupChannel) = addChannels(groupId, listOf(channel))

    fun addChannels(groupId: String, channels: List<GroupChannel>) {
        val group = service.findChannelGroupById(groupId) ?: return
        val missing = channels.filter { !group.contains(it.channelId) }.distinctBy { it.channelId }
        if (missing.isEmpty()) return
        // Appended in the given order (ItemGroup.add would put every channel first)
        group.addAll(missing.map { service.createChannel(it.title, it.iconUrl, it.channelId) })
        reload()
    }

    fun removeChannel(groupId: String, channelId: String) {
        val group = service.findChannelGroupById(groupId) ?: return
        group.remove(channelId)
        reload()
    }

    /** Adds the channel to the group or takes it out. Returns true when it's now in the group. */
    fun toggle(groupId: String, channel: GroupChannel): Boolean {
        val inGroup = group(groupId)?.channels?.any { it.channelId == channel.channelId } == true
        if (inGroup) removeChannel(groupId, channel.channelId) else addChannel(groupId, channel)
        return !inGroup
    }

    /** Fills channel names that came without one (PocketTube/GrayJay imports) with the authors seen in the feed */
    fun learnTitles(groupId: String, videos: List<Video>) {
        val data = group(groupId) ?: return
        val authors = videos.mapNotNull { v -> v.channelId?.let { id -> v.getAuthor()?.let { id to it } } }.toMap()
        val updates = ChannelGroupFormats.missingTitles(data.channels, authors)
        if (updates.isEmpty()) return
        val group = service.findChannelGroupById(groupId) ?: return
        val updated = group.items.map { item ->
            updates.firstOrNull { it.channelId == item.channelId }?.let { service.createChannel(it.title, item.iconUrl, item.channelId) } ?: item
        }
        group.addAll(updated) // replaces the items in place, keeping the order
        reload()
    }

    // ---- Import / export ----

    fun merge(incoming: List<ChannelGroupData>): ImportResult {
        reload()
        var created = 0
        var added = 0
        for (step in ChannelGroupFormats.planMerge(groups, incoming)) {
            if (step.targetId == null) {
                create(step.title, step.newChannels, step.iconUrl)
                created++
            } else {
                addChannels(step.targetId, step.newChannels)
            }
            added += step.newChannels.size
        }
        reload()
        return ImportResult(created, added)
    }

    /**
     * Imports a SmartTube / SuperTube TV backup zip, a SuperTube export or backup, or a GrayJay, PocketTube or
     * NewPipe file. Returns null when the file has no channel groups.
     */
    fun import(uri: Uri): ImportResult? {
        val incoming = context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            input.mark(8)
            val head = ByteArray(4)
            val read = input.read(head)
            input.reset()
            if (read == 4 && ChannelGroupFormats.isZip(head)) {
                ChannelGroupFormats.readBackupZip(input)?.let { ChannelGroupFormats.visibleGroups(it, context.getString(R.string.mobile_groups_smarttube_subscriptions)) }
            } else {
                ChannelGroupFormats.parseJson(input.bufferedReader().readText(), context.getString(R.string.mobile_groups_newpipe))
            }
        } ?: return null
        return merge(incoming)
    }

    fun exportJson(): String {
        reload()
        return ChannelGroupFormats.toExportJson(groups)
    }

    fun export(uri: Uri) {
        val json = exportJson()
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
    }

    private fun ItemGroup.toData() = ChannelGroupData(
        id, title, iconUrl,
        items.mapNotNull { item -> item.channelId?.let { GroupChannel(it, item.title, item.iconUrl) } }.distinctBy { it.channelId },
    )

    companion object {
        /** "More" section id (local, like favorites and downloads) */
        const val SECTION_ID = -1002

        @SuppressLint("StaticFieldLeak")
        private var instance: ChannelGroupsStore? = null

        fun instance(context: Context): ChannelGroupsStore =
            instance ?: ChannelGroupsStore(context).also { instance = it; it.reload() }
    }
}
