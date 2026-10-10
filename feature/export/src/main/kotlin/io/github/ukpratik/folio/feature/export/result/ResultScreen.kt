// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.components.FolioSecondaryButton
import io.github.ukpratik.folio.core.ui.components.RenameDialog
import io.github.ukpratik.folio.core.ui.components.StatusBadge
import io.github.ukpratik.folio.core.ui.components.StatusTone
import io.github.ukpratik.folio.core.ui.components.pageOutline
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.core.ui.format.sizeRange
import io.github.ukpratik.folio.core.ui.share.rememberSaveExportLauncher
import io.github.ukpratik.folio.core.ui.share.shareExport
import io.github.ukpratik.folio.core.ui.text.resolveWith
import io.github.ukpratik.folio.core.ui.theme.LocalStatusColors
import io.github.ukpratik.folio.feature.export.R
import io.github.ukpratik.folio.feature.export.preview.PdfPageImage
import io.github.ukpratik.folio.feature.export.preview.rememberPdfPages
import io.github.ukpratik.folio.feature.export.sheet.extension
import java.io.File
import kotlinx.coroutines.launch

@Composable
internal fun ResultRoute(
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onReexport: () -> Unit,
    onPreview: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val save = rememberSaveExportLauncher { uri -> viewModel.onIntent(ResultIntent.SaveTo(uri)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ResultEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(effect.text.resolveWith(context)) }
                ResultEffect.Reexporting -> onReexport()
                ResultEffect.Close -> onClose()
            }
        }
    }
    ResultScreen(
        state = state,
        snackbar = snackbar,
        onIntent = viewModel::onIntent,
        onClose = onClose,
        onShare = { state.export?.let(context::shareExport) },
        onSave = { state.export?.let { save.launch(it, state.title, state.lastFolderUri) } },
        onEdit = onEdit,
        onPreview = onPreview,
    )
}

/** S7a success, S7b target missed, S7c over limit, S7e JPG images (FR-18, FR-20–FR-24). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultScreen(
    state: ResultState,
    snackbar: SnackbarHostState,
    onIntent: (ResultIntent) -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
) {
    val export = state.export ?: return
    var showRename by rememberSaveable { mutableStateOf(false) }
    val isPdf = export.format == ExportFormat.PDF
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.result_close)) } },
                title = {
                    Text(
                        stringResource(
                            when {
                                state.showAlternatives -> R.string.result_almost_there
                                isPdf -> R.string.result_pdf_ready
                                else -> R.string.result_images_ready
                            },
                        ),
                        modifier = Modifier.semantics { heading() },
                    )
                },
            )
        },
        bottomBar = { if (!state.showAlternatives) Actions(export, onShare, onSave, { showRename = true }, onEdit) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isPdf) {
                PdfThumbnail(export.paths.single(), onPreview, small = state.showAlternatives)
            }
            Summary(state, export, onRename = { showRename = true })
            if (state.showAlternatives) {
                TargetMissedCard(state, export, onIntent, onEdit)
            } else if (!isPdf) {
                ImageList(state.files)
            }
        }
    }
    if (showRename) {
        RenameDialog(state.title, onRename = { onIntent(ResultIntent.Rename(it)) }, onDismiss = { showRename = false }, extension = export.format.extension)
    }
}

@Composable
private fun PdfThumbnail(path: String, onPreview: () -> Unit, small: Boolean) {
    val pages = rememberPdfPages(path.takeUnless { LocalInspectionMode.current })
    val previewLabel = stringResource(R.string.result_preview_description)
    // Shorter phones: shrink the thumbnail so the name, size and badge stay above the action buttons.
    val fullWidth = ((LocalConfiguration.current.screenHeightDp - 560) * 0.7f).coerceIn(120f, 200f).dp
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(if (small) 120.dp else fullWidth)
                .clip(RoundedCornerShape(12.dp))
                .pageOutline()
                .clickable(onClickLabel = previewLabel, role = Role.Button, onClick = onPreview),
        ) {
            val widthPx = with(LocalDensity.current) { 200.dp.roundToPx() }
            pages?.takeIf { it.pageCount > 0 }?.let { PdfPageImage(it, 0, widthPx, Modifier.fillMaxWidth(), previewLabel) }
                ?: Surface(Modifier.fillMaxWidth().heightIn(min = if (small) 160.dp else 270.dp), color = MaterialTheme.colorScheme.surface) {}
        }
        if (!small) {
            Text(
                stringResource(R.string.result_tap_to_preview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Summary(state: ResultState, export: ExportResult, onRename: () -> Unit) {
    val isPdf = export.format == ExportFormat.PDF
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.clickable(onClickLabel = stringResource(R.string.result_rename), onClick = onRename),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.Outlined.Edit, null, Modifier.padding(start = 8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(meta(export, state.files), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val target = export.target
        if (target != null && !state.showAlternatives) {
            if (state.targetMet == true) {
                StatusBadge(stringResource(if (isPdf) R.string.result_under else R.string.result_each_under, target.display()), StatusTone.SUCCESS)
            } else {
                val over = state.imagesOverLimit
                StatusBadge(
                    if (isPdf) stringResource(R.string.result_over_limit, target.display())
                    else pluralStringResource(R.plurals.result_images_over_limit, over, over, target.display()),
                    StatusTone.WARNING,
                )
                // Only suggest black & white while it can still make a difference.
                val hint = when {
                    isPdf && !state.allBlackAndWhite -> R.string.result_over_limit_hint
                    isPdf -> R.string.result_over_limit_hint_bw
                    !state.allBlackAndWhite -> R.string.result_over_limit_hint_jpg
                    else -> R.string.result_over_limit_hint_jpg_bw
                }
                Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun meta(export: ExportResult, files: List<OutputFile>): String = when (export.format) {
    ExportFormat.PDF -> pluralStringResource(R.plurals.result_pages_size, export.pageCount, export.pageCount, export.size.display())
    ExportFormat.JPG -> {
        val sizes = files.map { it.size.bytes }
        val range = sizeRange(ByteSize(sizes.minOrNull() ?: 0L), ByteSize(sizes.maxOrNull() ?: 0L))
        pluralStringResource(R.plurals.result_images_size_range, files.size, files.size, range)
    }
}

@Composable
private fun TargetMissedCard(state: ResultState, export: ExportResult, onIntent: (ResultIntent) -> Unit, onRemovePages: () -> Unit) {
    val status = LocalStatusColors.current
    val target = export.target?.display().orEmpty()
    val smallest = (state.smallestPossible ?: export.size).display()
    val isJpg = export.format == ExportFormat.JPG
    Surface(color = status.warningContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Warning, null, tint = status.warning)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.result_missed_title, target),
                        style = MaterialTheme.typography.titleMedium,
                        color = status.onWarningContainer,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(if (isJpg) R.string.result_missed_body_jpg else R.string.result_missed_body, smallest),
                        style = MaterialTheme.typography.bodyMedium,
                        color = status.onWarningContainer,
                    )
                }
            }
            // Already all B&W: retrying B&W would give the same file, so don't offer it again.
            if (!state.allBlackAndWhite) {
                FolioPrimaryButton(stringResource(R.string.result_try_bw), { onIntent(ResultIntent.TryBlackAndWhite) })
                FolioSecondaryButton(stringResource(R.string.result_remove_pages), onRemovePages)
            } else {
                FolioPrimaryButton(stringResource(R.string.result_remove_pages), onRemovePages)
            }
            TextButton(onClick = { onIntent(ResultIntent.Keep) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (isJpg) stringResource(R.string.result_keep_images) else stringResource(R.string.result_keep, smallest))
            }
        }
    }
    if (!state.allBlackAndWhite) {
        Text(stringResource(R.string.result_missed_tip), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImageList(files: List<OutputFile>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        files.forEachIndexed { i, file ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(width = 44.dp, height = 58.dp).clip(RoundedCornerShape(6.dp)).pageOutline()) {
                    if (!LocalInspectionMode.current) {
                        AsyncImage(model = File(file.path), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(file.size.display(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Actions(export: ExportResult, onShare: () -> Unit, onSave: () -> Unit, onRename: () -> Unit, onEdit: () -> Unit) {
    val isPdf = export.format == ExportFormat.PDF
    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FolioPrimaryButton(
            text = if (isPdf) stringResource(R.string.result_share) else pluralStringResource(R.plurals.result_share_images, export.paths.size, export.paths.size),
            onClick = onShare,
            icon = Icons.Outlined.Share,
        )
        // Saving is as important as sharing (people re-upload the same file later): same weight as Share.
        FolioPrimaryButton(
            text = stringResource(if (isPdf) R.string.result_save else R.string.result_save_folder),
            onClick = onSave,
            icon = Icons.Outlined.Download,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (isPdf) ActionButton(Icons.Outlined.DriveFileRenameOutline, stringResource(R.string.result_rename), onRename)
            ActionButton(Icons.Outlined.Edit, stringResource(R.string.result_edit), onEdit)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
