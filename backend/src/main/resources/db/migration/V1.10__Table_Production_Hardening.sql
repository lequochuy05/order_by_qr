ALTER TABLE tables
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_tables_status_active
ON tables (status)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_session_tokens_active_session_issued
ON table_session_tokens (session_id, issued_at)
WHERE revoked_at IS NULL AND is_deleted = false;
