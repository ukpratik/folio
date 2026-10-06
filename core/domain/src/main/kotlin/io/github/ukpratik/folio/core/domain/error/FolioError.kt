// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.error

/** Every failure the UI can show. Each maps to one entry in the UX error catalogue (UX spec §8). */
sealed interface FolioError {
    data object LowStorage : FolioError
    data class UnsupportedFormat(val mime: String?) : FolioError
    data class CorruptImage(val cause: Throwable?) : FolioError
    data object TooManyPages : FolioError
    data object CameraUnavailable : FolioError
    data object SaveTargetUnavailable : FolioError
    data object InvalidTitle : FolioError
    data class Unexpected(val cause: Throwable) : FolioError
}

/** Result type for use cases. Exceptions never cross the domain boundary (LLD §8). */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: FolioError) : Outcome<Nothing>
}
