# ADR-0020: minSdk 26, targetSdk per Google Play, phones first

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
In India, the installed base is heavily skewed to entry and mid devices. Some still run Android 8–9.

## Options considered
| minSdk | Reach | Trade-off |
|---|---|---|
| 24 (7.0) | Slightly more | Bitmaps on the Java heap (OOM risk); more legacy code paths |
| **26 (8.0)** | Covers the vast majority of active devices | Bitmaps on the native heap; adaptive icons; `ImageDecoder` still needs 28 (HEIC only) |
| 28 (9.0) | Native HEIC, ImageDecoder | Drops a meaningful slice of low-end Indian devices |

## Decision
- **minSdk 26**
- **targetSdk** = the level Google Play requires at release time (edge-to-edge enforced from 35, predictive back)
- **ABIs:** arm64-v8a and armeabi-v7a (AAB splits); x86_64 for emulators/debug only
- **Form factors:** phones, portrait-optimised. Tablets and foldables must work (resizeable, no crashes) but get no dedicated layout in v1.

## Consequences
- HEIC import is available only on API 28+, with a friendly error below that (FR-01).
- The QA matrix spans API 26–latest across Samsung, Xiaomi, Vivo/Oppo/Realme, Motorola and Pixel (QA plan §2).
