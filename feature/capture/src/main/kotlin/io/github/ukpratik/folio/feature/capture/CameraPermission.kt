// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

/** LLD §6.2: what to show when the user taps Scan. */
enum class CameraAccess { GRANTED, SHOW_RATIONALE, DENIED }

object CameraPermission {
    /**
     * Android's shouldShowRequestPermissionRationale is false both before the first request and after
     * "Don't ask again", so we also remember whether we've asked before (DataStore).
     */
    fun resolve(granted: Boolean, requestedBefore: Boolean, shouldShowRationale: Boolean): CameraAccess = when {
        granted -> CameraAccess.GRANTED
        !requestedBefore || shouldShowRationale -> CameraAccess.SHOW_RATIONALE
        else -> CameraAccess.DENIED
    }

    /** UX S2c: any denial in the moment shows "Camera access is off" (with Import images / Open settings). */
    fun afterRequest(granted: Boolean): CameraAccess = if (granted) CameraAccess.GRANTED else CameraAccess.DENIED
}
