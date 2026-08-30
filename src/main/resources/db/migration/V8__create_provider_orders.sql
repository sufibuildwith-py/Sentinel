CREATE TABLE provider_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    provider_reference VARCHAR(255),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_orders_incident_id ON provider_orders(incident_id);
CREATE INDEX idx_provider_orders_razorpay_order_id ON provider_orders(razorpay_order_id);
