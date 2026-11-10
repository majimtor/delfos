# Delfos

[![CI](https://github.com/majimtor/delfos/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/majimtor/delfos/actions/workflows/ci.yml)
[![Licence: MIT](https://img.shields.io/badge/licence-MIT-blue.svg)](LICENSE)

Delfos is a survey platform: managers build surveys with conditional logic,
publish and version them, watch results arrive and export them; respondents
answer from the web or from a mobile application.

## Status

In development. The plan and the architecture decisions are written; no code
is deployed yet. This section is replaced by the demo link and the demo
accounts when the first slice ships.

## Where to look

- [PLAN.md](PLAN.md): what gets built, in which order, with which stack and
  why, how long each step takes, and how the result stays alive.
- [docs/adr/](docs/adr/README.md): the decisions that are expensive to
  reverse, each with its alternatives and the condition that reopens it.
- [docs/engineering-rules.md](docs/engineering-rules.md): the rules every
  change in this repository follows.

## Planned layout

| Directory | Content | Language |
| --- | --- | --- |
| `api/` | HTTP API on Spring Boot | Kotlin |
| `shared/` | Rules evaluator and contract models shared by server and mobile | Kotlin Multiplatform |
| `web/` | Single-page application | TypeScript, React |
| `mobile/` | Android-first application | Kotlin, Compose Multiplatform |
| `contract/` | OpenAPI contract and conformance cases | YAML, JSON |

## Working on the repository

After cloning, point Git at the versioned hooks and install
[gitleaks](https://github.com/gitleaks/gitleaks/releases) so that a commit
containing a secret is blocked before it exists:

```sh
git config core.hooksPath .githooks
```

Markdown is checked with `markdownlint-cli2`; the configuration lives in
`.markdownlint-cli2.yaml`.

## Licence

MIT. See [LICENSE](LICENSE).
