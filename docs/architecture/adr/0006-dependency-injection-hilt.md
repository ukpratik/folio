# ADR-0006: Hilt for dependency injection

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Hilt** | Official; compile-time safe; integrates with ViewModel, Navigation and WorkManager; KSP support | Annotation processing cost; Dagger error messages |
| Koin | Simple DSL, no codegen | Runtime resolution errors; slight startup cost; less compile-time safety |
| Manual DI | Zero dependency | Boilerplate grows with coordinators and engines |
| kotlin-inject / Metro | Lightweight compile-time | Smaller ecosystem; less Android integration |

## Decision
Use **Hilt with KSP**.
- Coordinators (`ImportCoordinator`, `ExportCoordinator`) and engines are `@Singleton`.
- The application `CoroutineScope` is provided as `@ApplicationScope`.
- Dispatchers are injected through qualifiers (`@IoDispatcher`, `@ProcessingDispatcher`) so tests can swap them.

## Consequences
- Nothing heavy is constructed at startup. OpenCV loading and DB opening are lazy (`Lazy<T>` / `Provider<T>`), which protects NFR-03.
