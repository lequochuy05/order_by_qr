ALTER TABLE orders
ALTER COLUMN subtotal_amount SET NOT NULL,
ALTER COLUMN discount_amount SET NOT NULL,
ALTER COLUMN final_amount SET NOT NULL,
ALTER COLUMN paid_amount SET NOT NULL,
ALTER COLUMN business_date SET NOT NULL;

ALTER TABLE orders
ADD CONSTRAINT chk_orders_subtotal_amount_non_negative CHECK (subtotal_amount >= 0),
ADD CONSTRAINT chk_orders_discount_amount_non_negative CHECK (discount_amount >= 0),
ADD CONSTRAINT chk_orders_final_amount_non_negative CHECK (final_amount >= 0),
ADD CONSTRAINT chk_orders_paid_amount_non_negative CHECK (paid_amount >= 0);

ALTER TABLE order_item
ALTER COLUMN item_name_snapshot SET NOT NULL,
ALTER COLUMN item_type SET NOT NULL,
ALTER COLUMN line_total SET NOT NULL;

ALTER TABLE order_item
ADD CONSTRAINT chk_order_item_product_xor
CHECK (
  (menu_item_id IS NOT NULL AND combo_id IS NULL)
  OR
  (menu_item_id IS NULL AND combo_id IS NOT NULL)
);

ALTER TABLE order_item
ADD CONSTRAINT chk_order_item_item_type
CHECK (item_type IN ('MENU_ITEM', 'COMBO'));

ALTER TABLE order_item
ADD CONSTRAINT chk_order_item_line_total_non_negative
CHECK (line_total >= 0);
