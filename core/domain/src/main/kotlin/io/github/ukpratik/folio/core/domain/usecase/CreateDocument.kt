// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentTitle
import io.github.ukpratik.folio.core.model.ExportSettings
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Creates an empty draft named Folio_YYYYMMDD_HHMM (D-25), starting from the Settings defaults (FR-34):
 * the chosen page size (or the region default, PRD §8) and quality.
 */
class CreateDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(region: String = Locale.getDefault().country): Document {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault())
        val prefs = preferences.preferences.first()
        val settings = ExportSettings(
            pageSize = prefs.defaultPageSize ?: ExportSettings.defaultPageSizeFor(region),
            quality = prefs.defaultQuality,
        )
        return documents.create(DocumentTitle.default(now), settings)
    }
}
