-- Tối ưu truy vấn Dashboard (Index Only Scan)
CREATE INDEX idx_orders_completed_revenue 
ON orders (payment_time) 
INCLUDE (total_amount) 
WHERE status = 'COMPLETED' AND is_deleted = false;