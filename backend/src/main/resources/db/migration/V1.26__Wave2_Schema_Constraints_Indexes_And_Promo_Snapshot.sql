-- V1.26__Wave2_Schema_Constraints_Indexes_And_Promo_Snapshot.sql
-- Wave-2 Hardening:
--   ISSUE-023: Widen users.avatar_url to VARCHAR(500) and users.full_name to VARCHAR(100)
--   ISSUE-024: Persist applied_promotion_percent snapshot on orders
--   ISSUE-029: Drop duplicate V1.1 indexes superseded by V1.9, add order_reporting_fact(created_at DESC) index,
--              and add defensive CHECK constraints on promotions, vouchers, and inventory_items.

-- =========================
-- 1. Users column width alignment (ISSUE-023)
-- =========================
ALTER TABLE users
    ALTER COLUMN avatar_url TYPE VARCHAR(500),
    ALTER COLUMN full_name TYPE VARCHAR(100);

-- =========================
-- 2. Happy-Hour promotion snapshot on orders (ISSUE-024)
-- =========================
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS applied_promotion_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_applied_promotion_percent_range') THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_applied_promotion_percent_range
            CHECK (applied_promotion_percent >= 0 AND applied_promotion_percent <= 100);
    END IF;
END $$;

-- =========================
-- 3. Drop duplicate V1.1 indexes superseded by V1.9 (ISSUE-029)
-- =========================
DROP INDEX IF EXISTS uk_category_name_active;
DROP INDEX IF EXISTS uk_menu_item_name_active;
DROP INDEX IF EXISTS uk_combos_name_active;
DROP INDEX IF EXISTS idx_menu_item_name_gin;
DROP INDEX IF EXISTS idx_category_name_gin;

-- =========================
-- 4. Missing reporting index & defensive CHECK constraints (ISSUE-029)
-- =========================
CREATE INDEX IF NOT EXISTS idx_order_reporting_fact_created_at_desc
    ON order_reporting_fact (created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_promotions_discount_percent_range') THEN
        ALTER TABLE promotions
            ADD CONSTRAINT chk_promotions_discount_percent_range
            CHECK (discount_percent > 0 AND discount_percent <= 100);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_vouchers_usage_and_dates') THEN
        ALTER TABLE vouchers
            ADD CONSTRAINT chk_vouchers_usage_and_dates
            CHECK (
                used_count >= 0
                AND reserved_count >= 0
                AND (usage_limit IS NULL OR usage_limit = 0 OR used_count <= usage_limit)
                AND valid_to > valid_from
            );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_items_unit_cost_non_negative') THEN
        ALTER TABLE inventory_items
            ADD CONSTRAINT chk_inventory_items_unit_cost_non_negative
            CHECK (unit_cost >= 0);
    END IF;
END $$;
