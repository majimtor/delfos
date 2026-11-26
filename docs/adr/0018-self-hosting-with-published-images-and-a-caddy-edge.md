# 0018. Self-hosting with published images and a Caddy edge

- Status: accepted
- Date: 2026-11-26
- Deciders: the maintainer

## Context

The demo runs on free tiers (ADR 0011, ADR 0016): the API on Render from the
repository's Dockerfile, the web on a Cloudflare Worker with the API's origin
embedded at build time, the database on Neon. On 2026-11-26 the maintainer
approved a milestone so that someone else, a school or an association, can
run Delfos on a machine of their own with what the repository publishes,
free under the MIT licence and without a guarantee of support (PLAN.md,
section 15, S6). The constraints stay: no cost to the maintainer, ten hours
a week, nothing that changes how the demo is hosted.

Facts verified on 2026-11-26: the CI workflow already builds the API image
and pushes it to GitHub's registry tagged with the commit and with `main`,
but no tag names a release; the web client reads the API's origin from
`VITE_API_BASE_URL` when it is built (`web/src/api/client.ts`), and an empty
value makes it call the page's own origin; the session cookie is a
`__Host-` cookie, which needs the web and the API on one secure origin or
the cross-origin setting the demo uses.

## Decision

Each release publishes two images, tagged with the version and with
`latest`: `ghcr.io/majimtor/delfos-api`, the image the demo already uses,
and `ghcr.io/majimtor/delfos-web`, Caddy serving the built web. The release
workflow waits for the images that the CI workflow of the same commit
pushed, so that a version names bits that passed every check, and names
both images on the release.

The web image is the edge of an instance: it terminates TLS for the site
address it is given, serves the built files with the single-page fallback
and the same response headers as the demo's Worker, and passes `/api/*` and
`/actuator/health/*` to the API container. The web is built with an empty
API origin, so one image serves any domain, and the two names it needs,
`DELFOS_SITE_ADDRESS` and `DELFOS_API_UPSTREAM`, come from the environment
of the container, with defaults for a local try on port 80. An operator's
compose file and the guide complete the milestone (#113, #114); the demo
keeps ADR 0016 as it is.

## Alternatives considered

- nginx as the edge. Smaller image and a familiar configuration, but no
  automatic certificates: an operator would add a certificate tool and a
  renewal job, which is the part most often done wrong.
- The API serving the web as static resources, one container for both.
  Couples the two release cycles, puts a JVM in front of static files, and
  makes the demo and the self-hosted instance differ in what the API is.
- Kubernetes manifests or a Helm chart. The audience is one machine; a
  chart would be published without anyone to run it.
- Only `latest`, without a version tag. Pinning is what lets an operator
  upgrade on purpose and roll back; `latest` is kept as a convenience and
  documented as moving.
- Building the images in the release workflow instead of waiting for the
  CI's. Simpler to read, but the release would carry a build that no check
  looked at.

## Consequences

- Positive: a version is a pair of images anyone can pull; the web and the
  API share one origin in an instance, so cookies and CORS need no setting;
  the demo does not change.
- Negative: two images to keep building, one more job on every run, and the
  release workflow depends on the CI of the same commit (it waits, up to
  25 minutes, and fails loudly if the images never come); the web image is
  not used by the demo, so an operator is its first user.
- Strongest argument against: publishing images invites operators the
  project cannot support. The licence and the README say so; the milestone
  adds the guide so that most questions are answered before they are asked.

## Reopen when

An operator asks for another architecture than amd64 or for another
registry, or the demo moves to the published images.

## Links

ADR 0011, ADR 0012, ADR 0013, ADR 0016; PLAN.md sections 11, 12 and 15;
issues #112, #113 and #114.
