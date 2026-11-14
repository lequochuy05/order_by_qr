-- Allow settlement with unfinished kitchen items (configurable)
ALTER TABLE system_settings
    ADD COLUMN allow_settlement_with_unfinished_items BOOLEAN NOT NULL DEFAULT false;
