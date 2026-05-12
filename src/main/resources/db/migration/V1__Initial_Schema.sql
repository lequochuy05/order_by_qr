-- V1__Initial_Schema.sql
-- Hệ thống: QR Ordering System (QROS)
-- Mục tiêu: Khởi tạo cấu trúc bảng dựa trên JPA Entities

-- 1. Bảng Users (Nhân viên, Quản lý, Đầu bếp)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE,
    avatar_url VARCHAR(150),
    role VARCHAR(20) NOT NULL, -- STAFF, MANAGER, CHEF
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, BANNED, INACTIVE
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. Bảng Category (Danh mục món ăn)
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    img VARCHAR(150),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. Bảng Tables (Bàn ăn)
CREATE TABLE tables (
    id BIGSERIAL PRIMARY KEY,
    table_number VARCHAR(10) NOT NULL UNIQUE,
    table_code VARCHAR(50) NOT NULL UNIQUE,
    qr_code_url VARCHAR(150) NOT NULL UNIQUE,
    qr_code_public_id VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, WAITING_FOR_PAYMENT
    capacity INTEGER NOT NULL CHECK (capacity >= 1),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 4. Bảng Menu Item (Món ăn/Đồ uống)
CREATE TABLE menu_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    img VARCHAR(150) DEFAULT 'default_menu_item.png',
    price DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    cate_id INTEGER NOT NULL REFERENCES category(id),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 5. Bảng Item Option (Nhóm lựa chọn: Size, Topping...)
CREATE TABLE item_option (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    max_selection INTEGER NOT NULL DEFAULT 1 CHECK (max_selection >= 1),
    menu_item_id BIGINT NOT NULL REFERENCES menu_item(id),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 6. Bảng Item Option Values (Giá trị cụ thể: Topping Trân châu, Size L...)
CREATE TABLE item_option_values (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    extra_price DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (extra_price >= 0),
    item_option_id BIGINT NOT NULL REFERENCES item_option(id),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 7. Bảng Combos (Gói khuyến mãi)
CREATE TABLE combos (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 8. Bảng Combo Items (Món ăn trong Combo)
CREATE TABLE combo_items (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES combos(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_item(id),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 9. Bảng Vouchers (Mã giảm giá)
CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL DEFAULT 'FIXED_AMOUNT', -- FIXED_AMOUNT, PERCENTAGE
    discount_amount DECIMAL(15, 2) CHECK (discount_amount >= 0),
    discount_percent DOUBLE PRECISION CHECK (discount_percent >= 0),
    usage_limit INTEGER CHECK (usage_limit >= 0),
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 10. Bảng Promotions (Chương trình khuyến mãi theo thời gian)
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    discount_percent DOUBLE PRECISION,
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE,
    days_of_week VARCHAR(255), -- MON,TUE,WED...
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 11. Bảng Orders (Đơn hàng chính)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT REFERENCES tables(id),
    paid_by BIGINT REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SERVING, COMPLETED, CANCELLED
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN', -- DINE_IN, TAKEAWAY
    original_total DECIMAL(15, 2) CHECK (original_total >= 0),
    discount_voucher DECIMAL(15, 2) DEFAULT 0,
    voucher_code VARCHAR(50),
    total_amount DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED
    payment_method VARCHAR(20), -- CASH, PAYOS
    payment_time TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 12. Bảng Order Item (Chi tiết từng món trong đơn hàng)
CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    menu_item_id BIGINT REFERENCES menu_item(id),
    combo_id BIGINT REFERENCES combos(id),
    unit_price DECIMAL(15, 2) NOT NULL CHECK (unit_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    notes TEXT,
    prepared BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COOKING, READY, SERVED, FINISHED, CANCELLED
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 13. Bảng Order Item Options (Lựa chọn đã capture lại lúc đặt món)
CREATE TABLE order_item_options (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_item(id),
    item_option_value_id BIGINT REFERENCES item_option_values(id),
    option_name VARCHAR(50) NOT NULL,
    option_value_name VARCHAR(50) NOT NULL,
    extra_price DECIMAL(15, 2) NOT NULL CHECK (extra_price >= 0),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 14. Bảng Payment Transactions (Giao dịch thanh toán cổng ngoài)
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    amount DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED, FAILED
    payment_method VARCHAR(20) NOT NULL DEFAULT 'PAYOS',
    checkout_url VARCHAR(500),
    qr_code TEXT,
    payos_reference VARCHAR(100),
    cancel_reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 15. Bảng Password Reset Tokens (Khôi phục mật khẩu)
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    otp_code VARCHAR(6),
    via VARCHAR(10) NOT NULL, -- EMAIL, PHONE
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);