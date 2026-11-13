-- Invited mode: one row per invitation, with the token stored only as a hash, and the invitation a
-- response was submitted with. An invitation is used at most once until multiple responses per
-- invitation are offered.
CREATE TABLE invitation (
    id         uuid        PRIMARY KEY,
    survey_id  uuid        NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    token_hash text        NOT NULL UNIQUE,
    status     text        NOT NULL CHECK (status IN ('PENDING', 'USED', 'REVOKED')),
    created_at timestamptz NOT NULL,
    used_at    timestamptz,
    revoked_at timestamptz
);

CREATE INDEX invitation_survey_created_idx ON invitation (survey_id, created_at DESC, id);

ALTER TABLE response
    ADD CONSTRAINT response_invitation_fk FOREIGN KEY (invitation_id) REFERENCES invitation (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX response_invitation_unique_idx ON response (invitation_id) WHERE invitation_id IS NOT NULL;
