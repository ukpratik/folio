// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** Image URIs from an ACTION_SEND / ACTION_SEND_MULTIPLE intent, or empty for anything else. */
internal fun Intent.sharedImageUris(): List<String> {
    if (type?.startsWith("image/") != true) return emptyList()
    val uris = when (action) {
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    }
    return uris.filter { it.scheme == "content" }.map(Uri::toString)
}
