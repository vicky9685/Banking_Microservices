CREATE TABLE audit_log (
    id             UUID PRIMARY KEY,
    sequence       BIGINT NOT NULL UNIQUE,
    at             TIMESTAMPTZ NOT NULL,
    actor_id       VARCHAR(100),
    actor_roles    VARCHAR(500),
    request_id     VARCHAR(100),
    action         VARCHAR(80) NOT NULL,
    resource_type  VARCHAR(60),
    resource_id    VARCHAR(100),
    details        TEXT,
    prev_hash      VARCHAR(64),
    row_hash       VARCHAR(64) NOT NULL,
    service_name   VARCHAR(80) NOT NULL
);
CREATE INDEX idx_audit_actor    ON audit_log(actor_id);
CREATE INDEX idx_audit_resource ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_at       ON audit_log(at);
