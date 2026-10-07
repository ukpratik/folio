// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.CreateDocument
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.UserPreferences
import io.github.ukpratik.folio.core.processing.detect.Detection
import io.github.ukpratik.folio.core.processing.detect.EdgeDetector
import io.github.ukpratik.folio.core.processing.detect.GrayImage
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeImportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CameraViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)
    private val pages = FakePageRepository(clock)
    private val engine = FakeImportEngine()
    private val files = FakeDocumentFiles()
    private val prefs = FakePreferencesRepository()
    private val noDetection = object : EdgeDetector {
        override fun detect(gray: GrayImage): Detection? = null
        override fun detect(bitmap: Bitmap): Detection? = null
    }

    private fun viewModel(documentId: String? = null) = CameraViewModel(
        SavedStateHandle(documentId?.let { mapOf(CameraDestination.ARG_DOCUMENT_ID to it) } ?: emptyMap()),
        DocumentFrameAnalyzer(noDetection),
        prefs,
        files,
        AddPages(pages, documents, engine, clock),
        StartDocumentFromImages(CreateDocument(documents, FakePreferencesRepository(), clock), AddPages(pages, documents, engine, clock)),
    )

    @Test fun firstVisitShowsRationaleAndRememberingTheAskLeadsToDeniedScreen() = runTest {
        val vm = viewModel()
        vm.onIntent(CameraIntent.PermissionChecked(granted = false, shouldShowRationale = false))
        assertThat(vm.state.value.access).isEqualTo(CameraAccess.SHOW_RATIONALE)

        vm.effects.test {
            vm.onIntent(CameraIntent.RationaleAccepted)
            assertThat(awaitItem()).isEqualTo(CameraEffect.RequestPermission)
        }
        assertThat(prefs.preferences.value.cameraPermissionRequested).isTrue()

        vm.onIntent(CameraIntent.PermissionResult(granted = false))
        assertThat(vm.state.value.access).isEqualTo(CameraAccess.DENIED)
    }

    @Test fun permanentlyDeniedGoesStraightToDeniedScreen() = runTest {
        prefs.preferences.value = UserPreferences(cameraPermissionRequested = true)
        val vm = viewModel()
        vm.onIntent(CameraIntent.PermissionChecked(granted = false, shouldShowRationale = false))
        assertThat(vm.state.value.access).isEqualTo(CameraAccess.DENIED)
    }

    @Test fun batchCaptureThenDoneStartsANewDocument() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(CameraIntent.Shutter)
            assertThat(awaitItem()).isInstanceOf(CameraEffect.TakePicture::class.java)
            vm.onIntent(CameraIntent.Captured("file:///a.jpg"))
            vm.onIntent(CameraIntent.Shutter)
            awaitItem()
            vm.onIntent(CameraIntent.Captured("file:///b.jpg"))
            assertThat(vm.state.value.captured).hasSize(2)

            vm.onIntent(CameraIntent.Done)
            val open = awaitItem() as CameraEffect.OpenEditor
            assertThat(open.isNewDocument).isTrue()
            assertThat(open.documentId).isEqualTo(documents.all.single().id)
        }
        assertThat(engine.enqueued.map { it.source.uri }).containsExactly("file:///a.jpg", "file:///b.jpg").inOrder()
    }

    @Test fun scanningIntoAnExistingDocumentAddsPages() = runTest {
        val doc = documents.create("Existing")
        val vm = viewModel(documentId = doc.id.value)
        vm.onIntent(CameraIntent.Captured("file:///c.jpg"))
        vm.effects.test {
            vm.onIntent(CameraIntent.Done)
            assertThat(awaitItem()).isEqualTo(CameraEffect.OpenEditor(doc.id, isNewDocument = false))
        }
        assertThat(pages.current).hasSize(1)
    }

    @Test fun closingWithPagesAsksAndDiscardDeletesFiles() = runTest {
        val vm = viewModel()
        val shot = files.newWorkFile("capture", "jpg").apply { writeText("jpeg") }
        vm.onIntent(CameraIntent.Captured(shot.toURI().toString()))
        vm.onIntent(CameraIntent.Close)
        assertThat(vm.state.value.confirmClose).isTrue()

        vm.effects.test {
            vm.onIntent(CameraIntent.DiscardPages)
            assertThat(awaitItem()).isEqualTo(CameraEffect.Close)
        }
        assertThat(shot.exists()).isFalse()
        assertThat(documents.all).isEmpty()
    }

    @Test fun closingWithNoPagesJustCloses() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(CameraIntent.Close)
            assertThat(awaitItem()).isEqualTo(CameraEffect.Close)
        }
    }
}
