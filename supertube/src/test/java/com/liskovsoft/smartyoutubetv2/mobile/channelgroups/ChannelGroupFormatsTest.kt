package com.liskovsoft.smartyoutubetv2.mobile.channelgroups

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ChannelGroupFormatsTest {
    private val music = "UCaaaaaaaaaaaaaaaaaaaaaa"
    private val cooking = "UCbbbbbbbbbbbbbbbbbbbbbb"
    private val news = "UCcc-ccccccccccccccc_ccc"

    /**
     * Exactly what SmartTube 2x writes to files/yt_service_prefs/<profile>_channel_group_data
     * (ItemGroupImpl.toString joined with Helpers.ARRAY_DELIM; items are ItemImpl.toString).
     */
    private val smartTubeRaw = listOf(
        "x1Y2z3A4b5C6&sgi;Música&sgi;null&sgi;" +
            "Rock Channel&ci;https://yt3.ggpht.com/rock=s88&ci;$music&ci;null&ci;null&ci;null&ci;-1" +
            "&sga;" +
            "Cocina&ci;null&ci;$cooking&ci;null&ci;null&ci;null&ci;-1" +
            "&sgi;null",
        // A playlist item (no channel) must be ignored
        "q9w8e7r6t5y4&sgi;Mixed&sgi;https://icon&sgi;Some video&ci;null&ci;null&ci;dQw4w9WgXcQ&ci;null&ci;null&ci;-1&sgi;null",
        // Hidden groups
        "1000&sgi;Subscriptions&sgi;null&sgi;News&ci;null&ci;$news&ci;null&ci;null&ci;null&ci;-1&sgi;null",
        "1001&sgi;Notifications&sgi;null&sgi;News&ci;null&ci;$news&ci;null&ci;null&ci;null&ci;-1&sgi;null",
        // Empty group: SmartTube writes a space for an empty list
        "e1e2e3e4e5e6&sgi;Empty&sgi;null&sgi; &sgi;null",
    ).joinToString("%AR%")

    @Test
    fun readsSmartTubeStorage() {
        val groups = ChannelGroupFormats.parseSmartTube(smartTubeRaw)
        assertEquals(listOf("Música", "Mixed", "Subscriptions", "Notifications", "Empty"), groups.map { it.title })

        val first = groups[0]
        assertEquals("x1Y2z3A4b5C6", first.id)
        assertNull(first.iconUrl)
        assertEquals(listOf(GroupChannel(music, "Rock Channel", "https://yt3.ggpht.com/rock=s88"), GroupChannel(cooking, "Cocina", null)), first.channels)

        assertEquals("https://icon", groups[1].iconUrl)
        assertTrue(groups[1].channels.isEmpty())
        assertTrue(groups[4].channels.isEmpty())
    }

    @Test
    fun emptyOrMissingStorageHasNoGroups() {
        assertTrue(ChannelGroupFormats.parseSmartTube(null).isEmpty())
        assertTrue(ChannelGroupFormats.parseSmartTube(" ").isEmpty())
        assertTrue(ChannelGroupFormats.parseSmartTube("garbage").isEmpty())
    }

    @Test
    fun writesWhatSmartTubeReads() {
        val groups = ChannelGroupFormats.parseSmartTube(smartTubeRaw)
        val written = ChannelGroupFormats.toSmartTube(groups)
        assertEquals(groups, ChannelGroupFormats.parseSmartTube(written))
        assertEquals(" ", ChannelGroupFormats.toSmartTube(emptyList()))
    }

    @Test
    fun hiddenGroupsBecomeVisibleOrAreDropped() {
        val visible = ChannelGroupFormats.visibleGroups(ChannelGroupFormats.parseSmartTube(smartTubeRaw), "Subs (SmartTube)")
        assertEquals(listOf("Música", "Mixed", "Subs (SmartTube)", "Empty"), visible.map { it.title })
        // New id: it's a normal group from now on
        assertNull(visible[2].id)
        assertEquals(listOf(news), visible[2].channels.map { it.channelId })
    }

    // ---- Backup zips ----

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun readsSmartTubeBackupZipFromEveryProfile() {
        val bytes = zip(
            "org.smarttube.stable/Backup/shared_prefs/org.smarttube.stable_preferences.xml" to "<map/>",
            "org.smarttube.stable/Backup/files/yt_service_prefs/anonymous_channel_group_data" to smartTubeRaw,
            "org.smarttube.stable/Backup/files/yt_service_prefs/John_Doe_channel_group_data" to
                "abcdefabcdef&sgi;Work&sgi;null&sgi;W&ci;null&ci;$news&ci;null&ci;null&ci;null&ci;-1&sgi;null",
            "org.smarttube.stable/Backup/files/yt_service_prefs/anonymous_other_data" to "ignored",
        )
        assertTrue(ChannelGroupFormats.isZip(bytes.copyOf(4)))
        val groups = ChannelGroupFormats.readBackupZip(ByteArrayInputStream(bytes))!!
        assertEquals(listOf("Música", "Mixed", "Subscriptions", "Notifications", "Empty", "Work"), groups.map { it.title })
    }

    @Test
    fun readsOldBackupsWithTheDataInsideSharedPreferences() {
        val escaped = "abcdefabcdef&amp;sgi;Rock &amp; Roll&amp;sgi;null&amp;sgi;R&amp;ci;null&amp;ci;$music&amp;ci;null&amp;ci;null&amp;ci;null&amp;ci;-1&amp;sgi;null"
        val xml = """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
            <map>
                <string name="anonymous_channel_group_data">$escaped</string>
                <boolean name="other" value="true" />
            </map>"""
        val groups = ChannelGroupFormats.readBackupZip(ByteArrayInputStream(zip("es.gestal.supertube.tv/Backup/shared_prefs/yt_service_prefs.xml" to xml)))!!
        assertEquals("Rock & Roll", groups.single().title)
        assertEquals(music, groups.single().channels.single().channelId)
    }

    @Test
    fun zipWithoutGroupsIsNotRecognized() {
        assertNull(ChannelGroupFormats.readBackupZip(ByteArrayInputStream(zip("a/shared_prefs/x.xml" to "<map/>"))))
        assertFalse(ChannelGroupFormats.isZip("[{}]".toByteArray()))
    }

    // ---- SuperTube export ----

    private val tricky = listOf(
        ChannelGroupData("id1", "Música \"en vivo\" {live} \\ test", "https://icon/a", listOf(GroupChannel(music, "Rock \"Channel\"}", "https://yt3/rock"), GroupChannel(cooking))),
        ChannelGroupData(null, "Vacío", null, emptyList()),
    )

    @Test
    fun exportRoundTripsWithNamesAndAvatars() {
        val json = ChannelGroupFormats.toExportJson(tricky)
        val parsed = ChannelGroupFormats.parseJson(json, "NewPipe")!!
        assertEquals(tricky.map { it.copy(id = null) }, parsed)
    }

    /**
     * SmartTube and SuperTube TV import the export with their GrayJay importer, which first runs these replacements
     * and then reads "name", "image.url" and the channel ids from "urls".
     */
    @Test
    fun exportSurvivesTheTvGrayJayImporter() {
        val json = ChannelGroupFormats.toExportJson(tricky)
        val cleaned = json.replace("\"{", "{").replace("}\"", "}").replace("\\\"", "\"")
        assertEquals(json, cleaned) // nothing for the cleanup to touch
        val array = JSONArray(cleaned)
        assertEquals(2, array.length())
        val first = array.getJSONObject(0)
        assertEquals(tricky[0].title, first.getString("name"))
        assertEquals("https://icon/a", first.getJSONObject("image").getString("url"))
        val urls = first.getJSONArray("urls")
        assertEquals("https://www.youtube.com/channel/$music", urls.getString(0))
        assertEquals("https://www.youtube.com/channel/$cooking", urls.getString(1))
        assertTrue(first.getString("id").isNotEmpty())
        assertTrue(array.getJSONObject(1).getString("id").isNotEmpty())
    }

    // ---- Other apps ----

    @Test
    fun readsGrayJay() {
        // GrayJay stores every group as a JSON string inside the array
        val json = """["{\"id\":\"g1\",\"name\":\"Tech\",\"image\":{\"url\":\"https://img\"},\"urls\":[\"https://www.youtube.com/channel/$music\",\"https://www.youtube.com/channel/$music\",\"https://odysee.com/@x\"],\"creationTime\":1}"]"""
        val groups = ChannelGroupFormats.parseJson(json, "NewPipe")!!
        assertEquals(listOf(ChannelGroupData(null, "Tech", "https://img", listOf(GroupChannel(music)))), groups)
    }

    @Test
    fun readsPocketTube() {
        val json = """{"ysc_collection":{"Music":{"color":"red"},"News":{}},"Music":["$music","$cooking"],"News":["$news"],"ysc_settings":{}}"""
        // Key order of a JSON object isn't kept: compare sorted
        val groups = ChannelGroupFormats.parseJson(json, "NewPipe")!!.sortedBy { it.title }
        assertEquals(listOf("Music", "News"), groups.map { it.title })
        assertEquals(listOf(music, cooking), groups[0].channels.map { it.channelId })
        assertEquals(listOf(news), groups[1].channels.map { it.channelId })
    }

    @Test
    fun readsNewPipeAsOneGroup() {
        val json = """{"app_version":"0.27","app_version_int":1000,"subscriptions":[
            {"service_id":0,"url":"https://www.youtube.com/channel/$music","name":"Rock"},
            {"service_id":1,"url":"https://soundcloud.com/x","name":"Other"}]}"""
        val groups = ChannelGroupFormats.parseJson(json, "NewPipe")!!
        assertEquals(listOf(ChannelGroupData(null, "NewPipe", null, listOf(GroupChannel(music, "Rock")))), groups)
    }

    @Test
    fun readsTheGroupsOfASuperTubeBackup() {
        val backup = """{"format":"supertube-backup","version":1,"favorites":[],"settings":{},"channelGroups":${ChannelGroupFormats.toExportJson(tricky)}}"""
        assertEquals(tricky.map { it.copy(id = null) }, ChannelGroupFormats.parseJson(backup, "NewPipe"))
    }

    @Test
    fun unknownFilesAreRejected() {
        assertNull(ChannelGroupFormats.parseJson("""{"format":"supertube-backup","favorites":[]}""", "NewPipe"))
        assertNull(ChannelGroupFormats.parseJson("not json", "NewPipe"))
        assertNull(ChannelGroupFormats.parseJson("[1,2]", "NewPipe"))
    }

    @Test
    fun findsChannelIdsInLinks() {
        assertEquals(music, ChannelGroupFormats.channelIdFromUrl(music))
        assertEquals(music, ChannelGroupFormats.channelIdFromUrl(" https://www.youtube.com/channel/$music/videos?x=1 "))
        assertEquals(news, ChannelGroupFormats.channelIdFromUrl("https://m.youtube.com/channel/$news"))
        assertNull(ChannelGroupFormats.channelIdFromUrl("https://www.youtube.com/@handle"))
        assertNull(ChannelGroupFormats.channelIdFromUrl("UCshort"))
        assertNull(ChannelGroupFormats.channelIdFromUrl(null))
    }

    // ---- Merge ----

    @Test
    fun mergeAddsMissingChannelsToGroupsWithTheSameName() {
        val existing = listOf(ChannelGroupData("a", "Música", null, listOf(GroupChannel(music))))
        val incoming = listOf(
            ChannelGroupData(null, " música ", null, listOf(GroupChannel(music), GroupChannel(cooking))),
            ChannelGroupData(null, "News", "https://icon", listOf(GroupChannel(news))),
        )
        val steps = ChannelGroupFormats.planMerge(existing, incoming)
        assertEquals(
            listOf(
                MergeStep("a", "Música", null, listOf(GroupChannel(cooking))),
                MergeStep(null, "News", "https://icon", listOf(GroupChannel(news))),
            ),
            steps,
        )
    }

    @Test
    fun mergeOfTheSameDataDoesNothing() {
        val groups = ChannelGroupFormats.parseSmartTube(smartTubeRaw)
        assertTrue(ChannelGroupFormats.planMerge(groups, groups).isEmpty())
    }

    @Test
    fun mergeJoinsRepeatedIncomingGroups() {
        // Two profiles of a backup with a group of the same name
        val incoming = listOf(
            ChannelGroupData(null, "Work", null, listOf(GroupChannel(music))),
            ChannelGroupData(null, "WORK", null, listOf(GroupChannel(music), GroupChannel(news))),
        )
        assertEquals(listOf(MergeStep(null, "Work", null, listOf(GroupChannel(music), GroupChannel(news)))), ChannelGroupFormats.planMerge(emptyList(), incoming))
    }

    @Test
    fun mergeCreatesEmptyGroupsButSkipsNamelessOnes() {
        val incoming = listOf(ChannelGroupData(null, "Empty", null, emptyList()), ChannelGroupData(null, "  ", null, listOf(GroupChannel(music))))
        assertEquals(listOf(MergeStep(null, "Empty", null, emptyList())), ChannelGroupFormats.planMerge(emptyList(), incoming))
    }

    @Test
    fun learnsOnlyMissingChannelNames() {
        val channels = listOf(GroupChannel(music, "Rock"), GroupChannel(cooking), GroupChannel(news, " "))
        val authors = mapOf(music to "Other name", cooking to "Cocina", news to "Noticias")
        assertEquals(listOf(GroupChannel(cooking, "Cocina"), GroupChannel(news, "Noticias")), ChannelGroupFormats.missingTitles(channels, authors))
    }
}
