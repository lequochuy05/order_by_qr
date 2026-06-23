ALTER TABLE system_settings
    ADD COLUMN IF NOT EXISTS cash_payment_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS online_payment_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS payment_qr_expires_in_minutes INTEGER NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS auto_confirm_orders BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS kitchen_overdue_threshold_minutes INTEGER NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS show_unavailable_items BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS show_recommendations BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS show_combos BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS bill_title VARCHAR(100) NOT NULL DEFAULT 'HÓA ĐƠN THANH TOÁN',
    ADD COLUMN IF NOT EXISTS bill_footer_message VARCHAR(255) NOT NULL DEFAULT 'CẢM ƠN QUÝ KHÁCH & HẸN GẶP LẠI!',
    ADD COLUMN IF NOT EXISTS bill_paper_size VARCHAR(10) NOT NULL DEFAULT '80',
    ADD COLUMN IF NOT EXISTS show_wifi_on_bill BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS auto_print_bill BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS new_order_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS payment_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS kitchen_overdue_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE system_settings
    DROP CONSTRAINT IF EXISTS chk_system_settings_payment_methods,
    DROP CONSTRAINT IF EXISTS chk_system_settings_payment_qr_expiry,
    DROP CONSTRAINT IF EXISTS chk_system_settings_kitchen_overdue_threshold,
    DROP CONSTRAINT IF EXISTS chk_system_settings_bill_paper_size;

ALTER TABLE system_settings
    ADD CONSTRAINT chk_system_settings_payment_methods
        CHECK (cash_payment_enabled OR online_payment_enabled),
    ADD CONSTRAINT chk_system_settings_payment_qr_expiry
        CHECK (payment_qr_expires_in_minutes BETWEEN 1 AND 120),
    ADD CONSTRAINT chk_system_settings_kitchen_overdue_threshold
        CHECK (kitchen_overdue_threshold_minutes BETWEEN 1 AND 240),
    ADD CONSTRAINT chk_system_settings_bill_paper_size
        CHECK (bill_paper_size IN ('58', '80'));
