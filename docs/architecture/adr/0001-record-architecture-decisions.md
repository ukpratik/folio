# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Engineering lead, Founder/PM

## Context
Folio is built by a small team and will change hands. Decisions about the stack, privacy and processing have long-lived consequences, and their reasoning must stay visible.

## Decision
- Use **Architecture Decision Records** in the MADR style, one file per decision, in `Folio_Tech_Architecture/adr/`.
- Files are numbered sequentially and never renumbered.
- An accepted ADR is immutable. To change a decision, write a new ADR that **supersedes** it, and link both ways.
- ADRs reference PRD IDs (FR-xx/NFR-xx) and decision-log IDs (D-xx) where relevant.

## Template
```markdown
# ADR-NNNN: <title>
- Status: Proposed | Accepted | Superseded by ADR-XXXX | Deprecated
- Date: YYYY-MM-DD
- Deciders: …

## Context
## Decision drivers
## Options considered
### Option A — … (pros / cons)
## Decision
## Consequences (positive / negative / follow-ups)
## Links
```

## Consequences
- Reviewers can challenge a decision by reading one page.
- Writing an ADR costs a little time. That cost applies only to significant decisions (stack, architecture, privacy, data formats), not routine code.
