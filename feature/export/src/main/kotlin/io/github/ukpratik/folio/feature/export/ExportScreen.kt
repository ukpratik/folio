// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — epics E6–E8 (FR-14–FR-24). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(onBack: () -> Unit, detail: String = "") {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Create PDF") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Create PDF — coming in epics E6–E8 (FR-14–FR-24)\n$detail")
        }
    }
}
