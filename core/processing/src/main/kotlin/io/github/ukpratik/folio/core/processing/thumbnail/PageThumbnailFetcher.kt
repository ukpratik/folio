// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.thumbnail

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.model.PageThumbnail
import io.github.ukpratik.folio.core.processing.render.PageRenderer
import io.github.ukpratik.folio.core.processing.render.RenderRequest
import javax.inject.Inject

/** Renders a page with its edits for Coil (ADR-0016). Memory-cached by [PageThumbnailKeyer]. */
internal class PageThumbnailFetcher(
    private val data: PageThumbnail,
    private val renderer: PageRenderer,
    private val files: DocumentFiles,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val source = files.sourceFile(data.page.documentId, data.page.sourceId)
        val bitmap = renderer.render(RenderRequest(data.page, source, data.sizePx, forExport = false))
        return ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }
}

/** Registered on the app's ImageLoader; public so :app can wire it while the fetcher stays internal. */
class PageThumbnailFetcherFactory @Inject internal constructor(
    private val renderer: PageRenderer,
    private val files: DocumentFiles,
) : Fetcher.Factory<PageThumbnail> {
    override fun create(data: PageThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
        PageThumbnailFetcher(data, renderer, files)
}

/** Any edit bumps editVersion, so exactly that page's thumbnail is re-rendered. */
class PageThumbnailKeyer @Inject constructor() : Keyer<PageThumbnail> {
    override fun key(data: PageThumbnail, options: Options): String =
        "page:${data.page.sourceId}:${data.page.id.value}:${data.page.editVersion}:${data.sizePx}"
}
