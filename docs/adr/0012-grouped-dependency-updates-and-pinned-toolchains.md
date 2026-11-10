# 0012. Grouped dependency updates and pinned toolchains

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

A single maintainer at ten hours a week cannot review a pull request per
dependency per week, and a repository left alone for six months must come
back to a manageable queue. Verified on 2026-11-05: Dependabot supports
groups per ecosystem with pattern and update-type filters, weekly schedules
and automated merging through a workflow that checks the update type and
runs `gh pr merge --auto`, with required status checks recommended.

## Decision

Dependabot version updates, weekly, with one group per ecosystem (Gradle,
npm, GitHub Actions, Docker). Patch and minor updates merge automatically
when the required checks pass; major updates wait for a human and for the
migration notes. Tool versions are pinned once per ecosystem: the Gradle
toolchain and wrapper, `.node-version` and `engines`, lockfiles installed
with `npm ci`. GitHub Actions are referenced by commit SHA with the version
in a comment.

## Alternatives considered

- Renovate. More configurable and better at monorepos, at the price of a
  third-party app with repository access; not needed at this size.
- Manual updates on a calendar. Cheaper to set up and the first thing that
  stops happening.
- Automatic merging of majors. Saves time until a major breaks the build
  on a weekend.

## Consequences

- Positive: a small, predictable queue; reproducible builds on a clean
  machine; supply-chain exposure of CI reduced.
- Negative: grouped updates can hide which dependency broke a build;
  pinned Actions need their SHA refreshed, which Dependabot also does.
- Strongest argument against: automatic merging without a reviewer is
  still merging without a reviewer. The checks are the reviewer for patch
  and minor; anything else waits.

## Reopen when

Dependabot stops supporting an ecosystem in use, or the weekly queue
exceeds an hour of review.

## Links

ADR 0013. Dependabot options reference and automation guide
(docs.github.com, 2026-11-05).
