// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.ui.components.DeleteDocumentDialog
import io.github.ukpratik.folio.core.ui.components.EmptyState
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.components.FolioSecondaryButton
import io.github.ukpratik.folio.core.ui.components.RenameDialog
import io.github.ukpratik.folio.core.ui.components.showUndo
import io.github.ukpratik.folio.core.ui.text.resolveWith
import kotlinx.coroutines.launch
import io.github.ukpratik.folio.core.ui.R as CoreUiR

@Composable
internal fun EditorRoute(
    onClose: () -> Unit,
    onScan: (DocumentId) -> Unit,
    onOpenPage: (DocumentId, PageId) -> Unit,
    exportSheet: @Composable (DocumentId, onDismiss: () -> Unit) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    var showExport by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoMessage = stringResource(R.string.editor_page_deleted)
    val undoLabel = stringResource(R.string.editor_undo)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(Limits.MAX_PAGES)) { uris ->
        if (uris.isNotEmpty()) viewModel.onIntent(EditorIntent.AddImages(uris.map { it.toString() }))
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EditorEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(effect.text.resolveWith(context)) }
                is EditorEffect.ShowUndo -> scope.launch {
                    if (snackbar.showUndo(undoMessage, undoLabel)) viewModel.onIntent(EditorIntent.UndoDelete(effect.pageId))
                }
                is EditorEffect.Close -> {
                    effect.message?.let { Toast.makeText(context, it.resolveWith(context), Toast.LENGTH_SHORT).show() }
                    onClose()
                }
            }
        }
    }
    BackHandler { viewModel.onIntent(EditorIntent.Leave) }

    EditorScreen(
        state = state,
        snackbar = snackbar,
        onIntent = viewModel::onIntent,
        onBack = { viewModel.onIntent(EditorIntent.Leave) },
        onScan = { onScan(viewModel.documentId) },
        onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onOpenPage = { onOpenPage(viewModel.documentId, it) },
        onCreatePdf = { showExport = true },
    )
    // The export sheet belongs to :feature:export; :app plugs it in here (features never depend on each other).
    if (showExport) exportSheet(viewModel.documentId) { showExport = false }
}

/** S3a/b/c: stateless; all behaviour arrives through [onIntent] and callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
    state: EditorState,
    snackbar: SnackbarHostState,
    onIntent: (EditorIntent) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpenPage: (PageId) -> Unit,
    onCreatePdf: () -> Unit,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDeleteDocument by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.editor_back))
                    }
                },
                title = {
                    Row(
                        Modifier.clickable { showRename = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(CoreUiR.string.rename_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp).size(18.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.editor_more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(CoreUiR.string.rename_title)) }, onClick = { menuOpen = false; showRename = true })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.editor_delete_document), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; showDeleteDocument = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FolioPrimaryButton(
                    text = stringResource(R.string.editor_create_pdf),
                    onClick = onCreatePdf,
                    enabled = state.canCreatePdf,
                    icon = Icons.Outlined.Description,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.importing?.takeIf { it.isRunning }?.let { progress ->
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            pluralStringResource(R.plurals.import_adding_title, progress.total, progress.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.import_count, progress.finished, progress.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.finished.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    )
                }
            }
            if (state.isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = stringResource(R.string.editor_empty_title),
                        body = stringResource(R.string.editor_empty_body),
                    ) {
                        FolioPrimaryButton(stringResource(R.string.editor_add_scan_short), onScan, Modifier.width(140.dp))
                        FolioSecondaryButton(stringResource(R.string.editor_add_import), onImport, Modifier.width(160.dp))
                    }
                }
            } else {
                Text(
                    pluralStringResource(R.plurals.editor_subtitle, state.pages.size, state.pages.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                )
                PageGrid(
                    pages = state.pages,
                    onIntent = onIntent,
                    onOpenPage = onOpenPage,
                    onAddPages = { showAddSheet = true },
                )
            }
        }
    }

    if (showAddSheet) AddPagesSheet(onScan = onScan, onImport = onImport, onDismiss = { showAddSheet = false })
    if (showRename) RenameDialog(state.title, onRename = { onIntent(EditorIntent.Rename(it)) }, onDismiss = { showRename = false })
    if (showDeleteDocument) {
        DeleteDocumentDialog(
            title = state.title,
            onConfirm = { showDeleteDocument = false; onIntent(EditorIntent.DeleteDocument) },
            onDismiss = { showDeleteDocument = false },
        )
    }
}
