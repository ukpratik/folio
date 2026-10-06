# ADR-0017: Build, CI and release pipeline

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Decision
**Build:**
- Gradle Kotlin DSL, version catalog, `build-logic` convention plugins, KSP
- R8 full mode
- Android App Bundle with ABI splits
- **Baseline Profiles** generated via Macrobenchmark for cold start, the editor grid and the export path

**CI (GitHub Actions) on every PR:**
1. Spotless (ktlint), detekt, Android Lint (warnings as errors on new code)
2. Unit tests (JVM) + Robolectric + Roborazzi screenshot verification
3. `assembleRelease` / `bundleRelease`
4. **Permission gate** ([ADR-0013](0013-no-network-enforcement.md)) and **licence gate** (AboutLibraries report; fail on a non-allowlisted licence)
5. **Size gate:** bundletool `get-size total` for arm64 ≤ 25 MB
6. Golden PDF validation with `qpdf --check`
7. Nightly: instrumented tests on Gradle Managed Devices (API 26, 30, 34, latest) + benchmarks

**Release:**
- Play App Signing, upload key held in CI secrets
- Tracks: internal → closed beta (30–50 users) → production with a staged rollout (10% → 50% → 100%)
- Semantic `versionName`, monotonic `versionCode`

## Options considered
- **Other CI (Bitrise, GitLab CI, CircleCI):** all workable. **GitHub Actions confirmed (D-38).**
- **Fastlane vs Gradle Play Publisher:** start with manual upload. Add Gradle Play Publisher when release cadence warrants it.

## Consequences
- The gates protect the product promises (privacy, size) automatically, not by review discipline.
