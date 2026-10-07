// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import io.github.ukpratik.folio.core.model.ExportFileNames
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.ui.R
import java.io.File
import timber.log.Timber

/**
 * FR-21: hands the exported files to the system share sheet through our FileProvider
 * (authority `<applicationId>.files`, exports folder only — LLD §10). Read access is granted per share.
 */
fun Context.shareExport(export: ExportResult) {
    val authority = "$packageName.files"
    val uris = export.paths.map { FileProvider.getUriForFile(this, authority, File(it)) }
    val mime = ExportFileNames.mimeType(export.format)
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }.apply {
        type = mime
        clipData = ClipData.newRawUri(null, uris.first()).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app can receive the share")
    }
}

/** Starts the right system picker for an export: "Save to…" for a PDF, a folder for JPGs (FR-22). */
class SaveExportLauncher internal constructor(
    private val launchDocument: (String) -> Unit,
    private val launchFolder: (Uri?) -> Unit,
) {
    fun launch(export: ExportResult, title: String, lastFolderUri: String?) {
        when (export.format) {
            ExportFormat.PDF -> launchDocument(ExportFileNames.pdf(title))
            ExportFormat.JPG -> launchFolder(lastFolderUri?.let(Uri::parse))
        }
    }
}

/** [onPicked] receives the destination URI; nothing is called if the user backs out of the picker. */
@Composable
fun rememberSaveExportLauncher(onPicked: (String) -> Unit): SaveExportLauncher {
    val document = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return remember(document, folder) {
        SaveExportLauncher(
            launchDocument = { name -> document.launch(name) },
            launchFolder = { initial -> folder.launch(initial?.takeIf { DocumentsContract.isTreeUri(it) }) },
        )
    }
}
