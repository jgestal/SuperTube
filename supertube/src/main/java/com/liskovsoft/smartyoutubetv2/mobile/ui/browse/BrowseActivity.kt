package com.liskovsoft.smartyoutubetv2.mobile.ui.browse

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.ScreenActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.feed.FeedState
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SmartTubeTheme

/** Settings that only make sense on a TV (being casted to, display refresh rate switching) */
private val TV_ONLY_SETTINGS = setOf(
    com.liskovsoft.smartyoutubetv2.common.R.drawable.settings_cast,
    com.liskovsoft.smartyoutubetv2.common.R.drawable.settings_afr,
)

/** Content of a single section, kept while the section stays selected */
class SectionContent(val section: BrowseSection) {
    val feed = FeedState()
    /** Channels column of the "multi grid" sections (subscribed channels + their uploads) */
    val channels = FeedState()
    var selectedChannel by mutableStateOf<Video?>(null)
    var settings by mutableStateOf<List<SettingsItem>>(emptyList())
    var error by mutableStateOf<ErrorFragmentData?>(null)

    val isEmpty: Boolean get() = feed.isEmpty && channels.isEmpty && settings.isEmpty()

    fun clear() {
        feed.clear()
        channels.clear()
        selectedChannel = null
        settings = emptyList()
        error = null
    }
}

class BrowseUiState {
    val sections = mutableStateListOf<BrowseSection>()
    val contents = mutableStateMapOf<Int, SectionContent>()
    var selectedSectionId by mutableIntStateOf(-1)
    /** "More" destination is open but no secondary section has been picked yet */
    var isMoreListShown by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    val selectedSection: BrowseSection? get() = sections.firstOrNull { it.id == selectedSectionId }

    fun contentOf(section: BrowseSection): SectionContent = contents.getOrPut(section.id) { SectionContent(section) }

    val primarySections: List<BrowseSection>
        get() {
            val primaries = PRIMARY_SECTIONS.mapNotNull { id -> sections.firstOrNull { it.id == id } }
            // Fallback when the user disabled the default ones: promote the first sections
            return primaries.ifEmpty { sections.take(3) }
        }

    val secondarySections: List<BrowseSection> get() = sections.filter { it !in primarySections }

    fun isPrimary(sectionId: Int): Boolean = primarySections.any { it.id == sectionId }
}

/**
 * Root screen. Implements the same [BrowseView] contract as the TV BrowseFragment,
 * so [BrowsePresenter] drives sections, paging, errors and settings unchanged.
 */
class BrowseActivity : ScreenActivity(), BrowseView {
    private lateinit var presenter: BrowsePresenter
    private val state = BrowseUiState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = BrowsePresenter.instance(this)
        presenter.setView(this)

        setContent {
            SmartTubeTheme {
                BrowseScreen(
                    state = state,
                    presenter = presenter,
                    onSelectSection = ::openSection,
                    onShowMore = { state.isMoreListShown = true },
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

    override fun onPause() {
        super.onPause()
        presenter.onViewPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) {
            presenter.onViewDestroyed()
        }
    }

    private fun openSection(sectionId: Int) {
        val section = state.sections.firstOrNull { it.id == sectionId } ?: return
        state.isMoreListShown = false
        state.selectedSectionId = sectionId
        // Fresh content on every visit (like the TV fragments that are re-created on focus)
        state.contentOf(section).clear()
        presenter.onSectionFocused(sectionId)
    }

    // BrowseView

    override fun addSection(index: Int, section: BrowseSection) {
        state.sections.removeAll { it.id == section.id }
        if (index < 0 || index > state.sections.size) state.sections.add(section) else state.sections.add(index, section)
    }

    override fun removeSection(category: BrowseSection) {
        state.sections.removeAll { it.id == category.id }
        state.contents.remove(category.id)
    }

    override fun removeAllSections() {
        state.sections.clear()
        state.contents.clear()
    }

    override fun selectSection(index: Int, focusOnContent: Boolean) {
        val section = state.sections.getOrNull(index) ?: state.sections.firstOrNull() ?: return
        openSection(section.id)
    }

    override fun updateSection(group: VideoGroup) {
        val section = group.section ?: state.selectedSection ?: return
        val content = state.contentOf(section)
        content.error = null

        when (section.type) {
            BrowseSection.TYPE_ROW -> content.feed.updateRows(group)
            BrowseSection.TYPE_MULTI_GRID -> {
                if (group.position == 0) {
                    content.channels.updateGrid(group)
                    if (content.selectedChannel == null) content.selectedChannel = group.videos?.firstOrNull()
                } else {
                    content.feed.updateGrid(group)
                }
            }
            else -> content.feed.updateGrid(group)
        }
    }

    override fun updateSection(group: SettingsGroup) {
        val section = group.category ?: state.selectedSection ?: return
        val content = state.contentOf(section)
        content.error = null
        content.settings = group.items?.filter { it.imageResId !in TV_ONLY_SETTINGS } ?: emptyList()
    }

    override fun clearSection(section: BrowseSection) {
        state.contents[section.id]?.clear()
    }

    override fun selectSectionItem(index: Int) {
        // Touch UI has no focus. Nothing to do.
    }

    override fun selectSectionItem(item: Video?) {
        // Touch UI has no focus. Nothing to do.
    }

    override fun showError(data: ErrorFragmentData) {
        val section = state.selectedSection ?: return
        val content = state.contentOf(section)
        content.clear()
        content.error = data
    }

    override fun showProgressBar(show: Boolean) {
        state.isLoading = show
    }

    override fun isProgressBarShowing(): Boolean = state.isLoading

    override fun focusOnContent() {
        // Touch UI has no focus. Nothing to do.
    }

    override fun isEmpty(): Boolean {
        val section = state.selectedSection ?: return true
        return state.contents[section.id]?.isEmpty ?: true
    }

    override fun updateBadge() {
        // No badges on the mobile navigation yet
    }
}
