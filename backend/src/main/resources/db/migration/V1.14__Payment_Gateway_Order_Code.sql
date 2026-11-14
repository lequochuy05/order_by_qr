ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS gateway_order_code BIGINT;

UPDATE payment_transactions
SET gateway_order_code = id
WHERE gateway_order_code IS NULL
  AND payment_method = 'PAYOS';

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_gateway_order_code
    ON payment_transactions (gateway_order_code)
    WHERE gateway_order_code IS NOT NULL AND is_deleted = false;
