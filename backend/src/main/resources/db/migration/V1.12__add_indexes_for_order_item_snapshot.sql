CREATE INDEX IF NOT EXISTS idx_order_item_order_status
ON order_item(order_id, status)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_business_date_status
ON orders(business_date, status)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_business_date_payment_status
ON orders(business_date, payment_status)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_type_created
ON order_item(item_type, created_at)
WHERE is_deleted = false;
