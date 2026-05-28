-- Bảng order_item
CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_menu_item_id ON order_item (menu_item_id);
CREATE INDEX idx_order_item_combo_id ON order_item (combo_id);

-- Bảng orders
CREATE INDEX idx_orders_table_id ON orders (table_id);
CREATE INDEX idx_orders_paid_by ON orders (paid_by);

-- Bảng menu_item & category
CREATE INDEX idx_menu_item_cate_id ON menu_item (cate_id);

-- Bảng combo_items
CREATE INDEX idx_combo_items_combo_id ON combo_items (combo_id);
CREATE INDEX idx_combo_items_menu_item_id ON combo_items (menu_item_id);

-- Bảng item_option & item_option_values
CREATE INDEX idx_item_option_menu_item_id ON item_option (menu_item_id);
CREATE INDEX idx_item_option_values_option_id ON item_option_values (item_option_id);

-- Bảng order_item_options (Bao gồm Index chiều thuận và nghịch)
CREATE INDEX idx_order_item_options_item_id ON order_item_options (order_item_id);
CREATE INDEX idx_order_item_options_value_id ON order_item_options (item_option_value_id);

-- Bảng payment_transactions
CREATE INDEX idx_payment_transactions_order_id ON payment_transactions (order_id);