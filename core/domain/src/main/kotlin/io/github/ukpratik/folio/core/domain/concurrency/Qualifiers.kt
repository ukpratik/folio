// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.concurrency

import javax.inject.Qualifier

/** Blocking I/O: files, streams. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

/** CPU work that isn't image processing. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher

/** Image decode/encode/OpenCV. Bounded to 1–2 threads to cap memory (ADR-0012). */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProcessingDispatcher

/** Process-wide scope for coordinators that must outlive screens (ADR-0012). */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope
