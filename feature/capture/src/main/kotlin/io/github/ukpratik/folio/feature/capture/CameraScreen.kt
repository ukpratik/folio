// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import io.github.ukpratik.folio.core.ui.components.DarkScreenSystemBars
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.ui.components.ConfirmDialog
import io.github.ukpratik.folio.core.ui.components.FolioPrimaryButton
import io.github.ukpratik.folio.core.ui.components.FolioSecondaryButton
import io.github.ukpratik.folio.core.ui.text.resolveWith
import io.github.ukpratik.folio.core.ui.theme.FolioTheme

private val CameraBlack = Color(0xFF0E1012)

@Composable
internal fun CameraRoute(
    onClose: () -> Unit,
    onOpenEditor: (DocumentId, Boolean) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val frame by viewModel.frameAnalyzer.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var controller by remember { mutableStateOf<CameraController?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onIntent(CameraIntent.PermissionResult(granted))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(Limits.MAX_PAGES)) { uris ->
        if (uris.isNotEmpty()) viewModel.onIntent(CameraIntent.ImagesPicked(uris.map { it.toString() }))
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
        viewModel.onIntent(CameraIntent.PermissionChecked(granted, rationale))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraEffect.RequestPermission -> permissionLauncher.launch(Manifest.permission.CAMERA)
                is CameraEffect.TakePicture -> controller?.takePicture(
                    effect.target,
                    onSaved = { viewModel.onIntent(CameraIntent.Captured(Uri.fromFile(it).toString())) },
                    onError = { viewModel.onIntent(CameraIntent.CaptureFailed) },
                ) ?: viewModel.onIntent(CameraIntent.CaptureFailed)
                is CameraEffect.OpenEditor -> onOpenEditor(effect.documentId, effect.isNewDocument)
                is CameraEffect.ShowMessage -> Toast.makeText(context, effect.text.resolveWith(context), Toast.LENGTH_LONG).show()
                CameraEffect.Close -> onClose()
            }
        }
    }
    LaunchedEffect(state.torchOn, controller) { controller?.setTorch(state.torchOn) }
    BackHandler { viewModel.onIntent(CameraIntent.Close) }

    FolioTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(CameraBlack)) {
            when (state.access) {
                null -> Unit
                CameraAccess.GRANTED -> CameraScreen(
                    state = state,
                    frame = frame,
                    hasTorch = controller?.hasTorch == true,
                    onIntent = viewModel::onIntent,
                    preview = {
                        CameraPreview(
                            analyzer = viewModel.frameAnalyzer,
                            quad = frame.quad,
                            onReady = { controller = it },
                            onUnavailable = {
                                Toast.makeText(context, context.getString(R.string.camera_unavailable), Toast.LENGTH_LONG).show()
                                onClose()
                            },
                        )
                    },
                )
                CameraAccess.SHOW_RATIONALE -> PermissionRationale(
                    onContinue = { viewModel.onIntent(CameraIntent.RationaleAccepted) },
                    onNotNow = onClose,
                )
                CameraAccess.DENIED -> CameraDenied(
                    onClose = onClose,
                    onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
            }
        }
    }
}

/** S2b: preview with outline, hint, torch, shutter and the captured-pages stack. */
@Composable
internal fun CameraScreen(
    state: CameraState,
    frame: FrameState,
    hasTorch: Boolean,
    onIntent: (CameraIntent) -> Unit,
    preview: @Composable () -> Unit,
) {
    DarkScreenSystemBars()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { preview() }
            Spacer(Modifier.height(168.dp))
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(onClick = { onIntent(CameraIntent.Close) }) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.camera_close))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { HintPill(frame) }
            if (hasTorch) {
                RoundIcon(onClick = { onIntent(CameraIntent.ToggleTorch) }) {
                    Icon(
                        if (state.torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                        contentDescription = stringResource(if (state.torchOn) R.string.camera_torch_off else R.string.camera_torch_on),
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(168.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.width(64.dp))
            Shutter(enabled = !state.capturing && !state.finishing) { onIntent(CameraIntent.Shutter) }
            CapturedStack(count = state.captured.size) { onIntent(CameraIntent.Done) }
        }
    }

    if (state.confirmClose) {
        ConfirmDialog(
            title = pluralStringResource(R.plurals.camera_keep_title, state.captured.size, state.captured.size),
            text = stringResource(R.string.camera_keep_body),
            confirmLabel = stringResource(R.string.camera_keep),
            dismissLabel = stringResource(R.string.camera_discard),
            onConfirm = { onIntent(CameraIntent.KeepPages) },
            onDismiss = { onIntent(CameraIntent.DismissCloseDialog) },
            onDismissButton = { onIntent(CameraIntent.DiscardPages) },
        )
    }
}

@Composable
private fun HintPill(frame: FrameState) {
    val text = when {
        frame.tooDark -> R.string.camera_hint_dark
        frame.quad != null -> R.string.camera_hint_steady
        else -> R.string.camera_hint_point
    }
    Surface(shape = RoundedCornerShape(20.dp), color = CameraBlack.copy(alpha = 0.62f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (frame.quad != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3FC7B5)))
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(text), style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun RoundIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = CameraBlack.copy(alpha = 0.55f), contentColor = Color.White) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.camera_capture)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(80.dp).semantics { contentDescription = label },
    ) {
        Box(
            Modifier.size(80.dp).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun CapturedStack(count: Int, onDone: () -> Unit) {
    val description = pluralStringResource(R.plurals.camera_done_description, count, count)
    TextButton(
        onClick = onDone,
        enabled = count > 0,
        modifier = Modifier.width(72.dp).semantics { contentDescription = description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Box(Modifier.size(width = 40.dp, height = 52.dp).background(Color(0xFFF4F2EC), RoundedCornerShape(4.dp)))
                if (count > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(start = 30.dp).size(22.dp).clip(CircleShape).background(Color(0xFF3FC7B5)),
                        contentAlignment = Alignment.Center,
                    ) { Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = CameraBlack) }
                }
            }
            Text(stringResource(R.string.camera_done), style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

/** S2a: shown before the system dialog (FR-31). */
@Composable
internal fun PermissionRationale(onContinue: () -> Unit, onNotNow: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f)), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                Text(stringResource(R.string.camera_rationale_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.camera_rationale_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FolioPrimaryButton(stringResource(R.string.camera_continue), onContinue)
                TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.camera_not_now)) }
                Text(
                    stringResource(R.string.camera_rationale_footnote),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** S2c. */
@Composable
internal fun CameraDenied(onClose: () -> Unit, onImport: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.camera_close), tint = Color.White)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).background(Color(0xFF22262B), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.NoPhotography, contentDescription = null, tint = Color(0xFFB9BEC4)) }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.camera_denied_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.camera_denied_body),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB9BEC4),
                textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FolioPrimaryButton(stringResource(R.string.camera_import), onImport)
            FolioSecondaryButton(stringResource(R.string.camera_open_settings), onOpenSettings)
        }
    }
}
