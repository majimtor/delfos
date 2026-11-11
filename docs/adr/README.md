# Architecture decision records

Each record captures one decision that is expensive to reverse: the context
that forced it, the alternatives that were really on the table, what was
accepted knowingly, and the observable condition under which it is
reopened. Records are numbered in the order they were written and never
edited after acceptance except to change their status; a change of mind is a
new record that supersedes the old one.

Template: [0000-adr-template.md](0000-adr-template.md).

| Number | Title | Status |
| --- | --- | --- |
| [0001](0001-central-api-with-multiple-clients.md) | Central API with multiple clients | accepted |
| [0002](0002-backend-in-kotlin-on-spring-boot.md) | Backend in Kotlin on Spring Boot | accepted |
| [0003](0003-postgresql-with-flyway-migrations-and-schema-validation.md) | PostgreSQL with Flyway migrations and schema validation at startup | accepted |
| [0004](0004-web-client-in-react-with-vite.md) | Web client in React with Vite | accepted |
| [0005](0005-mobile-client-in-kotlin-multiplatform-android-first.md) | Mobile client in Kotlin Multiplatform, Android first | accepted |
| [0006](0006-no-desktop-client.md) | No desktop client | accepted |
| [0007](0007-api-first-openapi-contract.md) | API-first OpenAPI contract as the single source of truth | accepted |
| [0008](0008-opaque-session-tokens.md) | Opaque session tokens with cookie and bearer transports | accepted |
| [0009](0009-declarative-visibility-rules-evaluated-on-the-server.md) | Declarative visibility rules evaluated on the server | accepted |
| [0010](0010-single-repository-without-monorepo-tooling.md) | Single repository without monorepo tooling | accepted |
| [0011](0011-hosting-on-free-tiers-without-a-payment-card.md) | Hosting on free tiers without a payment card | superseded by 0016 |
| [0012](0012-grouped-dependency-updates-and-pinned-toolchains.md) | Grouped dependency updates and pinned toolchains | accepted |
| [0013](0013-github-flow-with-squash-merges-and-automated-releases.md) | GitHub Flow with squash merges and automated releases | accepted |
| [0014](0014-mit-licence.md) | MIT licence | accepted |
| [0015](0015-english-codebase-with-a-bilingual-interface.md) | English codebase with a bilingual interface | accepted |
| [0016](0016-hosting-on-free-tiers-with-the-web-on-cloudflare-workers.md) | Hosting on free tiers with the web on Cloudflare Workers | accepted |

To add a record: copy the template to `NNNN-title-in-kebab-case.md` with the
next number, fill every section, add the row above, and merge it in the same
pull request as the first change that depends on it.
