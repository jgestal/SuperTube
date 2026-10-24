package com.liskovsoft.smartyoutubetv2.mobile.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem
import com.liskovsoft.smartyoutubetv2.mobile.R

@Composable
fun AppDialogScreen(
    state: DialogUiState,
    onOpenCategory: (DialogPage, OptionCategory) -> Unit,
    onChanged: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val page = state.current ?: return
    val scrimAlpha = if (state.isTransparent || state.isOverlay) 0.12f else 0.45f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val isLarge = maxWidth >= 600.dp
            val sheetModifier = if (isLarge) {
                Modifier.align(Alignment.Center).widthIn(max = 560.dp).heightIn(max = maxHeight * 0.85f)
            } else {
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = maxHeight * 0.88f)
            }
            val visible = remember { MutableTransitionState(false).apply { targetState = true } }

            AnimatedVisibility(visibleState = visible, modifier = sheetModifier, enter = slideInVertically { it / 3 } + fadeIn(), exit = fadeOut()) {
                Surface(
                    // Swallow clicks so they don't reach the scrim
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    shape = if (isLarge) RoundedCornerShape(28.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                ) {
                    Column {
                        if (!isLarge) {
                            // Drag handle (visual affordance of a bottom sheet)
                            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape))
                            }
                        }
                        DialogHeader(page.title?.toString(), canGoBack = state.pages.size > 1, onBack = onBack)
                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                val forward = state.pages.indexOf(targetState) >= state.pages.indexOf(initialState)
                                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
                            },
                            label = "dialogPage",
                        ) { target ->
                            val revision = state.revision
                            Box(Modifier.padding(bottom = 8.dp)) {
                                if (target.opened != null) {
                                    CategoryContent(target.opened, revision, onChanged)
                                } else {
                                    CategoriesList(target, revision, onOpenCategory, onChanged)
                                }
                            }
                        }
                        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String?, canGoBack: Boolean, onBack: () -> Unit) {
    if (title.isNullOrBlank() && !canGoBack) {
        Spacer(Modifier.height(8.dp))
        return
    }
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mobile_back))
            }
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Text(
            title ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TransparentListItem @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun CategoriesList(page: DialogPage, revision: Int, onOpenCategory: (DialogPage, OptionCategory) -> Unit, onChanged: () -> Unit) {
    LazyColumn {
        itemsIndexed(page.categories) { _, category ->
            if (revision < 0) return@itemsIndexed
            when (category.type) {
                OptionCategory.TYPE_SINGLE_SWITCH -> category.options.firstOrNull()?.let { item ->
                    ListItem(
                        modifier = Modifier.clickable { item.onSelect(!item.isSelected); onChanged() },
                        colors = TransparentListItem,
                        headlineContent = { Text(item.title?.toString() ?: "") },
                        supportingContent = item.description?.let { { Text(it.toString()) } },
                        trailingContent = { Switch(checked = item.isSelected, onCheckedChange = { item.onSelect(it); onChanged() }) },
                    )
                }
                OptionCategory.TYPE_SINGLE_BUTTON -> category.options.firstOrNull()?.let { item ->
                    ListItem(
                        modifier = Modifier.clickable { item.onSelect(true); onChanged() },
                        colors = TransparentListItem,
                        headlineContent = { Text(item.title?.toString() ?: "") },
                        supportingContent = item.description?.let { { Text(it.toString()) } },
                    )
                }
                else -> ListItem(
                    modifier = Modifier.clickable { onOpenCategory(page, category) },
                    colors = TransparentListItem,
                    headlineContent = { Text(category.title?.toString() ?: "") },
                    supportingContent = summaryOf(category)?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            }
        }
    }
}

private fun summaryOf(category: OptionCategory): String? = when (category.type) {
    OptionCategory.TYPE_RADIO_LIST -> category.options.firstOrNull { it.isSelected }?.title?.toString()
    OptionCategory.TYPE_CHECKBOX_LIST -> category.options.filter { it.isSelected }.joinToString(", ") { it.title?.toString() ?: "" }.ifEmpty { null }
    else -> null
}

@Composable
private fun CategoryContent(category: OptionCategory, revision: Int, onChanged: () -> Unit) {
    when (category.type) {
        OptionCategory.TYPE_RADIO_LIST -> RadioList(category.options, onChanged)
        OptionCategory.TYPE_CHECKBOX_LIST -> CheckedList(category.options, revision, onChanged)
        OptionCategory.TYPE_STRING_LIST -> StringList(category.options, onChanged)
        OptionCategory.TYPE_LONG_TEXT -> LongText(category.options.firstOrNull()?.title?.toString())
        OptionCategory.TYPE_COMMENTS -> category.options.firstOrNull()?.commentsReceiver?.let { CommentsContent(it) }
        OptionCategory.TYPE_CHAT -> category.options.firstOrNull()?.chatReceiver?.let { ChatContent(it) }
        OptionCategory.TYPE_SINGLE_SWITCH, OptionCategory.TYPE_SINGLE_BUTTON -> CategoriesList(DialogPage(listOf(category), null), revision, { _, _ -> }, onChanged)
    }
}

/** Radio list: stays open after a selection (same as TV) so the user sees the result */
@Composable
private fun RadioList(options: List<OptionItem>, onChanged: () -> Unit) {
    var selected by remember(options) { mutableIntStateOf(options.indexOfFirst { it.isSelected }) }
    LazyColumn {
        itemsIndexed(options) { index, item ->
            val onClick = { selected = index; item.onSelect(true); onChanged() }
            ListItem(
                modifier = Modifier.clickable(onClick = onClick),
                colors = TransparentListItem,
                leadingContent = { RadioButton(selected = index == selected, onClick = onClick) },
                headlineContent = { Text(item.title?.toString() ?: "") },
                supportingContent = item.description?.let { { Text(it.toString()) } },
            )
        }
    }
}

/** Same rules as the TV AppPreferenceManager: "required" items warn, "radio" items are unchecked */
@Composable
private fun CheckedList(options: List<OptionItem>, revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    LazyColumn {
        itemsIndexed(options) { _, item ->
            val checked = revision >= 0 && item.isSelected
            val onToggle = {
                val newValue = !item.isSelected
                if (newValue) {
                    item.required?.forEach { required ->
                        if (!required.isSelected) {
                            MessageHelpers.showMessage(context, context.getString(com.liskovsoft.smartyoutubetv2.common.R.string.require_checked, required.title))
                        }
                    }
                    item.radio?.forEach { it.onSelect(false) }
                }
                item.onSelect(newValue)
                onChanged()
            }
            ListItem(
                modifier = Modifier.clickable(onClick = onToggle),
                colors = TransparentListItem,
                leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
                headlineContent = { Text(item.title?.toString() ?: "") },
                supportingContent = item.description?.let { { Text(it.toString()) } },
            )
        }
    }
}

@Composable
private fun StringList(options: List<OptionItem>, onChanged: () -> Unit) {
    LazyColumn {
        itemsIndexed(options) { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ListItem(
                modifier = Modifier.clickable { item.onSelect(!item.isSelected); onChanged() },
                colors = TransparentListItem,
                headlineContent = { Text(item.title?.toString() ?: "") },
                supportingContent = item.description?.let { { Text(it.toString()) } },
            )
        }
    }
}

@Composable
private fun LongText(text: String?) {
    SelectionContainer {
        Text(
            text ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}
