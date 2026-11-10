# 0010. Single repository without monorepo tooling

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The project has a server, a shared Kotlin module, a web client, a mobile
client and a contract that all change together when the API changes. One
maintainer reviews everything. A reviewer should understand the project's
shape from a single page.

## Decision

One public repository, `delfos`, with top-level directories `api`,
`shared`, `web`, `mobile`, `contract`, `docs` and `deploy`. Gradle builds
`api` and `mobile` as separate builds that include `shared` as a composite
build; npm builds `web`; `contract` is validated, not built. CI uses path
filters so that a change to the web does not run the server tests. No
monorepo orchestrator.

## Alternatives considered

- One repository per component. Clean ownership boundaries for a team, at
  the price of publishing the contract as a versioned package and keeping
  several CI configurations in step, for one person. Rejected.
- Nx, Turborepo or Bazel. Task graphs and caching that matter when builds
  take tens of minutes or dozens of packages exist; here they would be an
  orchestrator for two build systems and a tell of complexity nobody asked
  for.
- One Gradle build for server and mobile. The Spring Boot plugin and the
  Android plugin pin different Gradle and Kotlin expectations; a composite
  build shares code without sharing configuration.

## Consequences

- Positive: a contract change and all its implementations are one pull
  request; one place to look; one CI.
- Negative: the repository mixes toolchains, so the README must say which
  command runs where; CI configuration carries path filters that need
  maintenance.
- Strongest argument against: a very large repository slows clones and
  CI. Not expected at this scale; the reopening condition names the limit.

## Reopen when

CI exceeds fifteen minutes with path filters in place, or a second
maintainer needs an ownership boundary.

## Links

ADR 0013, PLAN.md section 9.
