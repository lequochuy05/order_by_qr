-- V1.23__Table_Service_Requests.sql
-- Adds table_service_requests for customer call-staff requests and staff single-claim queue.

CREATE TABLE IF NOT EXISTS table_service_requests (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    table_id BIGINT NOT NULL,
    table_session_id BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    client_request_id VARCHAR(64) NOT NULL,
    claimed_by_user_id BIGINT,
    claimed_at TIMESTAMP WITHOUT TIME ZONE,
    resolved_by_user_id BIGINT,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_by_type VARCHAR(20),
    cancelled_reason VARCHAR(200),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsr_table') THEN
        ALTER TABLE table_service_requests
            ADD CONSTRAINT fk_tsr_table
            FOREIGN KEY (table_id) REFERENCES tables(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsr_session') THEN
        ALTER TABLE table_service_requests
            ADD CONSTRAINT fk_tsr_session
            FOREIGN KEY (table_session_id) REFERENCES table_sessions(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsr_claimed_by_user') THEN
        ALTER TABLE table_service_requests
            ADD CONSTRAINT fk_tsr_claimed_by_user
            FOREIGN KEY (claimed_by_user_id) REFERENCES users(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsr_resolved_by_user') THEN
        ALTER TABLE table_service_requests
            ADD CONSTRAINT fk_tsr_resolved_by_user
            FOREIGN KEY (resolved_by_user_id) REFERENCES users(id);
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tsr_active_table_reason
    ON table_service_requests (table_id, reason)
    WHERE status IN ('PENDING', 'CLAIMED') AND is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tsr_session_client_request
    ON table_service_requests (table_session_id, client_request_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_tsr_status_created
    ON table_service_requests (status, created_at ASC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_tsr_session_created
    ON table_service_requests (table_session_id, created_at DESC)
    WHERE is_deleted = FALSE;
