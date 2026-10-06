// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

/**
 * An image request for a page with all its edits applied, at about [sizePx] on the long edge.
 * Loaded by Coil through a fetcher in :core:processing (ADR-0016).
 */
data class PageThumbnail(val page: Page, val sizePx: Int)
