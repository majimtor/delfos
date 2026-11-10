# 0001. Central API with multiple clients

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

Delfos has three clients in its vision: a web application for survey
managers and respondents, a mobile application, and whatever a reviewer
uses to call the API directly. All of them must apply the same rules:
survey lifecycle, versioning, conditional visibility of questions,
validation of answers, authorization by resource owner. A rule that lives in
more than one place drifts, and a client that reaches the database directly
needs database credentials on every device and bypasses every check the
server makes.

## Decision

One HTTP API is the only way to read or change data. Every client, including
the mobile one and any script, goes through it. Business rules, validation
and authorization are implemented once, on the server, and exposed through a
published contract (ADR 0007). Clients hold no database credentials and no
rule that the server does not also enforce; where a client evaluates
something for user experience, the server re-evaluates it on submission.

## Alternatives considered

- Clients with direct database access for administration or reporting. It
  removes one hop and lets a reporting tool query freely, at the price of a
  second copy of hashing, validation and authorization in another language,
  credentials on every machine that runs the client, and a schema that no
  single component owns. Rejected: the cost shows up as divergence within
  weeks.
- One backend-for-frontend per client. It tailors payloads to each screen,
  at the price of three servers for one maintainer and three places where a
  rule can be forgotten. Rejected for a one-person project; it can be added
  in front of the API later without changing the API.
- GraphQL instead of REST. It gives clients flexible queries, at the price
  of a weaker story for HTTP caching, for contract compatibility checks and
  for the OpenAPI tooling the clients depend on. Rejected: the client needs
  are known and small.

## Consequences

- Positive: rules and authorization have one implementation and one test
  suite; a new client costs a generated client, not a reimplementation.
- Negative: every operation pays a network round trip; a mobile client
  cannot work offline beyond what the server allows it to cache; the
  contract becomes a public commitment once a client is distributed.
- Strongest argument against: a thin client with server-side evaluation of
  conditional rules needs a request per answer, which an offline-first
  product could not accept. Delfos does not require offline response at
  launch; ADR 0009 keeps the door open.

## Reopen when

A client needs offline writes beyond rule evaluation, or a second backend
service appears and the "one API" assumption no longer describes the
system.

## Links

ADR 0007, ADR 0009, PLAN.md sections 2 and 4.
