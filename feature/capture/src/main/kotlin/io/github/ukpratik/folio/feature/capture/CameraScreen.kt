// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

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

/** Placeholder — epic E2 (CameraX, FR-02/03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit, detail: String = "") {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Scan") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Scan — coming in epic E2 (CameraX, FR-02/03)\n$detail")
        }
    }
}
