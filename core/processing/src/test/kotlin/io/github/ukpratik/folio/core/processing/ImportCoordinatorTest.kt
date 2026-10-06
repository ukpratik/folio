// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ImportJob
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.processing.image.ImageNormalizer
import io.github.ukpratik.folio.core.processing.image.NormalizedImage
import io.github.ukpratik.folio.core.processing.image.UnsupportedImageException
import io.github.ukpratik.folio.core.processing.importing.ImportCoordinator
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakePageRepository
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportCoordinatorTest {
    private val pages = FakePageRepository()
    private val files = FakeDocumentFiles()
    private val doc = DocumentId("doc")

    /** "content://ok/…" decodes; "content://bad/…" is damaged; anything else can't be opened. */
    private val normalizer = object : ImageNormalizer {
        override suspend fun normalize(input: File, output: OutputStream): NormalizedImage {
            if (input.readText() == "bad") throw UnsupportedImageException("damaged")
            output.write(byteArrayOf(1, 2, 3))
            return NormalizedImage(10, 10)
        }
    }

    /** Workers loop forever by design, so they run in the test's backgroundScope. */
    private fun TestScope.coordinator(): ImportCoordinator {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return ImportCoordinator(
        opener = { uri ->
            when {
                uri.startsWith("content://ok") -> "ok".byteInputStream()
                uri.startsWith("content://bad") -> "bad".byteInputStream()
                else -> throw FileNotFoundException(uri)
            }
        },
        normalizer = normalizer,
        files = files,
        pages = pages,
        config = ProcessingConfig(parallelism = 2),
        scope = backgroundScope,
        processing = dispatcher,
        io = dispatcher,
    )
    }

    private suspend fun jobs(vararg uris: String): List<ImportJob> = uris.mapIndexed { i, uri ->
        val page = Page(PageId("p$i"), doc, order = i, sourceId = "s$i", status = PageStatus.IMPORTING)
        pages.insertAll(listOf(page))
        ImportJob(doc, page.id, page.sourceId, ImportSource(uri))
    }

    @Test fun importsGoodPagesAndRemovesFailedOnes() = runTest {
        val coordinator = coordinator()
        coordinator.enqueue(jobs("content://ok/1", "content://bad/2", "content://gone/3", "content://ok/4"))
        assertThat(coordinator.progress.value[doc]).isEqualTo(ImportProgress(total = 4))

        runCurrent()

        assertThat(coordinator.progress.value[doc]).isEqualTo(ImportProgress(total = 4, done = 2, failed = 2))
        assertThat(pages.current.map { it.id.value to it.status }).containsExactly("p0" to PageStatus.READY, "p3" to PageStatus.READY)
        assertThat(files.sourceFile(doc, "s0").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(files.sourceFile(doc, "s1").exists()).isFalse()
    }

    @Test fun pageDeletedDuringImportLeavesNoSource() = runTest {
        val coordinator = coordinator()
        val queued = jobs("content://ok/1")
        coordinator.enqueue(queued)
        pages.deleteHard(listOf(queued.single().pageId))

        runCurrent()

        assertThat(files.sourceFile(doc, "s0").exists()).isFalse()
    }

    @Test fun acknowledgeClearsOnlyFinishedBatches() = runTest {
        val coordinator = coordinator()
        coordinator.enqueue(jobs("content://ok/1"))
        coordinator.acknowledge(doc)
        assertThat(coordinator.progress.value).containsKey(doc)

        runCurrent()
        coordinator.acknowledge(doc)

        assertThat(coordinator.progress.value).doesNotContainKey(doc)
    }
}
