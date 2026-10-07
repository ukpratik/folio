// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeExportEngine
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.feature.export.processing.ProcessingEffect
import io.github.ukpratik.folio.feature.export.processing.ProcessingIntent
import io.github.ukpratik.folio.feature.export.processing.ProcessingState
import io.github.ukpratik.folio.feature.export.processing.ProcessingViewModel
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProcessingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val documents = FakeDocumentRepository()
    private val engine = FakeExportEngine()
    private var id = DocumentId("")

    private suspend fun started(target: ByteSize? = ByteSize.kb(500)): ProcessingViewModel {
        id = documents.create("Doc").id
        val settings = ExportSettings(target = target)
        documents.saveExportSettings(id, settings)
        engine.start(id, settings)
        return ProcessingViewModel(SavedStateHandle(mapOf("documentId" to id.value)), engine, documents, ExportDocument(documents, engine))
    }

    private val result get() = ExportResult(ExportFormat.PDF, listOf("x.pdf"), ByteSize.kb(400), 6, ByteSize.kb(500), true, Instant.EPOCH)

    @Test fun mirrorsProgress() = runTest {
        val vm = started()
        engine.set(id, ExportState.Running(ExportPhase.ENCODING, 2, 6))
        val working = vm.state.value as ProcessingState.Working
        assertThat(working.page).isEqualTo(2)
        assertThat(working.pageCount).isEqualTo(6)
        assertThat(working.target).isEqualTo(ByteSize.kb(500))
        assertThat(working.progress).isWithin(0.001f).of(0.15f + 0.8f * 2 / 6)
    }

    @Test fun successHandsOffOnceAndAcknowledges() = runTest {
        val vm = started()
        vm.effects.test {
            engine.set(id, ExportState.Succeeded(result))
            assertThat(awaitItem()).isEqualTo(ProcessingEffect.Finished(targetMissed = false))
            expectNoEvents() // the acknowledged (null) state must not also trigger Leave
        }
        assertThat(engine.states.value[id]).isNull()
    }

    @Test fun targetMissedOffersAlternatives() = runTest {
        val vm = started()
        vm.effects.test {
            engine.set(id, ExportState.TargetMissed(result.copy(targetMet = false), ByteSize.kb(612)))
            assertThat(awaitItem()).isEqualTo(ProcessingEffect.Finished(targetMissed = true))
        }
    }

    @Test fun backCancelsAndLeaves() = runTest {
        val vm = started()
        vm.effects.test {
            vm.onIntent(ProcessingIntent.Cancel)
            assertThat(engine.cancelled).containsExactly(id)
            assertThat(awaitItem()).isEqualTo(ProcessingEffect.Leave)
        }
    }

    @Test fun lowStorageShowsTheShortfallAndRetries() = runTest {
        val vm = started()
        engine.set(id, ExportState.Failed(FolioError.LowStorage(ByteSize.mb(40))))
        assertThat(vm.state.value).isEqualTo(ProcessingState.LowStorage(ByteSize.mb(40)))

        vm.onIntent(ProcessingIntent.Retry)
        assertThat(engine.started).hasSize(2)
        assertThat(vm.state.value).isInstanceOf(ProcessingState.Working::class.java)
    }

    @Test fun unexpectedFailureShowsTheErrorScreen() = runTest {
        val vm = started()
        engine.set(id, ExportState.Failed(FolioError.Unexpected(IllegalStateException())))
        assertThat(vm.state.value).isEqualTo(ProcessingState.Failed)
        vm.effects.test {
            vm.onIntent(ProcessingIntent.Cancel) // back on the error screen just leaves
            assertThat(awaitItem()).isEqualTo(ProcessingEffect.Leave)
        }
        assertThat(engine.cancelled).isEmpty()
    }
}
