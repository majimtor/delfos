# Contract changelog

Changes to `openapi.yaml` and to the conformance suite. Until the first
release, breaking changes are allowed and recorded here.

## Unreleased

### Added

- Initial contract for the first slice: health, manager sessions with cookie
  and bearer transports, surveys with lifecycle, draft definitions, versions,
  results per version and across comparable questions, raw responses and
  answers, and the public operations to read, evaluate and answer a survey.
- Problem Details error shape with stable `code` and field errors.
- Conformance suite for the visibility rules and the definition validator.
- `POST /demo/reset`, protected by a shared token, applies the demo seed again;
  the public demo calls it once a day.

### Changed

- `Question` and `Option` are explicit schemas with required keys instead of
  `allOf` compositions, which client generators rendered as impossible types.
- `createSession` documents that the cookie transport requires the
  `X-Delfos-Client` header and that unknown usernames and wrong passwords
  answer the same code.
