// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.update

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import io.github.ukpratik.folio.R
import io.github.ukpratik.folio.core.domain.update.UpdatePrompt

/** D-48 update prompt. A required (critical) update can't be dismissed or postponed. */
@Composable
internal fun UpdateDialog(prompt: UpdatePrompt, onUpdate: () -> Unit, onLater: () -> Unit) {
    val required = prompt == UpdatePrompt.REQUIRE
    AlertDialog(
        onDismissRequest = { if (!required) onLater() },
        properties = DialogProperties(dismissOnBackPress = !required, dismissOnClickOutside = !required),
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(if (required) R.string.update_required_title else R.string.update_title)) },
        text = { Text(stringResource(if (required) R.string.update_required_body else R.string.update_body)) },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_now)) } },
        dismissButton = if (required) null else { { TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) } } },
    )
}
