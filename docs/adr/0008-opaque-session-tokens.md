# 0008. Opaque session tokens with cookie and bearer transports

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

Survey managers authenticate; respondents do not. The system has one API
and one database and no second service that needs to verify a credential
without asking the database. Real revocation is a requirement: a session
must stop working when it is logged out or when the account is disabled.
Verified on 2026-11-05: OWASP ASVS 5.0, level 1, requires session tokens to
be verified by a trusted backend service, generated with a CSPRNG with at
least 128 bits of entropy, replaced on authentication and terminated so
that further use is impossible; the OWASP Session Management Cheat Sheet
advises against storing session identifiers or tokens in browser Web
Storage and for cookies with `HttpOnly`, `Secure` and `SameSite`. A hosted
identity server such as Keycloak needs about 1,250 MB per pod, which no
free tier offers.

## Decision

Sessions are opaque tokens: 256 bits from a CSPRNG, stored as a SHA-256
hash with creation, last-use, expiry and revocation timestamps. Idle
timeout 30 minutes, absolute timeout 12 hours, a new token on every login,
revocation by deletion, all sessions revoked when an account is disabled.
Passwords are hashed with BCrypt through Spring Security's delegating
encoder, so the algorithm can change without a data migration. The web
client receives the token in a `__Host-session` cookie (`HttpOnly`,
`Secure`, `SameSite=Strict`) and sends a custom header on mutating requests
as a second CSRF defence; the mobile client receives the same token as a
bearer credential and stores it in the platform keystore or keychain. The
contract declares both security schemes.

## Alternatives considered

- JWT access tokens with refresh-token rotation. The industry default and
  the pattern interviewers ask about. Its advantage, verification without
  a database lookup, is unused here, and real revocation would need a
  server-side list anyway; a signed token also tempts developers to put
  claims in it that then leak. Not chosen; documented as the alternative
  for the case that a second verifier appears.
- Keycloak or another identity server. Standards, single sign-on and
  admin screens for free in software, at a memory cost no free hosting
  carries, and a second system to keep alive for six months.
- A hosted identity service. Removes the code, adds an external
  dependency that breaks the one-command local start and that can change
  its free plan.

## Consequences

- Positive: revocation is a delete; no signing key to protect or rotate;
  the token carries nothing to leak; ASVS level 1 session requirements are
  met by construction.
- Negative: one database lookup per authenticated request; cookies require
  the web client and the API to share a registrable domain, which fixes the
  demo's DNS layout; CSRF defence must be tested.
- Strongest argument against: a reviewer may read "no JWT" as inexperience.
  The README and this record state the reasoning; the pattern chosen is the
  one OWASP recommends for a browser client.

## Reopen when

A second service must verify sessions without the database, single sign-on
is required, or a measured lookup cost on the free tier becomes visible.

## Links

ADR 0001, ADR 0011. OWASP ASVS 5.0 chapter V7 (github.com/OWASP/ASVS,
2026-11-05); OWASP Session Management Cheat Sheet
(cheatsheetseries.owasp.org, 2026-11-05); Keycloak sizing guide
(keycloak.org, 2026-11-05).
