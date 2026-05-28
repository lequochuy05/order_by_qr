-- V1.5__Add_CreatedBy_To_Transactions.sql
-- Thêm cột created_by_id để ghi nhận nhân viên tạo mã thanh toán

ALTER TABLE payment_transactions 
ADD COLUMN created_by_id BIGINT REFERENCES users(id);

-- Thêm index để tối ưu truy vấn theo nhân viên nếu cần
CREATE INDEX idx_payment_transactions_created_by ON payment_transactions(created_by_id);
