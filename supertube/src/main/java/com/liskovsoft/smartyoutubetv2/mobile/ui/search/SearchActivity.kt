package com.liskovsoft.smartyoutubetv2.mobile.ui.search

import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

class SearchUiState {
    var query by mutableStateOf("")
    /** Last submitted query. Results belong to it. */
    var submittedQuery by mutableStateOf<String?>(null)
    var isEditing by mutableStateOf(true)
    val suggestions = mutableStateListOf<Tag>()
    val results = FeedState()
    /** Bumped to ask the screen to focus the text field (e.g. empty search started from a toolbar) */
    var focusRequests by mutableStateOf(0)
}

class SearchActivity : ScreenActivity(), SearchView {
    private lateinit var presenter: SearchPresenter
    private val state = SearchUiState()
    private var tagsProvider: MediaServiceSearchTagProvider? = null

    // Plain request codes: the androidx.fragment version used by the project rejects the codes of registerForActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VOICE) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) submit(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = SearchPresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                SearchScreen(
                    state = state,
                    presenter = presenter,
                    onQueryChange = ::onQueryChange,
                    onSubmit = ::submit,
                    onVoice = ::startVoiceRecognition,
                    onBack = { finish() },
                )
            }
        }

        presenter.onViewInitialized()
    }

    override fun onResume() {
        super.onResume()
        presenter.setView(this)
        presenter.onViewResumed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) presenter.onViewDestroyed()
    }

    override fun finish() {
        presenter.onFinish()
        super.finish()
    }

    private fun onQueryChange(query: String) {
        state.query = query
        state.isEditing = true
        loadSuggestions(query)
    }

    private fun loadSuggestions(query: String) {
        tagsProvider?.search(query) { tags ->
            runOnUiThread {
                state.suggestions.clear()
                if (tags != null) state.suggestions.addAll(tags)
            }
        }
    }

    private fun submit(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        state.query = text
        state.isEditing = false
        MobilePrefs.instance(this).addRecentSearch(text)
        if (text != state.submittedQuery) {
            state.submittedQuery = text
            state.results.clear()
            presenter.onSearch(text)
        }
    }

    // SearchView

    override fun updateSearch(group: VideoGroup) {
        state.results.updateRows(group)
    }

    override fun clearSearch() {
        // Called by the presenter right before loading new results: keep the submitted query (results screen)
        state.results.clear()
    }

    override fun clearSearchTags() {
        state.suggestions.clear()
    }

    override fun removeSearchTag(tag: Tag) {
        state.suggestions.remove(tag)
    }

    override fun setTagsProvider(provider: MediaServiceSearchTagProvider) {
        tagsProvider = provider
        loadSuggestions(state.query)
    }

    override fun showProgressBar(show: Boolean) {
        state.results.isLoading = show
    }

    override fun startSearch(searchText: String?) {
        if (searchText.isNullOrBlank()) {
            state.isEditing = true
            state.focusRequests++
            loadSuggestions(state.query)
        } else {
            submit(searchText)
        }
    }

    override fun getSearchText(): String = state.query

    override fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        try {
            startActivityForResult(intent, REQUEST_VOICE)
        } catch (e: ActivityNotFoundException) {
            MessageHelpers.showMessage(this, e.message)
        }
    }

    override fun finishReally() {
        super.finishReally()
    }

    private companion object {
        const val REQUEST_VOICE = 701
    }
}
