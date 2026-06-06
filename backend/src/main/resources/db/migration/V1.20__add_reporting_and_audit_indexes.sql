CREATE INDEX IF NOT EXISTS idx_orders_completed_business_date
ON orders(business_date, payment_time DESC)
WHERE is_deleted = false AND status = 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_order_item_snapshot_sales
ON order_item(item_type, item_name_snapshot, created_at)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_applied_at
ON order_discounts(applied_at DESC)
WHERE is_deleted = false;
