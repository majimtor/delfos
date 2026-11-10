# 0013. GitHub Flow with squash merges and automated releases

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

One maintainer, a public repository from the first commit, a history that
a reviewer will read as evidence of how the work was done. Branch
protection rules, secret scanning and push protection are available for
public repositories on the free plan (verified 2026-11-05).

## Decision

GitHub Flow: short branches, pull requests to `main`, `main` always
deployable. Pull requests merge by squash with a Conventional Commits
title; `main` is a linear sequence of reviewed changes and the pull
request keeps the intermediate steps. Branch protection on `main`: pull
request required, status checks required, linear history, conversation
resolution, force pushes and deletions blocked. Commits are signed with the
maintainer's SSH key. `release-please` turns the commit history into a
changelog and tags; version 1.0.0 marks the public launch. Exception: the
initial documentation commits before the first CI workflow go directly to
`main`, as the engineering rules state.

## Alternatives considered

- Direct commits to `main` with no pull requests. Honest for a solo
  developer and it leaves no record of why each change was made; the pull
  request description is that record.
- Rebase merges keeping every commit. Preserves detail and requires every
  intermediate commit to be presentable; squash with small pull requests
  gives the same readable history with less ceremony.
- Git flow with develop and release branches. Designed for parallel
  release lines that this project does not have.

## Consequences

- Positive: a readable history; every change has a description that
  answers why; releases without manual bookkeeping.
- Negative: squash loses intra-branch history from `main`; required checks
  mean a red CI blocks documentation fixes too, which is intended.
- Strongest argument against: pull requests reviewed by their own author
  are ceremony. The description template is what makes them useful: it
  forces the questions a reviewer would ask.

## Reopen when

A second contributor joins, or the release cadence needs parallel
maintenance lines.

## Links

ADR 0010, ADR 0012, engineering rules. GitHub branch protection and
secret scanning documentation (docs.github.com, 2026-11-05).
