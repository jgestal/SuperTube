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

    fun download(context: Context, video: Video, kind: DownloadKind) {
        val videoId = video.videoId ?: return
        val appContext = context.applicationContext
        requestNotificationPermission(context)

        val store = DownloadStore.instance(appContext)
        // Same video and type already queued or running: don't download it twice
        if (store.items.any { it.videoId == videoId && it.kind == kind && it.isActive }) {
            MessageHelpers.showMessage(appContext, appContext.getString(R.string.mobile_download_already))
            return
        }
        MessageHelpers.showMessage(appContext, appContext.getString(R.string.mobile_download_preparing))

        RxHelper.execute(
            YouTubeServiceManager.instance().mediaItemService.getFormatInfoObserve(videoId),
            { info: MediaItemFormatInfo -> queue(appContext, video, info, kind) },
            { MessageHelpers.showMessage(appContext, appContext.getString(R.string.mobile_download_unavailable)) },
        )
    }

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

    private fun queue(context: Context, video: Video, info: MediaItemFormatInfo, kind: DownloadKind) {
        val title = (video.getTitleFull() ?: video.getTitle() ?: info.title ?: video.videoId).orEmpty()
        val id = System.currentTimeMillis()
        val base = "${video.videoId}_$id"
        val formats = info.adaptiveFormats.orEmpty()

        val parts: List<DownloadPart>
        val quality: String?
        val extension: String
        if (kind == DownloadKind.AUDIO) {
            val audio = pickAudio(formats)
            if (audio != null) {
                extension = if (audio.mimeType?.contains("webm") == true) "webm" else "m4a"
                parts = listOf(audio.toPart("${base}_audio.$extension"))
                quality = audio.bitrate?.toLongOrNull()?.let { "${it / 1000} kbps" }
            } else {
                // No separate audio stream (e.g. signed out): download the single-file stream, the audio is extracted on the device
                val progressive = pickProgressive(info) ?: return unavailable(context)
                extension = "m4a"
                parts = listOf(progressive.toPart("${base}_full.mp4"))
                quality = "AAC"
            }
        } else {
            val videoTrack = formats
                .filter { it.mimeType?.startsWith("video/mp4") == true && it.mimeType?.contains("avc1") == true && !it.url.isNullOrEmpty() && it.height in 1..1080 }
                .maxByOrNull { it.height * 1000 + (it.fps?.toIntOrNull() ?: 0) }
            val audioTrack = formats
                .filter { it.mimeType?.startsWith("audio/mp4") == true && !it.url.isNullOrEmpty() && !it.isDrc }
                .maxByOrNull { it.bitrate?.toLongOrNull() ?: 0L }
            extension = "mp4"
            if (videoTrack != null && audioTrack != null) {
                parts = listOf(videoTrack.toPart("${base}_video.mp4"), audioTrack.toPart("${base}_audio.m4a"))
                quality = videoTrack.qualityLabel ?: "${videoTrack.height}p"
            } else {
                val progressive = pickProgressive(info) ?: return unavailable(context)
                parts = listOf(progressive.toPart("${base}_full.mp4"))
                quality = progressive.qualityLabel ?: "${progressive.height}p"
            }
        }

        DownloadStore.instance(context).add(
            DownloadItem(
                id = id,
                videoId = video.videoId,
                title = title,
                author = video.getAuthor() ?: info.author,
                thumbnail = video.getCardImageUrl() ?: thumbnailOf(video.videoId),
                kind = kind,
                status = DownloadStatus.QUEUED,
                parts = parts,
                outputName = "${sanitize(title)}.$extension",
                quality = quality,
            )
        )
        DownloadService.start(context)
        MessageHelpers.showMessage(context, context.getString(R.string.mobile_download_queued))
    }

    /** Standard YouTube thumbnail (videos opened from links don't carry one) */
    fun thumbnailOf(videoId: String): String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    private fun unavailable(context: Context) = MessageHelpers.showMessage(context, context.getString(R.string.mobile_download_unavailable))

    private fun MediaFormat.toPart(file: String) = DownloadPart(iTag, url, file, mimeType, clen?.toLongOrNull() ?: -1L, 0L)

    /** Highest resolution stream that already contains audio and video */
    private fun pickProgressive(info: MediaItemFormatInfo): MediaFormat? =
        info.urlFormats.orEmpty().filter { it.mimeType?.startsWith("video/mp4") == true && !it.url.isNullOrEmpty() }.maxByOrNull { it.height }

    /** Highest bitrate audio, m4a first (plays everywhere) */
    private fun pickAudio(formats: List<MediaFormat>): MediaFormat? =
        formats
            .filter { it.mimeType?.startsWith("audio/") == true && !it.url.isNullOrEmpty() && !it.isDrc }
            .sortedWith(compareByDescending<MediaFormat> { it.mimeType?.contains("mp4") == true }.thenByDescending { it.bitrate?.toLongOrNull() ?: 0L })
            .firstOrNull()

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(120).ifEmpty { "video" }

    private fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 && context is Activity &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}
