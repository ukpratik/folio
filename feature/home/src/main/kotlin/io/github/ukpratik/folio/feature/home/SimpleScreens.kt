// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Settings (FR-34). Help & FAQ is deferred to v1.1 (D-35). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            TextButton(onClick = onPrivacy) { Text(stringResource(R.string.privacy_title)) }
            // TODO(E1): defaults (page size, quality), send feedback, rate, open-source licences, version.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.privacy_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(stringResource(R.string.privacy_headline), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
