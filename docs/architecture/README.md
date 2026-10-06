# Folio: Doc Maker — Technical Architecture

**Owner:** Engineering | **Date:** 6 October 2026 | **Status:** Proposed for build
**Inputs:** `../Folio_PRD_v1.1/` (PRD, UX spec, engineering spec, decision log) and the reviewed design canvas.

This folder is the engineering source of truth for **how** Folio is built. The product **what** stays in `Folio_PRD_v1.1/01-product-requirements.md`. If they disagree, raise it in the decision log. Never silently diverge.

## Contents

| File | What it answers | Read it if you… |
|---|---|---|
| [`01-high-level-architecture.md`](01-high-level-architecture.md) | System context, layers, modules, data flows, threading, storage, privacy boundary, quality attributes | are joining the project, reviewing scope, or estimating |
| [`02-low-level-design.md`](02-low-level-design.md) | Packages, key interfaces, data schema, state machines, the processing pipeline, algorithms, error model, memory budgets | are implementing a module or reviewing a PR |
| [`03-tech-stack.md`](03-tech-stack.md) | Every library and tool, with version policy, licence and why it was chosen | are adding or upgrading a dependency |
| [`adr/`](adr/) | Architecture Decision Records: one decision per file, with context, options considered and consequences | want to know *why*, or want to change a decision |

## ADR index

| # | Decision | Status |
|---|---|---|
| [0001](adr/0001-record-architecture-decisions.md) | Use ADRs (MADR format) | Accepted |
| [0002](adr/0002-native-android-kotlin.md) | Native Android in Kotlin (not Flutter, React Native or KMP) | Accepted |
| [0003](adr/0003-jetpack-compose-material3.md) | Jetpack Compose + Material 3 for UI | Accepted |
| [0004](adr/0004-layered-architecture-udf.md) | Layered architecture with unidirectional data flow (MVVM + UDF) | Accepted |
| [0005](adr/0005-modularisation.md) | Feature + core Gradle modules with convention plugins | Accepted |
| [0006](adr/0006-dependency-injection-hilt.md) | Hilt for dependency injection | Accepted |
| [0007](adr/0007-persistence-room-datastore-files.md) | Room + DataStore + app-private file store | Accepted |
| [0008](adr/0008-camera-camerax.md) | CameraX for capture and live analysis | Accepted |
| [0009](adr/0009-image-processing-opencv.md) | OpenCV for edge detection, perspective and enhancement | Accepted, pending spike S1/S4 |
| [0010](adr/0010-pdf-writer-in-house.md) | In-house streaming PDF writer embedding JPEG | Accepted, pending spike S2 |
| [0011](adr/0011-target-size-compression.md) | Two-pass, per-page-budget target-size algorithm | Accepted, pending spike S2 |
| [0012](adr/0012-concurrency-and-background-work.md) | Coroutines with app-scoped coordinators; no WorkManager or foreground service | Accepted |
| [0013](adr/0013-no-network-enforcement.md) | Ship with zero INTERNET permission and enforce it in CI | Accepted |
| [0014](adr/0014-storage-access-without-permissions.md) | Photo Picker + SAF + FileProvider; no storage permissions | Accepted |
| [0015](adr/0015-navigation-compose-type-safe.md) | Navigation Compose with type-safe routes | Accepted |
| [0016](adr/0016-image-loading-coil.md) | Coil (no network module) for thumbnails | Accepted |
| [0017](adr/0017-build-ci-release.md) | Gradle KTS, version catalog, GitHub Actions, R8, Baseline Profiles | Accepted |
| [0018](adr/0018-testing-strategy.md) | Testing pyramid with JVM-first processing tests | Accepted |
| [0019](adr/0019-observability-offline.md) | Observability without network (Play vitals + local diagnostics) | Accepted |
| [0020](adr/0020-min-sdk-and-device-support.md) | minSdk 26, targetSdk = Play requirement, phones first | Accepted |
| [0021](adr/0021-distribution-play-and-fdroid.md) | Distribute on Google Play and F-Droid with `play`/`fdroid` flavours | Accepted |
| [0022](adr/0022-application-id.md) | Application ID `io.github.ukpratik.folio` | Accepted |
| [0023](adr/0023-source-licence-gpl3.md) | Source code licensed GPL-3.0-or-later; dependencies must be GPL-3.0-compatible | Accepted |

To add a new ADR, copy the template in ADR-0001 and take the next number. Never edit an accepted ADR's decision. Supersede it with a new ADR and link both ways.
