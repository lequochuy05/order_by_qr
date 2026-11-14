-- Add optimistic-lock support for inventory management updates.
-- Existing rows start at version 0; stock mutations retain their existing pessimistic locks.

ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
