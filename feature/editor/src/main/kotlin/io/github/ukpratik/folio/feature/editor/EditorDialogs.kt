// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ukpratik.folio.core.model.DocumentTitle

/** Sheets & dialogs board: Add pages chooser (FR-06). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPagesSheet(onScan: () -> Unit, onImport: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.editor_add_pages),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SheetOption(stringResource(R.string.editor_add_scan), { Icon(Icons.Outlined.PhotoCamera, null) }) { onDismiss(); onScan() }
            SheetOption(stringResource(R.string.editor_add_import), { Icon(Icons.Outlined.PhotoLibrary, null) }) { onDismiss(); onImport() }
        }
    }
}

@Composable
private fun SheetOption(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
        ListItem(
            headlineContent = { Text(label, style = MaterialTheme.typography.labelLarge) },
            leadingContent = icon,
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        )
    }
}

/** Rename (FR-23): live character counter; blank names can't be saved; forbidden characters become "_". */
@Composable
internal fun RenameDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(DocumentTitle.MAX_LENGTH) },
                label = { Text(stringResource(R.string.rename_label)) },
                singleLine = true,
                supportingText = {
                    Text(stringResource(R.string.rename_hint_count, text.length, DocumentTitle.MAX_LENGTH))
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(text); onDismiss() }, enabled = DocumentTitle.sanitize(text) != null) {
                Text(stringResource(R.string.rename_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_cancel)) } },
    )
}
