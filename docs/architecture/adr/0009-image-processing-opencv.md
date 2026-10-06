# ADR-0009: OpenCV for edge detection, perspective correction and enhancement

- **Status:** Accepted — pending spikes S1 (quality/speed) and S4 (size)
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
FR-03/04 need document quad detection, both live in the camera preview and on imported images. They also need perspective warp and four enhancement modes (adaptive threshold for B&W). All of this must work offline, on low-end devices, with a permissive licence.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **OpenCV (Android SDK, Apache 2.0)** | Battle-tested Canny/contours/warp/adaptive-threshold; fast native code; offline; permissive | About 10–20 MB per ABI in the full build. Mitigated with a slim build (core + imgproc) and AAB ABI splits |
| ML Kit Document Scanner (Play services) | Excellent quality, zero algorithm work | Play services dependency and model download; fixed UI; privacy story weaker; unavailable on de-Googled devices |
| Custom TFLite/LiteRT segmentation model | Robust on hard backgrounds | Must train or source a model; adds runtime and model size; more effort |
| Pure Kotlin / RenderScript-free CPU code | No native dependency | Slow; reinventing well-tested algorithms |
| Android `ColorMatrix` / `RenderEffect` for enhancement only | No dependency for simple modes | Can't do adaptive threshold or warp; still need OpenCV for detection |

## Decision
Use **OpenCV**, accessed only through the `EdgeDetector`, `PerspectiveCorrector` and `Enhancer` interfaces. Load it lazily with `System.loadLibrary` on the first processing call, not at startup.

## Consequences
- Spike S1 validates the detection quality targets. If they're missed, a LiteRT model can be added **behind the same `EdgeDetector` interface** without touching the UI.
- Spike S4 decides between a slim custom build and the Maven artifact with per-ABI splits.
- `Mat` lifetimes need discipline: always `release()` in `finally`. A small `use {}` helper is provided.
