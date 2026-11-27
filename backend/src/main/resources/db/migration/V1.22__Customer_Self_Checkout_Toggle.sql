-- Allow customers to self-checkout at the table via VietQR (PayOS) when enabled by Admin
ALTER TABLE system_settings
    ADD COLUMN customer_self_checkout_enabled BOOLEAN NOT NULL DEFAULT false;
