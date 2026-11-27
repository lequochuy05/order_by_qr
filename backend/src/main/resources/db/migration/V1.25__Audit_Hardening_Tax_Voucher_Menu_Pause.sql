-- WS-1 / WS-2 / WS-4 Audit Hardening Migration
-- 1. Tax & service charge snapshots on orders and order_reporting_fact + trigger sync fix (ISSUE-013, ISSUE-065)
-- 2. Password reset OTP attempt counter (ISSUE-015)
-- 3. Voucher optimistic locking version and reservation counter + payment voucher snapshot (ISSUE-040)
-- 4. Explicit manual pause flag for menu items and combos (ISSUE-049)

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS service_charge_percent_snapshot NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS service_charge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_percent_snapshot NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_service_charge_percent_non_negative') THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_service_charge_percent_non_negative
            CHECK (service_charge_percent_snapshot >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_service_charge_amount_non_negative') THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_service_charge_amount_non_negative
            CHECK (service_charge_amount >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_tax_percent_non_negative') THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_tax_percent_non_negative
            CHECK (tax_percent_snapshot >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_tax_amount_non_negative') THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_tax_amount_non_negative
            CHECK (tax_amount >= 0);
    END IF;
END $$;

ALTER TABLE order_reporting_fact
    ADD COLUMN IF NOT EXISTS service_charge_percent_snapshot NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS service_charge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_percent_snapshot NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION sync_order_reporting_fact()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    old_created_date DATE;
    new_created_date DATE;
    old_total BIGINT := 0;
    new_total BIGINT := 0;
    old_completed BIGINT := 0;
    new_completed BIGINT := 0;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        old_created_date := COALESCE(OLD.created_at::DATE, OLD.business_date);
        IF NOT OLD.is_deleted THEN
            old_total := 1;
            IF OLD.status = 'COMPLETED' THEN
                old_completed := 1;
            END IF;
        END IF;
    END IF;

    IF TG_OP <> 'DELETE' THEN
        new_created_date := COALESCE(NEW.created_at::DATE, NEW.business_date);
        IF NOT NEW.is_deleted THEN
            new_total := 1;
            IF NEW.status = 'COMPLETED' THEN
                new_completed := 1;
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' OR NEW.is_deleted THEN
        DELETE FROM order_reporting_fact
        WHERE order_id = CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END;
    ELSE
        INSERT INTO order_reporting_fact (
            order_id,
            created_date,
            business_date,
            status,
            payment_status,
            order_type,
            table_id,
            table_number_snapshot,
            paid_by,
            paid_by_name_snapshot,
            paid_by_avatar_url_snapshot,
            subtotal_amount,
            discount_amount,
            service_charge_percent_snapshot,
            service_charge_amount,
            tax_percent_snapshot,
            tax_amount,
            final_amount,
            paid_amount,
            payment_method,
            created_at,
            payment_time,
            source_updated_at,
            synced_at
        )
        SELECT
            NEW.id,
            new_created_date,
            NEW.business_date,
            NEW.status,
            NEW.payment_status,
            NEW.order_type,
            NEW.table_id,
            t.table_number,
            NEW.paid_by,
            u.full_name,
            u.avatar_url,
            NEW.subtotal_amount,
            NEW.discount_amount,
            NEW.service_charge_percent_snapshot,
            NEW.service_charge_amount,
            NEW.tax_percent_snapshot,
            NEW.tax_amount,
            NEW.final_amount,
            NEW.paid_amount,
            NEW.payment_method,
            NEW.created_at,
            NEW.payment_time,
            NEW.updated_at,
            CURRENT_TIMESTAMP
        FROM (SELECT 1) source
        LEFT JOIN tables t ON t.id = NEW.table_id
        LEFT JOIN users u ON u.id = NEW.paid_by
        ON CONFLICT (order_id) DO UPDATE
        SET created_date = EXCLUDED.created_date,
            business_date = EXCLUDED.business_date,
            status = EXCLUDED.status,
            payment_status = EXCLUDED.payment_status,
            order_type = EXCLUDED.order_type,
            table_id = EXCLUDED.table_id,
            table_number_snapshot = EXCLUDED.table_number_snapshot,
            paid_by = EXCLUDED.paid_by,
            paid_by_name_snapshot = EXCLUDED.paid_by_name_snapshot,
            paid_by_avatar_url_snapshot = EXCLUDED.paid_by_avatar_url_snapshot,
            subtotal_amount = EXCLUDED.subtotal_amount,
            discount_amount = EXCLUDED.discount_amount,
            service_charge_percent_snapshot = EXCLUDED.service_charge_percent_snapshot,
            service_charge_amount = EXCLUDED.service_charge_amount,
            tax_percent_snapshot = EXCLUDED.tax_percent_snapshot,
            tax_amount = EXCLUDED.tax_amount,
            final_amount = EXCLUDED.final_amount,
            paid_amount = EXCLUDED.paid_amount,
            payment_method = EXCLUDED.payment_method,
            created_at = EXCLUDED.created_at,
            payment_time = EXCLUDED.payment_time,
            source_updated_at = EXCLUDED.source_updated_at,
            synced_at = EXCLUDED.synced_at;
    END IF;

    IF old_created_date IS NOT DISTINCT FROM new_created_date THEN
        PERFORM apply_daily_order_activity_delta(
            COALESCE(new_created_date, old_created_date),
            new_total - old_total,
            new_completed - old_completed
        );
    ELSE
        PERFORM apply_daily_order_activity_delta(
            old_created_date,
            -old_total,
            -old_completed
        );
        PERFORM apply_daily_order_activity_delta(
            new_created_date,
            new_total,
            new_completed
        );
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

ALTER TABLE password_reset_tokens
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_password_reset_tokens_attempts_non_negative') THEN
        ALTER TABLE password_reset_tokens
            ADD CONSTRAINT chk_password_reset_tokens_attempts_non_negative
            CHECK (attempts >= 0);
    END IF;
END $$;

ALTER TABLE vouchers
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reserved_count INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_vouchers_reserved_count_non_negative') THEN
        ALTER TABLE vouchers
            ADD CONSTRAINT chk_vouchers_reserved_count_non_negative
            CHECK (reserved_count >= 0);
    END IF;
END $$;

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS voucher_usage_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS voucher_code_snapshot VARCHAR(50);

ALTER TABLE menu_item
    ADD COLUMN IF NOT EXISTS manually_paused BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS manually_paused BOOLEAN NOT NULL DEFAULT FALSE;
