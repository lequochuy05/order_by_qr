-- V1.5__Reporting_Facts.sql
-- Adds self-contained reporting facts so analytics requests do not scan or join
-- operational order tables.

ALTER TABLE daily_item_sales_summary
    ADD COLUMN IF NOT EXISTS category_name_snapshot VARCHAR(150);

ALTER TABLE daily_item_sales_summary
    ADD COLUMN IF NOT EXISTS image_url_snapshot VARCHAR(500);

CREATE TABLE order_reporting_fact (
    order_id BIGINT PRIMARY KEY,
    created_date DATE NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    table_id BIGINT,
    table_number_snapshot VARCHAR(50),
    paid_by BIGINT,
    paid_by_name_snapshot VARCHAR(255),
    paid_by_avatar_url_snapshot VARCHAR(500),
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    final_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    payment_time TIMESTAMP WITHOUT TIME ZONE,
    source_updated_at TIMESTAMP WITHOUT TIME ZONE,
    synced_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE daily_staff_sales_summary (
    business_date DATE NOT NULL,
    staff_key VARCHAR(80) NOT NULL,
    user_id BIGINT,
    full_name_snapshot VARCHAR(255) NOT NULL,
    avatar_url_snapshot VARCHAR(500),
    order_count BIGINT NOT NULL DEFAULT 0,
    revenue NUMERIC(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (business_date, staff_key)
);

CREATE TABLE daily_order_activity_summary (
    activity_date DATE PRIMARY KEY,
    total_orders BIGINT NOT NULL DEFAULT 0,
    completed_orders BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_order_reporting_fact_created_status
    ON order_reporting_fact (created_date, status);

CREATE INDEX idx_order_reporting_fact_business_status
    ON order_reporting_fact (business_date, status, payment_status);

CREATE INDEX idx_order_reporting_fact_payment_time
    ON order_reporting_fact (payment_time DESC)
    WHERE payment_time IS NOT NULL;

CREATE INDEX idx_order_reporting_fact_paid_by_business
    ON order_reporting_fact (paid_by, business_date)
    WHERE paid_by IS NOT NULL;

CREATE INDEX idx_order_reporting_fact_table_number
    ON order_reporting_fact (LOWER(table_number_snapshot))
    WHERE table_number_snapshot IS NOT NULL;

CREATE INDEX idx_daily_staff_sales_summary_date_revenue
    ON daily_staff_sales_summary (business_date, revenue DESC);

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
    o.id,
    COALESCE(o.created_at::DATE, o.business_date),
    o.business_date,
    o.status,
    o.payment_status,
    o.order_type,
    o.table_id,
    t.table_number,
    o.paid_by,
    u.full_name,
    u.avatar_url,
    o.subtotal_amount,
    o.discount_amount,
    o.final_amount,
    o.paid_amount,
    o.payment_method,
    o.created_at,
    o.payment_time,
    o.updated_at,
    CURRENT_TIMESTAMP
FROM orders o
LEFT JOIN tables t ON t.id = o.table_id
LEFT JOIN users u ON u.id = o.paid_by
WHERE o.is_deleted = false;

INSERT INTO daily_staff_sales_summary (
    business_date,
    staff_key,
    user_id,
    full_name_snapshot,
    avatar_url_snapshot,
    order_count,
    revenue,
    updated_at
)
SELECT
    fact.business_date,
    'USER:' || fact.paid_by,
    fact.paid_by,
    MAX(COALESCE(fact.paid_by_name_snapshot, 'Nhân viên')),
    MAX(fact.paid_by_avatar_url_snapshot),
    COUNT(*),
    COALESCE(SUM(fact.final_amount), 0),
    CURRENT_TIMESTAMP
FROM order_reporting_fact fact
WHERE fact.status = 'COMPLETED'
  AND fact.payment_status = 'PAID'
  AND fact.paid_by IS NOT NULL
GROUP BY fact.business_date, fact.paid_by;

INSERT INTO daily_order_activity_summary (
    activity_date,
    total_orders,
    completed_orders,
    updated_at
)
SELECT
    fact.created_date,
    COUNT(*),
    COUNT(*) FILTER (WHERE fact.status = 'COMPLETED'),
    CURRENT_TIMESTAMP
FROM order_reporting_fact fact
GROUP BY fact.created_date;

UPDATE daily_item_sales_summary summary
SET category_name_snapshot = CASE
        WHEN summary.item_type = 'MENU_ITEM' THEN COALESCE(category.name, 'Chưa phân loại')
        ELSE 'Combo'
    END,
    image_url_snapshot = CASE
        WHEN summary.item_type = 'MENU_ITEM' THEN menu_item.img
        ELSE NULL
    END
FROM menu_item
LEFT JOIN category ON category.id = menu_item.cate_id
WHERE summary.menu_item_id = menu_item.id;

UPDATE daily_item_sales_summary
SET category_name_snapshot = 'Combo'
WHERE item_type = 'COMBO'
  AND category_name_snapshot IS NULL;

CREATE OR REPLACE FUNCTION rebuild_daily_order_activity_summary(target_date DATE)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO daily_order_activity_summary (
        activity_date,
        total_orders,
        completed_orders,
        updated_at
    )
    SELECT
        target_date,
        COUNT(*),
        COUNT(*) FILTER (WHERE fact.status = 'COMPLETED'),
        CURRENT_TIMESTAMP
    FROM order_reporting_fact fact
    WHERE fact.created_date = target_date
    ON CONFLICT (activity_date) DO UPDATE
    SET total_orders = EXCLUDED.total_orders,
        completed_orders = EXCLUDED.completed_orders,
        updated_at = EXCLUDED.updated_at;
END;
$$;

CREATE OR REPLACE FUNCTION sync_order_reporting_fact()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    old_created_date DATE;
    new_created_date DATE;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        old_created_date := COALESCE(OLD.created_at::DATE, OLD.business_date);
    END IF;

    IF TG_OP = 'DELETE' THEN
        DELETE FROM order_reporting_fact
        WHERE order_id = OLD.id;

        PERFORM rebuild_daily_order_activity_summary(old_created_date);
        RETURN OLD;
    END IF;

    IF NEW.is_deleted THEN
        DELETE FROM order_reporting_fact
        WHERE order_id = NEW.id;

        PERFORM rebuild_daily_order_activity_summary(old_created_date);
        RETURN NEW;
    END IF;

    new_created_date := COALESCE(NEW.created_at::DATE, NEW.business_date);

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

    PERFORM rebuild_daily_order_activity_summary(new_created_date);

    IF old_created_date IS NOT NULL AND old_created_date <> new_created_date THEN
        PERFORM rebuild_daily_order_activity_summary(old_created_date);
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sync_order_reporting_fact
AFTER INSERT OR UPDATE OR DELETE ON orders
FOR EACH ROW
EXECUTE FUNCTION sync_order_reporting_fact();
