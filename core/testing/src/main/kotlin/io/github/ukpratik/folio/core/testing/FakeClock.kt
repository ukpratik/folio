// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.time.Clock

class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowMillis(): Long = now
}
