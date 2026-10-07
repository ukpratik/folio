// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.model.UpdateReminder

@Composable
internal fun SettingsRoute(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onLicences: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onPrivacy = onPrivacy,
        onFeedback = { context.sendFeedback(state.appInfo) },
        onRate = { context.openStoreListing(state.appInfo) },
        onLicences = onLicences,
    )
}

private enum class Picker { PAGE_SIZE, QUALITY, UPDATES }

/** S8 Settings (FR-34). Help & FAQ is deferred to v1.1 (D-35). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onFeedback: () -> Unit,
    onRate: () -> Unit,
    onLicences: () -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group(stringResource(R.string.settings_defaults)) {
                Row(Icons.Outlined.Description, stringResource(R.string.settings_page_size), stringResource(state.pageSize.labelRes)) { picker = Picker.PAGE_SIZE }
                RowDivider()
                Row(Icons.Outlined.Tune, stringResource(R.string.settings_quality), stringResource(state.quality.labelRes)) { picker = Picker.QUALITY }
            }
            if (state.appInfo.supportsUpdatePrompts) {
                Group(stringResource(R.string.settings_updates)) {
                    Row(Icons.Outlined.SystemUpdate, stringResource(R.string.settings_update_reminders), stringResource(state.updateReminder.labelRes)) {
                        picker = Picker.UPDATES
                    }
                }
            }
            Group(stringResource(R.string.settings_privacy)) {
                Row(Icons.Outlined.Lock, stringResource(R.string.privacy_title), stringResource(R.string.settings_privacy_sub), onPrivacy)
            }
            Group(stringResource(R.string.settings_about)) {
                Row(Icons.Outlined.MailOutline, stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_sub), onFeedback)
                RowDivider()
                Row(Icons.Outlined.StarOutline, stringResource(R.string.settings_rate), stringResource(R.string.settings_rate_sub), onRate)
                RowDivider()
                Row(Icons.Outlined.Code, stringResource(R.string.settings_licences), null, onLicences)
            }
            Text(
                stringResource(R.string.settings_version, state.appInfo.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    when (picker) {
        Picker.PAGE_SIZE -> ChoiceDialog(
            title = stringResource(R.string.settings_page_size),
            options = PageSize.entries,
            selected = state.pageSize,
            label = { stringResource(it.labelRes) },
            onSelect = { onIntent(SettingsIntent.SetPageSize(it)) },
            onDismiss = { picker = null },
        )
        Picker.QUALITY -> ChoiceDialog(
            title = stringResource(R.string.settings_quality),
            options = QualityPreset.entries,
            selected = state.quality,
            label = { stringResource(it.labelRes) },
            supporting = { stringResource(it.hintRes) },
            onSelect = { onIntent(SettingsIntent.SetQuality(it)) },
            onDismiss = { picker = null },
        )
        Picker.UPDATES -> ChoiceDialog(
            title = stringResource(R.string.settings_update_reminders),
            options = UpdateReminder.entries,
            selected = state.updateReminder,
            label = { stringResource(it.labelRes) },
            onSelect = { onIntent(SettingsIntent.SetUpdateReminder(it)) },
            onDismiss = { picker = null },
            footer = stringResource(R.string.settings_update_critical_note),
        )
        null -> Unit
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Column(content = content) }
    }
}

@Composable
private fun Row(icon: ImageVector, label: String, sub: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = sub?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    supporting: (@Composable (T) -> String)? = null,
    footer: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option); onDismiss() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                            supporting?.let {
                                Text(it(option), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                footer?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

internal val PageSize.labelRes: Int
    get() = when (this) {
        PageSize.A4 -> R.string.page_size_a4
        PageSize.LETTER -> R.string.page_size_letter
        PageSize.LEGAL -> R.string.page_size_legal
        PageSize.FIT -> R.string.page_size_fit
    }

private val UpdateReminder.labelRes: Int
    get() = when (this) {
        UpdateReminder.EVERY_LAUNCH -> R.string.update_every_launch
        UpdateReminder.DAILY -> R.string.update_daily
        UpdateReminder.WEEKLY -> R.string.update_weekly
    }

internal val QualityPreset.labelRes: Int
    get() = when (this) {
        QualityPreset.SMALL -> R.string.quality_small
        QualityPreset.BALANCED -> R.string.quality_balanced
        QualityPreset.HIGH -> R.string.quality_high
    }

private val QualityPreset.hintRes: Int
    get() = when (this) {
        QualityPreset.SMALL -> R.string.quality_small_hint
        QualityPreset.BALANCED -> R.string.quality_balanced_hint
        QualityPreset.HIGH -> R.string.quality_high_hint
    }
