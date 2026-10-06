# ADR-0019: Observability without a network

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
With no network (ADR-0013), crash SDKs and analytics are impossible. We still need stability signals and debuggability.

## Decision
- **Production signals:**
  - Play Console **Android vitals** (crash rate, ANR, startup, excessive wakeups) — collected by the OS for users who opted in, with no SDK
  - Play reviews
  - Closed-beta feedback
- **Debug builds:** Timber debug tree, StrictMode (disk/network/leaked closeables), LeakCanary, on-screen frame/memory overlay (dev menu).
- **Release builds:** Timber no-op tree; R8 strips `Log.*`.
- **User-initiated diagnostics (v1.1):** Settings › Send feedback attaches **only** the app version, device model, Android version and an anonymised local event ring buffer (last 50 non-sensitive events, e.g. `export_failed:LowStorage`). The user sees and sends it via their email app.
- **ANR hygiene:** no I/O on the main thread (StrictMode-enforced in debug), and Room main-thread queries are disallowed.

## Options considered
- Crashlytics / Sentry: needs network, so rejected.
- Writing crash logs to a local file and asking users to share them: deferred to 1.1 as part of the diagnostics bundle above.

## Consequences
- Bug reproduction relies on QA's device matrix and the beta group. Invest in deterministic tests and fixtures.
