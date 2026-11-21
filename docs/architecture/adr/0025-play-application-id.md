# ADR-0025: Separate application ID for Google Play

- **Status:** Accepted (amends ADR-0022 for the Play flavour)
- **Date:** 2026-11-21
- **Deciders:** Founder

## Context
Folio has been on F-Droid as `io.github.ukpratik.folio` since 2026-10-10, signed with F-Droid's key. Android developer verification only accepts that key as "eligible" for the package name, so publishing the same name on Google Play needs a manual ownership request with our upload key. That request was sent on 2026-11-18 and is still pending.

## Options considered
| Option | Pros | Cons |
|---|---|---|
| Wait for the ownership request | One package name everywhere | Unknown wait; blocks Play |
| **Play flavour gets `io.github.ukpratik.folio.play`** | Play can ship now; F-Droid untouched | Two separate apps: Play and F-Droid installs don't update each other (they already couldn't, different keys) |

## Decision
The `play` flavour sets `applicationIdSuffix = ".play"`, so the Play app is `io.github.ukpratik.folio.play`, and its "Rate" link points there. The `fdroid` flavour keeps `io.github.ukpratik.folio`. The Play upload key stays `folio-upload`; Play App Signing holds the app signing key.

## Consequences
- F-Droid's recipe (`gradle: [fdroid]`) is unchanged.
- The pending ownership request for `io.github.ukpratik.folio` no longer blocks anything; it can be withdrawn or left to expire.
- The `.play` application ID is permanent once uploaded.
