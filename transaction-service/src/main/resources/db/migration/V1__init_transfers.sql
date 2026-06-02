CREATE TABLE transfers (
    id                UUID PRIMARY KEY,
    from_account_id   UUID NOT NULL,
    to_account_id     UUID NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    currency          CHAR(3) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    failure_reason    VARCHAR(500),
    idempotency_key   VARCHAR(100) UNIQUE,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_transfer_status ON transfers(status);
CREATE INDEX idx_transfer_from   ON transfers(from_account_id);
CREATE INDEX idx_transfer_to     ON transfers(to_account_id);

CREATE TABLE outbox (
    id            UUID PRIMARY KEY,
    topic         VARCHAR(200) NOT NULL,
    aggregate_id  VARCHAR(200) NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    payload       TEXT NOT NULL,
    processed     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL,
    processed_at  TIMESTAMPTZ,
    attempts      INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_processed ON outbox(processed, created_at);
