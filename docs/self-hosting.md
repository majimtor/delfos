# Running Delfos yourself

Delfos runs on one machine of your own from the images each release
publishes: the web, served by Caddy with a certificate of its own, the API
and a PostgreSQL database, started together with Docker Compose. It is free
under the MIT licence and comes without a guarantee of support; questions
and problems go to the issues of the repository, where they are read when
the maintainer has time. The public demo is a separate thing: nothing here
touches it, and nothing of it reaches your instance.

## What you need

- A machine with Docker Engine and the Compose plugin, with at least 1 GB
  of memory free: the API uses about 130 MB at rest, the database and the
  edge the rest.
- A domain name that points at the machine, with ports 80 and 443 open,
  for a certificate issued by itself. For a try on the machine itself,
  plain HTTP on port 80 works with the site address `:80`.
- The two files of `deploy/self-hosting/` in the repository at the version
  you run: `compose.yaml` and `.env.example`, plus `smoke.sh` if you want
  the check.

## The first start

1. Put `compose.yaml` and `.env.example` in a directory of their own and
   copy the example to `.env`.
2. Fill `.env` in: the site address; the version, as the releases page
   names it; a password for the database, chosen once; the username and
   the password of the first administrator, which follow the rules of an
   account (3 to 64 lowercase letters, digits, dots, hyphens or
   underscores; 12 to 256 characters).
3. Start the instance:

   ```sh
   docker compose up -d
   ```

   The first start takes about a minute: the API prepares the database and
   creates the administrator, and until it is ready the edge answers 502.
4. Check it, if you like, with the smoke script:

   ```sh
   ./smoke.sh https://delfos.example.org admin 'the password'
   ```

   It waits for the API, looks at the web's headers, logs the
   administrator in and lists the surveys, none yet.
5. Open the site, log in and create the managers under Accounts. Each
   manager builds and publishes their own surveys; an administrator
   manages accounts and nothing else.

The two administrator variables matter only at a start with no account at
all. Once the administrator exists they do nothing, and they can leave
`.env`; a later change of them changes nothing.

## The variables

| Variable | Read by | Meaning | Default |
| --- | --- | --- | --- |
| `DELFOS_SITE_ADDRESS` | the web | The address Caddy serves: a domain for a certificate of its own, or `:80` for plain HTTP | none, required |
| `DELFOS_VERSION` | compose | The version of both images, as the releases page names it | `latest`, which moves |
| `DELFOS_DB_PASSWORD` | compose | The password of the database, for the API and the database alike | none, required |
| `DELFOS_ADMIN_USERNAME` | the API | The first administrator, applied while the instance has no account | empty |
| `DELFOS_ADMIN_PASSWORD` | the API | Its password | empty |
| `DELFOS_API_UPSTREAM` | the web | Where the edge passes the API's paths | `api:8080`, set by compose |

The API reads a few more, set in `compose.yaml` if you need them:
`DELFOS_LIMIT_PUBLIC_PER_MINUTE` (120 requests per minute and address on
the public operations), `DELFOS_LIMIT_LOGIN_PER_MINUTE` (10 login attempts
per minute and address), `DELFOS_LIMIT_MAX_BODY_BYTES` (262144), and
`PORT` (8080). `DELFOS_SEED_ENABLED` loads the demo data and resets it at
every start: it stays off in an instance, and so does the demo reset,
which does not exist without `DELFOS_SEED_RESET_TOKEN`. `DELFOS_WEB_ORIGIN`
allows a web on another origin and is not needed here, where the web and
the API share one.

## Backups

Everything of an instance is in the database. A backup is a dump of it:

```sh
docker compose exec -T db pg_dump -U delfos delfos > "delfos-$(date +%F).sql"
```

To restore it into an empty instance, start the database alone, load the
dump, then start the rest:

```sh
docker compose up -d db
docker compose exec -T db psql -U delfos delfos < delfos-2026-11-26.sql
docker compose up -d
```

Keep `.env` with the dump: the database password and the site address are
there. The certificates live in the `caddy-data` volume and are issued
again if it is lost.

## Upgrades

Back up, change `DELFOS_VERSION` in `.env` to the new version, then:

```sh
docker compose pull
docker compose up -d
```

The API applies the changes of the database schema at start, so an upgrade
cannot be undone by starting the previous version again: going back is
restoring the backup with the previous version. The changelog of the
repository says what each version changed; a version with a breaking change
in the API says so there.

## What an instance stores

The database holds the accounts (username, a hash of the password, whether
an administrator, status), the sessions (a hash of the token, when created,
last seen and when it expires: thirty minutes idle, twelve hours at most),
the surveys with their versions, questions, options and rules, the
invitations (a hash of the token, status and times) and the responses: the
version answered, the invitation if any, the time of submission and the
answers themselves. A response carries no address, no browser and no
account; a text answer holds whatever the respondent typed, which is what
the manager asked for. The limits by address live in memory and are not
stored; neither the edge nor the API keeps an access log with addresses.

Deleting a survey removes its versions, invitations and responses. An
account that owns surveys is disabled, not deleted, so that its surveys
keep an owner. An export is a copy a manager downloads; what they do with
it is theirs.

If you keep a record of processing, this is it: you decide what the surveys
ask and who answers them; Delfos stores the answers for you, on your
machine, until you delete the survey or the instance.

## Resetting an instance

To start again from nothing, certificates included:

```sh
docker compose down --volumes
```

## Trying it from the source

`compose.ci.yaml` next to `compose.yaml` builds both images from a checkout
of the repository instead of pulling them; it is what the CI workflow runs
on every change:

```sh
docker compose -f compose.yaml -f compose.ci.yaml up -d --build
```
