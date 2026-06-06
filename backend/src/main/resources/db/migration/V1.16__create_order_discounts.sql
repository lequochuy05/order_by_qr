CREATE TABLE order_discounts (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    voucher_id BIGINT REFERENCES vouchers(id),
    code_snapshot VARCHAR(50),
    discount_type_snapshot VARCHAR(20),
    discount_percent_snapshot DOUBLE PRECISION,
    discount_amount_snapshot DECIMAL(15, 2),
    applied_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (applied_amount >= 0),
    applied_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_order_discounts_order_id
ON order_discounts(order_id)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_voucher_id
ON order_discounts(voucher_id)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_code_snapshot
ON order_discounts(code_snapshot)
WHERE is_deleted = false;
