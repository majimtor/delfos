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
| The mobile client has no wait screen: while the API wakes from its sleep, opening a survey or logging in fails with "could not be loaded" or "we could not log you in" | 2026-11-14 | The first user after a sleep retries after a minute or two | A wake screen like the web's is a follow-up; the Try again action covers it meanwhile |
| The APK on each release is signed with a debug key generated on the runner at that moment, so every release carries a different signature | 2026-11-14 | Android warns that the developer is unknown, and a newer release does not install over an older one until the older one is removed | Accepted: a stable key would have to live in the repository or in the CI secrets, and this project distributes with none (plan, section 15; ADR 0005) |
| The iOS application is compiled in CI for the simulator and never run there: the keychain store and the time formatting of its platform code have not executed on a device or a simulator | 2026-11-14 | A defect in that platform code would show only when someone builds and runs it on a Mac | Accepted (ADR 0005: iOS is a CI target, not a product); the maintainer's Mac is Intel and cannot run the Apple-silicon simulator binary |

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

## Clean-machine drill

Performed on 2026-11-14 on a fresh clone in a new directory, as section 17
of the plan asks: clone, hooks path set, `docker compose up --build`
(2 min 59 s to a healthy stack, the API ready 13 s later), the shared
module checks on the JVM and Android (35 s), the API tests with
Testcontainers (49 s), the mobile host tests and debug APK (53 s), the web
checks (`npm ci` 14 s, `npm run check` 32 s), the web build and the
end-to-end path against the compose stack (45 s); 7 minutes in all, every
step green. The commit identity of the clone came from the repository's
own configuration. The Java on the path was 19 and the Node 22; Gradle
provisioned the JDK it declares and the web was run with the Node version
of `web/.node-version`, as the README says. The change made through a
pull request afterwards was the one that set the release version to 1.0.0.
