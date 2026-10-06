# ADR-0022: Application ID and code namespace

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Founder, Engineering lead

## Context
Android needs a globally unique **application ID**. After the first upload it is **permanent** on Google Play and identifies the app on F-Droid. By convention it is a reverse domain, but **neither store checks domain ownership**. Using a domain owned by someone else (e.g. `com.folio.*`) risks confusion, ID collisions and trademark complaints.

## Options considered
| Option | Cost | Pros | Cons |
|---|---|---|---|
| **`io.github.ukpratik.folio`** | Free | You already own the namespace (your GitHub account); common F-Droid convention; no purchase needed | Tied to the GitHub username spelling |
| `com.<ownbrand>.folio` after buying a domain | ~₹800–1,500/year | Looks polished; matches a future website | Must keep renewing; buy before the first upload |
| `com.folio.app` / `com.folio.docmaker` | Free | Short | Domain not owned; likely already taken; impersonation risk. **Rejected** |

## Decision
- **applicationId:** `io.github.ukpratik.folio` (GitHub user `ukpratik`). Debug builds use `io.github.ukpratik.folio.debug`.
- **Code namespace / package:** the same root, e.g. `io.github.ukpratik.folio.core.model`. Unlike the application ID, the namespace can be refactored later without affecting users.
- **Debug builds:** add `applicationIdSuffix = ".debug"` so debug and release can be installed side by side.

## Consequences
- No domain purchase is needed now. If a domain is bought later, the application ID stays the same (a change would mean a new Play listing). Only the website and support email use the new domain.
- The LLD now uses the final namespace `io.github.ukpratik.folio.*`.
- Renaming the GitHub account would not break the app (the ID stays valid), but the ID would no longer match the account, so keep the username stable.
