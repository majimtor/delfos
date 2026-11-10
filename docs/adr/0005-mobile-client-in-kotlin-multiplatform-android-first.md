# 0005. Mobile client in Kotlin Multiplatform, Android first

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

A mobile client is part of the vision and comes after the first public
slices. Distribution constraints verified on 2026-11-05: the Apple
Developer Program costs 99 USD per year and without it an app runs only on
up to three of the developer's own devices with profiles that expire after
seven days; Google Play charges a one-time 25 USD fee and requires personal
accounts to run a closed test with twelve testers for fourteen days before
production. The project budget is zero, so iOS cannot be distributed to a
reviewer and Android is distributed as an APK from GitHub Releases. Compose
Multiplatform is declared stable on Android, iOS and desktop (kotlinlang.org,
page dated 2025-09-10). The server is Kotlin (ADR 0002) and the rules
evaluator lives in a shared Kotlin module (ADR 0009).

## Decision

Kotlin Multiplatform with Compose Multiplatform, Android first. The shared
module provides the rules evaluator, the rule validator and the contract
models to the mobile client without a second implementation. The Android
build is published as an APK on GitHub Releases by CI; the iOS target is
compiled in CI on a macOS runner as proof of portability and is not
distributed. Tokens are stored in the platform keystore and keychain.

## Alternatives considered

- Flutter. The most mature cross-platform toolkit and a tool the maintainer
  already uses in production; the fastest route to a mobile screen. Not
  chosen because it would be a third implementation of the evaluator in
  Dart, shares nothing with the server or the web, and demonstrates nothing
  the maintainer does not already demonstrate. It remains the fallback if
  time becomes the binding constraint at the mobile slice.
- React Native with Expo. Shares TypeScript with the web, not with the
  server; neither in daily use nor Kotlin.
- Capacitor around the web client. Cheapest, and it demonstrates no mobile
  competence; a shell is not a client.
- Native Android with Compose, without the multiplatform layer. Simpler
  tooling, no iOS at all. The fallback if the iOS CI build costs more than
  it proves.

## Consequences

- Positive: the hardest logic exists once across server and Android; a
  Kotlin end-to-end story that a reviewer reads in a minute; real native
  Android competence on record.
- Negative: multiplatform Gradle builds are complex; Compose Multiplatform
  on iOS is young and its community smaller than Flutter's; iOS is never
  demonstrable within the budget; learning Compose on top of Kotlin.
- Strongest argument against: a multiplatform framework whose second
  platform cannot be shipped is paying for an option that may never be
  exercised. The option is kept cheap: iOS is a CI target, not a product.

## Reopen when

The iOS CI build exceeds the hours it proves, the shared module stays
empty in practice, the backend stops being Kotlin, or the budget decision
changes so that iOS can be distributed.

## Links

ADR 0002, ADR 0009, ADR 0011. Apple Developer Program and membership
comparison (developer.apple.com, 2026-11-05); Google Play registration and
closed-testing requirements (support.google.com, 2026-11-05); Kotlin
Multiplatform supported platforms (kotlinlang.org, 2026-11-05).
