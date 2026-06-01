-- Tạo bảng lưu trữ cấu hình hệ thống
CREATE TABLE system_settings (
    id SERIAL PRIMARY KEY,
    restaurant_name VARCHAR(100) NOT NULL,
    restaurant_address TEXT,
    restaurant_phone VARCHAR(20),
    restaurant_logo_url TEXT,
    vat_rate DECIMAL(5,2) DEFAULT 0.0,
    wifi_ssid VARCHAR(50),
    wifi_password VARCHAR(50),
    auto_approve_orders BOOLEAN DEFAULT TRUE,
    enable_ai_assistant BOOLEAN DEFAULT TRUE,
    enable_payos BOOLEAN DEFAULT TRUE,
    enable_cash BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chèn dữ liệu cấu hình mặc định ban đầu
INSERT INTO system_settings (
    restaurant_name, 
    restaurant_address, 
    restaurant_phone,  
    vat_rate, 
    auto_approve_orders, 
    enable_ai_assistant, 
    enable_payos, 
    enable_cash
) VALUES (
    'Sắc Màu Quán', 
    'Vu Gia - Đà Nẵng', 
    '0704102569', 
    8.0, -- VAT 8%
    TRUE, 
    TRUE, 
    TRUE, 
    TRUE
);
