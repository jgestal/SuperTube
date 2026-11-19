package com.liskovsoft.smartyoutubetv2.mobile.remote

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Sends videos to a TV over the YouTube Lounge protocol, the one the YouTube app uses for "Link with TV code".
 * SuperTube TV and SmartTube receive it in *Settings › Remote control*.
 *
 * The TV is paired once with the code it shows. Only its screen id is stored: a fresh lounge token is asked for
 * every time a video is sent. Network calls are blocking, so call them off the main thread.
 */
class LoungeSender private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("supertube_lounge", Context.MODE_PRIVATE)

    data class Screen(val id: String, val name: String)

    val screen: Screen?
        get() {
            val id = prefs.getString(KEY_SCREEN_ID, null) ?: return null
            return Screen(id, prefs.getString(KEY_SCREEN_NAME, null) ?: "TV")
        }

    private val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    /** Pairs with the TV that shows [code] (12 digits, separators allowed) and remembers it. */
    @Throws(IOException::class)
    fun pair(code: String): Screen {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 12) throw IOException("The TV code has 12 digits")
        val json = JSONObject(post("$BASE/pairing/get_screen", mapOf("pairing_code" to digits)))
        val screen = json.getJSONObject("screen")
        val result = Screen(screen.getString("screenId"), screen.optString("name").ifBlank { "TV" })
        prefs.edit().putString(KEY_SCREEN_ID, result.id).putString(KEY_SCREEN_NAME, result.name).apply()
        return result
    }

    fun unpair() {
        prefs.edit().remove(KEY_SCREEN_ID).remove(KEY_SCREEN_NAME).apply()
    }

    /** Asks the paired TV to play [videoId] from [positionSec]. */
    @Throws(IOException::class)
    fun play(videoId: String, positionSec: Long, playlistId: String? = null) {
        val screen = screen ?: throw IOException("No TV linked")
        val token = loungeToken(screen.id)

        val bindParams = mapOf(
            "device" to "REMOTE_CONTROL",
            "mdx-version" to "3",
            "ui" to "1",
            "v" to "2",
            "name" to "SuperTube",
            "app" to "youtube-desktop",
            "loungeIdToken" to token,
            "id" to deviceId,
            "VER" to "8",
            "CVER" to "1",
            "zx" to "xxxxxxxxxxxx",
            "t" to "1",
        )

        // Open a remote session: the answer carries the session ids ("c" and "S")
        val session = parseChunks(post("$BASE/bc/bind?" + encode(bindParams + ("RID" to "1")), mapOf("count" to "0")))
        val sid = session["c"] ?: throw IOException("The TV didn't open a session")
        val gsessionid = session["S"] ?: throw IOException("The TV didn't open a session")

        val command = mutableMapOf(
            "count" to "1",
            "ofs" to "0",
            "req0__sc" to "setPlaylist",
            "req0_videoId" to videoId,
            "req0_currentTime" to positionSec.coerceAtLeast(0).toString(),
            "req0_currentIndex" to "-1",
            "req0_audioOnly" to "false",
        )
        if (!playlistId.isNullOrEmpty()) command["req0_listId"] = playlistId

        post("$BASE/bc/bind?" + encode(bindParams + mapOf("RID" to "2", "SID" to sid, "gsessionid" to gsessionid)), command)
    }

    private fun loungeToken(screenId: String): String {
        val json = JSONObject(post("$BASE/pairing/get_lounge_token_batch", mapOf("screen_ids" to screenId)))
        val screens = json.optJSONArray("screens")
        if (screens == null || screens.length() == 0) throw IOException("The TV is no longer linked")
        return screens.getJSONObject(0).getString("loungeToken")
    }

    private fun post(url: String, form: Map<String, String>): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.use { it.write(encode(form).toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException(if (code == 404 && url.contains("get_screen")) "Wrong or expired TV code" else "HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val BASE = "https://www.youtube.com/api/lounge"
        private const val TIMEOUT_MS = 15_000
        private const val KEY_SCREEN_ID = "screen_id"
        private const val KEY_SCREEN_NAME = "screen_name"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var sInstance: LoungeSender? = null

        fun instance(context: Context): LoungeSender =
            sInstance ?: synchronized(this) { sInstance ?: LoungeSender(context).also { sInstance = it } }

        private fun encode(params: Map<String, String>): String =
            params.entries.joinToString("&") { (k, v) -> URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8") }

        /**
         * The bind answer is a list of "<length>\n<json array>" chunks, each holding [index, [command, args...]].
         * Returns the first argument of the session commands ("c" is the session id, "S" the gsessionid).
         */
        internal fun parseChunks(body: String): Map<String, String> {
            val result = mutableMapOf<String, String>()
            Regex("""\["(c|S)","([^"]+)"""").findAll(body).forEach { match ->
                result.putIfAbsent(match.groupValues[1], match.groupValues[2])
            }
            return result
        }
    }
}
