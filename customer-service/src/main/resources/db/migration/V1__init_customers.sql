CREATE TABLE customers (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(30)  NOT NULL,
    national_id     VARCHAR(50)  NOT NULL UNIQUE,
    date_of_birth   DATE,
    kyc_status      VARCHAR(20)  NOT NULL,
    line1           VARCHAR(255),
    line2           VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(100),
    postal_code     VARCHAR(20),
    country         VARCHAR(3),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_customer_email       ON customers(email);
CREATE INDEX idx_customer_national_id ON customers(national_id);
CREATE INDEX idx_customer_kyc_status  ON customers(kyc_status);
