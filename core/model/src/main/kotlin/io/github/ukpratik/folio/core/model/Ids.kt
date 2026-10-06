// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

import java.util.UUID

@JvmInline value class DocumentId(val value: String) {
    companion object { fun new() = DocumentId(UUID.randomUUID().toString()) }
}

@JvmInline value class PageId(val value: String) {
    companion object { fun new() = PageId(UUID.randomUUID().toString()) }
}

/** Byte count. 1 KB = 1000 bytes, matching how upload portals state limits. */
@JvmInline value class ByteSize(val bytes: Long) {
    companion object {
        fun kb(kb: Long) = ByteSize(kb * 1_000)
        fun mb(mb: Long) = ByteSize(mb * 1_000_000)
    }
}
