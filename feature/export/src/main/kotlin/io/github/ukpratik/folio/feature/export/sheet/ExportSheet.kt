// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.ui.components.ChoiceChipGroup
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.feature.export.R
import java.util.UUID

/**
 * S5 export settings, shown over the editor (:app passes this into the editor's export-sheet slot).
 * A fresh ViewModel per opening, so the sheet always starts from the document's remembered settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheetRoute(documentId: DocumentId, onStarted: () -> Unit, onDismiss: () -> Unit) {
    val session = rememberSaveable { UUID.randomUUID().toString() }
    val viewModel = hiltViewModel<ExportSheetViewModel, ExportSheetViewModel.Factory>(
        key = "export-sheet-$session",
        creationCallback = { it.create(documentId.value) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ExportSheetEffect.Started -> {
                    onDismiss()
                    onStarted()
                }
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ExportSheetContent(state, viewModel::onIntent)
    }
}

@Composable
internal fun ExportSheetContent(state: ExportSheetState, onIntent: (ExportSheetIntent) -> Unit) {
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    val isPdf = state.format == ExportFormat.PDF
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            stringResource(R.string.export_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = { onIntent(ExportSheetIntent.NameChanged(it)) },
            label = { Text(stringResource(R.string.export_name)) },
            suffix = { Text(state.format.extension) },
            singleLine = true,
            isError = state.loaded && state.name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Section(stringResource(R.string.export_format)) {
            ChoiceChipGroup(
                options = ExportFormat.entries,
                selected = state.format,
                label = { stringResource(if (it == ExportFormat.PDF) R.string.format_pdf else R.string.format_jpg) },
                onSelect = { onIntent(ExportSheetIntent.FormatChanged(it)) },
            )
        }
        Section(stringResource(if (isPdf) R.string.export_max_size else R.string.export_max_size_per_image)) {
            ChoiceChipGroup(
                options = SizeChoice.ALL,
                selected = state.size,
                label = { it.label() },
                onSelect = { onIntent(ExportSheetIntent.SizeChosen(it)) },
            )
            AnimatedVisibility(state.size == SizeChoice.Custom) { CustomSize(state, onIntent) }
            if (state.size != SizeChoice.None) Hint(stringResource(R.string.export_target_hint))
        }
        Section(stringResource(R.string.export_quality)) {
            ChoiceChipGroup(
                options = QualityPreset.entries,
                selected = state.quality,
                label = { stringResource(it.labelRes) },
                onSelect = { onIntent(ExportSheetIntent.QualityChanged(it)) },
            )
            Hint(stringResource(state.quality.hintRes))
        }
        if (isPdf) {
            MoreOptionsHeader(state, expanded = moreOptions) { moreOptions = !moreOptions }
            AnimatedVisibility(moreOptions) { MoreOptions(state, onIntent) }
        }
        FolioPrimaryButton(
            text = stringResource(if (isPdf) R.string.export_create else R.string.export_create_images),
            onClick = { onIntent(ExportSheetIntent.Create) },
            enabled = state.canCreate,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CustomSize(state: ExportSheetState, onIntent: (ExportSheetIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.customValue,
            onValueChange = { onIntent(ExportSheetIntent.CustomValueChanged(it)) },
            label = { Text(stringResource(R.string.export_custom_label)) },
            singleLine = true,
            isError = state.showCustomError,
            supportingText = {
                Text(stringResource(if (state.showCustomError) R.string.export_custom_error else R.string.export_custom_hint))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(180.dp),
        )
        ChoiceChipGroup(
            options = SizeUnit.entries,
            selected = state.customUnit,
            label = { stringResource(if (it == SizeUnit.KB) R.string.unit_kb else R.string.unit_mb) },
            onSelect = { onIntent(ExportSheetIntent.CustomUnitChanged(it)) },
        )
    }
}

@Composable
private fun MoreOptionsHeader(state: ExportSheetState, expanded: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.export_more_options), style = MaterialTheme.typography.titleSmall)
            Hint(
                stringResource(
                    R.string.export_more_summary,
                    stringResource(state.pageSize.labelRes),
                    stringResource(state.orientation.labelRes),
                    stringResource(if (state.margin == Margin.NONE) R.string.margin_summary_none else R.string.margin_summary_small),
                ),
            )
        }
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun MoreOptions(state: ExportSheetState, onIntent: (ExportSheetIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Section(stringResource(R.string.export_page_size)) {
            ChoiceChipGroup(PageSize.entries, state.pageSize, { stringResource(it.labelRes) }, { onIntent(ExportSheetIntent.PageSizeChanged(it)) })
            Hint(stringResource(R.string.export_page_size_hint))
        }
        Section(stringResource(R.string.export_orientation)) {
            ChoiceChipGroup(Orientation.entries, state.orientation, { stringResource(it.labelRes) }, { onIntent(ExportSheetIntent.OrientationChanged(it)) })
            Hint(stringResource(R.string.export_orientation_hint))
        }
        Section(stringResource(R.string.export_margins)) {
            ChoiceChipGroup(
                Margin.entries,
                state.margin,
                { stringResource(if (it == Margin.NONE) R.string.margin_none else R.string.margin_small) },
                { onIntent(ExportSheetIntent.MarginChanged(it)) },
            )
        }
    }
}

@Composable
private fun SizeChoice.label(): String = when (this) {
    SizeChoice.None -> stringResource(R.string.size_none)
    SizeChoice.Custom -> stringResource(R.string.size_custom)
    is SizeChoice.Preset -> size.display()
}

private val QualityPreset.labelRes: Int
    get() = when (this) {
        QualityPreset.SMALL -> R.string.quality_small
        QualityPreset.BALANCED -> R.string.quality_balanced
        QualityPreset.HIGH -> R.string.quality_high
    }

private val QualityPreset.hintRes: Int
    get() = when (this) {
        QualityPreset.SMALL -> R.string.quality_small_hint
        QualityPreset.BALANCED -> R.string.quality_balanced_hint
        QualityPreset.HIGH -> R.string.quality_high_hint
    }

private val PageSize.labelRes: Int
    get() = when (this) {
        PageSize.A4 -> R.string.page_size_a4
        PageSize.LETTER -> R.string.page_size_letter
        PageSize.LEGAL -> R.string.page_size_legal
        PageSize.FIT -> R.string.page_size_fit
    }

private val Orientation.labelRes: Int
    get() = when (this) {
        Orientation.AUTO -> R.string.orientation_auto
        Orientation.PORTRAIT -> R.string.orientation_portrait
        Orientation.LANDSCAPE -> R.string.orientation_landscape
    }
