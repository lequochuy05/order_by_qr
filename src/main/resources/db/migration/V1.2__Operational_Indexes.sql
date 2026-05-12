-- Tối ưu luồng quét QR và kiểm tra bàn (chỉ quan tâm đơn chưa xóa)
CREATE INDEX idx_orders_active_table ON orders (table_id, status) WHERE is_deleted = false;

-- Tối ưu luồng check mã giảm giá
CREATE INDEX idx_vouchers_active_code ON vouchers (code, active) WHERE is_deleted = false;