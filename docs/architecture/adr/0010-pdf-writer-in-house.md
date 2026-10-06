# ADR-0010: In-house streaming PDF writer that embeds JPEG pages

- **Status:** Accepted — pending spike S2
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
Folio's PDFs are simple: one full-page image per page, plus a tiny Info dictionary. Size accuracy is critical (FR-18). The final size must be predictable from the encoded JPEG sizes, and memory must stay bounded for 100 pages.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| Android `PdfDocument` (platform) | No dependency | Draws bitmaps via Canvas; the platform controls image compression, so we can't hit target sizes. Holds pages in memory |
| PdfBox-Android (Apache 2.0) | Full-featured; can embed JPEG | Large (fonts, AWT ports); more memory; we use under 5% of it |
| iText 7 | Full-featured | **AGPL**, so it's excluded |
| **In-house streaming writer (~300–400 LOC)** | JPEG bytes pass straight through (`/DCTDecode`, no re-encode), so output size ≈ Σ JPEG + small overhead. Streams to disk with O(1) memory. Full control of metadata (FR-32). Tiny | We own correctness, so it needs strong tests |

## Decision
Build a **minimal PDF 1.4 writer** ([LLD §5.8](../02-low-level-design.md)): catalog, pages tree, one image XObject and one content stream per page, an Info dictionary, and a classic xref table.

## Consequences
- **Correctness safeguards:**
  - golden-file tests parsed by PDFBox (test-only dependency)
  - `qpdf --check` in CI
  - manual open-tests in every NFR-09 viewer
- **Extensibility:** a hidden OCR text layer (roadmap 1.2) can be added as an extra content stream per page.
- **Fallback:** if spike S2 finds viewer incompatibilities, swap in PdfBox-Android behind the `PdfWriter` interface.
