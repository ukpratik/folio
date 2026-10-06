# ADR-0023: Source code licensed GPL-3.0-or-later

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Founder (D-41)

## Context
F-Droid distribution (ADR-0021) requires Folio's source code to be public under a free/open-source licence. The founder wants to stop others from repackaging Folio as a closed-source app.

## Options considered
| Option | Effect | Fit |
|---|---|---|
| **GPL-3.0-or-later** | Anyone may use, modify and redistribute, but distributed modified versions must also be GPL-3.0 with source | **Chosen:** blocks closed-source forks; widely used on F-Droid |
| Apache-2.0 | Anyone may reuse, including in closed-source apps; includes a patent grant | Rejected: allows closed repackaging |
| AGPL-3.0 | GPL plus network-use clause | Unnecessary: Folio has no server or network |
| MIT | Very permissive | Rejected for the same reason as Apache |

## Decision
- **Licence:** Folio's source code is **GPL-3.0-or-later**.
- **Repository:** public GitHub repository, proposed `github.com/ukpratik/folio`, containing:
  - a `LICENSE` file with the full GPL-3.0 text (add it with GitHub's licence picker when creating the repo)
  - an SPDX header (`SPDX-License-Identifier: GPL-3.0-or-later`) in each source file
  - a `NOTICE` / AboutLibraries screen listing third-party licences
- **Dependency rule:** shipped dependencies must be **GPL-3.0-compatible**. Allowed: Apache-2.0, MIT, BSD, SIL OFL (fonts), LGPL, GPL-3.0. Banned: GPL-2.0-only, AGPL, proprietary or binary-only SDKs. The CI licence gate (ADR-0017) enforces this allowlist. Test-only tools (e.g. JUnit/EPL) are not distributed and are exempt.
- **Distribution:** Google Play allows GPL apps. The Play build is the same source, signed with our upload key.

## Consequences
- **Positive:** a strong trust signal ("audited, open source, no network"); F-Droid eligible; forks must stay open.
- **Negative:** companies that might want to embed Folio code in closed products can't. That is acceptable, since it's the founder's intent.
- **Brand protection:** the GPL doesn't protect the *name* "Folio: Doc Maker" or the app icon. Add a short trademark/branding note in the README asking forks to rename (common practice). Formal trademark clearance stays open item D-17.
- **Contributions:** accept them under the same licence (inbound = outbound). Add a CONTRIBUTING.md with a DCO sign-off (`Signed-off-by`) rather than a CLA, for simplicity.
