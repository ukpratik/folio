# ADR-0021: Distribute on Google Play and F-Droid

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM (D-39)

## Context
Folio's audience values privacy, and the app has no network, no analytics and no Google Play services dependency. That makes it a natural fit for **F-Droid**, which only publishes free/open-source apps built from source. Google Play stays the main channel for reach in India.

F-Droid requirements that affect us:
- **Source availability:** F-Droid builds the app itself from a public source repository.
- **All dependencies must be FOSS** and buildable. Prebuilt proprietary binaries are rejected. Prebuilt native libraries (e.g. OpenCV `.so` files from a Maven AAR) may be flagged by F-Droid's scanner and may need to be built from source in the F-Droid recipe.
- **Anti-features:** anything like non-free network services, tracking or ads gets labelled. We have none.
- **Signing:** by default F-Droid signs with *its own* key, so Play and F-Droid installs can't update each other. With **reproducible builds**, F-Droid can publish the developer-signed APK instead.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| Play only | Simplest | Misses the privacy-focused audience; weaker trust signal |
| **Play + F-Droid (main repo)** | Reach + credibility; F-Droid audits the source | Needs an open-source licence for the code, an OpenCV build from source, and reproducible-build effort |
| Play + own F-Droid repo / GitHub Releases APK | Full control | Less discoverability; we maintain the repo |

## Decision
Distribute on **Google Play and the main F-Droid repository**, with two product flavours:

| Flavour | Differences |
|---|---|
| `play` | "Rate Folio" opens the Play Store listing. AAB upload with Play App Signing. |
| `fdroid` | "Rate Folio" is hidden or links to the F-Droid page. APK built from tagged source. No Google-specific metadata (e.g. no Photo Picker backport `ModuleDependencies` service entry). |

**Supporting rules:**
1. **Open-source the code under GPL-3.0-or-later** ([ADR-0023](0023-source-licence-gpl3.md)). This is required by F-Droid.
2. **Build OpenCV from source** (core + imgproc), using a Gradle task or an F-Droid `srclibs` recipe. This matches the slim build in ADR-0009 (spike S4).
3. **Aim for reproducible builds:**
   - pinned toolchain versions
   - `SOURCE_DATE_EPOCH`
   - no build timestamps in resources
   - deterministic R8 mapping

   This lets F-Droid ship the developer-signed APK, so users can move between stores.
4. **Fastlane metadata folder** (`fastlane/metadata/android/en-US/`) in the repo. F-Droid reads the listing text and screenshots from it, and the same content feeds Play.
5. **CI:** GitHub Actions builds both flavours. A nightly job checks reproducibility by building twice and diffing the APKs.

## Consequences
- **Positive:** a verifiable privacy story ("audited open source, no network permission"). Users also get a second install channel.
- **Negative:**
  - extra work: OpenCV source build, reproducible-build hygiene, two flavours to QA
  - F-Droid releases trail Play by days, because F-Droid builds on its own schedule
- **Follow-ups:** register the F-Droid metadata merge request after the first Play release.
