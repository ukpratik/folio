# ADR-0007: Room + DataStore + app-private file store

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
The app must store documents, ordered pages with edit parameters, export metadata, preferences and binary images and PDFs. It needs transactional reorders, reactive UI, migrations and crash safety.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| **Room (SQLite)** | Official; Flow support; compile-time-checked SQL; migrations tooling; transactions | Annotation processing |
| SQLDelight | SQL-first, KMP-ready | Less Android-integrated tooling; fine, but not needed now |
| Realm / ObjectBox | Object store, fast | Larger binary; Realm Kotlin is deprecated by MongoDB; licence and size concerns |
| Images as BLOBs in SQLite | One store | Bloats the DB; poor streaming; cursor window limits |
| **DataStore (Preferences)** for settings | Async, atomic, Flow | — |
| SharedPreferences | Familiar | Synchronous I/O pitfalls |

## Decision
- **Room** for metadata.
- **DataStore** for preferences.
- **Files** in `filesDir/documents/{docId}/` for images and PDFs. All writes are atomic (temp file, fsync, rename).
- Soft delete (`deleted_at`) supports Undo.
- Sources are reference-counted (duplicates share one source file).

## Consequences
- The schema is exported to the repo and migrations are tested from v1.
- **No encryption at rest beyond Android's file-based encryption.** Data is in the app sandbox, and FBE protects it at rest on API 29+ devices with a lock screen. SQLCipher is deferred to the app-lock feature (roadmap 1.4).
