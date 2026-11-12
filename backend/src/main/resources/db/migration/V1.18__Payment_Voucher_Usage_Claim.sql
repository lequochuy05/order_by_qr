-- Persist whether a payment transaction has claimed its voucher usage.
-- This keeps webhook retries idempotent after a successful local settlement.

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS voucher_usage_claimed BOOLEAN NOT NULL DEFAULT false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_discounts_order_code_active
    ON order_discounts (order_id, lower(code_snapshot))
    WHERE is_deleted = false;
