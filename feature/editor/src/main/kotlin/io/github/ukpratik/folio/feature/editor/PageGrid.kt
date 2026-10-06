// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.ui.components.PageImage
import io.github.ukpratik.folio.core.ui.components.PageNumberBadge
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private const val ADD_TILE_KEY = "add-pages"
private const val THUMBNAIL_PX = 480

/** Design S3a: tiles are close to square (≈169×176 dp) so ~6 pages fit on a phone screen. */
private const val TILE_ASPECT = 0.96f

/**
 * Two-column page grid (S3a). Long-press and drag to reorder; the ⋮ menu and TalkBack custom actions offer
 * the same moves without dragging (UX §7). The order is committed once, when the drag ends.
 */
@Composable
internal fun PageGrid(
    pages: List<Page>,
    onIntent: (EditorIntent) -> Unit,
    onOpenPage: (PageId) -> Unit,
    onAddPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf(pages) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(pages) { if (!dragging) items = pages }

    val gridState = rememberLazyGridState()
    val haptics = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromIndex = items.indexOfFirst { it.id.value == from.key }
        val toIndex = items.indexOfFirst { it.id.value == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            items = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.id.value }) { page ->
            val number = items.indexOf(page) + 1
            // No fade: items arriving with the first Room emission could stay at alpha 0. Placement animation stays.
            ReorderableItem(
                reorderState,
                key = page.id.value,
                animateItemModifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            ) { isDragging ->
                PageTile(
                    page = page,
                    number = number,
                    total = items.size,
                    lifted = isDragging,
                    onIntent = onIntent,
                    onOpen = { onOpenPage(page.id) },
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStarted = {
                            dragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            dragging = false
                            val target = items.indexOfFirst { it.id == page.id }
                            if (target >= 0 && target != pages.indexOfFirst { it.id == page.id }) {
                                onIntent(EditorIntent.Move(page.id, target))
                            }
                        },
                    ),
                )
            }
        }
        item(key = ADD_TILE_KEY) { AddPagesTile(onClick = onAddPages, modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) }
    }
}

@Composable
private fun PageTile(
    page: Page,
    number: Int,
    total: Int,
    lifted: Boolean,
    onIntent: (EditorIntent) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val description = stringResource(R.string.editor_page_description, number)
    val moveLeft = stringResource(R.string.editor_move_left)
    val moveRight = stringResource(R.string.editor_move_right)
    val rotate = stringResource(R.string.editor_rotate)
    val duplicate = stringResource(R.string.editor_duplicate)
    val delete = stringResource(R.string.editor_delete)

    Box(
        modifier
            .semantics {
                contentDescription = description
                customActions = buildList {
                    if (number > 1) add(CustomAccessibilityAction(moveLeft) { onIntent(EditorIntent.Move(page.id, number - 2)); true })
                    if (number < total) add(CustomAccessibilityAction(moveRight) { onIntent(EditorIntent.Move(page.id, number)); true })
                    add(CustomAccessibilityAction(rotate) { onIntent(EditorIntent.Rotate(page.id)); true })
                    add(CustomAccessibilityAction(duplicate) { onIntent(EditorIntent.Duplicate(page.id)); true })
                    add(CustomAccessibilityAction(delete) { onIntent(EditorIntent.Delete(page.id)); true })
                }
            },
    ) {
        Card(
            onClick = onOpen,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TILE_ASPECT)
                .then(if (lifted) Modifier.shadow(12.dp, MaterialTheme.shapes.medium) else Modifier),
        ) {
            Box(Modifier.fillMaxSize()) {
                PageImage(page = page, sizePx = THUMBNAIL_PX, modifier = Modifier.fillMaxSize().padding(10.dp))
                PageNumberBadge(number, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { menuOpen = true }) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.editor_page_options, number),
                        modifier = Modifier.padding(6.dp).size(20.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                fun select(intent: EditorIntent) {
                    menuOpen = false
                    onIntent(intent)
                }
                PageMenuItem(rotate) { select(EditorIntent.Rotate(page.id)) }
                PageMenuItem(duplicate) { select(EditorIntent.Duplicate(page.id)) }
                if (number > 1) PageMenuItem(moveLeft) { select(EditorIntent.Move(page.id, number - 2)) }
                if (number < total) PageMenuItem(moveRight) { select(EditorIntent.Move(page.id, number)) }
                PageMenuItem(delete, destructive = true) { select(EditorIntent.Delete(page.id)) }
            }
        }
    }
}

@Composable
private fun PageMenuItem(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(label, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        },
        onClick = onClick,
    )
}

@Composable
private fun AddPagesTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .aspectRatio(TILE_ASPECT)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
        Text(
            stringResource(R.string.editor_add_pages),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
