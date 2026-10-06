# ADR-0002: Native Android app in Kotlin

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
Folio v1 is Android-only (India-first). Its core is heavy on-device image processing, CameraX analysis, native-memory bitmaps, PDF writing and deep platform integration (Photo Picker, SAF, share-in, FileProvider, predictive back). Performance on 3–4 GB phones and app size (≤ 25 MB) are hard requirements. iOS is not planned for v1, but may come later.

## Decision drivers
1. Performance and memory control on low-end devices (NFR-04/05)
2. Direct access to CameraX, OpenCV and platform storage APIs
3. App size
4. Team productivity and hiring pool
5. Optional future iOS reuse

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Kotlin, native Android** | First-class Jetpack/CameraX/Compose support; best performance and memory control; smallest binary; largest Android talent pool; coroutines suit the pipeline | No iOS reuse by default |
| Java, native Android | Same platform access | More verbose; weaker null-safety; Jetpack is Kotlin-first; no coroutines/Compose |
| Flutter (Dart) | One codebase for iOS + Android; fast UI iteration | Camera, image processing and PDF need platform channels or FFI to native code anyway; adds about 5–8 MB of engine; Skia/Impeller rendering differs from native M3; native-memory control is harder; the plugin ecosystem for document scanning often pulls in ML Kit (Play services) |
| React Native | JS talent pool; OTA updates | OTA would need network (we ship with none); heavy native-module work for the camera and OpenCV pipeline; larger runtime; bridging large bitmaps is costly |
| Kotlin Multiplatform + Compose Multiplatform | Share domain and UI with iOS later | iOS isn't in scope; CameraX and OpenCV are platform-specific anyway; adds build complexity now |

## Decision
**Native Android in Kotlin.** Keep `:core:model` and `:core:domain` as **pure Kotlin** (no `android.*` imports), so they can later move to **Kotlin Multiplatform** without a rewrite if iOS becomes a goal.

## Consequences
- **Positive:** best fit for performance, size and platform integration; simplest toolchain.
- **Negative:** iOS would need its own UI and platform layer later (the domain logic is reusable).
- **Follow-up:** enforce "no Android imports" in `:core:model` and `:core:domain` with a module dependency check (they are plain `kotlin("jvm")` modules).
