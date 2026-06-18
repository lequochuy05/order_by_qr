-- V1__Initial.sql
-- QROS - Initial schema for a fresh database.
-- Quy ước: file V1 chỉ tạo bảng/cột/PRIMARY KEY/CHECK constraint và dữ liệu seed tối thiểu.
-- Foreign key, unique index và performance/search index nằm ở V1.1__FK_Indexes.sql.

-- =========================
-- 1. Users / Auth
-- =========================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    full_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    avatar_url VARCHAR(150),
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    otp_code VARCHAR(64),
    via VARCHAR(255) NOT NULL DEFAULT 'EMAIL',
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- =========================
-- 2. Settings
-- =========================
CREATE TABLE system_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    restaurant_name VARCHAR(150) NOT NULL,
    restaurant_address VARCHAR(255),
    restaurant_phone VARCHAR(30),
    restaurant_email VARCHAR(150),
    logo_url VARCHAR(500),
    wifi_name VARCHAR(100),
    wifi_password VARCHAR(255),
    opening_time TIME WITHOUT TIME ZONE,
    closing_time TIME WITHOUT TIME ZONE,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    tax_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    service_charge_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ordering_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_system_settings_single_row CHECK (id = 1),
    CONSTRAINT chk_system_settings_tax_percent_range CHECK (tax_percent >= 0 AND tax_percent <= 100),
    CONSTRAINT chk_system_settings_service_charge_percent_range CHECK (service_charge_percent >= 0 AND service_charge_percent <= 100),
    CONSTRAINT chk_system_settings_opening_closing_time CHECK (
        opening_time IS NULL
        OR closing_time IS NULL
        OR closing_time > opening_time
    )
);

INSERT INTO system_settings (
    id,
    restaurant_name,
    currency,
    tax_percent,
    service_charge_percent,
    ordering_enabled,
    maintenance_mode,
    created_at,
    updated_at,
    is_deleted
) VALUES (
    1,
    'QROS Restaurant',
    'VND',
    0,
    0,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
);

-- =========================
-- 3. Table management
-- =========================
CREATE TABLE tables (
    id BIGSERIAL PRIMARY KEY,
    table_number VARCHAR(10) NOT NULL,
    table_code VARCHAR(50) NOT NULL,
    qr_code_url VARCHAR(500) NOT NULL,
    qr_code_public_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    capacity INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_tables_capacity_positive CHECK (capacity >= 1)
);

-- =========================
-- 4. Menu catalog
-- =========================
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    img VARCHAR(500),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE menu_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    img VARCHAR(500) DEFAULT 'default_menu_item.png',
    description VARCHAR(500),
    price NUMERIC(15, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    cate_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_menu_item_price_non_negative CHECK (price >= 0)
);

CREATE TABLE item_option (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    max_selection INTEGER NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL DEFAULT 0,
    menu_item_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_item_option_max_selection_positive CHECK (max_selection >= 1)
);

CREATE TABLE item_option_values (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    extra_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    item_option_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_item_option_values_extra_price_non_negative CHECK (extra_price >= 0)
);

CREATE TABLE combos (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price NUMERIC(15, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_combos_price_non_negative CHECK (price >= 0)
);

CREATE TABLE combo_items (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_combo_items_quantity_positive CHECK (quantity >= 1)
);

-- =========================
-- 5. Promotion / Voucher
-- =========================
CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'FIXED_AMOUNT',
    discount_amount NUMERIC(15, 2),
    discount_percent NUMERIC(5, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_vouchers_discount_amount_non_negative CHECK (discount_amount IS NULL OR discount_amount >= 0),
    CONSTRAINT chk_vouchers_discount_percent_non_negative CHECK (discount_percent IS NULL OR discount_percent >= 0),
    CONSTRAINT chk_vouchers_usage_limit_non_negative CHECK (usage_limit IS NULL OR usage_limit >= 0)
);

CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE promotion_days (
    promotion_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL
);

-- =========================
-- 6. Orders / Kitchen / Payment
-- =========================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT,
    paid_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    voucher_code VARCHAR(50),
    final_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    business_date DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    payment_time TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_orders_subtotal_amount_non_negative CHECK (subtotal_amount >= 0),
    CONSTRAINT chk_orders_discount_amount_non_negative CHECK (discount_amount >= 0),
    CONSTRAINT chk_orders_final_amount_non_negative CHECK (final_amount >= 0),
    CONSTRAINT chk_orders_paid_amount_non_negative CHECK (paid_amount >= 0)
);

CREATE TABLE order_batches (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'QR',
    note VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT,
    combo_id BIGINT,
    batch_id BIGINT,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    item_name_snapshot VARCHAR(150) NOT NULL DEFAULT '',
    item_type VARCHAR(20) NOT NULL DEFAULT 'MENU_ITEM',
    quantity INTEGER NOT NULL DEFAULT 1,
    line_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes VARCHAR(255),
    prepared BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_order_item_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_order_item_quantity_positive CHECK (quantity >= 1),
    CONSTRAINT chk_order_item_line_total_non_negative CHECK (line_total >= 0),
    CONSTRAINT chk_order_item_product_xor CHECK (
        (menu_item_id IS NOT NULL AND combo_id IS NULL)
        OR
        (menu_item_id IS NULL AND combo_id IS NOT NULL)
    ),
    CONSTRAINT chk_order_item_item_type CHECK (item_type IN ('MENU_ITEM', 'COMBO'))
);

CREATE TABLE order_item_options (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    item_option_value_id BIGINT,
    option_name VARCHAR(50) NOT NULL,
    option_value_name VARCHAR(50) NOT NULL,
    extra_price NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_order_item_options_extra_price_non_negative CHECK (extra_price >= 0)
);

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL DEFAULT 'PAYOS',
    checkout_url VARCHAR(500),
    qr_code TEXT,
    created_by_id BIGINT,
    paid_at TIMESTAMP WITHOUT TIME ZONE,
    business_date DATE,
    external_reference VARCHAR(100),
    idempotency_key VARCHAR(100),
    provider_payload JSONB,
    failure_reason VARCHAR(255),
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_payment_transactions_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE order_discounts (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    voucher_id BIGINT,
    code_snapshot VARCHAR(50) NOT NULL,
    discount_type_snapshot VARCHAR(30) NOT NULL,
    discount_percent_snapshot NUMERIC(5, 2),
    discount_amount_snapshot NUMERIC(15, 2),
    applied_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    applied_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_order_discounts_applied_amount_non_negative CHECK (applied_amount >= 0)
);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason VARCHAR(255)
);

CREATE TABLE order_item_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason VARCHAR(255)
);

-- =========================
-- 7. Reporting summary
-- =========================
CREATE TABLE daily_revenue_summary (
    business_date DATE PRIMARY KEY,
    total_orders BIGINT NOT NULL DEFAULT 0,
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    final_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE daily_item_sales_summary (
    business_date DATE NOT NULL,
    item_key VARCHAR(80) NOT NULL,
    menu_item_id BIGINT,
    item_name_snapshot VARCHAR(150) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0,
    revenue NUMERIC(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (business_date, item_key)
);

-- =========================
-- 8. Inventory
-- =========================
CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    quantity_on_hand NUMERIC(15, 3) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    low_stock_threshold NUMERIC(15, 3) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_inventory_items_quantity_consistency CHECK (
        quantity_on_hand >= 0
        AND reserved_quantity >= 0
        AND low_stock_threshold >= 0
        AND reserved_quantity <= quantity_on_hand
    )
);

CREATE TABLE recipe_items (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity_required NUMERIC(15, 3) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_recipe_item_quantity_positive CHECK (quantity_required > 0)
);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    order_item_id BIGINT,
    type VARCHAR(30) NOT NULL,
    quantity NUMERIC(15, 3) NOT NULL,
    quantity_before NUMERIC(15, 3) NOT NULL,
    quantity_after NUMERIC(15, 3) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_stock_movement_quantity_non_zero CHECK (quantity <> 0)
);

CREATE TABLE order_item_inventory_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    reserved_quantity NUMERIC(15, 3) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    reserved_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP WITHOUT TIME ZONE,
    consumed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_order_item_inventory_reserved_positive CHECK (reserved_quantity > 0),
    CONSTRAINT chk_order_item_inventory_status CHECK (status IN ('RESERVED', 'RELEASED', 'CONSUMED'))
);
