// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.ukpratik.folio.core.model.DocumentTitle
import io.github.ukpratik.folio.core.ui.R

/**
 * Rename (FR-23): live character counter; blank names can't be saved; forbidden characters become "_".
 * [extension] (".pdf") is shown after the field so people see the final file name.
 */
@Composable
fun RenameDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit, extension: String? = null) {
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
                suffix = extension?.let { { Text(it) } },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** FR-25 / UX §4. */
@Composable
fun DeleteDocumentDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.delete_document_title, title),
        text = stringResource(R.string.delete_document_body),
        confirmLabel = stringResource(R.string.delete),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
