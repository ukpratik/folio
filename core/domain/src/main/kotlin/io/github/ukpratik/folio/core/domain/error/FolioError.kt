// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.error

import io.github.ukpratik.folio.core.model.ByteSize

/** Every failure the UI can show. Each maps to one entry in the UX error catalogue (UX spec §8). */
sealed interface FolioError {
    /** [shortBy] is roughly how much space to free (UX §8: "Free up about {X} MB"). */
    data class LowStorage(val shortBy: ByteSize) : FolioError
    data class UnsupportedFormat(val mime: String?) : FolioError
    data class CorruptImage(val cause: Throwable?) : FolioError
    data object TooManyPages : FolioError
    data object CameraUnavailable : FolioError
    data object SaveTargetUnavailable : FolioError
    data object InvalidTitle : FolioError
    data object NothingToExport : FolioError
    data class Unexpected(val cause: Throwable) : FolioError
}

/** Result type for use cases. Exceptions never cross the domain boundary (LLD §8). */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: FolioError) : Outcome<Nothing>
}
