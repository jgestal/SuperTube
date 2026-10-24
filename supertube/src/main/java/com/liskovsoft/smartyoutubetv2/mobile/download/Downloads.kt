package com.liskovsoft.smartyoutubetv2.mobile.download

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.sharedutils.rx.RxHelper
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager

/**
 * Entry point of the downloads: picks the streams and queues a [DownloadItem] for [DownloadService].
 *
 * - Audio: best adaptive audio stream (m4a preferred), no transcoding.
 * - Video: best H.264 track up to 1080p + best m4a audio, joined on the device (stream copy).
 *   A single-file (progressive) stream is used when it's the only option.
 */
object Downloads {
    internal const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    /** One downloadable quality shown in the options sheet */
    class Option(
        val kind: DownloadKind,
        val label: String,
        val sizeBytes: Long,
        /** Video + audio tracks (joined on the device), a single-file stream, or one audio stream */
        val formats: List<MediaFormat>,
        /** Audio taken from a single-file video stream */
        val extractAudio: Boolean = false,
    )

    /** Loads the available qualities of a video (video options first, best quality first) */
    fun loadOptions(context: Context, video: Video, onResult: (MediaItemFormatInfo, List<Option>) -> Unit) {
        val videoId = video.videoId ?: return
        val appContext = context.applicationContext
        RxHelper.execute(
            YouTubeServiceManager.instance().mediaItemService.getFormatInfoObserve(videoId),
            { info: MediaItemFormatInfo ->
                val options = buildOptions(info)
                if (options.isEmpty()) unavailable(appContext) else onResult(info, options)
            },
            { unavailable(appContext) },
        )
    }

    /** Queues the chosen option */
    fun download(context: Context, video: Video, info: MediaItemFormatInfo, option: Option) {
        requestNotificationPermission(context)
        val appContext = context.applicationContext
        if (DownloadStore.instance(appContext).items.any { it.videoId == video.videoId && it.kind == option.kind && it.isActive }) {
            MessageHelpers.showMessage(appContext, appContext.getString(R.string.mobile_download_already))
            return
        }
        queue(appContext, video, info, option)
    }

    /** Downloads the best option of the given type without asking (used to download a missing file again) */
    fun download(context: Context, video: Video, kind: DownloadKind) {
        MessageHelpers.showMessage(context, context.getString(R.string.mobile_download_preparing))
        loadOptions(context, video) { info, options ->
            options.firstOrNull { it.kind == kind }?.let { download(context, video, info, it) } ?: unavailable(context)
        }
    }

    private fun buildOptions(info: MediaItemFormatInfo): List<Option> {
        val adaptive = info.adaptiveFormats.orEmpty().filter { !it.url.isNullOrEmpty() }
        val progressive = info.urlFormats.orEmpty().filter { it.mimeType?.startsWith("video/mp4") == true && !it.url.isNullOrEmpty() }
        val bestM4a = adaptive.filter { it.mimeType?.startsWith("audio/mp4") == true && !it.isDrc }.maxByOrNull { it.bitrate?.toLongOrNull() ?: 0L }
        val options = mutableListOf<Option>()

        // Video: H.264 tracks up to 1080p joined with the best m4a audio; single-file streams otherwise
        val videoTracks = adaptive
            .filter { it.mimeType?.startsWith("video/mp4") == true && it.mimeType?.contains("avc1") == true && it.height in 1..1080 }
            .groupBy { it.height }
            .map { (_, sameHeight) -> sameHeight.maxBy { (it.fps?.toIntOrNull() ?: 0) * 1_000_000 + (it.bitrate?.toLongOrNull() ?: 0L) / 1000 } }
            .sortedByDescending { it.height }
        if (videoTracks.isNotEmpty() && bestM4a != null) {
            videoTracks.forEach { track ->
                options += Option(DownloadKind.VIDEO, track.qualityLabel ?: "${track.height}p", size(track) + size(bestM4a), listOf(track, bestM4a))
            }
        } else {
            progressive.groupBy { it.height }.map { it.value.first() }.sortedByDescending { it.height }.forEach { stream ->
                options += Option(DownloadKind.VIDEO, stream.qualityLabel ?: "${stream.height}p", size(stream), listOf(stream))
            }
        }

        // Audio: the separate audio streams (m4a first), or the audio track of a single-file stream
        val audioTracks = adaptive
            .filter { it.mimeType?.startsWith("audio/") == true && !it.isDrc }
            .sortedWith(compareByDescending<MediaFormat> { it.mimeType?.contains("mp4") == true }.thenByDescending { it.bitrate?.toLongOrNull() ?: 0L })
            .distinctBy { (it.mimeType?.substringBefore(";") ?: "") + ((it.bitrate?.toLongOrNull() ?: 0L) / 16_000) }
            .take(4)
        if (audioTracks.isNotEmpty()) {
            audioTracks.forEach { track ->
                val container = if (track.mimeType?.contains("webm") == true) "opus" else "m4a"
                val kbps = track.bitrate?.toLongOrNull()?.let { "${it / 1000} kbps" } ?: ""
                options += Option(DownloadKind.AUDIO, "$container $kbps".trim(), size(track), listOf(track))
            }
        } else {
            progressive.minByOrNull { it.height }?.let { stream ->
                options += Option(DownloadKind.AUDIO, "AAC", -1, listOf(stream), extractAudio = true)
            }
        }
        return options
    }

    private fun size(format: MediaFormat): Long = format.clen?.toLongOrNull() ?: -1L

    fun pause(context: Context, item: DownloadItem) {
        DownloadStore.instance(context).update(item.id) { it.copy(status = DownloadStatus.PAUSED) }
        DownloadService.pause(context, item.id)
    }

    fun resume(context: Context, item: DownloadItem) {
        DownloadStore.instance(context).update(item.id) { it.copy(status = DownloadStatus.QUEUED, error = null) }
        DownloadService.start(context)
    }

    /** Cancels a running download or removes a finished one (optionally deleting the file too) */
    fun remove(context: Context, item: DownloadItem, deleteFile: Boolean) {
        val store = DownloadStore.instance(context)
        if (item.isActive) DownloadService.pause(context, item.id)
        if (deleteFile) store.deleteFile(item)
        store.remove(item.id)
    }

    /** The file was moved or deleted outside the app: download it again from scratch */
    fun retry(context: Context, item: DownloadItem) {
        DownloadStore.instance(context).remove(item.id)
        download(context, Video().apply { videoId = item.videoId; title = item.title; author = item.author; cardImageUrl = item.thumbnail }, item.kind)
    }

    private fun queue(context: Context, video: Video, info: MediaItemFormatInfo, option: Option) {
        val title = (video.getTitleFull() ?: video.getTitle() ?: info.title ?: video.videoId).orEmpty()
        val id = System.currentTimeMillis()
        val base = "${video.videoId}_$id"
        val extension = when {
            option.kind == DownloadKind.VIDEO -> "mp4"
            option.formats[0].mimeType?.contains("webm") == true && !option.extractAudio -> "webm"
            else -> "m4a"
        }
        val parts = option.formats.mapIndexed { index, format ->
            val suffix = when {
                option.formats.size == 2 -> if (index == 0) "video.mp4" else "audio.m4a"
                option.extractAudio || option.kind == DownloadKind.VIDEO -> "full.mp4"
                else -> "audio.$extension"
            }
            format.toPart("${base}_$suffix")
        }

        DownloadStore.instance(context).add(
            DownloadItem(
                id = id,
                videoId = video.videoId,
                title = title,
                author = video.getAuthor() ?: info.author,
                thumbnail = video.getCardImageUrl() ?: thumbnailOf(video.videoId),
                kind = option.kind,
                status = DownloadStatus.QUEUED,
                parts = parts,
                outputName = "${sanitize(title)}.$extension",
                quality = option.label,
            )
        )
        DownloadService.start(context)
        MessageHelpers.showMessage(context, context.getString(R.string.mobile_download_queued))
    }

    /** Standard YouTube thumbnail (videos opened from links don't carry one) */
    fun thumbnailOf(videoId: String): String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    private fun unavailable(context: Context) = MessageHelpers.showMessage(context, context.getString(R.string.mobile_download_unavailable))

    private fun MediaFormat.toPart(file: String) = DownloadPart(iTag, url, file, mimeType, clen?.toLongOrNull() ?: -1L, 0L)

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(120).ifEmpty { "video" }

    private fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 && context is Activity &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}
