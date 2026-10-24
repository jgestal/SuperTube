package com.liskovsoft.smartyoutubetv2.mobile.download

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

/** Joins a video-only and an audio-only file into one mp4 (stream copy, no re-encoding) */
object Muxer {
    @SuppressLint("WrongConstant")
    fun mux(videoFile: File, audioFile: File, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val video = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
        val audio = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
        try {
            val videoTrack = selectTrack(video, "video/")
            val audioTrack = selectTrack(audio, "audio/")
            val outVideo = muxer.addTrack(video.getTrackFormat(videoTrack))
            val outAudio = muxer.addTrack(audio.getTrackFormat(audioTrack))
            muxer.start()
            copy(video, muxer, outVideo)
            copy(audio, muxer, outAudio)
            muxer.stop()
        } finally {
            muxer.release()
            video.release()
            audio.release()
        }
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME)?.startsWith(prefix) == true) {
                extractor.selectTrack(i)
                return i
            }
        }
        throw IllegalStateException("No $prefix track")
    }

    @SuppressLint("WrongConstant")
    private fun copy(extractor: MediaExtractor, muxer: MediaMuxer, track: Int) {
        val buffer = ByteBuffer.allocate(4 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(track, buffer, info)
            extractor.advance()
        }
    }

    /** Copies the file to Movies/SuperTube or Music/SuperTube (visible in the gallery / music apps) and returns its uri */
    fun publish(context: Context, file: File, displayName: String, kind: DownloadKind): Uri {
        val isVideo = kind == DownloadKind.VIDEO
        val directory = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
        val mime = when {
            isVideo -> "video/mp4"
            displayName.endsWith(".webm") -> "audio/webm"
            else -> "audio/mp4"
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/SuperTube")
            }
            val uri = context.contentResolver.insert(collection, values) ?: throw IllegalStateException("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            return uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(directory), "SuperTube").apply { mkdirs() }
            val target = File(dir, displayName)
            file.copyTo(target, overwrite = true)
            return Uri.fromFile(target)
        }
    }
}
