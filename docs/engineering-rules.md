# Engineering rules

These rules apply to every change in this repository, whoever or whatever
produces it. Each rule names the failure it prevents. A rule without a
failure behind it does not belong here. When a rule and a situation do not
fit, the exception is written down next to the code with the reason, and the
rule is revisited.

## Language

- Code, identifiers, comments, commit messages, pull requests, documentation
  and configuration are written in English.
- User-facing text is bilingual, Spanish first and English second, through
  the translation catalogues of each client. Sample surveys in the seed are
  content, not interface text: they exist as separate Spanish and English
  surveys.
- Error responses carry stable codes, not translated messages. Clients
  translate. This keeps the API language-neutral and avoids a second
  translation catalogue on the server.

## Commits

- One logical change per commit. Messages follow Conventional Commits:
  `type(scope): summary` in the imperative, at most 72 characters, with a
  body that explains why when the diff does not. Types in use: `feat`,
  `fix`, `docs`, `test`, `ci`, `build`, `chore`, `refactor`, `perf`.
- Forbidden summaries: `wip`, `update`, `fix` without an object, `misc`.
- History is never rewritten after a push. A mistake gets a new commit.
- Commits are signed. The author identity is configured per repository and
  checked with `git config --show-origin user.email` before the first
  commit on a new machine. The one exception is the commit of the release
  pull request, which `release-please` writes through the API without a
  signature: it never reaches `main` as such, because the squash merge
  creates the commit on `main`, signed by GitHub. For that reason the
  branch protection of `main` does not require signed commits on the pull
  request branches; every commit on `main` is signed all the same.

## Branches and pull requests

- `main` is always deployable. Work happens on short-lived branches named
  `type/short-description`, merged through a pull request with squash. The
  pull request title becomes the commit message, so it follows the commit
  rules.
- A pull request answers five questions in its description: what changes,
  which failure it prevents or which capability it adds, which test covers
  it, what is deliberately left out, which document changes with it.
- Keep pull requests small enough to read in one sitting, as a guide under
  400 changed lines. Split otherwise.
- Required checks must pass before merging. There is no second reviewer;
  the description above is the review.
- Exception, written here on purpose: the initial documentation commits
  that precede the first CI workflow go directly to `main`. From the commit
  that adds the workflow on, everything goes through a pull request.

## The API contract

- `contract/openapi.yaml` is the single source of truth for the HTTP API.
  Server interfaces, client types and request validation are generated from
  it. Generated code is not committed.
- A change to the contract and the code that implements it travel in the
  same pull request, with an entry in `contract/CHANGELOG.md`.
- Breaking changes are what `oasdiff breaking` reports. Until launch they
  are allowed with a changelog entry. After the first distributed client
  they require a new major prefix (`/api/v2`) and a deprecation period for
  the previous one.
- Errors use one shape, Problem Details (RFC 9457), with a stable `code`
  and optional `errors[]` for field-level detail. No endpoint returns a bare
  string or an ad hoc object on failure.
- Lists are paginated with an opaque cursor and a bounded `limit`. No
  endpoint returns an unbounded collection.

## Schema and migrations

- The schema lives in versioned Flyway migrations under the API module.
  Nothing else creates or alters tables: no `ddl-auto` other than
  `validate`, no manual changes on a live database.
- The application fails to start if its entities do not match the schema.
  This failure is intended and must not be silenced.
- A migration is immutable once merged. A correction is a new migration.
- Enumerations of the domain (statuses, question types, operators) are
  code, not lookup tables. Cascading deletes exist only inside an aggregate.

## Environment and lockfiles

- Tool versions are pinned in one place per ecosystem: the Gradle toolchain
  for Java, `.node-version` and `engines` for Node, the Gradle wrapper and
  `package-lock.json` for dependencies. Installation and CI use `npm ci` and
  the wrapper; a mismatch fails the build instead of warning.
- Lockfiles are committed. A change to a lockfile without a change to the
  corresponding manifest is reviewed as a dependency change.

## Dependencies

- Every dependency has a reason written at the point where it is added
  (pull request description and, for the long-lived ones, the ADR or the
  README section that justifies it). "Convenient" is not a reason.
- Automated updates are grouped per ecosystem and merged automatically for
  patch and minor versions when checks pass. Major versions are updated by
  hand, one at a time, with the migration notes read first.
- GitHub Actions are pinned to a commit SHA, run with the minimum
  permissions and never use `pull_request_target`.

## Experimental technology

- Nothing in beta, preview or experimental status enters the first public
  slice without an ADR that names the stable alternative it displaces, where
  it is contained and how it is removed.
- A deprecated API or library version is not introduced. One already in use
  is replaced before the next major update of its ecosystem.

## Secrets and personal data

- No secret in the repository, ever: no keys, passwords, tokens, connection
  strings or signing material, in code, comments, examples, tests or
  history. Examples use obvious placeholders; `.env.example` documents the
  variables without values.
- A secret-scanning hook runs before every commit and in CI, and push
  protection stays enabled. A secret that reaches the history is rotated
  first; the history is not rewritten.
- No real personal data in the repository, the seed, the tests or the demo:
  people, emails, phone numbers and photos are fictitious, and emails use
  reserved documentation domains.
- The application stores no IP addresses, device fingerprints or request
  bodies in its logs. Rate limiting keeps its counters in memory only.
- Configuration values come from the environment. A missing value fails
  startup with a message naming the variable; there are no production
  defaults for credentials.

## Security headers and CI gates

- The API sets the standard security headers on every response and
  restricts CORS to the configured web origin. A test asserts both.
- CI runs, in order: formatting and lint, unit tests, API tests against a
  real database, contract compatibility (`oasdiff`), web build in both
  languages with the untranslated-keys check, code scanning, image build.
  A red step blocks the merge; there is no "allow failure".
- Deployment happens only from `main` and only after the checks pass.

## Code conventions

- Names say what a thing is in the domain: `SurveyVersion`, `publishSurvey`,
  `VisibilityRule`. Generic names such as `data`, `items`, `result`,
  `handleClick`, `Manager`, `utils`, `common` and `helpers` are not used for
  modules or types; a method may use a short local name when its scope is a
  few lines.
- Layers exist where they isolate a change, and nowhere else. There is no
  DTO, mapper or service created by reflex for every entity. A controller
  may call a repository directly when there is no rule in between, and the
  comment says so.
- Comments explain why, not what: the constraint, the bug or the measurement
  behind a line that looks odd. No comment restates the code; no
  documentation comment on trivial members.
- Facts have one source: the schema (migrations), the contract (OpenAPI),
  the validation rules (contract plus domain), tool versions (the pinned
  files). Code may be duplicated where abstracting it would couple unrelated
  parts; the duplication is noted at both sites.
- Every abstraction has a documented escape hatch for the case that does not
  fit.
- Failures are loud: a broken invariant throws, a missing configuration
  stops startup, a failed migration stops deployment. Nothing logs a warning
  and continues in a degraded state without a decision written down.
- Known limitations are recorded in `docs/known-issues.md` with the reason
  they were accepted and what would reopen them.

## Documentation

- Decisions that are expensive to reverse get an ADR in `docs/adr/`, written
  from the template, with real alternatives and a reopening condition. An
  ADR that only confirms a choice is rewritten or deleted.
- The README describes what the project does, how to run it with one
  command, how to run the tests and where the decisions are. It does not
  describe what the code already says.
- Badges are limited to the CI status and the licence.
- Personal tooling files (editor settings, local notes, local task runners)
  are excluded through `.git/info/exclude` or a global ignore file, not
  through the repository's `.gitignore`.
