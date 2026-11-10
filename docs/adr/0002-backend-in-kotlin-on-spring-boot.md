# 0002. Backend in Kotlin on Spring Boot

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The backend is the axis of this project: it carries the domain, the rules
engine, authentication and the contract. The maintainer works daily with
Spring Boot and Java and with TypeScript on the web, and is learning Kotlin.
The mobile client (ADR 0005) is a Kotlin Multiplatform application that
should share the rules evaluator and the contract models with the server.
Verified on 2026-11-05 and 2026-11-10: Spring Boot 4.1.2 is current,
requires Java 17 to 27 and Spring Framework 7.0, which supports Kotlin 2.2
and later; Kotlin 2.4.21 is current; Java 25 is the current long-term
support release, with Temurin builds supported until at least September
2031. Measured on 2026-11-10 on a 512 MB free instance: a Spring Boot 4.1
service on Java 25 idles at 169 MB resident without tuning and 133 MB with
conservative JVM flags.

## Decision

Kotlin 2.4 on Spring Boot 4.1, Java 25, built with Gradle. The server
module depends on a pure Kotlin module (`shared`) that holds the rules
evaluator, the rule validator and the contract models; that module targets
the JVM from the first slice and gains Android and iOS targets when the
mobile client arrives. Persistence through Spring Data JPA with Hibernate in
validation mode (ADR 0003); security through Spring Security primitives
(ADR 0008).

## Alternatives considered

- Java 25 on Spring Boot. Zero language learning, the strongest keyword in
  job postings, the same platform. Not chosen because the evaluator and the
  models could not be shared with a Kotlin Multiplatform client, which would
  mean a second implementation kept honest only by a conformance suite, and
  because a portfolio in exactly the language and framework of the
  maintainer's day job shows less growth than one that adds a language on
  familiar ground.
- Ktor with Kotlin. Lighter and idiomatic, at the price of assembling
  security, validation, persistence and testing from parts, and of far
  smaller demand in the target market.
- NestJS with TypeScript. One language across server, web and a Capacitor
  client, no learning cost, the smallest first slice. Not chosen because it
  moves the backend away from the JVM, which is the professional axis this
  project exists to demonstrate.

## Consequences

- Positive: one implementation of the hardest logic across server and
  Android; Spring Boot remains the framework a reviewer recognises; modern
  Boot 4 practices on record.
- Negative: Kotlin with JPA needs the `kotlin-spring` and `kotlin-jpa`
  compiler plugins and discipline around data classes as entities; Gradle
  is mandatory because of Kotlin Multiplatform, so the Maven tooling the
  maintainer knows best is not used; two languages remain in the repository
  (Kotlin and TypeScript).
- Strongest argument against: a Java-first employer may discount Kotlin.
  The README states the reason for the choice in one paragraph, and the
  code reads from Java.

## Reopen when

The mobile client stops being Kotlin (ADR 0005), or Spring's Kotlin support
regresses in a way that blocks an upgrade, or a measured startup or memory
figure exceeds what the free instance allows after the mitigations in ADR
0011.

## Links

ADR 0003, ADR 0005, ADR 0009, ADR 0011. Spring Boot system requirements
(docs.spring.io, 2026-11-05); Spring Framework Kotlin requirements
(docs.spring.io, 2026-11-05); Adoptium support roadmap (adoptium.net,
2026-11-05); Kotlin releases (kotlinlang.org, 2026-11-05).
