CREATE INDEX IF NOT EXISTS idx_payment_transactions_pending_expires
    ON payment_transactions (status, expires_at)
    WHERE is_deleted = false;
