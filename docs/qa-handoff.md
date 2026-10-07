# QA handoff — Folio MVP (M1–M8)

Status on 7 Oct 2026: every P0 requirement in `01-product-requirements.md` is built. Help & FAQ is deferred to v1.1 (D-35). The build is ready for QA's device pass against `04-qa-test-plan.md`.

## Builds

| Build | How | Notes |
|---|---|---|
| Debug (`io.github.ukpratik.folio.debug`) | `./gradlew :app:installPlayDebug` | StrictMode logs and LeakCanary. Installs next to release |
| Release (`io.github.ukpratik.folio`) | `./gradlew assemblePlayRelease`, then sign | R8-minified, baseline profile, the build that ships |
| F-Droid flavour | `assembleFdroidRelease` | Identical apart from the Rate link |

**After installing over an older build:** builds made before M6 used an earlier, unreleased database layout. Clear the app's data (Settings → Apps → Folio → Storage → Clear) if the app closes on launch.

## What to test first

1. **Core journeys (PRD §9):** Scan → fix → fit a size limit → share; Import → reorder → export JPG → save to a folder; share-in from Gallery or WhatsApp.
2. **Target sizes (T20):** 100 KB, 200 KB and 500 KB limits with real marksheets and certificates. Check readability at the floor, the "Couldn't get under…" card, and Try black & white.
3. **Detection on real documents (T01–T14):** the live camera outline has only been tested with synthetic images and the emulator camera. This is the biggest untested area.
4. **Viewer compatibility (NFR-09):** open exports in Drive, Acrobat, Chrome, Files by Google and Samsung viewers, and upload them to real portals.
5. **Performance on the reference phone (NFR-03/04/05):**
   - Cold start ≤ 1.5 s.
   - A 10-page Balanced PDF in ≤ 10 s. Targeted exports take longer; the emulator needed 13 s for 10 noisy pages at 2 MB.
   - 108 MP sources and 100 pages without running out of memory.
6. **Privacy checks (§5):** the release APK declares only CAMERA (`scripts/check-permissions.sh`), no network traffic in airplane mode, no EXIF in outputs, backup excluded.

## Known issues and limits

- A faint 1 px dotted edge can remain on one side of tightly cropped B&W pages (T01/T10).
- Cancel during export is covered by automated tests only. Short exports finish before Back can be pressed on the emulator.
- Export-size results on the emulator came from deliberately noisy synthetic pages (about 82 KB per page at the floor). Real printed pages should be much smaller. Please record real numbers.
- "Rate Folio" opens the web listing on devices without the Play Store.

## Open before release (not QA blockers)

- Feedback email address (`folio.feedbackEmail` in `gradle.properties`) is not set yet.
- Release signing keys and Play Console setup.
- F-Droid: build OpenCV from source instead of the Maven AAR (ADR-0009).
- GitHub: push to `ukpratik/folio` once that account is logged in.
