package com.liskovsoft.smartyoutubetv2.mobile.download

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class DownloadKind { AUDIO, VIDEO }

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, PROCESSING, COMPLETED, FAILED }

/** One stream of a download (a video download has a video track and an audio track) */
data class DownloadPart(
    val itag: String?,
    val url: String,
    val tempFile: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
)

data class DownloadItem(
    val id: Long,
    val videoId: String,
    val title: String,
    val author: String?,
    val thumbnail: String?,
    val kind: DownloadKind,
    val status: DownloadStatus,
    val parts: List<DownloadPart>,
    /** Final file (MediaStore uri) once completed */
    val outputUri: String? = null,
    val outputName: String,
    val quality: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Size of the final file (can differ from the downloaded streams, e.g. audio extracted from a video) */
    val outputBytes: Long = -1,
) {
    val totalBytes: Long get() = parts.sumOf { it.totalBytes.coerceAtLeast(0) }
    val downloadedBytes: Long get() = parts.sumOf { it.downloadedBytes }
    val progress: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PROCESSING
}

/**
 * All downloads (active and finished). Compose state, persisted as JSON in the app files.
 * Every mutation is done on the main thread, the download service posts its updates here.
 */
class DownloadStore private constructor(private val context: Context) {
    private val file = File(context.filesDir, "downloads.json")
    private val main = Handler(Looper.getMainLooper())
    private var lastSave = 0L

    val items = mutableStateListOf<DownloadItem>().apply { addAll(load()) }

    init {
        // A process restart interrupts the transfers: they are resumed by the service, show them as queued
        for (i in items.indices) {
            if (items[i].status == DownloadStatus.DOWNLOADING || items[i].status == DownloadStatus.PROCESSING) {
                items[i] = items[i].copy(status = DownloadStatus.QUEUED)
            }
        }
    }

    val activeCount: Int get() = items.count { it.isActive }

    fun get(id: Long): DownloadItem? = items.firstOrNull { it.id == id }

    fun add(item: DownloadItem) = onMain {
        items.add(0, item)
        save(force = true)
    }

    fun update(id: Long, force: Boolean = true, transform: (DownloadItem) -> DownloadItem) = onMain {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            items[index] = transform(items[index])
            save(force)
        }
    }

    fun remove(id: Long) = onMain {
        val item = items.firstOrNull { it.id == id } ?: return@onMain
        item.parts.forEach { File(tempDir(context), it.tempFile).delete() }
        items.remove(item)
        save(force = true)
    }

    /** The user may delete or move the file outside the app */
    fun fileExists(item: DownloadItem): Boolean {
        val uri = item.outputUri ?: return false
        return try {
            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(item: DownloadItem) {
        val uri = item.outputUri ?: return
        try {
            context.contentResolver.delete(Uri.parse(uri), null, null)
        } catch (e: Exception) {
            // Already gone or not ours anymore
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    /** Progress updates are frequent: write them to disk at most every 2 s */
    private fun save(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSave < 2_000) return
        lastSave = now
        val json = JSONArray()
        items.forEach { json.put(toJson(it)) }
        try {
            file.writeText(json.toString())
        } catch (e: Exception) {
            // Disk full: keep in memory
        }
    }

    private fun load(): List<DownloadItem> = try {
        if (!file.exists()) emptyList() else {
            val json = JSONArray(file.readText())
            (0 until json.length()).mapNotNull { runCatching { fromJson(json.getJSONObject(it)) }.getOrNull() }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun toJson(item: DownloadItem) = JSONObject().apply {
        put("id", item.id); put("videoId", item.videoId); put("title", item.title); put("author", item.author)
        put("thumb", item.thumbnail); put("kind", item.kind.name); put("status", item.status.name)
        put("out", item.outputUri); put("name", item.outputName); put("quality", item.quality); put("error", item.error); put("created", item.createdAt); put("size", item.outputBytes)
        put("parts", JSONArray().apply {
            item.parts.forEach { p ->
                put(JSONObject().put("itag", p.itag).put("url", p.url).put("file", p.tempFile).put("mime", p.mimeType).put("total", p.totalBytes).put("done", p.downloadedBytes))
            }
        })
    }

    private fun fromJson(o: JSONObject): DownloadItem {
        val parts = o.getJSONArray("parts")
        return DownloadItem(
            id = o.getLong("id"),
            videoId = o.getString("videoId"),
            title = o.getString("title"),
            author = o.optNullable("author"),
            thumbnail = o.optNullable("thumb"),
            kind = DownloadKind.valueOf(o.getString("kind")),
            status = DownloadStatus.valueOf(o.getString("status")),
            parts = (0 until parts.length()).map { i ->
                val p = parts.getJSONObject(i)
                DownloadPart(p.optNullable("itag"), p.getString("url"), p.getString("file"), p.optNullable("mime"), p.getLong("total"), p.getLong("done"))
            },
            outputUri = o.optNullable("out"),
            outputName = o.getString("name"),
            quality = o.optNullable("quality"),
            error = o.optNullable("error"),
            createdAt = o.optLong("created"),
            outputBytes = o.optLong("size", -1),
        )
    }

    private fun JSONObject.optNullable(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)

    companion object {
        /** Local navigation section id for "Downloads" */
        const val SECTION_ID = -1001

        fun tempDir(context: Context): File = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }

        @Volatile
        private var instance: DownloadStore? = null

        fun instance(context: Context): DownloadStore =
            instance ?: synchronized(this) { instance ?: DownloadStore(context.applicationContext).also { instance = it } }
    }
}
