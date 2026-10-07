// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.usecase.DeleteDocument
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.domain.usecase.ExportInBlackAndWhite
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.SaveExport
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeExportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import io.github.ukpratik.folio.core.testing.FakeSaveDestinations
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExportUseCasesTest {
    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)
    private val pages = FakePageRepository(clock)
    private val files = FakeDocumentFiles()
    private val engine = FakeExportEngine()
    private val destinations = FakeSaveDestinations()
    private val preferences = FakePreferencesRepository()

    /** A document with a real export on disk: one PDF, or [jpgs] JPG files. */
    private suspend fun exported(title: String, format: ExportFormat = ExportFormat.PDF, jpgs: Int = 2): DocumentId {
        val id = documents.create(title).id
        val names = if (format == ExportFormat.PDF) listOf("$title.pdf") else List(jpgs) { "${title}_0${it + 1}.jpg" }
        val paths = names.map { name -> files.outputFile(id, name).apply { parentFile!!.mkdirs(); writeText(name) }.path }
        documents.markExported(id, ExportResult(format, paths, ByteSize(10), jpgs, null, null, Instant.EPOCH))
        return id
    }

    @Test fun renamingAnExportedDocumentRenamesItsFiles() = runTest {
        val id = exported("Folio_1", ExportFormat.JPG)
        assertThat(RenameDocument(documents, files)(id, "Fees: Sept")).isEqualTo(Outcome.Success("Fees_ Sept"))
        val paths = documents.get(id)!!.lastExport!!.paths
        assertThat(paths.map { it.substringAfterLast('/') }).containsExactly("Fees_ Sept_01.jpg", "Fees_ Sept_02.jpg").inOrder()
        assertThat(paths.all { java.io.File(it).exists() }).isTrue()
    }

    @Test fun renamingADraftTouchesNoFiles() = runTest {
        val id = documents.create("Draft").id
        RenameDocument(documents, files)(id, "Receipt")
        assertThat(documents.get(id)!!.title).isEqualTo("Receipt")
        assertThat(documents.get(id)!!.lastExport).isNull()
    }

    @Test fun savingAPdfCopiesTheOneFile() = runTest {
        val id = exported("Marksheet")
        val outcome = SaveExport(documents, destinations, preferences)(id, "content://doc/1")
        assertThat(outcome).isEqualTo(Outcome.Success(null))
        assertThat(destinations.copies.single()).isEqualTo("content://doc/1" to listOf("Marksheet.pdf"))
    }

    @Test fun savingJpgsUsesTheFolderAndRemembersIt() = runTest {
        val id = exported("Marksheet", ExportFormat.JPG, jpgs = 3)
        val outcome = SaveExport(documents, destinations, preferences)(id, "content://tree/docs")
        assertThat(outcome).isEqualTo(Outcome.Success("Downloads"))
        assertThat(destinations.copies.single().second).hasSize(3)
        assertThat(preferences.preferences.value.lastSaveFolderUri).isEqualTo("content://tree/docs")
    }

    @Test fun unavailableDestinationIsReported() = runTest {
        val id = exported("Marksheet")
        destinations.unavailable = true
        val outcome = SaveExport(documents, destinations, preferences)(id, "content://doc/1")
        assertThat(outcome).isEqualTo(Outcome.Failure(FolioError.SaveTargetUnavailable))
    }

    @Test fun savingWithoutAnExportFails() = runTest {
        val id = documents.create("Draft").id
        assertThat(SaveExport(documents, destinations, preferences)(id, "content://doc/1"))
            .isEqualTo(Outcome.Failure(FolioError.NothingToExport))
    }

    @Test fun blackAndWhiteRetrySetsEveryPageAndExportsWithTheSameSettings() = runTest {
        val id = documents.create("Marksheet").id
        val settings = ExportSettings(quality = QualityPreset.HIGH, target = ByteSize.kb(100))
        documents.saveExportSettings(id, settings)
        pages.insertAll((0 until 3).map { Page(PageId("p$it"), id, it, "s$it", status = PageStatus.READY) })

        ExportInBlackAndWhite(documents, pages, ExportDocument(documents, engine))(id)

        assertThat(pages.observePages(id).first().map { it.mode }.toSet()).containsExactly(EnhancementMode.BW)
        assertThat(engine.started.single()).isEqualTo(id to settings)
    }

    @Test fun deletingCancelsARunningExportFirst() = runTest {
        val id = exported("Marksheet")
        engine.start(id, ExportSettings())
        DeleteDocument(documents, files, engine)(id)
        assertThat(engine.cancelled).containsExactly(id)
        assertThat(documents.get(id)).isNull()
        assertThat(files.outputFile(id, "Marksheet.pdf").exists()).isFalse()
    }
}
