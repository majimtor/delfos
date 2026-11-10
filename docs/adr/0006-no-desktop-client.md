# 0006. No desktop client

- Status: accepted
- Date: 2026-11-10
- Deciders: the maintainer

## Context

Survey tools sometimes ship a desktop application for administration or
reporting. Everything such a client would do in Delfos, managing surveys,
viewing results and exporting them, is a web page or a download, and
nothing in the domain needs local hardware, local files or an offline
workflow.

## Decision

There is no desktop client. The web application is responsive, serves
every management use case, and ships a web application manifest so it can be
installed from a browser. It does not implement an offline service worker;
there is nothing to synchronise.

## Alternatives considered

- Electron or Tauri wrapper. A browser in a window, plus packaging,
  signing and update channels to maintain, for no capability the browser
  lacks here.
- Compose Multiplatform desktop target. Cheap to compile once the mobile
  client exists, still a client to design, test and keep current; it would
  be a demonstration of tooling, not of the product.
- A reporting tool with direct database access. Rejected on the grounds of
  ADR 0001.

## Consequences

- Positive: one web client to keep accessible and translated; exports are
  the reporting feature and they are testable on the server.
- Negative: a user who wants a native desktop feel does not get it; printing
  relies on the browser.
- Strongest argument against: desktop clients are where some employers
  still live. The project targets backend and web roles first; the point is
  not denied, it is deprioritised.

## Reopen when

A requirement appears that needs a local process: hardware, bulk offline
work, or integration with desktop software.

## Links

ADR 0001, PLAN.md section 18.
