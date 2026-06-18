-- V1.1__FK_Indexes.sql
-- QROS - Foreign keys, unique indexes, search indexes and performance indexes.
-- Chạy sau V1__Initial.sql trên database mới.

-- =========================
-- 1. Extensions
-- =========================
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

-- =========================
-- 2. Foreign keys
-- =========================
ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE menu_item
    ADD CONSTRAINT fk_menu_item_category
    FOREIGN KEY (cate_id) REFERENCES category(id);

ALTER TABLE item_option
    ADD CONSTRAINT fk_item_option_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id);

ALTER TABLE item_option_values
    ADD CONSTRAINT fk_item_option_values_item_option
    FOREIGN KEY (item_option_id) REFERENCES item_option(id);

ALTER TABLE combo_items
    ADD CONSTRAINT fk_combo_items_combo
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    ADD CONSTRAINT fk_combo_items_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id);

ALTER TABLE promotion_days
    ADD CONSTRAINT fk_promotion_days_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_table
    FOREIGN KEY (table_id) REFERENCES tables(id),
    ADD CONSTRAINT fk_orders_paid_by
    FOREIGN KEY (paid_by) REFERENCES users(id);

ALTER TABLE order_batches
    ADD CONSTRAINT fk_order_batches_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT fk_order_item_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id),
    ADD CONSTRAINT fk_order_item_combo
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    ADD CONSTRAINT fk_order_item_batch
    FOREIGN KEY (batch_id) REFERENCES order_batches(id);

ALTER TABLE order_item_options
    ADD CONSTRAINT fk_order_item_options_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    ADD CONSTRAINT fk_order_item_options_item_option_value
    FOREIGN KEY (item_option_value_id) REFERENCES item_option_values(id);

ALTER TABLE payment_transactions
    ADD CONSTRAINT fk_payment_transactions_order
    FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT fk_payment_transactions_created_by
    FOREIGN KEY (created_by_id) REFERENCES users(id);

ALTER TABLE order_discounts
    ADD CONSTRAINT fk_order_discounts_order
    FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT fk_order_discounts_voucher
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id);

ALTER TABLE order_status_history
    ADD CONSTRAINT fk_order_status_history_order
    FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT fk_order_status_history_changed_by
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE order_item_status_history
    ADD CONSTRAINT fk_order_item_status_history_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    ADD CONSTRAINT fk_order_item_status_history_changed_by
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE recipe_items
    ADD CONSTRAINT fk_recipe_items_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id),
    ADD CONSTRAINT fk_recipe_items_inventory_item
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_inventory_item
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    ADD CONSTRAINT fk_stock_movements_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_item(id);

ALTER TABLE order_item_inventory_reservations
    ADD CONSTRAINT fk_order_item_inventory_reservations_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    ADD CONSTRAINT fk_order_item_inventory_reservations_inventory_item
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);

-- =========================
-- 3. Soft-delete aware unique indexes
-- =========================
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_active
    ON users (LOWER(email))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone_active
    ON users (phone)
    WHERE phone IS NOT NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_active
    ON category (LOWER(name))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_menu_item_name_active
    ON menu_item (LOWER(name))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_number_active
    ON tables (LOWER(table_number))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_code_active
    ON tables (LOWER(table_code))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_qr_code_url_active
    ON tables (qr_code_url)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_qr_code_public_id_active
    ON tables (qr_code_public_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_combos_name_active
    ON combos (LOWER(name))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vouchers_code_active
    ON vouchers (LOWER(code))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_promotion_days
    ON promotion_days (promotion_id, day_of_week);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_tokens_token
    ON password_reset_tokens (token);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_idempotency_key
    ON payment_transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_external_reference
    ON payment_transactions (external_reference)
    WHERE external_reference IS NOT NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_one_active_per_table
    ON orders(table_id)
    WHERE is_deleted = false
      AND table_id IS NOT NULL
      AND status IN ('PENDING', 'SERVING', 'AWAITING_PAYMENT');

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_items_name_active
    ON inventory_items (LOWER(name))
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_recipe_item_active
    ON recipe_items (menu_item_id, inventory_item_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_item_inventory_reservation_active
    ON order_item_inventory_reservations (order_item_id, inventory_item_id)
    WHERE is_deleted = false AND status = 'RESERVED';

-- =========================
-- 4. Foreign-key lookup indexes
-- =========================
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_menu_item_cate_id
    ON menu_item (cate_id);

CREATE INDEX IF NOT EXISTS idx_item_option_menu_item_id
    ON item_option (menu_item_id);

CREATE INDEX IF NOT EXISTS idx_item_option_values_option_id
    ON item_option_values (item_option_id);

CREATE INDEX IF NOT EXISTS idx_combo_items_combo_id
    ON combo_items (combo_id);

CREATE INDEX IF NOT EXISTS idx_combo_items_menu_item_id
    ON combo_items (menu_item_id);

CREATE INDEX IF NOT EXISTS idx_orders_table_id
    ON orders (table_id);

CREATE INDEX IF NOT EXISTS idx_orders_paid_by
    ON orders (paid_by);

CREATE INDEX IF NOT EXISTS idx_order_batches_order_submitted
    ON order_batches (order_id, submitted_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_order_id
    ON order_item (order_id);

CREATE INDEX IF NOT EXISTS idx_order_item_menu_item_id
    ON order_item (menu_item_id);

CREATE INDEX IF NOT EXISTS idx_order_item_combo_id
    ON order_item (combo_id);

CREATE INDEX IF NOT EXISTS idx_order_item_batch_id
    ON order_item (batch_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_options_item_id
    ON order_item_options (order_item_id);

CREATE INDEX IF NOT EXISTS idx_order_item_options_value_id
    ON order_item_options (item_option_value_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id
    ON payment_transactions (order_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_created_by
    ON payment_transactions (created_by_id);

CREATE INDEX IF NOT EXISTS idx_order_discounts_order_id
    ON order_discounts (order_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_voucher_id
    ON order_discounts (voucher_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_recipe_items_menu_item_id
    ON recipe_items (menu_item_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_recipe_items_inventory_item_id
    ON recipe_items (inventory_item_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_stock_movements_order_item
    ON stock_movements (order_item_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_inventory_reservation_order_item
    ON order_item_inventory_reservations (order_item_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_inventory_reservation_inventory
    ON order_item_inventory_reservations (inventory_item_id)
    WHERE is_deleted = false;

-- =========================
-- 5. Operational / performance indexes
-- =========================
CREATE INDEX IF NOT EXISTS idx_orders_active_table
    ON orders (table_id, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_vouchers_active_code
    ON vouchers (code, active)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_status_created
    ON orders (status, created_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_history_filter
    ON orders (table_id, status, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_payment_status
    ON orders (status, payment_status)
    WHERE is_deleted = false AND payment_status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_order_item_order_status
    ON order_item (order_id, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_business_date_status
    ON orders (business_date, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_business_date_payment_status
    ON orders (business_date, payment_status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_type_created
    ON order_item (item_type, created_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_created
    ON payment_transactions (order_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status_created
    ON payment_transactions (status, created_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_business_date_status
    ON payment_transactions (business_date, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_code_snapshot
    ON order_discounts (code_snapshot)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_discounts_applied_at
    ON order_discounts (applied_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_changed
    ON order_status_history (order_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_item_status_history_item_changed
    ON order_item_status_history (order_item_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_daily_item_sales_summary_date_revenue
    ON daily_item_sales_summary (business_date, revenue DESC);

CREATE INDEX IF NOT EXISTS idx_orders_completed_business_date
    ON orders (business_date, payment_time DESC)
    WHERE is_deleted = false AND status = 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_order_item_snapshot_sales
    ON order_item (item_type, item_name_snapshot, created_at)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_inventory_items_low_stock
    ON inventory_items (active, quantity_on_hand, reserved_quantity, low_stock_threshold)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_stock_movements_inventory_created
    ON stock_movements (inventory_item_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_reservations_order_item_status
    ON order_item_inventory_reservations (order_item_id, status)
    WHERE is_deleted = false;

-- =========================
-- 6. Search indexes
-- =========================
CREATE INDEX IF NOT EXISTS idx_menu_item_name_gin
    ON menu_item USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_category_name_gin
    ON category USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_fullname_gin
    ON users USING gin (full_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_email_gin
    ON users USING gin (email gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_phone_gin
    ON users USING gin (phone gin_trgm_ops);
