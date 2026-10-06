// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.ukpratik.folio.core.ui.theme.FolioTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after recreation the share was already handled.
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            FolioTheme {
                FolioNavHost(mainEffects = viewModel.effects)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** URI grants are tied to this intent, so hand them to the import engine straight away (LLD §7). */
    private fun handleShare(intent: Intent) {
        val uris = intent.sharedImageUris()
        if (uris.isNotEmpty()) viewModel.onImagesShared(uris)
    }
}
