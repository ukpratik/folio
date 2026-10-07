// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import io.github.ukpratik.folio.update.UpdateDialog
import io.github.ukpratik.folio.update.UpdateLauncher
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var updateLauncher: UpdateLauncher

    /** Play's update screen. If the user backs out of a required update, it's offered again on resume. */
    private val updateFlow = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after recreation the share was already handled.
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            FolioTheme {
                FolioNavHost(mainEffects = viewModel.effects)
                val prompt by viewModel.updatePrompt.collectAsStateWithLifecycle()
                prompt?.let {
                    UpdateDialog(
                        prompt = it,
                        onUpdate = {
                            viewModel.updateStarted()
                            updateLauncher.launch(updateFlow)
                        },
                        onLater = viewModel::postponeUpdate,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkForUpdate()
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
