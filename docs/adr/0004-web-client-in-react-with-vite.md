# 0004. Web client in React with Vite

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

The web client has two very different parts: a public page to answer a
survey, which is simple, and an editor for surveys with conditional rules,
which is the most complex user interface in the project. The maintainer
works with React daily. Verified on 2026-11-10 from the npm registry: React
19.3, Vite 8.3, React Router 8.5, TanStack Query 5.107, react-hook-form
7.91, react-intl 12.1, MUI 9.5, Vitest 5.1, Playwright 1.65, TypeScript 7.0.
Hosting is static (ADR 0011).

## Decision

React 19 with TypeScript, built with Vite, as a single-page application on
static hosting. Routing with React Router; data access, caching and polling
with TanStack Query; forms with react-hook-form validated by schemas derived
from the API contract; editor state in reducers; internationalisation with
FormatJS (`react-intl`) and message extraction checked in CI; components
from MUI, with custom components only in the editor; unit tests with Vitest,
end-to-end tests with Playwright, accessibility checks with axe. Types and
the HTTP client are generated from the contract with `openapi-typescript`
and `openapi-fetch`.

## Alternatives considered

- Angular. An integrated framework with a yearly major release cycle and
  built-in internationalisation; not in the maintainer's daily use and with
  lower demand in the target market. Not chosen; it would put the most
  expensive interface on the least practised tool.
- Next.js or another meta-framework. Server rendering and file routing
  that a static demo does not need, at the price of a server or a static
  export step. Not chosen.
- Vue or Svelte. Capable and lighter, with a learning cost that buys
  nothing a reviewer of this project would weigh.
- Headless primitives with hand-written styles instead of a component
  library. More craft, more hours, more accessibility to prove by hand.
  Not chosen for the first slices; the editor is the one place where
  custom components are allowed.

## Consequences

- Positive: the editor is built on daily-used tools; generated types keep
  the client honest against the contract; one build serves both languages
  with a runtime switch.
- Negative: the React ecosystem is assembled from parts, each of which is a
  dependency to justify and to update; bundle size needs watching; the
  component library's look needs restraint to avoid a template feel.
- Strongest argument against: Angular would give the same result with
  fewer choices to defend. The choices are listed with their reasons in the
  README and kept to the minimum above.

## Reopen when

The target job market shifts clearly to Angular, a server-rendering
requirement appears, or the dependency list grows beyond what one person
updates in an hour a month.

## Links

ADR 0007, ADR 0011, ADR 0015. npm registry (registry.npmjs.org,
2026-11-10); OWASP Session Management Cheat Sheet (cheatsheetseries.owasp.org,
2026-11-05) for the cookie transport used by the client.
