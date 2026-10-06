# ADR-0012: Coroutines with app-scoped coordinators; no WorkManager or foreground service

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
Imports (up to 100 images) and exports take seconds to tens of seconds. They must survive rotation and navigation, be cancellable, and avoid the notification permission (D-23).

## Options considered
| Option | Pros | Cons |
|---|---|---|
| viewModelScope only | Simple | Work dies if the user navigates away (e.g. back to Home during an import) |
| **@Singleton coordinators with an application-scoped `CoroutineScope` + bounded dispatchers** | Survive navigation and config changes; observable via StateFlow; cancellable; no extra permission | Lost on process death (handled by recovery, LLD §3.4) |
| WorkManager (expedited) | Survives process death | Expedited work on Android 12+ may require a foreground notification; serialisation overhead; more complex progress; exports only take seconds |
| Foreground service | Robust in the background | Needs the notification permission (Android 13+) and a foreground-service type; heavier UX; not justified |

## Decision
- **Kotlin coroutines everywhere.**
- `ImportCoordinator` and `ExportCoordinator` are singletons running in `@ApplicationScope` (`SupervisorJob() + Dispatchers.Default`).
- Processing runs on `Dispatchers.Default.limitedParallelism(n)`, with n = 1 when `ActivityManager.isLowRamDevice` or memory class < 256 MB, otherwise 2.
- **Process death is handled by recovery**, not prevented:
  - sources are copied first
  - page status is persisted
  - an interrupted export is flagged

## Consequences
- No notification permission and no foreground service type in the manifest.
- If the OS kills the app in the background mid-export, the user retries. This is acceptable for multi-second jobs, and the UX shows "Export didn't finish".
- Revisit (new ADR) if real-world data shows frequent background kills during long exports.
