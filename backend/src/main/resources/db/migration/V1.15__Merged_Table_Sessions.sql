-- V1.15__Merged_Table_Sessions.sql
-- Adds physical-table memberships for one shared table session/order.

ALTER TABLE table_sessions
    ADD COLUMN IF NOT EXISTS guest_count INTEGER,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_table_sessions_created_by_user') THEN
        ALTER TABLE table_sessions
            ADD CONSTRAINT fk_table_sessions_created_by_user
            FOREIGN KEY (created_by_user_id) REFERENCES users(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_table_sessions_guest_count_positive') THEN
        ALTER TABLE table_sessions
            ADD CONSTRAINT chk_table_sessions_guest_count_positive
            CHECK (guest_count IS NULL OR guest_count >= 1);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS table_session_tables (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    released_at TIMESTAMP WITHOUT TIME ZONE,
    joined_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_table_session_tables_session') THEN
        ALTER TABLE table_session_tables
            ADD CONSTRAINT fk_table_session_tables_session
            FOREIGN KEY (session_id) REFERENCES table_sessions(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_table_session_tables_table') THEN
        ALTER TABLE table_session_tables
            ADD CONSTRAINT fk_table_session_tables_table
            FOREIGN KEY (table_id) REFERENCES tables(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_table_session_tables_joined_by_user') THEN
        ALTER TABLE table_session_tables
            ADD CONSTRAINT fk_table_session_tables_joined_by_user
            FOREIGN KEY (joined_by_user_id) REFERENCES users(id);
    END IF;
END
$$;

-- Production guard: legacy deployments may have lost the old partial unique
-- index or accumulated duplicate OPEN sessions before it was introduced.
-- Keep the most recently active session and expire the rest before membership
-- rows and their unique indexes are created.
WITH ranked_open_sessions AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY table_id
            ORDER BY last_activity_at DESC NULLS LAST, opened_at DESC NULLS LAST, id DESC
        ) AS row_number
    FROM table_sessions
    WHERE status = 'OPEN'
      AND is_deleted = false
)
UPDATE table_sessions session
SET status = 'EXPIRED',
    closed_at = COALESCE(session.closed_at, NOW()),
    closed_reason = COALESCE(session.closed_reason, 'Deduplicated by merged-table migration'),
    version = session.version + 1,
    updated_at = NOW()
FROM ranked_open_sessions ranked
WHERE session.id = ranked.id
  AND ranked.row_number > 1;

UPDATE table_session_tokens token
SET revoked_at = COALESCE(token.revoked_at, NOW()),
    updated_at = NOW()
FROM table_sessions session
WHERE token.session_id = session.id
  AND session.closed_reason = 'Deduplicated by merged-table migration'
  AND token.revoked_at IS NULL
  AND token.is_deleted = false;

UPDATE orders order_to_detach
SET table_session_id = NULL,
    updated_at = NOW()
FROM table_sessions session
WHERE order_to_detach.table_session_id = session.id
  AND session.closed_reason = 'Deduplicated by merged-table migration'
  AND order_to_detach.status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT')
  AND order_to_detach.is_deleted = false;

INSERT INTO table_session_tables (
    session_id,
    table_id,
    is_primary,
    joined_at,
    released_at,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    s.id,
    s.table_id,
    TRUE,
    COALESCE(s.opened_at, s.created_at, NOW()),
    CASE
        WHEN s.status = 'OPEN' THEN NULL
        ELSE COALESCE(s.closed_at, s.updated_at, s.created_at, s.opened_at, NOW())
    END,
    s.created_at,
    s.updated_at,
    s.is_deleted
FROM table_sessions s
WHERE s.table_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM table_session_tables tst
      WHERE tst.session_id = s.id
        AND tst.table_id = s.table_id
        AND tst.is_deleted = false
  );

-- If dirty data already points multiple active orders at one session, preserve
-- all orders but detach all except the newest. The table-scoped fallback can
-- still surface those orders without violating the new session invariant.
WITH ranked_session_orders AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY table_session_id
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_number
    FROM orders
    WHERE table_session_id IS NOT NULL
      AND order_type = 'DINE_IN'
      AND status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT')
      AND is_deleted = false
)
UPDATE orders order_to_detach
SET table_session_id = NULL,
    updated_at = NOW()
FROM ranked_session_orders ranked
WHERE order_to_detach.id = ranked.id
  AND ranked.row_number > 1;

-- Attach at most one legacy table-scoped active order to each OPEN session.
-- Prefer the newest order, matching the legacy read path.
WITH ranked_unassigned_orders AS (
    SELECT
        id,
        table_id,
        ROW_NUMBER() OVER (
            PARTITION BY table_id
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_number
    FROM orders
    WHERE table_session_id IS NULL
      AND order_type = 'DINE_IN'
      AND status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT')
      AND is_deleted = false
)
UPDATE orders order_to_attach
SET table_session_id = session.id,
    updated_at = NOW()
FROM ranked_unassigned_orders ranked
JOIN table_sessions session
  ON session.table_id = ranked.table_id
 AND session.status = 'OPEN'
 AND session.is_deleted = false
WHERE order_to_attach.id = ranked.id
  AND ranked.row_number = 1
  AND NOT EXISTS (
      SELECT 1
      FROM orders existing_order
      WHERE existing_order.table_session_id = session.id
        AND existing_order.order_type = 'DINE_IN'
        AND existing_order.status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT')
        AND existing_order.is_deleted = false
  );

-- Defensive cleanup for a partially prepared/manual schema: retain one row
-- per session/table, one active membership per table, and one active primary.
WITH duplicate_memberships AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY session_id, table_id
            ORDER BY released_at NULLS FIRST, joined_at DESC, id DESC
        ) AS row_number
    FROM table_session_tables
    WHERE is_deleted = false
)
UPDATE table_session_tables membership
SET is_deleted = true,
    version = membership.version + 1,
    updated_at = NOW()
FROM duplicate_memberships duplicate
WHERE membership.id = duplicate.id
  AND duplicate.row_number > 1;

WITH duplicate_active_tables AS (
    SELECT
        membership.id,
        ROW_NUMBER() OVER (
            PARTITION BY membership.table_id
            ORDER BY
                (session.status = 'OPEN') DESC,
                membership.is_primary DESC,
                membership.joined_at DESC,
                membership.id DESC
        ) AS row_number
    FROM table_session_tables membership
    JOIN table_sessions session ON session.id = membership.session_id
    WHERE membership.released_at IS NULL
      AND membership.is_deleted = false
)
UPDATE table_session_tables membership
SET released_at = NOW(),
    version = membership.version + 1,
    updated_at = NOW()
FROM duplicate_active_tables duplicate
WHERE membership.id = duplicate.id
  AND duplicate.row_number > 1;

WITH duplicate_active_primaries AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY session_id
            ORDER BY joined_at ASC, id ASC
        ) AS row_number
    FROM table_session_tables
    WHERE is_primary = true
      AND released_at IS NULL
      AND is_deleted = false
)
UPDATE table_session_tables membership
SET is_primary = false,
    version = membership.version + 1,
    updated_at = NOW()
FROM duplicate_active_primaries duplicate
WHERE membership.id = duplicate.id
  AND duplicate.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_table_session_tables_session_table
    ON table_session_tables (session_id, table_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_table_session_tables_one_active_per_table
    ON table_session_tables (table_id)
    WHERE released_at IS NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_table_session_tables_one_active_primary_per_session
    ON table_session_tables (session_id)
    WHERE is_primary = true AND released_at IS NULL AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_session_tables_session_active
    ON table_session_tables (session_id, released_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_session_tables_table_active
    ON table_session_tables (table_id, released_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_table_session_tables_table_history
    ON table_session_tables (table_id, joined_at)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_one_active_per_session
    ON orders (table_session_id)
    WHERE is_deleted = false
      AND table_session_id IS NOT NULL
      AND order_type = 'DINE_IN'
      AND status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT');
