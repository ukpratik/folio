// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.ui.components.DeleteDocumentDialog
import io.github.ukpratik.folio.core.ui.components.EmptyState
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.components.FolioSecondaryButton
import io.github.ukpratik.folio.core.ui.components.PageImage
import io.github.ukpratik.folio.core.ui.components.RenameDialog
import io.github.ukpratik.folio.core.ui.components.TagChip
import io.github.ukpratik.folio.core.ui.components.WarningTag
import io.github.ukpratik.folio.core.ui.components.pageOutline
import io.github.ukpratik.folio.core.ui.format.RecentDay
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.core.ui.format.recentDay
import io.github.ukpratik.folio.core.ui.format.shortTime
import io.github.ukpratik.folio.core.ui.share.rememberSaveExportLauncher
import io.github.ukpratik.folio.core.ui.share.shareExport
import io.github.ukpratik.folio.core.ui.text.resolveWith
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onScan: () -> Unit,
    onOpenDraft: (DocumentId) -> Unit,
    onOpenExported: (DocumentId) -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Photo Picker: no storage permission on any API level (ADR-0014).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(Limits.MAX_PAGES)) { uris ->
        viewModel.onIntent(HomeIntent.ImagesPicked(uris.map { it.toString() }))
    }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var saving by rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberSaveExportLauncher { uri -> saving?.let { viewModel.onIntent(HomeIntent.SaveTo(DocumentId(it), uri)) } }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenEditor -> onOpenDraft(effect.documentId)
                is HomeEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(effect.text.resolveWith(context)) }
            }
        }
    }
    HomeScreen(
        state = state,
        snackbar = snackbar,
        onIntent = viewModel::onIntent,
        onScan = onScan,
        onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onOpen = { if (it.isDraft) onOpenDraft(it.id) else onOpenExported(it.id) },
        onShare = { recent -> recent.export?.let(context::shareExport) },
        onSave = { recent ->
            recent.export?.let {
                saving = recent.id.value
                save.launch(it, recent.title, state.lastFolderUri)
            }
        },
        onSettings = onSettings,
        onPrivacy = onPrivacy,
    )
}

/** S1 Home: Recents (FR-25) with Scan / Import always within thumb reach. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    snackbar: SnackbarHostState,
    onIntent: (HomeIntent) -> Unit,
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpen: (RecentUi) -> Unit,
    onShare: (RecentUi) -> Unit,
    onSave: (RecentUi) -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
) {
    var renaming by remember { mutableStateOf<RecentUi?>(null) }
    var deleting by remember { mutableStateOf<RecentUi?>(null) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.home_settings)) }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FolioPrimaryButton(stringResource(R.string.home_scan), onScan, Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera)
                FolioSecondaryButton(stringResource(R.string.home_import), onImport, Modifier.weight(1f), icon = Icons.Outlined.PhotoLibrary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onPrivacy).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = stringResource(R.string.home_privacy_line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (!state.loading && state.recents.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.home_recent),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp).semantics { heading() },
                        )
                    }
                    items(state.recents, key = { it.id.value }) { recent ->
                        RecentRow(
                            recent,
                            onOpen = { onOpen(recent) },
                            onShare = { onShare(recent) },
                            onSave = { onSave(recent) },
                            onRename = { renaming = recent },
                            onDelete = { deleting = recent },
                        )
                    }
                }
            }
        }
    }
    renaming?.let { recent ->
        RenameDialog(recent.title, onRename = { onIntent(HomeIntent.Rename(recent.id, it)) }, onDismiss = { renaming = null })
    }
    deleting?.let { recent ->
        DeleteDocumentDialog(recent.title, onConfirm = { deleting = null; onIntent(HomeIntent.Delete(recent.id)) }, onDismiss = { deleting = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentRow(
    recent: RecentUi,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.clickable(onClick = onOpen).padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 52.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).pageOutline()) {
                recent.cover?.let { PageImage(it, sizePx = 200, modifier = Modifier.fillMaxSize()) }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recent.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Wraps at large font sizes so the page count never disappears behind the tag.
                FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        recent.exportInterrupted -> WarningTag(stringResource(R.string.home_export_interrupted))
                        recent.isDraft -> TagChip(stringResource(R.string.home_draft))
                    }
                    Text(meta(recent), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(whenText(recent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.home_more_options, recent.title))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (recent.export != null) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.home_share)) }, onClick = { menuOpen = false; onShare() })
                        DropdownMenuItem(
                            text = { Text(stringResource(if (recent.export.format == ExportFormat.PDF) R.string.home_save else R.string.home_save_folder)) },
                            onClick = { menuOpen = false; onSave() },
                        )
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_rename)) }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun meta(recent: RecentUi): String {
    val export = recent.export
    return if (recent.isDraft || export == null) {
        pluralStringResource(R.plurals.home_pages, recent.pageCount, recent.pageCount)
    } else {
        pluralStringResource(R.plurals.home_pages_size, export.pageCount, export.pageCount, export.size.display())
    }
}

@Composable
private fun whenText(recent: RecentUi): String = when (val day = recent.updatedAt.recentDay()) {
    RecentDay.Today -> stringResource(R.string.home_today_at, recent.updatedAt.shortTime())
    RecentDay.Yesterday -> stringResource(R.string.home_yesterday)
    is RecentDay.On -> day.text
}
