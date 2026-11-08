package com.liskovsoft.smartyoutubetv2.mobile.channelgroups

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.zip.ZipInputStream

/** A channel inside a group. Title and avatar can be missing (PocketTube/GrayJay exports only carry the ids). */
data class GroupChannel(val channelId: String, val title: String? = null, val iconUrl: String? = null)

/** Plain copy of a channel group (SmartTube's ItemGroup), independent from Android and from the service */
data class ChannelGroupData(val id: String?, val title: String, val iconUrl: String?, val channels: List<GroupChannel>)

/** What an import does to an existing group (targetId) or a new one (targetId == null) */
data class MergeStep(val targetId: String?, val title: String, val iconUrl: String?, val newChannels: List<GroupChannel>)

/**
 * Formats used to move channel groups between SuperTube (phone and TV) and SmartTube.
 *
 * - SmartTube's own storage (`files/yt_service_prefs/<profile>_channel_group_data`, also inside its backup zips).
 * - The SuperTube export: a GrayJay-compatible JSON array, so SmartTube and SuperTube TV can import it with
 *   "Import (GrayJay/PocketTube/NewPipe)". Every group also carries "channels" with names and avatars.
 * - GrayJay, PocketTube and NewPipe exports.
 *
 * Kept free of Android classes so it runs in plain unit tests.
 */
object ChannelGroupFormats {
    /** Hidden SmartTube groups: local subscriptions (signed out) and notifications */
    const val SMARTTUBE_SUBSCRIPTIONS_ID = "1000"
    const val SMARTTUBE_NOTIFICATIONS_ID = "1001"

    // SmartTube delimiters (Helpers.ARRAY_DELIM, ItemGroupImpl, ItemImpl)
    private const val GROUPS_DELIM = "%AR%"
    private const val GROUP_DELIM = "&sgi;"
    private const val ITEMS_DELIM = "&sga;"
    private const val ITEM_DELIM = "&ci;"
    private const val NULL = "null"

    private const val DATA_SUFFIX = "_channel_group_data"
    private const val PREFS_XML = "yt_service_prefs.xml"
    private const val FORMAT = "supertube-channel-groups"

    private val CHANNEL_ID = Regex("^UC[\\w-]{22}$")
    private val CHANNEL_URL = Regex("/channel/(UC[\\w-]{22})")

    // ---- SmartTube storage ----

    /** Parses SmartTube's stored channel groups. Items without a channel (playlists, videos) are skipped. */
    fun parseSmartTube(raw: String?): List<ChannelGroupData> {
        if (raw.isNullOrBlank()) return emptyList()
        return split(raw, GROUPS_DELIM).mapNotNull { spec ->
            val fields = split(spec, GROUP_DELIM)
            val id = field(fields, 0) ?: return@mapNotNull null
            val title = field(fields, 1) ?: return@mapNotNull null
            val channels = field(fields, 3)?.let { items -> split(items, ITEMS_DELIM).mapNotNull(::parseSmartTubeItem) } ?: emptyList()
            ChannelGroupData(id, title, field(fields, 2), channels.distinctBy { it.channelId })
        }
    }

    private fun parseSmartTubeItem(spec: String): GroupChannel? {
        val fields = split(spec, ITEM_DELIM)
        val channelId = field(fields, 2) ?: return null
        return GroupChannel(channelId, field(fields, 0), field(fields, 1))
    }

    /** Same text SmartTube writes (used by the tests and to keep the format documented in code) */
    fun toSmartTube(groups: List<ChannelGroupData>): String {
        if (groups.isEmpty()) return " "
        return groups.joinToString(GROUPS_DELIM) { group ->
            val items = if (group.channels.isEmpty()) " " else group.channels.joinToString(ITEMS_DELIM) { channel ->
                listOf(channel.title, channel.iconUrl, channel.channelId, null, null, null, -1).joinToString(ITEM_DELIM) { it?.toString() ?: NULL }
            }
            listOf(group.id, group.title, group.iconUrl, items, null).joinToString(GROUP_DELIM) { it ?: NULL }
        }
    }

    /**
     * SmartTube's hidden groups as seen by the user: local subscriptions become a normal group named
     * [subscriptionsTitle], notifications are dropped.
     */
    fun visibleGroups(groups: List<ChannelGroupData>, subscriptionsTitle: String): List<ChannelGroupData> = groups.mapNotNull {
        when (it.id) {
            SMARTTUBE_NOTIFICATIONS_ID -> null
            SMARTTUBE_SUBSCRIPTIONS_ID -> if (it.channels.isEmpty()) null else it.copy(id = null, title = subscriptionsTitle)
            else -> it
        }
    }

    // Java's String.split semantics, like SmartTube's Helpers.split (a single space means empty)
    private fun split(data: String, delim: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(delim).dropLastWhile { it.isEmpty() }

    private fun field(fields: List<String>, index: Int): String? = fields.getOrNull(index)?.takeIf { it != NULL }

    // ---- Backup zips (SmartTube / SuperTube TV) ----

    /**
     * Channel groups inside a SmartTube or SuperTube TV backup zip, from every profile in it.
     * Returns null when the zip has no channel group data.
     */
    fun readBackupZip(input: InputStream): List<ChannelGroupData>? {
        val result = mutableListOf<ChannelGroupData>()
        var found = false
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                when {
                    entry.isDirectory -> Unit
                    name.endsWith(DATA_SUFFIX) -> {
                        found = true
                        result += parseSmartTube(zip.readBytes().toString(Charsets.UTF_8))
                    }
                    // Older versions kept the data inside the shared preferences file
                    name == PREFS_XML -> {
                        val groups = readPrefsXml(zip.readBytes().toString(Charsets.UTF_8))
                        if (groups != null) {
                            found = true
                            result += groups
                        }
                    }
                }
            }
        }
        return if (found) result else null
    }

    private val PREFS_ENTRY = Regex("<string name=\"[^\"]*$DATA_SUFFIX\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)

    internal fun readPrefsXml(xml: String): List<ChannelGroupData>? {
        val matches = PREFS_ENTRY.findAll(xml).toList()
        if (matches.isEmpty()) return null
        return matches.flatMap { parseSmartTube(unescapeXml(it.groupValues[1])) }
    }

    private fun unescapeXml(text: String): String = text
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
        .replace("&amp;", "&")

    fun isZip(head: ByteArray): Boolean = head.size >= 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() && head[2].toInt() == 3 && head[3].toInt() == 4

    // ---- SuperTube export (GrayJay compatible) ----

    /**
     * Written by hand: SmartTube's GrayJay importer strips the quotes around embedded objects ("{ -> {, }" -> {,
     * \" -> ") before parsing. Texts escape quotes, backslashes and braces as \u sequences, so that cleanup never
     * matches inside them.
     */
    fun toExportJson(groups: List<ChannelGroupData>): String = buildString {
        append("[\n")
        groups.forEachIndexed { index, group ->
            if (index > 0) append(",\n")
            append("  {\n")
            append("    \"format\":").append(jsonText(FORMAT)).append(",\n")
            append("    \"id\":").append(jsonText(group.id ?: "supertube-$index")).append(",\n")
            append("    \"name\":").append(jsonText(group.title)).append(",\n")
            append("    \"image\":{\"url\":").append(jsonText(group.iconUrl)).append("},\n")
            append("    \"urls\":[").append(group.channels.joinToString(",") { jsonText("https://www.youtube.com/channel/${it.channelId}") }).append("],\n")
            append("    \"creationTime\":0,\n")
            append("    \"channels\":[")
            append(group.channels.joinToString(",") { channel ->
                "\n      {\"channelId\":${jsonText(channel.channelId)},\"title\":${jsonText(channel.title)},\"iconUrl\":${jsonText(channel.iconUrl)}}"
            })
            if (group.channels.isNotEmpty()) append("\n    ")
            append("]\n  }")
        }
        append("\n]\n")
    }

    private fun jsonText(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder("\"")
        for (c in value) {
            if (c == '"' || c == '\\' || c == '{' || c == '}' || c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
        }
        return sb.append('"').toString()
    }

    // ---- Import of any supported JSON ----

    /**
     * Groups from a SuperTube export, a SuperTube phone backup, GrayJay, PocketTube or NewPipe.
     * [newPipeTitle] names the single group made from a NewPipe subscriptions export. Null when not recognized.
     */
    fun parseJson(text: String, newPipeTitle: String): List<ChannelGroupData>? {
        val trimmed = text.trim().removePrefix("\uFEFF")
        return try {
            when {
                trimmed.startsWith("[") -> parseGroupArray(JSONArray(trimmed))
                trimmed.startsWith("{") -> {
                    val json = JSONObject(trimmed)
                    when {
                        json.has("channelGroups") -> parseGroupArray(json.getJSONArray("channelGroups"))
                        json.has("ysc_collection") -> parsePocketTube(json)
                        json.has("subscriptions") -> parseNewPipe(json, newPipeTitle)
                        else -> null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** SuperTube export or GrayJay (where every group may be a JSON object or a JSON string holding one) */
    private fun parseGroupArray(array: JSONArray): List<ChannelGroupData>? {
        val result = mutableListOf<ChannelGroupData>()
        for (i in 0 until array.length()) {
            val group = when (val item = array.get(i)) {
                is JSONObject -> item
                is String -> JSONObject(item)
                else -> return null
            }
            val title = group.optString("name").ifBlank { group.optString("title") }.ifBlank { return null }
            val channels = mutableListOf<GroupChannel>()
            group.optJSONArray("channels")?.let { list ->
                for (j in 0 until list.length()) {
                    val channel = list.optJSONObject(j) ?: continue
                    val id = channel.optString("channelId").takeIf { CHANNEL_ID.matches(it) } ?: continue
                    channels += GroupChannel(id, channel.optStringOrNull("title"), channel.optStringOrNull("iconUrl"))
                }
            }
            group.optJSONArray("urls")?.let { urls ->
                for (j in 0 until urls.length()) {
                    channelIdFromUrl(urls.optString(j))?.let { id -> if (channels.none { it.channelId == id }) channels += GroupChannel(id) }
                }
            }
            val icon = group.optJSONObject("image")?.optStringOrNull("url") ?: group.optStringOrNull("iconUrl")
            result += ChannelGroupData(null, title.trim(), icon, channels)
        }
        return result
    }

    /** PocketTube: {"ysc_collection": {...}, "<group>": ["UC...", ...]} */
    private fun parsePocketTube(json: JSONObject): List<ChannelGroupData> {
        val names = json.getJSONObject("ysc_collection").keys().asSequence().toList()
        return names.mapNotNull { name ->
            val ids = json.optJSONArray(name) ?: return@mapNotNull null
            val channels = (0 until ids.length()).mapNotNull { channelIdFromUrl(ids.optString(it)) }.distinct().map { GroupChannel(it) }
            ChannelGroupData(null, name, null, channels)
        }
    }

    /** NewPipe: {"subscriptions": [{"url": ".../channel/UC...", "name": "..."}]} */
    private fun parseNewPipe(json: JSONObject, title: String): List<ChannelGroupData> {
        val list = json.getJSONArray("subscriptions")
        val channels = (0 until list.length()).mapNotNull { i ->
            val item = list.optJSONObject(i) ?: return@mapNotNull null
            channelIdFromUrl(item.optString("url"))?.let { GroupChannel(it, item.optStringOrNull("name")) }
        }.distinctBy { it.channelId }
        return listOf(ChannelGroupData(null, title, null, channels))
    }

    /** Channel id from a bare id or a youtube.com/channel/ URL */
    fun channelIdFromUrl(text: String?): String? {
        val value = text?.trim() ?: return null
        if (CHANNEL_ID.matches(value)) return value
        return CHANNEL_URL.find(value)?.groupValues?.get(1)
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    // ---- Merge ----

    /**
     * How to bring [incoming] into [existing]: a group with the same name (ignoring case) receives the channels it
     * lacks, the other ones are created. Steps that add nothing are left out.
     */
    fun planMerge(existing: List<ChannelGroupData>, incoming: List<ChannelGroupData>): List<MergeStep> {
        val steps = mutableListOf<MergeStep>()
        for (group in incoming) {
            val title = group.title.trim()
            if (title.isEmpty()) continue
            val key = title.lowercase()
            val target = existing.firstOrNull { it.title.trim().lowercase() == key }
            // Several incoming groups with the same name (e.g. two profiles) go into one step
            val pending = steps.indexOfFirst { it.title.lowercase() == key }
            val known = (target?.channels.orEmpty() + steps.getOrNull(pending)?.newChannels.orEmpty()).map { it.channelId }.toSet()
            val added = group.channels.distinctBy { it.channelId }.filter { it.channelId !in known }
            when {
                pending >= 0 -> steps[pending] = steps[pending].copy(newChannels = steps[pending].newChannels + added)
                target != null -> if (added.isNotEmpty()) steps += MergeStep(target.id, target.title, target.iconUrl, added)
                else -> steps += MergeStep(null, title, group.iconUrl, added)
            }
        }
        return steps
    }

    /**
     * Channel names learned from the group feed (RSS videos carry the author): fills the channels that have no name.
     * Returns only the channels that changed.
     */
    fun missingTitles(channels: List<GroupChannel>, authors: Map<String, String>): List<GroupChannel> =
        channels.filter { it.title.isNullOrBlank() }.mapNotNull { channel -> authors[channel.channelId]?.takeIf { it.isNotBlank() }?.let { channel.copy(title = it) } }
}
