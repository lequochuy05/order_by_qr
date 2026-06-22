ALTER TABLE system_settings
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE system_settings
DROP CONSTRAINT IF EXISTS chk_system_settings_opening_closing_time;

INSERT INTO system_settings (
    id,
    restaurant_name,
    currency,
    tax_percent,
    service_charge_percent,
    ordering_enabled,
    maintenance_mode,
    created_at,
    updated_at,
    is_deleted,
    version
)
VALUES (
    1,
    'QROS Restaurant',
    'VND',
    0,
    0,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE,
    0
)
ON CONFLICT (id) DO UPDATE
SET is_deleted = FALSE;
