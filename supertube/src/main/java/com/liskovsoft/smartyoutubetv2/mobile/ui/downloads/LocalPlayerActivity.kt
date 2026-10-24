package com.liskovsoft.smartyoutubetv2.mobile.ui.downloads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.liskovsoft.smartyoutubetv2.mobile.R

/**
 * Plays a downloaded file (offline). Standard ExoPlayer controls; audio files show the thumbnail.
 * Position is kept across rotations (the activity handles its configuration changes).
 */
class LocalPlayerActivity : Activity() {
    private var player: SimpleExoPlayer? = null
    private var position = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SmartTube_Mobile_Player)
        super.onCreate(savedInstanceState)
        position = savedInstanceState?.getLong(KEY_POSITION) ?: 0L

        val root = FrameLayout(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        if (intent.getBooleanExtra(EXTRA_AUDIO, false)) {
            root.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(this@LocalPlayerActivity).load(intent.getStringExtra(EXTRA_THUMB)).into(this)
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val view = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 3000
            controllerHideOnTouch = true
            if (intent.getBooleanExtra(EXTRA_AUDIO, false)) controllerShowTimeoutMs = 0 // audio: controls always visible
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        title = intent.getStringExtra(EXTRA_TITLE)

        val newPlayer = ExoPlayerFactory.newSimpleInstance(this)
        val source = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(this, "SuperTube")).createMediaSource(intent.data)
        newPlayer.prepare(source)
        newPlayer.seekTo(position)
        newPlayer.playWhenReady = true
        view.player = newPlayer
        player = newPlayer
        applySystemBars(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBars(newConfig)
    }

    private fun applySystemBars(config: Configuration) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_POSITION, player?.currentPosition ?: 0L)
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_AUDIO = "audio"
        private const val EXTRA_THUMB = "thumb"
        private const val KEY_POSITION = "position"

        fun intent(context: Context, uri: Uri, title: String, isAudio: Boolean, thumbnail: String?): Intent =
            Intent(context, LocalPlayerActivity::class.java)
                .setData(uri)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_AUDIO, isAudio)
                .putExtra(EXTRA_THUMB, thumbnail)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
