// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.time

/** Injected time source so rules that depend on "now" stay testable. */
interface Clock {
    fun nowMillis(): Long
}

/** When this app process started. Startup recovery only touches data older than this. */
data class SessionInfo(val startedAtMillis: Long)
