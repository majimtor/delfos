# 0007. API-first OpenAPI contract as the single source of truth

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

Three clients consume the same API. Hand-written client code, duplicated
validation rules and documentation that does not come from the code are
the usual sources of drift. Verified on 2026-11-05: openapi-generator 7.27
supports OpenAPI 3.1 in beta; `openapi-typescript` supports 3.0 and 3.1;
`oasdiff` detects breaking changes between two specifications and runs as a
GitHub Action; springdoc 3.2 can generate a specification from code for
Spring Boot 4.

## Decision

`contract/openapi.yaml` is written by hand, in OpenAPI 3.0.3 until a
generator test shows 3.1 works end to end, and is the only definition of
the API. The server implements interfaces generated from it, with request
validation annotations generated into the DTOs; the web client consumes
types and a fetch client generated from it; the mobile client consumes
generated Kotlin models. The documentation page serves the file itself.
API tests validate real requests and responses against the file; CI runs
`oasdiff breaking` against `main` and against the last released contract.
Errors use Problem Details with a stable `code`; lists use cursor
pagination; security schemes are declared per operation.

## Alternatives considered

- Code-first with a generated specification. The server never drifts from
  its own output, and the specification is a build artefact rather than a
  design document; drift between what clients expect and what the server
  serves is only caught by a snapshot test. Not chosen: the contract is the
  design, and a client-independent definition survives a change of server
  language.
- A shared type package instead of a contract. Works for one language; the
  clients here are in two.
- GraphQL or gRPC. See ADR 0001.

## Consequences

- Positive: no second definition of the API anywhere; a breaking change
  cannot pass CI unnoticed; adding a client is generation, not
  transcription.
- Negative: adding an endpoint is two steps (contract, then code);
  generator quirks become build problems; generated code is excluded from
  the repository and must be reproducible on a clean machine.
- Strongest argument against: hand-written specifications rot when the
  team is one person in a hurry. The generated server interfaces make
  rotting impossible: the code does not compile against a stale contract.

## Reopen when

A generator cannot express a construct the API needs, an OpenAPI 3.1 feature
becomes necessary, or a second server appears that needs its own contract.

## Links

ADR 0001, ADR 0004, ADR 0005. openapi-generator (github.com/OpenAPITools,
2026-11-05); openapi-typescript (openapi-ts.dev, 2026-11-05); oasdiff
(github.com/oasdiff, 2026-11-05); RFC 9457 Problem Details.
