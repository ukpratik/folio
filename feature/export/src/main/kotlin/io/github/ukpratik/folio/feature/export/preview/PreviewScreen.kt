// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.ExportFileNames
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.feature.export.R

@Composable
internal fun PreviewRoute(onBack: () -> Unit, viewModel: PreviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state.missing) onBack() }
    PreviewScreen(state, onBack)
}

/** S7d: vertical scroll of the real PDF pages, pinch to zoom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreviewScreen(state: PreviewState, onBack: () -> Unit) {
    val pages = rememberPdfPages(state.path)
    val list = rememberLazyListState()
    val current by remember { derivedStateOf { list.firstVisibleItemIndex + 1 } }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.preview_back)) }
                },
                title = {
                    Column {
                        Text(ExportFileNames.pdf(state.title), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        state.size?.let { size ->
                            Text(
                                androidx.compose.ui.res.pluralStringResource(R.plurals.result_pages_size, state.pageCount, state.pageCount, size.display()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val maxX = (scale - 1f) * widthPx / 2f
            pages?.let { pdf ->
                LazyColumn(
                    state = list,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Two fingers zoom and pan; one finger keeps scrolling the list.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                                        val limit = (scale - 1f) * size.width / 2f
                                        offset = Offset((offset.x + event.calculatePan().x).coerceIn(-limit, limit), 0f)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x.coerceIn(-maxX, maxX)
                        },
                ) {
                    items(pdf.pageCount) { index ->
                        PdfPageImage(
                            pdf,
                            index,
                            widthPx = (widthPx * 1.5f).toInt(), // a little extra resolution for zooming
                            modifier = Modifier.fillMaxWidth(),
                            contentDescription = stringResource(R.string.preview_page_description, index + 1),
                        )
                    }
                }
                Text(
                    stringResource(R.string.preview_page_of, current, pdf.pageCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .background(Color(0xCC22262B), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            } ?: Box(Modifier.fillMaxSize())
        }
    }
}

private const val MAX_ZOOM = 4f
