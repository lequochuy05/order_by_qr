-- V1.4__Reporting_Summaries.sql
-- Turns the reporting summary tables into maintained, query-ready aggregates.

ALTER TABLE daily_item_sales_summary
    ADD COLUMN IF NOT EXISTS combo_id BIGINT;

ALTER TABLE daily_item_sales_summary
    ADD CONSTRAINT fk_daily_item_sales_summary_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id) ON DELETE SET NULL;

ALTER TABLE daily_item_sales_summary
    ADD CONSTRAINT fk_daily_item_sales_summary_combo
    FOREIGN KEY (combo_id) REFERENCES combos(id) ON DELETE SET NULL;

ALTER TABLE daily_item_sales_summary
    ADD CONSTRAINT chk_daily_item_sales_summary_item_type
    CHECK (item_type IN ('MENU_ITEM', 'COMBO'));

ALTER TABLE daily_item_sales_summary
    ADD CONSTRAINT chk_daily_item_sales_summary_product_xor
    CHECK (NOT (menu_item_id IS NOT NULL AND combo_id IS NOT NULL));

CREATE INDEX IF NOT EXISTS idx_daily_item_sales_summary_menu_item_date
    ON daily_item_sales_summary (menu_item_id, business_date)
    WHERE menu_item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_daily_item_sales_summary_combo_date
    ON daily_item_sales_summary (combo_id, business_date)
    WHERE combo_id IS NOT NULL;

DELETE FROM daily_item_sales_summary;
DELETE FROM daily_revenue_summary;

INSERT INTO daily_revenue_summary (
    business_date,
    total_orders,
    subtotal_amount,
    discount_amount,
    final_amount,
    paid_amount,
    updated_at
)
SELECT
    o.business_date,
    COUNT(*) AS total_orders,
    COALESCE(SUM(o.subtotal_amount), 0) AS subtotal_amount,
    COALESCE(SUM(o.discount_amount), 0) AS discount_amount,
    COALESCE(SUM(o.final_amount), 0) AS final_amount,
    COALESCE(SUM(o.paid_amount), 0) AS paid_amount,
    CURRENT_TIMESTAMP
FROM orders o
WHERE o.status = 'COMPLETED'
  AND o.payment_status = 'PAID'
  AND o.is_deleted = false
GROUP BY o.business_date;

WITH item_sales AS (
    SELECT
        o.business_date,
        CASE
            WHEN oi.item_type = 'MENU_ITEM' AND oi.menu_item_id IS NOT NULL
                THEN 'MENU_ITEM:' || oi.menu_item_id
            WHEN oi.item_type = 'COMBO' AND oi.combo_id IS NOT NULL
                THEN 'COMBO:' || oi.combo_id
            ELSE oi.item_type || ':SNAPSHOT:' || MD5(COALESCE(oi.item_name_snapshot, ''))
        END AS item_key,
        CASE WHEN oi.item_type = 'MENU_ITEM' THEN oi.menu_item_id END AS menu_item_id,
        CASE WHEN oi.item_type = 'COMBO' THEN oi.combo_id END AS combo_id,
        MAX(oi.item_name_snapshot) AS item_name_snapshot,
        oi.item_type,
        SUM(oi.quantity)::BIGINT AS quantity,
        COALESCE(SUM(COALESCE(oi.line_total, oi.quantity * oi.unit_price)), 0) AS revenue
    FROM order_item oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.status = 'COMPLETED'
      AND o.payment_status = 'PAID'
      AND o.is_deleted = false
      AND oi.is_deleted = false
      AND oi.status <> 'CANCELLED'
    GROUP BY
        o.business_date,
        CASE
            WHEN oi.item_type = 'MENU_ITEM' AND oi.menu_item_id IS NOT NULL
                THEN 'MENU_ITEM:' || oi.menu_item_id
            WHEN oi.item_type = 'COMBO' AND oi.combo_id IS NOT NULL
                THEN 'COMBO:' || oi.combo_id
            ELSE oi.item_type || ':SNAPSHOT:' || MD5(COALESCE(oi.item_name_snapshot, ''))
        END,
        CASE WHEN oi.item_type = 'MENU_ITEM' THEN oi.menu_item_id END,
        CASE WHEN oi.item_type = 'COMBO' THEN oi.combo_id END,
        oi.item_type
)
INSERT INTO daily_item_sales_summary (
    business_date,
    item_key,
    menu_item_id,
    combo_id,
    item_name_snapshot,
    item_type,
    quantity,
    revenue,
    updated_at
)
SELECT
    business_date,
    item_key,
    menu_item_id,
    combo_id,
    item_name_snapshot,
    item_type,
    quantity,
    revenue,
    CURRENT_TIMESTAMP
FROM item_sales;
