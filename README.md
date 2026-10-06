# Folio: Doc Maker

**Turn photos and paper documents into clean, ready-to-submit PDFs — completely offline.**

Folio is a privacy-first Android app. It scans or imports document photos, crops and cleans them, and creates a PDF (or JPG images) that fits upload size limits. It works without internet: the app has **no network permission**, no account, no ads and no tracking.

> **Status:** early development (project skeleton). Not yet on Google Play or F-Droid.

## Build

Requirements: JDK 17 or newer, Android SDK with platform 37.

```bash
./gradlew assemblePlayDebug      # Google Play flavour
./gradlew assembleFdroidDebug    # F-Droid flavour
./gradlew test                   # unit tests
./gradlew lintPlayDebug          # Android Lint
scripts/check-permissions.sh app/build/outputs/apk/*/release/*.apk   # privacy gate (after assemble*Release)
```

## Project layout

```
app/                 single activity, navigation, Hilt root, manifest
core/model           pure Kotlin domain types
core/domain          use cases + repository interfaces (pure Kotlin)
core/data            Room, DataStore, app-private file store
core/processing      image pipeline, edge detection, PDF writer (in progress)
core/ui              theme (light/dark), Plus Jakarta Sans
core/testing         fakes for tests
feature/*            home, capture, editor, export
build-logic/         Gradle convention plugins
docs/architecture    HLD, LLD, tech stack, ADRs
docs/designs         design canvas source (HTML artboards)
```

Read [`docs/architecture/README.md`](docs/architecture/README.md) before contributing. Architecture decisions live in [`docs/architecture/adr/`](docs/architecture/adr/).

## Privacy guarantees (enforced in CI)

- The release APK may request **only** `android.permission.CAMERA`. Any other permission fails the build.
- Android backup and device transfer are disabled for all app data.
- No analytics, crash-reporting or ad SDKs.

## Licence

Folio is free software, licensed under the **GNU General Public License v3.0 or later** — see [`LICENSE`](LICENSE).

The bundled Plus Jakarta Sans font is under the SIL Open Font License 1.1 ([`core/ui/FONT-LICENSE-OFL.txt`](core/ui/FONT-LICENSE-OFL.txt)).

### Name and branding

The GPL covers the code, not the name. If you publish a modified version, please give it a different name and icon so users don't confuse it with Folio: Doc Maker.
