# Folio — Tech Stack

**Version policy:** pin the **latest stable** of each library at project start in `gradle/libs.versions.toml`. Upgrade monthly through Dependabot PRs, which must pass CI including the permission and size gates. No alphas or betas in release builds, except where an ADR allows it.

**Licence policy:** Folio's own code is **GPL-3.0-or-later** ([ADR-0023](adr/0023-source-licence-gpl3.md)). Shipped dependencies must be **GPL-3.0-compatible and FOSS**: Apache 2.0, MIT, BSD, SIL OFL (fonts), LGPL, or GPL-3.0. No GPL-2.0-only, no AGPL, no proprietary binaries. The licence report (AboutLibraries) is generated at build time and shown in Settings.

## Languages and build

| Item | Choice | Licence | Notes |
|---|---|---|---|
| Language | **Kotlin** (K2 compiler) | Apache 2.0 | [ADR-0002](adr/0002-native-android-kotlin.md) |
| JVM target | Java 17 toolchain | — | Required by current AGP |
| Build | Gradle (Kotlin DSL) + Android Gradle Plugin + version catalog + `build-logic` convention plugins | Apache 2.0 | [ADR-0017](adr/0017-build-ci-release.md) |
| Annotation processing | KSP (Room, Hilt) | Apache 2.0 | KSP rather than kapt, for build speed |
| Serialization | kotlinx.serialization | Apache 2.0 | Navigation routes only |

## Runtime libraries

| Area | Library | Licence | Why |
|---|---|---|---|
| UI | Jetpack Compose (BOM), Material 3, `material-icons-extended` (or a curated vector subset) | Apache 2.0 | [ADR-0003](adr/0003-jetpack-compose-material3.md) |
| App structure | AndroidX Activity, Lifecycle (ViewModel, runtime-compose), Core KTX | Apache 2.0 | Standard |
| Navigation | Navigation Compose (type-safe routes) | Apache 2.0 | [ADR-0015](adr/0015-navigation-compose-type-safe.md) |
| DI | Hilt (Dagger) + `hilt-navigation-compose` | Apache 2.0 | [ADR-0006](adr/0006-dependency-injection-hilt.md) |
| Concurrency | kotlinx.coroutines | Apache 2.0 | [ADR-0012](adr/0012-concurrency-and-background-work.md) |
| Database | Room (KTX, KSP) | Apache 2.0 | [ADR-0007](adr/0007-persistence-room-datastore-files.md) |
| Preferences | DataStore (Preferences) | Apache 2.0 | |
| Camera | CameraX (core, camera2, lifecycle, view) | Apache 2.0 | [ADR-0008](adr/0008-camera-camerax.md) |
| Image processing | OpenCV for Android (slim build: core + imgproc) | Apache 2.0 | [ADR-0009](adr/0009-image-processing-opencv.md) |
| EXIF | `androidx.exifinterface` | Apache 2.0 | |
| Thumbnails | Coil 3 (`coil-compose` **without** any `coil-network-*` artifact) | Apache 2.0 | [ADR-0016](adr/0016-image-loading-coil.md) |
| Grid reorder | `sh.calvin.reorderable` | Apache 2.0 | D-33 |
| PDF write | In-house `StreamingPdfWriter` | — | [ADR-0010](adr/0010-pdf-writer-in-house.md) |
| PDF preview | `android.graphics.pdf.PdfRenderer` (platform) | — | |
| Licences screen | AboutLibraries | Apache 2.0 | |
| Logging | Timber (no-op tree in release) | Apache 2.0 | [ADR-0019](adr/0019-observability-offline.md) |
| Startup | `androidx.profileinstaller` (Baseline Profiles) | Apache 2.0 | NFR-03 |
| Font | Plus Jakarta Sans (bundled, 4 weights) | SIL OFL 1.1 | Decided (D-36) |

## Debug-only

| Tool | Licence | Purpose |
|---|---|---|
| LeakCanary | Apache 2.0 | Memory leaks (debug builds only) |
| StrictMode (platform) | — | Disk and network on main thread, leaked closeables |

## Testing

| Tool | Licence | Use |
|---|---|---|
| JUnit 4 + Google Truth | EPL / Apache | Unit assertions |
| kotlinx-coroutines-test + Turbine | Apache 2.0 | Coroutines and Flows |
| MockK (sparingly; prefer fakes) | Apache 2.0 | Platform-edge mocking |
| Robolectric | MIT | JVM tests for Android-touching code |
| Compose UI Test | Apache 2.0 | Screen behaviour, semantics |
| Roborazzi | Apache 2.0 | Screenshot tests (light/dark/200% font/360 dp) |
| Room `MigrationTestHelper` | Apache 2.0 | Schema migrations |
| Apache PDFBox (test only) | Apache 2.0 | Parse and verify generated PDFs |
| OpenCV Java desktop bindings (test only) | Apache 2.0 | JVM tests of detection and enhancement |
| Macrobenchmark + Baseline Profile generator | Apache 2.0 | Startup, export benchmark |

## Tooling and CI

| Tool | Use |
|---|---|
| GitHub Actions | CI: lint, detekt, unit tests, assemble AAB, permission gate, size gate, licence report, `qpdf --check` on golden PDFs |
| Android Lint + detekt + ktlint (Spotless) | Static analysis and formatting |
| bundletool | Download-size measurement |
| Firebase Test Lab / Gradle Managed Devices | Instrumented tests across API levels. Managed Devices preferred (no external account needed) |
| Play Console | Internal → closed beta → staged production; Android vitals |

## Explicitly not used (and why)

| Library | Reason |
|---|---|
| Firebase (Analytics, Crashlytics), Sentry, any analytics SDK | Requires network; violates D-03 / FR-30 |
| Google Play services ML Kit Document Scanner | Pulls in Play services and downloads its model; its own UI conflicts with our UX; not usable on de-Googled devices |
| iText | AGPL licence |
| PdfBox-Android (runtime) | Large; we only need to write JPEG pages. Kept as the fallback in ADR-0010 |
| Retrofit / OkHttp / Ktor | No network |
| RxJava | Coroutines/Flow cover our needs |
| Glide | Coil is Compose-first and Kotlin-native; either works |
