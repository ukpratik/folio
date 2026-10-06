// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId

/** Photo Picker import cap per document (FR-01). */
private const val MAX_PICK = 100

@Composable
fun HomeRoute(
    onScan: () -> Unit,
    onOpenDocument: (DocumentId) -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Photo Picker: no storage permission on any API level (ADR-0014).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        viewModel.onImagesPicked(uris.size)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenEditor -> onOpenDocument(effect.documentId)
            }
        }
    }
    HomeScreen(
        state = state,
        onScan = onScan,
        onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onOpenDocument = onOpenDocument,
        onSettings = onSettings,
        onPrivacy = onPrivacy,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpenDocument: (DocumentId) -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium) },
                actions = { TextButton(onClick = onSettings) { Text(stringResource(R.string.home_settings)) } },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onScan, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text(stringResource(R.string.home_scan))
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text(stringResource(R.string.home_import))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = stringResource(R.string.home_privacy_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = onPrivacy).padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (!state.loading && state.recents.isEmpty()) {
                EmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.recents, key = { it.id.value }) { doc ->
                        Card(Modifier.fillMaxWidth().clickable { onOpenDocument(doc.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(doc.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val status = when {
                                    doc.exportInterrupted -> stringResource(R.string.home_export_interrupted)
                                    doc.isDraft -> stringResource(R.string.home_draft)
                                    else -> stringResource(R.string.home_pdf_ready)
                                }
                                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
