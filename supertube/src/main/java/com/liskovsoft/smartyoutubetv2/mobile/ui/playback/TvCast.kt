package com.liskovsoft.smartyoutubetv2.mobile.ui.playback

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.remote.LoungeSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Play on TV" button of the player top bar. Tap: sends the current video to the linked TV (SuperTube TV or SmartTube,
 * *Settings › Remote control*) and pauses it here. The first time, or on long press, asks for the TV code.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvCastButton(state: PlayerUiState, presenter: PlaybackPresenter, engine: PlaybackActivity) {
    val context = LocalContext.current
    val sender = remember { LoungeSender.instance(context) }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(sender.screen) }
    var showPairing by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    fun send() {
        val video = state.video ?: return
        val videoId = video.videoId ?: return
        val positionSec = if (video.isLive) 0L else state.positionMs / 1000
        val target = screen ?: return
        isSending = true
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching { sender.play(videoId, positionSec, video.playlistId) }.exceptionOrNull()
            }
            isSending = false
            if (error == null) {
                presenter.onPauseClicked()
                engine.playWhenReady = false
                Toast.makeText(context, context.getString(R.string.mobile_tv_sent, target.name), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.mobile_tv_error, error.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        Modifier
            .size(48.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.mobile_tv_play),
                onLongClickLabel = stringResource(R.string.mobile_tv_link),
                onClick = { if (screen == null) showPairing = true else if (!isSending) send() },
                onLongClick = { showPairing = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSending) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(
                if (screen != null) Icons.Default.CastConnected else Icons.Default.Cast,
                stringResource(R.string.mobile_tv_play),
                tint = Color.White,
            )
        }
    }

    if (showPairing) {
        TvPairingDialog(
            sender = sender,
            current = screen,
            onDismiss = { showPairing = false },
            onChanged = { linked ->
                screen = linked
                showPairing = false
                if (linked != null) send()
            },
        )
    }
}

@Composable
private fun TvPairingDialog(
    sender: LoungeSender,
    current: LoungeSender.Screen?,
    onDismiss: () -> Unit,
    onChanged: (LoungeSender.Screen?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isPairing by remember { mutableStateOf(false) }
    val digits = code.count { it.isDigit() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Cast, null) },
        title = { Text(stringResource(R.string.mobile_tv_link)) },
        text = {
            Column {
                if (current != null) {
                    Text(stringResource(R.string.mobile_tv_linked_to, current.name))
                    Spacer(Modifier.height(12.dp))
                }
                Text(stringResource(R.string.mobile_tv_link_help))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value.filter { it.isDigit() || it == ' ' || it == '-' }.take(17); error = null },
                    label = { Text(stringResource(R.string.mobile_tv_code)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isPairing,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = digits == 12 && !isPairing,
                onClick = {
                    isPairing = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { sender.pair(code) } }
                        isPairing = false
                        result.onSuccess { onChanged(it) }.onFailure { error = it.message }
                    }
                },
            ) {
                if (isPairing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.mobile_tv_link_action))
            }
        },
        dismissButton = {
            if (current != null) {
                TextButton(onClick = { sender.unpair(); onChanged(null) }) { Text(stringResource(R.string.mobile_tv_unlink)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}
