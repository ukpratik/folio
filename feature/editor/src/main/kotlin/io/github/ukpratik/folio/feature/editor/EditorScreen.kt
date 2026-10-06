// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.ui.text.resolveWith

@Composable
internal fun EditorRoute(onBack: () -> Unit, onCreatePdf: (DocumentId) -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EditorEffect.ShowMessage -> Toast.makeText(context, effect.text.resolveWith(context), Toast.LENGTH_LONG).show()
            }
        }
    }
    EditorScreen(state = state, onBack = onBack, onCreatePdf = { onCreatePdf(viewModel.documentId) })
}

/** M1 scaffold: live page list + import progress. The full grid, menus and reordering arrive in M3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(state: EditorState, onBack: () -> Unit, onCreatePdf: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.editor_back)) } },
            )
        },
        bottomBar = {
            Button(
                onClick = onCreatePdf,
                enabled = state.canCreatePdf,
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
            ) { Text(stringResource(R.string.editor_create_pdf)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.importing?.takeIf { it.isRunning }?.let { progress ->
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        pluralStringResource(R.plurals.import_adding, progress.total, progress.total, progress.finished),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { progress.finished.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            Text(
                pluralStringResource(R.plurals.editor_page_count, state.pages.size, state.pages.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.pages, key = { it.id.value }) { page ->
                    val status = when (page.status) {
                        PageStatus.READY -> stringResource(R.string.editor_page_ready)
                        PageStatus.IMPORTING -> stringResource(R.string.editor_page_importing)
                        PageStatus.FAILED -> stringResource(R.string.editor_page_failed)
                    }
                    Text(stringResource(R.string.editor_page_row, page.order + 1, status))
                }
            }
        }
    }
}
