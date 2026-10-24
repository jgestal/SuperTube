package com.liskovsoft.smartyoutubetv2.mobile.ui.dialog

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liskovsoft.mediaserviceinterfaces.data.ChatItem
import com.liskovsoft.mediaserviceinterfaces.data.CommentGroup
import com.liskovsoft.mediaserviceinterfaces.data.CommentItem
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver
import com.liskovsoft.smartyoutubetv2.mobile.ui.components.RemoteImage

private val mainHandler = Handler(Looper.getMainLooper())

/** Restores the list when the user comes back from the replies of a comment */
private class CommentsBackup(val items: List<CommentItem>, val group: CommentGroup?) : CommentsReceiver.Backup

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentsContent(receiver: CommentsReceiver) {
    val comments = remember(receiver) { mutableStateListOf<CommentItem>() }
    var group by remember(receiver) { mutableStateOf<CommentGroup?>(null) }
    var message by remember(receiver) { mutableStateOf<String?>(receiver.loadingMessage) }
    val listState = rememberLazyListState()

    DisposableEffect(receiver) {
        receiver.setCallback(object : CommentsReceiver.Callback {
            override fun onCommentGroup(commentGroup: CommentGroup?) { mainHandler.post {
                val items = commentGroup?.comments
                if (items == null) {
                    message = receiver.errorMessage
                    return@post
                }
                comments.addAll(items.filter { !it.isEmpty })
                message = if (comments.isEmpty()) receiver.errorMessage else null
                group = commentGroup
                // Short lists that can be continued don't trigger the scroll end
                if (comments.size <= 10 && commentGroup.nextCommentsKey != null) receiver.onLoadMore(commentGroup)
            } }

            override fun onBackup(backup: CommentsReceiver.Backup?) { mainHandler.post {
                val restored = backup as? CommentsBackup ?: return@post
                comments.clear()
                comments.addAll(restored.items)
                group = restored.group
                message = if (comments.isEmpty()) receiver.errorMessage else null
            } }

            override fun onSync(commentItem: CommentItem?) { mainHandler.post {
                if (commentItem == null) return@post
                val index = comments.indexOfFirst { it.id == commentItem.id }
                if (index != -1) comments[index] = commentItem
            } }
        })
        receiver.onStart()

        onDispose {
            receiver.onFinish(CommentsBackup(comments.toList(), group))
        }
    }

    val reachedEnd by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= comments.size - 3 } }
    LaunchedEffect(reachedEnd, comments.size) {
        val current = group
        if (reachedEnd && comments.isNotEmpty() && current?.nextCommentsKey != null) receiver.onLoadMore(current)
    }

    if (comments.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            if (message == receiver.loadingMessage) CircularProgressIndicator() else Text(message ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(state = listState) {
        itemsIndexed(comments) { _, comment ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { receiver.onCommentClicked(comment) }, onLongClick = { receiver.onCommentLongClicked(comment) })
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                RemoteImage(comment.authorPhoto, Modifier.size(36.dp).clip(CircleShape), width = 96, height = 96)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        listOfNotNull(comment.authorName, comment.publishedDate).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(comment.message ?: "", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (comment.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(comment.likeCount ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val replies = comment.replyCount
                        if (!replies.isNullOrBlank() && comment.nestedCommentsKey != null) {
                            Spacer(Modifier.width(16.dp))
                            Text(replies, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatContent(receiver: ChatReceiver) {
    val messages = remember(receiver) { mutableStateListOf<ChatItem>() }
    val listState = rememberLazyListState()

    DisposableEffect(receiver) {
        receiver.setCallback { item -> mainHandler.post { messages.add(item) } }
        onDispose { receiver.setCallback(null) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LazyColumn(state = listState) {
        itemsIndexed(messages) { _, item ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
                RemoteImage(item.authorPhoto, Modifier.size(24.dp).clip(CircleShape), width = 64, height = 64)
                Spacer(Modifier.width(8.dp))
                Text(
                    buildString {
                        append(item.authorName ?: "")
                        append("  ")
                        append(item.message ?: "")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
