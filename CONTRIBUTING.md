# Contributing to Folio

Thanks for helping! A few rules keep Folio private, small and maintainable.

## Ground rules

1. **No network.** Don't add anything that needs the INTERNET permission or talks to a server. CI will fail the build.
2. **Licences.** New dependencies must be FOSS and GPL-3.0-compatible (Apache-2.0, MIT, BSD, SIL OFL, LGPL, GPL-3.0). No AGPL, GPL-2.0-only or proprietary SDKs (ADR-0023).
3. **Architecture.** Follow the layering in `docs/architecture/`. Significant decisions need an ADR (`docs/architecture/adr/`, template in ADR-0001).
4. **Tests.** New behaviour needs tests. Prefer fakes over mocks (ADR-0018).
5. **Source headers.** Start each new source file with `// SPDX-License-Identifier: GPL-3.0-or-later`.

## Sign your commits (DCO)

We use the [Developer Certificate of Origin](https://developercertificate.org/) instead of a CLA. Add a sign-off to each commit:

```bash
git commit -s -m "Your message"
```

This adds `Signed-off-by: Your Name <you@example.com>` and certifies you have the right to submit the change under GPL-3.0-or-later.
