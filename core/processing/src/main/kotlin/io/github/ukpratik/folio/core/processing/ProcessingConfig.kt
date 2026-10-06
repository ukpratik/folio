// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

/** Parallelism for image work: 1 on low-RAM phones, 2 otherwise (ADR-0012). */
data class ProcessingConfig(val parallelism: Int) {
    init {
        require(parallelism in 1..4)
    }
}
