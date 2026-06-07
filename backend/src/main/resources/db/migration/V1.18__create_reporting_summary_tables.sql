CREATE TABLE daily_revenue_summary (
    business_date DATE PRIMARY KEY,
    total_orders BIGINT NOT NULL DEFAULT 0,
    subtotal_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE daily_item_sales_summary (
    business_date DATE NOT NULL,
    item_key VARCHAR(80) NOT NULL,
    menu_item_id BIGINT,
    item_name_snapshot VARCHAR(150) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0,
    revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (business_date, item_key)
);

CREATE INDEX IF NOT EXISTS idx_daily_item_sales_summary_date_revenue
ON daily_item_sales_summary(business_date, revenue DESC);
