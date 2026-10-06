# ADR-0008: CameraX for capture and live document detection

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **CameraX** (Preview + ImageCapture + ImageAnalysis) | Lifecycle-aware; handles OEM quirks (important for Xiaomi/Vivo/Oppo in India); simultaneous preview and analysis; torch control | Less low-level control than Camera2 |
| Camera2 directly | Full control | Huge OEM-quirk surface; much more code |
| System camera intent (`ACTION_IMAGE_CAPTURE`) | No CAMERA permission | No live outline, no batch flow, inconsistent OEM UIs. Fails FR-02/03 |
| ML Kit Document Scanner | Ready-made UI and detection | Needs Google Play services and a model download; we can't customise its UI; conflicts with the no-network and privacy story |

## Decision
Use **CameraX**:
- `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` at about 640×480, feeding the **Y plane** to OpenCV.
- `ImageCapture` in `MAXIMIZE_QUALITY` mode, capped at about 12 MP.
- `PreviewView` embedded through `AndroidView`.

## Consequences
- **Permission flow:** CAMERA is requested just in time ([LLD §6.2](../02-low-level-design.md)).
- **Device QA:** the matrix must include the major Indian OEMs (QA plan §2).
