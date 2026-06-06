DROP INDEX IF EXISTS idx_orders_completed_revenue;

ALTER TABLE orders
DROP COLUMN IF EXISTS original_total,
DROP COLUMN IF EXISTS discount_voucher,
DROP COLUMN IF EXISTS total_amount;

ALTER TABLE payment_transactions
DROP COLUMN IF EXISTS payos_reference,
DROP COLUMN IF EXISTS cancel_reason;
