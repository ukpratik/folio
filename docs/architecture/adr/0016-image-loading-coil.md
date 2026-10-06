# ADR-0016: Coil 3 for thumbnail loading and caching

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
The page grid (up to 100 pages), Recents and the result screen need rendered thumbnails that reflect edits. They need memory and disk caching and cancellation on scroll.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Coil 3** (`coil-compose`, **no network artifacts**) | Kotlin/Compose-first; coroutines; custom `Fetcher`/`Keyer` for our `Page` model; memory + disk cache | Must make sure no network module is pulled in |
| Glide | Mature | Java-centric; Compose integration is secondary |
| Hand-rolled LruCache + `produceState` | No dependency | Reimplements cancellation, disk cache and sizing |

## Decision
Use **Coil 3** with:
- a `PageFetcher` that calls `PageRenderer` at thumbnail size
- a `PageKeyer` that returns `"$sourceId:$editVersion:$size"`
- a disk cache in `cacheDir/thumbs` (~50 MB)

## Consequences
- Editing a page bumps `editVersion`, which invalidates exactly one thumbnail.
- The CI permission gate ([ADR-0013](0013-no-network-enforcement.md)) catches any accidental network artifact.
