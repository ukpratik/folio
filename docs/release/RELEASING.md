# Releasing Folio

Two channels: **F-Droid** (open-source build, F-Droid signs it) and **Google Play** (Play build, signed with our upload key, re-signed by Play App Signing). Both build from the same tag.

## Every release

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (≤ 500 characters, plain text).
3. Gates green: `./gradlew test lintPlayDebug lintFdroidDebug assemblePlayRelease assembleFdroidRelease`, `scripts/check-permissions.sh`, `scripts/check-size.sh`.
4. Commit, then tag and push: `git tag -s v<versionName> -m "Folio <versionName>"` (or `-a` if you don't sign tags) and `git push origin v<versionName>`.
5. **F-Droid** picks the new tag up automatically (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`), usually within a few days.
6. **Play:** `./gradlew bundlePlayRelease` → upload `app/build/outputs/bundle/playRelease/app-play-release.aab` in Play Console (see below).

## Signing

| Key | Where | Used for |
|---|---|---|
| Upload key | `~/.android-keys/folio-upload.jks` (alias `folio-upload`); passwords in `~/.gradle/gradle.properties` (`folio.upload.*`) | Signing what we upload to Play. **Back up the .jks and the password** (password manager + an offline copy). If lost, Play support can reset it. |
| App signing key | Held by Google (Play App Signing) | What users' phones verify for the Play build |
| F-Droid key | Held by F-Droid | The F-Droid build |

The Play and F-Droid builds are signed by different keys, so a user can't switch between them without uninstalling. That's normal.

Upload certificate SHA-256: `B5:BD:DB:7E:2E:99:86:11:8D:CF:2F:7F:08:28:CD:07:0E:AE:05:73:73:F9:CF:2B:C2:DE:2A:4C:0A:6D:A1:6D`

## F-Droid: first submission (one time)

Prerequisites (done): public source on GitHub, GPL-3.0-or-later, `fdroid` flavour with no Google code, fastlane metadata and images in the repo, tag `v1.0.0`, metadata file `fdroid/io.github.ukpratik.folio.yml`.

Verified locally with fdroidserver 2.4.5 against the public tag: `fdroid lint` is clean, `fdroid scanner` finds 0 problems, and `fdroid build` produces `io.github.ukpratik.folio_1.apk`. That APK passes the permission gate (CAMERA only), launches, and shows no update prompt.
- `scandelete: build-logic/convention/build` is needed: F-Droid runs `gradle clean` before scanning, which compiles the convention plugins, and the scanner would otherwise flag those generated `.class` files as binaries.

1. Create a GitLab account (gitlab.com) if you don't have one.
2. Fork https://gitlab.com/fdroid/fdroiddata.
3. In your fork, add `metadata/io.github.ukpratik.folio.yml` with the contents of `fdroid/io.github.ukpratik.folio.yml` from this repo (in the GitLab web editor: *+ → New file*).
4. Commit on a new branch named `io.github.ukpratik.folio`, open a **merge request** to `fdroid/fdroiddata`, and choose the **"App inclusion"** template. Tick its checklist (all items are met; see below).
5. When the website should advertise it, change `site/index.html` back to a "Get it on F-Droid" link (https://f-droid.org/packages/io.github.ukpratik.folio/).
6. The fdroiddata CI builds the app. Reviewers may ask questions in the MR; answer there. After merge, the app appears in F-Droid within about a week.

Checklist answers for the MR template:
- Source is public and the licence is GPL-3.0-or-later (OSI-approved). ✓
- No non-free dependencies in the `fdroid` flavour (Play's in-app update library is `playImplementation` only and never compiled for F-Droid). ✓
- No tracking or ads, and no network permission at all. ✓ No anti-features.
- Builds with the Gradle wrapper and the Android SDK only, with no prebuilt binaries in the repo. OpenCV comes from Maven Central (Apache-2.0). If reviewers ask for OpenCV to be built from source, that's the planned follow-up (ADR-0009 note).

## Google Play: first release (after the organisation account exists)

1. **Create the app:** name "Folio: Doc Maker", default language English (India) or English (United States), App, Free.
2. **Play App Signing:** accept Google-managed key. Upload the first AAB; Play registers our upload key from it.
3. **Store listing:** copy from `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt`, `full_description.txt`. Graphics are in `images/`:
   - App icon: `icon.png` (512×512).
   - Feature graphic: `featureGraphic.png` (1024×500).
   - Phone screenshots: `phoneScreenshots/1.png` to `6.png` (1080×1920).
   - Category: **Productivity**. Contact email: pratikuk99@gmail.com. Website: https://ukpratik.github.io/folio/
4. **Privacy policy URL:** https://ukpratik.github.io/folio/privacy/
5. **App content** (answers below).
6. **Testing:** internal testing first (up to 100 testers, available in minutes; this is also where the update prompt can be tested). Then a closed track if you want wider feedback, then production. Organisation accounts don't need the 12-tester / 14-day closed test.
7. **Countries:** India first (PRD), or all countries.

### App content answers

| Section | Answer |
|---|---|
| Privacy policy | https://ukpratik.github.io/folio/privacy/ |
| Ads | No, the app has no ads |
| App access | All functionality is available without special access (no login) |
| Content rating (IARC) | Category: Utility, Productivity, Communication, or Other. Answer **No** to every question (no violence, sexuality, language, controlled substances, gambling, user-to-user communication or sharing between users, location sharing, or purchases). Expected rating: Everyone / 3+ |
| Target audience | 18+ (or 13+). Not designed for children, to stay out of the Families programme |
| News app | No |
| COVID-19 contact tracing / status | No |
| Data safety | **No data collected, no data shared.** "Does your app collect or share any of the required user data types?" → **No**. Data is processed on device only. No encryption-in-transit question applies (no network). Account creation: No |
| Government app | No |
| Financial features | None |
| Health | No |
| Permissions | CAMERA only (runtime, for scanning). No sensitive-permission declarations needed |
| Photo and video permissions | Not used (Folio uses the system Photo Picker) |

### In-app update priority (D-48)

When you create a release in Play Console you can't set the in-app update priority in the web UI; it's set through the Play Developer API (`inAppUpdatePriority` on the track release, 0–5). Use 4–5 only for critical fixes: those make the update **required**. Everything else uses 0, and users get the friendly reminder according to their Settings.
