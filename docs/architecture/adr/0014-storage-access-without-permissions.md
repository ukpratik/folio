# ADR-0014: Photo Picker + SAF + FileProvider; no storage permissions

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Options considered
| Need | Chosen | Alternatives rejected |
|---|---|---|
| Import images | **Photo Picker** (`PickMultipleVisualMedia`; the AndroidX contract falls back to `ACTION_OPEN_DOCUMENT` on older devices) | `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`: broad access, permission prompt, Play policy scrutiny |
| Receive shared images | `ACTION_SEND`/`SEND_MULTIPLE` intent filter; copy immediately | — |
| Save PDF where the user chooses | **SAF `ACTION_CREATE_DOCUMENT`** | `MediaStore` Downloads insert: no user choice of folder (D-10) |
| Save JPG set (P1) | SAF `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission` | — |
| Share | **FileProvider** + `ACTION_SEND` | `file://` URIs (crash on API 24+) |

## Decision
Use the chosen column. The app requests **no storage permission on any API level**.

## Consequences
- Imported files are copied into app storage right away, because grants are temporary ([LLD §5.2](../02-low-level-design.md)).
- The saved folder's display name may be unavailable, so the snackbar says "Saved" (D-28 review).
- Works without Google Play services.
