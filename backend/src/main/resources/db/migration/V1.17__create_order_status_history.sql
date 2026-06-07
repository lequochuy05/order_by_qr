CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_changed
ON order_status_history(order_id, changed_at DESC);

CREATE TABLE order_item_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_item(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_order_item_status_history_item_changed
ON order_item_status_history(order_item_id, changed_at DESC);
