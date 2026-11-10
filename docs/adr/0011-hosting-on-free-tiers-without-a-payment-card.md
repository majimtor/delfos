# 0011. Hosting on free tiers without a payment card

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The infrastructure budget is zero euros per month and no provider that asks
for a payment card is acceptable, not even for identity verification. The
demo must survive six months without attention. Verified on 2026-11-05 and
2026-11-10 from provider documentation and by signing up: Render's free web
service (512 MB, 0.1 CPU) needs no card, sleeps after 15 minutes without
traffic and grants 750 instance hours per month; Neon's free PostgreSQL is
permanent and needs no card; Cloudflare Pages serves a single-page
application with no stated bandwidth limit; GitHub Actions, Pages and the
container registry are free for public repositories. Northflank, Koyeb,
Oracle, Google Cloud and Azure require a card; Fly.io has no free tier;
Railway and AWS offer expiring credits. Measured on 2026-11-10 with a
throwaway Spring Boot 4.1 service on Render: wake-up after sleep 93 s with
default JVM settings, 53 s with conservative flags, 47 s with flags plus a
JVM AOT cache built into the image; resident memory 133 to 169 MB; database
round trip to Neon in Frankfurt 3 to 21 ms.

## Decision

API on a Render free web service in Frankfurt, deployed from the
repository's Dockerfile when CI passes; PostgreSQL on Neon in Frankfurt; web
on Cloudflare Pages; container images on GitHub's registry; CI on GitHub
Actions. Web and API live under the maintainer's own domain
(`delfos.manuelalejandro.com`, `api.delfos.manuelalejandro.com`) so that
session cookies work and the URL outlives any provider. Three measures
against sleep: JVM flags and an AOT cache in the image; a scheduled workflow
that requests the liveness endpoint every ten minutes while the repository
is active; a wait screen in the web client while the API is unreachable.
The whole system also runs locally with one `docker compose up`, with the
same image, the same database engine and the same seed.

## Alternatives considered

- A small virtual private server with Docker Compose. Always on and fully
  controlled, for roughly five euros a month plus operating-system
  maintenance; it demonstrates server administration, which is not this
  project's axis, and it violates the budget. Kept as a configuration
  change, since the deployable is a container.
- Providers requiring a card as verification only (Northflank always-on at
  512 MB, Koyeb, Oracle's larger free VM). Better compute, excluded by the
  rule above.
- A native image to cut the wake-up below 30 s. Possible later; it adds
  build time and reflection configuration. Kept as an experiment with a
  trigger.

## Consequences

- Positive: zero cost, no card anywhere, one-command portability, measured
  numbers rather than assumptions.
- Negative: a cold request may wait about 45 s when the keep-alive is not
  running; free plans change without notice; Render may restart the
  instance at any time and states that free services are not for
  production.
- Strongest argument against: a demo that may greet a reviewer with a
  45-second wait undermines the point of a demo. The keep-alive covers
  active months; the wait screen tells the truth for the rest; the
  provider can be swapped in an afternoon.

## Reopen when

Render or Neon change their free plans, the keep-alive workflow is
disabled and the measured wake-up exceeds what the wait screen can
excuse, or the budget decision changes.

## Links

ADR 0003, ADR 0008, PLAN.md sections 11 and 12. Render free instances,
deploy hooks and image deployment documentation (render.com, 2026-11-05 and
2026-11-10); Neon plans (neon.com, 2026-11-05); Cloudflare Pages limits
(developers.cloudflare.com, 2026-11-05); GitHub Actions and Packages
billing (docs.github.com, 2026-11-05).
