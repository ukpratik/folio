# ADR-0005: Feature + core Gradle modules with convention plugins

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
The goals are fast incremental builds, enforced boundaries (pure domain, processing isolated from UI) and parallel work by several developers, without over-modularising a small app.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| Single module | Simplest | No enforced boundaries; slower incremental builds as code grows; harder to test processing in isolation |
| **About 10 modules: `:app`, `:core:{model,domain,data,processing,ui,testing}`, `:feature:{home,capture,editor,export}`** | Clear ownership; domain stays pure; processing is testable alone; parallel builds | Some Gradle setup (solved with convention plugins) |
| Fine-grained (api/impl per feature, 25+ modules) | Maximum isolation | Overkill; slows the team |

## Decision
Use the roughly 10-module layout in HLD §4.

**Dependency rules:**
- Features depend on `:core:*` only, never on each other.
- `:core:domain` depends only on `:core:model`.
- `:core:processing` depends on `:core:model` and `:core:domain` (for interfaces) and must not depend on `:core:data` or UI.

**Build setup:** shared Gradle setup lives in `build-logic/` convention plugins (`folio.android.library`, `folio.android.compose`, `folio.hilt`, `folio.jvm.library`).

## Consequences
- A dependency-rule check (Gradle `dependencyAnalysis` or a simple custom task) runs in CI.
- Navigation contracts (route classes) live in `:app` or a tiny `:core:navigation` if features need to reference each other's routes.
