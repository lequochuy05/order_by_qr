ALTER TABLE orders
ADD COLUMN subtotal_amount DECIMAL(15,2),
ADD COLUMN discount_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN final_amount DECIMAL(15,2),
ADD COLUMN paid_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN business_date DATE;
