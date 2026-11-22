# 0017. Visual identity and design tokens

- Status: accepted
- Date: 2026-11-22
- Deciders: the maintainer

## Context

Until this record the web looked like the Material library it is built with:
its standard blue, the system font, upper-case buttons, no mark, a landing
page with two buttons and most of the viewport empty, and a light theme only
while the page let the browser use a dark colour scheme for its native
controls (`docs/known-issues.md`). That is correct and accessible, and it is
also the first thing a visitor sees before reading a line of the repository.
Section 1 of the plan rejects ornamental layers; section 13 fixes WCAG 2.2 AA
for the public pages, verified with axe on every pull request. A visual
identity has to pass both: be a set of named decisions the code reads, and
change nothing of what the checks guarantee.

## Decision

The identity is a set of design tokens declared once, in the theme of the
web, and documented here.

- Colour: a petrol brand colour, `#0B5B6B` on light surfaces and `#6CC5D6` on
  dark ones, with neutrals biased towards it (`#F4F7F7` page, `#FFFFFF`
  surface, `#0F1F20` text, `#44585A` secondary text, `#C9D6D6` borders, and
  their dark counterparts `#0C1517`, `#122023`, `#E6EEEE`, `#9DB2B3`,
  `#2B4145`). Semantic colours for success, warning, error and information
  are separate from the brand colour and keep their meaning in both schemes.
  Every text pair reaches 4.5:1; a unit test of the theme fails when a value
  breaks that.
- Two colour schemes with the same roles. The device's choice applies by
  default; the visitor can choose light or dark from the application bar, and
  the choice is remembered per browser. A script loaded before the
  application sets the scheme on the document, so the first paint already has
  the right background.
- Type: IBM Plex Sans, variable weight axis, served with the web from its own
  origin, as the content security policy requires. Spanish and English
  visitors download the Latin subset only, 46 KB. Headings are semibold with
  slightly tightened tracking; buttons are sentence case; figures in tables
  and results are tabular.
- Shape and surfaces: a 6 px radius on controls and 10 px on cards and
  frames; flat surfaces with a 1 px border instead of shadows; buttons and
  icon buttons at least 44 px tall, which is the touch target of WCAG 2.5.8
  with margin.
- The mark: the delta of the favicon, in the brand colour, next to the name
  in the application bar; the favicon, the installable icons, the manifest
  and the browser theme colour follow the palette.
- A budget of the built web, enforced by a script after every build: 900 KB
  of JavaScript, 60 KB of CSS and 50 KB for the Latin font file.

The Material library stays; the theme is declared through its colour schemes
and CSS variables, so every component reads the tokens and the mobile client
can take the same values later.

## Alternatives considered

- Keep the library's defaults. No cost and no decision; it reads as a
  scaffold and leaves the dark scheme half done.
- Replace the component library with utility classes and own components.
  Weeks of work and the loss of components whose keyboard and screen-reader
  behaviour is already verified.
- A display serif or a fashionable grotesque. More personality for a
  magazine; for a tool whose pages are forms and tables, a text face with
  good numerals serves better.
- Fonts from a hosted service. Blocked by the content security policy, and a
  third party in every page view.

## Consequences

- Positive: a page that looks decided; a dark scheme that closes a row of the
  known issues; tokens with names that the plan, the tests and the mobile
  client can cite; a budget that notices growth.
- Negative: 46 KB more on a first visit; two palettes to keep in step; the
  screenshots of the README and the social card must be taken again after
  each visual change.
- Strongest argument against: taste is not a requirement, and time spent on
  it is time not spent on the postponement list. Accepted because the demo is
  the first impression, and the pass is bounded by this record.

## Reopen when

The brand changes; the mobile client needs the same tokens; the budget is
exceeded by a dependency the project wants; or a measured contrast on a real
device contradicts the computed one.

## Links

ADR 0004, ADR 0015, PLAN.md sections 1 and 13. WCAG 2.2 (W3C, 12 December
2024), success criteria 1.4.3, 1.4.11 and 2.5.8; Material UI documentation on
colour schemes and CSS theme variables (mui.com, 2026-11-22); Fontsource, IBM
Plex Sans variable (fontsource.org, 2026-11-22).
