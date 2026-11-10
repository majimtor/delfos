-- Initial schema of Delfos. Enumerations are code; the database stores their names and checks them.
-- Cascading deletes exist only inside an aggregate: survey -> versions -> questions -> options,
-- response -> answers. Timestamps are stored with time zone and written in UTC.

CREATE TABLE account (
    id            uuid        PRIMARY KEY,
    username      text        NOT NULL,
    password_hash text        NOT NULL,
    is_admin      boolean     NOT NULL DEFAULT false,
    status        text        NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);

CREATE UNIQUE INDEX account_username_key ON account (lower(username));

CREATE TABLE session (
    id           uuid        PRIMARY KEY,
    account_id   uuid        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    token_hash   text        NOT NULL UNIQUE,
    created_at   timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    expires_at   timestamptz NOT NULL,
    revoked_at   timestamptz
);

CREATE INDEX session_account_idx ON session (account_id);

CREATE TABLE survey (
    id                                uuid        PRIMARY KEY,
    public_id                         text        NOT NULL UNIQUE,
    owner_id                          uuid        NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    title                             text        NOT NULL,
    description                       text,
    language                          text        NOT NULL CHECK (language IN ('es', 'en')),
    status                            text        NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    respondent_mode                   text        NOT NULL CHECK (respondent_mode IN ('ANONYMOUS', 'INVITED')),
    multiple_responses_per_invitation boolean     NOT NULL DEFAULT false,
    closes_at                         timestamptz,
    max_responses                     integer     CHECK (max_responses >= 1),
    created_at                        timestamptz NOT NULL,
    updated_at                        timestamptz NOT NULL
);

CREATE INDEX survey_owner_updated_idx ON survey (owner_id, updated_at DESC, id);

CREATE TABLE survey_version (
    id           uuid        PRIMARY KEY,
    survey_id    uuid        NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    number       integer     NOT NULL CHECK (number >= 1),
    status       text        NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED')),
    rules        jsonb       NOT NULL DEFAULT '[]'::jsonb,
    created_at   timestamptz NOT NULL,
    published_at timestamptz,
    UNIQUE (survey_id, number)
);

-- At most one draft and one published version per survey, enforced by the database.
CREATE UNIQUE INDEX survey_version_one_draft_idx ON survey_version (survey_id) WHERE status = 'DRAFT';
CREATE UNIQUE INDEX survey_version_one_published_idx ON survey_version (survey_id) WHERE status = 'PUBLISHED';

CREATE TABLE question (
    id         uuid    PRIMARY KEY,
    version_id uuid    NOT NULL REFERENCES survey_version (id) ON DELETE CASCADE,
    key        text    NOT NULL,
    position   integer NOT NULL CHECK (position >= 0),
    type       text    NOT NULL CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TEXT', 'NUMBER', 'SCALE')),
    text       text    NOT NULL,
    required   boolean NOT NULL DEFAULT false,
    config     jsonb   NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (version_id, position),
    UNIQUE (version_id, key)
);

CREATE TABLE question_option (
    id          uuid    PRIMARY KEY,
    question_id uuid    NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    key         text    NOT NULL,
    position    integer NOT NULL CHECK (position >= 0),
    label       text    NOT NULL,
    UNIQUE (question_id, position),
    UNIQUE (question_id, key)
);

CREATE TABLE response (
    id            uuid        PRIMARY KEY,
    version_id    uuid        NOT NULL REFERENCES survey_version (id) ON DELETE CASCADE,
    invitation_id uuid,
    submitted_at  timestamptz NOT NULL
);

CREATE INDEX response_version_submitted_idx ON response (version_id, submitted_at DESC, id);

-- One typed row per answered question; aggregates are computed here in SQL.
CREATE TABLE answer (
    response_id  uuid    NOT NULL REFERENCES response (id) ON DELETE CASCADE,
    question_key text    NOT NULL,
    type         text    NOT NULL CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TEXT', 'NUMBER', 'SCALE')),
    option_keys  text[],
    number_value numeric,
    text_value   text,
    PRIMARY KEY (response_id, question_key)
);
