CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(120) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_id  UUID
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
