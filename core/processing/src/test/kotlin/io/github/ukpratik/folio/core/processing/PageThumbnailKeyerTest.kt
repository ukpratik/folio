// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageThumbnail
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.processing.thumbnail.PageThumbnailKeyer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PageThumbnailKeyerTest {
    private val keyer = PageThumbnailKeyer()
    private val page = Page(PageId("p"), DocumentId("d"), 0, "s")
    private fun key(p: Page, size: Int = 480) = keyer.key(PageThumbnail(p, size), coil3.request.Options(RuntimeEnvironment.getApplication()))

    @Test fun editsAndSizesChangeTheKey() {
        val base = key(page)
        assertThat(key(page.copy(rotation = Rotation.R90))).isNotEqualTo(base)
        assertThat(key(page.copy(mode = EnhancementMode.BW))).isNotEqualTo(base)
        assertThat(key(page, size = 1080)).isNotEqualTo(base)
    }

    @Test fun duplicatesWithTheSameEditsShareTheKey() {
        assertThat(key(page.copy(id = PageId("copy"), editVersion = 7))).isEqualTo(key(page))
    }
}
