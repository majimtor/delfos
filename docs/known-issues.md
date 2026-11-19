# Known issues and operating figures

What is known not to work, or to work within limits, in the demo and in
the code, with the date it was seen and what answers it; and the figures
the quarterly check of section 17 of the plan reads. A row leaves this
file when its issue is fixed, with the pull request that fixed it.

## Known issues

| Issue | Seen | Effect | Answer |
| --- | --- | --- | --- |
| GitHub runs scheduled workflows late or not at all: the daily demo reset of 05:50 ran at 14:00 on 2026-11-12, and the keep-alive, then a workflow scheduled every ten minutes, ran five times between 2026-11-11 23:37 and 2026-11-13 02:08 | 2026-11-13 | A reset later than announced; the API also reseeds at every start | The keep-alive moved to a cron trigger on the Cloudflare Worker on 2026-11-16 (plan, section 12); the reset could follow, with its token as a Worker secret, if a late reset ever matters |
| The API wakes in 47 seconds without the seed and in about 95 with it | 2026-11-11 | The first visitor after a sleep waits | The wait screen, which explains and counts |
| Anyone with the link can answer an anonymous survey several times | By design | Duplicates in the results of the demo surveys | Invited mode, with one response per single-use link, for the cases that need one per person |
| The invitation tokens of the demo surveys are public, `<public id>-<n>` | By design | Anyone can use the pending ones | Real invitations get random tokens shown once |
| An account created through the demo administrator disappears at the next reset, with its surveys | By design | A visitor's work lasts a day at most | The README says so |
| A scale question is a row of buttons: one tab stop per value where a radio group would offer one | 2026-11-11 | More keystrokes for keyboard users | Kept: the VoiceOver pass of 2026-11-17 found nothing to fix in it |
| The theme is light only, while the page lets the browser use a dark colour scheme for its native controls | 2026-11-11 | Mixed appearance under a dark system theme | Accepted |
| The workbook export is minimal: no bold header, column widths or date cells | 2026-11-13 | Plain cells: a reader widens the columns by hand, and the submission dates are ISO text, not dates. Opened on 2026-11-16 in Numbers and in Google Sheets: no import warning, the header row and every column in place, numbers as numbers, the options of a multiple-choice question as columns of 0 and 1 | Accepted |
| The results event stream closes every ten minutes and the browser reconnects; a response arriving in that gap is announced by the next poll, up to a minute later | 2026-11-13 | A late update, rarely | Accepted |
| The web has a manifest and icons but no service worker: nothing works offline | 2026-11-13 | Chrome 124 on Android 15 offers "Install app" in its menu and opens the installed web in a standalone window without the browser bar (seen on 2026-11-16 on an emulator, where it was added as a home-screen shortcut); without network the installed web shows nothing | Accepted; offline use is in the plan's postponement list |
| The tour video of the README is WebM | 2026-11-12 | Safari before 14.1 does not play it | Accepted |
| The APK on each release is signed with a debug key generated on the runner at that moment, so every release carries a different signature | 2026-11-14 | Android warns that the developer is unknown, and a newer release does not install over an older one until the older one is removed | Accepted: a stable key would have to live in the repository or in the CI secrets, and this project distributes with none (plan, section 15; ADR 0005) |
| The iOS application is compiled in CI for the simulator and never run there: the keychain store and the time formatting of its platform code have not executed on a device or a simulator | 2026-11-14 | A defect in that platform code would show only when someone builds and runs it on a Mac | Accepted (ADR 0005: iOS is a CI target, not a product); the maintainer's Mac is Intel and cannot run the Apple-silicon simulator binary |

## Operating figures

Read at the quarterly check; one row per reading. The hours of Neon and
Render come from their dashboards.

| Date | Cold start | Results of the mobility survey | Neon compute hours (of 100 a month) | Neon storage (of 1 GB) | Render instance hours (of 750 a month) | Last successful demo reset | Dependabot |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 | 47 s without the seed, 95 s with it, measured on 2026-11-11 | not read | not read | not read | not read | 2026-11-12 14:00 UTC, scheduled and late | nothing open |
| 2026-11-16 | 91 s with the seed; 2.26 s for the first call after it | 0.27 to 0.38 s warm, 0.58 s for the workbook export | not read | not read | not read | 2026-11-15 12:58 UTC, scheduled and seven hours late | nothing open |
| 2026-11-17 | 91 s with the seed, measured on 2026-11-16; on 2026-11-17 the Worker's cron kept the API awake all day | 2.9 s for the first call, with Neon's compute waking; 0.31 to 0.46 s after it; 0.99 s for the workbook export | 4.33 since 2026-11-10 | 33.39 MB, plus 3.49 MB of history | 38.3 since 2026-11-01; bandwidth 61 MB of 5 GB | 2026-11-17 13:56 UTC, by hand at the check; the scheduled one of 05:50 ran at 12:58 on 2026-11-15 | nothing open; #57, #58 and #64 merged this week |

## Quarterly checks

The procedure is in section 17 of the plan and `deploy/quarterly-check.sh`
prints the measured figures. Each check adds a row above and a line here
with its date and what it changed.

- 2026-11-17, the first, at launch: demo opened and logged in; the results of
  the mobility survey timed; seed reset by hand (the scheduled reset runs about
  seven hours late); Neon and Render read from their dashboards; Dependabot
  groups merged during the week; seed-reset workflow enabled and the Worker's
  cron confirmed by an API that answered warm all day. Changed: nothing. Next
  check due on 2027-02-15.

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
