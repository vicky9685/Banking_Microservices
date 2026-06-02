CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    account_number  VARCHAR(32)  NOT NULL UNIQUE,
    customer_id     UUID         NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    balance         NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        CHAR(3)      NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_account_customer ON accounts(customer_id);
CREATE INDEX idx_account_status   ON accounts(status);
