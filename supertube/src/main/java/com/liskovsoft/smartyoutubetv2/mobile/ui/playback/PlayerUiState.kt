package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState

/** Everything the touch player UI renders. Written by [PlaybackActivity] (the PlaybackView), read by Compose. */
class PlayerUiState {
    var video by mutableStateOf<Video?>(null)
    var title by mutableStateOf<String?>(null)
    var subtitle by mutableStateOf<String?>(null)
    var channelIconUrl by mutableStateOf<String?>(null)
    var nextTitle by mutableStateOf<String?>(null)
    var qualityInfo by mutableStateOf<String?>(null)
    var backgroundUrl by mutableStateOf<String?>(null)
    var chatReceiver by mutableStateOf<ChatReceiver?>(null)
    var segments by mutableStateOf<List<SeekBarSegment>>(emptyList())

    var isOverlayShown by mutableStateOf(true)
    var isSuggestionsShown by mutableStateOf(false)
    var isProgressShown by mutableStateOf(false)
    var isSubtitlesShown by mutableStateOf(true)
    var isInPip by mutableStateOf(false)
    /** Fullscreen of a vertical video, in portrait (see FullscreenPolicy) */
    var isPortraitFullscreen by mutableStateOf(false)

    // Polled from the engine
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var bufferedMs by mutableLongStateOf(0L)
    var speed by mutableFloatStateOf(1f)

    /** Width / height of the current video (used for vertical videos in the portrait layout) */
    var videoAspect by mutableFloatStateOf(16f / 9f)

    val buttonStates = mutableStateMapOf<Int, Int>()
    /** Bumped when the video object is mutated in place (likes, subscription...) */
    var revision by mutableIntStateOf(0)

    val suggestions = FeedState()

    fun buttonState(id: Int): Int = buttonStates[id] ?: PlayerUI.BUTTON_OFF
}
