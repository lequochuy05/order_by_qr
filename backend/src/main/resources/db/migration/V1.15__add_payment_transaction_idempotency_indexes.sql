CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_idempotency_key
ON payment_transactions(idempotency_key)
WHERE idempotency_key IS NOT NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_external_reference
ON payment_transactions(external_reference)
WHERE external_reference IS NOT NULL AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_created
ON payment_transactions(order_id, created_at DESC)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status_created
ON payment_transactions(status, created_at)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_business_date_status
ON payment_transactions(business_date, status)
WHERE is_deleted = false;
