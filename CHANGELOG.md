# Changelog

## 0.1.0 (2026-11-13)


### Features

* **api:** add manager sessions with cookie and bearer transports ([#6](https://github.com/majimtor/delfos/issues/6)) ([eb42ffc](https://github.com/majimtor/delfos/commit/eb42ffcbbebbbc36379126c75c97f231098b9fff))
* **api:** add rate limiting and a request body size limit ([#11](https://github.com/majimtor/delfos/issues/11)) ([2a72c93](https://github.com/majimtor/delfos/commit/2a72c93be5e69001ba17526d476f1a0048291a9c))
* **api:** add results, comparable results and raw listings ([#9](https://github.com/majimtor/delfos/issues/9)) ([b7269d3](https://github.com/majimtor/delfos/commit/b7269d358684a37e0c4e815c45f3f73871e7dfb2))
* **api:** add the demo reset operation with the keep-alive and reset workflows ([#18](https://github.com/majimtor/delfos/issues/18)) ([b6f1996](https://github.com/majimtor/delfos/commit/b6f1996d5e60f94873cf2fa82e919b8160dc153c))
* **api:** add the demo seed ([#10](https://github.com/majimtor/delfos/issues/10)) ([c14e301](https://github.com/majimtor/delfos/commit/c14e301732b77f41e9e801f5ec18d44d54f223c6))
* **api:** add the initial schema, entities and repositories ([#5](https://github.com/majimtor/delfos/issues/5)) ([c9aa44b](https://github.com/majimtor/delfos/commit/c9aa44ba6a8be5345d281699265b412ff9496d59))
* **api:** add the public operations to read, evaluate and answer a survey ([#8](https://github.com/majimtor/delfos/issues/8)) ([c754a2a](https://github.com/majimtor/delfos/commit/c754a2aef43339c667e85aaf8c41728a05a39a31))
* **api:** add the Spring Boot skeleton generated from the contract ([#4](https://github.com/majimtor/delfos/issues/4)) ([bb479a6](https://github.com/majimtor/delfos/commit/bb479a642a9ed22a3c2f40692022de1b9e91bd41))
* **api:** add the survey aggregate with versions and drafts ([#7](https://github.com/majimtor/delfos/issues/7)) ([bad8bb1](https://github.com/majimtor/delfos/commit/bad8bb195dbd03da7e1de1669995f4044771dc89))
* **api:** export the responses of a version as CSV ([#27](https://github.com/majimtor/delfos/issues/27)) ([e6bcc1d](https://github.com/majimtor/delfos/commit/e6bcc1d97a124595506e746f2c6f0ac39d0fc3fe))
* **contract:** add the API contract and conformance suite for the first slice ([#2](https://github.com/majimtor/delfos/issues/2)) ([005811c](https://github.com/majimtor/delfos/commit/005811c028d092de50a664f9ab125d8646a52cfb))
* **seed:** add the English sample surveys and run the demo path in Spanish ([#22](https://github.com/majimtor/delfos/issues/22)) ([c5f4687](https://github.com/majimtor/delfos/commit/c5f46874bb39ac3d97c85164da83cf201c3e0183))
* **shared:** add the rules evaluator and definition validator ([#3](https://github.com/majimtor/delfos/issues/3)) ([5fdccdc](https://github.com/majimtor/delfos/commit/5fdccdc31ee9e24ca6251a5dec1817575a5abaa7))
* **web:** add the manager login and the survey list ([#15](https://github.com/majimtor/delfos/issues/15)) ([07a9e2e](https://github.com/majimtor/delfos/commit/07a9e2e5dfda756282a17dcdd4f4993db46ffa85))
* **web:** add the public answer page with server-side evaluation ([#14](https://github.com/majimtor/delfos/issues/14)) ([e3cb061](https://github.com/majimtor/delfos/commit/e3cb061c25117731122b6e6dcd82978bfd0d5b34))
* **web:** add the results page with polling ([#16](https://github.com/majimtor/delfos/issues/16)) ([e177a8d](https://github.com/majimtor/delfos/commit/e177a8d86ff606d931d022249ad3a7b2ed5815e8))
* **web:** add the survey editor for questions and options with inline validation ([#28](https://github.com/majimtor/delfos/issues/28)) ([7211492](https://github.com/majimtor/delfos/commit/7211492fe55d11c6b0bf1d48f77c0a41021a80af))
* **web:** add the web client scaffold with the wait screen and two languages ([#13](https://github.com/majimtor/delfos/issues/13)) ([bb3f0f0](https://github.com/majimtor/delfos/commit/bb3f0f017a514685bedf8842e21d8873b0e0db8f))
* **web:** audit the accessibility of the public pages and fix what it found ([#24](https://github.com/majimtor/delfos/issues/24)) ([1ebfea8](https://github.com/majimtor/delfos/commit/1ebfea8f9fbbea0589c3d4f522aa9eafc1b4fd54))
* **web:** edit the visibility rules of a draft with inline validation ([#29](https://github.com/majimtor/delfos/issues/29)) ([3aa506c](https://github.com/majimtor/delfos/commit/3aa506ca1f18a69300c4ca99bb8a357f0c950ae6))
* **web:** publish a new version of a survey and edit its title and description ([#30](https://github.com/majimtor/delfos/issues/30)) ([f9e7a21](https://github.com/majimtor/delfos/commit/f9e7a216d75c68475cd74a6bff29cbe1c82d6b51))


### Bug Fixes

* **api:** keep one rate-limit window per kind of operation and tell the real wake-up time ([#19](https://github.com/majimtor/delfos/issues/19)) ([2a108c8](https://github.com/majimtor/delfos/commit/2a108c8351ccc67dc6bcfd8301a820ca67f757bc))
* **api:** serialize the mutations of a survey and answer conflicts as 409 ([#20](https://github.com/majimtor/delfos/issues/20)) ([f9bd7ca](https://github.com/majimtor/delfos/commit/f9bd7ca2435ae43b79913eae50fae6660d16aeea))
* **web:** keep the results ETag with the data of its query ([#23](https://github.com/majimtor/delfos/issues/23)) ([2386b93](https://github.com/majimtor/delfos/commit/2386b93dd52b1b0d370a01a342b6da80971d6279))
