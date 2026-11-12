# Accessibility of the public pages

Audit of 2026-11-11 against WCAG 2.2 level AA, on the pages a visitor
reaches without an account: the landing page, the answer page, the wait
screen, the page for unknown routes and the login page, together with the
header and the language switch they share. The interface was audited in
Spanish, the primary language, and checked for parity in English.

## Method

- Automated: axe-core with the WCAG 2.0, 2.1 and 2.2 rules of levels A and
  AA runs on every page of the end-to-end path (`web/e2e/`), on every pull
  request, and fails the build on any violation.
- Manual: a keyboard-only pass (tab order, visible focus, activation with
  Enter and Space, no traps); a review of the accessibility tree in
  Chromium for the name, role and state of every control and for the live
  regions; 200 % zoom and reflow at 320 px wide; the reduced-motion
  preference; the document language and the page titles.
- Not done: a pass with a screen reader (VoiceOver or NVDA). The
  accessibility tree stands in for it; the pass is pending and listed
  below.

## Findings and fixes

| Finding | Criterion | Fix |
| --- | --- | --- |
| The status chip of a draft showed white text on an orange of 3.2:1 | 1.4.3 | A darker warning colour in the theme, 5:1 |
| The document language was always Spanish, whatever the interface showed | 3.1.1 | The `lang` attribute follows the interface language from the first render |
| Every page shared the title "Delfos" | 2.4.2 | A title per page: the survey, the results, the login, the list, the unknown route |
| No way to skip the header with the keyboard | 2.4.1 | A skip link as the first tab stop, visible when focused |
| After a navigation the focus stayed wherever it was | 2.4.3 | The focus moves to the main region on every route change |
| Hints and errors hung from the fieldset, which assistive technology does not read for the inputs | 1.3.1, 3.3.1, 3.3.2 | They are attached to the control itself; required and invalid states are exposed on it |
| The end labels of a scale ("Muy mal · Muy bien") were not associated with the scale | 1.3.1 | The scale group is described by them |
| Keyboard focus on buttons was only the default ripple | 2.4.7, 2.4.11 | A visible ring on every button-like control |
| The elapsed-time counter of the wait screen sat in a live region and was announced every three seconds | 4.1.3 | The title and the explanation are announced once; the counter is visual only |
| The required marker was glued to the question text in the accessible name | 1.3.1 | A space between them |
| Animations played regardless of the visitor's preference | 2.3.3 (AAA) | Reduced motion honoured for spinners and transitions |

## Verified and left as is

Landmarks (banner, main) and one first-level heading per page; labels on
the login fields; the language of the survey content marked on titles,
descriptions and questions; target sizes of at least 24 by 24 pixels;
status messages for loading, for a recorded response and for a closed
survey; errors identified by text, never by colour alone; no time limits
and no keyboard traps; 200 % zoom without loss of content; reflow at 320 px
(the manager's survey table scrolls horizontally, which the criterion
allows for data tables).

## Known limits

- No screen-reader pass yet. It is the next step of this audit.
- A scale question is a row of buttons: operable with Tab and Enter, with
  one stop per value where a radio group would offer one. Kept for now;
  revisit if the pass above finds it confusing.
- The theme is light only, while the page allows the browser a dark colour
  scheme for its native controls.
- The manager pages (survey list, results) got the automated checks and the
  shared fixes, not a dedicated manual pass; the editor of the next slice
  will bring one.

## How to repeat it

The automated part is the end-to-end path (see the README). The manual
part is the list under Method, in the order given, with the keyboard only
and the browser's accessibility panel open.
