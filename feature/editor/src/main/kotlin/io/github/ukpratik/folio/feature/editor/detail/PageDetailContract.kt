// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.ui.text.UiText

enum class DetailTab { CROP, ROTATE, ENHANCE }

data class PageDetailState(
    val loaded: Boolean = false,
    val pages: List<Page> = emptyList(),
    val currentIndex: Int = 0,
    val tab: DetailTab = DetailTab.CROP,
    /** Slider/mode values shown before the debounced save lands (keeps sliders smooth). */
    val draft: Pair<PageId, Enhancement>? = null,
) {
    val current: Page? get() = pages.getOrNull(currentIndex)

    /** The look to show for the current page: the in-flight draft if any, else what's saved. */
    val enhancement: Enhancement? get() = current?.let { page -> draft?.takeIf { it.first == page.id }?.second ?: page.enhancement }

    /** The current page as it should be previewed, including any unsaved draft. */
    val previewPage: Page? get() = current?.let { page -> enhancement?.let { page.copy(mode = it.mode, adjustments = it.adjustments) } ?: page }
}

sealed interface PageDetailIntent {
    data class SelectPage(val index: Int) : PageDetailIntent
    data class SelectTab(val tab: DetailTab) : PageDetailIntent
    data class CommitCorners(val corners: Quad) : PageDetailIntent
    data object AutoCrop : PageDetailIntent
    data object FullImage : PageDetailIntent
    data class Rotate(val clockwise: Boolean) : PageDetailIntent
    data class SetMode(val mode: EnhancementMode) : PageDetailIntent
    data class SetBrightness(val value: Float) : PageDetailIntent
    data class SetContrast(val value: Float) : PageDetailIntent
    data object ApplyToAll : PageDetailIntent
    data object UndoApplyToAll : PageDetailIntent
    data object ResetPage : PageDetailIntent
}

sealed interface PageDetailEffect {
    data class ShowMessage(val text: UiText) : PageDetailEffect
    data class ShowApplyAllUndo(val count: Int) : PageDetailEffect
    data object Close : PageDetailEffect
}
