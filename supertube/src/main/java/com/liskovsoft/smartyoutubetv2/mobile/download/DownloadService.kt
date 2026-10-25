package com.liskovsoft.smartyoutubetv2.mobile.download

import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs
import android.net.NetworkCapabilities
import android.net.Network
import android.net.ConnectivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.splash.SplashActivity
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground service that runs the queued downloads (two at a time).
 * Streams are fetched in 8 MB ranges (avoids YouTube throttling) and appended to a temp file,
 * so a paused or interrupted download continues where it stopped. Expired urls are refreshed by itag.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = ConcurrentHashMap<Long, Job>()
    private lateinit var store: DownloadStore

    override fun onBind(intent: Intent?): IBinder? = null

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        store = DownloadStore.instance(this)
        createChannel()
        // "Wi‑Fi only": start the waiting downloads as soon as an unmetered network is available
        val connectivity = getSystemService(ConnectivityManager::class.java)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) scope.launch { schedule() }
            }
        }.also { runCatching { connectivity.registerDefaultNetworkCallback(it) } }
    }

    /** Downloads must wait: "Wi‑Fi only" is on and the current network is metered (mobile data) */
    private fun mustWaitForWifi(): Boolean =
        MobilePrefs.instance(this).wifiOnlyDownloads && getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        when (intent?.action) {
            ACTION_PAUSE -> intent.getLongExtra(EXTRA_ID, -1).let { id -> running.remove(id)?.cancel(); store.update(id) { it.copy(status = DownloadStatus.PAUSED) } }
        }
        scope.launch { schedule() }
        return START_STICKY
    }

    /** Starts queued items while there are free slots, stops the service when nothing is left */
    private suspend fun schedule() {
        withContext(Dispatchers.Main) {
            if (mustWaitForWifi()) {
                // Keep the service (and its notification) while waiting for Wi‑Fi
                updateNotification()
                return@withContext
            }
            val queued = store.items.filter { it.status == DownloadStatus.QUEUED && it.id !in running.keys }.sortedBy { it.createdAt }
            for (item in queued) {
                if (running.size >= MAX_PARALLEL) break
                running[item.id] = scope.launch { run(item.id) }
            }
            if (running.isEmpty() && store.items.none { it.status == DownloadStatus.QUEUED }) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                updateNotification()
            }
        }
    }

    private suspend fun run(id: Long) {
        try {
            store.update(id) { it.copy(status = DownloadStatus.DOWNLOADING, error = null) }
            var item = withContext(Dispatchers.Main) { store.get(id) } ?: return
            item.parts.forEachIndexed { index, _ -> downloadPart(id, index) }
            item = withContext(Dispatchers.Main) { store.get(id) } ?: return
            if (item.status != DownloadStatus.DOWNLOADING) return // paused or removed meanwhile

            store.update(id) { it.copy(status = DownloadStatus.PROCESSING) }
            val (uri, size) = finish(item)
            store.update(id) { it.copy(status = DownloadStatus.COMPLETED, outputUri = uri, outputBytes = size) }
            item.parts.forEach { File(DownloadStore.tempDir(this), it.tempFile).delete() }
        } catch (e: WaitForWifi) {
            // Back to the queue, the progress is kept
            store.update(id) { it.copy(status = DownloadStatus.QUEUED) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Paused/cancelled by the user: keep the temp files to resume later
        } catch (e: Exception) {
            store.update(id) { it.copy(status = DownloadStatus.FAILED, error = e.message) }
        } finally {
            running.remove(id)
            scope.launch { schedule() }
        }
    }

    private suspend fun downloadPart(id: Long, index: Int) {
        var refreshed = false
        while (currentCoroutineContext().isActive) {
            val item = withContext(Dispatchers.Main) { store.get(id) } ?: return
            if (item.status != DownloadStatus.DOWNLOADING) return
            if (mustWaitForWifi()) throw WaitForWifi()
            val part = item.parts[index]
            if (part.totalBytes > 0 && part.downloadedBytes >= part.totalBytes) return

            val file = File(DownloadStore.tempDir(this), part.tempFile)
            // Trust the file on disk (the saved counter is written with a delay)
            var position = if (file.exists()) file.length() else 0L
            val end = if (part.totalBytes > 0) minOf(position + CHUNK - 1, part.totalBytes - 1) else position + CHUNK - 1

            val connection = (URL(part.url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", Downloads.USER_AGENT)
                setRequestProperty("Range", "bytes=$position-$end")
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            val code = connection.responseCode
            if (code == 403 || code == 410) {
                connection.disconnect()
                // The stream url expired (they last a few hours): get a fresh one for the same format
                if (refreshed) throw IllegalStateException("HTTP $code")
                refreshUrl(id, index, part)
                refreshed = true
                continue
            }
            if (code == 416) { // Range past the end: the part is complete
                connection.disconnect()
                store.update(id) { it.copy(parts = it.parts.replaceAt(index) { p -> p.copy(downloadedBytes = position, totalBytes = position) }) }
                return
            }
            if (code !in 200..299) throw IllegalStateException("HTTP $code")

            val total = part.totalBytes.takeIf { it > 0 } ?: connection.getHeaderField("Content-Range")?.substringAfter('/')?.toLongOrNull() ?: -1L
            var lastUpdate = 0L
            RandomAccessFile(file, "rw").use { out ->
                out.seek(position)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        position += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 500) {
                            lastUpdate = now
                            val done = position
                            store.update(id, force = false) { it.copy(parts = it.parts.replaceAt(index) { p -> p.copy(downloadedBytes = done, totalBytes = total) }) }
                            withContext(Dispatchers.Main) { updateNotification() }
                        }
                        currentCoroutineContext().ensureActive() // pause = cancel the job
                    }
                }
            }
            connection.disconnect()
            val done = position
            // No total size known and the server returned less than asked: that was the end
            val finished = (total > 0 && done >= total) || (total <= 0 && done < end)
            store.update(id) { it.copy(parts = it.parts.replaceAt(index) { p -> p.copy(downloadedBytes = done, totalBytes = if (finished && total <= 0) done else total) }) }
            if (finished) return
            delay(1) // cooperative cancellation point
        }
    }

    private suspend fun refreshUrl(id: Long, index: Int, part: DownloadPart) {
        val item = withContext(Dispatchers.Main) { store.get(id) } ?: return
        val info: MediaItemFormatInfo = YouTubeServiceManager.instance().mediaItemService.getFormatInfoObserve(item.videoId).blockingFirst()
        val fresh = (info.adaptiveFormats.orEmpty() + info.urlFormats.orEmpty()).firstOrNull { it.iTag == part.itag && !it.url.isNullOrEmpty() }
            ?: throw IllegalStateException("Format not available anymore")
        store.update(id) { it.copy(parts = it.parts.replaceAt(index) { p -> p.copy(url = fresh.url) }) }
    }

    /** Joins the tracks (video) or copies the single stream, publishes it in Movies/ or Music/ and returns its uri */
    private fun finish(item: DownloadItem): Pair<String, Long> {
        val files = item.parts.map { File(DownloadStore.tempDir(this), it.tempFile) }
        return if (item.kind == DownloadKind.VIDEO && files.size == 2) {
            val output = File(cacheDir, "mux_${item.id}.mp4")
            try {
                Muxer.mux(files[0], files[1], output)
                Muxer.publish(this, output, item.outputName, item.kind).toString() to output.length()
            } finally {
                output.delete()
            }
        } else if (item.kind == DownloadKind.AUDIO && item.parts[0].mimeType?.startsWith("video/") == true) {
            // Audio taken from a single-file video stream
            val output = File(cacheDir, "audio_${item.id}.m4a")
            try {
                Muxer.extractAudio(files[0], output)
                Muxer.publish(this, output, item.outputName, item.kind).toString() to output.length()
            } finally {
                output.delete()
            }
        } else {
            Muxer.publish(this, files[0], item.outputName, item.kind).toString() to files[0].length()
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.mobile_downloads), NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val active = store.items.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PROCESSING }
        val total = active.sumOf { it.totalBytes }
        val done = active.sumOf { it.downloadedBytes }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, SplashActivity::class.java).putExtra(EXTRA_OPEN_DOWNLOADS, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(active.firstOrNull()?.title ?: getString(R.string.mobile_downloads))
            .setContentText(
                if (mustWaitForWifi()) getString(R.string.mobile_download_status_waiting_wifi)
                else resources.getQuantityString(R.plurals.mobile_active_downloads, active.size, active.size)
            )
            .setProgress(100, if (total > 0) (done * 100 / total).toInt() else 0, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        networkCallback?.let { runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 4242
        private const val MAX_PARALLEL = 2
        private const val CHUNK = 8L * 1024 * 1024
        private const val ACTION_PAUSE = "pause"
        private const val EXTRA_ID = "id"
        const val EXTRA_OPEN_DOWNLOADS = "open_downloads"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
        }

        fun pause(context: Context, id: Long) {
            ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java).setAction(ACTION_PAUSE).putExtra(EXTRA_ID, id))
        }
    }
}

private class WaitForWifi : Exception()

private fun <T> List<T>.replaceAt(index: Int, transform: (T) -> T): List<T> = mapIndexed { i, v -> if (i == index) transform(v) else v }
