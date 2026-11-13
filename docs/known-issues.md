# Known issues and operating figures

What is known not to work, or to work within limits, in the demo and in
the code, with the date it was seen and what answers it; and the figures
the quarterly check of section 17 of the plan reads. A row leaves this
file when its issue is fixed, with the pull request that fixed it.

## Known issues

| Issue | Seen | Effect | Answer |
| --- | --- | --- | --- |
| GitHub runs scheduled workflows late or not at all: the keep-alive, scheduled every ten minutes from 06:00 to 24:00 UTC, ran five times between 2026-11-11 23:37 and 2026-11-13 02:08, and the daily demo reset of 05:50 ran at 14:00 | 2026-11-13 | The API sleeps between runs; the wait screen covers the wake, up to two minutes with the seed | Accepted for now; a cron trigger on the Cloudflare Worker that serves the web is the reliable replacement if it matters (plan, section 12) |
| The API wakes in 47 seconds without the seed and in about 95 with it | 2026-11-11 | The first visitor after a sleep waits | The wait screen, which explains and counts |
| Anyone with the link can answer an anonymous survey several times | By design | Duplicates in the results of the demo surveys | Invited mode, with one response per single-use link, for the cases that need one per person |
| The invitation tokens of the demo surveys are public, `<public id>-<n>` | By design | Anyone can use the pending ones | Real invitations get random tokens shown once |
| An account created through the demo administrator disappears at the next reset, with its surveys | By design | A visitor's work lasts a day at most | The README says so |
| A scale question is a row of buttons: one tab stop per value where a radio group would offer one | 2026-11-11 | More keystrokes for keyboard users | Revisit after the screen-reader pass |
| No screen-reader pass yet; the accessibility tree stood in for it | 2026-11-11 | Unknown problems with VoiceOver or NVDA | Pending, see `docs/accessibility.md` |
| The theme is light only, while the page lets the browser use a dark colour scheme for its native controls | 2026-11-11 | Mixed appearance under a dark system theme | Accepted |
| The workbook export is minimal: no bold header, column widths or date cells, and it has not been opened in a spreadsheet application yet | 2026-11-13 | Plain cells; a reader may find a quirk | Open one from the demo in Excel or Numbers and record the result here |
| The results event stream closes every ten minutes and the browser reconnects; a response arriving in that gap is announced by the next poll, up to a minute later | 2026-11-13 | A late update, rarely | Accepted |
| The web has a manifest and icons but no service worker: nothing works offline, and an install prompt has not been seen yet | 2026-11-13 | Installable only where the browser does not require a service worker | Check on a phone and record the result here; offline use is in the plan's postponement list |
| The tour video of the README is WebM | 2026-11-12 | Safari before 14.1 does not play it | Accepted |

## Operating figures

Read at the quarterly check; one row per reading. The hours of Neon and
Render come from their dashboards.

| Date | Cold start | Neon compute hours (of 100 a month) | Neon storage (of 1 GB) | Render instance hours (of 750 a month) | Last successful demo reset | Dependabot |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 | 47 s without the seed, 95 s with it, measured on 2026-11-11 | not read | not read | not read | 2026-11-12 14:00 UTC, scheduled and late | nothing open |

## Quarterly checks

The first is due in February 2027; the procedure is in section 17 of the
plan. Each check adds a row above and a line here with its date and what
it changed.
