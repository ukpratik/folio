# ADR-0013: Ship with zero INTERNET permission and enforce it

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
The core promise is "your documents never leave your phone" (FR-29/30, D-03). Policy alone is unverifiable. Removing the capability makes the promise verifiable by anyone who inspects the APK.

## Decision
- The release manifest declares **no `android.permission.INTERNET`** and no `ACCESS_NETWORK_STATE`.
- `tools:node="remove"` is used for any permission that a library merges in.
- **A CI gate** parses the merged release manifest (`apkanalyzer manifest permissions`) and fails unless the set ⊆ `{android.permission.CAMERA}`.
- No analytics or crash SDKs. Crash and ANR data comes from Play Console Android vitals only ([ADR-0019](0019-observability-offline.md)).
- **Backup is disabled:** `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules` excluding cloud backup and device transfer.
- **"Rate" and "Feedback" use intents** (Play Store, email app). Folio itself makes no network calls.

## Options considered
- **Opt-in analytics:** rejected for v1 (D-03). It would require INTERNET and weaken the claim. Revisit after launch (O-08).
- **Network permission present but unused:** rejected. It's not verifiable, so it's not a trust differentiator.

## Consequences
- Every new dependency must pass the permission gate. Libraries that require network (e.g. `coil-network-*`) are banned.
- The Play **Data Safety** form can truthfully say "No data collected / shared".
- Any future sync feature (2.0) requires a new ADR, an explicit opt-in design and a separate privacy review.
