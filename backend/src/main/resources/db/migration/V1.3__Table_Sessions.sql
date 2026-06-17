-- V1.2__Table_Sessions.sql
-- Adds table-scoped customer sessions for static QR ordering.

CREATE TABLE table_sessions (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    closed_reason VARCHAR(255),
    created_source VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE table_session_tokens (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITHOUT TIME ZONE,
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE orders
    ADD COLUMN table_session_id BIGINT;

ALTER TABLE table_sessions
    ADD CONSTRAINT fk_table_sessions_table
    FOREIGN KEY (table_id) REFERENCES tables(id);

ALTER TABLE table_session_tokens
    ADD CONSTRAINT fk_table_session_tokens_session
    FOREIGN KEY (session_id) REFERENCES table_sessions(id) ON DELETE CASCADE;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_table_session
    FOREIGN KEY (table_session_id) REFERENCES table_sessions(id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_table_sessions_one_open_per_table
    ON table_sessions (table_id)
    WHERE status = 'OPEN' AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_table_session_tokens_hash_active
    ON table_session_tokens (token_hash)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_sessions_table_status
    ON table_sessions (table_id, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_sessions_last_activity
    ON table_sessions (last_activity_at)
    WHERE status = 'OPEN' AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_session_tokens_session_id
    ON table_session_tokens (session_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_table_session_id
    ON orders (table_session_id)
    WHERE is_deleted = false;
