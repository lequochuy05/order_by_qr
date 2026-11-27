-- V1.27__Subscription_Plans_And_Invoices.sql
-- Subscription plans (FREE / PRO) and PayOS VietQR upgrade invoices for QROS (008-pro-subscription-paywall)

CREATE TABLE IF NOT EXISTS restaurant_subscriptions (
    id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    plan_tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(20),
    started_at TIMESTAMP,
    expires_at TIMESTAMP,
    trial_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_restaurant_subscriptions_plan_tier CHECK (plan_tier IN ('FREE', 'PRO')),
    CONSTRAINT chk_restaurant_subscriptions_status CHECK (status IN ('ACTIVE', 'TRIALING', 'EXPIRED')),
    CONSTRAINT chk_restaurant_subscriptions_billing_cycle CHECK (
        billing_cycle IS NULL OR billing_cycle IN ('MONTHLY', 'YEARLY', 'TRIAL')
    )
);

INSERT INTO restaurant_subscriptions (id, version, plan_tier, status, billing_cycle, started_at, expires_at, trial_used)
VALUES (1, 0, 'FREE', 'ACTIVE', NULL, CURRENT_TIMESTAMP, NULL, FALSE)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS subscription_invoices (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    plan_tier VARCHAR(20) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_order_code BIGINT UNIQUE,
    payment_link_id VARCHAR(120),
    checkout_url VARCHAR(1000),
    qr_code TEXT,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_by_email VARCHAR(150),
    external_reference VARCHAR(120),
    provider_payload TEXT,
    paid_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscription_invoices_plan_tier CHECK (plan_tier IN ('PRO')),
    CONSTRAINT chk_subscription_invoices_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_invoices_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_subscription_invoices_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_invoices_created_at_desc
    ON subscription_invoices (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_invoices_status_expires_at
    ON subscription_invoices (status, expires_at);
