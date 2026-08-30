CREATE TABLE provider_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_order_id UUID REFERENCES provider_orders(id),
    razorpay_payment_id VARCHAR(255) NOT NULL UNIQUE,
    razorpay_order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paise BIGINT,
    method VARCHAR(100),
    captured_at TIMESTAMPTZ,
    raw_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_payments_order_id ON provider_payments(provider_order_id);
CREATE INDEX idx_provider_payments_razorpay_payment_id
    ON provider_payments(razorpay_payment_id);
