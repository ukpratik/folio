// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.ukpratik.folio.core.ui.theme.FolioTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // TODO(E2): handle ACTION_SEND / SEND_MULTIPLE here — copy URIs immediately, then open the editor (LLD §7).
        setContent {
            FolioTheme {
                FolioNavHost()
            }
        }
    }
}
