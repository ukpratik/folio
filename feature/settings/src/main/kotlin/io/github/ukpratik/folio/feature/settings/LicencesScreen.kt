// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/** Open-source licences (FR-34), generated at build time by AboutLibraries — no network involved. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicencesRoute(onBack: () -> Unit, viewModel: LicencesViewModel = hiltViewModel()) {
    val libraries by produceLibraries(viewModel.licencesResId)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(stringResource(R.string.settings_licences)) },
            )
        },
    ) { padding ->
        LibrariesContainer(libraries, Modifier.fillMaxSize().padding(padding))
    }
}
