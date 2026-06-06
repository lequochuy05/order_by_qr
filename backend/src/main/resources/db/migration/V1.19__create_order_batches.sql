CREATE TABLE order_batches (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    source VARCHAR(20),
    note VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE order_item
ADD COLUMN batch_id BIGINT REFERENCES order_batches(id);

CREATE INDEX IF NOT EXISTS idx_order_batches_order_submitted
ON order_batches(order_id, submitted_at)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_order_item_batch_id
ON order_item(batch_id)
WHERE is_deleted = false;
