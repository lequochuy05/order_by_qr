CREATE TABLE idempotency_requests (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(100) NOT NULL,
    request_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_json TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT chk_idempotency_requests_status
        CHECK (status IN ('PROCESSING', 'SUCCEEDED'))
);

CREATE UNIQUE INDEX ux_idempotency_requests_namespace_key
    ON idempotency_requests (namespace, request_key);

CREATE INDEX idx_idempotency_requests_expires_at
    ON idempotency_requests (expires_at);
