// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.usecase.ApplyToAllPages
import io.github.ukpratik.folio.core.domain.usecase.ResetPage
import io.github.ukpratik.folio.core.domain.usecase.RotatePage
import io.github.ukpratik.folio.core.domain.usecase.SetEnhancement
import io.github.ukpratik.folio.core.domain.usecase.UpdateCorners
import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.ui.text.UiText
import io.github.ukpratik.folio.feature.editor.R
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S4a/b. One commit model (D-29): every edit saves itself — corners on finger-up, sliders after a short
 * debounce, everything else immediately. "Done" just closes.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class PageDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    pages: PageRepository,
    private val updateCorners: UpdateCorners,
    private val rotatePage: RotatePage,
    private val setEnhancement: SetEnhancement,
    private val applyToAllPages: ApplyToAllPages,
    private val resetPage: ResetPage,
) : ViewModel() {

    private val documentId = DocumentId(checkNotNull(savedState.get<String>(PageDetailDestination.ARG_DOCUMENT_ID)))
    private val startPageId = PageId(checkNotNull(savedState.get<String>(PageDetailDestination.ARG_PAGE_ID)))

    private val ui = MutableStateFlow(UiSelection())
    /** Latest slider value wins: older pending values are dropped, never the newest. */
    private val pendingSave = MutableSharedFlow<Pair<PageId, Enhancement>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var lastApplyAll: Map<PageId, Enhancement>? = null

    val state: StateFlow<PageDetailState> = combine(pages.observePages(documentId), ui) { list, selection ->
        val index = selection.index ?: list.indexOfFirst { it.id == startPageId }.coerceAtLeast(0)
        PageDetailState(
            loaded = true,
            pages = list,
            currentIndex = index.coerceIn(0, (list.size - 1).coerceAtLeast(0)),
            tab = selection.tab,
            draft = selection.draft,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageDetailState())

    private val _effects = Channel<PageDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PageDetailEffect> = _effects.receiveAsFlow()

    init {
        // Sliders fire many times a second; persist after they settle (UX: preview ≤ 300 ms).
        viewModelScope.launch {
            pendingSave.debounce(SLIDER_DEBOUNCE_MS).collect { (id, enhancement) -> setEnhancement(id, enhancement) }
        }
    }

    fun onIntent(intent: PageDetailIntent) {
        val page = state.value.current
        when (intent) {
            is PageDetailIntent.SelectPage -> ui.update { it.copy(index = intent.index, draft = null) }
            is PageDetailIntent.SelectTab -> ui.update { it.copy(tab = intent.tab) }
            is PageDetailIntent.CommitCorners -> page?.let { launch { updateCorners(it.id, intent.corners) } }
            PageDetailIntent.AutoCrop -> page?.let {
                if (it.autoCorners == null) {
                    send(PageDetailEffect.ShowMessage(UiText.Res(R.string.detail_no_document_found)))
                } else {
                    launch { updateCorners(it.id, it.autoCorners) }
                }
            }
            PageDetailIntent.FullImage -> page?.let { launch { updateCorners(it.id, null) } }
            is PageDetailIntent.Rotate -> page?.let { launch { rotatePage(it.id, intent.clockwise) } }
            is PageDetailIntent.SetMode -> editLook(saveNow = true) { it.copy(mode = intent.mode) }
            is PageDetailIntent.SetBrightness -> editLook { it.copy(adjustments = it.adjustments.copy(brightness = intent.value)) }
            is PageDetailIntent.SetContrast -> editLook { it.copy(adjustments = it.adjustments.copy(contrast = intent.value)) }
            PageDetailIntent.ApplyToAll -> applyToAll()
            PageDetailIntent.UndoApplyToAll -> lastApplyAll?.let { previous ->
                lastApplyAll = null
                launch { applyToAllPages.undo(previous) }
            }
            PageDetailIntent.ResetPage -> page?.let {
                ui.update { s -> s.copy(draft = null) }
                launch { resetPage(it.id) }
            }
        }
    }

    private fun editLook(saveNow: Boolean = false, change: (Enhancement) -> Enhancement) {
        val page = state.value.current ?: return
        val next = change(state.value.enhancement ?: page.enhancement)
        ui.update { it.copy(draft = page.id to next) }
        if (saveNow) launch { setEnhancement(page.id, next) } else pendingSave.tryEmit(page.id to next)
    }

    private fun applyToAll() {
        val current = state.value
        val look = current.enhancement ?: return
        launch {
            lastApplyAll = applyToAllPages(current.pages, look)
            ui.update { it.copy(draft = null) }
            _effects.send(PageDetailEffect.ShowApplyAllUndo(current.pages.size))
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun send(effect: PageDetailEffect) = launch { _effects.send(effect) }

    private data class UiSelection(
        val index: Int? = null,
        val tab: DetailTab = DetailTab.CROP,
        val draft: Pair<PageId, Enhancement>? = null,
    )

    companion object {
        const val SLIDER_DEBOUNCE_MS = 150L
        /** Slider range in the UI; [Adjustments] accepts −1..1. */
        val SLIDER_RANGE = -1f..1f
    }
}
