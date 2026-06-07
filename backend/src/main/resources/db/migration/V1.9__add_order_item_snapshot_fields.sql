ALTER TABLE order_item
ADD COLUMN item_name_snapshot VARCHAR(150),
ADD COLUMN item_type VARCHAR(20),
ADD COLUMN line_total DECIMAL(15,2);