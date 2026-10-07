// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeExportEngine
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetEffect
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetIntent
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetViewModel
import io.github.ukpratik.folio.feature.export.sheet.SizeChoice
import io.github.ukpratik.folio.feature.export.sheet.SizeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExportSheetViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val documents = FakeDocumentRepository()
    private val engine = FakeExportEngine()

    private suspend fun document(settings: ExportSettings = ExportSettings()): DocumentId =
        documents.create("Folio_20261007_0900").id.also { documents.saveExportSettings(it, settings) }

    private fun vm(id: DocumentId) =
        ExportSheetViewModel(id.value, documents, engine, RenameDocument(documents, FakeDocumentFiles()), ExportDocument(documents, engine))

    @Test fun prefillsFromTheDocumentsRememberedSettings() = runTest {
        val id = document(ExportSettings(format = ExportFormat.JPG, quality = QualityPreset.HIGH, target = ByteSize.kb(500)))
        val state = vm(id).state.value
        assertThat(state.name).isEqualTo("Folio_20261007_0900")
        assertThat(state.format).isEqualTo(ExportFormat.JPG)
        assertThat(state.quality).isEqualTo(QualityPreset.HIGH)
        assertThat(state.size).isEqualTo(SizeChoice.Preset(ByteSize.kb(500)))
    }

    @Test fun aNonPresetTargetOpensAsCustom() = runTest {
        val id = document(ExportSettings(target = ByteSize(1_500_000)))
        val state = vm(id).state.value
        assertThat(state.size).isEqualTo(SizeChoice.Custom)
        assertThat(state.customValue).isEqualTo("1.5")
        assertThat(state.customUnit).isEqualTo(SizeUnit.MB)
        assertThat(state.target).isEqualTo(ByteSize(1_500_000))
    }

    @Test fun createRenamesSavesSettingsAndStarts() = runTest {
        val id = document()
        val vm = vm(id)
        vm.effects.test {
            vm.onIntent(ExportSheetIntent.NameChanged("Marksheet Class 12"))
            vm.onIntent(ExportSheetIntent.SizeChosen(SizeChoice.Preset(ByteSize.kb(100))))
            vm.onIntent(ExportSheetIntent.QualityChanged(QualityPreset.SMALL))
            vm.onIntent(ExportSheetIntent.Create)
            assertThat(awaitItem()).isEqualTo(ExportSheetEffect.Started)
        }
        val expected = ExportSettings(quality = QualityPreset.SMALL, target = ByteSize.kb(100))
        assertThat(documents.get(id)!!.title).isEqualTo("Marksheet Class 12")
        assertThat(documents.get(id)!!.exportSettings).isEqualTo(expected)
        assertThat(engine.started.single()).isEqualTo(id to expected)
    }

    @Test fun customSizeOutsideTheRangeIsRejected() = runTest {
        val id = document()
        val vm = vm(id)
        vm.onIntent(ExportSheetIntent.SizeChosen(SizeChoice.Custom))
        vm.onIntent(ExportSheetIntent.CustomValueChanged("20")) // 20 KB < 50 KB
        vm.onIntent(ExportSheetIntent.Create)
        assertThat(vm.state.value.showCustomError).isTrue()
        assertThat(engine.started).isEmpty()

        vm.onIntent(ExportSheetIntent.CustomUnitChanged(SizeUnit.MB)) // 20 MB is fine
        assertThat(vm.state.value.showCustomError).isFalse()
        vm.onIntent(ExportSheetIntent.Create)
        assertThat(engine.started.single().second.target).isEqualTo(ByteSize.mb(20))
    }

    @Test fun blankNameCannotCreate() = runTest {
        val vm = vm(document())
        vm.onIntent(ExportSheetIntent.NameChanged("   "))
        vm.onIntent(ExportSheetIntent.Create)
        assertThat(engine.started).isEmpty()
    }

    @Test fun anExportAlreadyRunningGoesStraightToProgress() = runTest {
        val id = document()
        engine.set(id, ExportState.Running(ExportPhase.ENCODING, 1, 3))
        vm(id).effects.test { assertThat(awaitItem()).isEqualTo(ExportSheetEffect.Started) }
    }
}
