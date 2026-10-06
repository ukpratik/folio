# ADR-0003: Jetpack Compose with Material 3

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
There are about 20 screens with sheets, chips, dialogs, a reorderable grid, a dark theme, 200% font scaling and TalkBack custom actions. The designs are specified as M3 components.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Jetpack Compose + Material 3** | Declarative, fits UDF state; M3 components match the designs (ModalBottomSheet, FilterChip, SegmentedButton); simple theming (light/dark tokens); first-class semantics APIs for accessibility; previews and screenshot tests | Must watch recomposition cost in the page grid; some APIs still evolving |
| Android Views + XML + Material Components | Mature, predictable performance | More boilerplate; harder state handling; M3 Views lag Compose; slower to build |
| Hybrid (Views for camera/crop, Compose elsewhere) | — | Two UI paradigms. Not needed: `PreviewView` is embedded via `AndroidView` |

## Decision
**Compose + Material 3** for all UI. CameraX `PreviewView` is hosted through `AndroidView`. The crop handles, quad overlay and loupe use Compose `Canvas` and `pointerInput`.

## Consequences
- Stable, immutable UI models (`@Immutable` data classes, `ImmutableList` from kotlinx.collections.immutable) keep the grid from recomposing needlessly.
- Baseline Profiles are mandatory to hit the cold-start target ([ADR-0017](0017-build-ci-release.md)).
- Theme tokens from the design canvas (light and dark) live in `:core:ui` as a `ColorScheme` plus `Typography`.
