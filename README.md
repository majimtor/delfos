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

## Run it

One command starts PostgreSQL 18 and the API with the demo data, on port 8080:

```sh
docker compose -f deploy/compose.yaml up --build
```

Then `http://localhost:8080/api/v1/public/surveys/demo-movilidad` answers, and
the manager operations accept the accounts below. The database listens on
`localhost:5433`.

## Demo data

With `DELFOS_SEED_ENABLED=true` the API applies the demo data at startup and
resets it on every start: three accounts and the sample surveys in Spanish.
The accounts are fictitious and their password is public on purpose.

| Account | Password | Role |
| --- | --- | --- |
| `demo-manager` | `delfos-demo` | Owns the sample surveys |
| `demo-manager-2` | `delfos-demo` | Owns one survey, to show that managers only see their own |
| `demo-admin` | `delfos-demo` | Manager with the administrator flag |

Sample surveys of `demo-manager`: an unpublished draft, a published survey
with the five question types and every rule operator (40 responses), a
survey with a superseded first version and a published second one (25
responses across both), and a closed survey (20 responses). The published
ones can be answered at `/api/v1/public/surveys/demo-movilidad` and
`/api/v1/public/surveys/demo-servicio`.

## Working on the repository

After cloning, point Git at the versioned hooks and install
[gitleaks](https://github.com/gitleaks/gitleaks/releases) so that a commit
containing a secret is blocked before it exists:

```sh
git config core.hooksPath .githooks
```

Markdown is checked with `markdownlint-cli2`; the configuration lives in
`.markdownlint-cli2.yaml`.

The Kotlin builds use the Gradle wrapper at the repository root and provision
the JDK they declare, so no Java installation is needed beyond one that can
start Gradle (Java 17 or newer):

```sh
./gradlew -p shared check
./gradlew -p api check
```

The API tests start PostgreSQL 18 in a container, so Docker must be running.

## Licence

MIT. See [LICENSE](LICENSE).
