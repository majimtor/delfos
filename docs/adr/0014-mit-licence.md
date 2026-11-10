# 0014. MIT licence

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The repository is public from its first commit, so the licence must be
decided before it. Verified on 2026-11-05: GitHub's terms grant other users
only the right to view and fork a public repository; any further right
comes from a licence the owner adopts. The project is a portfolio piece
with no revenue; a reviewer expects to be able to read, run and reuse it.

## Decision

MIT, in a `LICENSE` file at the repository root with the maintainer's name
and year, identified as `MIT` in the README. It covers code, contract,
documentation and seed data. No per-file headers.

## Alternatives considered

- Apache-2.0. Adds an explicit patent grant and a NOTICE mechanism, common
  in the JVM ecosystem; heavier than needed for one person's project with
  no patents.
- All rights reserved (no licence). The only reversible choice, and it
  reads as an oversight on a portfolio and gives reviewers no right to run
  the code.
- A copyleft licence (GPL, AGPL). Appropriate for a product whose core
  should stay open when offered as a service; this is not a product.

## Consequences

- Positive: maximal reuse, minimal text, the expectation of a portfolio
  met.
- Negative: anyone may use the code commercially without contributing
  back; the maintainer's name is in the licence file, which is already
  public elsewhere.
- Strongest argument against: relicensing to something stricter later
  only applies to future versions. Accepted; nothing here needs protecting.

## Reopen when

The project becomes a product whose service form should remain open, or
external contributions require a contributor agreement.

## Links

GitHub Terms of Service, section D (docs.github.com, 2026-11-05).
