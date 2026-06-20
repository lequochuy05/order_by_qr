CREATE OR REPLACE FUNCTION apply_daily_order_activity_delta(
    target_date DATE,
    total_delta BIGINT,
    completed_delta BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    IF target_date IS NULL OR (total_delta = 0 AND completed_delta = 0) THEN
        RETURN;
    END IF;

    INSERT INTO daily_order_activity_summary (
        activity_date,
        total_orders,
        completed_orders,
        updated_at
    )
    VALUES (
        target_date,
        GREATEST(total_delta, 0),
        GREATEST(completed_delta, 0),
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (activity_date) DO UPDATE
    SET total_orders = GREATEST(daily_order_activity_summary.total_orders + total_delta, 0),
        completed_orders = GREATEST(daily_order_activity_summary.completed_orders + completed_delta, 0),
        updated_at = CURRENT_TIMESTAMP;

    DELETE FROM daily_order_activity_summary
    WHERE activity_date = target_date
      AND total_orders = 0
      AND completed_orders = 0;
END;
$$;

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
        WHERE order_id = OLD.id;
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
