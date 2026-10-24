package com.liskovsoft.smartyoutubetv2.mobile.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedContainer
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.MultiRowGrid
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.VideoActions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    presenter: SearchPresenter,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoice: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.focusRequests) {
        if (state.isEditing) focusRequester.requestFocus()
    }
    LaunchedEffect(state.isEditing) {
        if (!state.isEditing) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    val actions = VideoActions(
        onClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemClicked(it) },
        onLongClick = { presenter.onVideoItemSelected(it); presenter.onVideoItemLongClicked(it) },
        onScrollEnd = { presenter.onScrollEnd(it) },
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mobile_back))
                    }
                    TextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) state.isEditing = true },
                        placeholder = { Text(stringResource(R.string.mobile_search_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit(state.query) }),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange(""); focusRequester.requestFocus() }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.mobile_clear))
                                }
                            }
                        },
                    )
                    IconButton(onClick = onVoice) {
                        Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.mobile_voice_search))
                    }
                    IconButton(onClick = { presenter.onSearchSettingsClicked() }) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.isEditing || state.submittedQuery == null) {
                LazyColumn(Modifier.fillMaxSize().imePadding()) {
                    items(state.suggestions) { tag ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = { onSubmit(tag.tag ?: "") },
                                onLongClick = { presenter.onTagLongClicked(tag) },
                            ),
                            leadingContent = {
                                Icon(if (state.query.isEmpty()) Icons.Outlined.History else Icons.Outlined.Search, null)
                            },
                            headlineContent = { Text(tag.tag ?: "", maxLines = 1) },
                            trailingContent = {
                                IconButton(onClick = { onQueryChange((tag.tag ?: "") + " "); focusRequester.requestFocus() }) {
                                    Icon(Icons.Default.NorthWest, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            } else {
                FeedContainer(isLoading = state.results.isLoading, isEmpty = state.results.isEmpty, onRefresh = null) {
                    MultiRowGrid(state.results, actions)
                }
            }
        }
    }
}
