// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentTitle
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/** Creates an empty draft named Folio_YYYYMMDD_HHMM (D-25). */
class CreateDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Document {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault())
        return documents.create(DocumentTitle.default(now))
    }
}
