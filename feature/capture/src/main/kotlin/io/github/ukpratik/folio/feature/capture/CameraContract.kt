// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.ui.text.UiText
import java.io.File

data class CameraState(
    val access: CameraAccess? = null,
    /** content/file URIs of pages captured in this session, in order. */
    val captured: List<String> = emptyList(),
    val capturing: Boolean = false,
    val torchOn: Boolean = false,
    val confirmClose: Boolean = false,
    val finishing: Boolean = false,
)

sealed interface CameraIntent {
    data class PermissionChecked(val granted: Boolean, val shouldShowRationale: Boolean) : CameraIntent
    data object RationaleAccepted : CameraIntent
    data class PermissionResult(val granted: Boolean) : CameraIntent
    data object Shutter : CameraIntent
    data class Captured(val uri: String) : CameraIntent
    data object CaptureFailed : CameraIntent
    data object ToggleTorch : CameraIntent
    data object Done : CameraIntent
    data object Close : CameraIntent
    data object KeepPages : CameraIntent
    data object DiscardPages : CameraIntent
    data object DismissCloseDialog : CameraIntent
    data class ImagesPicked(val uris: List<String>) : CameraIntent
}

sealed interface CameraEffect {
    data object RequestPermission : CameraEffect
    data class TakePicture(val target: File) : CameraEffect
    data class OpenEditor(val documentId: DocumentId, val isNewDocument: Boolean) : CameraEffect
    data class ShowMessage(val text: UiText) : CameraEffect
    data object Close : CameraEffect
}
