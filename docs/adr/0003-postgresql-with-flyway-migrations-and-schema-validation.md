# 0003. PostgreSQL with Flyway migrations and schema validation at startup

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The domain needs relational integrity between surveys, versions, questions,
options, responses and answers, enumerated types, array columns for
selected options and a JSON column for rule definitions. The database must
run on a free tier without a payment card and survive six months without
attention. Verified on 2026-11-05: Neon's free plan is permanent, needs no
card, offers 1 GB per project in Frankfurt, suspends compute after five
minutes of inactivity and resumes in hundreds of milliseconds, and does not
pause or delete projects for inactivity. The free MySQL offers found either
expire or power off until someone turns them back on. PostgreSQL 18 is
supported until November 2030.

## Decision

PostgreSQL 18 on Neon. The schema is defined only by versioned SQL
migrations run by Flyway (community edition). Hibernate runs with
`ddl-auto=validate`, so the application refuses to start when its entities
and the migrated schema disagree. Aggregations for results are written in
SQL through `JdbcTemplate`, not through the object model. Domain
enumerations are code; the database stores their names. Cascading deletes
exist only within an aggregate (survey, version, question, option;
response, answer).

## Alternatives considered

- MySQL. Familiar to many reviewers, but no durable free hosting without a
  card was found, and PostgreSQL's `jsonb`, enumerated types and arrays fit
  the rule and answer models better.
- Liquibase. Equivalent capability with XML or YAML changelogs; more
  verbose for a single maintainer who reads SQL.
- Spring Data JDBC instead of JPA. Simpler aggregate mapping and no lazy
  loading surprises, at the price of losing schema validation at startup
  and of being less familiar to Spring reviewers. Not chosen; the
  validation at startup is a requirement of this project.
- Letting Hibernate manage the schema (`ddl-auto=update`). Convenient in
  development and a well-known source of silent drift and data loss; the
  precise failure this decision prevents.

## Consequences

- Positive: one definition of the schema; a wrong entity fails loudly at
  startup; migrations are reviewable SQL in the repository; integration
  tests run against the real engine in containers.
- Negative: Kotlin entities need compiler plugins (ADR 0002); every schema
  change is two artefacts (migration and entity) kept in step; Flyway's
  free edition offers no undo, so a bad migration is fixed forward.
- Strongest argument against: the free tier's 1 GB and compute hours cap
  are small. The demo data is synthetic and reset on a schedule, so they
  are not expected to be reached; the metric is watched quarterly.

## Reopen when

The provider changes its free plan, storage approaches 1 GB, or the
project needs features Neon does not offer (for example multi-region).

## Links

ADR 0002, ADR 0011, PLAN.md section 5. Neon plans and scale-to-zero
documentation (neon.com, 2026-11-05); PostgreSQL versioning policy
(postgresql.org, 2026-11-05); Flyway documentation (red-gate.com,
2026-11-05).
