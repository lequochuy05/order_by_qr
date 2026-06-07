ALTER TABLE payment_transactions
ADD COLUMN paid_at TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN business_date DATE,
ADD COLUMN external_reference VARCHAR(100),
ADD COLUMN idempotency_key VARCHAR(100),
ADD COLUMN provider_payload JSONB,
ADD COLUMN failure_reason VARCHAR(255);