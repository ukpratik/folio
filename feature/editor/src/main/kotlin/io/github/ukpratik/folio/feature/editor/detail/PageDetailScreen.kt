// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FilterBAndW
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageThumbnail
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.ui.components.ConfirmDialog
import io.github.ukpratik.folio.core.ui.components.PageImage
import io.github.ukpratik.folio.core.ui.components.showUndo
import io.github.ukpratik.folio.core.ui.text.resolveWith
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import io.github.ukpratik.folio.feature.editor.R
import kotlinx.coroutines.launch

private const val PREVIEW_PX = 1080

@Composable
internal fun PageDetailRoute(onClose: () -> Unit, viewModel: PageDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.editor_undo)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PageDetailEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(effect.text.resolveWith(context)) }
                is PageDetailEffect.ShowApplyAllUndo -> scope.launch {
                    val message = context.resources.getQuantityString(R.plurals.detail_applied_all, effect.count, effect.count)
                    if (snackbar.showUndo(message, undoLabel)) viewModel.onIntent(PageDetailIntent.UndoApplyToAll)
                }
                PageDetailEffect.Close -> onClose()
            }
        }
    }
    // Page detail is dark in both themes (design S4a/b).
    FolioTheme(darkTheme = true) {
        PageDetailScreen(state, snackbar, viewModel::onIntent, onClose)
    }
}

/** S4a/b, stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageDetailScreen(
    state: PageDetailState,
    snackbar: SnackbarHostState,
    onIntent: (PageDetailIntent) -> Unit,
    onClose: () -> Unit,
    cropImageOverride: ImageBitmap? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                title = {
                    if (state.pages.isNotEmpty()) {
                        Text(
                            stringResource(R.string.detail_title, state.currentIndex + 1, state.pages.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.detail_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_reset_page)) },
                            leadingIcon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                            onClick = { menuOpen = false; confirmReset = true },
                        )
                    }
                    Button(onClick = onClose, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.detail_done)) }
                },
            )
        },
        bottomBar = { TabBar(state.tab) { onIntent(PageDetailIntent.SelectTab(it)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PagesPager(state, onIntent, cropImageOverride, Modifier.weight(1f))
            TabControls(state, onIntent)
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.detail_reset_title),
            text = stringResource(R.string.detail_reset_body),
            confirmLabel = stringResource(R.string.detail_reset_confirm),
            onConfirm = { confirmReset = false; onIntent(PageDetailIntent.ResetPage) },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun PagesPager(state: PageDetailState, onIntent: (PageDetailIntent) -> Unit, cropImageOverride: ImageBitmap?, modifier: Modifier) {
    if (state.pages.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = state.currentIndex) { state.pages.size }
    LaunchedEffect(state.currentIndex) { if (pagerState.currentPage != state.currentIndex) pagerState.scrollToPage(state.currentIndex) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onIntent(PageDetailIntent.SelectPage(it)) }
    }
    HorizontalPager(
        state = pagerState,
        // Swiping would fight the corner handles on the crop tab (UX S4).
        userScrollEnabled = state.tab != DetailTab.CROP,
        key = { state.pages[it].id.value },
        modifier = modifier.fillMaxWidth(),
    ) { index ->
        val page = if (index == state.currentIndex) state.previewPage ?: state.pages[index] else state.pages[index]
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            if (state.tab == DetailTab.CROP) {
                val image = cropImageOverride ?: rememberSourceImage(page)
                CropEditor(image = image, corners = page.corners, onCommit = { onIntent(PageDetailIntent.CommitCorners(it)) })
            } else {
                PageImage(page = page, sizePx = PREVIEW_PX, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** The unedited source at preview size, loaded through Coil (renderer with no edits applied). */
@Composable
private fun rememberSourceImage(page: Page): ImageBitmap? {
    if (LocalInspectionMode.current) return null
    val context = LocalContext.current
    val original = page.copy(corners = null, rotation = Rotation.R0, mode = EnhancementMode.ORIGINAL, adjustments = Adjustments())
    val image by produceState<ImageBitmap?>(null, page.documentId, page.sourceId) {
        val request = ImageRequest.Builder(context).data(PageThumbnail(original, PREVIEW_PX)).build()
        value = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()?.asImageBitmap()
    }
    return image
}

@Composable
private fun TabControls(state: PageDetailState, onIntent: (PageDetailIntent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (state.tab) {
            DetailTab.CROP -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onIntent(PageDetailIntent.AutoCrop) }) {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                    Text(stringResource(R.string.detail_auto), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { onIntent(PageDetailIntent.FullImage) }) {
                    Icon(Icons.Outlined.CropFree, contentDescription = null)
                    Text(stringResource(R.string.detail_full_image), Modifier.padding(start = 8.dp))
                }
            }
            DetailTab.ROTATE -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = { onIntent(PageDetailIntent.Rotate(clockwise = false)) }) {
                    Icon(Icons.AutoMirrored.Outlined.RotateLeft, contentDescription = stringResource(R.string.detail_rotate_left))
                }
                IconButton(onClick = { onIntent(PageDetailIntent.Rotate(clockwise = true)) }) {
                    Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = stringResource(R.string.detail_rotate_right))
                }
            }
            DetailTab.ENHANCE -> EnhanceControls(state, onIntent)
        }
    }
}

@Composable
private fun EnhanceControls(state: PageDetailState, onIntent: (PageDetailIntent) -> Unit) {
    val look = state.enhancement ?: return
    // Four equal tiles with the label underneath (design S4b) so labels fit at 360 dp. Static icons, not live
    // renders (review decision D-29).
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(EnhancementMode.ORIGINAL, Icons.Outlined.Image, R.string.detail_mode_original),
            Triple(EnhancementMode.AUTO, Icons.Outlined.AutoFixHigh, R.string.detail_mode_auto),
            Triple(EnhancementMode.GRAYSCALE, Icons.Outlined.FilterBAndW, R.string.detail_mode_grayscale),
            Triple(EnhancementMode.BW, Icons.Outlined.Contrast, R.string.detail_mode_bw),
        ).forEach { (mode, icon, label) ->
            ModeTile(
                label = stringResource(label),
                icon = icon,
                selected = look.mode == mode,
                onClick = { onIntent(PageDetailIntent.SetMode(mode)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    AdjustmentSlider(
        label = stringResource(R.string.detail_brightness),
        value = look.adjustments.brightness,
        onChange = { onIntent(PageDetailIntent.SetBrightness(it)) },
    )
    AdjustmentSlider(
        label = stringResource(R.string.detail_contrast),
        value = look.adjustments.contrast,
        onChange = { onIntent(PageDetailIntent.SetContrast(it)) },
    )
    OutlinedButton(onClick = { onIntent(PageDetailIntent.ApplyToAll) }, modifier = Modifier.padding(top = 4.dp)) {
        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
        Text(stringResource(R.string.detail_apply_all), Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ModeTile(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .border(if (selected) 2.5.dp else 1.dp, ring, MaterialTheme.shapes.small)
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(88.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = PageDetailViewModel.SLIDER_RANGE,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(0f) }, enabled = value != 0f) {
            Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.detail_reset_slider, label))
        }
    }
}

@Composable
private fun TabBar(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        listOf(
            Triple(DetailTab.CROP, Icons.Outlined.Crop, R.string.detail_tab_crop),
            Triple(DetailTab.ROTATE, Icons.Outlined.Rotate90DegreesCw, R.string.detail_tab_rotate),
            Triple(DetailTab.ENHANCE, Icons.Outlined.AutoFixHigh, R.string.detail_tab_enhance),
        ).forEach { (tab, icon, label) ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}
