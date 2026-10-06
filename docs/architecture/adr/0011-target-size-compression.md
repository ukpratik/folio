# ADR-0011: Two-pass, per-page-budget target-size algorithm

- **Status:** Accepted — pending spike S2
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
FR-18 requires best-effort output under a user-chosen size (as small as 50 KB), with a readability floor, within the export-time budget (NFR-04), and without holding all pages in memory.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| Global binary search on one quality value, re-encoding all pages per step | Uniform quality | Each step re-renders or holds every page: O(pages × steps) renders or O(pages) memory. Too slow or too heavy for 100 pages |
| Fixed quality table by target | Fast | Inaccurate; misses or over-shrinks |
| **Two-pass per-page budget** (pass 1: low-res complexity sample per page; split the budget proportionally; pass 2: per page, binary-search quality, then step DPI down; carry the surplus forward) | Bounded memory (1 page at a time); about 1–2 full renders per page; adapts to page complexity (text vs photo); predictable | Quality can vary slightly between pages (acceptable) |
| Downscale only | Simple | Poor text legibility at small sizes |

## Decision
Use the **two-pass per-page-budget algorithm** in [LLD §5.7](../02-low-level-design.md):

| Setting | Values |
|---|---|
| Presets | High 300 dpi / q88; Balanced 200 / q75; Small 150 / q60 |
| Quality range | qMin 35 |
| DPI ladder | 300 → 200 → 150 → 120 → 100 (floor) |
| Binary search | ≤ 6 steps per page per DPI |

**The target overrides the preset (D-30).** If the target is missed at the floor, return the smallest file with `targetMet=false`, and the UI offers B&W, Remove pages or Keep.

## Consequences
- Spike S2 measures accuracy (≥ 90% of targets met on the QA set) and time.
- B&W pages are encoded as 8-bit grayscale JPEG. 1-bit CCITT G4 could make text pages 3–5× smaller and is noted as a future optimisation.
