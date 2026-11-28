# Folio: Doc Maker

**Turn photos and paper documents into clean, ready-to-submit PDFs — completely offline.**

Folio is a privacy-first Android app. It scans or imports document photos, crops and cleans them, and creates a PDF (or JPG images) that fits upload size limits. It works without internet: the app has **no network permission**, no account, no ads and no tracking.

<a href="https://play.google.com/store/apps/details?id=io.github.ukpratik.folio.play&utm_source=github&utm_medium=badge"><img src="site/badges/google-play.png" alt="Get it on Google Play" height="80"></a>
<a href="https://f-droid.org/packages/io.github.ukpratik.folio/"><img src="site/badges/f-droid.png" alt="Get it on F-Droid" height="80"></a>

## Install

Folio is free and live on both stores:

- **Google Play:** [io.github.ukpratik.folio.play](https://play.google.com/store/apps/details?id=io.github.ukpratik.folio.play&utm_source=github&utm_medium=link), the `play` build flavour.
- **F-Droid:** [io.github.ukpratik.folio](https://f-droid.org/packages/io.github.ukpratik.folio/), built from this source and signed by F-Droid.

The two builds have different package names and signing keys, so they install side by side and do not update each other. Website and privacy policy: https://ukpratik.github.io/folio/

<sub>Google Play and the Google Play logo are trademarks of Google LLC.</sub>

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
core/processing      image pipeline, edge detection, size optimiser, PDF writer
core/ui              theme (light/dark), Plus Jakarta Sans
core/testing         fakes for tests
feature/*            home, capture, editor, export, settings
baselineprofile/     startup Baseline Profile + cold-start benchmark
fdroid/              F-Droid metadata (submitted to fdroiddata)
site/                website + privacy policy (GitHub Pages)
build-logic/         Gradle convention plugins
docs/architecture    HLD, LLD, tech stack, ADRs
docs/designs         design canvas source (HTML artboards)
docs/release         how to release (F-Droid, Google Play)
```

Read [`docs/architecture/README.md`](docs/architecture/README.md) before contributing. Architecture decisions live in [`docs/architecture/adr/`](docs/architecture/adr/).

## Privacy guarantees (enforced in CI)

- The release APK may request **only** `android.permission.CAMERA`. Any other permission fails the build.
- Android backup and device transfer are disabled for all app data.
- No analytics, crash-reporting or ad SDKs.

## Signing certificates

Folio (`io.github.ukpratik.folio`) is published by its developer, Pratik Ukey ([@ukpratik](https://github.com/ukpratik)). SHA-256 certificate fingerprints:

| Key | Fingerprint |
|---|---|
| Google Play upload key (held by the developer) | `B5:BD:DB:7E:2E:99:86:11:8D:CF:2F:7F:08:28:CD:07:0E:AE:05:73:73:F9:CF:2B:C2:DE:2A:4C:0A:6D:A1:6D` |
| F-Droid build (F-Droid builds from this source and signs with its own key) | `35:0C:A3:D7:5C:DB:72:F2:EE:69:CE:B5:43:86:C2:6C:E1:C3:27:3C:38:D5:2D:81:57:42:67:51:BD:6F:8B:B4` |

## Support

Folio is free, with no ads and no tracking. If it's useful to you, you can support its development on [GitHub Sponsors](https://github.com/sponsors/ukpratik). Starring the repo or telling a friend about it helps too.

## Licence

Folio is free software, licensed under the **GNU General Public License v3.0 or later** — see [`LICENSE`](LICENSE).

The bundled Plus Jakarta Sans font is under the SIL Open Font License 1.1 ([`core/ui/FONT-LICENSE-OFL.txt`](core/ui/FONT-LICENSE-OFL.txt)).

### Name and branding

The GPL covers the code, not the name. If you publish a modified version, please give it a different name and icon so users don't confuse it with Folio: Doc Maker.
