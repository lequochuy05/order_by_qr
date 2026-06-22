-- Active-user indexes for manager invariants and recent-user administration queries.

CREATE INDEX IF NOT EXISTS idx_users_active_status
    ON users (status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_users_active_role_status
    ON users (role, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_users_active_created_at
    ON users (created_at DESC)
    WHERE is_deleted = false;
