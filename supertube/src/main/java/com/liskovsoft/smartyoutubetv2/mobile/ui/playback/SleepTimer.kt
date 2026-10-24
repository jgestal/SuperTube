package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.liskovsoft.smartyoutubetv2.mobile.R

/**
 * Sleep timer: pauses the playback after a delay or at the end of the current video.
 * Checked by [PlaybackActivity.pollEngine] (every 500 ms while the player exists, also in background playback).
 */
object SleepTimer {
    /** 0 = off */
    var endAtMs by mutableLongStateOf(0L)
        private set
    var atEndOfVideo by mutableStateOf(false)
        private set
    /** Updated by the player poll to refresh the chip label */
    var nowMs by mutableLongStateOf(System.currentTimeMillis())

    val isActive: Boolean get() = endAtMs > 0 || atEndOfVideo

    fun start(minutes: Int) {
        atEndOfVideo = false
        endAtMs = System.currentTimeMillis() + minutes * 60_000L
    }

    fun startAtEndOfVideo() {
        endAtMs = 0
        atEndOfVideo = true
    }

    fun cancel() {
        endAtMs = 0
        atEndOfVideo = false
    }

    /** Returns true (and resets the timer) when the playback must stop now */
    fun shouldStop(positionMs: Long, durationMs: Long): Boolean {
        nowMs = System.currentTimeMillis()
        val expired = (endAtMs > 0 && nowMs >= endAtMs) ||
                (atEndOfVideo && durationMs > 0 && positionMs >= durationMs - 1_500)
        if (expired) cancel()
        return expired
    }

    fun remainingMinutes(): Long = ((endAtMs - nowMs).coerceAtLeast(0) + 59_999) / 60_000
}

private val OPTIONS = listOf(15, 30, 45, 60, 90)

/** Player chip: shows the remaining time when active, opens the options sheet */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerChip() {
    var showSheet by remember { mutableStateOf(false) }
    val active = SleepTimer.isActive
    val label = when {
        SleepTimer.atEndOfVideo -> stringResource(R.string.mobile_sleep_end_short)
        SleepTimer.endAtMs > 0 -> stringResource(R.string.mobile_sleep_minutes, SleepTimer.remainingMinutes())
        else -> stringResource(R.string.mobile_sleep_timer)
    }
    FilterChip(
        selected = active,
        onClick = { showSheet = true },
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(Icons.Outlined.Bedtime, null, Modifier.size(FilterChipDefaults.IconSize)) },
        shape = RoundedCornerShape(18.dp),
    )

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    stringResource(R.string.mobile_sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                @Composable
                fun Option(text: String, selected: Boolean, onClick: () -> Unit) {
                    ListItem(
                        modifier = Modifier.clickable { onClick(); showSheet = false },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { RadioButton(selected = selected, onClick = { onClick(); showSheet = false }) },
                        headlineContent = { Text(text) },
                    )
                }
                Option(stringResource(R.string.mobile_sleep_off), !SleepTimer.isActive) { SleepTimer.cancel() }
                OPTIONS.forEach { minutes ->
                    Option(stringResource(R.string.mobile_sleep_minutes, minutes.toLong()), false) { SleepTimer.start(minutes) }
                }
                Option(stringResource(R.string.mobile_sleep_end), SleepTimer.atEndOfVideo) { SleepTimer.startAtEndOfVideo() }
            }
        }
    }
}
