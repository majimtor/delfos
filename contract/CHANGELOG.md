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
- `GET /surveys/{id}/versions/{n}/export.csv` exports the responses of a
  version as a streamed CSV file: one row per response, one column per
  question, one column per option for multiple choice, text protected against
  formula injection.

- Invited mode: `POST`, `GET /surveys/{id}/invitations` and
  `DELETE /surveys/{id}/invitations/{invitationId}` create, list and revoke
  single-use invitations; the public operations take the `invitation` query
  parameter and answer `INVITATION_REQUIRED`, `INVITATION_INVALID` or
  `INVITATION_USED`; `PublicSurvey` carries `respondentMode`.

- Accounts for administrators: `GET`, `POST /accounts` and
  `POST /accounts/{accountId}/disable`, with the codes `ADMIN_REQUIRED` and
  `USERNAME_TAKEN`.

- `GET /surveys/{id}/versions/{n}/export.xlsx` exports the responses of a
  version as a workbook with the rows and columns of the CSV export.

- `GET /surveys/{id}/results/events`: server-sent events with the response
  count of a survey, on connection and on every new response, with a
  heartbeat between them.

### Changed

- `Question` and `Option` are explicit schemas with required keys instead of
  `allOf` compositions, which client generators rendered as impossible types.
- `createSession` documents that the cookie transport requires the
  `X-Delfos-Client` header and that unknown usernames and wrong passwords
  answer the same code.
