# ADR-0018: Testing strategy

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Decision
| Layer | Tests | Tooling | Target |
|---|---|---|---|
| Domain / use cases | Unit | JUnit, Truth, coroutines-test, fakes | High coverage on rules (order, undo, duplicate, rename sanitising) |
| Processing algorithms | JVM unit + fixture images | OpenCV desktop bindings, golden images with tolerance | Edge detection on the 20-scenario set; enhancement histograms |
| PDF writer + size optimiser | Golden + property tests | PDFBox (test), qpdf | Page count, MediaBox, order, rotation; size ≤ target on fixtures |
| Data | Instrumented | Room MigrationTestHelper, in-memory DB | Every migration |
| ViewModels / coordinators | Unit | Turbine, TestDispatcher | State machines (LLD §6) |
| UI | Compose UI + screenshot | Compose test, Roborazzi | All screens: light/dark/200% font/360 dp |
| End-to-end | Instrumented smoke | Compose test on Managed Devices | Import → edit → export → share intent fired |
| Performance | Macrobenchmark | — | NFR-03/04 budgets on the reference device |
| Privacy | CI gates + QA PV-01…11 | apkanalyzer, exiftool | Release blocker |

**Principles:** prefer fakes to mocks; test behaviour, not implementation; keep fixtures small (downscaled copies), except a separate large-image memory suite that runs nightly.

## Consequences
- The processing engine is designed for JVM testability: pure functions on `Mat`, and Bitmap adapters kept thin.
