# ADR-0015: Navigation Compose with type-safe routes

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Navigation Compose (type-safe, kotlinx.serialization routes)** | Official; Hilt ViewModel scoping; deep links (share-in); back stack + predictive back | Bottom-sheet destinations need care |
| Navigation 3 | Newer, back-stack-as-state | Evaluate at project start. Adopt only if stable at that time |
| Voyager / Decompose | Nice APIs | Third-party; Decompose is KMP-oriented (not needed) |

## Decision
Use **Navigation Compose with `@Serializable` route classes** ([LLD §7](../02-low-level-design.md)).
- The export sheet is a `ModalBottomSheet` hosted by the editor screen, not a nav destination. This keeps the API surface small.
- **Predictive back** is enabled. Processing overrides back to mean cancel.

## Consequences
- Routes carry IDs only. Screens load their state from Room, which makes them robust to process death.
