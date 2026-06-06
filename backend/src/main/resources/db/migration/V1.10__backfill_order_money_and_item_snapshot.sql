UPDATE orders
SET subtotal_amount = COALESCE(original_total, total_amount, 0),
    discount_amount = COALESCE(discount_voucher, 0),
    final_amount = COALESCE(total_amount, 0),
    paid_amount = CASE WHEN payment_status = 'PAID' THEN COALESCE(total_amount, 0) ELSE 0 END,
    business_date = DATE(COALESCE(payment_time, created_at, CURRENT_TIMESTAMP));
    
UPDATE order_item oi
SET item_name_snapshot = mi.name,
    item_type = 'MENU_ITEM',
    line_total = oi.unit_price * oi.quantity
FROM menu_item mi
WHERE oi.menu_item_id = mi.id;

UPDATE order_item oi
SET item_name_snapshot = c.name,
    item_type = 'COMBO',
    line_total = oi.unit_price * oi.quantity
FROM combos c
WHERE oi.combo_id = c.id;

UPDATE order_item
SET line_total = COALESCE(unit_price, 0) * quantity
WHERE line_total IS NULL;
