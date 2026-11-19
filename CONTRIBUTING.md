# Contributing

Delfos is a one-person portfolio project built to a plan and a set of
rules; issues and pull requests are welcome within them.

- Read `PLAN.md` for what is built, what is postponed and why, and
  `docs/engineering-rules.md` for the rules every change follows.
- Set up with the README: one command starts the whole system, and each
  module says how to run its checks.
- Open a pull request from a branch named `type/short-description`. The
  template asks five questions, and the answers are the review. Commits
  follow Conventional Commits; the merge is a squash.
- Every gate of CI blocks: formatting, tests, contract compatibility, code
  scanning and the end-to-end path. A change to the API starts in
  `contract/openapi.yaml` and in its conformance suite.
- Code, comments and documents are in English; the interface is bilingual,
  Spanish first, through the message catalogues.
- No secrets and no real personal data in any file, test or seed.
- The code of conduct applies to every thread.
- Anything on the plan's postponement list (section 18) needs its trigger
  first: open an issue to discuss it before coding.
