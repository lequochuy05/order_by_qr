ALTER TABLE category
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE menu_item
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE combos
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_name_active
ON category (LOWER(name))
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_menu_item_name_active
ON menu_item (LOWER(name))
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_combos_name_active
ON combos (LOWER(name))
WHERE is_deleted = false;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_category_name_trgm_active
ON category USING gin (LOWER(name) gin_trgm_ops)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_menu_item_name_trgm_active
ON menu_item USING gin (LOWER(name) gin_trgm_ops)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_combos_name_trgm_active
ON combos USING gin (LOWER(name) gin_trgm_ops)
WHERE is_deleted = false;
