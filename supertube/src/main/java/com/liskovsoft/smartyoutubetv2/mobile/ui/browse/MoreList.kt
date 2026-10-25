package com.liskovsoft.smartyoutubetv2.mobile.ui.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.mobile.R

/** Sections of "More" in the user order (prefs), the not ordered ones after them and settings always last */
fun orderSections(sections: List<BrowseSection>, order: List<Int>): List<BrowseSection> {
    val (ordered, rest) = sections.partition { it.id in order }
    return ordered.sortedBy { order.indexOf(it.id) } + rest
}

/**
 * Secondary sections. In reorder mode every row gets up/down buttons and a drag handle;
 * the new order is reported through [onOrderChanged] when a move ends.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoreList(
    sections: List<BrowseSection>,
    isReordering: Boolean,
    onClick: (BrowseSection) -> Unit,
    onLongClick: (BrowseSection) -> Unit,
    onOrderChanged: (List<Int>) -> Unit,
) {
    // Settings at the end, separated from the content sections (not movable)
    val (settings, content) = sections.partition { it.id == MediaGroup.TYPE_SETTINGS }

    // Filled in the first composition: if the list only had "Settings" in its first frame, the lazy list would keep
    // "Settings" as the first visible item when the sections arrive, opening the screen scrolled down
    val items = remember { mutableStateListOf<BrowseSection>().apply { addAll(content) } }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current

    // Follow external changes (new/removed sections) while not dragging
    LaunchedEffect(content.map { it.id }) {
        if (draggingId == null) {
            items.clear()
            items.addAll(content)
        }
    }

    fun commit() = onOrderChanged(items.map { it.id })

    fun move(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
    }

    fun onDrag(dy: Float) {
        val id = draggingId ?: return
        dragOffset += dy
        val index = items.indexOfFirst { it.id == id }
        val visible = listState.layoutInfo.visibleItemsInfo
        if (dragOffset > 0 && index < items.lastIndex) {
            val next = visible.firstOrNull { it.key == items[index + 1].id } ?: return
            if (dragOffset > next.size / 2f) {
                move(index, index + 1)
                dragOffset -= next.size
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else if (dragOffset < 0 && index > 0) {
            val previous = visible.firstOrNull { it.key == items[index - 1].id } ?: return
            if (-dragOffset > previous.size / 2f) {
                move(index, index - 1)
                dragOffset += previous.size
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
        if (isReordering) {
            item(key = "hint") {
                Text(
                    stringResource(R.string.mobile_drag_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        itemsIndexed(items, key = { _, section -> section.id }) { index, section ->
            val isDragging = section.id == draggingId
            ListItem(
                modifier = Modifier
                    .then(if (isDragging) Modifier else Modifier.animateItem())
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                    }
                    .combinedClickable(
                        enabled = !isReordering,
                        onClick = { onClick(section) },
                        onLongClick = { onLongClick(section) },
                    ),
                colors = if (isDragging) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) else ListItemDefaults.colors(),
                headlineContent = { Text(section.title ?: "") },
                leadingContent = { SectionIconView(section, false) },
                trailingContent = if (!isReordering) null else ({
                    Row {
                        IconButton(enabled = index > 0, onClick = { move(index, index - 1); commit() }) {
                            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.mobile_move_up))
                        }
                        IconButton(enabled = index < items.lastIndex, onClick = { move(index, index + 1); commit() }) {
                            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.mobile_move_down))
                        }
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .pointerInput(section.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = section.id
                                            dragOffset = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            onDrag(amount.y)
                                        },
                                        onDragEnd = { draggingId = null; dragOffset = 0f; commit() },
                                        onDragCancel = { draggingId = null; dragOffset = 0f; commit() },
                                    )
                                },
                        )
                    }
                }),
            )
        }

        if (settings.isNotEmpty() && !isReordering) {
            item(key = "divider") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            items(settings, key = { it.id }) { section ->
                ListItem(
                    modifier = Modifier.combinedClickable(onClick = { onClick(section) }, onLongClick = { onLongClick(section) }),
                    headlineContent = { Text(section.title ?: "") },
                    leadingContent = { SectionIconView(section, false) },
                )
            }
        }
    }
}
