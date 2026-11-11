# 0016. Hosting on free tiers with the web on Cloudflare Workers

- Status: accepted
- Date: 2026-11-11
- Deciders: the maintainer

## Context

ADR 0011 fixed the hosting under two constraints, zero euros per month and
no payment card anywhere, and put the web on Cloudflare Pages while PLAN.md
section 19 left open whether Pages or Workers with static assets would
serve the single-page application, to be decided at the first web
deployment. That moment arrived on 2026-11-11 and the facts were read again
from the providers' documentation on that date.

Cloudflare: its migration guide describes Workers as having "a distinctly
broader set of features" than Pages and keeps a compatibility matrix in
which the Vite plugin and Workers Logs exist only on Workers; "requests to
static assets are free and unlimited"; a Worker without a script serves a
single-page application when `assets.not_found_handling` is
`single-page-application`; a custom domain declared in the Wrangler
configuration makes Cloudflare "create DNS records and issue necessary
certificates", provided the hostname has no existing `CNAME`; the Free
plan's limit of 100,000 requests per day applies to script invocations, of
which an asset-only Worker has none; a Worker version may hold 20,000
files of 25 MiB each. Cloudflare's own Git integration, Workers Builds,
offers 3,000 build minutes per month, but its documentation does not state
whether the whole repository is cloned when the root directory is a
subfolder, and the web build reads the contract from `contract/`. The
Universal certificate covers the apex and first-level subdomains only, so a
proxied `api.delfos.manuelalejandro.com` would not serve a valid
certificate without a paid add-on.

Render, unchanged: the free web service spins down after 15 minutes without
inbound traffic and spins up in "about one minute"; the workspace has 750
free instance hours per calendar month, and exhausting them suspends every
free service until the next month; a Blueprint can live at a custom path
and prompts for the variables marked `sync: false`.

Neon, unchanged in price, refined in use: the Free plan offers 100
compute-unit hours per project and month, "enough to run a 0.25 CU compute
in a project for 400 hours/month"; the compute suspends after five minutes
of inactivity, fixed on the Free plan, and "reactivates automatically
within a few hundred milliseconds"; the pooled endpoint runs PgBouncer in
transaction mode, which rejects session-level features such as the
advisory lock Flyway takes while migrating; a 0.25 CU compute accepts 105
direct application connections. The documentation does not say whether an
open but idle client connection counts as activity.

Measured on 2026-11-10 with a throwaway Spring Boot 4.1 service on Render:
wake-up after sleep 93 s with default JVM settings, 53 s with conservative
flags, 47 s with flags plus a JVM AOT cache built into the image; resident
memory 133 to 169 MB; database round trip to Neon in Frankfurt 3 to 21 ms.

## Decision

API on a Render free web service in Frankfurt, deployed from the
repository's Dockerfile when CI passes, declared in `deploy/render.yaml`.
PostgreSQL on Neon in Frankfurt, reached through the direct endpoint so
that Flyway's lock works, with a connection pool that keeps no idle
connection longer than a minute so that the compute can scale to zero while
the API stays awake. Web on a Cloudflare Worker that serves the built
single-page application as static assets, declared in `web/wrangler.jsonc`
and deployed by the CI workflow from `main` after the same checks as the
API, with `wrangler` authenticated by a scoped API token held in the
repository's Actions secrets. Container images on GitHub's registry; CI on
GitHub Actions.

Web and API live under the maintainer's own domain so that session cookies
work and the URL outlives any provider: `delfos.manuelalejandro.com` is
declared in the Worker configuration and created by Cloudflare with its
certificate; `api.delfos.manuelalejandro.com` is a `CNAME` to the Render
service with Cloudflare's proxy off, permanently, because of the
certificate depth above. Three measures against sleep, as in ADR 0011: JVM
flags and an AOT cache in the image; a scheduled workflow that requests the
liveness endpoint, which never touches the database; a wait screen in the
web client while the API is unreachable. The whole system also runs locally
with one `docker compose up`, with the same image, the same database engine
and the same seed.

## Alternatives considered

- Cloudflare Pages, the choice of ADR 0011. Same free serving of a
  single-page application and per-branch previews out of the box; one more
  product, which Cloudflare is folding into Workers, for no feature the demo
  uses, and a Git integration that would deploy regardless of this
  repository's pipeline.
- Workers Builds, Cloudflare's Git integration, instead of the pipeline. No
  token to manage; deploys on every push whether or not the checks passed,
  keeps the API's address in a dashboard instead of the repository, and
  leaves the monorepo clone unspecified. Kept as the fallback if the token
  route fails.
- Neon's pooled endpoint. Thousands of client connections instead of 105;
  one API instance with a pool of ten never needs them, and transaction
  pooling breaks the migration lock.
- A small virtual private server, providers that require a card as
  verification, and a native image: as in ADR 0011, excluded by the budget
  rule or kept as a later experiment.

## Consequences

- Positive: zero cost, no card anywhere; every deployment declaration lives
  in the repository (`deploy/render.yaml`, `web/wrangler.jsonc`, the
  workflow) and one pipeline decides both deployments; one-command
  portability; measured numbers rather than assumptions.
- Negative: a cold request may wait about 45 s when the keep-alive is not
  running; the first database request after a pause pays one connection
  handshake and the compute's wake-up of a few hundred milliseconds; a
  Cloudflare API token lives in Actions secrets and must be rotated if it
  leaks; no per-branch preview deployments; free plans change without
  notice; Render may restart the instance at any time and states that free
  services are not for production.
- Strongest argument against: a demo that may greet a reviewer with a
  45-second wait undermines the point of a demo. The keep-alive covers
  active months; the wait screen tells the truth for the rest; each
  provider can be swapped in an afternoon.

## Reopen when

Render, Neon or Cloudflare change their free plans; the keep-alive workflow
is disabled and the measured wake-up exceeds what the wait screen can
excuse; Neon's console shows the compute active while the API is idle, so
that the monthly compute hours come within reach; the API token proves
unable to manage the custom domain and the domain has to be managed by
hand; or the budget decision changes.

## Links

Supersedes ADR 0011. ADR 0003, ADR 0008, PLAN.md sections 11, 12 and 19.
Cloudflare documentation on static assets, single-page-application routing,
custom domains, billing and limitations, Workers Builds, platform pricing
and limits, the migration from Pages, and Universal SSL limitations
(developers.cloudflare.com, 2026-11-11); Render documentation on free
instances, Blueprints, custom domains and Cloudflare DNS (render.com,
2026-11-05, 2026-11-10 and 2026-11-11); Neon documentation on plans, scale
to zero, connection pooling, connection latency and Postgres versions
(neon.com, 2026-11-05 and 2026-11-11); GitHub Actions and Packages billing
(docs.github.com, 2026-11-05).
