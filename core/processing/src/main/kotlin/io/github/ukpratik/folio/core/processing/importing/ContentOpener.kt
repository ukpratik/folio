// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.importing

import android.content.ContentResolver
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject

/** Opens an import URI. Abstracted so the coordinator is testable without a ContentResolver (DIP). */
fun interface ContentOpener {
    fun open(uri: String): InputStream
}

internal class ContentResolverOpener @Inject constructor(@ApplicationContext context: Context) : ContentOpener {
    private val resolver: ContentResolver = context.contentResolver

    override fun open(uri: String): InputStream =
        resolver.openInputStream(Uri.parse(uri)) ?: throw FileNotFoundException("No stream for $uri")
}
