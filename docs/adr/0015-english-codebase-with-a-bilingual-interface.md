# 0015. English codebase with a bilingual interface

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The maintainer's target market is Spain and remote roles in the European
Union and the United States; reviewers may read neither Spanish nor the
same English. The product's first users would be Spanish speakers. Two
languages double every piece of user-facing text and create a new failure
mode: a stale translation, which reads worse than a single language.

## Decision

Everything versioned is in English: code, identifiers, comments, commit
messages, pull requests, documentation, configuration, seed identifiers.
The user interface is bilingual with Spanish as the primary language and
English as the secondary one, through per-client translation catalogues
(FormatJS on the web, Compose resources on mobile). The API returns stable
error codes and parameters, never translated messages; clients translate.
Sample surveys in the seed are content, so they exist twice, as a Spanish
and an English survey, with identifiers in English. CI extracts messages
and fails when a catalogue has untranslated keys; end-to-end tests run in
Spanish.

## Alternatives considered

- Spanish codebase. Natural for the local market; it would exclude every
  reviewer who does not read Spanish and break the convention of every
  library in use.
- A single-language interface. Cheaper, and it would hide the product from
  one of the two audiences; the CI check is what keeps the second language
  from rotting.
- Translating API messages on the server with `Accept-Language`. A second
  catalogue on the server and a negotiation every client must get right,
  for messages that are never shown verbatim anyway.

## Consequences

- Positive: a codebase any reviewer can read; a product any reviewer can
  try in their language; translation drift fails the build instead of
  shipping.
- Negative: every string is two artefacts; the seed carries two sets of
  surveys; estimates for user-facing work include the second language.
- Strongest argument against: a stale translation is worse than none, and
  CI cannot judge quality, only presence. Accepted; quality is reviewed when
  the catalogue changes.

## Reopen when

A third language is requested, or the untranslated-keys check fails on
two consecutive releases, which would mean the process, not the rule, needs
fixing.

## Links

ADR 0004, ADR 0005, ADR 0007, engineering rules.
