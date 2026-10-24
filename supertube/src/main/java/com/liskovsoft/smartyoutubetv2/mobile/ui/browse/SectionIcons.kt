package com.liskovsoft.smartyoutubetv2.mobile.ui.browse

import androidx.compose.material.icons.outlined.Download
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadStore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoritesStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.ui.graphics.vector.ImageVector
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup

/** Material Symbols for the built-in sections (the TV drawables are designed for a dark 10-foot UI) */
fun sectionIcon(sectionId: Int, selected: Boolean = false): ImageVector = when (sectionId) {
    MediaGroup.TYPE_HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    MediaGroup.TYPE_SHORTS -> if (selected) Icons.Filled.Slideshow else Icons.Outlined.Slideshow
    MediaGroup.TYPE_SUBSCRIPTIONS -> if (selected) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions
    MediaGroup.TYPE_TRENDING -> Icons.Outlined.Whatshot
    MediaGroup.TYPE_KIDS_HOME -> Icons.Outlined.ChildCare
    MediaGroup.TYPE_SPORTS -> Icons.Outlined.SportsSoccer
    MediaGroup.TYPE_LIVE -> Icons.Outlined.LiveTv
    MediaGroup.TYPE_MY_VIDEOS -> Icons.Outlined.VideoLibrary
    MediaGroup.TYPE_GAMING -> Icons.Outlined.SportsEsports
    MediaGroup.TYPE_NEWS -> Icons.Outlined.Newspaper
    MediaGroup.TYPE_MUSIC -> Icons.Outlined.MusicNote
    MediaGroup.TYPE_CHANNEL_UPLOADS -> Icons.Outlined.AccountBox
    MediaGroup.TYPE_HISTORY -> Icons.Outlined.History
    MediaGroup.TYPE_BLOCKED_CHANNELS -> Icons.Outlined.Block
    MediaGroup.TYPE_USER_PLAYLISTS -> Icons.AutoMirrored.Outlined.PlaylistPlay
    MediaGroup.TYPE_NOTIFICATIONS -> Icons.Outlined.Notifications
    MediaGroup.TYPE_PLAYBACK_QUEUE -> Icons.AutoMirrored.Outlined.QueueMusic
    MediaGroup.TYPE_SETTINGS -> Icons.Outlined.Settings
    DownloadStore.SECTION_ID -> Icons.Outlined.Download
    FavoritesStore.SECTION_ID -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    else -> Icons.Outlined.Folder
}

/** Sections promoted to the bottom bar / rail. Everything else lives in the "More" destination. */
val PRIMARY_SECTIONS = listOf(MediaGroup.TYPE_HOME, MediaGroup.TYPE_SHORTS, MediaGroup.TYPE_SUBSCRIPTIONS)
