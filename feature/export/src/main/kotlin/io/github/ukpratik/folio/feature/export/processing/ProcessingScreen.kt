// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.processing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.components.FolioSecondaryButton
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.feature.export.R

@Composable
internal fun ProcessingRoute(
    onFinished: (targetMissed: Boolean) -> Unit,
    onLeave: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProcessingEffect.Finished -> onFinished(effect.targetMissed)
                ProcessingEffect.Leave -> onLeave()
            }
        }
    }
    BackHandler { viewModel.onIntent(ProcessingIntent.Cancel) }
    ProcessingScreen(state, viewModel::onIntent)
}

/** S6 and the export error states (FR-19, FR-28). */
@Composable
internal fun ProcessingScreen(state: ProcessingState, onIntent: (ProcessingIntent) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (state) {
                    is ProcessingState.Working -> Working(state)
                    is ProcessingState.LowStorage -> Problem(
                        icon = { Icon(Icons.Outlined.SdStorage, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error) },
                        title = stringResource(R.string.error_storage_title),
                        body = stringResource(R.string.error_storage_body, state.shortBy.display()),
                    )
                    ProcessingState.Failed -> Problem(
                        icon = { Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error) },
                        title = stringResource(R.string.error_failed_title),
                        body = stringResource(R.string.error_failed_body),
                    )
                }
            }
            when (state) {
                is ProcessingState.Working -> {
                    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.processing_on_device),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    FolioSecondaryButton(stringResource(R.string.processing_cancel), { onIntent(ProcessingIntent.Cancel) })
                }
                is ProcessingState.LowStorage -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FolioSecondaryButton(stringResource(R.string.error_close), { onIntent(ProcessingIntent.Close) }, Modifier.weight(1f))
                    FolioPrimaryButton(stringResource(R.string.error_retry), { onIntent(ProcessingIntent.Retry) }, Modifier.weight(1f))
                }
                ProcessingState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FolioPrimaryButton(stringResource(R.string.error_retry), { onIntent(ProcessingIntent.Retry) })
                    FolioSecondaryButton(stringResource(R.string.error_back_to_pages), { onIntent(ProcessingIntent.Close) })
                }
            }
        }
    }
}

@Composable
private fun Working(state: ProcessingState.Working) {
    val shrinking = state.phase == ExportPhase.SHRINKING
    val headline = stringResource(
        when {
            shrinking -> R.string.processing_shrinking
            state.format == ExportFormat.JPG -> R.string.processing_images
            else -> R.string.processing_pdf
        },
    )
    val status = when {
        shrinking && state.target != null -> stringResource(R.string.processing_fitting, state.target.display())
        state.phase == ExportPhase.PREPARING || state.pageCount == 0 -> stringResource(R.string.processing_preparing)
        else -> stringResource(R.string.processing_page, (state.page + 1).coerceAtMost(state.pageCount), state.pageCount)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Description, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(headline, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        // Announced by TalkBack as it changes ("Page 3 of 6", UX §7).
        Text(
            status,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth(0.8f))
    }
}

@Composable
private fun Problem(icon: @Composable () -> Unit, title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        icon()
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
