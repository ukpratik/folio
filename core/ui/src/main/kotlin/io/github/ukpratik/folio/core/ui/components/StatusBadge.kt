// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ukpratik.folio.core.ui.theme.LocalStatusColors

enum class StatusTone { SUCCESS, WARNING }

/** Icon + text pill; status is never shown by colour alone (UX §7). */
@Composable
fun StatusBadge(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    val (container, content) = when (tone) {
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.WARNING -> status.warningContainer to status.warning
    }
    Row(
        modifier.background(container, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (tone == StatusTone.SUCCESS) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** Warning-toned [TagChip] ("Export didn't finish"). */
@Composable
fun WarningTag(text: String, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    TagChip(text, modifier, container = status.warningContainer, content = status.warning)
}

/** Small neutral chip used on Recents rows ("Draft"). */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.surfaceContainerHigh, content: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier.background(container, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
