# ADR-0026: 16 KB memory page support for Google Play

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** Founder, Engineering lead

## Context
Google Play rejects new app releases whose 64-bit native libraries are not aligned for 16 KB memory pages. Folio 1.0.2 shipped OpenCV 4.9.0, whose `libopencv_java4.so` and bundled `libc++_shared.so` are 4 KB-aligned; Play Console blocked the production release ("Your app does not support 16 KB memory page sizes").

## Options considered
| Option | Result |
|---|---|
| OpenCV 4.12, 4.13 or 4.14 (fully 16 KB) | **Rejected.** 4.12 and 4.14 crash with SIGILL in OpenCV's static initialisers on an arm64 emulator (Apple Silicon host): the prebuilt AAR uses CPU instructions not every ARMv8 CPU has, so older phones could crash at start-up. |
| OpenCV 4.11.0 | `libopencv_java4.so` is 16 KB-aligned and all 15 instrumented OpenCV tests pass, but its bundled `libc++_shared.so` is still 4 KB. |
| Build OpenCV from source | Most control, but slow builds and a large CI change. Later, if needed. |

## Decision
- OpenCV **4.11.0**.
- The **Play** flavour ships the installed NDK's `libc++_shared.so` (NDK r28, 16 KB-aligned) instead of OpenCV's: a `copyPlayLibcxx` task copies it into `build/generated/playLibcxx` at build time, added as a `play` jniLibs source with `pickFirsts`. The NDK's rule is to ship the newest libc++ any component was built with (OpenCV 4.11 was built with NDK r26), so the newer runtime is the supported choice.
- The **fdroid** flavour keeps OpenCV's own copy (no NDK needed on F-Droid's build server, no binaries in the repo; 16 KB is a Play requirement only).

## Consequences
- Building the Play flavour needs the NDK installed (Android Studio SDK Manager → NDK).
- Verified: all 64-bit libraries in the Play APK are 16 KB-aligned (`llvm-objdump -p`, `zipalign -c -P 16`); 15/15 OpenCV instrumented tests pass with the swapped libc++.
- Revisit when an OpenCV release is both 16 KB-aligned and runs on baseline ARMv8.
