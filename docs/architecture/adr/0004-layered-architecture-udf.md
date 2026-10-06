# ADR-0004: Layered architecture with unidirectional data flow (MVVM + UDF)

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
State changes come from many sources: user edits, Room emissions, import and export progress, and process-death recovery. The app must survive configuration changes and process death without losing work (FR-26).

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **MVVM + UDF** (ViewModel exposes `StateFlow<State>`, accepts `Intent`s, emits one-shot `Effect`s), with use cases in a domain layer | Google-recommended; simple; testable; no framework lock-in | Some boilerplate per screen |
| MVI framework (Orbit, Mavericks) | Strong conventions | Extra dependency and learning curve for little gain at our size |
| MVP | Familiar to some | Imperative view updates fight Compose |
| "Clean Architecture" with a mapper per layer | Strict isolation | Over-engineering for about 10 entities; mapper fatigue |

## Decision
- **Layers:**
  - UI (Compose)
  - Presentation (ViewModel)
  - Domain (use cases + interfaces, pure Kotlin)
  - Data (Room/DataStore/files)
  - Processing engine
- **Pattern:** MVVM with UDF.
  - `State` is derived from Room `Flow`s via `combine` + `stateIn(WhileSubscribed(5s))`.
  - Effects are delivered through a `Channel`.
- **Use cases only where logic lives.** Trivial pass-through reads may call the repository directly from the ViewModel. This is pragmatic, not dogmatic.
- **Persisted DB state is the single source of truth.** ViewModels hold no authoritative copies.

## Consequences
- Process death: on restore, the screen re-reads from Room using `documentId` from `SavedStateHandle`. Nothing is lost.
- Long-running work belongs to **coordinators**, not ViewModels ([ADR-0012](0012-concurrency-and-background-work.md)).
