-- =============================================
-- V1.7 - Performance Indexes for Kitchen & History
-- =============================================

-- C1: Composite index for Kitchen Orders query (findByStatusIn: PENDING, SERVING)
-- Covers the most frequent real-time query in the system
CREATE INDEX IF NOT EXISTS idx_orders_status_created
    ON orders (status, created_at)
    WHERE is_deleted = false;

-- C2: Composite index for Order History filter with table joins
-- Optimizes the admin order history page with status + date range + table_id filtering
CREATE INDEX IF NOT EXISTS idx_orders_history_filter
    ON orders (table_id, status, created_at DESC)
    WHERE is_deleted = false;

-- C3: Partial index for payment status reconciliation
-- Speeds up payOrder() and confirmPaid() lookups on payment_status
CREATE INDEX IF NOT EXISTS idx_orders_payment_status
    ON orders (status, payment_status)
    WHERE is_deleted = false AND payment_status = 'PENDING';
