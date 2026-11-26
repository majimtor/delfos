# syntax=docker/dockerfile:1
# The web for an operator (ADR 0018): the built files served by Caddy, which also terminates TLS
# and passes the API's paths to the API container, so that the web and the API share one origin and
# the session cookie needs no cross-origin setting. Build context: the repository root.

FROM node:26.13.0-alpine AS build
WORKDIR /src/web
COPY version.txt /src/version.txt
COPY contract/openapi.yaml /src/contract/openapi.yaml
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web ./
# An empty origin makes the client call the API on the page's own origin (web/src/api/client.ts).
ENV VITE_API_BASE_URL=""
RUN npm run build

FROM caddy:2-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /src/web/dist /srv
EXPOSE 80 443
